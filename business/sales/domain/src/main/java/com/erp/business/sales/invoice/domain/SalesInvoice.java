package com.erp.business.sales.invoice.domain;

import com.erp.platform.common.nepali.date.NepaliDateConverter;
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
 * Sales invoice aggregate root: the IRD tax invoice the company issues to a
 * customer. This is the document the Nepal VAT return is built from.
 *
 * <p><strong>Aggregate boundary.</strong> An invoice owns its lines and nothing
 * else. The sales order, delivery and customer are referenced by id with a frozen
 * snapshot of their display fields, so a later rename never rewrites an issued
 * invoice - an issued tax invoice is a legal record and must be immutable in what
 * it says.
 *
 * <p><strong>IRD mandatory content.</strong> The buyer's PAN, the issue date in
 * both AD and Bikram Sambat, the sequential invoice number, and the VAT breakdown
 * (taxable / exempt / zero-rated / export, and the VAT amount) are all first-class
 * fields, because a valid Nepal tax invoice must carry them and the sales register
 * aggregates them.
 *
 * <p><strong>Totals are derived, never accepted.</strong> {@link #recalculateTotals()}
 * is the single writer of every money field, called after every line mutation, so
 * the header can never disagree with the lines. {@code balanceAmount} is derived
 * from {@code paidAmount}, which only {@link #recordPayment} writes.
 */
@Entity
@Table(
        name = "sales_invoices",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_sales_invoices_tenant_company_number",
                        columnNames = {"tenant_id", "company_id", "invoice_number"}
                ),
                @UniqueConstraint(
                        name = "uk_sales_invoices_order",
                        columnNames = {"tenant_id", "company_id", "sales_order_id"}
                )
        },
        indexes = {
                @Index(name = "idx_sales_invoices_tenant_company",
                        columnList = "tenant_id, company_id"),
                @Index(name = "idx_sales_invoices_customer",
                        columnList = "tenant_id, company_id, customer_id, invoice_date"),
                @Index(name = "idx_sales_invoices_order",
                        columnList = "tenant_id, company_id, sales_order_id, invoice_date"),
                @Index(name = "idx_sales_invoices_status",
                        columnList = "tenant_id, company_id, status, invoice_date")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class SalesInvoice extends TenantScopedEntity<Long> {

    private static final long serialVersionUID = 1L;

    private static final int MONEY_SCALE = 4;

    /**
     * IRD sequential invoice number, allocated by the document counter inside the
     * same transaction as the insert. Unique per tenant and company.
     */
    @NotBlank(message = "Invoice number is required")
    @Column(name = "invoice_number", nullable = false, length = 40)
    private String invoiceNumber;

    @Column(name = "fiscal_year_id")
    private Long fiscalYearId;

    @Column(name = "branch_id")
    private Long branchId;

    @NotNull(message = "Sales order is required")
    @Column(name = "sales_order_id", nullable = false)
    private Long salesOrderId;

    @NotBlank(message = "Sales order number is required")
    @Column(name = "so_number", nullable = false, length = 40)
    private String soNumber;

    /** The delivery this invoice bills, when raised from one. Optional. */
    @Column(name = "delivery_id")
    private Long deliveryId;

    @Column(name = "delivery_number", length = 40)
    private String deliveryNumber;

    @NotNull(message = "Customer is required")
    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "customer_code", nullable = false, length = 64)
    private String customerCode;

    @Column(name = "customer_name", nullable = false, length = 255)
    private String customerName;

    /** Frozen buyer PAN, so a B2B tax invoice carries the buyer's IRD PAN. */
    @Column(name = "customer_pan_number", length = 16)
    private String customerPanNumber;

    @NotNull(message = "Invoice date is required")
    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    /** Bikram Sambat rendering of {@link #invoiceDate}, derived at creation. */
    @Column(name = "invoice_date_np", nullable = false, length = 20)
    private String invoiceDateNp;

    @Column(name = "due_date")
    private LocalDate dueDate;

    /**
     * Whether this is a full "Tax Invoice". True for a B2B supply (VAT-registered
     * buyer), which must print the buyer's PAN; a simplified invoice to a consumer
     * sets this false.
     */
    @NotNull(message = "Tax invoice flag is required")
    @Column(name = "is_tax_invoice", nullable = false)
    @Builder.Default
    private Boolean isTaxInvoice = true;

    @NotBlank(message = "Currency is required")
    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "NPR";

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private SalesInvoiceStatus status = SalesInvoiceStatus.DRAFT;

    // ---- Derived totals. Written only by recalculateTotals(). ----
    /** Net of all lines (VAT-exclusive). */
    @Column(name = "subtotal_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal subtotalAmount = BigDecimal.ZERO.setScale(MONEY_SCALE);

    /** Net of standard-rated (TAXABLE) lines. The VAT-201 "taxable sales" figure. */
    @Column(name = "taxable_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal taxableAmount = BigDecimal.ZERO.setScale(MONEY_SCALE);

    /** Net of EXEMPT lines. */
    @Column(name = "exempt_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal exemptAmount = BigDecimal.ZERO.setScale(MONEY_SCALE);

    /** Net of ZERO_RATED and EXPORT lines. */
    @Column(name = "zero_rated_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal zeroRatedAmount = BigDecimal.ZERO.setScale(MONEY_SCALE);

    /** Output VAT: the sum of the lines' VAT. The VAT-201 "output tax" figure. */
    @Column(name = "vat_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal vatAmount = BigDecimal.ZERO.setScale(MONEY_SCALE);

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO.setScale(MONEY_SCALE);

    /** Cumulative settled amount. Written only by recordPayment. */
    @Column(name = "paid_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO.setScale(MONEY_SCALE);

    /** What is still owed: total minus paid. Derived. */
    @Column(name = "balance_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal balanceAmount = BigDecimal.ZERO.setScale(MONEY_SCALE);

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // ---- Workflow stamps: NULL until the transition happens. ----
    @Column(name = "issued_at")
    private Instant issuedAt;

    @Column(name = "issued_by", length = 100)
    private String issuedBy;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancelled_by", length = 100)
    private String cancelledBy;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @OneToMany(mappedBy = "salesInvoice", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    @OrderBy("lineNumber ASC")
    @ToString.Exclude
    @Builder.Default
    private List<SalesInvoiceLine> lines = new ArrayList<>();

    // -----------------------------------------------------------------------
    // Construction
    // -----------------------------------------------------------------------

    /**
     * Creates a draft invoice. The Bikram Sambat date is derived from the AD date
     * here, so the two can never drift apart.
     */
    public static SalesInvoice draft(String invoiceNumber, Long salesOrderId, String soNumber,
                                     Long deliveryId, String deliveryNumber,
                                     Long customerId, String customerCode, String customerName,
                                     String customerPanNumber, LocalDate invoiceDate, LocalDate dueDate,
                                     boolean isTaxInvoice, String currency, Long fiscalYearId,
                                     Long branchId, String notes) {
        Objects.requireNonNull(invoiceDate, "Invoice date is required");
        Objects.requireNonNull(salesOrderId, "Sales order is required");

        SalesInvoice invoice = SalesInvoice.builder()
                .invoiceNumber(invoiceNumber)
                .salesOrderId(salesOrderId)
                .soNumber(soNumber)
                .deliveryId(deliveryId)
                .deliveryNumber(deliveryNumber)
                .customerId(customerId)
                .customerCode(customerCode)
                .customerName(customerName)
                .customerPanNumber(customerPanNumber)
                .invoiceDate(invoiceDate)
                .invoiceDateNp(NepaliDateConverter.format(invoiceDate))
                .dueDate(dueDate)
                .isTaxInvoice(isTaxInvoice)
                .currency(currency != null && !currency.isBlank() ? currency.trim().toUpperCase() : "NPR")
                .fiscalYearId(fiscalYearId)
                .branchId(branchId)
                .notes(notes)
                .status(SalesInvoiceStatus.DRAFT)
                .lines(new ArrayList<>())
                .build();
        invoice.recalculateTotals();
        return invoice;
    }

    // -----------------------------------------------------------------------
    // Line mutation (draft only)
    // -----------------------------------------------------------------------

    public void addLine(SalesInvoiceLine line) {
        requireDraft("add a line to");
        if (lines.stream().anyMatch(existing -> existing.getLineNumber().equals(line.getLineNumber()))) {
            throw new IllegalStateException(
                    "Line number " + line.getLineNumber() + " is already used on this invoice");
        }
        line.assignScope(getTenantId(), getCompanyId());
        line.attachTo(this);
        lines.add(line);
        recalculateTotals();
    }

    public void removeLine(int lineNumber) {
        requireDraft("remove a line from");
        boolean removed = lines.removeIf(existing -> existing.getLineNumber() == lineNumber);
        if (!removed) {
            throw new IllegalStateException("No line " + lineNumber + " on this invoice");
        }
        recalculateTotals();
    }

    // -----------------------------------------------------------------------
    // Totals
    // -----------------------------------------------------------------------

    /**
     * Recomputes every derived money field from the lines, including the VAT
     * register breakdown, then re-derives the balance. The single writer.
     */
    public void recalculateTotals() {
        BigDecimal net = BigDecimal.ZERO;
        BigDecimal taxable = BigDecimal.ZERO;
        BigDecimal exempt = BigDecimal.ZERO;
        BigDecimal zeroRated = BigDecimal.ZERO;
        BigDecimal vat = BigDecimal.ZERO;
        for (SalesInvoiceLine line : lines) {
            net = net.add(line.getLineAmount());
            vat = vat.add(line.vatAmount());
            switch (line.getVatType()) {
                case TAXABLE -> taxable = taxable.add(line.getLineAmount());
                case EXEMPT -> exempt = exempt.add(line.getLineAmount());
                case ZERO_RATED, EXPORT -> zeroRated = zeroRated.add(line.getLineAmount());
            }
        }
        this.subtotalAmount = net.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        this.taxableAmount = taxable.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        this.exemptAmount = exempt.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        this.zeroRatedAmount = zeroRated.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        this.vatAmount = vat.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        this.totalAmount = net.add(vat).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        this.balanceAmount = this.totalAmount.subtract(this.paidAmount)
                .max(BigDecimal.ZERO).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    /** Issues the draft invoice to the customer. Requires at least one line. */
    public void issue(String actor) {
        requireDraft("issue");
        if (lines.isEmpty()) {
            throw new IllegalStateException("Cannot issue an invoice with no lines");
        }
        this.status = SalesInvoiceStatus.ISSUED;
        this.issuedAt = Instant.now();
        this.issuedBy = actor;
    }

    /**
     * Applies a payment and re-derives the state. Refuses to overpay, so the
     * balance cannot go negative and a receivable cannot quietly become a credit.
     */
    public void recordPayment(BigDecimal amount) {
        if (status == SalesInvoiceStatus.DRAFT || status == SalesInvoiceStatus.CANCELLED) {
            throw new IllegalStateException("Cannot pay an invoice in status " + status);
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }
        BigDecimal newPaid = paidAmount.add(amount);
        if (newPaid.compareTo(totalAmount) > 0) {
            throw new IllegalStateException(
                    "Paying " + amount.toPlainString() + " would exceed the balance of "
                            + balanceAmount.toPlainString() + " on invoice " + invoiceNumber);
        }
        this.paidAmount = newPaid.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        this.balanceAmount = totalAmount.subtract(this.paidAmount).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        this.status = SalesInvoiceStatus.fromPaidAmount(totalAmount, this.paidAmount);
    }

    /**
     * Reverses a previously applied payment (a cancelled customer receipt) and
     * re-derives the state. Refused if it would take the paid amount below zero.
     */
    public void reversePayment(BigDecimal amount) {
        if (status == SalesInvoiceStatus.DRAFT || status == SalesInvoiceStatus.CANCELLED) {
            throw new IllegalStateException("Cannot reverse a payment on an invoice in status " + status);
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Reversed amount must be greater than zero");
        }
        BigDecimal newPaid = paidAmount.subtract(amount);
        if (newPaid.signum() < 0) {
            throw new IllegalStateException(
                    "Reversing " + amount.toPlainString() + " would take invoice " + invoiceNumber
                            + " below zero paid");
        }
        this.paidAmount = newPaid.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        this.balanceAmount = totalAmount.subtract(this.paidAmount).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        this.status = SalesInvoiceStatus.fromPaidAmount(totalAmount, this.paidAmount);
    }

    /** Voids the invoice. Refused once any payment has been applied. */
    public void cancel(String actor, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A reason is required to cancel an invoice");
        }
        if (status == SalesInvoiceStatus.CANCELLED) {
            throw new IllegalStateException("Invoice " + invoiceNumber + " is already cancelled");
        }
        if (paidAmount.signum() > 0) {
            throw new IllegalStateException(
                    "Invoice " + invoiceNumber + " cannot be cancelled because "
                            + paidAmount.toPlainString() + " has already been paid against it");
        }
        this.status = SalesInvoiceStatus.CANCELLED;
        this.cancelledAt = Instant.now();
        this.cancelledBy = actor;
        this.cancellationReason = reason;
    }

    private void requireDraft(String action) {
        if (status != SalesInvoiceStatus.DRAFT) {
            throw new IllegalStateException("Cannot " + action + " an invoice in status " + status);
        }
    }
}
