package com.erp.business.sales.salesreturn;

import com.erp.business.inventory.interfaces.item.ItemService;
import com.erp.business.inventory.interfaces.stock.StockMovementLineRequest;
import com.erp.business.inventory.interfaces.stock.StockMovementService;
import com.erp.business.inventory.interfaces.stock.StockReceiptRequest;
import com.erp.business.sales.invoice.domain.VatType;
import com.erp.business.sales.invoice.dto.SalesInvoiceResponse;
import com.erp.business.sales.invoice.SalesInvoiceService;
import com.erp.business.sales.salesreturn.domain.ReturnStatus;
import com.erp.business.sales.salesreturn.domain.SalesReturn;
import com.erp.business.sales.salesreturn.domain.SalesReturnLine;
import com.erp.business.sales.salesreturn.dto.CreateReturnRequest;
import com.erp.business.sales.salesreturn.dto.ReturnDecisionRequest;
import com.erp.business.sales.salesreturn.dto.ReturnLineRequest;
import com.erp.business.sales.salesreturn.dto.SalesReturnResponse;
import com.erp.business.sales.salesreturn.exception.SalesReturnNotFoundException;
import com.erp.business.sales.salesreturn.infrastructure.persistence.SalesReturnRepository;
import com.erp.business.sales.salesreturn.mapper.SalesReturnMapper;
import com.erp.business.sales.shared.context.BusinessContextAccessor;
import com.erp.business.sales.shared.numbering.DocumentNumberService;
import com.erp.business.sales.shared.security.SalesReturnPermissions;
import com.erp.platform.security.annotation.RequiresPermission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Sales return (credit note) use-case implementation.
 *
 * <p>The issued invoice is never mutated: a credit note is a separate IRD document
 * that references it. Posting restocks the returned goods (an inbound movement in
 * Inventory) inside the same transaction as the status change, so a partial failure
 * leaves neither a half-posted credit note nor a stock movement without it.
 */
@Service
@Transactional
public class SalesReturnServiceImpl implements SalesReturnService {

    private static final String DOCUMENT_TYPE = "SALES_RETURN";
    private static final String NUMBER_PREFIX = "CN-";
    private static final String STOCK_REFERENCE_TYPE = "SALES_RETURN";

    private final SalesReturnRepository returnRepository;
    private final SalesInvoiceService salesInvoiceService;
    private final ItemService itemService;
    private final StockMovementService stockMovementService;
    private final SalesReturnMapper returnMapper;
    private final DocumentNumberService documentNumberService;
    private final BusinessContextAccessor context;

    public SalesReturnServiceImpl(SalesReturnRepository returnRepository,
                                  SalesInvoiceService salesInvoiceService,
                                  ItemService itemService,
                                  StockMovementService stockMovementService,
                                  SalesReturnMapper returnMapper,
                                  DocumentNumberService documentNumberService,
                                  BusinessContextAccessor context) {
        this.returnRepository = returnRepository;
        this.salesInvoiceService = salesInvoiceService;
        this.itemService = itemService;
        this.stockMovementService = stockMovementService;
        this.returnMapper = returnMapper;
        this.documentNumberService = documentNumberService;
        this.context = context;
    }

    @Override
    @RequiresPermission(SalesReturnPermissions.SALES_RETURN_CREATE)
    public SalesReturnResponse createReturn(Long tenantId, Long companyId, CreateReturnRequest request) {
        SalesInvoiceResponse invoice = salesInvoiceService.getInvoiceById(tenantId, companyId, request.invoiceId());

        String periodKey = context.findFiscalYearId() != null
                ? "FY" + context.findFiscalYearId() : "CY" + request.returnDate().getYear();
        String creditNoteNumber = documentNumberService.nextNumber(
                tenantId, companyId, DOCUMENT_TYPE, periodKey, NUMBER_PREFIX);

        SalesReturn salesReturn = SalesReturn.draft(
                creditNoteNumber, invoice.id(), invoice.invoiceNumber(),
                invoice.salesOrderId(), invoice.soNumber(),
                invoice.customerId(), invoice.customerCode(), invoice.customerName(),
                invoice.customerPanNumber(), request.warehouseId(), request.returnDate(),
                invoice.fiscalYearId(), invoice.branchId(), request.reason());
        salesReturn.assignScope(tenantId, companyId);

        int lineNumber = 1;
        for (ReturnLineRequest lineRequest : request.lines()) {
            salesReturn.addLine(buildLine(lineNumber++, lineRequest));
        }

        return returnMapper.toResponse(returnRepository.save(salesReturn));
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(SalesReturnPermissions.SALES_RETURN_VIEW)
    public SalesReturnResponse getReturnById(Long tenantId, Long companyId, Long id) {
        return returnMapper.toResponse(loadReturn(tenantId, companyId, id));
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(SalesReturnPermissions.SALES_RETURN_VIEW)
    public SalesReturnResponse getReturnByNumber(Long tenantId, Long companyId, String creditNoteNumber) {
        SalesReturn salesReturn = returnRepository
                .findByTenantIdAndCompanyIdAndCreditNoteNumber(tenantId, companyId, creditNoteNumber)
                .orElseThrow(() -> new SalesReturnNotFoundException(
                        "Sales return not found with number: " + creditNoteNumber));
        return returnMapper.toResponse(salesReturn);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(SalesReturnPermissions.SALES_RETURN_VIEW)
    public Page<SalesReturnResponse> listReturns(Long tenantId, Long companyId, Pageable pageable) {
        return returnRepository.findByTenantIdAndCompanyId(tenantId, companyId, pageable)
                .map(returnMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(SalesReturnPermissions.SALES_RETURN_VIEW)
    public Page<SalesReturnResponse> listReturnsByStatus(Long tenantId, Long companyId,
                                                         ReturnStatus status, Pageable pageable) {
        return returnRepository.findByTenantIdAndCompanyIdAndStatus(tenantId, companyId, status, pageable)
                .map(returnMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(SalesReturnPermissions.SALES_RETURN_VIEW)
    public Page<SalesReturnResponse> listReturnsByInvoice(Long tenantId, Long companyId,
                                                          Long invoiceId, Pageable pageable) {
        return returnRepository.findByTenantIdAndCompanyIdAndInvoiceId(tenantId, companyId, invoiceId, pageable)
                .map(returnMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(SalesReturnPermissions.SALES_RETURN_VIEW)
    public Page<SalesReturnResponse> searchReturns(Long tenantId, Long companyId, String term, Pageable pageable) {
        return returnRepository.searchByNumber(tenantId, companyId, term, pageable)
                .map(returnMapper::toResponse);
    }

    @Override
    @RequiresPermission(SalesReturnPermissions.SALES_RETURN_POST)
    public SalesReturnResponse postReturn(Long tenantId, Long companyId, Long id) {
        SalesReturn salesReturn = loadReturn(tenantId, companyId, id);
        salesReturn.post(currentUser());
        SalesReturn saved = returnRepository.save(salesReturn);
        // Restock the returned goods (an inbound movement), idempotent by reference.
        stockMovementService.postReceipt(buildStockRequest(saved));
        return returnMapper.toResponse(saved);
    }

    @Override
    @RequiresPermission(SalesReturnPermissions.SALES_RETURN_CANCEL)
    public SalesReturnResponse cancelReturn(Long tenantId, Long companyId, Long id,
                                            ReturnDecisionRequest request) {
        SalesReturn salesReturn = loadReturn(tenantId, companyId, id);
        boolean wasPosted = salesReturn.getStatus() == ReturnStatus.POSTED;
        salesReturn.cancel(currentUser(), request.reason());
        SalesReturn saved = returnRepository.save(salesReturn);

        // Reverse the inbound stock movement if it was posted.
        if (wasPosted) {
            stockMovementService.reverseReceipt(buildStockRequest(saved));
        }
        return returnMapper.toResponse(saved);
    }

    private SalesReturn loadReturn(Long tenantId, Long companyId, Long id) {
        return returnRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, id)
                .orElseThrow(() -> new SalesReturnNotFoundException(
                        "Sales return not found with ID: " + id));
    }

    private SalesReturnLine buildLine(int lineNumber, ReturnLineRequest request) {
        ItemService.ItemResponse item = itemService.findById(request.itemId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Item not found with ID: " + request.itemId()));
        VatType vatType = request.vatType() != null ? request.vatType() : VatType.TAXABLE;
        return SalesReturnLine.create(
                lineNumber, item.getId(), item.getSku(), item.getName(),
                request.uomCode(), request.quantity(), request.unitPrice(),
                vatType, request.vatRate(), request.description());
    }

    private StockReceiptRequest buildStockRequest(SalesReturn salesReturn) {
        List<StockMovementLineRequest> stockLines = salesReturn.getLines().stream()
                .map(line -> new StockMovementLineRequest(
                        salesReturn.getWarehouseId(), line.getItemId(), line.getUomCode(), line.getQuantity()))
                .toList();
        return new StockReceiptRequest(
                STOCK_REFERENCE_TYPE, salesReturn.getId(), salesReturn.getCreditNoteNumber(),
                Instant.now(), currentUser(), null, stockLines);
    }

    private String currentUser() {
        return context.getUserId();
    }
}
