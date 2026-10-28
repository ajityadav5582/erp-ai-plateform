package com.erp.business.inventory.report;

import com.erp.business.inventory.domain.report.LowStockView;
import com.erp.business.inventory.domain.report.StockAgingView;
import com.erp.business.inventory.domain.report.NegativeStockView;
import com.erp.business.inventory.domain.report.StockValuationView;
import com.erp.business.inventory.domain.report.StockOnHandView;
import com.erp.business.inventory.infrastructure.persistence.LowStockProjection;
import com.erp.business.inventory.infrastructure.persistence.NegativeStockProjection;
import com.erp.business.inventory.infrastructure.persistence.StockAgingProjection;
import com.erp.business.inventory.infrastructure.persistence.StockValuationProjection;
import com.erp.business.inventory.infrastructure.persistence.StockValuationTotalsProjection;
import com.erp.business.inventory.infrastructure.persistence.StockBalanceRepository;
import com.erp.business.inventory.infrastructure.persistence.StockLedgerProjection;
import com.erp.business.inventory.infrastructure.persistence.OpeningBalanceProjection;
import com.erp.business.inventory.infrastructure.persistence.StockMovementRepository;
import com.erp.business.inventory.infrastructure.persistence.StockOnHandProjection;
import com.erp.business.inventory.infrastructure.persistence.StockPeriodSummaryProjection;
import com.erp.business.inventory.report.dto.StockCardResponse;
import com.erp.business.inventory.report.dto.StockCardRow;
import com.erp.business.inventory.report.dto.StockMovementSummaryResponse;
import com.erp.business.inventory.report.dto.StockMovementSummaryRow;
import com.erp.business.inventory.report.dto.StockValuationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StockReportServiceImpl implements StockReportService {

    private final StockBalanceRepository balanceRepository;
    private final StockMovementRepository movementRepository;

    public StockReportServiceImpl(StockBalanceRepository balanceRepository,
                                  StockMovementRepository movementRepository) {
        this.balanceRepository = balanceRepository;
        this.movementRepository = movementRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockOnHandView> onHand(Long tenantId, Long companyId,
                                        Long warehouseId, Long itemId, boolean inStockOnly,
                                        Pageable pageable) {
        Page<StockOnHandProjection> page = balanceRepository.findOnHand(
                tenantId, companyId, warehouseId, itemId, inStockOnly, pageable);
        List<StockOnHandView> rows = page.getContent().stream()
                .map(p -> new StockOnHandView(
                        p.getItemId(), p.getSku(), p.getItemName(),
                        p.getWarehouseId(), p.getWarehouseCode(), p.getWarehouseName(),
                        p.getUnitId(), p.getUnitCode(),
                        p.getOnHand(), p.getReserved(), p.getAvailable(), p.getLastMovementAt()))
                .toList();
        return new PageImpl<>(rows, pageable, page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public StockCardResponse stockCard(Long tenantId, Long companyId, Long itemId,
                                       Long warehouseId, String type, Instant from, Instant to) {
        BigDecimal opening = movementRepository.sumOnHandBefore(tenantId, companyId, itemId, warehouseId, from);
        List<StockLedgerProjection> movements = movementRepository.findLedger(
                tenantId, companyId, itemId, warehouseId, type, from, to);

        String sku = null;
        String itemName = null;
        BigDecimal running = opening != null ? opening : BigDecimal.ZERO;
        List<StockCardRow> rows = new ArrayList<>();
        for (StockLedgerProjection m : movements) {
            sku = m.getSku();
            itemName = m.getItemName();
            BigDecimal delta = onHandDelta(m.getMovementType(), m.getQuantity());
            running = running.add(delta);
            rows.add(new StockCardRow(
                    m.getMovementDate(), m.getMovementType(), m.getReferenceType(), m.getReferenceNumber(),
                    m.getQuantity(), delta, running,
                    m.getPostedBy(), m.getNotes(), m.getWarehouseCode(), m.getWarehouseName(), m.getUnitCode()));
        }

        return new StockCardResponse(
                itemId, sku, itemName, from, to,
                opening != null ? opening : BigDecimal.ZERO, running, rows);
    }

    /** On-hand effect of a movement: IN/ADJUST add (ADJUST is signed), OUT removes, RESERVE/RELEASE none. */
    private static BigDecimal onHandDelta(String type, BigDecimal qty) {
        if (qty == null) {
            return BigDecimal.ZERO;
        }
        return switch (type) {
            case "IN", "ADJUST" -> qty;
            case "OUT" -> qty.negate();
            default -> BigDecimal.ZERO;
        };
    }

    @Override
    @Transactional(readOnly = true)
    public StockMovementSummaryResponse summarize(Long tenantId, Long companyId, Long warehouseId,
                                                  Instant from, Instant to) {
        List<StockPeriodSummaryProjection> periodRows =
                movementRepository.summarizePeriod(tenantId, companyId, warehouseId, from, to);
        List<OpeningBalanceProjection> openingRows =
                movementRepository.sumOnHandBeforeGrouped(tenantId, companyId, warehouseId, from);

        Map<String, BigDecimal> openingByKey = new HashMap<>();
        for (OpeningBalanceProjection o : openingRows) {
            openingByKey.put(key(o.getItemId(), o.getWarehouseId(), o.getUnitId()), nz(o.getOnHand()));
        }

        List<StockMovementSummaryRow> rows = new ArrayList<>();
        BigDecimal tOpen = BigDecimal.ZERO, tIn = BigDecimal.ZERO, tOut = BigDecimal.ZERO;
        BigDecimal tAdj = BigDecimal.ZERO, tClose = BigDecimal.ZERO;
        for (StockPeriodSummaryProjection p : periodRows) {
            BigDecimal opening = openingByKey.getOrDefault(
                    key(p.getItemId(), p.getWarehouseId(), p.getUnitId()), BigDecimal.ZERO);
            BigDecimal in = nz(p.getTotalIn());
            BigDecimal out = nz(p.getTotalOut());
            BigDecimal adj = nz(p.getTotalAdjust());
            BigDecimal closing = opening.add(in).subtract(out).add(adj);
            rows.add(new StockMovementSummaryRow(
                    p.getItemId(), p.getSku(), p.getItemName(),
                    p.getWarehouseId(), p.getWarehouseCode(), p.getWarehouseName(),
                    p.getUnitId(), p.getUnitCode(), opening, in, out, adj, closing));
            tOpen = tOpen.add(opening);
            tIn = tIn.add(in);
            tOut = tOut.add(out);
            tAdj = tAdj.add(adj);
            tClose = tClose.add(closing);
        }
        return new StockMovementSummaryResponse(from, to, tOpen, tIn, tOut, tAdj, tClose, rows);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LowStockView> lowStock(Long tenantId, Long companyId, Long warehouseId,
                                       Pageable pageable) {
        Page<LowStockProjection> page =
                balanceRepository.findLowStock(tenantId, companyId, warehouseId, pageable);
        List<LowStockView> rows = page.getContent().stream()
                .map(StockReportServiceImpl::toLowStockView)
                .toList();
        return new PageImpl<>(rows, pageable, page.getTotalElements());
    }

    /**
     * Maps a low-stock row and derives its urgency: the shortfall against the
     * reorder threshold, the status band, and the suggested order quantity.
     *
     * <p>The suggested quantity is the item's own reorder quantity when one is
     * configured, otherwise a top-up back to the threshold — the two policies a
     * buyer actually maintains on the item master.
     */
    private static LowStockView toLowStockView(LowStockProjection p) {
        BigDecimal onHand = nz(p.getOnHand());
        BigDecimal reserved = nz(p.getReserved());
        BigDecimal available = nz(p.getAvailable());
        BigDecimal min = nz(p.getMinStockLevel());
        BigDecimal reorder = nz(p.getReorderLevel());
        BigDecimal reorderQty = nz(p.getReorderQuantity());

        BigDecimal threshold = reorder.max(min);
        BigDecimal shortfall = threshold.subtract(available).max(BigDecimal.ZERO);

        String status = available.signum() <= 0
                ? LowStockView.STATUS_OUT_OF_STOCK
                : available.compareTo(min) <= 0
                        ? LowStockView.STATUS_BELOW_MIN
                        : LowStockView.STATUS_BELOW_REORDER;

        BigDecimal suggested = reorderQty.signum() > 0 ? reorderQty : shortfall;

        return new LowStockView(
                p.getItemId(), p.getSku(), p.getItemName(),
                p.getWarehouseId(), p.getWarehouseCode(), p.getWarehouseName(),
                p.getUnitId(), p.getUnitCode(),
                onHand, reserved, available, min, reorder, reorderQty,
                shortfall, suggested, status, p.getLastMovementAt());
    }

    @Override
    @Transactional(readOnly = true)
    public StockValuationResponse valuation(Long tenantId, Long companyId, Long warehouseId,
                                            Pageable pageable) {
        Page<StockValuationProjection> page =
                balanceRepository.findValuation(tenantId, companyId, warehouseId, pageable);
        StockValuationTotalsProjection totals =
                balanceRepository.sumValuation(tenantId, companyId, warehouseId);

        List<StockValuationView> rows = page.getContent().stream()
                .map(p -> new StockValuationView(
                        p.getItemId(), p.getSku(), p.getItemName(),
                        p.getWarehouseId(), p.getWarehouseCode(), p.getWarehouseName(),
                        p.getUnitId(), p.getUnitCode(),
                        nz(p.getOnHand()), nz(p.getReserved()), nz(p.getAvailable()),
                        nz(p.getUnitCost()), nz(p.getOnHandValue()), nz(p.getReservedValue()),
                        nz(p.getAvailableValue()), p.isCostBasisMissing(), p.getLastMovementAt()))
                .toList();

        return new StockValuationResponse(
                nz(totals.getTotalOnHandValue()),
                nz(totals.getTotalReservedValue()),
                nz(totals.getTotalAvailableValue()),
                totals.getItemsValued() != null ? totals.getItemsValued() : 0L,
                totals.getItemsMissingCost() != null ? totals.getItemsMissingCost() : 0L,
                new PageImpl<>(rows, pageable, page.getTotalElements()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NegativeStockView> negativeStock(Long tenantId, Long companyId, Long warehouseId,
                                                 Pageable pageable) {
        Page<NegativeStockProjection> page =
                balanceRepository.findNegativeStock(tenantId, companyId, warehouseId, pageable);
        List<NegativeStockView> rows = page.getContent().stream()
                .map(p -> new NegativeStockView(
                        p.getItemId(), p.getSku(), p.getItemName(),
                        p.getWarehouseId(), p.getWarehouseCode(), p.getWarehouseName(),
                        p.getUnitId(), p.getUnitCode(),
                        nz(p.getOnHand()), nz(p.getReserved()), nz(p.getAvailable()),
                        p.isNegativeStockAllowed(), p.getLastMovementAt()))
                .toList();
        return new PageImpl<>(rows, pageable, page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockAgingView> aging(Long tenantId, Long companyId, Long warehouseId, long minDays,
                                      Pageable pageable) {
        Page<StockAgingProjection> page =
                balanceRepository.findAging(tenantId, companyId, warehouseId, minDays, pageable);
        List<StockAgingView> rows = page.getContent().stream()
                .map(StockReportServiceImpl::toAgingView)
                .toList();
        return new PageImpl<>(rows, pageable, page.getTotalElements());
    }

    /** Buckets a row by how long it has been idle; a balance that never moved is its own bucket. */
    private static StockAgingView toAgingView(StockAgingProjection p) {
        Long days = p.getDaysSinceMovement();
        String bucket = days == null
                ? StockAgingView.BUCKET_NEVER_MOVED
                : days <= StockAgingView.bucketLimitDays(StockAgingView.BUCKET_FRESH)
                        ? StockAgingView.BUCKET_FRESH
                        : days <= StockAgingView.bucketLimitDays(StockAgingView.BUCKET_AGING)
                                ? StockAgingView.BUCKET_AGING
                                : days <= StockAgingView.bucketLimitDays(StockAgingView.BUCKET_SLOW)
                                        ? StockAgingView.BUCKET_SLOW
                                        : days <= StockAgingView.bucketLimitDays(StockAgingView.BUCKET_STAGNANT)
                                                ? StockAgingView.BUCKET_STAGNANT
                                                : StockAgingView.BUCKET_DEAD;

        return new StockAgingView(
                p.getItemId(), p.getSku(), p.getItemName(),
                p.getWarehouseId(), p.getWarehouseCode(), p.getWarehouseName(),
                p.getUnitId(), p.getUnitCode(),
                nz(p.getOnHand()), nz(p.getReserved()), nz(p.getAvailable()),
                nz(p.getUnitCost()), nz(p.getOnHandValue()),
                p.getLastMovementAt(), days, bucket);
    }

    private static String key(Long itemId, Long warehouseId, Long unitId) {
        return itemId + ":" + warehouseId + ":" + unitId;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
