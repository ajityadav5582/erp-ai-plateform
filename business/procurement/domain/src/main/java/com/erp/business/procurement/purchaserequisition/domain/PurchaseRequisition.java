package com.erp.business.procurement.purchaserequisition.domain;

import com.erp.platform.data.entity.TenantScopedEntity;
import com.erp.platform.common.nepali.date.NepaliDate;
import com.erp.platform.common.nepali.date.NepaliDateConverter;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
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
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Branch purchase requisition: the internal statement of what one branch needs.
 *
 * <p><strong>Aggregate boundary.</strong> A requisition owns its lines, its
 * approval chain, its status history and its links to purchase orders.
 * {@code PurchaseRequisitionApprovalPolicy} is deliberately outside the
 * aggregate: it is company configuration, shared across many requisitions, and
 * must never be loaded or locked through a document.
 *
 * <p><strong>Why there is no supplier.</strong> This document carries no supplier
 * id, name or agreed price. Supplier selection happens later, at RFQ stage,
 * because one requisition line is routinely split across several suppliers and
 * one RFQ routinely consolidates lines from several branches. The price held
 * here is an <em>estimate</em> for budget planning, never a commercial
 * commitment; conflating the two is how procurement systems end up paying
 * estimated prices.
 *
 * <p><strong>Totals are derived, never accepted.</strong> There is no
 * {@code setEstimatedTotal(...)}. {@link #recalculateTotals()} is the single
 * writer, called after every line mutation, so the header total cannot disagree
 * with the lines that produce it. Values are still persisted because branch
 * budget dashboards must not aggregate the line table on every read.
 *
 * <p><strong>Estimate and commitment are keyed on different quantities.</strong>
 * {@code estimated*} totals are derived from each line's <em>requested</em>
 * quantity, because they exist from the moment the request is drafted and are
 * what the approval-policy amount bands are resolved against. {@code approved*}
 * and {@code ordered*} totals are derived from the <em>approved</em> quantity,
 * because only granted demand may consume budget. Both families are gross, so
 * estimated and committed figures are directly comparable without a tax
 * adjustment that nobody would remember to apply.
 *
 * <p><strong>Budget is consumed at approval, not at order.</strong>
 * {@link #recalculateTotals()} moves value from the estimated columns into
 * {@code approvedTotalAmount} only when the header reaches
 * {@link PurchaseRequisitionStatus#APPROVED}. Estimating is free; committing
 * budget is not.
 *
 * <p><strong>Concurrency.</strong> The inherited {@code version} column provides
 * optimistic locking, so two approvers acting on the same document produce an
 * {@code OptimisticLockingFailureException} on the loser rather than a silent
 * lost update.
 *
 * @since 1.0.0
 */
@Entity
@Table(
        name = "purchase_requisitions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_purchase_requisitions_tenant_company_number",
                columnNames = {"tenant_id", "company_id", "pr_number"}
        ),
        indexes = {
                @Index(name = "idx_purchase_requisitions_scope",
                        columnList = "tenant_id, company_id"),
                @Index(name = "idx_purchase_requisitions_branch",
                        columnList = "tenant_id, company_id, branch_id, request_date"),
                @Index(name = "idx_purchase_requisitions_approval_queue",
                        columnList = "tenant_id, company_id, status, submitted_at"),
                @Index(name = "idx_purchase_requisitions_fiscal_year",
                        columnList = "tenant_id, company_id, fiscal_year_id, request_date"),
                @Index(name = "idx_purchase_requisitions_np_year",
                        columnList = "tenant_id, company_id, request_date_np_year")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class PurchaseRequisition extends TenantScopedEntity<Long> {

    private static final long serialVersionUID = 1L;

    private static final int MONEY_SCALE = 4;

    /**
     * Human readable document number, unique per tenant and company.
     *
     * <p>Allocated by the application layer from the shared document sequence
     * table inside the same transaction as the insert. Generating it here in
     * Java with {@code max + 1} would reintroduce the duplicate-number race
     * that a sequence table exists to remove.
     */
    @NotBlank(message = "Requisition number is required")
    @Column(name = "pr_number", nullable = false, length = 40)
    private String prNumber;

    /**
     * The branch raising this demand.
     *
     * <p>This is a reference into the identity service, not a foreign key.
     * A cross-service database constraint would couple the two services'
     * migration timelines and make an identity schema change a procurement
     * release. The service validates that the branch belongs to this tenant and
     * company before persisting.
     *
     * <p>Branch is a first-class accountability dimension, not a reporting
     * filter: branch budget is consumed here, branch heads approve here, and
     * company roll-up aggregates from here.
     */
    @NotNull(message = "Branch is required")
    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    /** Optional cost centre within the branch. Also an identity reference. */
    @Column(name = "department_id")
    private Long departmentId;

    /**
     * Fiscal year for budget and period reporting.
     *
     * <p>Assigned from the request's fiscal-year selection by
     * {@link #assignFiscalYear(Long)}. Null is a legitimate transient state for
     * a document created before the caller selected a period, but a document that
     * stays null is invisible to
     * {@code sumApprovedAmountForBranchAndFiscalYear}, so the creation path
     * treats a missing selection as worth logging rather than as normal.
     */
    @Column(name = "fiscal_year_id")
    private Long fiscalYearId;

    /** Identity user id of the requester. */
    @NotNull(message = "Requester is required")
    @Column(name = "requested_by", nullable = false)
    private Long requestedBy;

    @NotNull(message = "Request date is required")
    @Column(name = "request_date", nullable = false)
    private LocalDate requestDate;

    /**
     * The request date projected into Bikram Sambat, as {@code yyyy-MM-dd}.
     *
     * <p><strong>Derived, never authoritative.</strong> {@link #requestDate} is the
     * canonical value: it is what every index, range filter and the Java
     * {@code LocalDate} binding use. This column exists so Nepali-facing reports,
     * exports and statutory documents can print the BS date without re-deriving it
     * in every consumer, which is how the mismatched fiscal-year rows got in.
     *
     * <p>It is written only by {@link #syncNepaliDate()}, which runs on every
     * persist and update. There is deliberately no public setter: a caller cannot
     * write a BS date that disagrees with the AD date beside it.
     */
    @Column(name = "request_date_np", length = 10)
    private String requestDateNp;

    /**
     * The BS year of {@link #requestDate}, materialised so "show me everything in
     * fiscal year 2081/82" is an indexed integer comparison rather than a string
     * prefix match on the full date.
     */
    @Column(name = "request_date_np_year")
    private Integer requestDateNpYear;

    /** Date by which the branch needs delivery. Must not precede {@link #requestDate}. */
    @Column(name = "required_by_date")
    private LocalDate requiredByDate;

    /**
     * The needed-by date projected into Bikram Sambat. Derived on the same terms as
     * {@link #requestDateNp}: nullable because the needed-by date itself is optional.
     */
    @Column(name = "required_by_date_np", length = 10)
    private String requiredByDateNp;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private PurchaseRequisitionStatus status = PurchaseRequisitionStatus.DRAFT;

    @NotNull(message = "Priority is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    @Builder.Default
    private RequisitionPriority priority = RequisitionPriority.NORMAL;

    /**
     * A requisition is single currency. Lines carry no currency of their own,
     * which removes the possibility of a mixed-currency document.
     */
    @NotBlank(message = "Currency is required")
    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "NPR";

    // ---- Estimated totals. Planning figures, not commitments. ----
    // Derived from each line's REQUESTED quantity. See recalculateTotals().
    @Column(name = "estimated_subtotal_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal estimatedSubtotalAmount = zeroMoney();

    @Column(name = "estimated_tax_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal estimatedTaxAmount = zeroMoney();

    @Column(name = "estimated_total_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal estimatedTotalAmount = zeroMoney();

    // ---- Committed totals. Populated at approval. ----
    // Derived from each line's APPROVED quantity, tax inclusive, so that the
    // committed figure sits on the same basis as the estimated figure above.
    @Column(name = "approved_total_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal approvedTotalAmount = zeroMoney();

    @Column(name = "ordered_total_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal orderedTotalAmount = zeroMoney();

    /**
     * Where the goods are to be delivered, as a reference to a warehouse in the
     * inventory service rather than as free text.
     *
     * <p><strong>Why an id and not a name.</strong> This used to be a free-text
     * "delivery location". That could not be validated against a site the company
     * had closed, could not be aggregated for stock planning, and spelled the same
     * godown four different ways across four requisitions. The warehouse is
     * mastered in the inventory service, so this is a bare id with no foreign key
     * across services, in the same shape as {@code supplierId}.
     *
     * <p>Nullable on the requisition because the requester frequently does not know
     * or care which site will serve the request; the resolution happens at order
     * and receipt. Null here is a legitimate answer, not a gap.
     */
    @Column(name = "warehouse_id")
    private Long warehouseId;

    /**
     * Frozen warehouse code and name.
     *
     * <p>Snapshotted for the same reason {@code poNumber} and
     * {@code supplierCode} are: this is a legal record, and a site renamed next
     * year must not retroactively change what an approved requisition said it was
     * buying for. Read back from the catalogue only when the document is written.
     */
    @Column(name = "warehouse_code", length = 64)
    private String warehouseCode;

    @Column(name = "warehouse_name", length = 255)
    private String warehouseName;

    /**
     * Why this purchase is needed. Mandatory for high and urgent priority,
     * where the request is asking to jump a queue and therefore owes the
     * approver an explanation.
     */
    @Column(name = "justification", columnDefinition = "TEXT")
    private String justification;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // ---- Workflow stamps: null until the transition happens. ----
    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "submitted_by", length = 100)
    private String submittedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "rejected_by", length = 100)
    private String rejectedBy;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;
@Column(name = "converted_at")
private Instant convertedAt;

@Column(name = "converted_by", length = 100)
private String convertedBy;


    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancelled_by", length = 100)
    private String cancelledBy;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    /**
     * The lines of this requisition.
     *
     * <p>{@code cascade = ALL} because a line has no meaning outside its
     * header, and {@code orphanRemoval = true} so that dropping the last
     * reference from an editable document deletes the row instead of orphaning
     * a line that still points at a live header.
     */
    @OneToMany(mappedBy = "requisition", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    @OrderBy("lineNumber ASC")
    @ToString.Exclude
    @Builder.Default
    private List<PurchaseRequisitionLine> lines = new ArrayList<>();

    /**
     * The approval chain snapshot taken at submission.
     *
     * <p>Cascaded and orphan-removable so that resubmitting a returned document
     * can replace the chain wholesale rather than leaving stale steps from the
     * previous attempt on the row.
     */
    @OneToMany(mappedBy = "requisition", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    @OrderBy("sequenceNo ASC")
    @ToString.Exclude
    @Builder.Default
    private List<PurchaseRequisitionApproval> approvals = new ArrayList<>();

    /**
     * Append-only lifecycle audit trail.
     *
     * <p>Cascaded for insertion only. There is deliberately no removal path: a
     * procurement document is a legal record and its transition history must
     * outlive the document's own edits.
     */
    @OneToMany(mappedBy = "requisition", cascade = CascadeType.ALL, fetch = FetchType.LAZY,
            orphanRemoval = false)
    @OrderBy("changedAt DESC")
    @ToString.Exclude
    @Builder.Default
    private List<PurchaseRequisitionStatusHistory> statusHistory = new ArrayList<>();

    /**
     * Creates a draft requisition for one branch.
     *
     * <p>{@code prNumber} must come from the document sequence allocator, called
     * inside the same transaction as this insert.
     *
     * @param prNumber         the allocated document number
     * @param branchId         the requesting branch
     * @param requestedBy      the requesting user id
     * @param requestDate      the request date, must not be null
     * @param requiredByDate   the needed-by date, may be null, must not precede the request date
     * @param priority         the business urgency
     * @param currency         the ISO 4217 currency code
     * @param fiscalYearId     the owning fiscal year, may be null
     * @param departmentId     the requesting cost centre, may be null
     * @return a new unsaved draft requisition with no lines
     */
    public static PurchaseRequisition draft(String prNumber, Long branchId, Long requestedBy,
                                            LocalDate requestDate, LocalDate requiredByDate,
                                            RequisitionPriority priority, String currency,
                                            Long fiscalYearId, Long departmentId) {

        Objects.requireNonNull(requestDate, "Request date is required");
        Objects.requireNonNull(branchId, "Branch is required");
        Objects.requireNonNull(requestedBy, "Requester is required");

        if (requiredByDate != null && requiredByDate.isBefore(requestDate)) {
            throw new IllegalArgumentException(
                    "Required-by date " + requiredByDate + " precedes the request date " + requestDate);
        }

        PurchaseRequisition requisition = PurchaseRequisition.builder()
                .prNumber(prNumber)
                .branchId(branchId)
                .requestedBy(requestedBy)
                .departmentId(departmentId)
                .fiscalYearId(fiscalYearId)
                .requestDate(requestDate)
                .requiredByDate(requiredByDate)
                .priority(priority == null ? RequisitionPriority.NORMAL : priority)
                .currency(normalizeCurrency(currency))
                .status(PurchaseRequisitionStatus.DRAFT)
                .lines(new ArrayList<>())
                .approvals(new ArrayList<>())
                .statusHistory(new ArrayList<>())
                .build();
        requisition.recalculateTotals();
        return requisition;
    }

    /**
     * Records the fiscal year this document belongs to.
     *
     * <p><strong>Why this exists rather than a constructor argument.</strong> The
     * fiscal year drives budget consumption and period reporting, so a requisition
     * with a null {@code fiscalYearId} silently disappears from both. Making
     * assignment an explicit, separately invoked step means "not yet assigned" is
     * a decision somebody took rather than an accident of a call site that had
     * nothing to hand.
     *
     * <p>Idempotent for the same value, and refuses to move a document whose value
     * has already been committed: re-assigning an approved requisition to a
     * different fiscal year would move value between periods, which is precisely
     * the thing period reporting exists to prevent.
     *
     * <p>The guard keys off editability rather than off
     * {@link PurchaseRequisitionStatus#isBudgetConsuming()}. Budget is only
     * committed once a document is approved, but a document that is merely sitting
     * in the approval queue has already been numbered out of a fiscal period and
     * is already being judged against that period's budget by its approvers.
     * Re-filing it underneath them would make the header disagree with the number
     * printed on it. Freezing at the same point the other editable fields freeze
     * keeps one rule rather than two.
     *
     * @param fiscalYearId the owning fiscal year, null to leave unassigned
     * @throws IllegalStateException if the document is past editing and already
     *                               assigned to a different fiscal year
     */
    public void assignFiscalYear(Long fiscalYearId) {
        if (fiscalYearId == null || Objects.equals(this.fiscalYearId, fiscalYearId)) {
            return;
        }
        if (this.fiscalYearId != null && !isEditable()) {
            throw new IllegalStateException(
                    "Requisition " + prNumber + " is " + status + " and already belongs to fiscal year "
                            + this.fiscalYearId + "; it cannot be moved to " + fiscalYearId
                            + " because it has already left draft and its number was issued from "
                            + "the period it belongs to");
        }
        this.fiscalYearId = fiscalYearId;
    }

    /**
     * Recomputes every Bikram Sambat projection from its Gregorian source.
     *
     * <p>Runs automatically before every insert and update, which also repairs any
     * row written before these columns existed. Doing this in the entity rather than
     * in the service means no code path - seeder, import job, ad-hoc console - can
     * persist a requisition whose AD and BS dates disagree.
     *
     * <p>The projection is deliberately non-strict: a date outside the published
     * calendar range leaves the NP columns null rather than blocking the write. A
     * purchase requisition is real business data; refusing to record it because the
     * calendar table has not been extended yet would be worse than a missing
     * projection. Reports that need the BS date filter those rows out.
     *
     * <p>AD remains canonical in every case. There is no code path that writes an
     * NP column without a matching AD column.
     */
    @PrePersist
    @PreUpdate
    void syncNepaliDate() {
        if (requestDate == null) {
            this.requestDateNp = null;
            this.requestDateNpYear = null;
        } else {
            try {
                NepaliDate bs = NepaliDateConverter.fromAd(requestDate);
                this.requestDateNp = bs.toString();
                this.requestDateNpYear = bs.year();
            } catch (RuntimeException e) {
                this.requestDateNp = null;
                this.requestDateNpYear = null;
            }
        }

        this.requiredByDateNp = requiredByDate == null ? null : projectToBs(requiredByDate);
    }

    /**
     * @return the BS form of {@code adDate}, or {@code null} when the date falls
     *         outside the published calendar range
     */
    private static String projectToBs(LocalDate adDate) {
        try {
            return NepaliDateConverter.fromAd(adDate).toString();
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * Adds a line to an editable requisition and refreshes the totals.
     *
     * <p>The building block for a grid edit, not a bulk operation: renaming a line
     * is a remove followed by an add, and the caller has to flush between the two.
     * See {@link #removeLine(int)} and the note immediately above this method's
     * replacement for why that ordering is not hidden inside the aggregate.
     *
     * @param line the line, already constructed via {@link PurchaseRequisitionLine#create}
     * @throws IllegalStateException if the requisition is not editable or the line number is taken
     */
    public void addLine(PurchaseRequisitionLine line) {
        requireEditable();
        Objects.requireNonNull(line, "Line is required");
        if (lines.stream()
                .anyMatch(existing -> existing.getLineNumber().equals(line.getLineNumber()))) {
            throw new IllegalStateException(
                    "Line number " + line.getLineNumber() + " is already used on this requisition");
        }
        line.assignScope(getTenantId(), getCompanyId());
        line.attachTo(this);
        lines.add(line);
        recalculateTotals();
    }

    // There is deliberately no replaceLines(List) counterpart to addLine.
    //
    // It existed, it had no callers, and it was the more dangerous of the two edit
    // paths. It swapped the collection in one step and claimed that was what made
    // it safe - but "swap in one step" is precisely the failure: on a grid edit
    // every line is renumbered, so Hibernate's flush queue inserts the replacement
    // for line 1 before the deleted line 1 leaves the table, and
    // uk_purchase_requisition_lines_requisition_number rejects it. The method that
    // actually works is removeLine-then-flush-then-add, and the flush between the
    // two belongs to the transactional service rather than to this aggregate. See
    // applyEditedLines in PurchaseRequisitionServiceImpl for the working version.

    /**
     * Updates the descriptive fields of an editable requisition.
     *
     * <p><strong>Why currency is edited here and not somewhere else.</strong> It
     * used to be settable only at construction, which meant the update endpoint
     * accepted a currency and quietly discarded it. The requester who picked the
     * wrong one was told their save had succeeded and found the old value still
     * there on reload, with no way to tell that from a save that had genuinely
     * worked. A field that a client can send but cannot set is worse than a field
     * that is absent, because it converts a visible bug into an invisible one.
     *
     * <p>A null or blank currency means "leave it alone" rather than "reset it".
     * Under the full-replacement contract the other absent fields are cleared, and
     * for most of them that is right. Currency is the exception: clearing it would
     * fall through to the NPR default and silently re-denominate every amount on
     * the document, turning a 5,000 rupee estimate into a 5,000 dollar one without
     * a single number changing. Omitting the field keeps the money as it was.
     *
     * @param requiredByDate  the revised needed-by date, may be null
     * @param priority        the revised urgency, or null to keep the current one
     * @param currency        the revised ISO 4217 code, or null/blank to keep the current one
     * @param warehouseId     the revised destination warehouse, or null to keep the current one
     * @param warehouseCode   the revised warehouse's frozen code, or null to keep it
     * @param warehouseName   the revised warehouse's frozen name, or null to keep it
     * @param justificationText the business justification
     * @param requisitionNotes free text notes
     * @throws IllegalArgumentException if the currency is not a three-letter code, or
     *                                  the warehouse reference is unusable
     */
    public void amendDetails(LocalDate requiredByDate, RequisitionPriority priority,
                             String currency, Long warehouseId, String warehouseCode,
                             String warehouseName, String justificationText,
                             String requisitionNotes) {

        requireEditable();

        if (requiredByDate != null && requiredByDate.isBefore(this.requestDate)) {
            throw new IllegalArgumentException(
                    "Required-by date " + requiredByDate + " precedes the request date "
                            + this.requestDate);
        }

        RequisitionPriority newPriority = priority == null ? this.priority : priority;
        if ((newPriority == RequisitionPriority.HIGH || newPriority == RequisitionPriority.URGENT)
                && (justificationText == null || justificationText.isBlank())) {
            throw new IllegalArgumentException(
                    "A justification is required for " + newPriority + " requisitions");
        }

        this.requiredByDate = requiredByDate;
        this.priority = newPriority;
        // Absent AND blank both mean "leave it alone". Blank matters as much as null:
        // a form that the user cleared submits "", not null, and letting that through
        // normalizeCurrency would fall to the NPR default and re-denominate the whole
        // document. The two spellings of "not supplied" must not behave differently.
        if (currency != null && !currency.isBlank()) {
            this.currency = normalizeCurrency(currency);
        }
        // Absent means "leave it alone", matching how currency behaves above. This
        // differs from the other fields on purpose: clearing the destination would
        // leave a requisition with nowhere for its goods to go, and that should be
        // a deliberate choice somewhere that can see the consequences rather than a
        // side effect of a form omitting an untouched field.
        if (warehouseId != null) {
            requireUsableWarehouse(warehouseId, warehouseCode, warehouseName);
            this.warehouseId = warehouseId;
            this.warehouseCode = warehouseCode;
            this.warehouseName = warehouseName;
        }
        this.justification = justificationText;
        this.notes = requisitionNotes;
    }

    /**
     * Refuses a warehouse reference that cannot be printed on a document.
     *
     * <p>The id is a live pointer into another service's master data and the code and
     * name are the frozen copy that makes the document readable afterwards. A
     * non-positive id is not an identifier at all, and an id with no code or name is
     * half a snapshot - which reads on paper as authoritative while resolving to
     * nothing, and can only be completed by calling Inventory at read time. That is
     * precisely what storing the snapshot exists to avoid, so a half-supplied
     * warehouse is refused rather than stored.
     *
     * <p>Existence and tenant ownership of the id are not checked here, for the same
     * reason they are not checked for items: this module holds no copy of the
     * warehouse master and has no client to Inventory. What is checked is that the
     * caller supplied something this document can actually carry.
     *
     * @param warehouseId   the warehouse identifier, must be positive
     * @param warehouseCode the code frozen at nomination time, must not be blank
     * @param warehouseName the name frozen at nomination time, must not be blank
     * @throws IllegalArgumentException if the reference is unusable
     */
    private static void requireUsableWarehouse(Long warehouseId, String warehouseCode,
                                               String warehouseName) {
        if (warehouseId <= 0) {
            throw new IllegalArgumentException(
                    "Warehouse id must be a positive identifier, was " + warehouseId);
        }
        if (warehouseCode == null || warehouseCode.isBlank()
                || warehouseName == null || warehouseName.isBlank()) {
            throw new IllegalArgumentException(
                    "Warehouse " + warehouseId + " must carry the code and name captured at "
                            + "the time it was nominated");
        }
    }

    /**
     * Moves the request date on a document that has not yet been submitted.
     *
     * <p>Permitted while DRAFT only. Once a document is in the
     * workflow its date is part of its identity: it fixes the numbering period,
     * the fiscal year the document is reported against, and where it sits in a
     * queue of things waiting. Changing it afterwards would move a document
     * between budget periods without moving any money, which is the one edit a
     * period report can least defend.
     *
     * <p>The Bikram Sambat projections are not recomputed here. They are written
     * by {@link #syncNepaliDate()} on every persist, so the derived columns can
     * never disagree with this one, which is why there is no setter that could
     * write a BS date independently.
     *
     * @param newRequestDate the revised request date, must not be null
     * @throws IllegalStateException    if the requisition is not editable
     * @throws IllegalArgumentException if the date is null or precedes the needed-by date
     */
    public void amendRequestDate(LocalDate newRequestDate) {
        requireEditable();
        Objects.requireNonNull(newRequestDate, "Request date is required");
        if (this.requiredByDate != null && newRequestDate.isAfter(this.requiredByDate)) {
            throw new IllegalArgumentException(
                    "Request date " + newRequestDate + " falls after the needed-by date "
                            + this.requiredByDate + "; a requisition cannot be needed before it was raised");
        }
        this.requestDate = newRequestDate;
    }

    /**
     * Submits this requisition for approval with a materialised chain.
     *
     * <p>Validation happens here, at the moment the obligation is created, and
     * not at read time: the policy that applied must be captured, otherwise
     * editing a policy mid-flight would retroactively skip a step.
     *
     * @param approvalChain the steps resolved from the effective policy, must not be empty
     * @param actor         the submitting user id
     * @throws IllegalStateException if the requisition is not submittable or the chain is empty
     */
    public void submit(List<PurchaseRequisitionApproval> approvalChain, String actor) {
        if (!status.canTransitionTo(PurchaseRequisitionStatus.SUBMITTED)) {
            throw new IllegalStateException(
                    "Requisition " + prNumber + " is " + status + " and cannot be submitted");
        }
        if (lines.isEmpty()) {
            throw new IllegalStateException("A requisition with no lines cannot be submitted");
        }
        Objects.requireNonNull(approvalChain, "Approval chain is required");
        if (approvalChain.isEmpty()) {
            throw new IllegalStateException(
                    "No approval policy matched this requisition; submission is blocked");
        }

        approvalChain.forEach(step -> {
            step.assignScope(getTenantId(), getCompanyId());
            step.attachTo(this);
        });
        this.approvals.clear();
        this.approvals.addAll(approvalChain);

        Instant now = Instant.now();
        this.submittedAt = now;
        this.submittedBy = actor;
        this.status = PurchaseRequisitionStatus.SUBMITTED;
    }

    /**
     * Records an approval decision that grants the whole requisition.
     *
     * <p>Approving is the moment the company commits to fund the demand, so the
     * totals are recomputed here rather than left to the next write: after this
     * returns, {@code approvedTotalAmount} is the committed figure and a budget
     * report reads it without having to know that anything happened.
     *
     * <p>The submitted approval rows are completed by
     * {@link PurchaseRequisitionApproval#decide} before this is called, so this
     * method does not need to know how many approvers there were - only that the
     * workflow decided they were enough.
     *
     * @param actor the acting user id
     * @throws IllegalStateException if the requisition is not awaiting a decision
     */
    public void approve(String actor) {
        requireDecidable();
        Instant now = Instant.now();
        this.approvedAt = now;
        this.approvedBy = actor;
        this.status = PurchaseRequisitionStatus.APPROVED;
        recalculateTotals();
    }

    /**
     * Refuses the requisition outright.
     *
     * <p>A reason is mandatory. "Rejected" with no explanation is not something a
     * requester can act on or an auditor can review, and the only lawful response
     * to it is to raise a fresh draft with the same defect.
     *
     * @param actor  the acting user id
     * @param reason why the requisition was refused
     * @throws IllegalArgumentException if the reason is missing
     * @throws IllegalStateException    if the requisition is not awaiting a decision
     */
    public void reject(String actor, String reason) {
        requireDecidable();
        requireReason(reason, "rejection");
        Instant now = Instant.now();
        this.rejectedAt = now;
        this.rejectedBy = actor;
        this.rejectionReason = reason;
        this.status = PurchaseRequisitionStatus.REJECTED;
    }

    /**
     * Withdraws the requisition before or during approval.
     *
     * <p>Available from DRAFT and SUBMITTED only. An approved requisition cannot be
     * cancelled: it has already committed budget, and releasing that commitment is
     * a different event with different consequences for the order it produced.
     *
     * @param actor  the acting user id
     * @param reason why the requisition was withdrawn
     * @throws IllegalArgumentException if the reason is missing
     * @throws IllegalStateException    if the requisition is past the approval stage
     */
    public void cancel(String actor, String reason) {
        if (!status.canTransitionTo(PurchaseRequisitionStatus.CANCELLED)) {
            throw new IllegalStateException(
                    "Requisition " + prNumber + " is " + status
                            + "; only a DRAFT or SUBMITTED requisition can be cancelled");
        }
        requireReason(reason, "cancellation");
        Instant now = Instant.now();
        this.cancelledAt = now;
        this.cancelledBy = actor;
        this.cancellationReason = reason;
        this.status = PurchaseRequisitionStatus.CANCELLED;
    }

    /**
     * Marks the approved demand as converted into purchase orders.
     *
     * <p>Call this only after the order links and line quantities have actually
     * been written. No amount is accepted as an argument: the ordered value is
     * derived from the lines the caller has already updated, so the header cannot
     * claim a conversion the lines do not support.
     *
     * @param actor the acting user id
     * @throws IllegalStateException if the requisition is not approved
     */
    public void convertToPO(String actor) {
        if (!status.canTransitionTo(PurchaseRequisitionStatus.CONVERTED_TO_PO)) {
            throw new IllegalStateException(
                    "Requisition " + prNumber + " is " + status
                            + "; only an APPROVED requisition can be converted into a purchase order");
        }
        recalculateTotals();
        this.convertedAt = Instant.now();
        this.convertedBy = actor;
        this.status = PurchaseRequisitionStatus.CONVERTED_TO_PO;
    }

    /**
     * Recomputes every header total from the current line set.
     *
     * <p>Called after each mutation rather than lazily on read so the persisted
     * values are always current, which is what lets branch budget dashboards read
     * {@code estimatedTotalAmount} directly instead of aggregating lines.
     *
     * <p>Rejected and cancelled lines are excluded, which makes line cancellation
     * a safe way to amend a document without recomputing by hand.
     *
     * <p><strong>The estimate is keyed on the requested quantity.</strong> This is
     * the single most important rule in this method. An estimate answers "what is
     * this branch asking for", and that question is meaningful from the moment the
     * line is created - long before anyone has granted anything. Deriving the
     * estimate from the approved quantity instead made every draft read as zero,
     * which in turn made {@code submit()} resolve the approval chain against zero
     * and route every requisition, of any value, through the lowest amount band.
     * Amount-based approval control only works if the amount it is tested against
     * is the amount the branch actually requested.
     *
     * <p><strong>Committed value is keyed on the approved quantity, and is
     * gross.</strong> {@code approvedTotalAmount} is only moved off zero once the
     * document is actually approved; before that, approved quantity on the lines
     * is still a proposal and must not be reported as committed budget. The
     * committed figure is net plus tax, matching the gross basis of the estimate,
     * so a budget report that compares the two does not silently understate
     * consumption by exactly the tax.
     */
    public void recalculateTotals() {
        BigDecimal estimatedSubtotal = BigDecimal.ZERO;
        BigDecimal estimatedTax = BigDecimal.ZERO;
        BigDecimal approvedValue = BigDecimal.ZERO;
        BigDecimal orderedValue = BigDecimal.ZERO;

        for (PurchaseRequisitionLine line : lines) {
            if (!line.isCounted()) {
                continue;
            }
            estimatedSubtotal = estimatedSubtotal.add(line.requestedNetAmount());
            estimatedTax = estimatedTax.add(line.requestedTaxAmount());

            if (status.isBudgetConsuming()) {
                approvedValue = approvedValue.add(line.approvedGrossAmount());
                BigDecimal orderedRatio = line.getApprovedQuantity().signum() == 0
                        ? BigDecimal.ZERO
                        : line.getOrderedQuantity()
                                .divide(line.getApprovedQuantity(), 10, RoundingMode.HALF_UP);
                orderedValue = orderedValue.add(
                        line.approvedGrossAmount().multiply(orderedRatio)
                                .setScale(MONEY_SCALE, RoundingMode.HALF_UP));
            }
        }

        this.estimatedSubtotalAmount = estimatedSubtotal.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        this.estimatedTaxAmount = estimatedTax.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        this.estimatedTotalAmount = estimatedSubtotal.add(estimatedTax)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        if (status.isBudgetConsuming()) {
            this.approvedTotalAmount = approvedValue.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            this.orderedTotalAmount = orderedValue.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        } else {
            this.approvedTotalAmount = zeroMoney();
            this.orderedTotalAmount = zeroMoney();
        }
    }

    /**
     * Returns the approved value still available for ordering.
     *
     * @return approved total minus ordered total, never negative
     */
    public BigDecimal outstandingValue() {
        return approvedTotalAmount.subtract(orderedTotalAmount).max(BigDecimal.ZERO);
    }

    /**
     * Returns the line with the given ordinal.
     *
     * @param lineNumber the 1-based ordinal
     * @return the matching line
     * @throws IllegalArgumentException if no such line exists
     */
    public PurchaseRequisitionLine findLine(int lineNumber) {
        return lines.stream()
                .filter(line -> line.getLineNumber() == lineNumber)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Requisition " + prNumber + " has no line " + lineNumber));
    }

    /**
     * Returns the lines that still have un-ordered approved quantity.
     *
     * @return the outstanding lines, in line number order
     */
    public List<PurchaseRequisitionLine> outstandingLines() {
        return lines.stream()
                .filter(PurchaseRequisitionLine::hasOutstandingQuantity)
                .toList();
    }

    /**
     * Looks up a line by its 1-based ordinal.
     *
     * <p>Separate from {@link #findLine(int)} because an edit that is rebuilding
     * the grid needs to ask "is there already a line 3?" without being told to
     * fail when the answer is no. {@code findLine} is for callers that require
     * the line to exist.
     *
     * @param lineNumber the 1-based ordinal
     * @return the matching line, or empty when this requisition has none
     */
    public Optional<PurchaseRequisitionLine> findLineByNumber(int lineNumber) {
        return lines.stream()
                .filter(line -> line.getLineNumber() == lineNumber)
                .findFirst();
    }

    /**
     * Drops a line from an editable requisition.
     *
     * <p>Called by the application layer only after the orphan has been flushed
     * out, because of how Hibernate orders a flush queue. Deleting and inserting
     * in one pass would insert the replacement for line 1 <em>before</em> the
     * deleted line 1 leaves the table, and the unique constraint on
     * {@code (purchase_requisition_id, line_number)} would reject it. Callers
     * that are about to reuse a freed ordinal must flush between the removal and
     * the insertion; that ordering constraint belongs to the persistence layer,
     * which is why it is not hidden inside this method.
     *
     * @param lineNumber the 1-based ordinal to remove
     * @throws IllegalStateException    if the requisition is not editable
     * @throws IllegalArgumentException if no such line exists
     */
    public void removeLine(int lineNumber) {
        requireEditable();
        PurchaseRequisitionLine line = findLine(lineNumber);
        this.lines.remove(line);
        recalculateTotals();
    }

    /**
     * Looks up a line by its persisted id.
     *
     * <p>Exists so a caller acting on a line named in a request body goes through
     * the aggregate rather than reaching into the collection. The id arrives from
     * the wire, so it has to be proved to belong to <em>this</em> document:
     * matching on ordinal alone would let a caller who knows line 3 of one
     * requisition act on line 3 of a different one.
     *
     * @param lineId the persisted line id
     * @return the matching line, or empty when this requisition has no such line
     */
    public Optional<PurchaseRequisitionLine> findLineById(Long lineId) {
        if (lineId == null) {
            return Optional.empty();
        }
        return lines.stream()
                .filter(line -> lineId.equals(line.getId()))
                .findFirst();
    }

    /**
     * Refuses specific lines without voiding the whole document.
     *
     * <p>Line-level rejection is what makes partial approval usable in practice: a
     * branch asking for ten items should not lose the three that are genuinely
     * needed because one was disallowed. The alternative - rejecting the whole
     * requisition - forces the requester to re-raise the nine acceptable lines from
     * scratch, and in practice they simply do not.
     *
     * <p>Refusal is one-directional. Nothing here can bring a rejected line back;
     * reinstating demand is the requester's job on a returned document, where it
     * is recorded as a fresh submission rather than a quiet edit of a decision that
     * has already been given and may already have been acted on.
     *
     * @param lineIds the lines to refuse, must be non-empty and all belong here
     * @throws IllegalStateException    if the document is not awaiting a decision
     * @throws IllegalArgumentException if any id names no line of this requisition
     */
    public void rejectLines(Collection<Long> lineIds) {
        requireDecidable();
        requireLineIds(lineIds, "rejected");
        lineIds.forEach(lineId -> requireLineById(lineId).rejectLine());
        recalculateTotals();
    }

    /**
     * Withdraws specific lines, recorded as a withdrawal rather than a refusal.
     *
     * <p>The distinction is for the audit trail: "the approver disallowed this" and
     * "this demand is no longer needed" are different statements about why a line
     * contributes nothing, and a reader of the register needs to tell them apart.
     *
     * @param lineIds the lines to withdraw, must be non-empty and all belong here
     * @throws IllegalStateException    if the document is not awaiting a decision
     * @throws IllegalArgumentException if any id names no line of this requisition
     */
    public void cancelLines(Collection<Long> lineIds) {
        requireDecidable();
        requireLineIds(lineIds, "cancelled");
        lineIds.forEach(lineId -> requireLineById(lineId).cancelLine());
        recalculateTotals();
    }

    /**
     * @param lineId the persisted line id
     * @return the matching line
     * @throws IllegalArgumentException if this requisition has no such line
     */
    private PurchaseRequisitionLine requireLineById(Long lineId) {
        return findLineById(lineId).orElseThrow(() -> new IllegalArgumentException(
                "Requisition " + prNumber + " has no line with id " + lineId));
    }

    private void requireLineIds(Collection<Long> lineIds, String label) {
        if (lineIds == null || lineIds.isEmpty()) {
            throw new IllegalArgumentException("At least one line must be " + label);
        }
        if (lineIds.stream().distinct().count() != lineIds.size()) {
            throw new IllegalArgumentException(
                    "The same line cannot be listed twice among the lines to be " + label);
        }
    }

    /**
     * Refuses a line decision on a document that is not in front of an approver.
     *
     * <p>SUBMITTED is the only qualifying state: it is where a requisition waits
     * for a decision, and once it has left that state the approvers' view of the
     * lines is final.
     */
    private void requireDecidable() {
        if (!status.isAwaitingApproval()) {
            throw new IllegalStateException(
                    "Requisition " + prNumber + " is " + status
                            + "; lines can only be refused or withdrawn while the document awaits a decision");
        }
    }

    /**
     * Appends a lifecycle audit entry for a transition this document just made.
     *
     * <p>Called by the application layer after the status has been applied,
     * passing the state it came from. Creation is recorded with a null previous
     * state.
     *
     * @param from   the previous state, null when recording creation
     * @param to     the state now held
     * @param actor  the acting user id
     * @param reason the reason, required for rejection and cancellation
     * @throws IllegalStateException    if the transition is not permitted
     * @throws IllegalArgumentException if a required reason is missing
     */
    public void recordTransition(PurchaseRequisitionStatus from, PurchaseRequisitionStatus to,
                                 String actor, String reason) {
        PurchaseRequisitionStatusHistory entry =
                PurchaseRequisitionStatusHistory.record(from, to, actor, reason);
        entry.assignScope(getTenantId(), getCompanyId());
        entry.attachTo(this);
        this.statusHistory.add(entry);
    }

    /**
     * Returns whether this requisition may still have its content edited.
     *
     * @return {@code true} while the document is a draft
     */
    public boolean isEditable() {
        return status.isEditable();
    }

    private void requireEditable() {
        if (!isEditable()) {
            throw new IllegalStateException(
                    "Requisition " + prNumber + " is " + status
                            + "; its lines can only be changed while DRAFT");
        }
    }

    private static void requireReason(String reason, String label) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A reason is required to record a " + label);
        }
    }

    private static BigDecimal zeroMoney() {
        return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
    }

    /**
     * Validates and canonicalises the document currency.
     *
     * <p>Absent currency still defaults to NPR, because a single-country
     * deployment should not have to send the same value on every request. A
     * currency that is present but malformed is <em>rejected</em> rather than
     * silently rewritten to NPR: a request carrying "NPRS" or "RS" is a client
     * bug, and quietly storing it as NPR produces documents whose recorded
     * currency does not match what the user asked for - the class of defect that
     * is invisible until a statutory report disagrees with the ledger.
     *
     * @param currency the submitted ISO 4217 code, may be null or blank
     * @return the uppercase three-letter code, or "NPR" when none was supplied
     * @throws IllegalArgumentException if a non-blank value is not three letters
     */
    private static String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return "NPR";
        }
        String normalized = currency.trim().toUpperCase();
        if (!normalized.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException(
                    "Currency '" + currency + "' is not a valid ISO 4217 code; "
                            + "expected three letters such as NPR, USD or INR");
        }
        return normalized;
    }
}
