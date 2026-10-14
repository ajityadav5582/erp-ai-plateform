package com.erp.business.pos.domain;

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
import jakarta.validation.constraints.PositiveOrZero;
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
import java.util.Objects;

/**
 * A cashier shift on a POS register.
 *
 * <p><strong>Aggregate boundary.</strong> A session owns its own totals — it
 * does not own transactions. Transactions reference the session by id.
 * Session totals ({@code totalCashSales}, {@code totalCardSales}, etc.) are
 * maintained by the service layer when a transaction is completed or voided,
 * inside the same database transaction. This keeps the session's numbers
 * consistent without a full re-aggregation on every read.
 *
 * <p><strong>One open session per register.</strong> Enforced by:
 * <ol>
 *   <li>A partial unique index {@code uk_pos_sessions_register_open} on the
 *       {@code pos_sessions} table (DB-level guarantee).</li>
 *   <li>{@link PosRegister#recordSessionOpened} guard (app-level early failure
 *       with a user-friendly message before the DB sees the insert).</li>
 * </ol>
 *
 * <p><strong>Cash variance.</strong> Calculated at close time:
 * {@code cashVariance = closingCashAmount - expectedCash},
 * where {@code expectedCash = openingCashAmount + totalCashSales}.
 * A positive variance means more cash than expected (over); negative means short.
 *
 * @since 1.0.0
 */
@Entity
@Table(
        name = "pos_sessions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_pos_sessions_tenant_company_number",
                        columnNames = {"tenant_id", "company_id", "session_number"}
                )
        },
        indexes = {
                @Index(name = "idx_pos_sessions_tenant_company",
                        columnList = "tenant_id, company_id"),
                @Index(name = "idx_pos_sessions_register",
                        columnList = "tenant_id, company_id, register_id, opened_at DESC"),
                @Index(name = "idx_pos_sessions_status",
                        columnList = "tenant_id, company_id, status"),
                @Index(name = "idx_pos_sessions_cashier",
                        columnList = "tenant_id, company_id, cashier_id, opened_at DESC")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class PosSession extends TenantScopedEntity<Long> {

    private static final long serialVersionUID = 1L;

    private static final int MONEY_SCALE = 4;
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);

    // -----------------------------------------------------------------------
    // Identity
    // -----------------------------------------------------------------------

    /** Human-readable session number (e.g. SES-000042). */
    @NotBlank(message = "Session number is required")
    @Column(name = "session_number", nullable = false, length = 40, updatable = false)
    private String sessionNumber;

    /** Register this session belongs to. */
    @NotNull(message = "Register is required")
    @Column(name = "register_id", nullable = false, updatable = false)
    private Long registerId;

    @NotBlank(message = "Register code snapshot is required")
    @Column(name = "register_code", nullable = false, length = 40)
    private String registerCode;

    @NotBlank(message = "Register name snapshot is required")
    @Column(name = "register_name", nullable = false, length = 255)
    private String registerName;

    /** Fiscal year the session belongs to. Nullable pre-assignment. */
    @Column(name = "fiscal_year_id")
    private Long fiscalYearId;

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
    // Timing
    // -----------------------------------------------------------------------

    @NotNull(message = "Opened-at timestamp is required")
    @Column(name = "opened_at", nullable = false, updatable = false)
    private Instant openedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    // -----------------------------------------------------------------------
    // Cash drawer
    // -----------------------------------------------------------------------

    @NotNull(message = "Opening cash amount is required")
    @PositiveOrZero(message = "Opening cash amount must not be negative")
    @Column(name = "opening_cash_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal openingCashAmount;

    /** Amount physically counted at session close. Null until session is closed. */
    @Column(name = "closing_cash_amount", precision = 19, scale = 4)
    private BigDecimal closingCashAmount;

    /** Expected cash = openingCashAmount + totalCashSales. Computed at close. */
    @Column(name = "expected_cash", precision = 19, scale = 4)
    private BigDecimal expectedCash;

    /**
     * Cash variance at close = closingCashAmount - expectedCash.
     * Positive = over; negative = short.
     */
    @Column(name = "cash_variance", precision = 19, scale = 4)
    private BigDecimal cashVariance;

    // -----------------------------------------------------------------------
    // Running totals (maintained by the service on every transaction event)
    // -----------------------------------------------------------------------

    @Builder.Default
    @Column(name = "total_transactions", nullable = false)
    private int totalTransactions = 0;

    @Builder.Default
    @Column(name = "total_voided", nullable = false)
    private int totalVoided = 0;

    @Builder.Default
    @Column(name = "total_cash_sales", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalCashSales = ZERO;

    @Builder.Default
    @Column(name = "total_card_sales", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalCardSales = ZERO;

    @Builder.Default
    @Column(name = "total_wallet_sales", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalWalletSales = ZERO;

    @Builder.Default
    @Column(name = "total_other_sales", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalOtherSales = ZERO;

    @Builder.Default
    @Column(name = "total_vat_collected", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalVatCollected = ZERO;

    @Builder.Default
    @Column(name = "gross_sales", nullable = false, precision = 19, scale = 4)
    private BigDecimal grossSales = ZERO;

    // -----------------------------------------------------------------------
    // Status
    // -----------------------------------------------------------------------

    @NotNull(message = "Session status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PosSessionStatus status;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "close_notes", columnDefinition = "TEXT")
    private String closeNotes;

    // -----------------------------------------------------------------------
    // Factory method
    // -----------------------------------------------------------------------

    /**
     * Opens a new cashier session on a register. The tenant/company scope must
     * be assigned via {@link #assignScope(Long, Long)} before persisting.
     *
     * @param sessionNumber     pre-allocated document number (e.g. SES-000042)
     * @param registerId        the owning register's id
     * @param registerCode      register code snapshot for display
     * @param registerName      register name snapshot for display
     * @param cashierId         authenticated user id opening the session
     * @param cashierName       cashier's display name
     * @param openingCashAmount physical cash placed in the drawer at open
     * @param fiscalYearId      fiscal year, may be null
     * @param notes             optional opening notes
     * @return a new {@link PosSession} in {@link PosSessionStatus#OPEN} state
     */
    public static PosSession open(
            String sessionNumber, Long registerId, String registerCode, String registerName,
            String cashierId, String cashierName,
            BigDecimal openingCashAmount, Long fiscalYearId, String notes) {

        Objects.requireNonNull(sessionNumber, "Session number is required");
        Objects.requireNonNull(registerId, "Register id is required");
        Objects.requireNonNull(cashierId, "Cashier id is required");

        BigDecimal opening = openingCashAmount != null
                ? openingCashAmount.setScale(MONEY_SCALE, RoundingMode.HALF_UP)
                : ZERO;

        return PosSession.builder()
                .sessionNumber(sessionNumber)
                .registerId(registerId)
                .registerCode(registerCode)
                .registerName(registerName)
                .cashierId(cashierId)
                .cashierName(cashierName)
                .openingCashAmount(opening)
                .fiscalYearId(fiscalYearId)
                .status(PosSessionStatus.OPEN)
                .openedAt(Instant.now())
                .notes(notes)
                .build();
    }

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    /**
     * Closes this session, computing the cash drawer variance.
     *
     * @param closingCashAmount physical cash counted at close
     * @param closeNotes        optional notes (e.g. denominations counted)
     * @throws IllegalStateException if the session is already closed
     */
    public void close(BigDecimal closingCashAmount, String closeNotes) {
        requireOpen("close");
        BigDecimal closing = closingCashAmount != null
                ? closingCashAmount.setScale(MONEY_SCALE, RoundingMode.HALF_UP)
                : ZERO;
        BigDecimal expected = openingCashAmount.add(totalCashSales);

        this.closingCashAmount = closing;
        this.expectedCash = expected.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        this.cashVariance = closing.subtract(expected).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        this.closedAt = Instant.now();
        this.closeNotes = closeNotes;
        this.status = PosSessionStatus.CLOSED;
    }

    // -----------------------------------------------------------------------
    // Running total maintenance (called by the service on transaction events)
    // -----------------------------------------------------------------------

    /**
     * Updates session totals when a transaction is completed. Called by the
     * application service inside the same transaction as the completion.
     *
     * @param paymentMethod the method used to pay
     * @param totalAmount   the transaction's gross total (including VAT)
     * @param vatAmount     the VAT portion of the total
     */
    public void recordCompletedTransaction(
            PosPaymentMethod paymentMethod, BigDecimal totalAmount, BigDecimal vatAmount) {
        requireOpen("record a completed transaction on");
        Objects.requireNonNull(paymentMethod, "Payment method is required");

        BigDecimal amount = scale(totalAmount);
        BigDecimal vat = scale(vatAmount);

        this.totalTransactions++;
        this.grossSales = grossSales.add(amount);
        this.totalVatCollected = totalVatCollected.add(vat);

        switch (paymentMethod) {
            case CASH -> this.totalCashSales = totalCashSales.add(amount);
            case CARD -> this.totalCardSales = totalCardSales.add(amount);
            case DIGITAL_WALLET -> this.totalWalletSales = totalWalletSales.add(amount);
            default -> this.totalOtherSales = totalOtherSales.add(amount);
        }
    }

    /**
     * Reverses session totals when a completed transaction is voided. Called
     * by the application service inside the same transaction as the void.
     *
     * @param paymentMethod the method that was used to pay
     * @param totalAmount   the transaction's gross total
     * @param vatAmount     the VAT portion
     */
    public void recordVoidedTransaction(
            PosPaymentMethod paymentMethod, BigDecimal totalAmount, BigDecimal vatAmount) {
        requireOpen("record a voided transaction on");
        Objects.requireNonNull(paymentMethod, "Payment method is required");

        BigDecimal amount = scale(totalAmount);
        BigDecimal vat = scale(vatAmount);

        this.totalVoided++;
        this.grossSales = grossSales.subtract(amount).max(ZERO);
        this.totalVatCollected = totalVatCollected.subtract(vat).max(ZERO);

        switch (paymentMethod) {
            case CASH -> this.totalCashSales = totalCashSales.subtract(amount).max(ZERO);
            case CARD -> this.totalCardSales = totalCardSales.subtract(amount).max(ZERO);
            case DIGITAL_WALLET -> this.totalWalletSales = totalWalletSales.subtract(amount).max(ZERO);
            default -> this.totalOtherSales = totalOtherSales.subtract(amount).max(ZERO);
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void requireOpen(String action) {
        if (status != PosSessionStatus.OPEN) {
            throw new IllegalStateException(
                    "Cannot " + action + " session '" + sessionNumber
                            + "' because it is already " + status + ".");
        }
    }

    private static BigDecimal scale(BigDecimal value) {
        return (value != null ? value : BigDecimal.ZERO)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /** Returns whether this session is currently open. */
    public boolean isOpen() {
        return status == PosSessionStatus.OPEN;
    }
}
