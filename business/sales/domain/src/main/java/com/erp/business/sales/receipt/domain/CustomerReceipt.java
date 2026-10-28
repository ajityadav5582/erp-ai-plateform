package com.erp.business.sales.receipt.domain;

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
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Customer receipt aggregate root: money received against a sales invoice.
 *
 * <p>The receivable side of a sale. A receipt is recorded POSTED on creation -
 * receiving money is the act, there is no draft - and it carries a frozen snapshot
 * of the invoice and customer it settles, so a later rename never rewrites a
 * booked receipt.
 *
 * <p><strong>Applying the money is the service's job.</strong> The aggregate owns
 * its own status and amount; the application service calls
 * {@code SalesInvoiceService.recordPayment} inside the same transaction, so a
 * receipt and the invoice's reduced balance commit together or not at all.
 */
@Entity
@Table(
        name = "customer_receipts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_customer_receipts_tenant_company_number",
                        columnNames = {"tenant_id", "company_id", "receipt_number"}
                )
        },
        indexes = {
                @Index(name = "idx_customer_receipts_tenant_company",
                        columnList = "tenant_id, company_id"),
                @Index(name = "idx_customer_receipts_invoice",
                        columnList = "tenant_id, company_id, invoice_id, receipt_date"),
                @Index(name = "idx_customer_receipts_customer",
                        columnList = "tenant_id, company_id, customer_id, receipt_date")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class CustomerReceipt extends TenantScopedEntity<Long> {

    private static final long serialVersionUID = 1L;

    private static final int MONEY_SCALE = 4;

    @NotBlank(message = "Receipt number is required")
    @Column(name = "receipt_number", nullable = false, length = 40)
    private String receiptNumber;

    @Column(name = "fiscal_year_id")
    private Long fiscalYearId;

    @Column(name = "branch_id")
    private Long branchId;

    @NotNull(message = "Invoice is required")
    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;

    @NotBlank(message = "Invoice number is required")
    @Column(name = "invoice_number", nullable = false, length = 40)
    private String invoiceNumber;

    @NotNull(message = "Customer is required")
    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "customer_code", nullable = false, length = 64)
    private String customerCode;

    @Column(name = "customer_name", nullable = false, length = 255)
    private String customerName;

    @NotNull(message = "Amount is required")
    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @NotNull(message = "Payment method is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 32)
    private PaymentMethod paymentMethod;

    @NotNull(message = "Receipt date is required")
    @Column(name = "receipt_date", nullable = false)
    private LocalDate receiptDate;

    /** Free reference such as a cheque number or transaction id. */
    @Column(name = "reference", length = 100)
    private String reference;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ReceiptStatus status = ReceiptStatus.POSTED;

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
     * Creates a posted receipt. The amount must be positive; the caller has already
     * confirmed it does not exceed the invoice balance (the invoice refuses an
     * overpayment, and both are written in one transaction).
     */
    public static CustomerReceipt posted(String receiptNumber, Long invoiceId, String invoiceNumber,
                                         Long customerId, String customerCode, String customerName,
                                         BigDecimal amount, PaymentMethod paymentMethod, LocalDate receiptDate,
                                         Long fiscalYearId, Long branchId, String reference, String notes,
                                         String actor) {
        Objects.requireNonNull(invoiceId, "Invoice is required");
        Objects.requireNonNull(paymentMethod, "Payment method is required");
        Objects.requireNonNull(receiptDate, "Receipt date is required");
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Receipt amount must be greater than zero");
        }

        return CustomerReceipt.builder()
                .receiptNumber(receiptNumber)
                .invoiceId(invoiceId)
                .invoiceNumber(invoiceNumber)
                .customerId(customerId)
                .customerCode(customerCode)
                .customerName(customerName)
                .amount(amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP))
                .paymentMethod(paymentMethod)
                .receiptDate(receiptDate)
                .fiscalYearId(fiscalYearId)
                .branchId(branchId)
                .reference(reference)
                .notes(notes)
                .status(ReceiptStatus.POSTED)
                .postedAt(Instant.now())
                .postedBy(actor)
                .build();
    }

    /** Reverses a posted receipt. A reason is required. */
    public void cancel(String actor, String reason) {
        if (status != ReceiptStatus.POSTED) {
            throw new IllegalStateException("Only a posted receipt can be cancelled");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A reason is required to cancel a receipt");
        }
        this.status = ReceiptStatus.CANCELLED;
        this.cancelledAt = Instant.now();
        this.cancelledBy = actor;
        this.cancellationReason = reason;
    }
}
