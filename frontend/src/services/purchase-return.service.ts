import { api } from "./api";
import type { PageResponse } from "@/types/api";

export const PURCHASE_RETURN_STATUSES = [
  "DRAFT",
  "APPROVED",
  "POSTED",
  "CANCELLED",
] as const;
export type PurchaseReturnStatus = (typeof PURCHASE_RETURN_STATUSES)[number];

export const PURCHASE_RETURN_STATUS_LABELS: Record<PurchaseReturnStatus, string> = {
  DRAFT: "Draft",
  APPROVED: "Approved",
  POSTED: "Posted",
  CANCELLED: "Cancelled",
};

export function formatPurchaseReturnStatus(
  status: string | null | undefined
): string {
  if (!status) return "—";
  return PURCHASE_RETURN_STATUS_LABELS[status as PurchaseReturnStatus] ?? status;
}

export interface PurchaseReturnLine {
  id: number;
  lineNumber: number;
  purchaseInvoiceLineId: number | null;
  itemId: number;
  itemCode: string;
  itemName: string;
  uomCode: string;
  returnQuantity: number;
  unitPrice: number;
  taxCategory: string;
  taxRate: number;
  taxableAmount: number;
  taxAmount: number;
  subtotalAmount: number;
  totalAmount: number;
  remarks: string | null;
}

export interface PurchaseReturn {
  id: number;
  tenantId: number;
  companyId: number;
  debitNoteNumber: string;
  fiscalYearId: number | null;
  branchId: number | null;
  purchaseInvoiceId: number;
  invoiceNumber: string;
  goodsReceiptId: number | null;
  grnNumber: string | null;
  purchaseOrderId: number | null;
  poNumber: string | null;
  supplierId: number;
  supplierCode: string;
  supplierName: string;
  supplierPanNumber: string | null;
  warehouseId: number;
  warehouseCode: string | null;
  warehouseName: string | null;
  returnDate: string;
  returnDateNp: string | null;
  reason: string;
  status: PurchaseReturnStatus;
  stockPostStatus: "PENDING" | "POSTED" | "FAILED";
  stockPostError: string | null;
  currency: string;
  subtotalAmount: number;
  taxAmount: number;
  totalAmount: number;
  notes: string | null;
  approvedAt: string | null;
  approvedBy: string | null;
  postedAt: string | null;
  postedBy: string | null;
  cancelledAt: string | null;
  cancelledBy: string | null;
  cancellationReason: string | null;
  createdAt: string;
  lines: PurchaseReturnLine[];
}

export interface ReturnableLineResponse {
  purchaseInvoiceLineId: number;
  lineNumber: number;
  itemId: number;
  itemCode: string;
  itemName: string;
  uomCode: string;
  invoicedQuantity: number;
  alreadyReturnedQuantity: number;
  returnableQuantity: number;
  unitPrice: number;
  taxCategory: string;
  taxRate: number;
}

export interface CreatePurchaseReturnLineRequest {
  purchaseInvoiceLineId?: number;
  itemId: number;
  returnQuantity: number;
  unitPrice?: number;
  taxCategory?: string;
  taxRate?: number;
  remarks?: string;
}

export interface CreatePurchaseReturnRequest {
  purchaseInvoiceId: number;
  warehouseId?: number;
  returnDate?: string;
  reason: string;
  notes?: string;
  lines: CreatePurchaseReturnLineRequest[];
}

export interface ReturnDecisionRequest {
  reason: string;
}

export interface GetPurchaseReturnsParams {
  status?: PurchaseReturnStatus;
  invoiceId?: number;
  search?: string;
  page?: number;
  size?: number;
  sort?: string;
}

const BASE_URL = "/procurement/purchase-returns";

export const purchaseReturnApi = api.injectEndpoints({
  endpoints: (build) => ({
    getPurchaseReturns: build.query<
      PageResponse<PurchaseReturn>,
      GetPurchaseReturnsParams | void
    >({
      query: (params) => ({
        url: BASE_URL,
        method: "GET",
        params: params ?? {},
      }),
      providesTags: ["PurchaseReturn"],
    }),

    getPurchaseReturn: build.query<PurchaseReturn, number>({
      query: (id) => ({
        url: `${BASE_URL}/${id}`,
        method: "GET",
      }),
      providesTags: (_res, _err, id) => [
        { type: "PurchaseReturn", id },
        "PurchaseReturn",
      ],
    }),

    getReturnableLines: build.query<ReturnableLineResponse[], number>({
      query: (invoiceId) => ({
        url: `${BASE_URL}/invoices/${invoiceId}/returnable-lines`,
        method: "GET",
      }),
      providesTags: ["PurchaseReturn", "PurchaseInvoice"],
    }),

    createPurchaseReturn: build.mutation<
      PurchaseReturn,
      CreatePurchaseReturnRequest
    >({
      query: (body) => ({
        url: BASE_URL,
        method: "POST",
        data: body,
      }),
      invalidatesTags: ["PurchaseReturn", "PurchaseInvoice"],
    }),

    approvePurchaseReturn: build.mutation<PurchaseReturn, number>({
      query: (id) => ({
        url: `${BASE_URL}/${id}/approve`,
        method: "POST",
      }),
      invalidatesTags: (_res, _err, id) => [
        { type: "PurchaseReturn", id },
        "PurchaseReturn",
      ],
    }),

    postPurchaseReturn: build.mutation<PurchaseReturn, number>({
      query: (id) => ({
        url: `${BASE_URL}/${id}/post`,
        method: "POST",
      }),
      invalidatesTags: (_res, _err, id) => [
        { type: "PurchaseReturn", id },
        "PurchaseReturn",
        "PurchaseInvoice",
        "Inventory",
      ],
    }),

    cancelPurchaseReturn: build.mutation<
      PurchaseReturn,
      { id: number; data: ReturnDecisionRequest }
    >({
      query: ({ id, data }) => ({
        url: `${BASE_URL}/${id}/cancel`,
        method: "POST",
        data,
      }),
      invalidatesTags: (_res, _err, { id }) => [
        { type: "PurchaseReturn", id },
        "PurchaseReturn",
        "PurchaseInvoice",
        "Inventory",
      ],
    }),

    retryStockPost: build.mutation<PurchaseReturn, number>({
      query: (id) => ({
        url: `${BASE_URL}/${id}/retry-stock`,
        method: "POST",
      }),
      invalidatesTags: (_res, _err, id) => [
        { type: "PurchaseReturn", id },
        "PurchaseReturn",
        "Inventory",
      ],
    }),
  }),
});

export const {
  useGetPurchaseReturnsQuery,
  useGetPurchaseReturnQuery,
  useGetReturnableLinesQuery,
  useCreatePurchaseReturnMutation,
  useApprovePurchaseReturnMutation,
  usePostPurchaseReturnMutation,
  useCancelPurchaseReturnMutation,
  useRetryStockPostMutation,
} = purchaseReturnApi;

