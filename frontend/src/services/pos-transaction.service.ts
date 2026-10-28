import { api } from "./api";
import type { PageResponse } from "@/types/api";
import type { PosVatType } from "./pos-scan.service";

export { POS_VAT_TYPES } from "./pos-scan.service";
export type { PosVatType } from "./pos-scan.service";

/** Mirrors the backend `PosPaymentMethod` enum. */
export const POS_PAYMENT_METHODS = [
  "CASH",
  "CARD",
  "DIGITAL_WALLET",
  "BANK_TRANSFER",
  "CHEQUE",
  "OTHER",
] as const;
export type PosPaymentMethod = (typeof POS_PAYMENT_METHODS)[number];

export const POS_PAYMENT_METHOD_LABELS: Record<PosPaymentMethod, string> = {
  CASH: "Cash",
  CARD: "Card",
  DIGITAL_WALLET: "Digital Wallet",
  BANK_TRANSFER: "Bank Transfer",
  CHEQUE: "Cheque",
  OTHER: "Other",
};

/** Mirrors the backend `PosTransactionStatus` enum. */
export const POS_TRANSACTION_STATUSES = ["OPEN", "COMPLETED", "VOIDED"] as const;
export type PosTransactionStatus = (typeof POS_TRANSACTION_STATUSES)[number];

export const POS_TRANSACTION_STATUS_LABELS: Record<PosTransactionStatus, string> = {
  OPEN: "Parked",
  COMPLETED: "Completed",
  VOIDED: "Voided",
};

/** Mirrors the backend `PosTransactionLineResponse` record. */
export interface PosTransactionLine {
  id: number;
  lineNumber: number;
  itemId: number;
  itemCode: string;
  itemName: string;
  barcode: string | null;
  uomCode: string;
  quantity: number;
  unitPrice: number;
  discountPercentage: number;
  lineAmount: number;
  vatType: PosVatType;
  vatRate: number;
  vatAmount: number;
  description: string | null;
}

/** Mirrors the backend `PosTransactionResponse` record. */
export interface PosTransaction {
  id: number;
  tenantId: number;
  companyId: number;
  transactionNumber: string | null;
  sessionId: number;
  registerId: number;
  fiscalYearId: number | null;
  branchId: number | null;
  transactionDate: string;
  transactionDateNp: string;
  customerId: number | null;
  customerCode: string | null;
  customerName: string;
  customerPanNumber: string | null;
  cashierId: string;
  cashierName: string;
  warehouseId: number;
  warehouseCode: string;
  warehouseName: string;
  currency: string;
  subtotalAmount: number;
  taxableAmount: number;
  exemptAmount: number;
  zeroRatedAmount: number;
  vatAmount: number;
  totalAmount: number;
  paymentMethod: PosPaymentMethod | null;
  paymentReference: string | null;
  amountTendered: number | null;
  changeAmount: number | null;
  status: PosTransactionStatus;
  completedAt: string | null;
  voidedAt: string | null;
  voidedBy: string | null;
  voidReason: string | null;
  isTaxInvoice: boolean;
  cbmsSynced: boolean;
  cbmsRetryCount: number;
  notes: string | null;
  lines: PosTransactionLine[];
  version: number;
}

/** Mirrors the backend `PosReceiptResponse` record. */
export interface PosReceipt {
  transaction: PosTransaction;
  sessionNumber: string;
  registerCode: string;
  registerName: string;
}

export interface CheckoutLineRequest {
  itemId: number;
  quantity: number;
  discountPercentage?: number;
}

export interface CheckoutRequest {
  sessionId: number;
  tableId?: number;
  customerId?: number;
  customerName?: string;
  customerPanNumber?: string;
  lines: CheckoutLineRequest[];
  paymentMethod: PosPaymentMethod;
  amountTendered?: number;
  paymentReference?: string;
  notes?: string;
}

export interface ParkTransactionRequest {
  sessionId: number;
  customerId?: number;
  customerName?: string;
  customerPanNumber?: string;
  lines: CheckoutLineRequest[];
  notes?: string;
}

export interface UpdateLineRequest {
  quantity?: number;
  discountPercentage?: number;
}

export interface VoidTransactionRequest {
  reason: string;
}

export interface GetPosTransactionsParams {
  page?: number;
  size?: number;
  sort?: string;
  status?: PosTransactionStatus;
  sessionId?: number;
  registerId?: number;
  term?: string;
}

const BASE_URL = "/pos/transactions";

export const posTransactionApi = api.injectEndpoints({
  endpoints: (build) => ({
    getPosTransactions: build.query<PageResponse<PosTransaction>, GetPosTransactionsParams | void>({
      query: (params) => ({ url: BASE_URL, method: "GET", params: params ?? {} }),
      providesTags: (result) => [
        ...(result?.content.map((t) => ({ type: "PosTransaction" as const, id: t.id })) ?? []),
        "PosTransaction",
      ],
    }),

    getPosTransaction: build.query<PosTransaction, number>({
      query: (id) => ({ url: `${BASE_URL}/${id}`, method: "GET" }),
      providesTags: (_r, _e, id) => [{ type: "PosTransaction", id }, "PosTransaction"],
    }),

    getPosReceipt: build.query<PosReceipt, number>({
      query: (id) => ({ url: `${BASE_URL}/${id}/receipt`, method: "GET" }),
      providesTags: (_r, _e, id) => [{ type: "PosTransaction", id }],
    }),

    /** Parked (OPEN) carts, newest first — the terminal's resume list. */
    getParkedPosTransactions: build.query<PageResponse<PosTransaction>, void>({
      query: () => ({ url: BASE_URL, method: "GET", params: { status: "OPEN", size: 50 } }),
      providesTags: ["PosTransaction"],
    }),

    checkout: build.mutation<PosTransaction, { request: CheckoutRequest; idempotencyKey?: string }>({
      query: ({ request, idempotencyKey }) => ({
        url: BASE_URL,
        method: "POST",
        data: request,
        headers: idempotencyKey ? { "Idempotency-Key": idempotencyKey } : undefined,
      }),
      invalidatesTags: ["PosTransaction", "PosSession"],
    }),

    parkTransaction: build.mutation<PosTransaction, ParkTransactionRequest>({
      query: (body) => ({ url: `${BASE_URL}/park`, method: "POST", data: body }),
      invalidatesTags: ["PosTransaction"],
    }),

    addTransactionLine: build.mutation<
      PosTransaction,
      { id: number; body: CheckoutLineRequest }
    >({
      query: ({ id, body }) => ({ url: `${BASE_URL}/${id}/lines`, method: "POST", data: body }),
      invalidatesTags: (_r, _e, { id }) => [{ type: "PosTransaction", id }, "PosTransaction"],
    }),

    updateTransactionLine: build.mutation<
      PosTransaction,
      { id: number; lineNumber: number; body: UpdateLineRequest }
    >({
      query: ({ id, lineNumber, body }) => ({
        url: `${BASE_URL}/${id}/lines/${lineNumber}`,
        method: "PUT",
        data: body,
      }),
      invalidatesTags: (_r, _e, { id }) => [{ type: "PosTransaction", id }, "PosTransaction"],
    }),

    removeTransactionLine: build.mutation<PosTransaction, { id: number; lineNumber: number }>({
      query: ({ id, lineNumber }) => ({
        url: `${BASE_URL}/${id}/lines/${lineNumber}`,
        method: "DELETE",
      }),
      invalidatesTags: (_r, _e, { id }) => [{ type: "PosTransaction", id }, "PosTransaction"],
    }),

    voidPosTransaction: build.mutation<PosTransaction, { id: number; body: VoidTransactionRequest }>({
      query: ({ id, body }) => ({ url: `${BASE_URL}/${id}/void`, method: "POST", data: body }),
      invalidatesTags: (_r, _e, { id }) => [
        { type: "PosTransaction", id },
        "PosTransaction",
        "PosSession",
      ],
    }),
  }),
});

export const {
  useGetPosTransactionsQuery,
  useGetPosTransactionQuery,
  useGetPosReceiptQuery,
  useGetParkedPosTransactionsQuery,
  useCheckoutMutation,
  useParkTransactionMutation,
  useAddTransactionLineMutation,
  useUpdateTransactionLineMutation,
  useRemoveTransactionLineMutation,
  useVoidPosTransactionMutation,
} = posTransactionApi;
