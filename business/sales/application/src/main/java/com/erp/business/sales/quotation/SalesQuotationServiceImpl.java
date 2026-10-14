package com.erp.business.sales.quotation;

import com.erp.business.inventory.interfaces.item.ItemService;
import com.erp.business.sales.customer.domain.Customer;
import com.erp.business.sales.customer.infrastructure.persistence.CustomerRepository;
import com.erp.business.sales.quotation.domain.SalesQuotation;
import com.erp.business.sales.quotation.domain.SalesQuotationLine;
import com.erp.business.sales.quotation.domain.SalesQuotationStatus;
import com.erp.business.sales.quotation.dto.CreateSalesQuotationRequest;
import com.erp.business.sales.quotation.dto.SalesQuotationLineRequest;
import com.erp.business.sales.quotation.dto.SalesQuotationResponse;
import com.erp.business.sales.quotation.infrastructure.persistence.SalesQuotationRepository;
import com.erp.business.sales.quotation.mapper.SalesQuotationMapper;
import com.erp.business.sales.salesorder.SalesOrderService;
import com.erp.business.sales.salesorder.dto.CreateSalesOrderRequest;
import com.erp.business.sales.salesorder.dto.SalesOrderLineRequest;
import com.erp.business.sales.salesorder.dto.SalesOrderResponse;
import com.erp.business.sales.shared.context.BusinessContextAccessor;
import com.erp.business.sales.shared.numbering.DocumentNumberService;
import com.erp.business.sales.shared.security.SalesOrderPermissions;
import com.erp.platform.security.annotation.RequiresPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class SalesQuotationServiceImpl implements SalesQuotationService {

    private static final String DOCUMENT_TYPE = "SALES_QUOTATION";
    private static final String NUMBER_PREFIX = "QT-";

    private final SalesQuotationRepository quotationRepository;
    private final CustomerRepository customerRepository;
    private final ItemService itemService;
    private final SalesOrderService salesOrderService;
    private final SalesQuotationMapper quotationMapper;
    private final DocumentNumberService documentNumberService;
    private final BusinessContextAccessor context;

    @Override
    @RequiresPermission(SalesOrderPermissions.SALES_ORDER_CREATE)
    public SalesQuotationResponse createQuotation(Long tenantId, Long companyId, CreateSalesQuotationRequest request) {
        Customer customer = customerRepository
                .findByTenantIdAndCompanyIdAndId(tenantId, companyId, request.customerId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Customer not found with ID: " + request.customerId()));

        if (Boolean.FALSE.equals(customer.getIsActive())) {
            throw new IllegalStateException("Customer '" + customer.getName() + " (" + customer.getCode()
                    + ")' is inactive. Cannot create quotations for an inactive customer.");
        }

        if (request.lines() == null || request.lines().isEmpty()) {
            throw new IllegalArgumentException("Sales quotation must contain at least one line item.");
        }

        for (SalesQuotationLineRequest lineReq : request.lines()) {
            if (lineReq.quantity() == null || lineReq.quantity().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Line quantity must be greater than zero.");
            }
            if (lineReq.unitPrice() != null && lineReq.unitPrice().compareTo(java.math.BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Unit price must not be negative.");
            }
        }

        String periodKey = resolvePeriodKey(request.quotationDate());
        String quotationNumber = documentNumberService.nextNumber(
                tenantId, companyId, DOCUMENT_TYPE, periodKey, NUMBER_PREFIX);

        SalesQuotation quotation = SalesQuotation.draft(
                quotationNumber,
                request.quotationDate(),
                request.validUntilDate(),
                customer.getId(),
                customer.getCode(),
                customer.getName(),
                customer.getPanNumber(),
                customer.getCurrency(),
                context.findFiscalYearId(),
                request.branchId(),
                request.warehouseId(),
                request.reference(),
                request.termsAndConditions(),
                request.notes()
        );

        quotation.assignScope(tenantId, companyId);

        int lineNumber = 1;
        for (SalesQuotationLineRequest lineRequest : request.lines()) {
            quotation.addLine(buildLine(lineNumber++, lineRequest));
        }

        return quotationMapper.toResponse(quotationRepository.save(quotation));
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(SalesOrderPermissions.SALES_ORDER_VIEW)
    public SalesQuotationResponse getQuotationById(Long tenantId, Long companyId, Long id) {
        SalesQuotation quotation = quotationRepository
                .findByTenantIdAndCompanyIdAndId(tenantId, companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Quotation not found with ID: " + id));
        return quotationMapper.toResponse(quotation);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(SalesOrderPermissions.SALES_ORDER_VIEW)
    public Page<SalesQuotationResponse> listQuotations(Long tenantId, Long companyId, Pageable pageable) {
        return quotationRepository.findByTenantIdAndCompanyId(tenantId, companyId, pageable)
                .map(quotationMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(SalesOrderPermissions.SALES_ORDER_VIEW)
    public Page<SalesQuotationResponse> listQuotationsByStatus(Long tenantId, Long companyId,
                                                              SalesQuotationStatus status, Pageable pageable) {
        return quotationRepository.findByTenantIdAndCompanyIdAndStatus(tenantId, companyId, status, pageable)
                .map(quotationMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(SalesOrderPermissions.SALES_ORDER_VIEW)
    public Page<SalesQuotationResponse> searchQuotations(Long tenantId, Long companyId, String term, Pageable pageable) {
        return quotationRepository.search(tenantId, companyId, term, pageable)
                .map(quotationMapper::toResponse);
    }

    @Override
    @RequiresPermission(SalesOrderPermissions.SALES_ORDER_EDIT)
    public SalesQuotationResponse sendQuotation(Long tenantId, Long companyId, Long id) {
        SalesQuotation quotation = quotationRepository
                .findByTenantIdAndCompanyIdAndId(tenantId, companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Quotation not found with ID: " + id));
        quotation.markSent();
        return quotationMapper.toResponse(quotationRepository.save(quotation));
    }

    @Override
    @RequiresPermission(SalesOrderPermissions.SALES_ORDER_EDIT)
    public SalesQuotationResponse acceptQuotation(Long tenantId, Long companyId, Long id) {
        SalesQuotation quotation = quotationRepository
                .findByTenantIdAndCompanyIdAndId(tenantId, companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Quotation not found with ID: " + id));
        quotation.markAccepted();
        return quotationMapper.toResponse(quotationRepository.save(quotation));
    }

    @Override
    @RequiresPermission(SalesOrderPermissions.SALES_ORDER_EDIT)
    public SalesQuotationResponse rejectQuotation(Long tenantId, Long companyId, Long id, String reason) {
        SalesQuotation quotation = quotationRepository
                .findByTenantIdAndCompanyIdAndId(tenantId, companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Quotation not found with ID: " + id));
        quotation.markRejected(reason);
        return quotationMapper.toResponse(quotationRepository.save(quotation));
    }

    @Override
    @RequiresPermission(SalesOrderPermissions.SALES_ORDER_CREATE)
    public SalesOrderResponse convertToSalesOrder(Long tenantId, Long companyId, Long id) {
        SalesQuotation quotation = quotationRepository
                .findByTenantIdAndCompanyIdAndId(tenantId, companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Quotation not found with ID: " + id));

        if (quotation.getStatus() == SalesQuotationStatus.CONVERTED) {
            throw new IllegalStateException("Quotation " + quotation.getQuotationNumber()
                    + " is already converted to a Sales Order.");
        }
        if (quotation.getStatus() == SalesQuotationStatus.REJECTED) {
            throw new IllegalStateException("Quotation " + quotation.getQuotationNumber()
                    + " has been rejected and cannot be converted to a Sales Order.");
        }
        if (quotation.getValidUntilDate() != null && quotation.getValidUntilDate().isBefore(LocalDate.now())) {
            throw new IllegalStateException("Quotation " + quotation.getQuotationNumber()
                    + " expired on " + quotation.getValidUntilDate() + " and cannot be converted without renewal.");
        }
        if (quotation.getLines() == null || quotation.getLines().isEmpty()) {
            throw new IllegalStateException("Cannot convert Quotation " + quotation.getQuotationNumber()
                    + ": Quotation contains no line items.");
        }

        List<SalesOrderLineRequest> orderLines = quotation.getLines().stream()
                .map(ql -> new SalesOrderLineRequest(
                        ql.getItemId(),
                        ql.getUomCode(),
                        ql.getQuantity(),
                        ql.getUnitPrice(),
                        ql.getDiscountPercentage(),
                        ql.getTaxRate(),
                        ql.getDescription()
                ))
                .toList();

        CreateSalesOrderRequest createOrderRequest = new CreateSalesOrderRequest(
                quotation.getCustomerId(),
                LocalDate.now(),
                quotation.getValidUntilDate(),
                quotation.getBranchId(),
                quotation.getWarehouseId(),
                null,
                quotation.getQuotationNumber(),
                "Converted from Quotation " + quotation.getQuotationNumber()
                        + (quotation.getNotes() != null ? ("\n" + quotation.getNotes()) : ""),
                orderLines
        );

        SalesOrderResponse createdOrder = salesOrderService.createOrder(tenantId, companyId, createOrderRequest);

        quotation.markConverted(createdOrder.id());
        quotationRepository.save(quotation);

        return createdOrder;
    }

    private SalesQuotationLine buildLine(int lineNumber, SalesQuotationLineRequest request) {
        ItemService.ItemResponse item = itemService.findById(request.itemId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Item not found with ID: " + request.itemId()));
        if (Boolean.FALSE.equals(item.getIsActive())) {
            throw new IllegalArgumentException("Item " + item.getSku() + " is not active");
        }
        return SalesQuotationLine.create(
                lineNumber,
                item.getId(),
                item.getSku(),
                item.getName(),
                request.uomCode() != null ? request.uomCode() : item.getUnitCode(),
                request.quantity(),
                request.unitPrice(),
                request.discountPercentage(),
                request.taxRate(),
                request.description()
        );
    }

    private String resolvePeriodKey(LocalDate quotationDate) {
        Long fiscalYearId = context.findFiscalYearId();
        return fiscalYearId != null ? "FY" + fiscalYearId : "CY" + quotationDate.getYear();
    }
}
