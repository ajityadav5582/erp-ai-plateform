package com.erp.business.procurement.purchaseinvoice.domain;

import com.erp.business.procurement.goodsreceipt.domain.GoodsReceiptLine;
import com.erp.platform.data.entity.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/**
 * One line of a supplier invoice: what was billed, and at what price.
 *
 * <p><strong>Unlike a receipt line, this one is allowed to disagree with the order.</strong>
 * A supplier may bill a different unit price, or bill for a quantity that differs from
 * what arrived. That difference is the entire reason a three-way match exists, and it
 * has to be recorded somewhere rather than being silently overwritten with the ordered
 * figures. {@code unitPrice} and {@code lineAmount} are therefore taken from the invoice
 * as received, and compared against the order later.
 */
@Entity
@Table(
        name = "purchase_invoice_lines",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_purchase_invoice_lines_invoice_line_number",
                columnNames = {"purchase_invoice_id", "line_number"}
        ),
        indexes = {
                @Index(name = "idx_purchase_invoice_lines_invoice",
                        columnList = "purchase_invoice_id, line_number"),
                @Index(name = "idx_purchase_invoice_lines_receipt_line",
                        columnList = "tenant_id, company_id, goods_receipt_line_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class PurchaseInvoiceLine extends TenantScopedEntity<Long> {

    private static final long serialVersionUID = 1L;

    private static final int MONEY_SCALE = 4;
    private static final int QUANTITY_SCALE = 6;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    @NotNull(message = "Line number is required")
    @Positive(message = "Line number must be positive")
    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    /**
     * The receipt line this invoice line bills for.
     *
     * <p>An id rather than a relation: the invoice and the receipt are separate
     * aggregates that must stay independently loadable.
     */
    @Column(name = "goods_receipt_line_id")
    private Long goodsReceiptLineId;

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

    /** The price the supplier billed, which may differ from the ordered price. */
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

    @Column(name = "description", length = 500)
    private String description;

    /** Owning invoice. Written by the aggregate root only. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_invoice_id", nullable = false, updatable = false)
    @ToString.Exclude
    private PurchaseInvoice purchaseInvoice;

    void attachTo(PurchaseInvoice invoice) {
        this.purchaseInvoice = invoice;
    }

    /**
     * Creates an invoice line by billing a received line at the supplier's own price.
     *
     * @param lineNumber       the 1-based ordinal within this invoice
     * @param receiptLine      the receipt line being billed
     * @param quantity         the billed quantity, must be positive
     * @param unitPrice        the supplier's billed unit price, must not be negative
     * @param discountRate     the billed discount percentage, 0 to 100
     * @param taxRate          the tax percentage, 0 to 100
     * @param description      what the supplier described this line as, may be null
     * @return a new unsaved invoice line
     */
    public static PurchaseInvoiceLine bill(Integer lineNumber, GoodsReceiptLine receiptLine,
                                           BigDecimal quantity, BigDecimal unitPrice,
                                           BigDecimal discountRate, BigDecimal taxRate,
                                           String description) {

        validateQuantity(quantity);
        if (unitPrice == null || unitPrice.signum() < 0) {
            throw new IllegalArgumentException("Unit price cannot be negative");
        }
        BigDecimal discount = discountRate == null ? BigDecimal.ZERO : discountRate;
        BigDecimal tax = taxRate == null ? BigDecimal.ZERO : taxRate;
        validatePercentage(discount, "Discount");
        validatePercentage(tax, "Tax rate");

        BigDecimal gross = quantity.multiply(unitPrice);
        BigDecimal net = gross.multiply(
                        ONE_HUNDRED.subtract(discount).divide(ONE_HUNDRED, 10, RoundingMode.HALF_UP))
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        return PurchaseInvoiceLine.builder()
                .lineNumber(lineNumber)
                .goodsReceiptLineId(receiptLine.getId())
                .itemId(receiptLine.getItemId())
                .itemCode(receiptLine.getItemCode())
                .itemName(receiptLine.getItemName())
                .uomCode(receiptLine.getUomCode())
                .quantity(quantity.setScale(QUANTITY_SCALE, RoundingMode.HALF_UP))
                .unitPrice(unitPrice.setScale(MONEY_SCALE, RoundingMode.HALF_UP))
                .discountPercentage(discount.setScale(2, RoundingMode.HALF_UP))
                .taxRate(tax.setScale(2, RoundingMode.HALF_UP))
                .lineAmount(net)
                .description(description)
                .build();
    }

    /**
     * Returns the tax due on this line.
     */
    public BigDecimal taxAmount() {
        return lineAmount.multiply(taxRate)
                .divide(ONE_HUNDRED, MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private static void validateQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
    }

    private static void validatePercentage(BigDecimal value, String label) {
        if (value.signum() < 0 || value.compareTo(ONE_HUNDRED) > 0) {
            throw new IllegalArgumentException(label + " percentage must be between 0 and 100");
        }
    }
}
