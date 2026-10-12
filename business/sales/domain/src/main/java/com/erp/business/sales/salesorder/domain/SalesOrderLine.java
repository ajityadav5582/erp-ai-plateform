package com.erp.business.sales.salesorder.domain;

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
import java.util.Objects;

/**
 * One line of a sales order: what was promised to the customer, at what price.
 *
 * <p><strong>Item is a frozen snapshot.</strong> The item lives in the inventory
 * service and is referenced by id with its display fields copied here, so a later
 * rename never rewrites a confirmed order.
 *
 * <p><strong>Delivery progress is derived, never accepted.</strong>
 * {@code deliveredQuantity} is written only by {@link #recordDelivery} and
 * {@link #reverseDelivery}, and {@link #status} is recomputed from it, so the
 * outstanding quantity can never disagree with what has actually shipped.
 */
@Entity
@Table(
        name = "sales_order_lines",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_sales_order_lines_order_line_number",
                columnNames = {"sales_order_id", "line_number"}
        ),
        indexes = {
                @Index(name = "idx_sales_order_lines_order",
                        columnList = "sales_order_id, line_number"),
                @Index(name = "idx_sales_order_lines_item",
                        columnList = "tenant_id, company_id, item_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class SalesOrderLine extends TenantScopedEntity<Long> {

    private static final long serialVersionUID = 1L;

    private static final int MONEY_SCALE = 4;
    private static final int QUANTITY_SCALE = 6;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    @NotNull(message = "Line number is required")
    @Positive(message = "Line number must be positive")
    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

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

    @NotNull(message = "Quantity is required")
    @Column(name = "quantity", nullable = false, precision = 19, scale = 6)
    private BigDecimal quantity;

    @NotNull(message = "Unit price is required")
    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "discount_percentage", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal discountPercentage = BigDecimal.ZERO;

    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal taxRate = BigDecimal.ZERO;

    @NotNull(message = "Line amount is required")
    @Column(name = "line_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal lineAmount;

    /** Cumulative delivered quantity. Written only by record/reverseDelivery. */
    @Column(name = "delivered_quantity", nullable = false, precision = 19, scale = 6)
    @Builder.Default
    private BigDecimal deliveredQuantity = BigDecimal.ZERO.setScale(QUANTITY_SCALE);

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    @Builder.Default
    private SalesOrderLineStatus status = SalesOrderLineStatus.OPEN;

    @Column(name = "description", length = 500)
    private String description;

    /** Owning order. Written by the aggregate root only. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sales_order_id", nullable = false)
    @ToString.Exclude
    private SalesOrder salesOrder;

    /**
     * Creates a line and computes its amount from quantity, price and discount.
     *
     * <p>The amount is net of discount and excludes tax; tax is applied at the
     * header so a single document-level VAT treatment can be enforced.
     */
    public static SalesOrderLine create(int lineNumber, Long itemId, String itemCode, String itemName,
                                        String uomCode, BigDecimal quantity, BigDecimal unitPrice,
                                        BigDecimal discountPercentage, BigDecimal taxRate, String description) {
        validateQuantity(quantity);
        validateUnitPrice(unitPrice);
        validateDiscount(discountPercentage);
        SalesOrderLine line = SalesOrderLine.builder()
                .lineNumber(lineNumber)
                .itemId(Objects.requireNonNull(itemId, "Item is required"))
                .itemCode(itemCode)
                .itemName(itemName)
                .uomCode(uomCode)
                .quantity(quantity.setScale(QUANTITY_SCALE, RoundingMode.HALF_UP))
                .unitPrice(unitPrice.setScale(MONEY_SCALE, RoundingMode.HALF_UP))
                .discountPercentage(discountPercentage == null ? BigDecimal.ZERO : discountPercentage)
                .taxRate(taxRate == null ? BigDecimal.ZERO : taxRate)
                .description(description)
                .deliveredQuantity(BigDecimal.ZERO.setScale(QUANTITY_SCALE))
                .status(SalesOrderLineStatus.OPEN)
                .build();
        line.recalculateAmount();
        return line;
    }

    /**
     * Updates an existing draft line in place.
     */
    public void update(Long itemId, String itemCode, String itemName,
                       String uomCode, BigDecimal quantity, BigDecimal unitPrice,
                       BigDecimal discountPercentage, BigDecimal taxRate, String description) {
        validateQuantity(quantity);
        validateUnitPrice(unitPrice);
        validateDiscount(discountPercentage);
        this.itemId = Objects.requireNonNull(itemId, "Item is required");
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.uomCode = uomCode;
        this.quantity = quantity.setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);
        this.unitPrice = unitPrice.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        this.discountPercentage = discountPercentage == null ? BigDecimal.ZERO : discountPercentage;
        this.taxRate = taxRate == null ? BigDecimal.ZERO : taxRate;
        this.description = description;
        recalculateAmount();
    }

    private static void validateQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
    }

    private static void validateUnitPrice(BigDecimal unitPrice) {
        if (unitPrice == null || unitPrice.signum() < 0) {
            throw new IllegalArgumentException("Unit price must not be negative");
        }
    }

    private static void validateDiscount(BigDecimal discountPercentage) {
        if (discountPercentage != null && (discountPercentage.signum() < 0 || discountPercentage.compareTo(ONE_HUNDRED) > 0)) {
            throw new IllegalArgumentException("Discount percentage must be between 0 and 100");
        }
    }

    /** Recomputes the net line amount from quantity, unit price and discount. */
    public void recalculateAmount() {
        BigDecimal gross = quantity.multiply(unitPrice);
        BigDecimal discount = gross.multiply(discountPercentage).divide(ONE_HUNDRED, MONEY_SCALE, RoundingMode.HALF_UP);
        this.lineAmount = gross.subtract(discount).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /** The VAT carried by this line, at its own tax rate. */
    public BigDecimal taxAmount() {
        return lineAmount.multiply(taxRate).divide(ONE_HUNDRED, MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /** Quantity still owed to the customer. */
    public BigDecimal outstandingQuantity() {
        return quantity.subtract(deliveredQuantity).setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Records a delivery against this line and re-derives its status.
     *
     * @throws IllegalStateException if the delivery would exceed the ordered quantity
     */
    public void recordDelivery(BigDecimal delivered) {
        if (delivered == null || delivered.signum() <= 0) {
            throw new IllegalArgumentException("Delivered quantity must be greater than zero");
        }
        BigDecimal newDelivered = deliveredQuantity.add(delivered);
        if (newDelivered.compareTo(quantity) > 0) {
            throw new IllegalStateException(
                    "Delivering " + delivered.toPlainString() + " would exceed the ordered quantity "
                            + quantity.toPlainString() + " on line " + lineNumber);
        }
        this.deliveredQuantity = newDelivered.setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);
        this.status = deriveStatus();
    }

    /**
     * Reverses a previously recorded delivery (a voided delivery) and re-derives
     * the status. The ledger is append-only; this only moves the progress back.
     */
    public void reverseDelivery(BigDecimal delivered) {
        if (delivered == null || delivered.signum() <= 0) {
            throw new IllegalArgumentException("Reversed quantity must be greater than zero");
        }
        BigDecimal newDelivered = deliveredQuantity.subtract(delivered);
        if (newDelivered.signum() < 0) {
            throw new IllegalStateException(
                    "Reversing " + delivered.toPlainString() + " would take line " + lineNumber
                            + " below zero delivered");
        }
        this.deliveredQuantity = newDelivered.setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);
        this.status = deriveStatus();
    }

    private SalesOrderLineStatus deriveStatus() {
        if (status == SalesOrderLineStatus.CANCELLED) {
            return SalesOrderLineStatus.CANCELLED;
        }
        if (deliveredQuantity.signum() == 0) {
            return SalesOrderLineStatus.OPEN;
        }
        return deliveredQuantity.compareTo(quantity) >= 0
                ? SalesOrderLineStatus.DELIVERED
                : SalesOrderLineStatus.PARTIALLY_DELIVERED;
    }

    void attachTo(SalesOrder order) {
        this.salesOrder = order;
    }
}
