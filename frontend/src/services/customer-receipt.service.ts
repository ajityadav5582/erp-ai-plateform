import { api } from "./api";
import type { PageResponse } from "@/types/api";

/** Mirrors the backend `PaymentMethod` enum. */
export const PAYMENT_METHODS = [
  "CASH",
  "BANK_TRANSFER",
  "CHEQUE",
  "CARD",
  "DIGITAL_WALLET",
  "OTHER",
] as const;
export type PaymentMethod = (typeof PAYMENT_METHODS)[number];

export const PAYMENT_METHOD_LABELS: Record<PaymentMethod, string> = {
  CASH: "Cash",
  BANK_TRANSFER: "Bank Transfer",
  CHEQUE: "Cheque",
  CARD: "Card",
  DIGITAL_WALLET: "Digital Wallet",
  OTHER: "Other",
};

/** Mirrors the backend `ReceiptStatus` enum. */
export const RECEIPT_STATUSES = ["POSTED", "CANCELLED"] as const;
export type ReceiptStatus = (typeof RECEIPT_STATUSES)[number];

export const RECEIPT_STATUS_LABELS: Record<ReceiptStatus, string> = {
  POSTED: "Posted",
  CANCELLED: "Cancelled",
};

export interface CustomerReceipt {
  id: number;
  tenantId: number;
  companyId: number;
  receiptNumber: string;
  fiscalYearId: number | null;
  branchId: number | null;
  invoiceId: number;
  invoiceNumber: string;
  customerId: number;
  customerCode: string;
  customerName: string;
  amount: number;
  paymentMethod: PaymentMethod;
  receiptDate: string;
  reference: string | null;
  notes: string | null;
  status: ReceiptStatus;
  version: number;
}

export interface CreateReceiptRequest {
  invoiceId: number;
  amount: number;
  paymentMethod: PaymentMethod;
  receiptDate: string;
  reference?: string;
  notes?: string;
}

export interface ReceiptDecisionRequest {
  reason: string;
}

export interface GetReceiptsParams {
  page?: number;
  size?: number;
  sort?: string;
}

const BASE_URL = "/sales/receipts";

export const customerReceiptApi = api.injectEndpoints({
  endpoints: (build) => ({
    getReceipts: build.query<PageResponse<CustomerReceipt>, GetReceiptsParams | void>({
      query: (params) => ({ url: BASE_URL, method: "GET", params: params ?? {} }),
      providesTags: (result) => [
        ...(result?.content.map((r) => ({ type: "CustomerReceipt" as const, id: r.id })) ?? []),
        "CustomerReceipt",
      ],
    }),

    getReceipt: build.query<CustomerReceipt, number>({
      query: (id) => ({ url: `${BASE_URL}/${id}`, method: "GET" }),
      providesTags: (_r, _e, id) => [{ type: "CustomerReceipt", id }, "CustomerReceipt"],
    }),

    createReceipt: build.mutation<CustomerReceipt, CreateReceiptRequest>({
      query: (body) => ({ url: BASE_URL, method: "POST", data: body }),
      invalidatesTags: ["CustomerReceipt", "SalesInvoice"],
    }),

    cancelReceipt: build.mutation<
      CustomerReceipt,
      { id: number; data: ReceiptDecisionRequest }
    >({
      query: ({ id, data }) => ({ url: `${BASE_URL}/${id}/cancel`, method: "POST", data }),
      invalidatesTags: (_r, _e, { id }) => [
        { type: "CustomerReceipt", id },
        "CustomerReceipt",
        "SalesInvoice",
      ],
    }),
  }),
});

export const {
  useGetReceiptsQuery,
  useGetReceiptQuery,
  useCreateReceiptMutation,
  useCancelReceiptMutation,
} = customerReceiptApi;
