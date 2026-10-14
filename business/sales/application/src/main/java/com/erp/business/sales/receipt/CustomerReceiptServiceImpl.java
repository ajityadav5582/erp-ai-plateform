package com.erp.business.sales.receipt;

import com.erp.business.sales.invoice.SalesInvoiceService;
import com.erp.business.sales.invoice.dto.SalesInvoiceResponse;
import com.erp.business.sales.receipt.domain.CustomerReceipt;
import com.erp.business.sales.receipt.domain.ReceiptStatus;
import com.erp.business.sales.receipt.dto.CreateReceiptRequest;
import com.erp.business.sales.receipt.dto.CustomerReceiptResponse;
import com.erp.business.sales.receipt.dto.ReceiptDecisionRequest;
import com.erp.business.sales.receipt.exception.CustomerReceiptNotFoundException;
import com.erp.business.sales.receipt.infrastructure.persistence.CustomerReceiptRepository;
import com.erp.business.sales.receipt.mapper.CustomerReceiptMapper;
import com.erp.business.sales.shared.context.BusinessContextAccessor;
import com.erp.business.sales.shared.numbering.DocumentNumberService;
import com.erp.business.sales.shared.security.CustomerReceiptPermissions;
import com.erp.platform.security.annotation.RequiresPermission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Customer receipt use-case implementation.
 *
 * <p>Recording a receipt and applying the payment to the invoice happen in one
 * transaction, so a receipt and the invoice's reduced balance commit together or
 * not at all. Cancelling reverses both. The invoice refuses an overpayment, so a
 * receipt larger than the outstanding balance fails loudly rather than silently
 * creating a credit.
 */
@Service
@Transactional
public class CustomerReceiptServiceImpl implements CustomerReceiptService {

    private static final String DOCUMENT_TYPE = "CUSTOMER_RECEIPT";
    private static final String NUMBER_PREFIX = "RCT-";

    private final CustomerReceiptRepository receiptRepository;
    private final SalesInvoiceService salesInvoiceService;
    private final CustomerReceiptMapper receiptMapper;
    private final DocumentNumberService documentNumberService;
    private final BusinessContextAccessor context;

    public CustomerReceiptServiceImpl(CustomerReceiptRepository receiptRepository,
                                      SalesInvoiceService salesInvoiceService,
                                      CustomerReceiptMapper receiptMapper,
                                      DocumentNumberService documentNumberService,
                                      BusinessContextAccessor context) {
        this.receiptRepository = receiptRepository;
        this.salesInvoiceService = salesInvoiceService;
        this.receiptMapper = receiptMapper;
        this.documentNumberService = documentNumberService;
        this.context = context;
    }

    @Override
    @RequiresPermission(CustomerReceiptPermissions.CUSTOMER_RECEIPT_CREATE)
    public CustomerReceiptResponse createReceipt(Long tenantId, Long companyId, CreateReceiptRequest request) {
        SalesInvoiceResponse invoice = salesInvoiceService.getInvoiceById(tenantId, companyId, request.invoiceId());
        if (invoice.status() == com.erp.business.sales.invoice.domain.SalesInvoiceStatus.DRAFT) {
            throw new IllegalStateException("Cannot record payment: Invoice " + invoice.invoiceNumber()
                    + " is in DRAFT status. It must be issued before customer payments can be recorded.");
        }
        if (invoice.status() == com.erp.business.sales.invoice.domain.SalesInvoiceStatus.CANCELLED) {
            throw new IllegalStateException("Cannot record payment: Invoice " + invoice.invoiceNumber()
                    + " has been CANCELLED.");
        }
        if (invoice.status() == com.erp.business.sales.invoice.domain.SalesInvoiceStatus.PAID
                || invoice.balanceAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Invoice " + invoice.invoiceNumber()
                    + " is already fully paid (Balance: NPR 0.00).");
        }
        if (request.amount() == null || request.amount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Receipt payment amount must be greater than zero.");
        }
        if (request.amount().compareTo(invoice.balanceAmount()) > 0) {
            throw new IllegalStateException("Receipt payment amount (" + request.amount() + " " + invoice.currency()
                    + ") exceeds the remaining invoice balance (" + invoice.balanceAmount() + " " + invoice.currency()
                    + ") on Invoice " + invoice.invoiceNumber() + ".");
        }

        String periodKey = context.findFiscalYearId() != null
                ? "FY" + context.findFiscalYearId() : "CY" + request.receiptDate().getYear();
        String receiptNumber = documentNumberService.nextNumber(
                tenantId, companyId, DOCUMENT_TYPE, periodKey, NUMBER_PREFIX);

        CustomerReceipt receipt = CustomerReceipt.posted(
                receiptNumber, invoice.id(), invoice.invoiceNumber(),
                invoice.customerId(), invoice.customerCode(), invoice.customerName(),
                request.amount(), request.paymentMethod(), request.receiptDate(),
                invoice.fiscalYearId(), invoice.branchId(), request.reference(), request.notes(),
                currentUser());
        receipt.assignScope(tenantId, companyId);

        CustomerReceipt saved = receiptRepository.save(receipt);
        // Apply the money to the invoice in the same transaction.
        salesInvoiceService.recordPayment(tenantId, companyId, invoice.id(), request.amount());
        return receiptMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(CustomerReceiptPermissions.CUSTOMER_RECEIPT_VIEW)
    public CustomerReceiptResponse getReceiptById(Long tenantId, Long companyId, Long id) {
        return receiptMapper.toResponse(loadReceipt(tenantId, companyId, id));
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(CustomerReceiptPermissions.CUSTOMER_RECEIPT_VIEW)
    public CustomerReceiptResponse getReceiptByNumber(Long tenantId, Long companyId, String receiptNumber) {
        CustomerReceipt receipt = receiptRepository
                .findByTenantIdAndCompanyIdAndReceiptNumber(tenantId, companyId, receiptNumber)
                .orElseThrow(() -> new CustomerReceiptNotFoundException(
                        "Customer receipt not found with number: " + receiptNumber));
        return receiptMapper.toResponse(receipt);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(CustomerReceiptPermissions.CUSTOMER_RECEIPT_VIEW)
    public Page<CustomerReceiptResponse> listReceipts(Long tenantId, Long companyId, Pageable pageable) {
        return receiptRepository.findByTenantIdAndCompanyId(tenantId, companyId, pageable)
                .map(receiptMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(CustomerReceiptPermissions.CUSTOMER_RECEIPT_VIEW)
    public Page<CustomerReceiptResponse> listReceiptsByStatus(Long tenantId, Long companyId,
                                                              ReceiptStatus status, Pageable pageable) {
        return receiptRepository.findByTenantIdAndCompanyIdAndStatus(tenantId, companyId, status, pageable)
                .map(receiptMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(CustomerReceiptPermissions.CUSTOMER_RECEIPT_VIEW)
    public Page<CustomerReceiptResponse> listReceiptsByInvoice(Long tenantId, Long companyId,
                                                               Long invoiceId, Pageable pageable) {
        return receiptRepository.findByTenantIdAndCompanyIdAndInvoiceId(tenantId, companyId, invoiceId, pageable)
                .map(receiptMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(CustomerReceiptPermissions.CUSTOMER_RECEIPT_VIEW)
    public Page<CustomerReceiptResponse> listReceiptsByCustomer(Long tenantId, Long companyId,
                                                                Long customerId, Pageable pageable) {
        return receiptRepository.findByTenantIdAndCompanyIdAndCustomerId(tenantId, companyId, customerId, pageable)
                .map(receiptMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(CustomerReceiptPermissions.CUSTOMER_RECEIPT_VIEW)
    public Page<CustomerReceiptResponse> searchReceipts(Long tenantId, Long companyId, String term, Pageable pageable) {
        return receiptRepository.searchByNumber(tenantId, companyId, term, pageable)
                .map(receiptMapper::toResponse);
    }

    @Override
    @RequiresPermission(CustomerReceiptPermissions.CUSTOMER_RECEIPT_CANCEL)
    public CustomerReceiptResponse cancelReceipt(Long tenantId, Long companyId, Long id,
                                                 ReceiptDecisionRequest request) {
        CustomerReceipt receipt = loadReceipt(tenantId, companyId, id);
        if (receipt.getStatus() == ReceiptStatus.CANCELLED) {
            throw new IllegalStateException("Customer receipt " + receipt.getReceiptNumber() + " is already cancelled.");
        }
        if (request == null || request.reason() == null || request.reason().isBlank()) {
            throw new IllegalArgumentException("A cancellation reason is required to cancel Customer receipt " + receipt.getReceiptNumber() + ".");
        }
        receipt.cancel(currentUser(), request.reason());
        CustomerReceipt saved = receiptRepository.save(receipt);
        // Roll the money back off the invoice in the same transaction.
        salesInvoiceService.reversePayment(tenantId, companyId, saved.getInvoiceId(), saved.getAmount());
        return receiptMapper.toResponse(saved);
    }

    private CustomerReceipt loadReceipt(Long tenantId, Long companyId, Long id) {
        return receiptRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, id)
                .orElseThrow(() -> new CustomerReceiptNotFoundException(
                        "Customer receipt not found with ID: " + id));
    }

    private String currentUser() {
        return context.getUserId();
    }
}
