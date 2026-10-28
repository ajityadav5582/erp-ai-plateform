package com.erp.business.procurement.supplierpayment.domain;

import com.erp.business.procurement.purchaseinvoice.domain.PurchaseInvoice;
import com.erp.platform.data.entity.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
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

/**
 * Money paid to a supplier against an invoice.
 *
 * <p><strong>It does not reduce the balance itself.</strong> The payable lives on
 * {@link PurchaseInvoice}; this document records that the money moved and names the
 * invoice it was for. Keeping the deduction on the invoice means the balance has exactly
 * one writer, and reversing a payment is a matter of the invoice refusing a negative
 * amount rather than two aggregates trying to stay in step.
 *
 * <p><strong>Currency is copied from the invoice.</strong> A payment settles a payable
 * denominated in one currency; it does not choose its own.
 */
@Entity
@Table(
        name = "supplier_payments",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_supplier_payments_tenant_company_number",
                columnNames = {"tenant_id", "company_id", "payment_number"}
        ),
        indexes = {
                @Index(name = "idx_supplier_payments_tenant_company",
                        columnList = "tenant_id, company_id"),
                @Index(name = "idx_supplier_payments_invoice",
                        columnList = "tenant_id, company_id, purchase_invoice_id"),
                @Index(name = "idx_supplier_payments_supplier",
                        columnList = "tenant_id, company_id, supplier_id, payment_date"),
                @Index(name = "idx_supplier_payments_fiscal_year",
                        columnList = "tenant_id, company_id, fiscal_year_id, payment_date")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class SupplierPayment extends TenantScopedEntity<Long> {

    private static final long serialVersionUID = 1L;

    private static final int MONEY_SCALE = 4;

    @NotBlank(message = "Payment number is required")
    @Column(name = "payment_number", nullable = false, length = 40)
    private String paymentNumber;

    @Column(name = "fiscal_year_id")
    private Long fiscalYearId;

    @Column(name = "branch_id")
    private Long branchId;

    /** The invoice being settled. */
    @Column(name = "purchase_invoice_id", nullable = false)
    private Long purchaseInvoiceId;

    @Column(name = "invoice_number", nullable = false, length = 60)
    private String invoiceNumber;

    @NotNull(message = "Payment date is required")
    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @NotNull(message = "Supplier is required")
    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "supplier_code", nullable = false, length = 64)
    private String supplierCode;

    @Column(name = "supplier_name", nullable = false, length = 255)
    private String supplierName;

    @NotNull(message = "Amount is required")
    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "NPR";

    @NotNull(message = "Payment method is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    /** Cheque number, bank transaction reference, or whatever the method needs. */
    @Column(name = "reference_no", length = 100)
    private String referenceNo;

    /** Branch or account the money left from, for bank reconciliation. */
    @Column(name = "paid_from", length = 255)
    private String paidFrom;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private SupplierPaymentStatus status = SupplierPaymentStatus.POSTED;

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

    /**
     * Creates a posted payment against an invoice.
     *
     * @param paymentNumber the allocated document number
     * @param paymentDate   the date the money moved, defaults to today when null
     * @param amount        the amount paid, must be positive
     * @param method        how the money was sent
     * @param referenceNo   the cheque or transaction reference, may be null
     * @param paidFrom      the branch or account paid from, may be null
     * @param invoice       the invoice being settled
     * @param notes         free text notes, may be null
     * @param actor         the acting user id
     * @return a new unsaved payment
     */
    public static SupplierPayment post(String paymentNumber, LocalDate paymentDate,
                                       BigDecimal amount, PaymentMethod method,
                                       String referenceNo, String paidFrom,
                                       PurchaseInvoice invoice, String notes, String actor) {

        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }
        if (method == null) {
            throw new IllegalArgumentException("A payment method is required");
        }

        return SupplierPayment.builder()
                .paymentNumber(paymentNumber)
                .paymentDate(paymentDate == null ? LocalDate.now() : paymentDate)
                .fiscalYearId(invoice.getFiscalYearId())
                .branchId(invoice.getBranchId())
                .purchaseInvoiceId(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .supplierId(invoice.getSupplierId())
                .supplierCode(invoice.getSupplierCode())
                .supplierName(invoice.getSupplierName())
                .amount(amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP))
                .currency(invoice.getCurrency())
                .paymentMethod(method)
                .referenceNo(referenceNo)
                .paidFrom(paidFrom)
                .status(SupplierPaymentStatus.POSTED)
                .notes(notes)
                .postedAt(Instant.now())
                .postedBy(actor)
                .build();
    }

    /**
     * Reverses this payment. A reason is required.
     *
     * <p>The caller is responsible for restoring the invoice balance in the same
     * transaction. There is no state on this document that could do it alone: reversing a
     * payment means the invoice was never as paid as it claimed, and only the invoice
     * can give that back.
     *
     * @param actor  the acting user id
     * @param reason why the payment is being reversed
     */
    public void cancel(String actor, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A reason is required to reverse a payment");
        }
        if (status == SupplierPaymentStatus.CANCELLED) {
            throw new IllegalStateException("Payment " + paymentNumber + " is already reversed");
        }
        this.status = SupplierPaymentStatus.CANCELLED;
        this.cancelledAt = Instant.now();
        this.cancelledBy = actor;
        this.cancellationReason = reason;
    }
}
