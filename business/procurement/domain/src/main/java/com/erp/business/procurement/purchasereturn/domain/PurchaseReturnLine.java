package com.erp.business.procurement.purchasereturn.domain;

import com.erp.business.procurement.purchaseinvoice.domain.PurchaseTaxCategory;
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

/**
 * One line item of a Purchase Return / Debit Note document.
 */
@Entity
@Table(
        name = "purchase_return_lines",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_prl_return_line_number",
                columnNames = {"purchase_return_id", "line_number"}
        ),
        indexes = {
                @Index(name = "idx_prl_return", columnList = "purchase_return_id, line_number"),
                @Index(name = "idx_prl_invoice_line", columnList = "tenant_id, company_id, purchase_invoice_line_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class PurchaseReturnLine extends TenantScopedEntity<Long> {

    private static final long serialVersionUID = 1L;
    private static final int QTY_SCALE = 6;
    private static final int MONEY_SCALE = 4;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_return_id", nullable = false)
    @ToString.Exclude
    private PurchaseReturn purchaseReturn;

    @NotNull(message = "Line number is required")
    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @Column(name = "purchase_invoice_line_id")
    private Long purchaseInvoiceLineId;

    @NotNull(message = "Item ID is required")
    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @NotBlank(message = "Item code is required")
    @Column(name = "item_code", nullable = false, length = 100)
    private String itemCode;

    @NotBlank(message = "Item name is required")
    @Column(name = "item_name", nullable = false, length = 255)
    private String itemName;

    @NotBlank(message = "UOM code is required")
    @Column(name = "uom_code", nullable = false, length = 32)
    private String uomCode;

    @NotNull(message = "Return quantity is required")
    @Positive(message = "Return quantity must be positive")
    @Column(name = "return_quantity", nullable = false, precision = 19, scale = 6)
    private BigDecimal returnQuantity;

    @NotNull(message = "Unit price is required")
    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_category", nullable = false, length = 32)
    @Builder.Default
    private PurchaseTaxCategory taxCategory = PurchaseTaxCategory.TAXABLE;

    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal taxRate = new BigDecimal("13.00");

    @Column(name = "taxable_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal taxableAmount = BigDecimal.ZERO.setScale(MONEY_SCALE);

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO.setScale(MONEY_SCALE);

    @Column(name = "subtotal_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal subtotalAmount = BigDecimal.ZERO.setScale(MONEY_SCALE);

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO.setScale(MONEY_SCALE);

    @Column(name = "remarks", length = 500)
    private String remarks;

    public static PurchaseReturnLine create(
            Integer lineNumber,
            Long purchaseInvoiceLineId,
            Long itemId,
            String itemCode,
            String itemName,
            String uomCode,
            BigDecimal returnQuantity,
            BigDecimal unitPrice,
            PurchaseTaxCategory taxCategory,
            BigDecimal taxRate,
            String remarks
    ) {
        if (returnQuantity == null || returnQuantity.signum() <= 0) {
            throw new IllegalArgumentException("Return quantity must be greater than zero");
        }
        if (unitPrice == null || unitPrice.signum() < 0) {
            throw new IllegalArgumentException("Unit price cannot be negative");
        }

        PurchaseTaxCategory resolvedCategory = taxCategory != null ? taxCategory : PurchaseTaxCategory.TAXABLE;
        BigDecimal resolvedTaxRate = taxRate != null ? taxRate : (resolvedCategory == PurchaseTaxCategory.TAXABLE ? new BigDecimal("13.00") : BigDecimal.ZERO);

        BigDecimal subtotal = returnQuantity.multiply(unitPrice).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal taxable = (resolvedCategory == PurchaseTaxCategory.TAXABLE) ? subtotal : BigDecimal.ZERO.setScale(MONEY_SCALE);
        BigDecimal tax = taxable.multiply(resolvedTaxRate).divide(new BigDecimal("100"), MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(tax).setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        return PurchaseReturnLine.builder()
                .lineNumber(lineNumber)
                .purchaseInvoiceLineId(purchaseInvoiceLineId)
                .itemId(itemId)
                .itemCode(itemCode)
                .itemName(itemName)
                .uomCode(uomCode != null ? uomCode.trim().toUpperCase() : "PCS")
                .returnQuantity(returnQuantity.setScale(QTY_SCALE, RoundingMode.HALF_UP))
                .unitPrice(unitPrice.setScale(MONEY_SCALE, RoundingMode.HALF_UP))
                .taxCategory(resolvedCategory)
                .taxRate(resolvedTaxRate.setScale(2, RoundingMode.HALF_UP))
                .taxableAmount(taxable)
                .taxAmount(tax)
                .subtotalAmount(subtotal)
                .totalAmount(total)
                .remarks(remarks)
                .build();
    }

    void assignToReturn(PurchaseReturn purchaseReturn) {
        this.purchaseReturn = purchaseReturn;
        assignScope(purchaseReturn.getTenantId(), purchaseReturn.getCompanyId());
    }
}

