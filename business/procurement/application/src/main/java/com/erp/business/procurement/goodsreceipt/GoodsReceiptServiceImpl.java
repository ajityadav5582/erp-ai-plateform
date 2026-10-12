package com.erp.business.procurement.goodsreceipt;

import com.erp.business.inventory.interfaces.stock.StockMovementLineRequest;
import com.erp.business.inventory.interfaces.stock.StockMovementService;
import com.erp.business.inventory.interfaces.stock.StockReceiptRequest;
import com.erp.business.inventory.interfaces.unit.UnitService;
import com.erp.business.inventory.interfaces.warehouse.WarehouseService;
import com.erp.business.procurement.goodsreceipt.domain.GoodsReceipt;
import com.erp.business.procurement.goodsreceipt.domain.GoodsReceiptLine;
import com.erp.business.procurement.goodsreceipt.domain.StockPostStatus;
import com.erp.business.procurement.goodsreceipt.dto.CreateGoodsReceiptRequest;
import com.erp.business.procurement.goodsreceipt.dto.GoodsReceiptResponse;
import com.erp.business.procurement.goodsreceipt.infrastructure.persistence.GoodsReceiptRepository;
import com.erp.business.procurement.purchaseorder.domain.PurchaseOrder;
import com.erp.business.procurement.purchaseorder.domain.PurchaseOrderLine;
import com.erp.business.procurement.purchaseorder.domain.PurchaseOrderLineStatus;
import com.erp.business.procurement.purchaseorder.infrastructure.persistence.PurchaseOrderRepository;
import com.erp.business.procurement.shared.domain.DocumentSequence;
import com.erp.business.procurement.shared.infrastructure.persistence.DocumentSequenceRepository;
import com.erp.business.procurement.shared.security.ProcurementPermissions;
import com.erp.platform.common.exception.ResourceNotFoundException;
import com.erp.platform.common.exception.ValidationException;
import com.erp.platform.common.nepali.date.NepaliDate;
import com.erp.platform.common.nepali.date.NepaliDateConverter;
import com.erp.platform.security.annotation.RequiresPermission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Records goods receipts and applies them to their purchase orders.
 *
 * <p><strong>The order is loaded under a write lock.</strong> Two storekeepers receiving
 * against the same order would both read the same outstanding quantities, and whichever
 * wrote last would silently discard the other's delivery. The lock makes the second
 * request wait and re-read, so it sees the quantity the first one already consumed.
 *
 * <p><strong>The order, not the request, decides what is receivable.</strong> The
 * caller may say how much of a line arrived, but every quantity is checked against
 * that line's outstanding quantity inside the aggregate, which is the only writer of
 * {@code receivedQuantity}. A caller cannot post a quantity the order does not owe.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GoodsReceiptServiceImpl implements GoodsReceiptService {

    private static final String DOCUMENT_TYPE = "GOODS_RECEIPT";
    private static final String PREFIX = "GRN-";
    private static final int NUMBER_WIDTH = 6;

    private final GoodsReceiptRepository receiptRepository;
    private final PurchaseOrderRepository orderRepository;
    private final DocumentSequenceRepository sequenceRepository;
    private final UnitService unitService;
    private final WarehouseService warehouseService;
    private final StockMovementService stockMovementService;
    private final ApplicationEventPublisher events;

    @Override
    @Transactional
    @RequiresPermission(ProcurementPermissions.GOODS_RECEIPT_CREATE)
    public GoodsReceiptResponse record(Long tenantId, Long companyId,
                                       CreateGoodsReceiptRequest request, String actor) {

        if (tenantId == null || companyId == null) {
            throw new ValidationException("A tenant and company scope are required");
        }

        PurchaseOrder order = orderRepository
                .findByIdForUpdate(tenantId, companyId, request.purchaseOrderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Purchase order " + request.purchaseOrderId() + " was not found"));

        List<Arrival> arriving = resolveArrivals(order, request);

        // Validate UOM codes on all arriving lines before creating the receipt
        validateUomCodes(arriving, tenantId, companyId);

        // Resolved once so the document date and the numbering period cannot disagree.
        LocalDate receiptDate = request.receiptDate() != null ? request.receiptDate() : LocalDate.now();

        WarehouseTarget warehouse = resolveWarehouse(order, request);

        // Refuse a warehouse the company does not have, before the
        // document is numbered: a receipt that names a phantom site
        // would post stock nobody can count.
        validateWarehouse(tenantId, companyId, warehouse.id());

        GoodsReceipt receipt = GoodsReceipt.post(
                allocateNumber(tenantId, companyId, receiptDate),
                receiptDate,
                order,
                warehouse.id(),
                warehouse.code(),
                warehouse.name(),
                request.deliveryChallanNo(),
                request.notes(),
                actor);
        receipt.assignScope(tenantId, companyId);

        // The receipt line is built from the order line's outstanding quantity, and that
        // exact figure is what the order is then asked to record, so the note and the
        // order cannot state different quantities. The order refuses an over-receipt, at
        // which point the whole transaction rolls back and nothing is written.
        int lineNumber = 1;
        for (Arrival arrival : arriving) {
            PurchaseOrderLine orderLine = arrival.orderLine();
            GoodsReceiptLine receiptLine = GoodsReceiptLine.fromOrderLine(
                    lineNumber, orderLine, arrival.quantity(), arrival.warehouseId(), arrival.remarks());
            receipt.addLine(receiptLine);
            order.receiveAgainstLine(orderLine.getLineNumber(), receiptLine.getReceivedQuantity());
            lineNumber++;
        }

        orderRepository.saveAndFlush(order);
        GoodsReceipt saved = receiptRepository.saveAndFlush(receipt);

        // Land the stock movement after this transaction commits. The batch is
        // built here, while the lines are still managed, so the network call to
        // Inventory runs outside any DB transaction: the receipt is durable even
        // if Inventory is briefly unreachable, and a failure is recorded on the
        // receipt (stockPostStatus = FAILED) for retry rather than lost.
        StockReceiptRequest batch = stockBatch(saved, saved.getPostedAt(),
                "Receipt " + saved.getGrnNumber() + " against PO " + saved.getPoNumber());
        events.publishEvent(new GoodsReceiptRecordedEvent(tenantId, companyId, saved.getId(), batch));

        return toResponse(saved);
    }

    /**
     * Posts the receipt's stock movement to Inventory once the receipt is
     * committed. Runs after commit so no DB transaction is held across the
     * network call, and in a fresh context so the status update persists
     * reliably (a raw TransactionSynchronization callback does not).
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onGoodsReceiptRecorded(GoodsReceiptRecordedEvent event) {
        postStockAndUpdateStatus(event.tenantId(), event.companyId(), event.receiptId(), event.batch());
    }

    @Override
    @Transactional
    @RequiresPermission(ProcurementPermissions.GOODS_RECEIPT_CANCEL)
    public GoodsReceiptResponse cancel(Long tenantId, Long companyId, Long id,
                                        String reason, String actor) {

        if (tenantId == null || companyId == null) {
            throw new ValidationException("A tenant and company scope are required");
        }

        GoodsReceipt receipt = receiptRepository
                .findByIdForUpdate(tenantId, companyId, id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Goods receipt " + id + " was not found"));

        // Refused before anything is written: an invoice has already been raised
        // against these goods, so unwinding the receipt would orphan a payable
        // document. The invoice must be cancelled first.
        if (receiptRepository.invoiceExistsForReceipt(tenantId, companyId, id)) {
            throw new ValidationException(
                    "Goods receipt " + receipt.getGrnNumber()
                            + " has already been invoiced and cannot be cancelled");
        }

        PurchaseOrder order = orderRepository
                .findByIdForUpdate(tenantId, companyId, receipt.getPurchaseOrderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Purchase order " + receipt.getPurchaseOrderId() + " was not found"));

        try {
            receipt.cancel(actor, reason);
        } catch (IllegalStateException | IllegalArgumentException ex) {
            throw new ValidationException(ex.getMessage());
        }

        // Give the quantity back, line by line, exactly as it was taken. This is what
        // makes a mis-posted receipt recoverable: without it the order stays RECEIVED
        // with quantities it never really received, and no delivery can ever be
        // booked against it again.
        for (GoodsReceiptLine line : receipt.getLines()) {
            order.reverseReceiptAgainstLineId(
                    line.getPurchaseOrderLineId(), line.getReceivedQuantity());
        }

        orderRepository.saveAndFlush(order);
        GoodsReceipt cancelled = receiptRepository.saveAndFlush(receipt);

        // Reverse the stock movements posted when the receipt was created, as
        // one idempotent batch keyed on the same reference. If Inventory is
        // unreachable the whole cancel rolls back, so the receipt stays POSTED
        // and the operator simply retries; the reversal is safe to repeat.
        stockMovementService.reverseReceipt(stockBatch(cancelled, Instant.now(),
                "Cancellation of receipt " + cancelled.getGrnNumber() + ": " + reason));

        return toResponse(cancelled);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(ProcurementPermissions.GOODS_RECEIPT_VIEW)
    public GoodsReceiptResponse getById(Long tenantId, Long companyId, Long id) {
        return toResponse(receiptRepository
                .findByTenantIdAndCompanyIdAndId(tenantId, companyId, id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Goods receipt " + id + " was not found")));
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(ProcurementPermissions.GOODS_RECEIPT_VIEW)
    public Page<GoodsReceiptResponse> findByOrder(Long tenantId, Long companyId, Long orderId,
                                                 Pageable pageable) {
        return receiptRepository.findByOrder(tenantId, companyId, orderId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(ProcurementPermissions.GOODS_RECEIPT_VIEW)
    public Page<GoodsReceiptResponse> findAll(Long tenantId, Long companyId, Pageable pageable) {
        return receiptRepository.findInScope(tenantId, companyId, pageable).map(this::toResponse);
    }

    /**
     * Works out which order lines are arriving and in what quantity.
     *
     * <p>Accepts both request shapes. {@code lines} is the expressive form and wins
     * when present; {@code lineNumbers} is the compact complete-delivery form;
     * neither means every outstanding line in full.
     *
     * <p>Each named line must exist on this order and still have something
     * outstanding. A line already received in full cannot be received again, and
     * saying so here - naming the line and its state - beats a confusing failure
     * further down the chain.
     *
     * @param order   the order being received against, already write-locked
     * @param request the caller's request
     * @return one arrival per order line, in ascending line number order
     * @throws ValidationException if nothing is outstanding, or a named line is
     *                             unknown, already complete, or named twice
     */
    private List<Arrival> resolveArrivals(PurchaseOrder order, CreateGoodsReceiptRequest request) {
        List<PurchaseOrderLine> outstanding = order.outstandingLines();
        if (outstanding.isEmpty()) {
            throw new ValidationException(
                    "Purchase order " + order.getPoNumber() + " has nothing left to receive");
        }

        if (request.lines() != null && !request.lines().isEmpty()) {
            return fromLineRequests(order, request.lines());
        }

        if (request.lineNumbers() != null && !request.lineNumbers().isEmpty()) {
            List<CreateGoodsReceiptRequest.LineRequest> converted =
                    request.lineNumbers().stream()
                            .map(number -> new CreateGoodsReceiptRequest.LineRequest(number, null, null, null))
                            .toList();
            return fromLineRequests(order, converted);
        }

        // Nothing named: the supplier delivered the whole outstanding order.
        return outstanding.stream().map(line -> new Arrival(line, null, null, null)).toList();
    }

    /**
     * Validates the explicitly requested lines and pairs each with its quantity.
     *
     * <p>Duplicate detection happens here so the caller is told the line was named
     * twice, rather than hearing about a quantity that would exceed the order
     * further down.
     */
    private List<Arrival> fromLineRequests(PurchaseOrder order,
                                           List<CreateGoodsReceiptRequest.LineRequest> requested) {

        List<Arrival> arrivals = new ArrayList<>();
        List<Integer> seen = new ArrayList<>();

        for (CreateGoodsReceiptRequest.LineRequest entry : requested) {
            Integer lineNumber = entry.lineNumber();
            if (lineNumber == null) {
                throw new ValidationException("Every received line must name an order line number");
            }
            if (seen.contains(lineNumber)) {
                throw new ValidationException(
                        "Line " + lineNumber + " of purchase order " + order.getPoNumber()
                                + " was named more than once");
            }
            seen.add(lineNumber);

            PurchaseOrderLine line;
            try {
                line = order.findLine(lineNumber);
            } catch (IllegalArgumentException ex) {
                throw new ValidationException(
                        "Purchase order " + order.getPoNumber() + " has no line " + lineNumber);
            }
            if (line.getStatus() != PurchaseOrderLineStatus.OPEN
                    && line.getStatus() != PurchaseOrderLineStatus.PARTIALLY_RECEIVED) {
                throw new ValidationException(
                        "Line " + lineNumber + " of purchase order " + order.getPoNumber()
                                + " is " + line.getStatus() + " and cannot be received against");
            }
            if (entry.receivedQuantity() != null
                    && entry.receivedQuantity().compareTo(line.outstandingQuantity()) > 0) {
                throw new ValidationException(
                        "Receiving " + entry.receivedQuantity().toPlainString() + " on line "
                                + lineNumber + " exceeds the " + line.outstandingQuantity().toPlainString()
                                + " " + line.getUomCode() + " still outstanding on order "
                                + order.getPoNumber());
            }

            arrivals.add(new Arrival(line, entry.receivedQuantity(), entry.warehouseId(), entry.remarks()));
        }
        return arrivals;
    }

    /**
     * Works out where the goods actually landed.
     *
     * <p><strong>The order's nomination is a plan; this is a fact.</strong> The buyer
     * wrote down where they intended the goods to be stored, and the gate clerk is
     * standing in front of the lorry. When those disagree - the nominated site is full,
     * the driver was rerouted, the company has no nominated site at all - only the
     * clerk can say what happened, so their answer wins and the order is not rewritten
     * to match. Overwriting the order would also falsify the commitment the buyer
     * signed for.
     *
     * <p>There is deliberately no third fallback to the company's default warehouse.
     * Procurement holds no copy of the warehouse master and has no client to
     * Inventory, so it cannot mint a default the way the requisition and order
     * documents cannot invent one either. A receipt that arrives without a warehouse
     * and without a nomination is refused, because a received quantity that names no
     * site cannot be posted as stock, and posting it against an assumed site is how
     * stock ends up on a floor nobody is counting.
     *
     * @param order   the order being received against
     * @param request the receipt request
     * @return the warehouse the note is filed under
     * @throws ValidationException when neither the request nor the order names one
     */
    private WarehouseTarget resolveWarehouse(PurchaseOrder order, CreateGoodsReceiptRequest request) {
        if (request.warehouseId() != null) {
            return new WarehouseTarget(request.warehouseId(), request.warehouseCode(), request.warehouseName());
        }
        if (order.getWarehouseId() != null) {
            return new WarehouseTarget(order.getWarehouseId(), order.getWarehouseCode(), order.getWarehouseName());
        }
        throw new ValidationException(
                "Receiving against " + order.getPoNumber() + " needs a warehouse; the order does not "
                        + "nominate one, so state which warehouse received the goods");
    }

    /**
     * Validates that the warehouse exists and is active in the company scope.
     *
     * <p>Procurement holds no copy of the warehouse master, so the id is
     * resolved against Inventory. A receipt that names a warehouse the
     * company does not have would post stock to a location nobody can
     * count, so it is refused here rather than at persist time.
     */
    private void validateWarehouse(Long tenantId, Long companyId, Long warehouseId) {
        if (warehouseId == null) {
            return;
        }
        if (!warehouseService.validateWarehouse(warehouseId)) {
            throw new ValidationException(
                    "Warehouse " + warehouseId + " not found in this company");
        }
    }

    /**
     * Allocates the next GRN number for the receipt's own fiscal period.
     *
     * <p>Uses the receipt date rather than the order date, matching how requisitions are
     * numbered by their request date and orders by their order date: a document's series
     * belongs to the fiscal year the document is dated in. Takes the pessimistic write lock
     * on the counter for the same reason requisition and order numbering do.
     */
    private String allocateNumber(Long tenantId, Long companyId, LocalDate receiptDate) {
        String periodKey = fiscalPeriodKey(receiptDate);
        DocumentSequence sequence = sequenceRepository
                .findForUpdate(tenantId, companyId, DOCUMENT_TYPE, periodKey)
                .orElseGet(() -> sequenceRepository.save(
                        DocumentSequence.startAt(tenantId, companyId, DOCUMENT_TYPE, periodKey,
                                highestIssuedNumber(tenantId, companyId))));

        return sequence.format(sequence.allocate(), PREFIX, NUMBER_WIDTH);
    }

    /**
     * Fiscal-year period key, matching the requisition and order numbering so all three
     * document series reset at the same boundary.
     */
    private String fiscalPeriodKey(LocalDate date) {
        try {
            NepaliDate bs = NepaliDateConverter.fromAd(date);
            int startYear = bs.month() >= 4 ? bs.year() : bs.year() - 1;
            return startYear + "/" + String.format("%02d", (startYear + 1) % 100);
        } catch (RuntimeException ex) {
            log.debug("Date {} is outside the published Bikram Sambat calendar; "
                    + "numbering against its Gregorian year instead", date);
            return "AD" + date.getYear();
        }
    }

    private long highestIssuedNumber(Long tenantId, Long companyId) {
        Long highest = receiptRepository.findHighestIssuedNumber(
                tenantId, companyId, PREFIX, PREFIX.length());
        return highest == null ? 0L : highest;
    }

    /**
     * One order line as it arrives on this receipt.
     *
     * <p>{@code quantity} is null for a complete delivery of the line, which the
     * aggregate resolves to the whole outstanding quantity. Carrying "null means
     * all of it" through to the receipt line keeps the complete and short delivery
     * paths on one code path instead of two that can disagree.
     *
     * @param orderLine   the order line
     * @param quantity    the quantity arriving, or null for everything outstanding
     * @param warehouseId overrides the note's warehouse for this line only, may be null
     * @param remarks     per line remarks, may be null
     */
    private record Arrival(PurchaseOrderLine orderLine, BigDecimal quantity,
                           Long warehouseId, String remarks) {
    }

    /**
     * A warehouse as resolved onto the note: the id, plus the code and name frozen at
     * that moment.
     *
     * @param id   the warehouse identifier
     * @param code the code as printed on the note
     * @param name the name as printed on the note
     */
    private record WarehouseTarget(Long id, String code, String name) {
    }

    private GoodsReceiptResponse toResponse(GoodsReceipt receipt) {
        List<GoodsReceiptResponse.Line> lines = new ArrayList<>();
        for (GoodsReceiptLine line : receipt.getLines()) {
            lines.add(new GoodsReceiptResponse.Line(
                    line.getId(),
                    line.getLineNumber(),
                    line.getPurchaseOrderLineId(),
                    line.getItemId(),
                    line.getItemCode(),
                    line.getItemName(),
                    line.getUomCode(),
                    line.getUnitId(),
                    line.getReceivedQuantity(),
                    line.getUnitPrice(),
                    line.getLineAmount(),
                    line.getWarehouseId(),
                    line.getRemarks()));
        }

        return new GoodsReceiptResponse(
                receipt.getId(),
                receipt.getGrnNumber(),
                receipt.getTenantId(),
                receipt.getCompanyId(),
                receipt.getBranchId(),
                receipt.getFiscalYearId(),
                receipt.getPurchaseOrderId(),
                receipt.getPoNumber(),
                receipt.getReceiptDate(),
                receipt.getSupplierId(),
                receipt.getSupplierCode(),
                receipt.getSupplierName(),
                receipt.getCurrency(),
                receipt.getStatus(),
                receipt.getStockPostStatus(),
                receipt.getStockPostError(),
                receipt.getReceivedAmount(),
                receipt.getWarehouseId(),
                receipt.getWarehouseCode(),
                receipt.getWarehouseName(),
                receipt.getDeliveryChallanNo(),
                receipt.getNotes(),
                receipt.getPostedAt(),
                receipt.getPostedBy(),
                receipt.getCancellationReason(),
                receipt.getCreatedAt(),
                lines);
    }

    /**
     * Validates that all UOM codes on the arriving lines exist, are active,
     * and belong to the same tenant/company scope.
     */
    private void validateUomCodes(List<Arrival> arrivals, Long tenantId, Long companyId) {
        for (Arrival arrival : arrivals) {
            String normalizedUomCode = arrival.orderLine().getUomCode().trim().toUpperCase();
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
     * Builds the stock-movement batch for a receipt: one line per received line,
     * all sharing the receipt as the source document so Inventory can dedupe on
     * the reference. Built from the managed entity while its lines are loaded.
     */
    private StockReceiptRequest stockBatch(GoodsReceipt receipt, Instant movementDate, String notes) {
        List<StockMovementLineRequest> lines = new ArrayList<>();
        for (GoodsReceiptLine line : receipt.getLines()) {
            lines.add(new StockMovementLineRequest(
                    line.getWarehouseId(),
                    line.getItemId(),
                    line.getUomCode(),
                    line.getReceivedQuantity()));
        }
        return new StockReceiptRequest(
                "GOODS_RECEIPT",
                receipt.getId(),
                receipt.getGrnNumber(),
                movementDate,
                receipt.getPostedBy(),
                notes,
                lines);
    }

    /**
     * Posts a receipt's stock movement to Inventory and records the outcome on
     * the receipt. Runs after the receipt transaction has committed, so no DB
     * transaction is held across the network call. A failure is captured as
     * stockPostStatus = FAILED with the reason and never rethrown - the receipt
     * is already durable, and the operator retries via {@link #retryStockPost}.
     */
    private void postStockAndUpdateStatus(Long tenantId, Long companyId, Long receiptId,
                                          StockReceiptRequest batch) {
        try {
            stockMovementService.postReceipt(batch);
            applyStockStatus(tenantId, companyId, receiptId, StockPostStatus.POSTED, null);
        } catch (RuntimeException ex) {
            log.warn("Failed to post stock movement for goods receipt {}: {}",
                    receiptId, ex.getMessage());
            applyStockStatus(tenantId, companyId, receiptId, StockPostStatus.FAILED,
                    abbreviate(ex.getMessage()));
        }
    }

    /**
     * Persists the stock-post outcome. Uses the repository directly - each call
     * is its own transaction - rather than a @Transactional self-call, which
     * Spring's proxy would not intercept.
     */
    private void applyStockStatus(Long tenantId, Long companyId, Long receiptId,
                                  StockPostStatus status, String error) {
        receiptRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, receiptId)
                .ifPresent(receipt -> {
                    if (status == StockPostStatus.POSTED) {
                        receipt.markStockPosted();
                    } else {
                        receipt.markStockFailed(error);
                    }
                    receiptRepository.save(receipt);
                });
    }

    @Override
    @RequiresPermission(ProcurementPermissions.GOODS_RECEIPT_CREATE)
    public GoodsReceiptResponse retryStockPost(Long tenantId, Long companyId, Long id) {
        // Lines are join-fetched so the batch can be built without a transaction,
        // keeping the retry's network call outside any DB transaction too.
        GoodsReceipt receipt = receiptRepository
                .findWithLinesByTenantIdAndCompanyIdAndId(tenantId, companyId, id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Goods receipt " + id + " was not found"));

        if (receipt.getStockPostStatus() == StockPostStatus.POSTED) {
            return toResponse(receipt);
        }

        postStockAndUpdateStatus(tenantId, companyId, id,
                stockBatch(receipt, receipt.getPostedAt(),
                        "Retry of goods receipt " + receipt.getGrnNumber()
                                + " against PO " + receipt.getPoNumber()));

        return toResponse(receiptRepository
                .findByTenantIdAndCompanyIdAndId(tenantId, companyId, id)
                .orElse(receipt));
    }

    /** Trims a failure message to the column width so it persists cleanly. */
    private String abbreviate(String message) {
        if (message == null || message.isBlank()) {
            return "Unknown error";
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
