import { api } from "./api";
import type { PageResponse } from "@/types/api";

/** Mirrors the backend `SalesOrderStatus` enum. */
export const SALES_ORDER_STATUSES = [
  "DRAFT",
  "CONFIRMED",
  "PARTIALLY_DELIVERED",
  "DELIVERED",
  "CANCELLED",
  "CLOSED",
] as const;
export type SalesOrderStatus = (typeof SALES_ORDER_STATUSES)[number];

export const SALES_ORDER_STATUS_LABELS: Record<SalesOrderStatus, string> = {
  DRAFT: "Draft",
  CONFIRMED: "Confirmed",
  PARTIALLY_DELIVERED: "Partially Delivered",
  DELIVERED: "Delivered",
  CANCELLED: "Cancelled",
  CLOSED: "Closed",
};

/** Mirrors the backend `SalesOrderLineStatus` enum. */
export const SALES_ORDER_LINE_STATUSES = [
  "OPEN",
  "PARTIALLY_DELIVERED",
  "DELIVERED",
  "CANCELLED",
] as const;
export type SalesOrderLineStatus = (typeof SALES_ORDER_LINE_STATUSES)[number];

/** Mirrors the backend `SalesOrderLineResponse` record. */
export interface SalesOrderLine {
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
  deliveredQuantity: number;
  outstandingQuantity: number;
  status: SalesOrderLineStatus;
  description: string | null;
}

/** Mirrors the backend `SalesOrderResponse` record. */
export interface SalesOrder {
  id: number;
  tenantId: number;
  companyId: number;
  soNumber: string;
  fiscalYearId: number | null;
  branchId: number | null;
  orderDate: string;
  expectedDeliveryDate: string | null;
  customerId: number;
  customerCode: string;
  customerName: string;
  customerPanNumber: string | null;
  status: SalesOrderStatus;
  currency: string;
  subtotalAmount: number;
  discountAmount: number;
  taxAmount: number;
  totalAmount: number;
  deliveredAmount: number;
  paymentTermsDays: number | null;
  warehouseId: number | null;
  warehouseCode: string | null;
  warehouseName: string | null;
  reference: string | null;
  notes: string | null;
  lines: SalesOrderLine[];
  version: number;
}

/** Mirrors the backend `SalesOrderLineRequest` record. */
export interface SalesOrderLineRequest {
  itemId: number;
  uomCode: string;
  quantity: number;
  unitPrice: number;
  discountPercentage?: number;
  taxRate?: number;
  description?: string;
}

/** Mirrors the backend `CreateSalesOrderRequest` record. */
export interface CreateSalesOrderRequest {
  customerId: number;
  orderDate: string;
  expectedDeliveryDate?: string;
  branchId?: number;
  warehouseId?: number;
  paymentTermsDays?: number;
  reference?: string;
  notes?: string;
  lines: SalesOrderLineRequest[];
}

/** Mirrors the backend `UpdateSalesOrderRequest` record. */
export interface UpdateSalesOrderRequest {
  customerId: number;
  orderDate: string;
  expectedDeliveryDate?: string;
  branchId?: number;
  warehouseId?: number;
  paymentTermsDays?: number;
  reference?: string;
  notes?: string;
  lines: SalesOrderLineRequest[];
}

/** Mirrors the backend `SalesOrderDecisionRequest` record. */
export interface SalesOrderDecisionRequest {
  reason: string;
}

export interface GetSalesOrdersParams {
  page?: number;
  size?: number;
  sort?: string;
}

const BASE_URL = "/sales/sales-orders";

/** Statuses that may still be confirmed. */
export const CONFIRMABLE_ORDER_STATUSES: SalesOrderStatus[] = ["DRAFT"];
/** Statuses that may still be cancelled. */
export const CANCELLABLE_ORDER_STATUSES: SalesOrderStatus[] = ["DRAFT", "CONFIRMED"];

/**
 * Sales order API with RTK Query caching and invalidation. All reads are
 * company-scoped server-side.
 */
export const salesOrderApi = api.injectEndpoints({
  endpoints: (build) => ({
    getSalesOrders: build.query<PageResponse<SalesOrder>, GetSalesOrdersParams | void>({
      query: (params) => ({ url: BASE_URL, method: "GET", params: params ?? {} }),
      providesTags: (result) => [
        ...(result?.content.map((o) => ({ type: "SalesOrder" as const, id: o.id })) ?? []),
        "SalesOrder",
      ],
    }),

    getOpenSalesOrders: build.query<SalesOrder[], void>({
      query: () => ({ url: `${BASE_URL}/open`, method: "GET" }),
      providesTags: ["SalesOrder"],
    }),

    getSalesOrder: build.query<SalesOrder, number>({
      query: (id) => ({ url: `${BASE_URL}/${id}`, method: "GET" }),
      providesTags: (_r, _e, id) => [{ type: "SalesOrder", id }, "SalesOrder"],
    }),

    createSalesOrder: build.mutation<SalesOrder, CreateSalesOrderRequest>({
      query: (body) => ({ url: BASE_URL, method: "POST", data: body }),
      invalidatesTags: ["SalesOrder"],
    }),

    updateSalesOrder: build.mutation<
      SalesOrder,
      { id: number; data: UpdateSalesOrderRequest }
    >({
      query: ({ id, data }) => ({ url: `${BASE_URL}/${id}`, method: "PUT", data }),
      invalidatesTags: (_r, _e, { id }) => [{ type: "SalesOrder", id }, "SalesOrder"],
    }),

    confirmSalesOrder: build.mutation<SalesOrder, number>({
      query: (id) => ({ url: `${BASE_URL}/${id}/confirm`, method: "POST" }),
      invalidatesTags: (_r, _e, id) => [{ type: "SalesOrder", id }, "SalesOrder"],
    }),

    cancelSalesOrder: build.mutation<
      SalesOrder,
      { id: number; data: SalesOrderDecisionRequest }
    >({
      query: ({ id, data }) => ({ url: `${BASE_URL}/${id}/cancel`, method: "POST", data }),
      invalidatesTags: (_r, _e, { id }) => [{ type: "SalesOrder", id }, "SalesOrder"],
    }),

    closeSalesOrder: build.mutation<SalesOrder, number>({
      query: (id) => ({ url: `${BASE_URL}/${id}/close`, method: "POST" }),
      invalidatesTags: (_r, _e, id) => [{ type: "SalesOrder", id }, "SalesOrder"],
    }),
  }),
});

export const {
  useGetSalesOrdersQuery,
  useGetOpenSalesOrdersQuery,
  useGetSalesOrderQuery,
  useLazyGetSalesOrderQuery,
  useCreateSalesOrderMutation,
  useUpdateSalesOrderMutation,
  useConfirmSalesOrderMutation,
  useCancelSalesOrderMutation,
  useCloseSalesOrderMutation,
} = salesOrderApi;

