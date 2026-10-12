import { api } from "./api";
import type { PageResponse } from "@/types/api";

/** Mirrors the backend `DeliveryStatus` enum. */
export const DELIVERY_STATUSES = ["DRAFT", "POSTED", "CANCELLED"] as const;
export type DeliveryStatus = (typeof DELIVERY_STATUSES)[number];

export const DELIVERY_STATUS_LABELS: Record<DeliveryStatus, string> = {
  DRAFT: "Draft",
  POSTED: "Posted",
  CANCELLED: "Cancelled",
};

export interface SalesDeliveryLine {
  id: number;
  lineNumber: number;
  salesOrderLineNumber: number;
  itemId: number;
  itemCode: string;
  itemName: string;
  uomCode: string;
  quantity: number;
  description: string | null;
}

export interface SalesDelivery {
  id: number;
  tenantId: number;
  companyId: number;
  deliveryNumber: string;
  fiscalYearId: number | null;
  branchId: number | null;
  salesOrderId: number;
  soNumber: string;
  customerId: number;
  customerCode: string;
  customerName: string;
  warehouseId: number;
  warehouseCode: string | null;
  warehouseName: string | null;
  deliveryDate: string;
  status: DeliveryStatus;
  notes: string | null;
  lines: SalesDeliveryLine[];
  version: number;
}

export interface DeliveryLineRequest {
  salesOrderLineNumber: number;
  itemId: number;
  uomCode: string;
  quantity: number;
  description?: string;
}

export interface CreateDeliveryRequest {
  salesOrderId: number;
  warehouseId: number;
  deliveryDate: string;
  notes?: string;
  lines: DeliveryLineRequest[];
}

export interface DeliveryDecisionRequest {
  reason: string;
}

export interface GetDeliveriesParams {
  page?: number;
  size?: number;
  sort?: string;
}

const BASE_URL = "/sales/deliveries";

export const salesDeliveryApi = api.injectEndpoints({
  endpoints: (build) => ({
    getDeliveries: build.query<PageResponse<SalesDelivery>, GetDeliveriesParams | void>({
      query: (params) => ({ url: BASE_URL, method: "GET", params: params ?? {} }),
      providesTags: (result) => [
        ...(result?.content.map((d) => ({ type: "SalesDelivery" as const, id: d.id })) ?? []),
        "SalesDelivery",
      ],
    }),

    getDelivery: build.query<SalesDelivery, number>({
      query: (id) => ({ url: `${BASE_URL}/${id}`, method: "GET" }),
      providesTags: (_r, _e, id) => [{ type: "SalesDelivery", id }, "SalesDelivery"],
    }),

    createDelivery: build.mutation<SalesDelivery, CreateDeliveryRequest>({
      query: (body) => ({ url: BASE_URL, method: "POST", data: body }),
      invalidatesTags: ["SalesDelivery", "SalesOrder"],
    }),

    postDelivery: build.mutation<SalesDelivery, number>({
      query: (id) => ({ url: `${BASE_URL}/${id}/post`, method: "POST" }),
      invalidatesTags: (_r, _e, id) => [
        { type: "SalesDelivery", id },
        "SalesDelivery",
        "SalesOrder",
      ],
    }),

    cancelDelivery: build.mutation<
      SalesDelivery,
      { id: number; data: DeliveryDecisionRequest }
    >({
      query: ({ id, data }) => ({ url: `${BASE_URL}/${id}/cancel`, method: "POST", data }),
      invalidatesTags: (_r, _e, { id }) => [
        { type: "SalesDelivery", id },
        "SalesDelivery",
        "SalesOrder",
      ],
    }),
  }),
});

export const {
  useGetDeliveriesQuery,
  useGetDeliveryQuery,
  useCreateDeliveryMutation,
  usePostDeliveryMutation,
  useCancelDeliveryMutation,
} = salesDeliveryApi;
