package com.erp.business.pos.domain;

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
import jakarta.validation.constraints.Size;
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
 * POS transaction aggregate root: one complete checkout at the point of sale.
 *
 * <p><strong>Lifecycle.</strong>
 * <pre>
 *   OPEN ─────────────► COMPLETED
 *    │                       │
 *    └──────► VOIDED ◄───────┘
 * </pre>
 * A transaction is created in {@link PosTransactionStatus#OPEN} state as the
 * cashier adds items to the cart. Calling {@link #complete} atomically
 * finalises the payment — stock deduction and CBMS sync happen outside the
 * aggregate in the application service, inside the same database transaction.
 * A completed or open transaction may be {@link #voidTransaction voided}; the
 * application service is responsible for reversing the stock deduction when
 * voiding a completed transaction.
 *
 * <p><strong>Aggregate boundary.</strong> A transaction owns its lines and
 * nothing else. The session, register, customer and items are referenced by
 * id with a frozen snapshot of their display fields, so a later rename never
 * rewrites a completed sale record.
 *
 * <p><strong>Totals are derived, never accepted.</strong>
 * {@link #recalculateTotals()} is the single writer. It is called after every
 * line mutation, so the header total cannot disagree with the lines.
 *
 * <p><strong>ACID guarantee for completeTransaction.</strong>
 * The application service calls {@link #complete} and persists the aggregate,
 * then calls the inventory stock-issue port and persists the updated session,
 * all inside a single {@code @Transactional} method. Either all three writes
 * commit or none do.
 *
 * <p><strong>CBMS columns always carry DB defaults.</strong>
 * {@code cbmsSynced} and {@code cbmsRetryCount} both declare
 * {@code columnDefinition} to ensure the DDL emits a {@code DEFAULT} clause.
 * This prevents the "column does not exist" failure that occurs when Hibernate
 * adds a NOT NULL column without a default to a table that already has rows.
 *
 * @since 1.0.0
 */
@Entity
@Table(
        name = "pos_transactions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_pos_transactions_tenant_company_number",
                        columnNames = {"tenant_id", "company_id", "transaction_number"}
                )
        },
        indexes = {
                @Index(name = "idx_pos_transactions_tenant_company",
                        columnList = "tenant_id, company_id"),
                @Index(name = "idx_pos_transactions_session",
                        columnList = "tenant_id, company_id, session_id, transaction_date DESC"),
                @Index(name = "idx_pos_transactions_register",
                        columnList = "tenant_id, company_id, register_id, transaction_date DESC"),
                @Index(name = "idx_pos_transactions_status",
                        columnList = "tenant_id, company_id, status, transaction_date DESC"),
                @Index(name = "idx_pos_transactions_customer",
                        columnList = "tenant_id, company_id, customer_id, transaction_date DESC"),
                @Index(name = "idx_pos_transactions_cbms",
                        columnList = "tenant_id, company_id, cbms_synced, status")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(callSuper = true, exclude = "lines")
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class PosTransaction extends TenantScopedEntity<Long> {

    private static final long serialVersionUID = 1L;

    private static final int MONEY_SCALE = 4;
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);

    // -----------------------------------------------------------------------
    // Document identity
    // -----------------------------------------------------------------------

    /**
     * Allocated on {@link #complete} via the document-number service, not on
     * creation — an OPEN transaction that is abandoned never consumes a number.
     * {@code null} while status is {@link PosTransactionStatus#OPEN}.
     */
    @Column(name = "transaction_number", length = 40)
    private String transactionNumber;

    @NotNull(message = "Session is required")
    @Column(name = "session_id", nullable = false, updatable = false)
    private Long sessionId;

    @NotNull(message = "Register is required")
    @Column(name = "register_id", nullable = false, updatable = false)
    private Long registerId;

    @Column(name = "fiscal_year_id")
    private Long fiscalYearId;

    @Column(name = "branch_id")
    private Long branchId;

    // -----------------------------------------------------------------------
    // Transaction date (both calendars)
    // -----------------------------------------------------------------------

    @NotNull(message = "Transaction date is required")
    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    /** Bikram Sambat equivalent — derived and never accepted from the client. */
    @NotBlank(message = "Transaction date (BS) is required")
    @Column(name = "transaction_date_np", nullable = false, length = 20)
    private String transactionDateNp;

    // -----------------------------------------------------------------------
    // Customer snapshot (nullable = walk-in / anonymous)
    // -----------------------------------------------------------------------

    /** {@code null} for anonymous walk-in customers — IRD compliant. */
    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "customer_code", length = 64)
    private String customerCode;

    /** Defaults to "Walk-in Customer" when no customer is selected. */
    @NotBlank(message = "Customer name is required")
    @Column(name = "customer_name", nullable = false, length = 255)
    private String customerName;

    /** PAN number for B2B tax invoices; may be blank for retail customers. */
    @Column(name = "customer_pan_number", length = 16)
    private String customerPanNumber;

    // -----------------------------------------------------------------------
    // Cashier snapshot
    // -----------------------------------------------------------------------

    @NotBlank(message = "Cashier id is required")
    @Column(name = "cashier_id", nullable = false, length = 255)
    private String cashierId;

    @NotBlank(message = "Cashier name is required")
    @Column(name = "cashier_name", nullable = false, length = 255)
    private String cashierName;

    // -----------------------------------------------------------------------
    // Warehouse snapshot (copied from register; frozen for stock audit)
    // -----------------------------------------------------------------------

    @NotNull(message = "Warehouse id is required")
    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @NotBlank(message = "Warehouse code is required")
    @Column(name = "warehouse_code", nullable = false, length = 64)
    private String warehouseCode;

    @NotBlank(message = "Warehouse name is required")
    @Column(name = "warehouse_name", nullable = false, length = 255)
    private String warehouseName;

    // -----------------------------------------------------------------------
    // Totals (derived from lines — single writer is recalculateTotals)
    // -----------------------------------------------------------------------

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "NPR";

    @Column(name = "subtotal_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal subtotalAmount = ZERO;

    @Column(name = "taxable_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal taxableAmount = ZERO;

    @Column(name = "exempt_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal exemptAmount = ZERO;

    @Column(name = "zero_rated_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal zeroRatedAmount = ZERO;

    @Column(name = "vat_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal vatAmount = ZERO;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal totalAmount = ZERO;

    // -----------------------------------------------------------------------
    // Payment (set on complete)
    // -----------------------------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 30)
    private PosPaymentMethod paymentMethod;

    /** Card approval code, wallet transaction id, cheque number, etc. */
    @Size(max = 100)
    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    /** For CASH: the amount physically handed over. Null for non-cash methods. */
    @Column(name = "amount_tendered", precision = 19, scale = 4)
    private BigDecimal amountTendered;

    /** For CASH: change returned to the customer. Zero for exact tender. */
    @Column(name = "change_amount", precision = 19, scale = 4)
    private BigDecimal changeAmount;

    // -----------------------------------------------------------------------
    // Status
    // -----------------------------------------------------------------------

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PosTransactionStatus status;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "voided_at")
    private Instant voidedAt;

    @Column(name = "voided_by", length = 255)
    private String voidedBy;

    @Size(max = 500)
    @Column(name = "void_reason", length = 500)
    private String voidReason;

    // -----------------------------------------------------------------------
    // IRD / CBMS
    // -----------------------------------------------------------------------

    @Column(name = "is_tax_invoice", nullable = false,
            columnDefinition = "boolean default true")
    @Builder.Default
    private boolean isTaxInvoice = true;

    /**
     * Whether this transaction has been successfully pushed to Nepal IRD CBMS.
     * Always carries a DB-level DEFAULT to avoid NOT-NULL constraint failures
     * when the column is added to an existing table.
     */
    @Column(name = "cbms_synced", nullable = false,
            columnDefinition = "boolean default false")
    @Builder.Default
    private boolean cbmsSynced = false;

    /**
     * Number of failed CBMS sync attempts. Capped by the sync worker at a
     * configured maximum before abandoning retries and alerting.
     */
    @Column(name = "cbms_retry_count", nullable = false,
            columnDefinition = "integer default 0")
    @Builder.Default
    private int cbmsRetryCount = 0;

    @Column(name = "cbms_sync_time")
    private Instant cbmsSyncTime;

    @Size(max = 500)
    @Column(name = "cbms_response", length = 500)
    private String cbmsResponse;

    // -----------------------------------------------------------------------
    // Notes
    // -----------------------------------------------------------------------

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // -----------------------------------------------------------------------
    // Lines (owned by this aggregate)
    // -----------------------------------------------------------------------

    @OneToMany(
            mappedBy = "posTransactionId",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY,
            orphanRemoval = true
    )
    @OrderBy("lineNumber ASC")
    @Builder.Default
    private List<PosTransactionLine> lines = new ArrayList<>();

    // -----------------------------------------------------------------------
    // Factory method
    // -----------------------------------------------------------------------

    /**
     * Creates an empty OPEN transaction ready to receive items.
     *
     * <p>The transaction number is <em>not</em> allocated here — numbers are
     * only allocated for completed transactions so that abandoned carts do not
     * consume document numbers from the IRD-compliant sequential series.
     *
     * @param sessionId          the owning session id
     * @param registerId         the register this transaction is running on
     * @param warehouseId        warehouse from which stock will be deducted at complete
     * @param warehouseCode      warehouse code snapshot
     * @param warehouseName      warehouse name snapshot
     * @param cashierId          authenticated user id
     * @param cashierName        cashier display name
     * @param transactionDate    sale date (AD)
     * @param transactionDateNp  sale date in Bikram Sambat
     * @param fiscalYearId       fiscal year, may be null
     * @param branchId           branch, may be null
     * @param customerId         customer id, null for walk-in
     * @param customerCode       customer code snapshot
     * @param customerName       customer name; "Walk-in Customer" if anonymous
     * @param customerPanNumber  PAN for B2B tax invoices; may be null
     * @param currency           ISO currency code (default "NPR")
     * @param notes              optional notes
     * @return a new {@link PosTransaction} in {@link PosTransactionStatus#OPEN}
     */
    public static PosTransaction open(
            Long sessionId, Long registerId,
            Long warehouseId, String warehouseCode, String warehouseName,
            String cashierId, String cashierName,
            LocalDate transactionDate, String transactionDateNp,
            Long fiscalYearId, Long branchId,
            Long customerId, String customerCode, String customerName, String customerPanNumber,
            String currency, String notes) {

        Objects.requireNonNull(sessionId, "Session id is required");
        Objects.requireNonNull(registerId, "Register id is required");
        Objects.requireNonNull(warehouseId, "Warehouse id is required");
        Objects.requireNonNull(transactionDate, "Transaction date is required");

        String effectiveCustomerName = (customerName != null && !customerName.isBlank())
                ? customerName : "Walk-in Customer";
        String effectiveCurrency = (currency != null && !currency.isBlank())
                ? currency.trim().toUpperCase() : "NPR";

        return PosTransaction.builder()
                .sessionId(sessionId)
                .registerId(registerId)
                .warehouseId(warehouseId)
                .warehouseCode(warehouseCode)
                .warehouseName(warehouseName)
                .cashierId(cashierId)
                .cashierName(cashierName)
                .transactionDate(transactionDate)
                .transactionDateNp(transactionDateNp)
                .fiscalYearId(fiscalYearId)
                .branchId(branchId)
                .customerId(customerId)
                .customerCode(customerCode)
                .customerName(effectiveCustomerName)
                .customerPanNumber(customerPanNumber)
                .currency(effectiveCurrency)
                .status(PosTransactionStatus.OPEN)
                .build();
    }

    // -----------------------------------------------------------------------
    // Line mutation (OPEN only)
    // -----------------------------------------------------------------------

    /**
     * Adds an item line to the cart. Only allowed while the transaction is
     * {@link PosTransactionStatus#OPEN}.
     *
     * @param line the line to add
     * @throws IllegalStateException    if the transaction is not OPEN
     * @throws IllegalArgumentException if the line number is already in use
     */
    public void addLine(PosTransactionLine line) {
        requireStatus(PosTransactionStatus.OPEN, "add a line to");
        Objects.requireNonNull(line, "Line must not be null");

        if (lines.stream().anyMatch(existing -> existing.getLineNumber() == line.getLineNumber())) {
            throw new IllegalArgumentException(
                    "Line number " + line.getLineNumber() + " is already used on this transaction.");
        }
        line.attachTo(Objects.requireNonNull(getId(), "Transaction must be persisted before lines are added"));
        line.assignScope(getTenantId(), getCompanyId());
        lines.add(line);
        recalculateTotals();
    }

    /**
     * Removes a line by line number. Only allowed while OPEN.
     *
     * @param lineNumber the 1-based line number to remove
     * @throws IllegalStateException    if the transaction is not OPEN
     * @throws IllegalArgumentException if no line with that number exists
     */
    public void removeLine(int lineNumber) {
        requireStatus(PosTransactionStatus.OPEN, "remove a line from");
        boolean removed = lines.removeIf(line -> line.getLineNumber() == lineNumber);
        if (!removed) {
            throw new IllegalArgumentException(
                    "No line with number " + lineNumber + " exists on this transaction.");
        }
        recalculateTotals();
    }

    /**
     * Updates the quantity of an existing line. Only allowed while OPEN.
     *
     * @param lineNumber  the line to update
     * @param newQuantity the new quantity (must be > 0)
     */
    public void updateLineQuantity(int lineNumber, BigDecimal newQuantity) {
        requireStatus(PosTransactionStatus.OPEN, "update a line on");
        PosTransactionLine line = findLineOrThrow(lineNumber);
        line.updateQuantity(newQuantity);
        recalculateTotals();
    }

    /**
     * Applies or changes a line-level discount. Only allowed while OPEN.
     *
     * @param lineNumber    the line to update
     * @param discountPct   discount percentage 0–100
     */
    public void applyLineDiscount(int lineNumber, BigDecimal discountPct) {
        requireStatus(PosTransactionStatus.OPEN, "apply a discount on");
        PosTransactionLine line = findLineOrThrow(lineNumber);
        line.updateDiscount(discountPct);
        recalculateTotals();
    }

    // -----------------------------------------------------------------------
    // Totals — single writer
    // -----------------------------------------------------------------------

    /**
     * Recomputes every derived money field from the current lines.
     * The single writer — called after every line mutation so the header
     * totals are always consistent with the line set.
     */
    public void recalculateTotals() {
        BigDecimal subtotal = ZERO;
        BigDecimal taxable = ZERO;
        BigDecimal exempt = ZERO;
        BigDecimal zeroRated = ZERO;
        BigDecimal vat = ZERO;

        for (PosTransactionLine line : lines) {
            subtotal = subtotal.add(line.getLineAmount());
            vat = vat.add(line.vatAmount());
            switch (line.getVatType()) {
                case TAXABLE  -> taxable   = taxable.add(line.getLineAmount());
                case EXEMPT   -> exempt    = exempt.add(line.getLineAmount());
                case ZERO_RATED, EXPORT -> zeroRated = zeroRated.add(line.getLineAmount());
            }
        }

        this.subtotalAmount  = subtotal.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        this.taxableAmount   = taxable.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        this.exemptAmount    = exempt.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        this.zeroRatedAmount = zeroRated.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        this.vatAmount       = vat.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        this.totalAmount     = subtotal.add(vat).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    /**
     * Completes the transaction and records payment.
     *
     * <p>This method mutates the aggregate state only. The caller (application
     * service) is responsible for:
     * <ol>
     *   <li>Deducting stock via {@code StockMovementService.postIssue()} <em>before</em>
     *       calling this method.</li>
     *   <li>Allocating the {@code transactionNumber} via {@code DocumentNumberService}
     *       and passing it here.</li>
     *   <li>Updating the session's running totals.</li>
     *   <li>Persisting all changes in the same {@code @Transactional} call.</li>
     * </ol>
     *
     * @param transactionNumber pre-allocated document number (e.g. POS-000042)
     * @param paymentMethod     how the customer paid
     * @param amountTendered    for CASH: amount handed over; for others, equals totalAmount
     * @param paymentReference  approval code / wallet txn id; may be null
     * @param actor             user id of the cashier completing the sale
     * @throws IllegalStateException    if the transaction is not OPEN
     * @throws IllegalArgumentException if no lines exist or the tendered amount
     *                                  is less than the total
     */
    public void complete(
            String transactionNumber, PosPaymentMethod paymentMethod,
            BigDecimal amountTendered, String paymentReference, String actor) {

        requireStatus(PosTransactionStatus.OPEN, "complete");

        if (lines.isEmpty()) {
            throw new IllegalStateException(
                    "Cannot complete a transaction with no items. Please add at least one item to the cart.");
        }
        Objects.requireNonNull(paymentMethod, "Payment method is required");

        BigDecimal tendered = (amountTendered != null ? amountTendered : totalAmount)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal total = totalAmount;

        if (tendered.compareTo(total) < 0) {
            throw new IllegalArgumentException(
                    "Amount tendered (" + tendered.toPlainString()
                            + ") is less than the total due (" + total.toPlainString()
                            + "). Please collect the full amount before completing the sale.");
        }

        BigDecimal change = paymentMethod == PosPaymentMethod.CASH
                ? tendered.subtract(total).setScale(MONEY_SCALE, RoundingMode.HALF_UP)
                : ZERO;

        this.transactionNumber = Objects.requireNonNull(transactionNumber,
                "Transaction number must be allocated before completing");
        this.paymentMethod    = paymentMethod;
        this.amountTendered   = tendered;
        this.changeAmount     = change;
        this.paymentReference = paymentReference;
        this.completedAt      = Instant.now();
        this.status           = PosTransactionStatus.COMPLETED;
    }

    /**
     * Voids this transaction.
     *
     * <p>If the transaction was {@link PosTransactionStatus#COMPLETED}, the
     * caller (application service) must reverse the stock issue via
     * {@code StockMovementService.reverseIssue()} <em>before</em> calling this
     * method and update the session's running totals.
     *
     * @param actor  user id performing the void
     * @param reason mandatory explanation for the void (audit trail)
     * @throws IllegalStateException    if the transaction is already VOIDED
     * @throws IllegalArgumentException if reason is blank
     */
    public void voidTransaction(String actor, String reason) {
        if (status == PosTransactionStatus.VOIDED) {
            throw new IllegalStateException(
                    "Transaction " + transactionNumber + " is already voided and cannot be voided again.");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                    "A reason is required to void a transaction. Please provide a brief explanation.");
        }
        this.voidedAt = Instant.now();
        this.voidedBy = actor;
        this.voidReason = reason;
        this.status = PosTransactionStatus.VOIDED;
    }

    // -----------------------------------------------------------------------
    // CBMS sync tracking (called by PosCbmsSyncWorker)
    // -----------------------------------------------------------------------

    /**
     * Records a successful CBMS sync. Marks the transaction as synced and
     * stores the IRD response for audit purposes.
     *
     * @param response the raw CBMS response body (truncated to 500 chars)
     */
    public void markCbmsSynced(String response) {
        this.cbmsSynced = true;
        this.cbmsSyncTime = Instant.now();
        this.cbmsResponse = truncate(response, 500);
    }

    /**
     * Records a failed CBMS sync attempt, incrementing the retry counter.
     * The sync worker will stop retrying once a configured maximum is reached.
     *
     * @param error the error message or CBMS response body
     */
    public void recordCbmsSyncFailure(String error) {
        this.cbmsSynced = false;
        this.cbmsRetryCount = (cbmsRetryCount + 1);
        this.cbmsResponse = truncate(error, 500);
    }

    // -----------------------------------------------------------------------
    // Query helpers
    // -----------------------------------------------------------------------

    /** Returns whether the transaction is currently open (items being added). */
    public boolean isOpen() {
        return status == PosTransactionStatus.OPEN;
    }

    /** Returns whether the transaction has been completed (payment taken). */
    public boolean isCompleted() {
        return status == PosTransactionStatus.COMPLETED;
    }

    /** Returns the number of lines currently in the cart. */
    public int lineCount() {
        return lines.size();
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private void requireStatus(PosTransactionStatus required, String action) {
        if (status != required) {
            throw new IllegalStateException(
                    "Cannot " + action + " a transaction in status " + status
                            + ". Transaction must be " + required + " to perform this action.");
        }
    }

    private PosTransactionLine findLineOrThrow(int lineNumber) {
        return lines.stream()
                .filter(line -> line.getLineNumber() == lineNumber)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No line with number " + lineNumber + " exists on this transaction."));
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
