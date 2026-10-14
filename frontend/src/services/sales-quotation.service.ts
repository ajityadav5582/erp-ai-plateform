import { api } from "./api";
import type { PageResponse } from "@/types/api";
import type { SalesOrder } from "./sales-order.service";

export const SALES_QUOTATION_STATUSES = [
  "DRAFT",
  "SENT",
  "ACCEPTED",
  "REJECTED",
  "EXPIRED",
  "CONVERTED",
] as const;
export type SalesQuotationStatus = (typeof SALES_QUOTATION_STATUSES)[number];

export const SALES_QUOTATION_STATUS_LABELS: Record<SalesQuotationStatus, string> = {
  DRAFT: "Draft",
  SENT: "Sent",
  ACCEPTED: "Accepted",
  REJECTED: "Rejected",
  EXPIRED: "Expired",
  CONVERTED: "Converted to Order",
};

export interface SalesQuotationLine {
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
  description: string | null;
}

export interface SalesQuotation {
  id: number;
  tenantId: number;
  companyId: number;
  quotationNumber: string;
  quotationDate: string;
  validUntilDate: string | null;
  customerId: number;
  customerCode: string;
  customerName: string;
  customerPanNumber: string | null;
  currency: string;
  fiscalYearId: number | null;
  branchId: number | null;
  warehouseId: number | null;
  status: SalesQuotationStatus;
  subtotalAmount: number;
  discountAmount: number;
  taxAmount: number;
  totalAmount: number;
  salesOrderId: number | null;
  convertedAt: string | null;
  reference: string | null;
  termsAndConditions: string | null;
  notes: string | null;
  lines: SalesQuotationLine[];
}

export interface SalesQuotationLineRequest {
  itemId: number;
  uomCode: string;
  quantity: number;
  unitPrice: number;
  discountPercentage?: number;
  taxRate?: number;
  description?: string;
}

export interface CreateSalesQuotationRequest {
  customerId: number;
  quotationDate: string;
  validUntilDate?: string;
  branchId?: number;
  warehouseId?: number;
  reference?: string;
  termsAndConditions?: string;
  notes?: string;
  lines: SalesQuotationLineRequest[];
}

export interface GetQuotationsParams {
  status?: SalesQuotationStatus;
  page?: number;
  size?: number;
  sort?: string;
}

const BASE_URL = "/sales/quotations";

export const salesQuotationApi = api.injectEndpoints({
  endpoints: (build) => ({
    getQuotations: build.query<PageResponse<SalesQuotation>, GetQuotationsParams | void>({
      query: (params) => ({ url: BASE_URL, method: "GET", params: params ?? {} }),
      providesTags: (result) => [
        ...(result?.content.map((q) => ({ type: "SalesQuotation" as const, id: q.id })) ?? []),
        "SalesQuotation",
      ],
    }),

    getQuotation: build.query<SalesQuotation, number>({
      query: (id) => ({ url: `${BASE_URL}/${id}`, method: "GET" }),
      providesTags: (_r, _e, id) => [{ type: "SalesQuotation", id }, "SalesQuotation"],
    }),

    createQuotation: build.mutation<SalesQuotation, CreateSalesQuotationRequest>({
      query: (body) => ({ url: BASE_URL, method: "POST", data: body }),
      invalidatesTags: ["SalesQuotation"],
    }),

    sendQuotation: build.mutation<SalesQuotation, number>({
      query: (id) => ({ url: `${BASE_URL}/${id}/send`, method: "POST" }),
      invalidatesTags: (_r, _e, id) => [{ type: "SalesQuotation", id }, "SalesQuotation"],
    }),

    acceptQuotation: build.mutation<SalesQuotation, number>({
      query: (id) => ({ url: `${BASE_URL}/${id}/accept`, method: "POST" }),
      invalidatesTags: (_r, _e, id) => [{ type: "SalesQuotation", id }, "SalesQuotation"],
    }),

    rejectQuotation: build.mutation<SalesQuotation, { id: number; reason?: string }>({
      query: ({ id, reason }) => ({
        url: `${BASE_URL}/${id}/reject`,
        method: "POST",
        data: reason ? { reason } : {},
      }),
      invalidatesTags: (_r, _e, { id }) => [{ type: "SalesQuotation", id }, "SalesQuotation"],
    }),

    convertToOrder: build.mutation<SalesOrder, number>({
      query: (id) => ({ url: `${BASE_URL}/${id}/convert-to-order`, method: "POST" }),
      invalidatesTags: (_r, _e, id) => [
        { type: "SalesQuotation", id },
        "SalesQuotation",
        "SalesOrder",
      ],
    }),
  }),
});

export const {
  useGetQuotationsQuery,
  useGetQuotationQuery,
  useCreateQuotationMutation,
  useSendQuotationMutation,
  useAcceptQuotationMutation,
  useRejectQuotationMutation,
  useConvertToOrderMutation,
} = salesQuotationApi;
