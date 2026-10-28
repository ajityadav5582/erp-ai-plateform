package com.erp.business.inventory.stock;

import com.erp.business.inventory.domain.Item;
import com.erp.business.inventory.domain.StockBalance;
import com.erp.business.inventory.domain.StockMovement;
import com.erp.business.inventory.domain.StockMovementType;
import com.erp.business.inventory.domain.Unit;
import com.erp.business.inventory.domain.Warehouse;
import com.erp.business.inventory.infrastructure.persistence.ItemRepository;
import com.erp.business.inventory.infrastructure.persistence.StockBalanceRepository;
import com.erp.business.inventory.infrastructure.persistence.StockMovementRepository;
import com.erp.business.inventory.infrastructure.persistence.UnitRepository;
import com.erp.business.inventory.infrastructure.persistence.WarehouseRepository;
import com.erp.business.inventory.interfaces.stock.StockMovementLineRequest;
import com.erp.business.inventory.interfaces.stock.StockReceiptRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Default {@link StockMovementService} implementation.
 *
 * <p>Posts an immutable ledger entry and updates the denormalized balance
 * in the same transaction. The balance is a cache of the ledger, not a
 * second source of truth: if they ever disagree, the ledger wins and the
 * balance can be rebuilt by replaying movements.
 *
 * <p>Reads run {@code readOnly = true} so Hibernate can skip dirty checking
 * and flushes on the query paths.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StockMovementServiceImpl implements StockMovementService {

    private final StockMovementRepository movementRepository;
    private final StockBalanceRepository balanceRepository;
    private final WarehouseRepository warehouseRepository;
    private final ItemRepository itemRepository;
    private final UnitRepository unitRepository;

    @Override
    @Transactional
    public StockMovement postReceipt(Long tenantId, Long companyId,
                                     Long warehouseId, Long itemId, Long unitId,
                                     BigDecimal quantity,
                                     String referenceType, Long referenceId, String referenceNumber,
                                     Instant movementDate, String postedBy, String notes) {
        return post(tenantId, companyId, StockMovementType.IN,
                warehouseId, itemId, unitId, quantity,
                referenceType, referenceId, referenceNumber,
                movementDate, postedBy, notes);
    }

    @Override
    @Transactional
    public StockMovement reverseReceipt(Long tenantId, Long companyId,
                                        Long warehouseId, Long itemId, Long unitId,
                                        BigDecimal quantity,
                                        String referenceType, Long referenceId, String referenceNumber,
                                        Instant movementDate, String postedBy, String notes) {
        // A reversal is an outbound movement against the same reference, so
        // the ledger shows both the receipt and its undo. The original row
        // is never deleted - the ledger is append-only.
        return post(tenantId, companyId, StockMovementType.OUT,
                warehouseId, itemId, unitId, quantity,
                referenceType, referenceId, referenceNumber,
                movementDate, postedBy, notes);
    }

    @Override
    @Transactional
    public StockMovementBatchResult postReceiptBatch(Long tenantId, Long companyId, StockReceiptRequest request) {
        return postBatch(tenantId, companyId, StockMovementType.IN, request);
    }

    @Override
    @Transactional
    public StockMovementBatchResult reverseReceiptBatch(Long tenantId, Long companyId, StockReceiptRequest request) {
        return postBatch(tenantId, companyId, StockMovementType.OUT, request);
    }

    @Override
    @Transactional
    public StockMovementBatchResult postIssueBatch(Long tenantId, Long companyId, StockReceiptRequest request) {
        // Goods leaving for a customer. An outbound (OUT) batch keyed on the
        // delivery reference, so it is idempotent by (referenceType, referenceId)
        // exactly as a receipt is, and never collides with a receipt's rows.
        return postBatch(tenantId, companyId, StockMovementType.OUT, request);
    }

    @Override
    @Transactional
    public StockMovementBatchResult reverseIssueBatch(Long tenantId, Long companyId, StockReceiptRequest request) {
        // A voided delivery: an inbound batch (IN) that reverses a prior issue (OUT),
        // sharing the same reference so the ledger shows both the issue and its undo.
        // It must be IN, not OUT: an OUT here would collide with the original issue's
        // (reference, OUT) idempotency key and be skipped, so the void would never
        // restock. IN restocks and stays idempotent by (reference, IN).
        return postBatch(tenantId, companyId, StockMovementType.IN, request);
    }

    @Override
    @Transactional
    public StockMovementBatchResult postAdjustmentBatch(Long tenantId, Long companyId, StockReceiptRequest request) {
        // Signed adjustment batch (count variance, damage, write-off). Line
        // quantities are signed; idempotent by (reference, ADJUST).
        return postBatch(tenantId, companyId, StockMovementType.ADJUST, request);
    }

    @Override
    @Transactional
    public StockMovementBatchResult reserveBatch(Long tenantId, Long companyId, StockReceiptRequest request) {
        return postBatch(tenantId, companyId, StockMovementType.RESERVE, request);
    }

    @Override
    @Transactional
    public StockMovementBatchResult releaseBatch(Long tenantId, Long companyId, StockReceiptRequest request) {
        return postBatch(tenantId, companyId, StockMovementType.RELEASE, request);
    }

    /**
     * The single idempotent write path for a batch.
     *
     * <p>If rows of the given type already exist for the request's
     * {@code (referenceType, referenceId)} the batch is treated as already
     * applied and returned unchanged - this is what makes a caller's retry
     * after a timeout or crash safe. Otherwise every line is posted inside this
     * transaction, so a partial failure leaves nothing behind.
     */
    private StockMovementBatchResult postBatch(Long tenantId, Long companyId,
                                          StockMovementType type, StockReceiptRequest request) {
        List<StockMovement> existing = movementRepository
                .findByTenantIdAndCompanyIdAndReferenceTypeAndReferenceIdAndMovementType(
                        tenantId, companyId, request.referenceType(), request.referenceId(), type);
        if (!existing.isEmpty()) {
            log.debug("Batch {} for {} {} already applied ({} rows); returning existing",
                    type, request.referenceType(), request.referenceId(), existing.size());
            return new StockMovementBatchResult(existing, true);
        }

        Instant when = request.movementDate() != null ? request.movementDate() : Instant.now();
        List<StockMovement> posted = new ArrayList<>();
        for (StockMovementLineRequest line : request.lines()) {
            Long unitId = resolveUnitId(tenantId, companyId, line.uomCode());
            posted.add(post(tenantId, companyId, type,
                    line.warehouseId(), line.itemId(), unitId, line.quantity(),
                    request.referenceType(), request.referenceId(), request.referenceNumber(),
                    when, request.postedBy(), request.notes()));
        }
        return new StockMovementBatchResult(posted, false);
    }

    /**
     * Resolves a unit-of-measure code to the unit id within the caller's
     * tenant/company. The caller sends the code, not an id, because it does not
     * hold Inventory's unit master and an id from another scope would be
     * rejected here anyway.
     */
    private Long resolveUnitId(Long tenantId, Long companyId, String uomCode) {
        return unitRepository.findByTenantIdAndCompanyIdAndCode(tenantId, companyId, uomCode)
                .map(Unit::getId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unit '" + uomCode + "' not found in this company"));
    }

    @Override
    @Transactional
    public StockMovement postIssue(Long tenantId, Long companyId,
                                   Long warehouseId, Long itemId, Long unitId,
                                   BigDecimal quantity,
                                   String referenceType, Long referenceId, String referenceNumber,
                                   Instant movementDate, String postedBy, String notes) {
        return post(tenantId, companyId, StockMovementType.OUT,
                warehouseId, itemId, unitId, quantity,
                referenceType, referenceId, referenceNumber,
                movementDate, postedBy, notes);
    }

    @Override
    @Transactional
    public StockMovement postAdjustment(Long tenantId, Long companyId,
                                        Long warehouseId, Long itemId, Long unitId,
                                        BigDecimal quantity,
                                        String referenceType, Long referenceId, String referenceNumber,
                                        Instant movementDate, String postedBy, String notes) {
        return post(tenantId, companyId, StockMovementType.ADJUST,
                warehouseId, itemId, unitId, quantity,
                referenceType, referenceId, referenceNumber,
                movementDate, postedBy, notes);
    }

    @Override
    @Transactional
    public StockMovement reserve(Long tenantId, Long companyId,
                                 Long warehouseId, Long itemId, Long unitId,
                                 BigDecimal quantity,
                                 String referenceType, Long referenceId, String referenceNumber,
                                 Instant movementDate, String postedBy, String notes) {
        return post(tenantId, companyId, StockMovementType.RESERVE,
                warehouseId, itemId, unitId, quantity,
                referenceType, referenceId, referenceNumber,
                movementDate, postedBy, notes);
    }

    @Override
    @Transactional
    public StockMovement release(Long tenantId, Long companyId,
                                 Long warehouseId, Long itemId, Long unitId,
                                 BigDecimal quantity,
                                 String referenceType, Long referenceId, String referenceNumber,
                                 Instant movementDate, String postedBy, String notes) {
        return post(tenantId, companyId, StockMovementType.RELEASE,
                warehouseId, itemId, unitId, quantity,
                referenceType, referenceId, referenceNumber,
                movementDate, postedBy, notes);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockMovement> listByWarehouse(Long tenantId, Long companyId,
                                               Long warehouseId, Pageable pageable) {
        return movementRepository.findByTenantIdAndCompanyIdAndWarehouseId(
                tenantId, companyId, warehouseId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockMovement> listByItem(Long tenantId, Long companyId,
                                          Long itemId, Pageable pageable) {
        return movementRepository.findByTenantIdAndCompanyIdAndItemId(
                tenantId, companyId, itemId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockMovement> listByWarehouseAndItem(Long tenantId, Long companyId,
                                                      Long warehouseId, Long itemId, Pageable pageable) {
        return movementRepository.findByTenantIdAndCompanyIdAndWarehouseIdAndItemId(
                tenantId, companyId, warehouseId, itemId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockMovement> findByReference(String referenceType, Long referenceId) {
        return movementRepository.findByReferenceTypeAndReferenceId(referenceType, referenceId);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal onHand(Long tenantId, Long companyId,
                             Long warehouseId, Long itemId, Long unitId) {
        return balanceRepository
                .findByTenantIdAndCompanyIdAndWarehouseIdAndItemIdAndUnitId(
                        tenantId, companyId, warehouseId, itemId, unitId)
                .map(StockBalance::getOnHandQty)
                .orElse(BigDecimal.ZERO);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal available(Long tenantId, Long companyId,
                                Long warehouseId, Long itemId, Long unitId) {
        return balanceRepository
                .findByTenantIdAndCompanyIdAndWarehouseIdAndItemIdAndUnitId(
                        tenantId, companyId, warehouseId, itemId, unitId)
                .map(StockBalance::getAvailableQty)
                .orElse(BigDecimal.ZERO);
    }

    /**
     * The single write path for every movement type.
     *
     * <p>Validates the warehouse, item and unit exist in scope, inserts the
     * ledger row, then applies it to the balance. Both writes happen in the
     * caller's transaction, so a failure rolls back the ledger and the
     * balance together.
     */
    private StockMovement post(Long tenantId, Long companyId, StockMovementType type,
                               Long warehouseId, Long itemId, Long unitId,
                               BigDecimal quantity,
                               String referenceType, Long referenceId, String referenceNumber,
                               Instant movementDate, String postedBy, String notes) {

        if (quantity == null || quantity.signum() == 0) {
            throw new IllegalArgumentException("Movement quantity must be non-zero");
        }
        if (type != StockMovementType.ADJUST && quantity.signum() < 0) {
            throw new IllegalArgumentException("Only ADJUST movements may carry a negative quantity");
        }

        Warehouse warehouse = warehouseRepository
                .findByTenantIdAndCompanyIdAndId(tenantId, companyId, warehouseId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Warehouse " + warehouseId + " not found in this company"));

        Item item = itemRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, itemId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Item " + itemId + " not found in this company"));

        Unit unit = unitRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, unitId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unit " + unitId + " not found in this company"));

        StockMovement movement = StockMovement.create(
                tenantId, companyId, type, warehouseId, itemId, unitId, quantity,
                referenceType, referenceId, referenceNumber,
                movementDate != null ? movementDate : Instant.now(),
                postedBy, notes);

        StockMovement saved = movementRepository.save(movement);
        applyToBalance(tenantId, companyId, warehouseId, itemId, unitId, saved,
                Boolean.TRUE.equals(warehouse.getAllowNegativeStock()),
                item.getSku(), warehouse.getCode());

        log.debug("Posted {} movement {} for item {} in warehouse {}: {}",
                type, saved.getId(), itemId, warehouseId, saved.getSignedQuantity());
        return saved;
    }

    /**
     * Applies a movement to the denormalized balance, creating the balance
     * row on first touch.
     *
     * <p>An outbound (OUT) movement is refused when it would take on-hand below
     * zero, unless the warehouse explicitly allows negative stock. The check runs
     * against the same balance row that is about to be updated, inside the
     * caller's transaction, so a rejected issue leaves neither a ledger row nor a
     * balance change behind.
     */
    private void applyToBalance(Long tenantId, Long companyId,
                                Long warehouseId, Long itemId, Long unitId,
                                StockMovement movement, boolean allowNegativeStock,
                                String itemSku, String warehouseCode) {
        StockBalance balance = balanceRepository
                .findByTenantIdAndCompanyIdAndWarehouseIdAndItemIdAndUnitId(
                        tenantId, companyId, warehouseId, itemId, unitId)
                .orElseGet(() -> balanceRepository.save(
                        StockBalance.create(tenantId, companyId, warehouseId, itemId, unitId)));

        boolean reducesOnHand = movement.getMovementType() == StockMovementType.OUT
                || (movement.getMovementType() == StockMovementType.ADJUST
                    && movement.getSignedQuantity().signum() < 0);
        if (reducesOnHand && !allowNegativeStock) {
            BigDecimal resultingOnHand = balance.getOnHandQty().add(movement.getSignedQuantity());
            if (resultingOnHand.signum() < 0) {
                throw new InsufficientStockException(
                        itemSku, warehouseCode, movement.getQuantity().abs(), balance.getOnHandQty());
            }
        }

        if (movement.getMovementType() == StockMovementType.RESERVE && !allowNegativeStock) {
            BigDecimal resultingAvailable = balance.getAvailableQty().subtract(movement.getQuantity());
            if (resultingAvailable.signum() < 0) {
                throw new InsufficientStockException(
                        itemSku, warehouseCode, movement.getQuantity(), balance.getAvailableQty());
            }
        }

        balance.applyMovement(movement);
        balanceRepository.save(balance);
    }
}
