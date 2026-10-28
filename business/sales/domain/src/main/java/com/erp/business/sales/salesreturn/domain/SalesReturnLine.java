package com.erp.business.sales.salesreturn.domain;

import com.erp.business.sales.invoice.domain.VatType;
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
 * One line of a sales return (credit note): what came back and the VAT it reverses.
 *
 * <p>The VAT treatment mirrors the original invoice line, so the credit note
 * reverses exactly the output VAT that was charged - which is what the sales
 * register and the VAT return need.
 */
@Entity
@Table(
        name = "sales_return_lines",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_sales_return_lines_return_line_number",
                columnNames = {"sales_return_id", "line_number"}
        ),
        indexes = {
                @Index(name = "idx_sales_return_lines_return",
                        columnList = "sales_return_id, line_number"),
                @Index(name = "idx_sales_return_lines_item",
                        columnList = "tenant_id, company_id, item_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class SalesReturnLine extends TenantScopedEntity<Long> {

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

    @NotNull(message = "VAT type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "vat_type", nullable = false, length = 20)
    private VatType vatType;

    @Column(name = "vat_rate", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal vatRate = BigDecimal.ZERO;

    @NotNull(message = "Line amount is required")
    @Column(name = "line_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal lineAmount;

    @Column(name = "description", length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sales_return_id", nullable = false)
    @ToString.Exclude
    private SalesReturn salesReturn;

    public static SalesReturnLine create(int lineNumber, Long itemId, String itemCode, String itemName,
                                         String uomCode, BigDecimal quantity, BigDecimal unitPrice,
                                         VatType vatType, BigDecimal vatRate, String description) {
        VatType type = Objects.requireNonNull(vatType, "VAT type is required");
        BigDecimal rate = type == VatType.TAXABLE
                ? (vatRate != null ? vatRate : VatType.STANDARD_RATE)
                : BigDecimal.ZERO;
        SalesReturnLine line = SalesReturnLine.builder()
                .lineNumber(lineNumber)
                .itemId(Objects.requireNonNull(itemId, "Item is required"))
                .itemCode(itemCode)
                .itemName(itemName)
                .uomCode(uomCode)
                .quantity(quantity.setScale(QUANTITY_SCALE, RoundingMode.HALF_UP))
                .unitPrice(unitPrice.setScale(MONEY_SCALE, RoundingMode.HALF_UP))
                .vatType(type)
                .vatRate(rate)
                .description(description)
                .build();
        line.recalculateAmount();
        return line;
    }

    public void recalculateAmount() {
        this.lineAmount = quantity.multiply(unitPrice).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal vatAmount() {
        return lineAmount.multiply(vatRate).divide(ONE_HUNDRED, MONEY_SCALE, RoundingMode.HALF_UP);
    }

    void attachTo(SalesReturn salesReturn) {
        this.salesReturn = salesReturn;
    }
}
