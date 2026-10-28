import { api } from "./api";
import type { PageResponse } from "@/types/api";

/**
 * Mirrors the backend `PurchaseRequisitionStatus` enum. Jackson rejects any value
 * outside this union, so the union is the contract rather than a convenience.
 */
export const REQUISITION_STATUSES = [
  "DRAFT",
  "SUBMITTED",
  "APPROVED",
  "REJECTED",
  "CANCELLED",
  "CONVERTED_TO_PO",
] as const;
export type RequisitionStatus = (typeof REQUISITION_STATUSES)[number];

export const REQUISITION_STATUS_LABELS: Record<RequisitionStatus, string> = {
  DRAFT: "Draft",
  SUBMITTED: "Submitted",
  APPROVED: "Approved",
  REJECTED: "Rejected",
  CANCELLED: "Cancelled",
  CONVERTED_TO_PO: "Converted to PO",
};

/** Mirrors the backend `RequisitionPriority` enum. */
export const REQUISITION_PRIORITIES = ["LOW", "NORMAL", "HIGH", "URGENT"] as const;
export type RequisitionPriority = (typeof REQUISITION_PRIORITIES)[number];

export const REQUISITION_PRIORITY_LABELS: Record<RequisitionPriority, string> = {
  LOW: "Low",
  NORMAL: "Normal",
  HIGH: "High",
  URGENT: "Urgent",
};

/** Mirrors the backend `PurchaseRequisitionLineStatus` enum. */
export const LINE_STATUSES = [
  "REQUESTED",
  "APPROVED",
  "PARTIALLY_APPROVED",
  "PARTIALLY_ORDERED",
  "ORDERED",
  "REJECTED",
  "CANCELLED",
] as const;
export type RequisitionLineStatus = (typeof LINE_STATUSES)[number];

export const LINE_STATUS_LABELS: Record<RequisitionLineStatus, string> = {
  REQUESTED: "Requested",
  APPROVED: "Approved",
  PARTIALLY_APPROVED: "Partially Approved",
  PARTIALLY_ORDERED: "Partially Ordered",
  ORDERED: "Ordered",
  REJECTED: "Rejected",
  CANCELLED: "Cancelled",
};

/**
 * Mirrors the backend `ApprovalAction` enum.
 *
 * `PENDING` and `SKIPPED` are states the policy resolver writes, not states a
 * user can choose, so only the two real decisions appear in the decision dialog.
 * There is no "return for correction": a requisition an approver does not accept
 * is rejected, and anything the requester still wants to buy is re-raised.
 */
export const APPROVAL_ACTIONS = ["APPROVED", "REJECTED"] as const;
export type ApprovalAction = (typeof APPROVAL_ACTIONS)[number];

export type ApprovalActionState =
  | ApprovalAction
  | "PENDING"
  | "SKIPPED";

/**
 * Statuses a document may be submitted from. Mirrors the transition table in
 * `PurchaseRequisitionStatus`.
 */
export const SUBMITTABLE_STATUSES: RequisitionStatus[] = ["DRAFT"];

/** Statuses an approver may record a decision against. */
export const DECIDABLE_STATUSES: RequisitionStatus[] = ["SUBMITTED"];

/** Statuses that may still be cancelled. */
export const CANCELLABLE_STATUSES: RequisitionStatus[] = ["DRAFT", "SUBMITTED"];

export function formatRequisitionStatus(
  status: string | null | undefined
): string {
  if (!status) return "—";
  return (
    REQUISITION_STATUS_LABELS[status as RequisitionStatus] ?? status
  );
}

export function formatRequisitionPriority(
  priority: string | null | undefined
): string {
  if (!priority) return "—";
  return (
    REQUISITION_PRIORITY_LABELS[priority as RequisitionPriority] ?? priority
  );
}

export function formatLineStatus(
  status: string | null | undefined
): string {
  if (!status) return "—";
  return LINE_STATUS_LABELS[status as RequisitionLineStatus] ?? status;
}

/** Mirrors the backend `RequisitionLineResponse` record. */
export interface RequisitionLine {
  id: number;
  lineNumber: number;
  itemId: number;
  itemCode: string;
  itemName: string;
  uomCode: string;
  requestedQuantity: number;
  approvedQuantity: number;
  orderedQuantity: number;
  shortfallQuantity: number;
  estimatedUnitPrice: number;
  taxRate: number;
  approvedNetAmount: number;
  approvedTaxAmount: number;
  approvedGrossAmount: number;
  status: RequisitionLineStatus;
  remarks: string | null;
}

/** Mirrors the nested `RequisitionDetailResponse.ApprovalStepResponse` record. */
export interface ApprovalStep {
  id: number;
  sequenceNo: number;
  approverId: number | null;
  approverRoleId: number | null;
  approverRole: string | null;
  amountThreshold: number | null;
  action: ApprovalActionState;
  actedBy: string | null;
  actedAt: string | null;
  comments: string | null;
}

/** Mirrors the nested `RequisitionDetailResponse.RequisitionHistoryResponse`. */
export interface RequisitionHistoryEntry {
  id: number;
  fromStatus: RequisitionStatus | null;
  toStatus: RequisitionStatus;
  changedBy: string | null;
  changedAt: string;
  reason: string | null;
}

/** Mirrors the backend `RequisitionListResponse` record. */
export interface RequisitionListResponse {
  id: number;
  prNumber: string;
  branchId: number;
  departmentId: number | null;
  requestedBy: number;
  requestDate: string;
  /** BS projection of `requestDate`, `yyyy-MM-dd`. Null if out of calendar range. */
  requestDateNp: string | null;
  requiredByDate: string | null;
  /** BS projection of `requiredByDate`. */
  requiredByDateNp: string | null;
  status: RequisitionStatus;
  priority: RequisitionPriority;
  currency: string;
  estimatedTotalAmount: number;
  approvedTotalAmount: number;
  orderedTotalAmount: number;
  outstandingValue: number;
  submittedAt: string | null;
  createdAt: string;
}

/** Mirrors the backend `RequisitionDetailResponse` record. */
export interface Requisition extends RequisitionListResponse {
  tenantId: number;
  requestDateNpYear: number | null;
  companyId: number;
  fiscalYearId: number | null;
  estimatedSubtotalAmount: number;
  estimatedTaxAmount: number;
  /**
   * The nominated destination warehouse, as an id plus the code and name frozen
   * when it was nominated. All three travel together: the id is a pointer into the
   * inventory service's master data, the code and name are what the document can
   * still print after that master is renamed.
   */
  warehouseId: number | null;
  warehouseCode: string | null;
  warehouseName: string | null;
  justification: string | null;
  notes: string | null;
  submittedBy: string | null;
  approvedAt: string | null;
  approvedBy: string | null;
  rejectedAt: string | null;
  rejectedBy: string | null;
  rejectionReason: string | null;
  convertedAt: string | null;
  convertedBy: string | null;
  cancelledAt: string | null;
  cancelledBy: string | null;
  cancellationReason: string | null;
  version: number;
  lines: RequisitionLine[];
  approvals: ApprovalStep[];
  statusHistory: RequisitionHistoryEntry[];
}

/** Mirrors the backend `CreateRequisitionLineRequest` record. */
export interface CreateRequisitionLineRequest {
  itemId: number;
  itemCode: string;
  itemName: string;
  uomCode: string;
  quantity: number;
  estimatedUnitPrice: number;
  taxRate: number;
  remarks?: string;
}

/** Mirrors the backend `CreateRequisitionRequest` record. */
export interface CreateRequisitionRequest {
  branchId: number;
  departmentId?: number;
  /** Canonical. The backend derives `requestDateNp` from this. */
  requestDate: string;
  /**
   * Optional advisory cross-check. The server recomputes the stored value from
   * `requestDate` regardless, and rejects a mismatch rather than silently
   * overwriting, so a picker bug surfaces instead of persisting a wrong date.
   */
  requestDateNp?: string;
  requiredByDate?: string;
  priority?: RequisitionPriority;
  currency?: string;
  /**
   * Optional nomination. A requester frequently cannot say which site will serve
   * the request, and forcing a guess adds friction to the most frequent entry path
   * in the system for no gain.
   *
   * Send all three or none. A partial nomination is rejected, because half a
   * snapshot reads as authoritative on the document while resolving to nothing.
   */
  warehouseId?: number;
  warehouseCode?: string;
  warehouseName?: string;
  justification?: string;
  notes?: string;
  lines: CreateRequisitionLineRequest[];
}

/** Mirrors the backend `ApprovalDecisionRequest` record. */
export interface ApprovalDecisionRequest {
  action: ApprovalAction;
  /**
   * Per-line grant quantities. Omitted lines are granted in full.
   *
   * Sending the whole set is optional: an approver who is happy with every line
   * only has to pick an action, which keeps the common case a single click.
   */
  lineApprovals?: LineApproval[];
  /**
   * Lines the approver refuses outright on an `APPROVED` decision. They stay on
   * the requisition as evidence of what was asked for, but carry no demand
   * forward, so they can never be awarded to a purchase order.
   *
   * Only honoured when the decision is `APPROVED`; the server rejects the field
   * on `REJECTED` rather than quietly discarding it.
   */
  rejectedLineIds?: number[];
  /**
   * Lines withdrawn from this requisition by the approver on an `APPROVED`
   * decision. Distinct from a rejection: the need is withdrawn, not denied.
   */
  cancelledLineIds?: number[];
  comments?: string;
}

/**
 * Mirrors the backend `UpdateRequisitionRequest` record.
 *
 * A full replacement rather than a patch: the client edits a whole grid, so one
 * replacement has exactly one meaning. `branchId` is deliberately absent — the
 * branch selects the approval policy, and re-pointing a document at another
 * branch would silently move it under a different control.
 */
export interface UpdateRequisitionRequest {
  /** Canonical. The backend derives `requestDateNp` from this. */
  requestDate: string;
  /** Advisory cross-check; the server recomputes and rejects a mismatch. */
  requestDateNp?: string;
  requiredByDate?: string;
  priority: RequisitionPriority;
  currency: string;
  /**
   * Same "all three or none" rule as on create. Omitting all three leaves the
   * existing nomination untouched rather than clearing it - the same protection
   * currency has, and the reason a saved line edit cannot silently un-nominate the
   * site the goods are meant to go to.
   */
  warehouseId?: number;
  warehouseCode?: string;
  warehouseName?: string;
  justification?: string;
  notes?: string;
  lines: CreateRequisitionLineRequest[];
}

/**
 * Mirrors the backend `RequisitionActionabilityResponse` record.
 *
 * Advisory: every action is re-authorised server-side when the endpoint that
 * performs it is called. This exists so the UI stops offering buttons that
 * would answer 403.
 */
export interface RequisitionActionability {
  requisitionId: number;
  /** DRAFT, and the caller owns it (or holds the admin permission). */
  canEdit: boolean;
  /** DRAFT, the caller owns it, and it still has at least one line. */
  canSubmit: boolean;
  /** Caller is the assignee of the current step and did not raise the document. */
  canDecide: boolean;
  /** Not yet closed out, and the caller owns it or holds the admin permission. */
  canCancel: boolean;
  /** DRAFT only, and the caller owns it or holds the admin permission. */
  canDelete: boolean;
  /** APPROVED with at least one line still carrying unfulfilled approved demand. */
  canAwardToOrder: boolean;
  /** 1-based step the document is waiting on, or null when no step is outstanding. */
  awaitingStepSequenceNo: number | null;
  /** Human-readable reasons the flags above are false. Empty when all are open. */
  blockers: string[];
}

export interface LineApproval {
  lineId: number;
  approvedQuantity: number;
}

/** Mirrors the backend `OrderAwardRequest` record. */
export interface OrderAwardRequest {
  purchaseOrderId: number;
  awards: LineAwardEntry[];
}

export interface LineAwardEntry {
  requisitionLineId: number;
  orderedQuantity: number;
  purchaseOrderLineId?: number;
}

export interface CancelRequisitionRequest {
  reason: string;
}

/**
 * Mirrors the backend `WorkflowConfigurationResponse` record.
 *
 * One shape for read and write, because the settings screen renders the server's
 * answer and posts back the same object. A separate request type would allow the
 * two to drift, and the drift would only show up as a setting that saves but does
 * not change anything.
 */
export interface WorkflowConfiguration {
  /**
   * Whether a submitted requisition waits for a person to approve it.
   *
   * `true`  - DRAFT -> SUBMITTED -> APPROVED, one or more approvers decide.
   * `false` - DRAFT -> SUBMITTED -> APPROVED, decided on submission by the system
   *           because this company does not require approval.
   *
   * Either way the document passes through SUBMITTED, and either way the approval
   * is recorded: with the flag off, the approval step names no approver and is
   * attributed to the system rather than to the submitter.
   *
   * The server treats an unconfigured company as `true`. The UI should render
   * whatever the server returned rather than assuming a value locally, since the
   * default is a server-side decision and not a client one.
   */
  purchaseRequisitionApprovalRequired: boolean;
}

export interface GetRequisitionsParams {
  page?: number;
  size?: number;
  sort?: string;
  branchId?: number;
  status?: RequisitionStatus;
}

const BASE_URL = "/procurement/purchase-requisitions";

/**
 * Company workflow switches. A separate base path from the requisitions because
 * these are settings for the company, not operations on a document - the only
 * thing the two share is the module they live in.
 */
const WORKFLOW_CONFIG_URL = "/procurement/workflow-configuration";

/**
 * Purchase requisition API endpoints with RTK Query caching and invalidation.
 *
 * No endpoint takes a `companyId`: the backend scopes every call to the company
 * in the `X-Company-Id` header, attached by the Axios request interceptor from
 * the current company selection.
 */
export const purchaseRequisitionApi = api.injectEndpoints({
  endpoints: (build) => ({
    getRequisitions: build.query<
      PageResponse<RequisitionListResponse>,
      GetRequisitionsParams | void
    >({
      query: (params) => ({
        url: BASE_URL,
        method: "GET",
        params: params ?? {},
      }),
      providesTags: ["PurchaseRequisition"],
    }),

    /**
     * Requisitions the caller raised.
     *
     * Separate from the full list because it is answered from the `requested_by`
     * index and so stays cheap as the company's requisition history grows.
     */
    getMyRequisitions: build.query<
      PageResponse<RequisitionListResponse>,
      GetRequisitionsParams | void
    >({
      query: (params) => ({
        url: `${BASE_URL}/mine`,
        method: "GET",
        params: params ?? {},
      }),
      providesTags: ["PurchaseRequisition"],
    }),

    /**
     * Requisitions whose *current* approval step is the caller's.
     *
     * Submitting a document materialises its whole approval chain at once, so a
     * naive "pending where assignee = me" query would show an approver a document
     * that is still waiting on an earlier step. The server filters to the current
     * step; this is the queue an approver actually works.
     */
    getApprovalInbox: build.query<
      PageResponse<RequisitionListResponse>,
      GetRequisitionsParams | void
    >({
      query: (params) => ({
        url: `${BASE_URL}/approval-inbox`,
        method: "GET",
        params: params ?? {},
      }),
      providesTags: ["PurchaseRequisition"],
    }),

    getRequisition: build.query<Requisition, number>({
      query: (requisitionId) => ({
        url: `${BASE_URL}/${requisitionId}`,
        method: "GET",
      }),
      providesTags: (_result, _error, requisitionId) => [
        { type: "PurchaseRequisition", id: requisitionId },
      ],
    }),

    /** What the caller may do with this requisition right now. */
    getRequisitionActions: build.query<RequisitionActionability, number>({
      query: (requisitionId) => ({
        url: `${BASE_URL}/${requisitionId}/actions`,
        method: "GET",
      }),
      providesTags: (_result, _error, requisitionId) => [
        { type: "PurchaseRequisition", id: requisitionId },
        "PurchaseRequisition",
      ],
    }),

    createRequisition: build.mutation<
      Requisition,
      CreateRequisitionRequest
    >({
      query: (body) => ({
        url: BASE_URL,
        method: "POST",
        data: body,
      }),
      invalidatesTags: ["PurchaseRequisition"],
    }),

    /**
     * Replaces a draft or returned requisition.
     *
     * Line rows are reused in place where they survive the edit, so the server
     * deletes the orphans before inserting the replacements; sending an id back
     * is therefore not a thing to do.
     */
    updateRequisition: build.mutation<
      Requisition,
      { requisitionId: number; data: UpdateRequisitionRequest }
    >({
      query: ({ requisitionId, data }) => ({
        url: `${BASE_URL}/${requisitionId}`,
        method: "PUT",
        data,
      }),
      invalidatesTags: (_result, _error, { requisitionId }) => [
        { type: "PurchaseRequisition", id: requisitionId },
        "PurchaseRequisition",
      ],
    }),

    submitRequisition: build.mutation<Requisition, number>({
      query: (requisitionId) => ({
        url: `${BASE_URL}/${requisitionId}/submit`,
        method: "POST",
      }),
      invalidatesTags: (_result, _error, requisitionId) => [
        { type: "PurchaseRequisition", id: requisitionId },
        "PurchaseRequisition",
      ],
    }),

    decideRequisition: build.mutation<
      Requisition,
      { requisitionId: number; data: ApprovalDecisionRequest }
    >({
      query: ({ requisitionId, data }) => ({
        url: `${BASE_URL}/${requisitionId}/decisions`,
        method: "POST",
        data,
      }),
      invalidatesTags: (_result, _error, { requisitionId }) => [
        { type: "PurchaseRequisition", id: requisitionId },
        "PurchaseRequisition",
      ],
    }),

    awardRequisitionToOrder: build.mutation<
      Requisition,
      { requisitionId: number; data: OrderAwardRequest }
    >({
      query: ({ requisitionId, data }) => ({
        url: `${BASE_URL}/${requisitionId}/order-awards`,
        method: "POST",
        data,
      }),
      invalidatesTags: (_result, _error, { requisitionId }) => [
        { type: "PurchaseRequisition", id: requisitionId },
        "PurchaseRequisition",
      ],
    }),

    cancelRequisition: build.mutation<
      Requisition,
      { requisitionId: number; data: CancelRequisitionRequest }
    >({
      query: ({ requisitionId, data }) => ({
        url: `${BASE_URL}/${requisitionId}/cancel`,
        method: "POST",
        data,
      }),
      invalidatesTags: (_result, _error, { requisitionId }) => [
        { type: "PurchaseRequisition", id: requisitionId },
        "PurchaseRequisition",
      ],
    }),

    deleteRequisition: build.mutation<void, number>({
      query: (requisitionId) => ({
        url: `${BASE_URL}/${requisitionId}`,
        method: "DELETE",
      }),
      invalidatesTags: ["PurchaseRequisition"],
    }),

    /**
     * The company's workflow switches.
     *
     * Readable by anyone who can view requisitions, because the requisition
     * screens need it to know whether an approval queue exists. Writing it needs
     * `PR_ADMIN`; the 403 comes from the server, not from hiding a control here.
     */
    getWorkflowConfiguration: build.query<WorkflowConfiguration, void>({
      query: () => ({
        url: WORKFLOW_CONFIG_URL,
        method: "GET",
      }),
      providesTags: ["WorkflowConfiguration"],
    }),

    /**
     * Replaces the company's workflow switches.
     *
     * PUT, matching the server: the client edits a whole form, so one replacement
     * has one meaning. A partial update would have to define what an absent field
     * means for a setting that silently changes how spend is authorised.
     *
     * Invalidates requisitions as well as the configuration itself: flipping the
     * flag changes what happens to the next submission, so a screen holding a
     * stale copy of "your requisition is awaiting approval" would be describing
     * the previous setting.
     */
    updateWorkflowConfiguration: build.mutation<
      WorkflowConfiguration,
      WorkflowConfiguration
    >({
      query: (body) => ({
        url: WORKFLOW_CONFIG_URL,
        method: "PUT",
        data: body,
      }),
      invalidatesTags: ["WorkflowConfiguration", "PurchaseRequisition"],
    }),
  }),
});

export const {
  useGetRequisitionsQuery,
  useGetMyRequisitionsQuery,
  useGetApprovalInboxQuery,
  useGetRequisitionQuery,
  useGetRequisitionActionsQuery,
  useCreateRequisitionMutation,
  useUpdateRequisitionMutation,
  useSubmitRequisitionMutation,
  useDecideRequisitionMutation,
  useAwardRequisitionToOrderMutation,
  useCancelRequisitionMutation,
  useDeleteRequisitionMutation,
  useGetWorkflowConfigurationQuery,
  useUpdateWorkflowConfigurationMutation,
} = purchaseRequisitionApi;
