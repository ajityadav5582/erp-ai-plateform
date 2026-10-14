package com.erp.business.pos.domain;

import com.erp.platform.data.entity.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * One line item within a {@link PosTransaction}.
 *
 * <p><strong>Item snapshot.</strong> All display fields (code, name, UOM) are
 * frozen at the time the line is added to the cart. A later rename of the item
 * in the inventory catalogue never rewrites a completed transaction. This is the
 * same pattern used by {@code SalesOrderLine} and {@code SalesInvoiceLine}.
 *
 * <p><strong>Derived amounts.</strong> {@link #recalculateAmount()} is the single
 * writer of {@link #lineAmount} and is called after every mutation (qty change,
 * discount change). The owning transaction then calls its own
 * {@link PosTransaction#recalculateTotals()} to roll up the header figures.
 *
 * @since 1.0.0
 */
@Entity
@Table(
        name = "pos_transaction_lines",
        indexes = {
                @Index(name = "idx_pos_tx_lines_transaction",
                        columnList = "pos_transaction_id, line_number"),
                @Index(name = "idx_pos_tx_lines_item",
                        columnList = "tenant_id, company_id, item_id")
        }
)
@Getter
@Setter(AccessLevel.PACKAGE) // only the parent aggregate mutates lines
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class PosTransactionLine extends TenantScopedEntity<Long> {

    private static final long serialVersionUID = 1L;

    private static final int MONEY_SCALE = 4;
    private static final int QTY_SCALE = 6;

    // -----------------------------------------------------------------------
    // Parent reference (owning side managed by PosTransaction)
    // -----------------------------------------------------------------------

    @NotNull(message = "POS transaction reference is required")
    @Column(name = "pos_transaction_id", nullable = false, updatable = false)
    private Long posTransactionId;

    @Min(value = 1, message = "Line number must be at least 1")
    @Column(name = "line_number", nullable = false)
    private int lineNumber;

    // -----------------------------------------------------------------------
    // Item snapshot (frozen at add-to-cart time)
    // -----------------------------------------------------------------------

    @NotNull(message = "Item id is required")
    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @NotBlank(message = "Item code snapshot is required")
    @Column(name = "item_code", nullable = false, length = 100)
    private String itemCode;

    @NotBlank(message = "Item name snapshot is required")
    @Column(name = "item_name", nullable = false, length = 255)
    private String itemName;

    @Size(max = 100)
    @Column(name = "barcode", length = 100)
    private String barcode;

    @NotBlank(message = "Unit of measure code is required")
    @Column(name = "uom_code", nullable = false, length = 32)
    private String uomCode;

    // -----------------------------------------------------------------------
    // Pricing
    // -----------------------------------------------------------------------

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.000001", message = "Quantity must be greater than zero")
    @Digits(integer = 13, fraction = 6, message = "Quantity has too many digits")
    @Column(name = "quantity", nullable = false, precision = 19, scale = 6)
    private BigDecimal quantity;

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.0000", message = "Unit price must not be negative")
    @Digits(integer = 15, fraction = 4, message = "Unit price has too many digits")
    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

    /**
     * Line-level discount percentage. Zero means no discount.
     * Range: 0.00 – 100.00.
     */
    @NotNull
    @Column(name = "discount_percentage", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal discountPercentage = BigDecimal.ZERO;

    /**
     * Net line amount after discount, <em>before</em> VAT.
     * {@code lineAmount = quantity × unitPrice × (1 - discountPct/100)}
     * Computed by {@link #recalculateAmount()}; never accepted from the client.
     */
    @Column(name = "line_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal lineAmount;

    // -----------------------------------------------------------------------
    // VAT
    // -----------------------------------------------------------------------

    @NotNull(message = "VAT type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "vat_type", nullable = false, length = 20)
    private PosVatType vatType;

    /** VAT rate as a percentage, e.g. 13.00 for 13%. Zero for EXEMPT/ZERO_RATED. */
    @NotNull
    @Column(name = "vat_rate", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal vatRate = BigDecimal.ZERO;

    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;

    // -----------------------------------------------------------------------
    // Factory method
    // -----------------------------------------------------------------------

    /**
     * Creates a new transaction line and immediately computes its amount.
     * Scope (tenant/company) must be assigned via
     * {@link #assignScope(Long, Long)} by the owning transaction before save.
     *
     * @param lineNumber         1-based sequential line number within the transaction
     * @param itemId             inventory item id
     * @param itemCode           item code snapshot
     * @param itemName           item name snapshot
     * @param barcode            barcode snapshot (may be null)
     * @param uomCode            unit of measure code
     * @param quantity           quantity sold
     * @param unitPrice          price per unit
     * @param discountPercentage line discount percentage (0 = no discount)
     * @param vatType            VAT treatment
     * @param vatRate            applicable VAT rate (e.g. 13.00)
     * @param description        optional description
     * @return the new line with {@link #lineAmount} pre-computed
     */
    public static PosTransactionLine of(
            int lineNumber, Long itemId, String itemCode, String itemName,
            String barcode, String uomCode,
            BigDecimal quantity, BigDecimal unitPrice, BigDecimal discountPercentage,
            PosVatType vatType, BigDecimal vatRate, String description) {

        Objects.requireNonNull(itemId, "Item id is required");
        Objects.requireNonNull(quantity, "Quantity is required");
        Objects.requireNonNull(unitPrice, "Unit price is required");

        BigDecimal disc = (discountPercentage != null && discountPercentage.signum() >= 0)
                ? discountPercentage : BigDecimal.ZERO;

        PosTransactionLine line = PosTransactionLine.builder()
                .lineNumber(lineNumber)
                .itemId(itemId)
                .itemCode(itemCode)
                .itemName(itemName)
                .barcode(barcode)
                .uomCode(uomCode)
                .quantity(quantity.setScale(QTY_SCALE, RoundingMode.HALF_UP))
                .unitPrice(unitPrice.setScale(MONEY_SCALE, RoundingMode.HALF_UP))
                .discountPercentage(disc.setScale(2, RoundingMode.HALF_UP))
                .vatType(vatType != null ? vatType : PosVatType.TAXABLE)
                .vatRate(vatRate != null ? vatRate.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .description(description)
                .build();
        line.recalculateAmount();
        return line;
    }

    // -----------------------------------------------------------------------
    // Computed values — single writer
    // -----------------------------------------------------------------------

    /**
     * Recomputes {@link #lineAmount} from the current quantity, unit price and
     * discount. Called on construction and after any mutation of these inputs.
     *
     * <p>{@code lineAmount = quantity × unitPrice × (1 − discountPct / 100)}
     */
    void recalculateAmount() {
        BigDecimal gross = quantity.multiply(unitPrice);
        BigDecimal discFactor = BigDecimal.ONE
                .subtract(discountPercentage.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP));
        this.lineAmount = gross.multiply(discFactor).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Returns the VAT amount for this line.
     *
     * <p>{@code vatAmount = lineAmount × vatRate / 100}
     */
    public BigDecimal vatAmount() {
        if (vatRate == null || vatRate.signum() == 0) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }
        return lineAmount.multiply(vatRate)
                .divide(BigDecimal.valueOf(100), MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Updates quantity and recomputes the line amount. Package-private so only
     * the owning transaction's mutator methods can call it.
     *
     * @param newQuantity the replacement quantity
     */
    void updateQuantity(BigDecimal newQuantity) {
        Objects.requireNonNull(newQuantity, "Quantity must not be null");
        if (newQuantity.signum() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        this.quantity = newQuantity.setScale(QTY_SCALE, RoundingMode.HALF_UP);
        recalculateAmount();
    }

    /**
     * Updates the discount and recomputes the line amount. Package-private so
     * only the owning transaction's mutator methods can call it.
     *
     * @param newDiscountPct the replacement discount percentage (0 – 100)
     */
    void updateDiscount(BigDecimal newDiscountPct) {
        if (newDiscountPct == null || newDiscountPct.signum() < 0) {
            throw new IllegalArgumentException("Discount percentage must be 0 or greater");
        }
        if (newDiscountPct.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Discount percentage cannot exceed 100");
        }
        this.discountPercentage = newDiscountPct.setScale(2, RoundingMode.HALF_UP);
        recalculateAmount();
    }

    /** Attaches this line to the given transaction id (called by the aggregate root). */
    void attachTo(Long transactionId) {
        this.posTransactionId = Objects.requireNonNull(transactionId, "Transaction id must not be null");
    }
}
