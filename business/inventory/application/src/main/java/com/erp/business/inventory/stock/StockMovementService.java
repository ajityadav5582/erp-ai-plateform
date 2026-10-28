package com.erp.business.inventory.stock;

import com.erp.business.inventory.domain.StockMovement;
import com.erp.business.inventory.domain.StockMovementType;
import com.erp.business.inventory.interfaces.stock.StockReceiptRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Posts stock movements and maintains the denormalized balance.
 *
 * <p>Every method takes the tenant and company explicitly rather than
 * reading them from a thread-local, so the scope is a visible, testable
 * parameter of every call instead of hidden state.
 */
public interface StockMovementService {

    /**
     * Posts an inbound receipt against a warehouse.
     *
     * @param tenantId        the tenant scope
     * @param companyId       the company scope
     * @param warehouseId     the warehouse receiving the goods
     * @param itemId          the item received
     * @param unitId          the unit of measure
     * @param quantity        the quantity received
     * @param referenceType   the source document type, e.g. "GOODS_RECEIPT"
     * @param referenceId     the source document id
     * @param referenceNumber the source document number
     * @param movementDate    when the goods physically arrived
     * @param postedBy        the actor posting the movement
     * @param notes           optional notes
     * @return the persisted movement
     */
    StockMovement postReceipt(Long tenantId, Long companyId,
                              Long warehouseId, Long itemId, Long unitId,
                              BigDecimal quantity,
                              String referenceType, Long referenceId, String referenceNumber,
                              Instant movementDate, String postedBy, String notes);

    /**
     * Posts an inbound batch for one source document, idempotently.
     *
     * <p>If movements of type IN already exist for the request's
     * {@code (referenceType, referenceId)}, the batch is recognised as already
     * posted and the existing rows are returned unchanged - no duplicates. The
     * whole batch is written in the caller's transaction, so a partial failure
     * leaves nothing behind and a retry is safe.
     *
     * @return the movements for the reference and whether they already existed
     */
    StockMovementBatchResult postReceiptBatch(Long tenantId, Long companyId, StockReceiptRequest request);

    /**
     * Posts an outbound batch that reverses a prior receipt, idempotently, keyed
     * on OUT movements for the same reference.
     *
     * @return the reversal movements for the reference and whether they already existed
     */
    StockMovementBatchResult reverseReceiptBatch(Long tenantId, Long companyId, StockReceiptRequest request);

    /**
     * Posts an outbound batch (goods leaving for a customer) for one source
     * document, idempotently, keyed on OUT movements for the reference. Used by
     * the sales module to land a delivery in the stock ledger.
     *
     * @return the issue movements for the reference and whether they already existed
     */
    StockMovementBatchResult postIssueBatch(Long tenantId, Long companyId, StockReceiptRequest request);

    /**
     * Posts an inbound batch (IN) that reverses a prior issue (a voided delivery),
     * idempotently, keyed on IN movements for the same reference.
     *
     * @return the restock movements for the reference and whether they already existed
     */
    StockMovementBatchResult reverseIssueBatch(Long tenantId, Long companyId, StockReceiptRequest request);

    /**
     * Posts a signed adjustment batch (count variance, damage, write-off),
     * idempotently, keyed on ADJUST movements for the reference. Line quantities
     * are signed: positive adds, negative removes.
     *
     * @return the adjustment movements for the reference and whether they already existed
     */
    StockMovementBatchResult postAdjustmentBatch(Long tenantId, Long companyId, StockReceiptRequest request);

    /**
     * Posts a RESERVE batch (soft allocation), idempotently, keyed on RESERVE
     * movements for the reference.
     */
    StockMovementBatchResult reserveBatch(Long tenantId, Long companyId, StockReceiptRequest request);

    /**
     * Posts a RELEASE batch (frees a reservation), idempotently, keyed on RELEASE
     * movements for the reference.
     */
    StockMovementBatchResult releaseBatch(Long tenantId, Long companyId, StockReceiptRequest request);

    /**
     * Reverses a previously posted receipt.
     *
     * <p>Creates an outbound movement with the same reference so the ledger
     * shows both the original receipt and its reversal. Does not delete the
     * original movement - the ledger is append-only.
     *
     * @param tenantId        the tenant scope
     * @param companyId       the company scope
     * @param warehouseId     the warehouse the goods left
     * @param itemId          the item
     * @param unitId          the unit of measure
     * @param quantity        the quantity to reverse
     * @param referenceType   the source document type
     * @param referenceId     the source document id
     * @param referenceNumber the source document number
     * @param movementDate    when the reversal is posted
     * @param postedBy        the actor posting the reversal
     * @param notes           optional notes
     * @return the persisted reversal movement
     */
    StockMovement reverseReceipt(Long tenantId, Long companyId,
                                 Long warehouseId, Long itemId, Long unitId,
                                 BigDecimal quantity,
                                 String referenceType, Long referenceId, String referenceNumber,
                                 Instant movementDate, String postedBy, String notes);

    /**
     * Posts an outbound movement (issue, consumption, transfer out).
     */
    StockMovement postIssue(Long tenantId, Long companyId,
                            Long warehouseId, Long itemId, Long unitId,
                            BigDecimal quantity,
                            String referenceType, Long referenceId, String referenceNumber,
                            Instant movementDate, String postedBy, String notes);

    /**
     * Posts a stock adjustment (count variance, damage, write-off).
     *
     * @param quantity signed: positive increases stock, negative decreases it
     */
    StockMovement postAdjustment(Long tenantId, Long companyId,
                                 Long warehouseId, Long itemId, Long unitId,
                                 BigDecimal quantity,
                                 String referenceType, Long referenceId, String referenceNumber,
                                 Instant movementDate, String postedBy, String notes);

    /**
     * Reserves stock against a demand (e.g. a sales order or production order).
     * Decreases available quantity without touching on-hand.
     */
    StockMovement reserve(Long tenantId, Long companyId,
                          Long warehouseId, Long itemId, Long unitId,
                          BigDecimal quantity,
                          String referenceType, Long referenceId, String referenceNumber,
                          Instant movementDate, String postedBy, String notes);

    /**
     * Releases a reservation. Increases available quantity without touching on-hand.
     */
    StockMovement release(Long tenantId, Long companyId,
                          Long warehouseId, Long itemId, Long unitId,
                          BigDecimal quantity,
                          String referenceType, Long referenceId, String referenceNumber,
                          Instant movementDate, String postedBy, String notes);

    /**
     * Lists movements for a warehouse, newest first.
     */
    Page<StockMovement> listByWarehouse(Long tenantId, Long companyId,
                                        Long warehouseId, Pageable pageable);

    /**
     * Lists movements for an item across all warehouses.
     */
    Page<StockMovement> listByItem(Long tenantId, Long companyId,
                                   Long itemId, Pageable pageable);

    /**
     * Lists movements for an item in a warehouse.
     */
    Page<StockMovement> listByWarehouseAndItem(Long tenantId, Long companyId,
                                               Long warehouseId, Long itemId, Pageable pageable);

    /**
     * Finds all movements posted against a source document.
     */
    List<StockMovement> findByReference(String referenceType, Long referenceId);

    /**
     * Current on-hand quantity for an item in a warehouse, in the given unit.
     */
    BigDecimal onHand(Long tenantId, Long companyId,
                      Long warehouseId, Long itemId, Long unitId);

    /**
     * Current available (on-hand minus reserved) quantity.
     */
    BigDecimal available(Long tenantId, Long companyId,
                         Long warehouseId, Long itemId, Long unitId);
}
