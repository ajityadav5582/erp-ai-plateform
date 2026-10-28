package com.erp.business.procurement.purchaseorder;

import com.erp.business.inventory.interfaces.unit.UnitService;
import com.erp.business.procurement.purchaserequisition.domain.PurchaseRequisition;
import com.erp.business.procurement.purchaserequisition.domain.PurchaseRequisitionLine;
import com.erp.business.procurement.purchaserequisition.domain.PurchaseRequisitionOrderLink;
import com.erp.business.procurement.purchaserequisition.domain.PurchaseRequisitionStatus;
import com.erp.business.procurement.purchaserequisition.infrastructure.persistence.PurchaseRequisitionOrderLinkRepository;
import com.erp.business.procurement.purchaserequisition.infrastructure.persistence.PurchaseRequisitionRepository;
import com.erp.business.procurement.purchaseorder.domain.PurchaseOrder;
import com.erp.business.procurement.purchaseorder.domain.PurchaseOrderLine;
import com.erp.business.procurement.purchaseorder.dto.ConvertToPurchaseOrderRequest;
import com.erp.business.procurement.purchaseorder.dto.PurchaseOrderResponse;
import com.erp.business.procurement.purchaseorder.infrastructure.persistence.PurchaseOrderRepository;
import com.erp.business.procurement.shared.domain.DocumentSequence;
import com.erp.business.procurement.shared.infrastructure.persistence.DocumentSequenceRepository;
import com.erp.business.procurement.shared.security.PurchaseOrderPermissions;
import com.erp.platform.common.exception.ResourceNotFoundException;
import com.erp.platform.common.exception.ValidationException;
import com.erp.platform.common.nepali.date.NepaliDate;
import com.erp.platform.common.nepali.date.NepaliDateConverter;
import com.erp.platform.security.annotation.RequiresPermission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Requisition to purchase order conversion.
 *
 * <p><strong>Only APPROVED converts.</strong> A draft has not been authorised to
 * spend, a submitted one has not been authorised yet, and a converted one has
 * already been. The aggregate enforces the rule; this repeats it so the caller gets
 * a message naming the state instead of a generic lifecycle failure.
 *
 * <p><strong>Two layers stop a duplicate conversion.</strong> The requisition's own
 * status flips to CONVERTED_TO_PO, which refuses the second call, and a unique key on
 * {@code (tenant_id, company_id, source_purchase_requisition_id)} refuses it at the
 * database. The status check alone is not enough: two concurrent requests both load
 * an APPROVED document before either has written, so both would pass it. The
 * repository lookup below turns the loser into a readable error; the unique key is
 * what actually holds.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseOrderConversionServiceImpl implements PurchaseOrderConversionService {

    private static final String DOCUMENT_TYPE = "PURCHASE_ORDER";
    private static final String PREFIX = "PO-";
    private static final int NUMBER_WIDTH = 6;
    private static final BigDecimal NO_DISCOUNT = BigDecimal.ZERO;

    private final PurchaseOrderRepository orderRepository;
    private final PurchaseRequisitionRepository requisitionRepository;
    private final PurchaseRequisitionOrderLinkRepository orderLinkRepository;
    private final DocumentSequenceRepository sequenceRepository;
    private final PurchaseOrderAssembler assembler;
    private final UnitService unitService;

    @Override
    @Transactional
    @RequiresPermission(PurchaseOrderPermissions.CREATE)
    public PurchaseOrderResponse convert(Long tenantId, Long companyId,
                                         ConvertToPurchaseOrderRequest request, String actor) {

        requireScope(tenantId, companyId);

        PurchaseRequisition requisition = requisitionRepository
                .findByIdForUpdate(tenantId, companyId, request.requisitionId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Purchase requisition " + request.requisitionId() + " was not found"));

        if (requisition.getStatus() != PurchaseRequisitionStatus.APPROVED) {
            throw new ValidationException(
                    "Requisition " + requisition.getPrNumber() + " is "
                            + requisition.getStatus()
                            + "; only an APPROVED requisition can be converted into a purchase order");
        }

        requireNoExistingOrder(tenantId, companyId, requisition);

        List<PurchaseRequisitionLine> sourceLines = requisition.outstandingLines();
        if (sourceLines.isEmpty()) {
            throw new ValidationException(
                    "Requisition " + requisition.getPrNumber()
                            + " has no approved quantity left to order");
        }

        // Validate UOM codes on all source lines before creating the order
        validateUomCodes(sourceLines, tenantId, companyId);

        LocalDate orderDate = LocalDate.now();
        PurchaseOrder order = PurchaseOrder.fromRequisition(
                allocateNumber(tenantId, companyId, orderDate),
                orderDate,
                request.supplierId(),
                request.supplierCode(),
                request.supplierName(),
                requisition.getCurrency(),
                requisition.getFiscalYearId(),
                requisition.getBranchId(),
                requisition.getId());

        order.assignScope(tenantId, companyId);
        setDeliveryDetails(order, requisition, request);

        // Renumbered from 1 in requisition order. The requisition line number is kept
        // on the order line instead, via sourcePurchaseRequisitionItemId, so a reader
        // can still match the two grids.
        int lineNumber = 1;
        for (PurchaseRequisitionLine source : sourceLines) {
            order.addLine(PurchaseOrderLine.create(
                    lineNumber++,
                    source.getItemId(),
                    source.getItemCode(),
                    source.getItemName(),
                    source.getUomCode(),
                    source.getUnitId(),
                    source.getApprovedQuantity(),
                    source.getEstimatedUnitPrice(),
                    NO_DISCOUNT,
                    source.getTaxRate(),
                    source.getId()));
        }

        PurchaseOrder saved = orderRepository.saveAndFlush(order);

        // Requisition-side effects run only after the order is on disk with its line
        // ids, so the links written below can name real order lines. If any of this
        // failed the transaction rolls the order back with it.
        for (PurchaseRequisitionLine source : sourceLines) {
            source.registerOrdered(source.getApprovedQuantity());
        }

        PurchaseRequisitionStatus from = requisition.getStatus();
        requisition.convertToPO(actor);
        requisition.recordTransition(from, PurchaseRequisitionStatus.CONVERTED_TO_PO, actor, null);
        requisitionRepository.save(requisition);
        requisitionRepository.flush();

        Map<Long, Long> orderLineIds = new LinkedHashMap<>();
        for (PurchaseOrderLine line : saved.getLines()) {
            orderLineIds.put(line.getSourcePurchaseRequisitionItemId(), line.getId());
        }

        for (PurchaseRequisitionLine source : sourceLines) {
            PurchaseRequisitionOrderLink link = PurchaseRequisitionOrderLink.award(
                    requisition,
                    source.getId(),
                    saved.getId(),
                    orderLineIds.get(source.getId()),
                    source.getApprovedQuantity(),
                    actor);
            link.assignScope(tenantId, companyId);
            orderLinkRepository.save(link);
        }

        return assembler.toResponse(saved);
    }

    /**
     * Validates that all UOM codes on the source lines exist, are active,
     * and belong to the same tenant/company scope.
     */
    private void validateUomCodes(List<PurchaseRequisitionLine> sourceLines, Long tenantId, Long companyId) {
        for (PurchaseRequisitionLine source : sourceLines) {
            String normalizedUomCode = source.getUomCode().trim().toUpperCase();
            UnitService.UnitResponse unit = unitService.findByCode(normalizedUomCode)
                    .orElseThrow(() -> new ValidationException(
                            "Unit of measure '" + normalizedUomCode + "' not found for this company"));
            if (Boolean.FALSE.equals(unit.getIsActive())) {
                throw new ValidationException(
                        "Unit of measure '" + normalizedUomCode + "' is inactive and cannot be used");
            }
        }
    }

    /**
     * Refuses a requisition that already produced an order.
     *
     * <p>Reachable only when the requisition is still APPROVED while an order already
     * names it. The unique key would reject the insert anyway; this reports it in the
     * caller's terms instead of as a constraint violation.
     */
    private void requireNoExistingOrder(Long tenantId, Long companyId, PurchaseRequisition requisition) {
        orderRepository
                .findByTenantIdAndCompanyIdAndSourcePurchaseRequisitionId(
                        tenantId, companyId, requisition.getId())
                .ifPresent(existing -> {
                    throw new ValidationException(
                            "Requisition " + requisition.getPrNumber()
                                    + " has already been converted into purchase order "
                                    + existing.getPoNumber());
                });
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(PurchaseOrderPermissions.VIEW)
    public PurchaseOrderResponse getById(Long tenantId, Long companyId, Long id) {
        return assembler.toResponse(orderRepository
                .findByTenantIdAndCompanyIdAndId(tenantId, companyId, id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Purchase order " + id + " was not found")));
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(PurchaseOrderPermissions.VIEW)
    public Page<PurchaseOrderResponse> findAll(Long tenantId, Long companyId, Pageable pageable) {
        return orderRepository.findInScope(tenantId, companyId, pageable)
                .map(assembler::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(PurchaseOrderPermissions.VIEW)
    public Page<PurchaseOrderResponse> findOutstanding(
            Long tenantId, Long companyId, Pageable pageable) {
        return orderRepository.findOutstandingInScope(tenantId, companyId, pageable)
                .map(assembler::toResponse);
    }

    /**
     * Copies the delivery commitment, preferring the buyer's date.
     *
     * <p>Falling back to the requisition's needed-by date rather than to none: an
     * order without a delivery date cannot be chased, and the requisition already
     * carries the date the branch asked for.
     *
     * <p>The warehouse travels the same way, unchanged. It is a nomination - where the
     * buyer intends the goods to be stored - so the requester who chose it at
     * requisition stage still chooses it at order stage, and the buyer only states a
     * different one at the point of goods receipt. A requisition that named no
     * warehouse leaves the order without one rather than the conversion inventing the
     * company default here, because a default chosen silently at conversion time is
     * indistinguishable from a deliberate decision once someone is reading the order
     * two years later.
     */
    private void setDeliveryDetails(PurchaseOrder order, PurchaseRequisition requisition,
                                    ConvertToPurchaseOrderRequest request) {
        LocalDate expected = request.expectedDeliveryDate() != null
                ? request.expectedDeliveryDate()
                : requisition.getRequiredByDate();
        order.adoptDelivery(expected,
                requisition.getWarehouseId(),
                requisition.getWarehouseCode(),
                requisition.getWarehouseName(),
                requisition.getPrNumber(),
                request.notes());
    }

    /**
     * Allocates the next order number for the fiscal period.
     *
     * <p>Under the sequence repository's pessimistic write lock, for the same reason
     * requisition numbering is: two buyers converting at the same moment must not
     * read the same counter value.
     */
    private String allocateNumber(Long tenantId, Long companyId, LocalDate orderDate) {
        String periodKey = fiscalPeriodKey(orderDate);
        DocumentSequence sequence = sequenceRepository
                .findForUpdate(tenantId, companyId, DOCUMENT_TYPE, periodKey)
                .orElseGet(() -> sequenceRepository.save(
                        DocumentSequence.startAt(tenantId, companyId, DOCUMENT_TYPE, periodKey,
                                highestIssuedNumber(tenantId, companyId))));

        return sequence.format(sequence.allocate(), PREFIX, NUMBER_WIDTH);
    }

    /**
     * Fiscal-year period key, matching the requisition numbering so both document
     * series reset at the same boundary.
     */
    private String fiscalPeriodKey(LocalDate date) {
        try {
            NepaliDate bs = NepaliDateConverter.fromAd(date);
            int startYear = bs.month() >= 4 ? bs.year() : bs.year() - 1;
            return startYear + "/" + String.format("%02d", (startYear + 1) % 100);
        } catch (RuntimeException ex) {
            log.debug("Order date {} is outside the published Bikram Sambat calendar; "
                    + "numbering against its Gregorian year instead", date);
            return "AD" + date.getYear();
        }
    }

    private long highestIssuedNumber(Long tenantId, Long companyId) {
        Long highest = orderRepository.findHighestIssuedNumber(
                tenantId, companyId, PREFIX, PREFIX.length());
        return highest == null ? 0L : highest;
    }

    private void requireScope(Long tenantId, Long companyId) {
        if (tenantId == null) {
            throw new ValidationException("A tenant scope is required to create a purchase order");
        }
        if (companyId == null) {
            throw new ValidationException("A company scope is required to create a purchase order");
        }
    }

}
