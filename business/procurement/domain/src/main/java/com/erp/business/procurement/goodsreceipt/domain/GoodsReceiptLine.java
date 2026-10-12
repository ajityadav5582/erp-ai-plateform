package com.erp.business.procurement.goodsreceipt.domain;

import com.erp.business.procurement.purchaseorder.domain.PurchaseOrderLine;
import com.erp.platform.data.entity.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * One line of a goods receipt note: what arrived, against which order line.
 *
 * <p>Item code, name and UOM are copied from the order line rather than referenced
 * live, for the same reason the order line snapshots them: the receipt is a warehouse
 * document that has to keep printing what was actually delivered even after the item is
 * renamed or discontinued.
 *
 * <p>The unit price is copied from the order line too. A receipt does not re-price
 * goods; the supplier invoice is where a price difference is discovered, and that
 * difference is resolved there. Carrying a price here would let a receipt silently
 * restate what the company committed to buy.
 */
@Entity
@Table(
        name = "goods_receipt_lines",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_goods_receipt_lines_receipt_line_number",
                columnNames = {"goods_receipt_id", "line_number"}
        ),
        indexes = {
                @Index(name = "idx_goods_receipt_lines_receipt",
                        columnList = "goods_receipt_id, line_number"),
                @Index(name = "idx_goods_receipt_lines_order_line",
                        columnList = "tenant_id, company_id, purchase_order_line_id"),
                @Index(name = "idx_goods_receipt_lines_item",
                        columnList = "tenant_id, company_id, item_id"),
                // Leads every future stock posting and on-hand lookup: a balance is
                // keyed on warehouse plus item, so this index is what stops that
                // query degrading into a scan once receipts accumulate.
                @Index(name = "idx_goods_receipt_lines_warehouse_item",
                        columnList = "tenant_id, company_id, warehouse_id, item_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class GoodsReceiptLine extends TenantScopedEntity<Long> {

    private static final long serialVersionUID = 1L;

    private static final int MONEY_SCALE = 4;
    private static final int QUANTITY_SCALE = 6;

    @NotNull(message = "Line number is required")
    @Positive(message = "Line number must be positive")
    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    /**
     * The order line this receipt satisfies.
     *
     * <p>An id, not a relation: the order and the receipt are separate aggregates that
     * must stay independently loadable and versionable.
     */
    @Column(name = "purchase_order_line_id", nullable = false)
    private Long purchaseOrderLineId;

    /** Frozen item snapshot, copied from the order line. */
    @NotNull(message = "Item is required")
    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @NotBlank(message = "Item code is required")
    @Column(name = "item_code", nullable = false, length = 100)
    private String itemCode;

    @NotBlank(message = "Item name is required")
    @Column(name = "item_name", nullable = false, length = 255)
    private String itemName;

    @NotBlank(message = "Unit of measure is required")
    @Column(name = "uom_code", nullable = false, length = 32)
    private String uomCode;

    /** Reference to the Unit master data. Not a database foreign key: cross-service. */
    @Column(name = "unit_id")
    private Long unitId;

    @NotNull(message = "Received quantity is required")
    @Column(name = "received_quantity", nullable = false, precision = 19, scale = 6)
    private BigDecimal receivedQuantity;

    @NotNull(message = "Unit price is required")
    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

    /** Net value of this receipt line, computed in Java. */
    @NotNull(message = "Line amount is required")
    @Column(name = "line_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal lineAmount;

    /**
     * The warehouse this line's quantity physically landed in.
     *
     * <p><strong>Why the line carries one when the note already has a
     * warehouse.</strong> A single consignment is routinely split across sites -
     * half the chairs go to the Kathmandu depot, half to Pokhara - and they
     * arrive on one challan under one GRN. Putting the warehouse only on the
     * header would force the clerk to invent a second, fictitious GRN to record
     * the second drop, which breaks the one-note-per-challan trail that
     * {@code deliveryChallanNo} exists to preserve.
     *
     * <p>Set from the owning receipt's warehouse by {@link #resolveWarehouse} when
     * the line is attached, and overridable per line for a genuinely split
     * delivery. Every line ends up non-null, because stock cannot be posted
     * against a line that does not say where the goods went.
     */
    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    /** Owning receipt. Written by the aggregate root only. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "goods_receipt_id", nullable = false, updatable = false)
    @ToString.Exclude
    private GoodsReceipt goodsReceipt;

    void attachTo(GoodsReceipt receipt) {
        this.goodsReceipt = receipt;
    }

    /**
     * Settles this line's warehouse, preferring an explicit per-line override.
     *
     * <p>Called once by the aggregate root as the line is attached, so no caller
     * outside {@link GoodsReceipt} can leave the column unset. Refuses a null
     * result rather than silently posting stock with no location: the whole point
     * of this field is that every received quantity can be attributed to a site.
     *
     * @param noteWarehouseId the warehouse of the receipt this line belongs to
     * @throws IllegalStateException if neither an override nor the note supplies one
     */
    void resolveWarehouse(Long noteWarehouseId) {
        Long resolved = warehouseId != null ? warehouseId : noteWarehouseId;
        if (resolved == null) {
            throw new IllegalStateException(
                    "Goods receipt line " + lineNumber + " has no warehouse to receive into");
        }
        this.warehouseId = resolved;
    }

    /**
     * Creates a receipt line taking the whole outstanding quantity of an order line.
     *
     * <p>The complete-delivery case: the supplier brought everything that was still
     * owed on this line. See
     * {@link #fromOrderLine(int, PurchaseOrderLine, BigDecimal, Long, String)} for the
     * short-delivery case.
     *
     * @param lineNumber the 1-based ordinal within this receipt
     * @param orderLine  the order line being received
     * @param remarks    free text remarks, may be null
     * @return a new unsaved receipt line
     * @throws IllegalArgumentException if the order line has nothing outstanding
     */
    public static GoodsReceiptLine fromOrderLine(int lineNumber, PurchaseOrderLine orderLine,
                                                  String remarks) {
        return fromOrderLine(lineNumber, orderLine, null, null, remarks);
    }

    /**
     * Creates a receipt line for a short delivery.
     *
     * <p><strong>Partial receipt is the normal case, not an edge case.</strong> A
     * supplier honouring a 500 unit order may ship 300 today and 200 next month,
     * and the receiving clerk has to record what physically arrived rather than
     * what was ordered. Booking the ordered quantity and correcting it later is how
     * stock ends up overstated.
     *
     * <p>The quantity is still bounded by the order line here, so a receipt can
     * never leave the order and the receipt note disagreeing about how much of a
     * line is still outstanding. The aggregate repeats the same check when it
     * applies the receipt to the order, which is the check that actually has to
     * hold.
     *
     * <p>A null {@code receivedQuantity} means "everything outstanding", so the
     * complete-delivery path and the short-delivery path are one code path with a
     * default rather than two that can drift.
     *
     * <p>{@code warehouseId} covers the split consignment: this quantity went to a
     * different site than the rest of the note. The note records the site it is filed
     * under and this line records where its goods actually are, which is the pair a
     * stock posting needs to credit the right balance.
     *
     * @param lineNumber       the 1-based ordinal within this receipt
     * @param orderLine        the order line being received
     * @param receivedQuantity the quantity that arrived, or null for the whole
     *                         outstanding quantity
     * @param warehouseId      the warehouse for this line, left unset here and
     *                         settled by {@link #resolveWarehouse} when the line is
     *                         attached to its note
     * @param remarks          free text remarks, may be null
     * @return a new unsaved receipt line
     * @throws IllegalArgumentException if nothing is outstanding, or the quantity
     *                                  is not positive or exceeds what remains
     */
    public static GoodsReceiptLine fromOrderLine(int lineNumber, PurchaseOrderLine orderLine,
                                                  BigDecimal receivedQuantity, Long warehouseId,
                                                  String remarks) {

        BigDecimal outstanding = orderLine.outstandingQuantity();
        if (outstanding.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Purchase order line " + orderLine.getLineNumber() + " has nothing left to receive");
        }

        BigDecimal quantity = receivedQuantity == null
                ? outstanding
                : receivedQuantity.setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);

        if (quantity.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Received quantity on line " + orderLine.getLineNumber() + " must be positive");
        }
        if (quantity.compareTo(outstanding) > 0) {
            throw new IllegalArgumentException(
                    "Receiving " + quantity.toPlainString() + " " + orderLine.getUomCode()
                            + " on line " + orderLine.getLineNumber() + " exceeds the "
                            + outstanding.toPlainString() + " still outstanding");
        }

        // Prorated on the ordered quantity rather than the unit price, so the
        // discount and tax already baked into the net line amount are carried
        // across proportionally instead of being re-priced.
        BigDecimal netAmount = orderLine.netAmount()
                .multiply(quantity)
                .divide(orderLine.getQuantity(), MONEY_SCALE, RoundingMode.HALF_UP);

        return GoodsReceiptLine.builder()
                .lineNumber(lineNumber)
                .purchaseOrderLineId(orderLine.getId())
                .itemId(orderLine.getItemId())
                .itemCode(orderLine.getItemCode())
                .itemName(orderLine.getItemName())
                .uomCode(orderLine.getUomCode())
                .unitId(orderLine.getUnitId())
                .receivedQuantity(quantity)
                .unitPrice(orderLine.getUnitPrice())
                .lineAmount(netAmount.setScale(MONEY_SCALE, RoundingMode.HALF_UP))
                .warehouseId(warehouseId)
                .remarks(remarks)
                .build();
    }
}
