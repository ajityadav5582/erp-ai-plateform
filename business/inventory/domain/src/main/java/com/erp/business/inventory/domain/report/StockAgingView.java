package com.erp.business.inventory.domain.report;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Read model for the stock aging report: one row per (warehouse, item, unit)
 * holding stock, with how long it has sat since the last movement.
 *
 * <p>Stock that has not moved is capital doing no work. The buckets turn that
 * into something a buyer can act on, from fresh stock through to dead stock
 * that should probably be written down or cleared.
 */
public record StockAgingView(
        Long itemId,
        String sku,
        String itemName,
        Long warehouseId,
        String warehouseCode,
        String warehouseName,
        Long unitId,
        String unitCode,
        BigDecimal onHand,
        BigDecimal reserved,
        BigDecimal available,
        BigDecimal unitCost,
        BigDecimal onHandValue,
        Instant lastMovementAt,
        Long daysSinceMovement,
        String ageBucket
) {

    /** No movement has ever been recorded against this balance. */
    public static final String BUCKET_NEVER_MOVED = "NEVER_MOVED";

    public static final String BUCKET_FRESH = "FRESH";

    public static final String BUCKET_AGING = "AGING";

    public static final String BUCKET_SLOW = "SLOW";

    public static final String BUCKET_STAGNANT = "STAGNANT";

    public static final String BUCKET_DEAD = "DEAD";

    /** Upper bound in days of each bucket, keyed by bucket name. */
    public static long bucketLimitDays(String bucket) {
        return switch (bucket) {
            case BUCKET_FRESH -> 30;
            case BUCKET_AGING -> 90;
            case BUCKET_SLOW -> 180;
            case BUCKET_STAGNANT -> 365;
            default -> Long.MAX_VALUE;
        };
    }
}
