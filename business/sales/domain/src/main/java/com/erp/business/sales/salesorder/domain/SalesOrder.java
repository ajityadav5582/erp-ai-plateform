package com.erp.business.sales.salesorder.domain;

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
import java.util.Objects;

/**
 * Sales order aggregate root: the commercial commitment to a customer.
 *
 * <p><strong>Aggregate boundary.</strong> A sales order owns its lines and nothing
 * else. Customer and item records live in the customer master and the inventory
 * service respectively and are referenced by id with a frozen snapshot of their
 * display fields. The header owns the status machine, the totals and the invariant
 * that the sum of the lines is the only source of the document totals.
 *
 * <p><strong>Totals are derived, never accepted.</strong> There is no
 * {@code setTotal(...)}. {@link #recalculateTotals()} is the single writer, called
 * after every line mutation, so the header total cannot disagree with the lines
 * that produce it. The values are still persisted, because list screens and reports
 * must not have to aggregate the line table on every read.
 *
 * <p><strong>Concurrency.</strong> The inherited {@code version} column gives
 * optimistic locking: two sellers editing the same order produce an
 * {@code OptimisticLockingFailureException} on the loser rather than a silent lost
 * update.
 */
@Entity
@Table(
        name = "sales_orders",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_sales_orders_tenant_company_number",
                        columnNames = {"tenant_id", "company_id", "so_number"}
                )
        },
        indexes = {
                @Index(name = "idx_sales_orders_tenant_company",
                        columnList = "tenant_id, company_id"),
                @Index(name = "idx_sales_orders_customer",
                        columnList = "tenant_id, company_id, customer_id, order_date"),
                @Index(name = "idx_sales_orders_fiscal_year",
                        columnList = "tenant_id, company_id, fiscal_year_id, order_date"),
                @Index(name = "idx_sales_orders_status",
                        columnList = "tenant_id, company_id, status, order_date")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class SalesOrder extends TenantScopedEntity<Long> {

    private static final long serialVersionUID = 1L;

    private static final int MONEY_SCALE = 4;

    /**
     * Human readable document number, allocated by the document-sequence counter
     * inside the same transaction as the insert. Unique per tenant and company.
     */
    @NotBlank(message = "SO number is required")
    @Column(name = "so_number", nullable = false, length = 40)
    private String soNumber;

    /** Fiscal year the order belongs to, for period reporting. May be null pre-assignment. */
    @Column(name = "fiscal_year_id")
    private Long fiscalYearId;

    /** Owning branch. */
    @Column(name = "branch_id")
    private Long branchId;

    @NotNull(message = "Order date is required")
    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    /**
     * Customer reference into the customer master. No database foreign key: the
     * customer is mastered in the same service but kept as a loose reference so the
     * order stays independently loadable and a customer rename never rewrites it.
     */
    @NotNull(message = "Customer is required")
    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    /** Frozen customer snapshot: the name printed on the issued document. */
    @NotBlank(message = "Customer code is required")
    @Column(name = "customer_code", nullable = false, length = 64)
    private String customerCode;

    @NotBlank(message = "Customer name is required")
    @Column(name = "customer_name", nullable = false, length = 255)
    private String customerName;

    /** Frozen customer PAN, so a B2B order carries the buyer's IRD PAN. */
    @Column(name = "customer_pan_number", length = 16)
    private String customerPanNumber;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    @Builder.Default
    private SalesOrderStatus status = SalesOrderStatus.DRAFT;

    /** A sales order is single currency; lines carry no currency of their own. */
    @NotBlank(message = "Currency is required")
    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "NPR";

    // ---- Derived totals. Written only by recalculateTotals(). ----
    @Column(name = "subtotal_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal subtotalAmount = BigDecimal.ZERO.setScale(MONEY_SCALE);

    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO.setScale(MONEY_SCALE);

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO.setScale(MONEY_SCALE);

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO.setScale(MONEY_SCALE);

    /** Cumulative delivered value, for progress reporting. Derived from the lines. */
    @Column(name = "delivered_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal deliveredAmount = BigDecimal.ZERO.setScale(MONEY_SCALE);

    @Column(name = "payment_terms_days")
    private Integer paymentTermsDays;

    /** The warehouse the goods are expected to ship from. */
    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "warehouse_code", length = 64)
    private String warehouseCode;

    @Column(name = "warehouse_name", length = 255)
    private String warehouseName;

    /** Free reference such as the quotation this order arose from. */
    @Column(name = "reference", length = 100)
    private String reference;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // ---- Workflow stamps: NULL until the transition happens. ----
    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "confirmed_by", length = 100)
    private String confirmedBy;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancelled_by", length = 100)
    private String cancelledBy;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "closed_by", length = 100)
    private String closedBy;

    /**
     * The lines of this order.
     *
     * <p>{@code cascade = ALL} because lines have no meaning outside their header,
     * and {@code orphanRemoval = true} so that removing the last reference from a
     * draft deletes the row rather than orphaning it.
     */
    @OneToMany(mappedBy = "salesOrder", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    @OrderBy("lineNumber ASC")
    @ToString.Exclude
    @Builder.Default
    private List<SalesOrderLine> lines = new ArrayList<>();

    // -----------------------------------------------------------------------
    // Construction
    // -----------------------------------------------------------------------

    /**
     * Creates a draft sales order.
     *
     * <p>{@code soNumber} must be supplied by the caller, which obtains it from the
     * document-sequence counter inside the same transaction as the insert.
     * Generating it in Java would reintroduce the duplicate-number race the counter
     * exists to remove.
     */
    public static SalesOrder draft(String soNumber, LocalDate orderDate, Long customerId,
                                   String customerCode, String customerName, String customerPanNumber,
                                   String currency, Long fiscalYearId, Long branchId, Long warehouseId,
                                   String warehouseCode, String warehouseName, Integer paymentTermsDays,
                                   String reference, String notes) {
        Objects.requireNonNull(orderDate, "Order date is required");
        Objects.requireNonNull(customerId, "Customer is required");

        SalesOrder order = SalesOrder.builder()
                .soNumber(soNumber)
                .orderDate(orderDate)
                .customerId(customerId)
                .customerCode(customerCode)
                .customerName(customerName)
                .customerPanNumber(customerPanNumber)
                .currency(normalizeCurrency(currency))
                .fiscalYearId(fiscalYearId)
                .branchId(branchId)
                .warehouseId(warehouseId)
                .warehouseCode(warehouseCode)
                .warehouseName(warehouseName)
                .paymentTermsDays(paymentTermsDays)
                .reference(reference)
                .notes(notes)
                .status(SalesOrderStatus.DRAFT)
                .lines(new ArrayList<>())
                .build();
        order.recalculateTotals();
        return order;
    }

    // -----------------------------------------------------------------------
    // -----------------------------------------------------------------------
    // Mutation (draft only)
    // -----------------------------------------------------------------------

    /**
     * Updates header details of a draft order.
     */
    public void updateDraft(LocalDate orderDate, LocalDate expectedDeliveryDate,
                            Long branchId, Long warehouseId, String warehouseCode,
                            String warehouseName, Integer paymentTermsDays,
                            String reference, String notes) {
        requireDraft("update details of");
        this.orderDate = Objects.requireNonNull(orderDate, "Order date is required");
        this.expectedDeliveryDate = expectedDeliveryDate;
        this.branchId = branchId;
        this.warehouseId = warehouseId;
        this.warehouseCode = warehouseCode;
        this.warehouseName = warehouseName;
        this.paymentTermsDays = paymentTermsDays;
        this.reference = reference;
        this.notes = notes;
    }

    /**
     * Updates customer snapshot on a draft order if customer is changed.
     */
    public void updateCustomer(Long customerId, String customerCode, String customerName,
                               String customerPanNumber, String currency) {
        requireDraft("change customer on");
        this.customerId = Objects.requireNonNull(customerId, "Customer is required");
        this.customerCode = customerCode;
        this.customerName = customerName;
        this.customerPanNumber = customerPanNumber;
        if (currency != null && !currency.isBlank()) {
            this.currency = normalizeCurrency(currency);
        }
    }

    /**
     * Adds a line to a draft order and refreshes the header totals.
     *
     * @throws IllegalStateException if the order is not a draft or the line number is taken
     */
    public void addLine(SalesOrderLine line) {
        requireDraft("add a line to");
        if (lines.stream().anyMatch(existing -> existing.getLineNumber().equals(line.getLineNumber()))) {
            throw new IllegalStateException(
                    "Line number " + line.getLineNumber() + " is already used on this order");
        }
        line.assignScope(getTenantId(), getCompanyId());
        line.attachTo(this);
        lines.add(line);
        recalculateTotals();
    }

    /** Removes a line from a draft order and refreshes the header totals. */
    public void removeLine(int lineNumber) {
        requireDraft("remove a line from");
        boolean removed = lines.removeIf(existing -> existing.getLineNumber() == lineNumber);
        if (!removed) {
            throw new IllegalStateException("No line " + lineNumber + " on this order");
        }
        recalculateTotals();
    }

    /**
     * Replaces the draft order lines, updating existing lines in place,
     * adding new lines, and removing excess lines to prevent JPA unique constraint issues.
     */
    public void replaceLines(List<SalesOrderLine> newLines) {
        requireDraft("replace lines on");
        if (newLines == null || newLines.isEmpty()) {
            throw new IllegalArgumentException("Sales order must contain at least one line");
        }
        int newSize = newLines.size();
        lines.removeIf(line -> line.getLineNumber() > newSize);

        for (SalesOrderLine newLine : newLines) {
            var existing = lines.stream()
                    .filter(l -> l.getLineNumber().equals(newLine.getLineNumber()))
                    .findFirst();
            if (existing.isPresent()) {
                SalesOrderLine line = existing.get();
                line.update(
                        newLine.getItemId(), newLine.getItemCode(), newLine.getItemName(),
                        newLine.getUomCode(), newLine.getQuantity(), newLine.getUnitPrice(),
                        newLine.getDiscountPercentage(), newLine.getTaxRate(), newLine.getDescription());
            } else {
                newLine.assignScope(getTenantId(), getCompanyId());
                newLine.attachTo(this);
                lines.add(newLine);
            }
        }
        recalculateTotals();
    }

    // -----------------------------------------------------------------------
    // Totals
    // -----------------------------------------------------------------------

    /**
     * Recomputes subtotal, discount, tax and total from the lines, then re-derives
     * the delivered value. The single writer of every derived money field.
     */
    public void recalculateTotals() {
        BigDecimal net = BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        BigDecimal delivered = BigDecimal.ZERO;
        for (SalesOrderLine line : lines) {
            net = net.add(line.getLineAmount());
            BigDecimal gross = line.getQuantity().multiply(line.getUnitPrice());
            BigDecimal lineDiscount = gross.subtract(line.getLineAmount());
            discount = discount.add(lineDiscount);
            tax = tax.add(line.taxAmount());
            delivered = delivered.add(deliveredValueOf(line));
        }
        this.subtotalAmount = net.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        this.discountAmount = discount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        this.taxAmount = tax.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        this.totalAmount = net.add(tax).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        this.deliveredAmount = delivered.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal deliveredValueOf(SalesOrderLine line) {
        if (line.getQuantity().signum() == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal unitNet = line.getLineAmount().divide(line.getQuantity(), MONEY_SCALE, RoundingMode.HALF_UP);
        return unitNet.multiply(line.getDeliveredQuantity()).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    /** Confirms a draft order, freezing its content and issuing it to the customer. */
    public void confirm(String actor) {
        if (lines.isEmpty()) {
            throw new IllegalStateException("Cannot confirm an order with no lines");
        }
        transitionTo(SalesOrderStatus.CONFIRMED, actor, null);
        this.confirmedAt = Instant.now();
        this.confirmedBy = actor;
    }

    /** Cancels a draft or confirmed order. A reason is required. */
    public void cancel(String actor, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A reason is required to cancel an order");
        }
        transitionTo(SalesOrderStatus.CANCELLED, actor, reason);
        this.cancelledAt = Instant.now();
        this.cancelledBy = actor;
        this.cancellationReason = reason;
    }

    /** Closes a fully delivered order. Terminal. */
    public void close(String actor) {
        transitionTo(SalesOrderStatus.CLOSED, actor, null);
        this.closedAt = Instant.now();
        this.closedBy = actor;
    }

    /**
     * Records a delivery against a line and re-derives the header status.
     *
     * @throws IllegalStateException if the order is not confirmed, or the delivery
     *         would exceed the ordered quantity
     */
    public void deliverAgainstLine(int lineNumber, BigDecimal delivered) {
        if (status != SalesOrderStatus.CONFIRMED && status != SalesOrderStatus.PARTIALLY_DELIVERED) {
            throw new IllegalStateException(
                    "Cannot deliver against an order in status " + status);
        }
        SalesOrderLine line = lines.stream()
                .filter(existing -> existing.getLineNumber() == lineNumber)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No line " + lineNumber + " on this order"));
        line.recordDelivery(delivered);
        recalculateTotals();
        this.status = deriveDeliveryStatus();
    }

    /**
     * Reverses a previously recorded delivery (a voided delivery) and re-derives
     * the header status.
     */
    public void reverseDeliveryAgainstLine(int lineNumber, BigDecimal delivered) {
        SalesOrderLine line = lines.stream()
                .filter(existing -> existing.getLineNumber() == lineNumber)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No line " + lineNumber + " on this order"));
        line.reverseDelivery(delivered);
        recalculateTotals();
        if (status == SalesOrderStatus.PARTIALLY_DELIVERED || status == SalesOrderStatus.DELIVERED) {
            this.status = deriveDeliveryStatus();
        }
    }

    private SalesOrderStatus deriveDeliveryStatus() {
        boolean anyDelivered = lines.stream().anyMatch(line -> line.getDeliveredQuantity().signum() > 0);
        boolean allDelivered = lines.stream()
                .allMatch(line -> line.getDeliveredQuantity().compareTo(line.getQuantity()) >= 0);
        if (allDelivered) {
            return SalesOrderStatus.DELIVERED;
        }
        return anyDelivered ? SalesOrderStatus.PARTIALLY_DELIVERED : SalesOrderStatus.CONFIRMED;
    }

    /**
     * The single place a status change is validated. Declares the permitted
     * transitions once so no caller can move the order into an illegal state.
     */
    private void transitionTo(SalesOrderStatus target, String actor, String reason) {
        if (!isTransitionAllowed(this.status, target)) {
            throw new IllegalStateException(
                    "Cannot move a sales order from " + this.status + " to " + target);
        }
        this.status = target;
    }

    private static boolean isTransitionAllowed(SalesOrderStatus from, SalesOrderStatus to) {
        return switch (from) {
            case DRAFT -> to == SalesOrderStatus.CONFIRMED || to == SalesOrderStatus.CANCELLED;
            case CONFIRMED -> to == SalesOrderStatus.PARTIALLY_DELIVERED
                    || to == SalesOrderStatus.DELIVERED
                    || to == SalesOrderStatus.CANCELLED
                    || to == SalesOrderStatus.CLOSED;
            // Delivering more keeps it partially delivered; finishing moves it on.
            case PARTIALLY_DELIVERED -> to == SalesOrderStatus.PARTIALLY_DELIVERED
                    || to == SalesOrderStatus.DELIVERED
                    || to == SalesOrderStatus.CLOSED;
            case DELIVERED -> to == SalesOrderStatus.CLOSED;
            case CANCELLED, CLOSED -> false;
        };
    }

    private void requireDraft(String action) {
        if (status != SalesOrderStatus.DRAFT) {
            throw new IllegalStateException("Cannot " + action + " an order in status " + status);
        }
    }

    private static String normalizeCurrency(String currency) {
        return currency != null && !currency.isBlank() ? currency.trim().toUpperCase() : "NPR";
    }
}
