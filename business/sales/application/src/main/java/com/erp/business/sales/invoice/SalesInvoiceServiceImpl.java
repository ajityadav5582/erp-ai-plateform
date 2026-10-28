package com.erp.business.sales.invoice;

import com.erp.business.inventory.interfaces.item.ItemService;
import com.erp.business.sales.customer.domain.Customer;
import com.erp.business.sales.customer.infrastructure.persistence.CustomerRepository;
import com.erp.business.sales.invoice.domain.SalesInvoice;
import com.erp.business.sales.invoice.domain.SalesInvoiceLine;
import com.erp.business.sales.invoice.domain.SalesInvoiceStatus;
import com.erp.business.sales.invoice.domain.VatType;
import com.erp.business.sales.invoice.dto.CreateInvoiceRequest;
import com.erp.business.sales.invoice.dto.CreatePosInvoiceRequest;
import com.erp.business.sales.invoice.dto.InvoiceDecisionRequest;
import com.erp.business.sales.invoice.dto.InvoiceLineRequest;
import com.erp.business.sales.invoice.dto.SalesInvoiceResponse;
import com.erp.business.sales.invoice.dto.TaxInvoiceView;
import com.erp.business.sales.invoice.exception.SalesInvoiceNotFoundException;
import com.erp.business.sales.invoice.infrastructure.persistence.SalesInvoiceRepository;
import com.erp.business.sales.invoice.mapper.SalesInvoiceMapper;
import com.erp.business.sales.invoice.mapper.TaxInvoiceMapper;
import com.erp.business.sales.salesorder.SalesOrderService;
import com.erp.business.sales.salesorder.dto.SalesOrderResponse;
import com.erp.platform.business.context.BusinessContextAccessor;
import com.erp.business.sales.shared.numbering.DocumentNumberService;
import com.erp.business.sales.shared.security.SalesInvoicePermissions;
import com.erp.platform.security.annotation.RequiresPermission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Sales (tax) invoice use-case implementation.
 *
 * <p>The IRD invoice number is allocated inside the same transaction as the insert,
 * keeping the series sequential and gap-free. The customer snapshot - including the
 * buyer PAN that a B2B tax invoice must print - is copied from the sales order, and
 * each line's VAT treatment drives the register breakdown the aggregate derives.
 */
@Service
@Transactional
public class SalesInvoiceServiceImpl implements SalesInvoiceService {

    private static final String DOCUMENT_TYPE = "SALES_INVOICE";
    private static final String NUMBER_PREFIX = "INV-";

    private final SalesInvoiceRepository invoiceRepository;
    private final SalesOrderService salesOrderService;
    private final ItemService itemService;
    private final CustomerRepository customerRepository;
    private final SalesInvoiceMapper invoiceMapper;
    private final TaxInvoiceMapper taxInvoiceMapper;
    private final DocumentNumberService documentNumberService;
    private final BusinessContextAccessor context;

    public SalesInvoiceServiceImpl(SalesInvoiceRepository invoiceRepository,
                                   SalesOrderService salesOrderService,
                                   ItemService itemService,
                                   CustomerRepository customerRepository,
                                   SalesInvoiceMapper invoiceMapper,
                                   TaxInvoiceMapper taxInvoiceMapper,
                                   DocumentNumberService documentNumberService,
                                   BusinessContextAccessor context) {
        this.invoiceRepository = invoiceRepository;
        this.salesOrderService = salesOrderService;
        this.itemService = itemService;
        this.customerRepository = customerRepository;
        this.invoiceMapper = invoiceMapper;
        this.taxInvoiceMapper = taxInvoiceMapper;
        this.documentNumberService = documentNumberService;
        this.context = context;
    }

    @Override
    @RequiresPermission(SalesInvoicePermissions.SALES_INVOICE_CREATE)
    public SalesInvoiceResponse createInvoice(Long tenantId, Long companyId, CreateInvoiceRequest request) {
        SalesOrderResponse order = salesOrderService.getOrderById(tenantId, companyId, request.salesOrderId());

        if (order.status() == com.erp.business.sales.salesorder.domain.SalesOrderStatus.DRAFT
                || order.status() == com.erp.business.sales.salesorder.domain.SalesOrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot create Tax Invoice for Sales Order " + order.soNumber()
                    + ": Order is in " + order.status() + " status. Sales orders must be Confirmed or Delivered before invoicing.");
        }

        if (request.lines() == null || request.lines().isEmpty()) {
            throw new IllegalArgumentException("Tax Invoice must contain at least one line item.");
        }

        for (InvoiceLineRequest lineRequest : request.lines()) {
            if (lineRequest.quantity() == null || lineRequest.quantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Invoice line quantity must be greater than zero.");
            }
            if (lineRequest.unitPrice() != null && lineRequest.unitPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Unit price must not be negative.");
            }
        }

        String periodKey = context.findFiscalYearId() != null
                ? "FY" + context.findFiscalYearId() : "CY" + request.invoiceDate().getYear();
        String invoiceNumber = documentNumberService.nextNumber(
                tenantId, companyId, DOCUMENT_TYPE, periodKey, NUMBER_PREFIX);

        // A tax invoice that prints the buyer's PAN is a B2B supply; without a PAN
        // it is a simplified invoice to a consumer.
        boolean isTaxInvoice = order.customerPanNumber() != null && !order.customerPanNumber().isBlank();

        SalesInvoice invoice = SalesInvoice.draft(
                invoiceNumber, order.id(), order.soNumber(),
                request.deliveryId(), null,
                order.customerId(), order.customerCode(), order.customerName(),
                order.customerPanNumber(), request.invoiceDate(), request.dueDate(),
                isTaxInvoice, order.currency(), order.fiscalYearId(), order.branchId(),
                request.notes());
        invoice.assignScope(tenantId, companyId);

        int lineNumber = 1;
        for (InvoiceLineRequest lineRequest : request.lines()) {
            invoice.addLine(buildLine(lineNumber++, lineRequest));
        }

        return invoiceMapper.toResponse(invoiceRepository.save(invoice));
    }

    @Override
    public SalesInvoiceResponse createPosInvoice(Long tenantId, Long companyId, CreatePosInvoiceRequest request) {
        java.util.Optional<SalesInvoice> existing = invoiceRepository
                .findByTenantIdAndCompanyIdAndPosTransactionId(tenantId, companyId, request.posTransactionId());
        if (existing.isPresent()) {
            return invoiceMapper.toResponse(existing.get());
        }

        String periodKey = context.findFiscalYearId() != null
                ? "FY" + context.findFiscalYearId() : "CY" + request.invoiceDate().getYear();
        String invoiceNumber = documentNumberService.nextNumber(
                tenantId, companyId, DOCUMENT_TYPE, periodKey, NUMBER_PREFIX);

        boolean isTaxInvoice = request.customerPanNumber() != null && !request.customerPanNumber().isBlank();

        SalesInvoice invoice = SalesInvoice.draft(
                invoiceNumber, null, null,
                null, null,
                request.customerId(), null, request.customerName(),
                request.customerPanNumber(), request.invoiceDate(), null,
                isTaxInvoice, "NPR", context.findFiscalYearId(), null,
                request.notes());
        invoice.setPosTransactionId(request.posTransactionId());
        invoice.assignScope(tenantId, companyId);

        int lineNumber = 1;
        for (InvoiceLineRequest lineRequest : request.lines()) {
            invoice.addLine(buildLine(lineNumber++, lineRequest));
        }

        SalesInvoice saved = invoiceRepository.save(invoice);
        saved.issue(currentUser());
        return invoiceMapper.toResponse(invoiceRepository.save(saved));
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(SalesInvoicePermissions.SALES_INVOICE_VIEW)
    public SalesInvoiceResponse getInvoiceById(Long tenantId, Long companyId, Long id) {
        return invoiceMapper.toResponse(loadInvoice(tenantId, companyId, id));
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(SalesInvoicePermissions.SALES_INVOICE_VIEW)
    public SalesInvoiceResponse getInvoiceByNumber(Long tenantId, Long companyId, String invoiceNumber) {
        SalesInvoice invoice = invoiceRepository
                .findByTenantIdAndCompanyIdAndInvoiceNumber(tenantId, companyId, invoiceNumber)
                .orElseThrow(() -> new SalesInvoiceNotFoundException(
                        "Sales invoice not found with number: " + invoiceNumber));
        return invoiceMapper.toResponse(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(SalesInvoicePermissions.SALES_INVOICE_VIEW)
    public SalesInvoiceResponse getInvoiceByOrder(Long tenantId, Long companyId, Long salesOrderId) {
        SalesInvoice invoice = invoiceRepository
                .findFirstByTenantIdAndCompanyIdAndSalesOrderIdOrderByInvoiceDateDesc(tenantId, companyId, salesOrderId)
                .orElseThrow(() -> new SalesInvoiceNotFoundException(
                        "Sales invoice not found for order: " + salesOrderId));
        return invoiceMapper.toResponse(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(SalesInvoicePermissions.SALES_INVOICE_VIEW)
    public Page<SalesInvoiceResponse> listInvoices(Long tenantId, Long companyId, Pageable pageable) {
        return invoiceRepository.findByTenantIdAndCompanyId(tenantId, companyId, pageable)
                .map(invoiceMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(SalesInvoicePermissions.SALES_INVOICE_VIEW)
    public Page<SalesInvoiceResponse> listInvoicesByStatus(Long tenantId, Long companyId,
                                                           SalesInvoiceStatus status, Pageable pageable) {
        return invoiceRepository.findByTenantIdAndCompanyIdAndStatus(tenantId, companyId, status, pageable)
                .map(invoiceMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(SalesInvoicePermissions.SALES_INVOICE_VIEW)
    public Page<SalesInvoiceResponse> listInvoicesByCustomer(Long tenantId, Long companyId,
                                                             Long customerId, Pageable pageable) {
        return invoiceRepository.findByTenantIdAndCompanyIdAndCustomerId(tenantId, companyId, customerId, pageable)
                .map(invoiceMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(SalesInvoicePermissions.SALES_INVOICE_VIEW)
    public Page<SalesInvoiceResponse> searchInvoices(Long tenantId, Long companyId, String term, Pageable pageable) {
        return invoiceRepository.searchByNumber(tenantId, companyId, term, pageable)
                .map(invoiceMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(SalesInvoicePermissions.SALES_INVOICE_VIEW)
    public List<SalesInvoiceResponse> listOutstandingInvoices(Long tenantId, Long companyId) {
        return invoiceRepository
                .findByTenantIdAndCompanyIdAndStatusInAndBalanceAmountGreaterThanOrderByIdDesc(
                        tenantId, companyId,
                        List.of(SalesInvoiceStatus.ISSUED, SalesInvoiceStatus.PARTIALLY_PAID),
                        BigDecimal.ZERO)
                .stream().map(invoiceMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(SalesInvoicePermissions.SALES_INVOICE_VIEW)
    public List<Long> getInvoicedOrderIds(Long tenantId, Long companyId) {
        return invoiceRepository.findInvoicedSalesOrderIds(tenantId, companyId);
    }

    @Override
    @RequiresPermission(SalesInvoicePermissions.SALES_INVOICE_ISSUE)
    public SalesInvoiceResponse issueInvoice(Long tenantId, Long companyId, Long id) {
        SalesInvoice invoice = loadInvoice(tenantId, companyId, id);

        if (invoice.getStatus() != SalesInvoiceStatus.DRAFT) {
            throw new IllegalStateException("Invoice " + invoice.getInvoiceNumber()
                    + " cannot be issued because it is already in " + invoice.getStatus() + " status.");
        }
        if (invoice.getLines() == null || invoice.getLines().isEmpty()) {
            throw new IllegalStateException("Cannot issue Tax Invoice " + invoice.getInvoiceNumber()
                    + ": Invoice has no line items.");
        }

        invoice.issue(currentUser());
        return invoiceMapper.toResponse(invoiceRepository.save(invoice));
    }

    @Override
    @RequiresPermission(SalesInvoicePermissions.SALES_INVOICE_CANCEL)
    public SalesInvoiceResponse cancelInvoice(Long tenantId, Long companyId, Long id,
                                              InvoiceDecisionRequest request) {
        SalesInvoice invoice = loadInvoice(tenantId, companyId, id);

        if (invoice.getStatus() == SalesInvoiceStatus.CANCELLED) {
            throw new IllegalStateException("Invoice " + invoice.getInvoiceNumber() + " is already cancelled.");
        }
        if (invoice.getPaidAmount() != null && invoice.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException("Cannot cancel Tax Invoice " + invoice.getInvoiceNumber()
                    + ": Payments totaling NPR " + invoice.getPaidAmount()
                    + " have already been received. Under Nepal IRD regulations, please issue a Sales Return / Credit Note (अनुसूची ७) to refund the customer instead.");
        }
        if (request == null || request.reason() == null || request.reason().isBlank()) {
            throw new IllegalArgumentException("A cancellation reason is required to cancel Invoice " + invoice.getInvoiceNumber() + ".");
        }

        invoice.cancel(currentUser(), request.reason());
        return invoiceMapper.toResponse(invoiceRepository.save(invoice));
    }

    @Override
    @RequiresPermission(SalesInvoicePermissions.SALES_INVOICE_VIEW)
    public SalesInvoiceResponse recordPayment(Long tenantId, Long companyId, Long id, BigDecimal amount) {
        SalesInvoice invoice = loadInvoice(tenantId, companyId, id);
        invoice.recordPayment(amount);
        return invoiceMapper.toResponse(invoiceRepository.save(invoice));
    }

    @Override
    @RequiresPermission(SalesInvoicePermissions.SALES_INVOICE_VIEW)
    public SalesInvoiceResponse reversePayment(Long tenantId, Long companyId, Long id, BigDecimal amount) {
        SalesInvoice invoice = loadInvoice(tenantId, companyId, id);
        invoice.reversePayment(amount);
        return invoiceMapper.toResponse(invoiceRepository.save(invoice));
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(SalesInvoicePermissions.SALES_INVOICE_VIEW)
    public TaxInvoiceView getTaxInvoiceView(Long tenantId, Long companyId, Long id) {
        SalesInvoice invoice = loadInvoice(tenantId, companyId, id);
        String buyerAddress = customerRepository
                .findByTenantIdAndCompanyIdAndId(tenantId, companyId, invoice.getCustomerId())
                .map(Customer::getAddress)
                .orElse(null);
        return taxInvoiceMapper.toView(invoice, null, null, null, buyerAddress);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private SalesInvoice loadInvoice(Long tenantId, Long companyId, Long id) {
        return invoiceRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, id)
                .orElseThrow(() -> new SalesInvoiceNotFoundException(
                        "Sales invoice not found with ID: " + id));
    }

    private SalesInvoiceLine buildLine(int lineNumber, InvoiceLineRequest request) {
        ItemService.ItemResponse item = itemService.findById(request.itemId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Item not found with ID: " + request.itemId()));
        VatType vatType = request.vatType() != null ? request.vatType() : VatType.TAXABLE;
        return SalesInvoiceLine.create(
                lineNumber, item.getId(), item.getSku(), item.getName(),
                request.uomCode(), request.quantity(), request.unitPrice(),
                request.discountPercentage(), vatType, request.vatRate(), request.description());
    }

    private String currentUser() {
        String userId = context.getUserId();
        return (userId == null || userId.isBlank()) ? "pos-integration" : userId;
    }
}
