package com.erp.business.procurement.purchaserequisition;

import com.erp.business.inventory.interfaces.unit.UnitService;
import com.erp.business.procurement.purchaserequisition.domain.ApprovalAction;
import com.erp.business.procurement.purchaserequisition.domain.PurchaseRequisition;
import com.erp.business.procurement.purchaserequisition.domain.PurchaseRequisitionApproval;
import com.erp.business.procurement.purchaserequisition.domain.PurchaseRequisitionApprovalPolicy;
import com.erp.business.procurement.purchaserequisition.domain.PurchaseRequisitionLine;
import com.erp.business.procurement.purchaserequisition.domain.PurchaseRequisitionOrderLink;
import com.erp.business.procurement.purchaserequisition.domain.PurchaseRequisitionStatus;
import com.erp.business.procurement.purchaserequisition.dto.ApprovalDecisionRequest;
import com.erp.business.procurement.purchaserequisition.dto.CreateRequisitionLineRequest;
import com.erp.business.procurement.purchaserequisition.dto.CreateRequisitionRequest;
import com.erp.business.procurement.purchaserequisition.dto.OrderAwardRequest;
import com.erp.business.procurement.purchaserequisition.dto.RequisitionActionabilityResponse;
import com.erp.business.procurement.purchaserequisition.dto.RequisitionDetailResponse;
import com.erp.business.procurement.purchaserequisition.dto.RequisitionLineResponse;
import com.erp.business.procurement.purchaserequisition.dto.RequisitionListResponse;
import com.erp.business.procurement.purchaserequisition.dto.UpdateRequisitionRequest;
import com.erp.business.procurement.purchaserequisition.infrastructure.persistence.PurchaseRequisitionApprovalPolicyRepository;
import com.erp.business.procurement.purchaserequisition.infrastructure.persistence.PurchaseRequisitionApprovalRepository;
import com.erp.business.procurement.purchaserequisition.infrastructure.persistence.PurchaseRequisitionOrderLinkRepository;
import com.erp.business.procurement.purchaserequisition.infrastructure.persistence.PurchaseRequisitionRepository;
import com.erp.business.procurement.purchaseorder.infrastructure.persistence.PurchaseOrderRepository;
import com.erp.business.procurement.shared.context.BusinessContextAccessor;
import com.erp.business.procurement.shared.domain.DocumentSequence;
import com.erp.business.procurement.shared.infrastructure.persistence.DocumentSequenceRepository;
import com.erp.business.procurement.shared.security.PurchaseRequisitionPermissions;
import com.erp.business.procurement.workflow.WorkflowConfigurationService;
import com.erp.platform.common.exception.ForbiddenException;
import com.erp.platform.common.exception.ResourceNotFoundException;
import com.erp.platform.common.exception.UnauthorizedException;
import com.erp.platform.common.exception.ValidationException;
import com.erp.platform.common.nepali.date.NepaliDate;
import com.erp.platform.common.nepali.date.NepaliDateConverter;
import com.erp.platform.security.annotation.RequiresPermission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Purchase requisition use cases.
 *
 * <p><strong>Authorization happens in three layers, and all three are needed.</strong>
 * <ol>
 *   <li>{@link RequiresPermission} on every public method, enforced by the shared
 *       {@code PermissionAspect} before the method body runs. This is the coarse
 *       gate: may this kind of caller do this kind of thing at all?</li>
 *   <li>An ownership check on the specific document, for operations that belong to
 *       whoever raised it. A company-wide permission is not consent to edit your
 *       colleague's requisition.</li>
 *   <li>The approval-assignee check in {@code decide()}: a permission is not
 *       enough to decide a step, the caller has to be the person or role that step
 *       names.</li>
 * </ol>
 * Layer 1 alone was the whole of the previous model, and it meant any user holding
 * a single procurement permission could submit, cancel or delete <em>anybody's</em>
 * requisition in the company.
 *
 * <p><strong>Nobody is excluded from approving by being the requester.</strong> This
 * module used to refuse that combination outright. It is the right default for a
 * company large enough to have an approver who is never the requester, and the wrong
 * one for the far more common case of a small business where the owner raises the
 * requisition, signs it off and orders it - a company of that shape has nobody else
 * to hand it to, so the rule makes the module unusable rather than safer. The control
 * that survives is the permission itself: a caller who does not hold
 * {@code PR_APPROVE} or {@code PR_REJECT} cannot decide anything, whoever raised it,
 * and a caller who does not hold the matching permission cannot decide <em>that</em>
 * decision. Splitting into distinct codes is what lets a company that does want a
 * second pair of eyes configure it - assign the step to another user or role through
 * an approval policy, and the requester is simply not the assignee.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseRequisitionServiceImpl implements PurchaseRequisitionService {

    private static final String DOCUMENT_TYPE = "PURCHASE_REQUISITION";
    private static final String PREFIX = "PR-";

    /**
     * Display label frozen onto an approval step that names nobody, so the
     * details screen tells an approver who may act rather than showing a blank.
     *
     * <p>Built from the permission codes so the two can never drift apart in the
     * UI: the string an approver reads is the string an administrator has to
     * grant. Both decision permissions are named, because either one may clear
     * an open step - refusing a requisition needs no more authority than
     * approving it.
     */
    private static final String PERMISSION_ADDRESSED_LABEL =
            "Any holder of " + PurchaseRequisitionPermissions.APPROVE
                    + " or " + PurchaseRequisitionPermissions.REJECT;
    private static final int NUMBER_WIDTH = 6;

    /**
     * Stand-in for an empty role list in the inbox query.
     *
     * <p>An empty {@code IN} collection is not portable JPQL, and the direct
     * assignee half of the query's disjunction has to keep working for a user who
     * holds no roles at all. A role name that cannot occur - {@code role.name}
     * starts with {@code role_} and is followed by a generated key - makes the role
     * half of the disjunction permanently false without removing the row set.
     */
    private static final String NO_ROLE_MATCH = "role.no-role-in-token";

    private final PurchaseRequisitionRepository requisitionRepository;
    private final PurchaseRequisitionApprovalRepository approvalRepository;
    private final PurchaseRequisitionOrderLinkRepository orderLinkRepository;
    private final PurchaseRequisitionApprovalPolicyRepository policyRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final DocumentSequenceRepository sequenceRepository;
    private final UnitService unitService;

    /**
     * Supplies the caller's effective role names.
     *
     * <p>Injected rather than threaded through every service signature because
     * roles are request-scoped, not part of a document. Approval steps may be
     * assigned to a role rather than a named person, and role membership lives in
     * the identity service, so the only trustworthy source is the validated
     * access token read here.
     */
    private final BusinessContextAccessor context;

    private final RequisitionReferenceValidator referenceValidator;

    /**
     * Tells submission whether this company requires approval at all.
     *
     * <p>Read once per submission, inside the same transaction as the write, so a
     * setting flipped concurrently cannot leave a document half-way between the
     * two workflows. Its methods carry no permission annotation: it is consulted
     * from inside {@link #submit}, which is already gated on
     * {@link PurchaseRequisitionPermissions#SUBMIT}, and re-authorising here would
     * ask a submitter to hold an administrative permission they do not need in order
     * to raise spend.
     */
    private final WorkflowConfigurationService workflowConfiguration;

    @Override
    @Transactional
    @RequiresPermission(PurchaseRequisitionPermissions.CREATE)
    public RequisitionDetailResponse create(Long tenantId, Long companyId, Long requestedBy,
                                            CreateRequisitionRequest request) {

        requireScope(tenantId, companyId, requestedBy);

        // Before the document number is allocated, so a rejected request costs
        // nothing and leaves no gap in the sequence.
        referenceValidator.validate(request, tenantId, companyId);

        String actor = String.valueOf(requestedBy);

        // requestDate is @NotNull on the request, so there is nothing to default it
        // from. This used to fall back to LocalDate.now(), which was both unreachable
        // and the wrong thing to have written: the request date decides the period the
        // document number is allocated out of, so guessing today's date would file the
        // document under a period the requester never asked for.
        LocalDate requestDate = request.requestDate();

        rejectContradictoryNepaliDate(requestDate, request.requestDateNp());

        PurchaseRequisition requisition = PurchaseRequisition.draft(
                allocateNumber(tenantId, companyId, requestDate),
                request.branchId(),
                requestedBy,
                requestDate,
                request.requiredByDate(),
                request.priority(),
                request.currency(),
                null,
                request.departmentId());

        requisition.assignScope(tenantId, companyId);
        assignFiscalYear(requisition, actor);

        // draft() already took requiredByDate, priority and currency. This second call
        // carries the remaining text fields and the HIGH/URGENT justification rule,
        // which draft() does not enforce; the overlap is deliberate rather than tidied
        // away, so the two entry points cannot drift apart on the same fields.
        requisition.amendDetails(
                request.requiredByDate(),
                request.priority(),
                request.currency(),
                request.warehouseId(),
                request.warehouseCode(),
                request.warehouseName(),
                request.justification(),
                request.notes());

        int lineNumber = 1;
        for (CreateRequisitionLineRequest lineRequest : request.lines()) {
            UnitService.UnitResponse unit = UnitResolver.resolveUnit(unitService, lineRequest.uomCode());
            Long unitId = unit.getId();

            PurchaseRequisitionLine line = PurchaseRequisitionLine.create(
                    lineNumber++,
                    lineRequest.itemId(),
                    lineRequest.itemCode(),
                    lineRequest.itemName(),
                    lineRequest.uomCode(),
                    unitId,
                    lineRequest.quantity(),
                    lineRequest.estimatedUnitPrice(),
                    lineRequest.taxRate(),
                    lineRequest.remarks());
            requisition.addLine(line);
        }

        requisition.recordTransition(null, PurchaseRequisitionStatus.DRAFT, actor, null);
        return toDetail(requisitionRepository.save(requisition));
    }

    @Override
    @Transactional
    @RequiresPermission(PurchaseRequisitionPermissions.EDIT)
    public RequisitionDetailResponse update(Long tenantId, Long companyId, Long id,
                                            UpdateRequisitionRequest request, String actor) {

        // Before anything is loaded or mutated, so a malformed line costs nothing.
        referenceValidator.validate(request, tenantId, companyId);

        // Same self-consistency check the create path applies. It used to exist only
        // there, which meant a client with a date-picker bug could have its wrong BS
        // date caught on the first save and quietly accepted on every save after it.
        rejectContradictoryNepaliDate(request.requestDate(), request.requestDateNp());

        PurchaseRequisition requisition = loadForUpdate(tenantId, companyId, id);
        requireOwnershipOrAdmin(requisition, actor, "edit");

        if (!requisition.isEditable()) {
            throw new ValidationException(
                    "Requisition " + requisition.getPrNumber() + " is " + requisition.getStatus()
                            + "; its content can only be changed while it is DRAFT");
        }

        // Unconditional: requestDate is @NotNull, so the old null guard could never be
        // false and only made the field look optional.
        requisition.amendRequestDate(request.requestDate());

        requisition.amendDetails(
                request.requiredByDate(),
                request.priority(),
                request.currency(),
                request.warehouseId(),
                request.warehouseCode(),
                request.warehouseName(),
                request.justification(),
                request.notes());

        applyEditedLines(requisition, request.lines(), tenantId, companyId);
        assignFiscalYear(requisition, actor);

        return toDetail(requisitionRepository.save(requisition));
    }

    /**
     * Refuses a request whose Bikram Sambat date contradicts its own AD date.
     *
     * <p>The BS column is derived from AD on persist, so a mismatch is not a data
     * problem - persisting the recomputed value would silently discard what the
     * client sent and be right about it. It is a client bug: a picker that converted
     * one date while the form held another, or a stale field left over from the row
     * being copied. Left alone it surfaces much later, as a document numbered under
     * a period nobody expected, which is close to undebuggable once it is in a
     * fiscal report.
     *
     * <p>Both entry points call this rather than each carrying its own copy. The
     * create path had one and the update path did not, and that divergence is the
     * whole reason the defect survived: there was nothing forcing the two to stay
     * the same.
     *
     * @param gregorianDate the AD date being written, must not be null
     * @param nepaliDate    the BS date the client supplied, may be null or blank
     * @throws ValidationException when the two disagree
     */
    private void rejectContradictoryNepaliDate(LocalDate gregorianDate, String nepaliDate) {
        if (nepaliDate == null || nepaliDate.isBlank()) {
            return;
        }
        String expected = projectToBs(gregorianDate);
        if (expected != null && !expected.equals(nepaliDate.trim())) {
            throw new ValidationException(
                    "Nepali request date " + nepaliDate
                            + " does not match " + gregorianDate + ", which is " + expected
                            + " in Bikram Sambat");
        }
    }

    /**
     * Rebuilds a requisition's line set from an edit.
     *
     * <p><strong>Rows are reused and orphans are flushed out before anything new
     * is inserted.</strong> The obvious implementation - clear the collection and
     * add the replacements, letting {@code orphanRemoval} do the deleting - fails
     * on a grid edit, because Hibernate runs a flush queue with inserts ahead of
     * deletes. Editing three lines renumbers every line after the first edit, so
     * the replacement for line 1 is inserted while the original line 1 is still on
     * the table, and {@code uk_purchase_requisition_lines_requisition_number}
     * rejects it. Deleting first, flushing, then inserting leaves no window in
     * which a freed ordinal is still occupied.
     */
    private void applyEditedLines(PurchaseRequisition requisition,
                                  List<CreateRequisitionLineRequest> lineRequests,
                                  Long tenantId, Long companyId) {

        int wanted = lineRequests.size();
        List<PurchaseRequisitionLine> dropped = requisition.getLines().stream()
                .filter(line -> line.getLineNumber() > wanted)
                .toList();

        dropped.forEach(line -> requisition.removeLine(line.getLineNumber()));

        if (!dropped.isEmpty()) {
            // Force the orphan deletes out now, before any replacement claims a
            // freed ordinal. Without this the queue would insert first.
            requisitionRepository.flush();
        }

        int lineNumber = 1;
        for (CreateRequisitionLineRequest lineRequest : lineRequests) {
            UnitService.UnitResponse unit = UnitResolver.resolveUnit(unitService, lineRequest.uomCode());
            Long unitId = unit.getId();

            Optional<PurchaseRequisitionLine> existing = requisition.findLineByNumber(lineNumber);
            if (existing.isPresent()) {
                existing.get().revise(
                        lineRequest.itemId(),
                        lineRequest.itemCode(),
                        lineRequest.itemName(),
                        lineRequest.uomCode(),
                        unitId,
                        lineRequest.quantity(),
                        lineRequest.estimatedUnitPrice(),
                        lineRequest.taxRate(),
                        lineRequest.remarks());
            } else {
                requisition.addLine(PurchaseRequisitionLine.create(
                        lineNumber,
                        lineRequest.itemId(),
                        lineRequest.itemCode(),
                        lineRequest.itemName(),
                        lineRequest.uomCode(),
                        unitId,
                        lineRequest.quantity(),
                        lineRequest.estimatedUnitPrice(),
                        lineRequest.taxRate(),
                        lineRequest.remarks()));
            }
            lineNumber++;
        }

        requisition.recalculateTotals();
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(PurchaseRequisitionPermissions.VIEW)
    public RequisitionDetailResponse getById(Long tenantId, Long companyId, Long id) {
        return toDetail(load(tenantId, companyId, id));
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(PurchaseRequisitionPermissions.VIEW)
    public Page<RequisitionListResponse> list(Long tenantId, Long companyId, Long branchId,
                                              PurchaseRequisitionStatus status, Pageable pageable) {

        Page<PurchaseRequisition> page;
        if (branchId != null && status != null) {
            page = requisitionRepository
                    .findByTenantIdAndCompanyIdAndBranchIdAndStatus(tenantId, companyId, branchId, status, pageable);
        } else if (branchId != null) {
            page = requisitionRepository
                    .findByTenantIdAndCompanyIdAndBranchId(tenantId, companyId, branchId, pageable);
        } else if (status != null) {
            page = requisitionRepository
                    .findByTenantIdAndCompanyIdAndStatus(tenantId, companyId, status, pageable);
        } else {
            page = requisitionRepository.findByTenantIdAndCompanyId(tenantId, companyId, pageable);
        }

        return page.map(this::toList);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(PurchaseRequisitionPermissions.VIEW)
    public Page<RequisitionListResponse> listMine(Long tenantId, Long companyId, Long requestedBy,
                                                  Pageable pageable) {

        if (requestedBy == null) {
            throw new ValidationException("An authenticated user is required to list their requisitions");
        }
        return requisitionRepository
                .findByTenantIdAndCompanyIdAndRequestedByOrderByCreatedAtDesc(
                        tenantId, companyId, requestedBy, pageable)
                .map(this::toList);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(value = {PurchaseRequisitionPermissions.APPROVE, PurchaseRequisitionPermissions.REJECT},
            logical = RequiresPermission.LogicalOperator.OR)
    public Page<RequisitionListResponse> approvalInbox(Long tenantId, Long companyId, Long approverId,
                                                       Pageable pageable) {

        if (approverId == null) {
            throw new ValidationException("An authenticated user is required to read an approval inbox");
        }

        List<String> roleNames = context.getCurrentUserRoles().stream()
                .filter(role -> role != null && !role.isBlank())
                .map(String::trim)
                .toList();

        List<PurchaseRequisitionApproval> candidates = approvalRepository.findActionableStepsFor(
                tenantId, companyId, approverId,
                roleNames.isEmpty() ? List.of(NO_ROLE_MATCH) : roleNames);

        // The query returns every pending step naming this user, including steps
        // that are not yet current. A submission materialises the whole chain as
        // pending at once, so the assignee of step 2 matches the query from the
        // moment it is submitted - but cannot act until step 1 is decided, and
        // showing it would put a document in an approver's queue that 403s when
        // they open it.
        Map<Long, PurchaseRequisition> distinct = new LinkedHashMap<>();
        candidates.stream()
                .filter(this::isCurrentStep)
                .map(PurchaseRequisitionApproval::getRequisition)
                .forEach(requisition ->
                        distinct.putIfAbsent(requisition.getId(), requisition));

        // Oldest submission first: an approver's queue is a fairness queue, and a
        // new arrival must never jump ahead of one that has been waiting since
        // before the approver opened the page. Never-submitted documents sort last
        // so a null submittedAt cannot masquerade as the oldest item.
        List<PurchaseRequisition> inbox = distinct.values().stream()
                .sorted(Comparator
                        .comparing(PurchaseRequisition::getSubmittedAt,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(PurchaseRequisition::getId))
                .toList();

        int from = (int) Math.min(pageable.getOffset(), inbox.size());
        int to = Math.min(from + pageable.getPageSize(), inbox.size());

        return new PageImpl<>(
                inbox.subList(from, to).stream().map(this::toList).toList(),
                pageable,
                inbox.size());
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(PurchaseRequisitionPermissions.VIEW)
    public RequisitionActionabilityResponse actionability(Long tenantId, Long companyId, Long id,
                                                          String actor) {

        PurchaseRequisition requisition = load(tenantId, companyId, id);
        PurchaseRequisitionStatus status = requisition.getStatus();

        List<String> blockers = new ArrayList<>();
        boolean admin = context.hasPermission(PurchaseRequisitionPermissions.ADMIN)
                || context.hasRole("TENANT_ADMIN")
                || context.hasRole("SUPER_ADMIN")
                || context.hasRole("OWNER");
        boolean requester = isRequester(requisition, actor);

        // Each block answers the same two questions in the same order: may this
        // operation be performed at all right now (permission), and is this
        // caller entitled to perform it on this document (ownership and the
        // approval chain)? Reordering them produces a report that explains the
        // wrong half of the refusal.

        // --- edit -----------------------------------------------------------
        boolean editable = requisition.isEditable();
        boolean canEdit = editable
                && context.hasPermission(PurchaseRequisitionPermissions.EDIT)
                && (requester || admin);
        if (!editable) {
            blockers.add("Content can only be changed while the requisition is DRAFT.");
        } else if (!context.hasPermission(PurchaseRequisitionPermissions.EDIT)) {
            blockers.add("Editing a requisition requires the " + PurchaseRequisitionPermissions.EDIT
                    + " permission.");
        } else if (!canEdit) {
            blockers.add("Only user " + requisition.getRequestedBy()
                    + ", who raised this requisition, or a purchase administrator, may edit it.");
        }

        // --- submit ---------------------------------------------------------
        boolean submittable = status.canTransitionTo(PurchaseRequisitionStatus.SUBMITTED);
        boolean canSubmit = submittable
                && context.hasPermission(PurchaseRequisitionPermissions.SUBMIT)
                && (requester || admin);
        if (!canSubmit) {
            if (!submittable) {
                blockers.add("A requisition that is " + status + " cannot be submitted.");
            } else if (!context.hasPermission(PurchaseRequisitionPermissions.SUBMIT)) {
                blockers.add("Submitting a requisition requires the "
                        + PurchaseRequisitionPermissions.SUBMIT + " permission.");
            } else {
                blockers.add("Only the user who raised this requisition, or a tenant administrator, may submit it.");
            }
        }

        // --- decide ---------------------------------------------------------
        Optional<PurchaseRequisitionApproval> awaiting = findCurrentStep(tenantId, companyId, id);
        boolean decidable = isDecidable(requisition);
        boolean decisionEntitled = context.hasPermission(PurchaseRequisitionPermissions.APPROVE)
                || context.hasPermission(PurchaseRequisitionPermissions.REJECT);
        boolean assigned = awaiting.filter(step -> isAssignee(step, actor)).isPresent();
        boolean canDecide = decidable && decisionEntitled && assigned;

        if (decidable && awaiting.isEmpty()) {
            blockers.add("No approval step is awaiting a decision on this requisition.");
        } else if (decidable && !decisionEntitled) {
            blockers.add("Recording a decision requires the " + PurchaseRequisitionPermissions.APPROVE
                    + " or the " + PurchaseRequisitionPermissions.REJECT + " permission.");
        } else if (decidable && !assigned) {
            blockers.add("The pending approval step is assigned to someone else.");
        }

        // --- cancel ---------------------------------------------------------
        boolean cancellable = status.canTransitionTo(PurchaseRequisitionStatus.CANCELLED);
        boolean canCancel = cancellable
                && context.hasPermission(PurchaseRequisitionPermissions.CANCEL)
                && (requester || admin);
        if (!canCancel && cancellable) {
            if (!context.hasPermission(PurchaseRequisitionPermissions.CANCEL)) {
                blockers.add("Cancelling a requisition requires the "
                        + PurchaseRequisitionPermissions.CANCEL + " permission.");
            } else {
                blockers.add("Only the requester or a purchase administrator may cancel this requisition.");
            }
        }

        // --- delete ---------------------------------------------------------
        boolean draft = status == PurchaseRequisitionStatus.DRAFT;
        boolean canDelete = draft
                && context.hasPermission(PurchaseRequisitionPermissions.DELETE)
                && (requester || admin);
        if (draft && !canDelete) {
            if (!context.hasPermission(PurchaseRequisitionPermissions.DELETE)) {
                blockers.add("Deleting a draft requires the "
                        + PurchaseRequisitionPermissions.DELETE + " permission.");
            } else {
                blockers.add("Only the requester or a purchase administrator may delete this draft.");
            }
        }

        // --- convert to purchase order --------------------------------------
        boolean convertible = status.isConvertibleToPurchaseOrder();
        boolean canAward = convertible
                && context.hasPermission(PurchaseRequisitionPermissions.CONVERT_TO_PO)
                && !requisition.outstandingLines().isEmpty();
        if (convertible && !context.hasPermission(PurchaseRequisitionPermissions.CONVERT_TO_PO)) {
            blockers.add("Converting a requisition to a purchase order requires the "
                    + PurchaseRequisitionPermissions.CONVERT_TO_PO + " permission.");
        } else if (convertible && requisition.outstandingLines().isEmpty()) {
            blockers.add("Every approved line on this requisition has already been ordered.");
        }

        return new RequisitionActionabilityResponse(
                requisition.getId(),
                status,
                canEdit,
                canSubmit,
                canDecide,
                canCancel,
                canDelete,
                canAward,
                awaiting.map(PurchaseRequisitionApproval::getSequenceNo).orElse(null),
                blockers);
    }

    @Override
    @Transactional
    @RequiresPermission(PurchaseRequisitionPermissions.SUBMIT)
    public RequisitionDetailResponse submit(Long tenantId, Long companyId, Long id, String actor) {
        PurchaseRequisition requisition = loadForUpdate(tenantId, companyId, id);
        requireOwnershipOrAdmin(requisition, actor, "submit");
        assignFiscalYear(requisition, actor);
        PurchaseRequisitionStatus from = requisition.getStatus();

        boolean approvalRequired = workflowConfiguration.isPurchaseRequisitionApprovalRequired(
                tenantId, companyId);

        requisition.submit(approvalChainFor(tenantId, companyId, requisition, approvalRequired), actor);
        requisition.recordTransition(from, PurchaseRequisitionStatus.SUBMITTED, actor, null);

        if (!approvalRequired) {
            // Two history entries, not one. Collapsing DRAFT -> APPROVED would hide
            // that the document went out for approval and came back inside the same
            // request, and the submitter's own "submitted" moment would vanish from
            // the trail. Recorded separately, the history reads exactly what a reader
            // of the details screen sees: submitted, then approved, with nothing in
            // between because nothing was needed.
            requisition.approve(PurchaseRequisitionApproval.SYSTEM_ACTOR);
            requisition.recordTransition(PurchaseRequisitionStatus.SUBMITTED,
                    PurchaseRequisitionStatus.APPROVED,
                    PurchaseRequisitionApproval.SYSTEM_ACTOR,
                    PurchaseRequisitionApproval.AUTO_APPROVAL_COMMENT);
        }

        return toDetail(requisitionRepository.save(requisition));
    }

    /**
     * Resolves the approval chain this document enters at submission.
     *
     * <p>With approval required, this is the configured policy, snapshotted by
     * {@link PurchaseRequisition#submit} so that a policy edited while the document
     * waits cannot retroactively change or skip an obligation.
     *
     * <p>With approval switched off, there is no obligation to snapshot and the
     * policy is deliberately <em>not</em> read. Materialising the configured steps
     * and then discharging them would write rows naming approvers who were never
     * asked and never decided - a history that says "Branch Manager approved this"
     * for a document no branch manager ever saw. Instead the document gets a single
     * completed step that says plainly that nobody was required to approve it. The
     * chain is not empty, which {@code submit} requires and which keeps "there is
     * always an approval record" true for every requisition in the system.
     *
     * @param tenantId         the tenant scope
     * @param companyId        the company scope
     * @param requisition      the requisition being submitted
     * @param approvalRequired whether the company's workflow settings demand approval
     * @return the steps to record against the requisition, never empty
     */
    private List<PurchaseRequisitionApproval> approvalChainFor(
            Long tenantId, Long companyId, PurchaseRequisition requisition, boolean approvalRequired) {

        if (!approvalRequired) {
            return List.of(PurchaseRequisitionApproval.autoApproved(
                    1, requisition.getEstimatedTotalAmount(), requisition.getBranchId()));
        }

        return buildApprovalChain(
                tenantId, companyId, requisition.getBranchId(), requisition.getEstimatedTotalAmount());
    }

    @Override
    @Transactional
    @RequiresPermission(value = {PurchaseRequisitionPermissions.APPROVE, PurchaseRequisitionPermissions.REJECT},
            logical = RequiresPermission.LogicalOperator.OR)
    public RequisitionDetailResponse decide(Long tenantId, Long companyId, Long id,
                                            ApprovalDecisionRequest request, String actor) {

        // Approving and rejecting are separate permissions, so the endpoint can
        // only be gated as "either". Which one it is depends on the body, and
        // that is settled before anything is loaded or locked: an unauthorised
        // caller must not be able to take a row-level write lock by posting a
        // decision the service was always going to refuse.
        if (request.action() == null) {
            throw new ValidationException("An approval decision is required");
        }
        requireDecisionPermission(request.action());

        PurchaseRequisition requisition = loadForUpdate(tenantId, companyId, id);
        PurchaseRequisitionStatus from = requisition.getStatus();

        PurchaseRequisitionApproval step = requireCurrentStep(tenantId, companyId, requisition.getId());
        requireAssignee(step, actor);

        switch (request.action()) {
            case APPROVED -> {
                applyLineApprovals(requisition, request);
                applyLineOutcomes(requisition, request);
                step.decide(ApprovalAction.APPROVED, actor, request.comments());

                // The document stays SUBMITTED until the last step signs off. The
                // steps in between are audited on their own approval rows, which
                // already carry who and when; writing a status-history row for a
                // move that did not happen would be noise, and the trail cannot
                // represent SUBMITTED -> SUBMITTED anyway.
                if (isLastStep(requisition, step)) {
                    requireRemainingDemand(requisition);
                    requisition.approve(actor);
                    requisition.recordTransition(from, PurchaseRequisitionStatus.APPROVED,
                            actor, request.comments());
                }
            }
            case REJECTED -> {
                requireNoLineOutcomes(request);
                step.decide(ApprovalAction.REJECTED, actor, request.comments());
                requisition.reject(actor, request.comments());
                requisition.recordTransition(from, PurchaseRequisitionStatus.REJECTED, actor, request.comments());
            }
            default -> throw new ValidationException(
                    request.action() + " is not a decision an approver can record");
        }

        return toDetail(requisitionRepository.save(requisition));
    }

    /**
     * Narrows the endpoint gate to the decision actually being recorded.
     *
     * <p>{@code decide()} carries {@code @RequiresPermission({APPROVE, REJECT},
     * logical = OR)}, because the annotation cannot see the request body and one
     * endpoint serves both decisions. That gate is deliberately coarse - it admits
     * any approver to the method - and this method then refuses the one decision
     * the caller is not entitled to. Anything coarser, such as a single APPROVE
     * annotation, would refuse a legitimate rejection and hand a reject-only
     * approver no way to stop spend at all.
     *
     * @param action the decision the caller asked to record
     * @throws ForbiddenException when the caller lacks that decision's permission
     */
    private void requireDecisionPermission(ApprovalAction action) {
        String required = switch (action) {
            case APPROVED -> PurchaseRequisitionPermissions.APPROVE;
            case REJECTED -> PurchaseRequisitionPermissions.REJECT;
            default -> throw new ValidationException(
                    action + " is not a decision an approver can record");
        };
        if (!context.hasPermission(required)) {
            throw new ForbiddenException(
                    "Recording a " + action + " decision requires the " + required + " permission");
        }
    }

    /**
     * Refuses an approval that would leave nothing to buy.
     *
     * <p>Rejecting or cancelling every line and then approving the document
     * commits zero budget and records an APPROVED requisition that procurement
     * will never order anything against. The approver almost certainly meant to
     * reject the whole thing, and telling them so is more useful than accepting a
     * document whose only meaning is that it consumed an approval decision.
     */
    private void requireRemainingDemand(PurchaseRequisition requisition) {
        boolean anyCounted = requisition.getLines().stream()
                .anyMatch(line -> line.isCounted() && line.getApprovedQuantity().signum() > 0);
        if (!anyCounted) {
            throw new ValidationException(
                    "Every line on " + requisition.getPrNumber()
                            + " has been refused, withdrawn or granted zero; reject or return the "
                            + "requisition instead of approving it");
        }
    }

    @Override
    @Transactional
    @RequiresPermission(PurchaseRequisitionPermissions.CONVERT_TO_PO)
    public RequisitionDetailResponse awardToOrder(Long tenantId, Long companyId, Long id,
                                                  OrderAwardRequest request, String actor) {

        requirePurchaseOrderInScope(tenantId, companyId, request.purchaseOrderId());

        PurchaseRequisition requisition = loadForUpdate(tenantId, companyId, id);
        PurchaseRequisitionStatus from = requisition.getStatus();

        if (!from.isConvertibleToPurchaseOrder()) {
            throw new ValidationException(
                    "Requisition " + requisition.getPrNumber() + " is " + from
                            + "; approved quantity can only be ordered from an APPROVED requisition");
        }

        // The whole award is kept, including its purchase-order line reference.
        // Collapsing each award to a bare quantity threw that reference away, so
        // every link but the first was written with a null PO line - losing the
        // ability to answer "which order line satisfied this demand?", which is
        // the entire reason the link table exists for split awards.
        Map<Long, OrderAwardRequest.LineAward> awards = new LinkedHashMap<>();
        for (OrderAwardRequest.LineAward award : request.awards()) {
            if (awards.put(award.requisitionLineId(), award) != null) {
                throw new ValidationException(
                        "Duplicate award for requisition line " + award.requisitionLineId());
            }
        }

        // Resolve and validate every award before mutating anything.
        //
        // The previous version applied registerOrdered() line by line, so an award
        // that over-committed line 3 threw with lines 1 and 2 already mutated.
        // Inside a transaction that rolls back, but the entity is left dirty for
        // any caller that catches the exception, and the failure surfaced as a
        // message about line 3 rather than about the award as a whole.
        Map<Long, PurchaseRequisitionLine> resolved = new LinkedHashMap<>();
        awards.forEach((lineId, award) -> {
            PurchaseRequisitionLine line = findLine(requisition, lineId);
            if (!line.isOrderable()) {
                throw new ValidationException(
                        "Requisition line " + line.getLineNumber() + " is " + line.getStatus()
                                + " and cannot be ordered against");
            }
            BigDecimal ordered = award.orderedQuantity();
            if (ordered == null || ordered.signum() <= 0) {
                throw new ValidationException(
                        "Ordered quantity for requisition line " + lineId + " must be greater than zero");
            }
            BigDecimal remaining = line.getApprovedQuantity().subtract(line.getOrderedQuantity());
            if (ordered.compareTo(remaining) > 0) {
                throw new ValidationException(
                        "Awarding " + ordered.toPlainString() + " against requisition line "
                                + line.getLineNumber() + " exceeds the "
                                + remaining.toPlainString()
                                + " still approved but un-ordered on that line");
            }
            resolved.put(lineId, line);
        });

        resolved.forEach((lineId, line) -> line.registerOrdered(awards.get(lineId).orderedQuantity()));

        // Conversion is the last thing that happens, after every line quantity has
        // been registered. The header therefore cannot claim to have been converted
        // against order links that were never written, and a second award of an
        // already converted requisition is refused by convertToPO() rather than
        // being allowed to quietly top up a closed document.
        requisition.convertToPO(actor);
        requisition.recordTransition(from, PurchaseRequisitionStatus.CONVERTED_TO_PO, actor, null);

        // The parent is written and flushed before the order links that reference
        // it. Persisting the child rows first meant the links could be flushed
        // while the header still carried its pre-award status and ordered total,
        // so anything reading inside the transaction - or any constraint evaluated
        // as the links land - saw an inconsistent pair.
        requisitionRepository.save(requisition);
        requisitionRepository.flush();

        awards.forEach((lineId, award) -> {
            PurchaseRequisitionLine line = resolved.get(lineId);
            PurchaseRequisitionOrderLink link = PurchaseRequisitionOrderLink.award(
                    requisition,
                    line.getId(),
                    request.purchaseOrderId(),
                    award.purchaseOrderLineId(),
                    award.orderedQuantity(),
                    actor);
            link.assignScope(tenantId, companyId);
            orderLinkRepository.save(link);
        });

        return toDetail(requisition);
    }

    /**
     * Rejects an award that names a purchase order outside the caller's scope.
     *
     * <p>There is no database foreign key from an order link to a purchase order
     * by design - the two documents are separate aggregates that must stay
     * independently loadable. That makes the application layer responsible, and
     * an unqualified lookup would let a caller in one company write a link to a
     * purchase order belonging to another.
     *
     * @throws ResourceNotFoundException when no such order exists in this scope
     */
    private void requirePurchaseOrderInScope(Long tenantId, Long companyId, Long purchaseOrderId) {
        if (purchaseOrderId == null) {
            throw new ValidationException("A purchase order is required for an order award");
        }
        if (!purchaseOrderRepository.existsByTenantIdAndCompanyIdAndId(tenantId, companyId, purchaseOrderId)) {
            throw new ResourceNotFoundException(
                    "Purchase order " + purchaseOrderId + " was not found in this company");
        }
    }

    @Override
    @Transactional
    @RequiresPermission(PurchaseRequisitionPermissions.CANCEL)
    public RequisitionDetailResponse cancel(Long tenantId, Long companyId, Long id,
                                            String reason, String actor) {

        PurchaseRequisition requisition = loadForUpdate(tenantId, companyId, id);
        requireOwnershipOrAdmin(requisition, actor, "cancel");
        PurchaseRequisitionStatus from = requisition.getStatus();

        requisition.cancel(actor, reason);
        requisition.recordTransition(from, PurchaseRequisitionStatus.CANCELLED, actor, reason);
        return toDetail(requisitionRepository.save(requisition));
    }

    @Override
    @Transactional
    @RequiresPermission(PurchaseRequisitionPermissions.DELETE)
    public void delete(Long tenantId, Long companyId, Long id, String actor) {
        PurchaseRequisition requisition = load(tenantId, companyId, id);
        requireOwnershipOrAdmin(requisition, actor, "delete");
        if (requisition.getStatus() != PurchaseRequisitionStatus.DRAFT) {
            throw new ValidationException(
                    "Only a draft requisition can be deleted; " + requisition.getPrNumber()
                            + " is " + requisition.getStatus());
        }
        requisitionRepository.delete(requisition);
    }

    // ---- Ownership --------------------------------------------------------

    /**
     * Restricts an operation to the user who raised the requisition.
     *
     * <p>Used for {@code submit}, where acting on someone else's draft would put a
     * document into the approval chain under the wrong person's name: the audit
     * trail would record that they asked for it and approved it.
     *
     * @param requisition the loaded document
     * @param actor       the acting user id
     * @param operation   the operation name, for the error message
     * @throws ForbiddenException when the caller did not raise the requisition
     */
    private void requireOwnership(PurchaseRequisition requisition, String actor, String operation) {
        if (!isRequester(requisition, actor)) {
            throw new ForbiddenException(
                    "Only the user who raised " + requisition.getPrNumber() + " may " + operation
                            + " it; it belongs to user " + requisition.getRequestedBy());
        }
    }

    /**
     * Restricts an operation to the requester, or to a caller holding
     * {@link PurchaseRequisitionPermissions#ADMIN}.
     *
     * <p>Used for {@code edit}, {@code cancel} and {@code delete}, where an
     * administrator needs a way in: the practical case is a user who raises a draft
     * and then leaves, and nobody can clear it out of the queue. The escape hatch
     * is a separate, deliberately narrow permission rather than a general bypass,
     * so granting it does not hand over the whole module.
     *
     * @param requisition the loaded document
     * @param actor       the acting user id
     * @param operation   the operation name, for the error message
     * @throws ForbiddenException when the caller is neither the requester nor an admin
     */
    private void requireOwnershipOrAdmin(PurchaseRequisition requisition, String actor, String operation) {
        if (isRequester(requisition, actor)) {
            return;
        }
        if (context.hasPermission(PurchaseRequisitionPermissions.ADMIN)
                || context.hasRole("TENANT_ADMIN")
                || context.hasRole("SUPER_ADMIN")
                || context.hasRole("OWNER")) {
            return;
        }
        throw new ForbiddenException(
                "Only the user who raised " + requisition.getPrNumber() + ", or a tenant administrator, may " + operation + " it");
    }

    /**
     * @return whether the acting user is the one who raised the requisition
     */
    private boolean isRequester(PurchaseRequisition requisition, String actor) {
        Long requestedBy = requisition.getRequestedBy();
        return requestedBy != null && actor != null
                && String.valueOf(requestedBy).equals(actor.trim());
    }

    // ---- Fiscal year -------------------------------------------------------

    /**
     * Assigns the caller's fiscal year to a document that does not yet have one.
     *
     * <p>Not a hard failure when the caller has selected no period. A purchase
     * requisition is real business data, and refusing to record one because a
     * header is missing would lose the demand; an unassigned document is still
     * visible to its requester and can be reconciled once the period is chosen.
     * It is invisible to fiscal-year budget reporting though, so the gap is logged
     * rather than passed over in silence.
     */
    private void assignFiscalYear(PurchaseRequisition requisition, String actor) {
        Long fiscalYearId = context.findFiscalYearId();
        if (fiscalYearId == null) {
            if (requisition.getFiscalYearId() == null) {
                log.warn("Requisition {} is being recorded with no fiscal year: the request carried no "
                                + "X-Fiscal-Year-Id header. It will not appear in fiscal-year budget "
                                + "reporting until it is assigned one.",
                        requisition.getPrNumber() == null ? "(unsaved)" : requisition.getPrNumber());
            }
            return;
        }
        requisition.assignFiscalYear(fiscalYearId);
    }

    // ---- Scope and loading -------------------------------------------------

    private void requireScope(Long tenantId, Long companyId, Long requestedBy) {
        if (tenantId == null || companyId == null) {
            throw new ValidationException("Tenant and company context are required");
        }
        if (requestedBy == null) {
            throw new ValidationException("An authenticated user is required to raise a requisition");
        }
    }

    private PurchaseRequisition load(Long tenantId, Long companyId, Long id) {
        return requisitionRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Purchase requisition " + id + " was not found"));
    }

    private PurchaseRequisition loadForUpdate(Long tenantId, Long companyId, Long id) {
        return requisitionRepository.findByIdForUpdate(tenantId, companyId, id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Purchase requisition " + id + " was not found"));
    }

    private PurchaseRequisitionLine findLine(PurchaseRequisition requisition, Long lineId) {
        return requisition.findLineById(lineId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Line " + lineId + " does not belong to requisition " + requisition.getPrNumber()));
    }

    // ---- Approval chain ----------------------------------------------------

    private List<PurchaseRequisitionApproval> buildApprovalChain(
            Long tenantId, Long companyId, Long branchId, BigDecimal amount) {

        List<PurchaseRequisitionApprovalPolicy> policies =
                policyRepository
                        .findByTenantIdAndCompanyIdAndBranchIdAndActiveTrueOrderBySequenceNoAsc(
                                tenantId, companyId, branchId);

        if (policies.isEmpty()) {
            policies = policyRepository
                    .findByTenantIdAndCompanyIdAndBranchIdIsNullAndActiveTrueOrderBySequenceNoAsc(
                            tenantId, companyId);

            // Falling back here is the documented behaviour, not a defect: the
            // company-wide chain is the catch-all for branches without one of their
            // own. It is still worth a log line when a company mixes both styles,
            // because that combination is where an approval-control gap hides - the
            // branch runs on the default chain, and if the default is looser than a
            // sibling branch's, a requester can reach the weaker control simply by
            // naming a branch that has no policy of its own. The fix is
            // configuration (give the branch a chain, or align the default with the
            // strictest one), not a runtime refusal, which would break every
            // legitimate use of the default.
            if (!policies.isEmpty() && policyRepository.existsBranchScopedPolicy(tenantId, companyId)) {
                log.warn("Branch {} has no approval policy of its own; falling back to the "
                                + "company-wide chain. This company also defines branch-scoped "
                                + "policies, so the branch is running on a weaker control than its "
                                + "siblings if the default is less strict.",
                        branchId);
            }
        }

        if (policies.isEmpty()) {
            return List.of(permissionDrivenStep(branchId, amount));
        }

        List<PurchaseRequisitionApproval> chain = new ArrayList<>();
        int sequence = 1;
        for (PurchaseRequisitionApprovalPolicy policy : policies) {
            chain.add(policy.appliesTo(amount)
                    ? toStep(policy, sequence++)
                    : PurchaseRequisitionApproval.skipped(
                            sequence++, policy.getApproverRole(), policy.getThresholdMinAmount(),
                            policy.getBranchId()));
        }

        boolean hasPending = chain.stream().anyMatch(PurchaseRequisitionApproval::isPending);
        if (!hasPending) {
            // Policies exist for this scope, so the company has made a deliberate
            // statement about who may authorise spend and this document simply
            // falls outside every band it declared. Falling back to the
            // permission-addressed step here would quietly widen a control the
            // company configured on purpose, so it stays an error - but naming the
            // bands makes it fixable without reading this code.
            throw new ValidationException(
                    "The approval policies configured for this branch cover amounts up to "
                            + highestBandCeiling(policies) + ", which does not include a "
                            + "requisition of value " + amount
                            + ". Extend the highest policy's upper bound, or add a policy "
                            + "covering this amount.");
        }
        return chain;
    }

    /**
     * The step a requisition gets when the company has configured no approval
     * policy at all.
     *
     * <p><strong>Why this exists.</strong> Submission used to be refused outright
     * with "no approval policy is configured". That was safe but left the module
     * unusable, because no API, screen or seed script exists through which a
     * policy could be created - every company was permanently blocked. Rather
     * than auto-approving, which would commit spend with no control at all, the
     * requisition gets one open step that any holder of the approve permission
     * may decide. Spend is still refused without a second pair of eyes; the
     * requisition merely stops requiring configuration nobody can perform.
     *
     * <p>Configured policies always win. The moment a company adds one, this
     * path stops being reached, and the fallback silently reverts to named
     * approvers and amount-banded sign-off.
     */
    private PurchaseRequisitionApproval permissionDrivenStep(Long branchId, BigDecimal amount) {
        log.warn("No approval policy is configured for branch {} and the company has no "
                        + "default chain either. Routing the requisition to a single approval step "
                        + "decided by the {} permission, so it can still be approved. Configure an "
                        + "approval policy to require named approvers or amount-banded sign-off.",
                branchId, PurchaseRequisitionPermissions.APPROVE);

        return PurchaseRequisitionApproval.openToApprovers(
                1, amount, branchId, PERMISSION_ADDRESSED_LABEL);
    }

    /**
     * The widest upper bound declared by the given policies, for error messages.
     *
     * @param policies the resolved chain, guaranteed non-empty
     * @return the largest upper bound, or "unbounded" when any band is open-ended
     */
    private String highestBandCeiling(List<PurchaseRequisitionApprovalPolicy> policies) {
        return policies.stream()
                .map(PurchaseRequisitionApprovalPolicy::getThresholdMaxAmount)
                .filter(java.util.Objects::nonNull)
                .max(BigDecimal::compareTo)
                .map(BigDecimal::toPlainString)
                .orElse("an unbounded amount");
    }

    private PurchaseRequisitionApproval toStep(PurchaseRequisitionApprovalPolicy policy, int sequence) {
        return policy.getApproverUserId() != null
                ? PurchaseRequisitionApproval.assignedToUser(
                        sequence, policy.getApproverUserId(), policy.getApproverRole(),
                        policy.getThresholdMinAmount(), policy.getBranchId())
                : PurchaseRequisitionApproval.assignedToRole(
                        sequence, policy.getApproverRoleId(), policy.getApproverRole(),
                        policy.getThresholdMinAmount(), policy.getBranchId());
    }

    /**
     * The step an approver may act on: the lowest sequence still pending.
     *
     * @return the current step, or empty when nothing is awaiting a decision
     */
    private Optional<PurchaseRequisitionApproval> findCurrentStep(
            Long tenantId, Long companyId, Long requisitionId) {

        return approvalRepository.findPendingSteps(tenantId, companyId, requisitionId).stream().findFirst();
    }

    private PurchaseRequisitionApproval requireCurrentStep(Long tenantId, Long companyId, Long requisitionId) {
        return findCurrentStep(tenantId, companyId, requisitionId)
                .orElseThrow(() -> new ValidationException(
                        "This requisition has no approval step awaiting a decision"));
    }

    /**
     * Confirms the caller is entitled to act on the current approval step.
     *
     * <p>A step addresses itself in one of three forms - a named user, a named
     * role, or the approve permission - and all three are honoured. Role-assigned
     * steps used to be rejected outright with a message telling the caller to go
     * and authorise themselves against their roles, which meant
     * {@code PurchaseRequisitionApprovalPolicy.forRole(...)} - the form most
     * companies actually want, because a role survives an approver leaving -
     * produced chains that no one could ever complete.
     *
     * <p>Membership is resolved from the signed access token, never from the
     * request, so a caller cannot widen their own authority.
     *
     * @param step  the step awaiting a decision
     * @param actor the acting user id
     * @throws UnauthorizedException when the caller is not entitled to act on the step
     */
    private void requireAssignee(PurchaseRequisitionApproval step, String actor) {
        if (!isAssignee(step, actor)) {
            if (step.isRoleAssigned()) {
                throw new UnauthorizedException(
                        "Approval step " + step.getSequenceNo() + " is assigned to role "
                                + step.getApproverRole() + ", which the acting user does not hold");
            }
            throw new UnauthorizedException("Only the assigned approver may act on this requisition");
        }
        if (step.isPermissionAssigned()
                && !context.hasPermission(PurchaseRequisitionPermissions.APPROVE)
                && !context.hasPermission(PurchaseRequisitionPermissions.REJECT)) {
            // Redundant while decide() keeps its @RequiresPermission, and kept
            // deliberately: should that annotation ever be dropped, a permission
            // addressed step would otherwise stop checking authority altogether,
            // silently. Failing loudly here is the safe direction to break in.
            // Either decision permission is enough, because refusing an open step
            // needs no more authority than clearing it.
            throw new UnauthorizedException(
                    "Deciding a requisition requires the "
                            + PurchaseRequisitionPermissions.APPROVE + " or the "
                            + PurchaseRequisitionPermissions.REJECT + " permission");
        }
    }

    /**
     * Non-throwing form of {@link #requireAssignee}, for the actionability report.
     */
    private boolean isAssignee(PurchaseRequisitionApproval step, String actor) {
        if (step.isPermissionAssigned()) {
            return true;
        }
        if (context.hasRole("TENANT_ADMIN")
                || context.hasRole("SUPER_ADMIN")
                || context.hasRole("OWNER")
                || context.hasPermission(PurchaseRequisitionPermissions.ADMIN)) {
            return true;
        }
        if (step.isRoleAssigned()) {
            return step.isRoleHeldBy(context.getCurrentUserRoles());
        }
        Long actorId = parseActor(actor);
        return actorId != null && step.isAssignedTo(actorId);
    }

    /**
     * @return whether the document is sitting in front of an approver at all
     */
    private boolean isDecidable(PurchaseRequisition requisition) {
        return requisition.getStatus().isAwaitingApproval();
    }

    /**
     * Whether a pending step is the one the document is actually waiting on.
     *
     * <p>Submission materialises every applicable step as pending at once, so "is
     * this step pending" is not the same question as "can this step be acted on".
     */
    private boolean isCurrentStep(PurchaseRequisitionApproval step) {
        if (step.getRequisition() == null || !isDecidable(step.getRequisition())) {
            return false;
        }
        return step.getRequisition().getApprovals().stream()
                .filter(PurchaseRequisitionApproval::isPending)
                .min(Comparator.comparing(PurchaseRequisitionApproval::getSequenceNo))
                .map(current -> current.getSequenceNo().equals(step.getSequenceNo()))
                .orElse(false);
    }

    private Long parseActor(String actor) {
        try {
            return actor == null ? null : Long.valueOf(actor.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean isLastStep(PurchaseRequisition requisition, PurchaseRequisitionApproval decided) {
        return requisition.getApprovals().stream()
                .filter(step -> !step.isPending())
                .count() == requisition.getApprovals().stream()
                .filter(step -> step.getSequenceNo() <= decided.getSequenceNo())
                .count();
    }

    /**
     * Applies the per-line grants carried by an approving decision.
     *
     * <p><strong>A grant is taken once and may only be lowered afterwards.</strong>
     * The first approving step on a document decides every line: a line the
     * request omits is approved in full, a line it names is approved at the
     * stated quantity. Every later approving step may reduce an existing grant
     * but must never restore one an earlier approver cut, and a line it does not
     * mention is left exactly as it stands.
     *
     * <p>Re-deciding every line on every step is what made partial approval
     * unusable in a multi-step chain: the old code defaulted each line to its
     * <em>requested</em> quantity on each pass, so an approver who granted 60 of
     * 100 was silently overruled by the next approver's empty grant list.
     *
     * @throws ValidationException when a later step attempts to raise a grant
     */
    private void applyLineApprovals(PurchaseRequisition requisition, ApprovalDecisionRequest request) {
        Map<Long, BigDecimal> grants = new LinkedHashMap<>();
        if (request.lineApprovals() != null) {
            request.lineApprovals().forEach(lineApproval -> {
                if (grants.put(lineApproval.lineId(), lineApproval.approvedQuantity()) != null) {
                    throw new ValidationException("Duplicate approval for a requisition line");
                }
            });
        }

        boolean grantsAlreadyDecided = requisition.getApprovals().stream()
                .anyMatch(approval -> approval.getAction() == ApprovalAction.APPROVED);

        if (!grantsAlreadyDecided) {
            for (PurchaseRequisitionLine line : requisition.getLines()) {
                line.approveQuantity(
                        grants.getOrDefault(line.getId(), line.getRequestedQuantity()));
            }
            requisition.recalculateTotals();
            return;
        }

        grants.forEach((lineId, quantity) -> {
            PurchaseRequisitionLine line = requisition.findLineById(lineId)
                    .orElseThrow(() -> new ValidationException(
                            "Line " + lineId + " does not belong to requisition " + requisition.getPrNumber()));

            BigDecimal alreadyGranted = line.getApprovedQuantity();
            if (quantity != null && alreadyGranted != null && quantity.compareTo(alreadyGranted) > 0) {
                throw new ValidationException(
                        "Line " + lineId + " was already granted " + alreadyGranted.toPlainString()
                                + " by an earlier approver; a later approval may reduce that grant but "
                                + "cannot raise it to " + quantity.toPlainString());
            }
            line.approveQuantity(quantity);
        });

        // The header totals are cached, and nothing else refreshes them until the
        // document reaches APPROVED. On an intermediate approval step that left the
        // next approver reading a document whose stated amount still included lines
        // the previous approver had just zeroed. Recompute here so the amount on
        // screen is the amount being decided.
        requisition.recalculateTotals();
    }

    /**
     * Applies the lines an approver refused outright or withdrew.
     *
     * <p>Applied after the grants, so a line that appears in both is refused - a
     * refusal is the stronger statement, and granting quantity on a line that is
     * simultaneously marked as disallowed would leave the register saying both
     * things. The overlap is rejected up front anyway, because a caller sending
     * both is a client bug and should be told rather than silently resolved.
     */
    private void applyLineOutcomes(PurchaseRequisition requisition, ApprovalDecisionRequest request) {
        Set<Long> rejected = sanitiseLineIds(request.rejectedLineIds());
        Set<Long> cancelled = sanitiseLineIds(request.cancelledLineIds());

        if (rejected.isEmpty() && cancelled.isEmpty()) {
            return;
        }

        Set<Long> granted = new HashSet<>();
        if (request.lineApprovals() != null) {
            request.lineApprovals().forEach(approval -> granted.add(approval.lineId()));
        }

        Set<Long> refused = new HashSet<>(rejected);
        refused.addAll(cancelled);

        Set<Long> grantedThenRefused = new HashSet<>(refused);
        grantedThenRefused.retainAll(granted);
        if (!grantedThenRefused.isEmpty()) {
            throw new ValidationException(
                    "Line(s) " + grantedThenRefused
                            + " cannot be both granted and refused in one approval decision");
        }

        Set<Long> rejectedAndCancelled = new HashSet<>(rejected);
        rejectedAndCancelled.retainAll(cancelled);
        if (!rejectedAndCancelled.isEmpty()) {
            throw new ValidationException(
                    "Line(s) " + rejectedAndCancelled
                            + " cannot be both rejected and cancelled in one decision");
        }

        if (!rejected.isEmpty()) {
            requisition.rejectLines(rejected);
        }
        if (!cancelled.isEmpty()) {
            requisition.cancelLines(cancelled);
        }
    }

    /**
     * Refuses line-level outcomes attached to a whole-document decision.
     *
     * <p>Not ignored: silently dropping a rejection the approver believed they had
     * recorded would leave them looking at a decision that says the opposite of
     * what they submitted.
     */
    private void requireNoLineOutcomes(ApprovalDecisionRequest request) {
        if (!sanitiseLineIds(request.rejectedLineIds()).isEmpty()
                || !sanitiseLineIds(request.cancelledLineIds()).isEmpty()) {
            throw new ValidationException(
                    "Individual lines can only be refused or withdrawn as part of an APPROVED "
                            + "decision; a " + request.action()
                            + " decision already applies to the whole requisition");
        }
    }

    private static Set<Long> sanitiseLineIds(List<Long> lineIds) {
        if (lineIds == null || lineIds.isEmpty()) {
            return Set.of();
        }
        Set<Long> cleaned = new HashSet<>();
        for (Long lineId : lineIds) {
            if (lineId == null || lineId <= 0) {
                throw new ValidationException(
                        "Line ids must be positive identifiers; got " + lineId);
            }
            cleaned.add(lineId);
        }
        return cleaned;
    }

    // ---- Numbering ---------------------------------------------------------

    /**
     * Allocates the next document number for the request's fiscal period.
     *
     * <p>The counter is taken under the repository's pessimistic write lock, so two
     * concurrent creations serialise rather than both reading the same value.
     */
    private String allocateNumber(Long tenantId, Long companyId, LocalDate requestDate) {
        String periodKey = fiscalPeriodKey(requestDate);

        DocumentSequence sequence = sequenceRepository
                .findForUpdate(tenantId, companyId, DOCUMENT_TYPE, periodKey)
                .orElseGet(() -> sequenceRepository.save(
                        DocumentSequence.startAt(tenantId, companyId, DOCUMENT_TYPE, periodKey,
                                highestIssuedNumber(tenantId, companyId))));

        return sequence.format(sequence.allocate(), PREFIX, NUMBER_WIDTH);
    }

    /**
     * Builds the counter reset period for a document date.
     *
     * <p><strong>Fiscal year, not calendar year.</strong> The previous key was the
     * bare Gregorian year, so the counter partitioned on a boundary that has
     * nothing to do with the financial year: a company's numbering never reset at
     * the fiscal boundary, and a requisition raised on 15 Ashadh and one raised on
     * 1 Shrawan fell into the same series even though they belong to different
     * budget periods.
     *
     * <p>The Nepali fiscal year runs 1 Shrawan (BS month 4) to the end of Ashadh
     * (BS month 3 of the following year), so a date before Shrawan belongs to the
     * year that began the previous Shrawan.
     *
     * <p>Outside the published calendar range there is no BS year to read, so the
     * Gregorian year is used with an {@code AD} prefix. The prefix matters: a bare
     * "2026" could otherwise be confused with a BS label and, worse, partition the
     * same calendar year in two different ways depending on which branch of this
     * method produced the key.
     */
    private String fiscalPeriodKey(LocalDate requestDate) {
        try {
            NepaliDate bs = NepaliDateConverter.fromAd(requestDate);
            int startYear = bs.month() >= 4 ? bs.year() : bs.year() - 1;
            return startYear + "/" + String.format("%02d", (startYear + 1) % 100);
        } catch (RuntimeException ex) {
            log.debug("Request date {} falls outside the published Bikram Sambat calendar; "
                    + "numbering this document against its Gregorian year instead", requestDate);
            return "AD" + requestDate.getYear();
        }
    }

    /**
     * The highest number this scope has ever issued, used to seed a new counter.
     *
     * <p>Without it, changing the period key would reset numbering: the first
     * requisition of a new fiscal year would be issued PR-000001, which the unique
     * constraint on {@code (tenant_id, company_id, pr_number)} then rejects,
     * failing every create until someone deleted the sequence row by hand.
     */
    private long highestIssuedNumber(Long tenantId, Long companyId) {
        Long highest = requisitionRepository.findHighestIssuedNumber(
                tenantId, companyId, PREFIX, PREFIX.length());
        return highest == null ? 0L : highest;
    }

    // ---- Mapping -----------------------------------------------------------

    /**
     * @return the Bikram Sambat form of {@code adDate}, or {@code null} when it falls
     *         outside the published calendar range
     */
    private static String projectToBs(LocalDate adDate) {
        if (adDate == null) {
            return null;
        }
        try {
            return NepaliDateConverter.fromAd(adDate).toString();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private RequisitionListResponse toList(PurchaseRequisition requisition) {
        return new RequisitionListResponse(
                requisition.getId(),
                requisition.getPrNumber(),
                requisition.getBranchId(),
                requisition.getDepartmentId(),
                requisition.getRequestedBy(),
                requisition.getRequestDate(),
                requisition.getRequestDateNp(),
                requisition.getRequiredByDate(),
                requisition.getRequiredByDateNp(),
                requisition.getStatus(),
                requisition.getPriority(),
                requisition.getCurrency(),
                requisition.getEstimatedTotalAmount(),
                requisition.getApprovedTotalAmount(),
                requisition.getOrderedTotalAmount(),
                requisition.outstandingValue(),
                requisition.getSubmittedAt(),
                requisition.getCreatedAt());
    }

    private RequisitionDetailResponse toDetail(PurchaseRequisition requisition) {
        return new RequisitionDetailResponse(
                requisition.getId(),
                requisition.getPrNumber(),
                requisition.getTenantId(),
                requisition.getCompanyId(),
                requisition.getBranchId(),
                requisition.getDepartmentId(),
                requisition.getFiscalYearId(),
                requisition.getRequestedBy(),
                requisition.getRequestDate(),
                requisition.getRequestDateNp(),
                requisition.getRequestDateNpYear(),
                requisition.getRequiredByDate(),
                requisition.getRequiredByDateNp(),
                requisition.getStatus(),
                requisition.getPriority(),
                requisition.getCurrency(),
                requisition.getEstimatedSubtotalAmount(),
                requisition.getEstimatedTaxAmount(),
                requisition.getEstimatedTotalAmount(),
                requisition.getApprovedTotalAmount(),
                requisition.getOrderedTotalAmount(),
                requisition.outstandingValue(),
                requisition.getWarehouseId(),
                requisition.getWarehouseCode(),
                requisition.getWarehouseName(),
                requisition.getJustification(),
                requisition.getNotes(),
                requisition.getSubmittedAt(),
                requisition.getSubmittedBy(),
                requisition.getApprovedAt(),
                requisition.getApprovedBy(),
                requisition.getRejectedAt(),
                requisition.getRejectedBy(),
                requisition.getRejectionReason(),
                requisition.getConvertedAt(),
                requisition.getConvertedBy(),
                requisition.getCancelledAt(),
                requisition.getCancelledBy(),
                requisition.getCancellationReason(),
                requisition.getVersion(),
                requisition.getLines().stream().map(this::toLine).toList(),
                requisition.getApprovals().stream().map(this::toApproval).toList(),
                requisition.getStatusHistory().stream().map(this::toHistory).toList());
    }

    private RequisitionLineResponse toLine(PurchaseRequisitionLine line) {
        return new RequisitionLineResponse(
                line.getId(),
                line.getLineNumber(),
                line.getItemId(),
                line.getItemCode(),
                line.getItemName(),
                line.getUomCode(),
                line.getUnitId(),
                line.getRequestedQuantity(),
                line.getApprovedQuantity(),
                line.getOrderedQuantity(),
                line.shortfallQuantity(),
                line.getEstimatedUnitPrice(),
                line.getTaxRate(),
                line.approvedNetAmount(),
                line.approvedTaxAmount(),
                line.approvedGrossAmount(),
                line.getStatus(),
                line.getRemarks());
    }

    private RequisitionDetailResponse.ApprovalStepResponse toApproval(PurchaseRequisitionApproval approval) {
        return new RequisitionDetailResponse.ApprovalStepResponse(
                approval.getId(),
                approval.getSequenceNo(),
                approval.getApproverId(),
                approval.getApproverRoleId(),
                approval.getApproverRole(),
                approval.getAmountThreshold(),
                approval.getAction(),
                approval.getActedBy(),
                approval.getActedAt(),
                approval.getComments());
    }

    private RequisitionDetailResponse.RequisitionHistoryResponse toHistory(
            com.erp.business.procurement.purchaserequisition.domain.PurchaseRequisitionStatusHistory history) {
        return new RequisitionDetailResponse.RequisitionHistoryResponse(
                history.getId(),
                history.getFromStatus(),
                history.getToStatus(),
                history.getChangedBy(),
                history.getChangedAt(),
                history.getReason());
    }
}
