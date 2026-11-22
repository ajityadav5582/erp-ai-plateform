package com.erp.business.procurement.supplierpayment.domain;

import com.erp.platform.data.entity.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

/**
 * A line-level allocation of a {@link SupplierPayment} against a single {@link com.erp.business.procurement.purchaseinvoice.domain.PurchaseInvoice}.
 *
 * <p>A single bank transfer or cheque may settle several invoices; each allocation
 * represents the amount directed towards one specific invoice.
 */
@Entity
@Table(
        name = "supplier_payment_allocations",
        indexes = {
                @Index(name = "idx_spa_tenant_company_payment",
                        columnList = "tenant_id, company_id, payment_id"),
                @Index(name = "idx_spa_tenant_company_invoice",
                        columnList = "tenant_id, company_id, purchase_invoice_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(callSuper = true, exclude = "payment")
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class SupplierPaymentAllocation extends TenantScopedEntity<Long> {

    private static final long serialVersionUID = 1L;
    private static final int MONEY_SCALE = 4;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private SupplierPayment payment;

    @NotNull(message = "Purchase invoice ID is required")
    @Column(name = "purchase_invoice_id", nullable = false)
    private Long purchaseInvoiceId;

    @NotBlank(message = "Invoice number is required")
    @Column(name = "invoice_number", nullable = false, length = 60)
    private String invoiceNumber;

    @NotNull(message = "Allocated amount is required")
    @Column(name = "allocated_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal allocatedAmount;

    @Builder.Default
    @Column(name = "discount_amount", precision = 19, scale = 4)
    private BigDecimal discountAmount = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * Creates an allocation line.
     */
    public static SupplierPaymentAllocation of(SupplierPayment payment,
                                               Long purchaseInvoiceId,
                                               String invoiceNumber,
                                               BigDecimal allocatedAmount,
                                               BigDecimal discountAmount,
                                               String notes) {
        if (purchaseInvoiceId == null) {
            throw new IllegalArgumentException("Purchase invoice ID is required");
        }
        if (allocatedAmount == null || allocatedAmount.signum() <= 0) {
            throw new IllegalArgumentException("Allocated amount must be greater than zero");
        }

        BigDecimal discount = discountAmount != null
                ? discountAmount.setScale(MONEY_SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        return SupplierPaymentAllocation.builder()
                .payment(payment)
                .purchaseInvoiceId(purchaseInvoiceId)
                .invoiceNumber(invoiceNumber != null ? invoiceNumber.trim() : "")
                .allocatedAmount(allocatedAmount.setScale(MONEY_SCALE, RoundingMode.HALF_UP))
                .discountAmount(discount)
                .notes(notes)
                .build();
    }
}

