import { api } from "./api";
import type { PageResponse } from "@/types/api";
import type { VatType } from "./sales-invoice.service";

/** Mirrors the backend `ReturnStatus` enum. */
export const RETURN_STATUSES = ["DRAFT", "POSTED", "CANCELLED"] as const;
export type ReturnStatus = (typeof RETURN_STATUSES)[number];

export const RETURN_STATUS_LABELS: Record<ReturnStatus, string> = {
  DRAFT: "Draft",
  POSTED: "Posted",
  CANCELLED: "Cancelled",
};

export interface SalesReturnLine {
  id: number;
  lineNumber: number;
  itemId: number;
  itemCode: string;
  itemName: string;
  uomCode: string;
  quantity: number;
  unitPrice: number;
  vatType: VatType;
  vatRate: number;
  lineAmount: number;
  vatAmount: number;
  description: string | null;
}

export interface SalesReturn {
  id: number;
  tenantId: number;
  companyId: number;
  creditNoteNumber: string;
  fiscalYearId: number | null;
  branchId: number | null;
  invoiceId: number;
  invoiceNumber: string;
  salesOrderId: number | null;
  soNumber: string | null;
  customerId: number;
  customerCode: string;
  customerName: string;
  customerPanNumber: string | null;
  warehouseId: number;
  returnDate: string;
  returnDateNp: string;
  reason: string | null;
  status: ReturnStatus;
  subtotalAmount: number;
  vatAmount: number;
  totalAmount: number;
  lines: SalesReturnLine[];
  version: number;
}

export interface ReturnLineRequest {
  itemId: number;
  uomCode: string;
  quantity: number;
  unitPrice: number;
  vatType: VatType;
  vatRate?: number;
  description?: string;
}

export interface CreateReturnRequest {
  invoiceId: number;
  warehouseId: number;
  returnDate: string;
  reason?: string;
  lines: ReturnLineRequest[];
}

export interface ReturnDecisionRequest {
  reason: string;
}

export interface GetReturnsParams {
  page?: number;
  size?: number;
  sort?: string;
}

const BASE_URL = "/sales/returns";

export const salesReturnApi = api.injectEndpoints({
  endpoints: (build) => ({
    getReturns: build.query<PageResponse<SalesReturn>, GetReturnsParams | void>({
      query: (params) => ({ url: BASE_URL, method: "GET", params: params ?? {} }),
      providesTags: (result) => [
        ...(result?.content.map((r) => ({ type: "SalesReturn" as const, id: r.id })) ?? []),
        "SalesReturn",
      ],
    }),

    getReturn: build.query<SalesReturn, number>({
      query: (id) => ({ url: `${BASE_URL}/${id}`, method: "GET" }),
      providesTags: (_r, _e, id) => [{ type: "SalesReturn", id }, "SalesReturn"],
    }),

    createReturn: build.mutation<SalesReturn, CreateReturnRequest>({
      query: (body) => ({ url: BASE_URL, method: "POST", data: body }),
      invalidatesTags: ["SalesReturn", "SalesInvoice"],
    }),

    postReturn: build.mutation<SalesReturn, number>({
      query: (id) => ({ url: `${BASE_URL}/${id}/post`, method: "POST" }),
      invalidatesTags: (_r, _e, id) => [{ type: "SalesReturn", id }, "SalesReturn"],
    }),

    cancelReturn: build.mutation<
      SalesReturn,
      { id: number; data: ReturnDecisionRequest }
    >({
      query: ({ id, data }) => ({ url: `${BASE_URL}/${id}/cancel`, method: "POST", data }),
      invalidatesTags: (_r, _e, { id }) => [{ type: "SalesReturn", id }, "SalesReturn"],
    }),
  }),
});

export const {
  useGetReturnsQuery,
  useGetReturnQuery,
  useCreateReturnMutation,
  usePostReturnMutation,
  useCancelReturnMutation,
} = salesReturnApi;
