import { api } from "./api";
import type { PageResponse } from "@/types/api";

/**
 * Mirrors the backend `PurchaseOrderStatus` enum.
 *
 * The order lifecycle is `DRAFT -> SUBMITTED -> APPROVED -> PARTIALLY_RECEIVED ->
 * RECEIVED -> CLOSED`, with `REJECTED` and `CANCELLED` as the off-ramps. The union is
 * the contract rather than a convenience: Jackson rejects a value outside it, so a
 * status the client invents would fail loudly rather than render as an unknown chip.
 */
export const PURCHASE_ORDER_STATUSES = [
  "DRAFT",
  "SUBMITTED",
  "APPROVED",
  "PARTIALLY_RECEIVED",
  "RECEIVED",
  "REJECTED",
  "CANCELLED",
  "CLOSED",
] as const;
export type PurchaseOrderStatus = (typeof PURCHASE_ORDER_STATUSES)[number];

export const PURCHASE_ORDER_STATUS_LABELS: Record<PurchaseOrderStatus, string> = {
  DRAFT: "Draft",
  SUBMITTED: "Submitted",
  APPROVED: "Approved",
  PARTIALLY_RECEIVED: "Partially Received",
  RECEIVED: "Received",
  REJECTED: "Rejected",
  CANCELLED: "Cancelled",
  CLOSED: "Closed",
};

/**
 * Mirrors the backend `PurchaseOrderLineStatus` enum. Derived server-side from the
 * received quantity, so the client never has to guess it.
 */
export const PURCHASE_ORDER_LINE_STATUSES = [
  "OPEN",
  "PARTIALLY_RECEIVED",
  "RECEIVED",
  "CANCELLED",
] as const;
export type PurchaseOrderLineStatus = (typeof PURCHASE_ORDER_LINE_STATUSES)[number];

export const PURCHASE_ORDER_LINE_STATUS_LABELS: Record<PurchaseOrderLineStatus, string> = {
  OPEN: "Open",
  PARTIALLY_RECEIVED: "Partially Received",
  RECEIVED: "Received",
  CANCELLED: "Cancelled",
};

export function formatPurchaseOrderStatus(
  status: string | null | undefined
): string {
  if (!status) return "—";
  return PURCHASE_ORDER_STATUS_LABELS[status as PurchaseOrderStatus] ?? status;
}

export function formatPurchaseOrderLineStatus(
  status: string | null | undefined
): string {
  if (!status) return "—";
  return (
    PURCHASE_ORDER_LINE_STATUS_LABELS[status as PurchaseOrderLineStatus] ?? status
  );
}

/** Statuses that still permit a submission for approval. */
export const SUBMITTABLE_ORDER_STATUSES: PurchaseOrderStatus[] = ["DRAFT"];

/** Statuses an approver may approve or refuse. */
export const DECIDABLE_ORDER_STATUSES: PurchaseOrderStatus[] = ["SUBMITTED"];

/** Statuses that may still be voided. */
export const CANCELLABLE_ORDER_STATUSES: PurchaseOrderStatus[] = [
  "DRAFT",
  "SUBMITTED",
  "APPROVED",
  "PARTIALLY_RECEIVED",
  "RECEIVED",
];

/** Statuses that may be closed once received. */
export const CLOSABLE_ORDER_STATUSES: PurchaseOrderStatus[] = [
  "APPROVED",
  "PARTIALLY_RECEIVED",
  "RECEIVED",
];

/** Mirrors the nested `PurchaseOrderResponse.Line` record. */
export interface PurchaseOrderLine {
  id: number;
  lineNumber: number;
  itemId: number;
  itemCode: string;
  itemName: string;
  uomCode: string;
  quantity: number;
  unitPrice: number;
  discountPercentage: number;
  taxRate: number;
  lineAmount: number;
  receivedQuantity: number;
  status: PurchaseOrderLineStatus;
  expectedDeliveryDate: string | null;
  notes: string | null;
  /** The requisition line this came from, or null for a line raised directly. */
  sourcePurchaseRequisitionItemId: number | null;
}

/** Mirrors the backend `PurchaseOrderResponse` record. */
export interface PurchaseOrder {
  id: number;
  poNumber: string;
  tenantId: number;
  companyId: number;
  branchId: number;
  fiscalYearId: number;
  /** The requisition this order was converted from, or null when raised directly. */
  sourcePurchaseRequisitionId: number | null;
  status: PurchaseOrderStatus;
  orderDate: string;
  expectedDeliveryDate: string | null;
  supplierId: number;
  supplierCode: string;
  supplierName: string;
  currency: string;
  /**
   * Where the buyer intends the goods to be stored, carried across unchanged from
   * the requisition. Nomination only - the receipt is what states where they
   * actually landed, and it may well differ.
   */
  warehouseId: number | null;
  warehouseCode: string | null;
  warehouseName: string | null;
  reference: string | null;
  notes: string | null;
  subtotalAmount: number;
  discountAmount: number;
  taxAmount: number;
  shippingAmount: number;
  totalAmount: number;
  /** Value of goods received so far. A money figure, not a quantity. */
  receivedAmount: number;
  /** Total minus received: the value still owed against this order. */
  outstandingAmount: number;
  createdAt: string;
  lines: PurchaseOrderLine[];
}

/**
 * Mirrors the backend `ConvertToPurchaseOrderRequest` record.
 *
 * Only the supplier is asked for. Item, UOM, quantity, price, tax rate, currency,
 * branch and delivery location are all copied from the requisition server-side, so a
 * client that let the buyer retype them would let the order disagree with the approval
 * that authorised it. The code/name pair is a frozen snapshot taken at conversion, not
 * a lookup that could drift if the catalogue changes later.
 */
export interface ConvertToPurchaseOrderRequest {
  requisitionId: number;
  supplierId: number;
  supplierCode: string;
  supplierName: string;
  expectedDeliveryDate?: string;
  notes?: string;
}

/** Mirrors the backend `PurchaseOrderDecisionRequest` record. */
export interface PurchaseOrderDecisionRequest {
  reason: string;
}

const BASE_URL = "/procurement/purchase-orders";

/**
 * Paging and sorting for the order book. Mirrors the backend `Pageable` binder, which
 * only honours `page`, `size` and `sort`; any other parameter is silently dropped, so
 * filtering is either done server-side or not at all.
 */
export interface GetPurchaseOrdersParams {
  page?: number;
  size?: number;
  sort?: string;
}

/**
 * Purchase order API endpoints with RTK Query caching and invalidation.
 *
 * The backend serves three reads: the whole order book, the orders still owing goods,
 * and one order by id. All three are company-scoped server-side.
 *
 * Every mutation invalidates `PurchaseRequisition` as well as `PurchaseOrder`:
 * conversion moves the source requisition to CONVERTED_TO_PO and receiving changes the
 * demand recorded against it, so a stale requisition copy would keep offering actions
 * the server now refuses.
 */
export const purchaseOrderApi = api.injectEndpoints({
  endpoints: (build) => ({
    /**
     * The order book.
     *
     * Provides the collection tag *and* an id-specific tag per row, so a mutation can
     * invalidate precisely the order it changed; the open details sheet for that order
     * refetches alongside the list instead of showing a stale copy until reload.
     */
    getPurchaseOrders: build.query<
      PageResponse<PurchaseOrder>,
      GetPurchaseOrdersParams | void
    >({
      query: (params) => ({
        url: BASE_URL,
        method: "GET",
        params: params ?? {},
      }),
      providesTags: (result) => [
        ...(result?.content.map((order) => ({ type: "PurchaseOrder" as const, id: order.id })) ?? []),
        "PurchaseOrder",
      ],
    }),

    /** The orders still owing goods - the receiving team's worklist. */
    getOutstandingPurchaseOrders: build.query<
      PageResponse<PurchaseOrder>,
      GetPurchaseOrdersParams | void
    >({
      query: (params) => ({
        url: `${BASE_URL}/outstanding`,
        method: "GET",
        params: params ?? {},
      }),
      providesTags: ["PurchaseOrder"],
    }),

    getPurchaseOrder: build.query<PurchaseOrder, number>({
      query: (purchaseOrderId) => ({
        url: `${BASE_URL}/${purchaseOrderId}`,
        method: "GET",
      }),
      providesTags: (_result, _error, purchaseOrderId) => [
        { type: "PurchaseOrder", id: purchaseOrderId },
        "PurchaseOrder",
      ],
    }),

    /** Converts an approved requisition into a draft order. */
    convertToPurchaseOrder: build.mutation<
      PurchaseOrder,
      ConvertToPurchaseOrderRequest
    >({
      query: (body) => ({
        url: BASE_URL,
        method: "POST",
        data: body,
      }),
      invalidatesTags: ["PurchaseOrder", "PurchaseRequisition"],
    }),

    submitPurchaseOrder: build.mutation<PurchaseOrder, number>({
      query: (purchaseOrderId) => ({
        url: `${BASE_URL}/${purchaseOrderId}/submit`,
        method: "POST",
      }),
      invalidatesTags: (_result, _error, purchaseOrderId) => [
        { type: "PurchaseOrder", id: purchaseOrderId },
        "PurchaseOrder",
        "PurchaseRequisition",
      ],
    }),

    approvePurchaseOrder: build.mutation<PurchaseOrder, number>({
      query: (purchaseOrderId) => ({
        url: `${BASE_URL}/${purchaseOrderId}/approve`,
        method: "POST",
      }),
      invalidatesTags: (_result, _error, purchaseOrderId) => [
        { type: "PurchaseOrder", id: purchaseOrderId },
        "PurchaseOrder",
      ],
    }),

    /** Refuses a submitted order. A reason is mandatory; the server rejects an empty one. */
    rejectPurchaseOrder: build.mutation<
      PurchaseOrder,
      { purchaseOrderId: number; data: PurchaseOrderDecisionRequest }
    >({
      query: ({ purchaseOrderId, data }) => ({
        url: `${BASE_URL}/${purchaseOrderId}/reject`,
        method: "POST",
        data,
      }),
      invalidatesTags: (_result, _error, { purchaseOrderId }) => [
        { type: "PurchaseOrder", id: purchaseOrderId },
        "PurchaseOrder",
      ],
    }),

    cancelPurchaseOrder: build.mutation<
      PurchaseOrder,
      { purchaseOrderId: number; data: PurchaseOrderDecisionRequest }
    >({
      query: ({ purchaseOrderId, data }) => ({
        url: `${BASE_URL}/${purchaseOrderId}/cancel`,
        method: "POST",
        data,
      }),
      invalidatesTags: (_result, _error, { purchaseOrderId }) => [
        { type: "PurchaseOrder", id: purchaseOrderId },
        "PurchaseOrder",
        "GoodsReceipt",
      ],
    }),

    closePurchaseOrder: build.mutation<PurchaseOrder, number>({
      query: (purchaseOrderId) => ({
        url: `${BASE_URL}/${purchaseOrderId}/close`,
        method: "POST",
      }),
      invalidatesTags: (_result, _error, purchaseOrderId) => [
        { type: "PurchaseOrder", id: purchaseOrderId },
        "PurchaseOrder",
      ],
    }),
  }),
});

export const {
  useGetPurchaseOrdersQuery,
  useGetOutstandingPurchaseOrdersQuery,
  useGetPurchaseOrderQuery,
  useConvertToPurchaseOrderMutation,
  useSubmitPurchaseOrderMutation,
  useApprovePurchaseOrderMutation,
  useRejectPurchaseOrderMutation,
  useCancelPurchaseOrderMutation,
  useClosePurchaseOrderMutation,
} = purchaseOrderApi;
