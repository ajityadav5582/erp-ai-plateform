package com.erp.business.procurement.goodsreceipt.domain;

import com.erp.business.procurement.purchaseorder.domain.PurchaseOrder;
import com.erp.platform.data.entity.TenantScopedEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Goods receipt note: proof that ordered goods physically arrived.
 *
 * <p><strong>What it does.</strong> It records what was received against a purchase
 * order. The order is the thing that moves: this aggregate does not hold its own notion
 * of outstanding quantity, it only asks the order to apply the receipt. Keeping one
 * writer for received quantity is what stops the order and the receipt note from
 * disagreeing about how much has arrived.
 *
 * <p><strong>It does not restate the price.</strong> A receipt copies the order's unit
 * price. A supplier billing a different figure is a three-way match problem, resolved on
 * the invoice, not something the warehouse decides.
 *
 * <p><strong>No draft state.</strong> A receipt is created already applied. See
 * {@link GoodsReceiptStatus}.
 */
@Entity
@Table(
        name = "goods_receipts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_goods_receipts_tenant_company_number",
                columnNames = {"tenant_id", "company_id", "grn_number"}
        ),
        indexes = {
                @Index(name = "idx_goods_receipts_tenant_company",
                        columnList = "tenant_id, company_id"),
                @Index(name = "idx_goods_receipts_order",
                        columnList = "tenant_id, company_id, purchase_order_id, receipt_date"),
                @Index(name = "idx_goods_receipts_supplier",
                        columnList = "tenant_id, company_id, supplier_id, receipt_date"),
                @Index(name = "idx_goods_receipts_fiscal_year",
                        columnList = "tenant_id, company_id, fiscal_year_id, receipt_date")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class GoodsReceipt extends TenantScopedEntity<Long> {

    private static final long serialVersionUID = 1L;

    private static final int MONEY_SCALE = 4;

    @NotBlank(message = "GRN number is required")
    @Column(name = "grn_number", nullable = false, length = 40)
    private String grnNumber;

    @Column(name = "fiscal_year_id")
    private Long fiscalYearId;

    @Column(name = "branch_id")
    private Long branchId;

    /** The order being received against. */
    @Column(name = "purchase_order_id", nullable = false)
    private Long purchaseOrderId;

    /** Frozen order number, so the note still prints correctly if the order is renamed. */
    @Column(name = "po_number", nullable = false, length = 40)
    private String poNumber;

    @NotNull(message = "Receipt date is required")
    @Column(name = "receipt_date", nullable = false)
    private LocalDate receiptDate;

    @NotNull(message = "Supplier is required")
    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "supplier_code", nullable = false, length = 64)
    private String supplierCode;

    @Column(name = "supplier_name", nullable = false, length = 255)
    private String supplierName;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "NPR";

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private GoodsReceiptStatus status = GoodsReceiptStatus.POSTED;

    /**
     * Whether this receipt's stock movement has been landed in the Inventory
     * ledger.
     *
     * <p>The receipt is recorded the moment goods arrive, independently of
     * whether Inventory is reachable. This tracks the second step so a failed
     * post is visible and retriable rather than silently lost.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "stock_post_status", nullable = false, length = 20)
    @Builder.Default
    private StockPostStatus stockPostStatus = StockPostStatus.PENDING;

    /** Why the last stock post failed, shown to the operator retrying it. */
    @Column(name = "stock_post_error", length = 500)
    private String stockPostError;

    /**
     * Net value of the goods received on this note.
     *
     * <p>Derived from the lines, never accepted: there is no setter, and the only writer
     * is {@link #recalculateTotals()} after a line is attached.
     */
    @Column(name = "received_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal receivedAmount = BigDecimal.ZERO.setScale(MONEY_SCALE);

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO.setScale(MONEY_SCALE);

    /**
     * The warehouse the goods were received into.
     *
     * <p><strong>This is the field stock posting hangs off.</strong> A balance is
     * keyed on warehouse plus item, so without a warehouse on the receipt there is
     * no key to post against and no way to answer "how much do we hold here".
     *
     * <p>Resolved before posting, in this order: what the receiving clerk states,
     * then the order's nominated warehouse, then the company default. The order's
     * nomination is a fallback rather than a rule - goods get redirected, and
     * recording where they actually landed is the whole point of a receipt.
     *
     * <p>Not nullable on a posted note. See {@link #resolveWarehouse(Long, String, String)}.
     */
    @Column(name = "warehouse_id")
    private Long warehouseId;

    /** Frozen warehouse code and name, so a rename never rewrites a posted note. */
    @Column(name = "warehouse_code", length = 64)
    private String warehouseCode;

    @Column(name = "warehouse_name", length = 255)
    private String warehouseName;

    /** The supplier's own delivery challan or gate pass number. */
    @Column(name = "delivery_challan_no", length = 100)
    private String deliveryChallanNo;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "posted_at")
    private Instant postedAt;

    @Column(name = "posted_by", length = 100)
    private String postedBy;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancelled_by", length = 100)
    private String cancelledBy;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @OneToMany(mappedBy = "goodsReceipt", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    @OrderBy("lineNumber ASC")
    @ToString.Exclude
    @Builder.Default
    private List<GoodsReceiptLine> lines = new ArrayList<>();

    /**
     * Creates a posted receipt note against an order, with the delivery details.
     *
     * <p>Supplier, currency, branch and fiscal year are copied from the order rather
     * than asked for: a receipt is evidence about an order, and letting it name a
     * different supplier or currency would break the audit trail the order exists to
     * provide.
     *
     * @param grnNumber    the allocated document number
     * @param receiptDate  the date the goods arrived, defaults to today when null
     * @param order        the order being received against
     * @param challanNo    the supplier's delivery challan number, may be null
     * @param notes        free text notes, may be null
     * @param actor        the acting user id
     * @return a new unsaved receipt with no lines
     */
    public static GoodsReceipt post(String grnNumber, LocalDate receiptDate,
                                    PurchaseOrder order, Long warehouseId,
                                    String warehouseCode, String warehouseName,
                                    String challanNo, String notes,
                                    String actor) {

        GoodsReceipt receipt = GoodsReceipt.builder()
                .grnNumber(grnNumber)
                .receiptDate(receiptDate == null ? LocalDate.now() : receiptDate)
                .purchaseOrderId(order.getId())
                .poNumber(order.getPoNumber())
                .fiscalYearId(order.getFiscalYearId())
                .branchId(order.getBranchId())
                .supplierId(order.getSupplierId())
                .supplierCode(order.getSupplierCode())
                .supplierName(order.getSupplierName())
                .currency(order.getCurrency())
                .status(GoodsReceiptStatus.POSTED)
                .warehouseId(warehouseId)
                .warehouseCode(warehouseCode)
                .warehouseName(warehouseName)
                .deliveryChallanNo(challanNo)
                .notes(notes)
                .postedAt(Instant.now())
                .postedBy(actor)
                .lines(new ArrayList<>())
                .build();

        // Refusing here rather than at persist means the caller gets a message
        // naming the missing decision, instead of a constraint violation from
        // the database several frames deeper.
        if (receipt.warehouseId == null) {
            throw new IllegalStateException(
                    "A goods receipt must name the warehouse the goods were received into. "
                            + "Supply one explicitly, or nominate one on purchase order "
                            + order.getPoNumber() + ".");
        }

        return receipt;
    }

    /**
     * Attaches a receipt line and refreshes the header total.
     *
     * @param line the line to attach
     */
    public void addLine(GoodsReceiptLine line) {
        if (lines.stream().anyMatch(existing ->
                existing.getLineNumber().equals(line.getLineNumber()))) {
            throw new IllegalStateException(
                    "Line number " + line.getLineNumber() + " is already used on this receipt");
        }
        line.assignScope(getTenantId(), getCompanyId());
        line.attachTo(this);
        // Settled here, by the aggregate root, so no line can be attached without a
        // warehouse. A line that carries its own value keeps it, which is what
        // allows one consignment to be split across two sites.
        line.resolveWarehouse(getWarehouseId());
        lines.add(line);
        recalculateTotals();
    }

    /**
     * Recomputes the receipt value from its lines.
     *
     * <p>Called on every line attachment rather than lazily on read, so the persisted
     * figure is always current and list screens do not have to aggregate the line table.
     */
    public void recalculateTotals() {
        BigDecimal net = BigDecimal.ZERO;
        for (GoodsReceiptLine line : lines) {
            net = net.add(line.getLineAmount());
        }
        this.receivedAmount = net.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Voids this receipt. A reason is required: an unexplained void of a warehouse
     * document is not auditable.
     *
     * @param actor  the acting user id
     * @param reason why the receipt is being voided
     */
    public void cancel(String actor, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A reason is required to cancel a goods receipt");
        }
        if (status == GoodsReceiptStatus.CANCELLED) {
            throw new IllegalStateException("Goods receipt " + grnNumber + " is already cancelled");
        }
        this.status = GoodsReceiptStatus.CANCELLED;
        this.cancelledAt = Instant.now();
        this.cancelledBy = actor;
        this.cancellationReason = reason;
    }

    /** Records that Inventory accepted this receipt's stock movement. */
    public void markStockPosted() {
        this.stockPostStatus = StockPostStatus.POSTED;
        this.stockPostError = null;
    }

    /**
     * Records that posting the stock movement failed, keeping the reason so the
     * operator retrying it can see what went wrong. The receipt itself is kept.
     */
    public void markStockFailed(String error) {
        this.stockPostStatus = StockPostStatus.FAILED;
        this.stockPostError = error;
    }
}
