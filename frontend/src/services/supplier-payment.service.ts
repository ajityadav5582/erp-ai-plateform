import { api } from "./api";
import type { PageResponse } from "@/types/api";

/**
 * Mirrors the backend `SupplierPaymentStatus` enum.
 *
 * A payment is created POSTED. Money leaving the bank is not a request to do so, so a
 * pending state would only create the illusion that an un-posted payment can still be
 * reviewed.
 */
export const SUPPLIER_PAYMENT_STATUSES = ["POSTED", "CANCELLED"] as const;
export type SupplierPaymentStatus = (typeof SUPPLIER_PAYMENT_STATUSES)[number];

export const SUPPLIER_PAYMENT_STATUS_LABELS: Record<SupplierPaymentStatus, string> = {
  POSTED: "Posted",
  CANCELLED: "Cancelled",
};

/** Mirrors the backend `PaymentMethod` enum. */
export const PAYMENT_METHODS = [
  "CASH",
  "BANK_TRANSFER",
  "CHEQUE",
  "CARD",
  "OTHER",
] as const;
export type PaymentMethod = (typeof PAYMENT_METHODS)[number];

export const PAYMENT_METHOD_LABELS: Record<PaymentMethod, string> = {
  CASH: "Cash",
  BANK_TRANSFER: "Bank Transfer",
  CHEQUE: "Cheque",
  CARD: "Card",
  OTHER: "Other",
};

/**
 * Methods for which a reference is meaningful.
 *
 * A cheque number and a bank reference are the two things an auditor asks for, so the
 * payment form asks for one whenever the method is either of those. The server does
 * not require it, so this is a prompt rather than a validation - the reason is free
 * text and a company may legitimately record one differently.
 */
export const REFERENCE_METHODS: PaymentMethod[] = ["CHEQUE", "BANK_TRANSFER"];

export function formatPaymentMethod(
  method: string | null | undefined
): string {
  if (!method) return "—";
  return PAYMENT_METHOD_LABELS[method as PaymentMethod] ?? method;
}

export function formatSupplierPaymentStatus(
  status: string | null | undefined
): string {
  if (!status) return "—";
  return (
    SUPPLIER_PAYMENT_STATUS_LABELS[status as SupplierPaymentStatus] ?? status
  );
}

/** Mirrors the backend `SupplierPaymentResponse` record. */
export interface SupplierPayment {
  id: number;
  paymentNumber: string;
  tenantId: number;
  companyId: number;
  branchId: number;
  fiscalYearId: number;
  /** The invoice this payment settles. */
  purchaseInvoiceId: number;
  invoiceNumber: string;
  paymentDate: string;
  supplierId: number;
  supplierCode: string;
  supplierName: string;
  amount: number;
  currency: string;
  paymentMethod: PaymentMethod;
  referenceNo: string | null;
  paidFrom: string | null;
  status: SupplierPaymentStatus;
  notes: string | null;
  postedAt: string | null;
  postedBy: string | null;
  cancellationReason: string | null;
  createdAt: string;
}

/**
 * Mirrors the backend `CreatePaymentRequest` record.
 *
 * The amount is required rather than defaulting to the whole balance, because settling
 * in instalments is ordinary and "pay the balance" is rarely what a treasury clerk means
 * when a part payment has fallen overdue. Supplier and currency are deliberately absent:
 * they belong to the invoice being settled, and taking them from the caller would allow
 * a payment to be recorded against an invoice of a different currency.
 */
export interface CreatePaymentRequest {
  purchaseInvoiceId: number;
  amount: number;
  paymentMethod: PaymentMethod;
  /** Defaults to today server-side when omitted. */
  paymentDate?: string;
  referenceNo?: string;
  paidFrom?: string;
  notes?: string;
}

export interface GetSupplierPaymentsParams {
  page?: number;
  size?: number;
  sort?: string;
}

const BASE_URL = "/procurement/supplier-payments";

/**
 * Supplier payment API endpoints with RTK Query caching and invalidation.
 *
 * Paying invalidates `PurchaseInvoice` because it moves the invoice's paid amount and
 * status. Without this, the invoice screen would keep showing a settled balance as
 * outstanding and would offer to pay it again.
 */
export const supplierPaymentApi = api.injectEndpoints({
  endpoints: (build) => ({
    getSupplierPayments: build.query<
      PageResponse<SupplierPayment>,
      GetSupplierPaymentsParams | void
    >({
      query: (params) => ({
        url: BASE_URL,
        method: "GET",
        params: params ?? {},
      }),
      providesTags: ["SupplierPayment"],
    }),

    getSupplierPayment: build.query<SupplierPayment, number>({
      query: (paymentId) => ({
        url: `${BASE_URL}/${paymentId}`,
        method: "GET",
      }),
      providesTags: (_result, _error, paymentId) => [
        { type: "SupplierPayment", id: paymentId },
        "SupplierPayment",
      ],
    }),

    /** The payments made against one invoice. */
    getSupplierPaymentsByInvoice: build.query<
      PageResponse<SupplierPayment>,
      { invoiceId: number } & GetSupplierPaymentsParams
    >({
      query: ({ invoiceId, ...params }) => ({
        url: `${BASE_URL}/by-invoice/${invoiceId}`,
        method: "GET",
        params,
      }),
      providesTags: (_result, _error, { invoiceId }) => [
        { type: "SupplierPayment", id: `invoice-${invoiceId}` },
        "SupplierPayment",
      ],
    }),

    paySupplier: build.mutation<SupplierPayment, CreatePaymentRequest>({
      query: (body) => ({
        url: BASE_URL,
        method: "POST",
        data: body,
      }),
      invalidatesTags: ["SupplierPayment", "PurchaseInvoice"],
    }),
  }),
});

export const {
  useGetSupplierPaymentsQuery,
  useGetSupplierPaymentQuery,
  useGetSupplierPaymentsByInvoiceQuery,
  usePaySupplierMutation,
} = supplierPaymentApi;
