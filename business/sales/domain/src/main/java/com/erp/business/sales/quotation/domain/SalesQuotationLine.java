package com.erp.business.sales.quotation.domain;

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
import java.util.Objects;

@Entity
@Table(
        name = "sales_quotation_lines",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_sales_quotation_lines_quotation_line",
                columnNames = {"quotation_id", "line_number"}
        ),
        indexes = {
                @Index(name = "idx_sales_quotation_lines_quotation",
                        columnList = "quotation_id, line_number"),
                @Index(name = "idx_sales_quotation_lines_item",
                        columnList = "tenant_id, company_id, item_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(callSuper = true, exclude = "quotation")
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class SalesQuotationLine extends TenantScopedEntity<Long> {

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
    private BigDecimal taxRate = new BigDecimal("13.00");

    @Column(name = "line_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal lineAmount;

    @Column(name = "description", length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quotation_id", nullable = false)
    private SalesQuotation quotation;

    public static SalesQuotationLine create(int lineNumber,
                                            Long itemId,
                                            String itemCode,
                                            String itemName,
                                            String uomCode,
                                            BigDecimal quantity,
                                            BigDecimal unitPrice,
                                            BigDecimal discountPercentage,
                                            BigDecimal taxRate,
                                            String description) {
        SalesQuotationLine line = SalesQuotationLine.builder()
                .lineNumber(lineNumber)
                .itemId(Objects.requireNonNull(itemId, "Item ID is required"))
                .itemCode(Objects.requireNonNull(itemCode, "Item code is required"))
                .itemName(Objects.requireNonNull(itemName, "Item name is required"))
                .uomCode(Objects.requireNonNull(uomCode, "UOM code is required"))
                .quantity(Objects.requireNonNull(quantity, "Quantity is required").setScale(QUANTITY_SCALE, RoundingMode.HALF_UP))
                .unitPrice(Objects.requireNonNull(unitPrice, "Unit price is required").setScale(MONEY_SCALE, RoundingMode.HALF_UP))
                .discountPercentage(discountPercentage != null ? discountPercentage : BigDecimal.ZERO)
                .taxRate(taxRate != null ? taxRate : new BigDecimal("13.00"))
                .description(description)
                .build();
        line.recalculateAmount();
        return line;
    }

    public void recalculateAmount() {
        BigDecimal gross = this.quantity.multiply(this.unitPrice).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal disc = this.discountPercentage != null && this.discountPercentage.compareTo(BigDecimal.ZERO) > 0
                ? gross.multiply(this.discountPercentage).divide(ONE_HUNDRED, MONEY_SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        this.lineAmount = gross.subtract(disc).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    void assignQuotation(SalesQuotation quotation) {
        this.quotation = quotation;
    }
}
