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

/** Line-level allocation against a specific purchase invoice. */
export interface PaymentAllocation {
  id?: number;
  purchaseInvoiceId: number;
  invoiceNumber?: string;
  allocatedAmount: number;
  discountAmount?: number;
  notes?: string | null;
}

/** Mirrors the backend `SupplierPaymentResponse` record. */
export interface SupplierPayment {
  id: number;
  paymentNumber: string;
  tenantId: number;
  companyId: number;
  branchId: number;
  fiscalYearId: number;
  /** The invoice this payment settles (for single-invoice) or null (for multi-invoice). */
  purchaseInvoiceId: number | null;
  invoiceNumber: string | null;
  paymentDate: string;
  supplierId: number;
  supplierCode: string;
  supplierName: string;
  amount: number;
  unallocatedAmount: number;
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
  allocations?: PaymentAllocation[];
}

export interface CreatePaymentAllocationRequest {
  purchaseInvoiceId: number;
  allocatedAmount: number;
  discountAmount?: number;
  notes?: string;
}

/**
 * Mirrors the backend `CreatePaymentRequest` record.
 *
 * Supports both single-invoice settlement and multi-invoice allocations.
 */
export interface CreatePaymentRequest {
  purchaseInvoiceId?: number | null;
  supplierId?: number;
  amount: number;
  paymentMethod: PaymentMethod;
  /** Defaults to today server-side when omitted. */
  paymentDate?: string;
  referenceNo?: string;
  paidFrom?: string;
  notes?: string;
  allocations?: CreatePaymentAllocationRequest[];
}

export interface ApplyAdvanceRequest {
  advancePaymentId: number;
  purchaseInvoiceId: number;
  amountToApply: number;
  notes?: string;
}

export interface GetSupplierPaymentsParams {
  page?: number;
  size?: number;
  sort?: string;
}

export interface SupplierBalanceSummary {
  supplierId: number;
  totalOutstandingPayable: number;
  totalAvailableAdvance: number;
  unpaidInvoiceCount: number;
  availableAdvanceCount: number;
  currency: string;
}

export interface SupplierStatementLine {
  date: string;
  documentType: "INVOICE" | "PAYMENT" | "DEBIT_NOTE";
  documentId: number;
  documentNumber: string;
  referenceNumber: string | null;
  description: string;
  debitAmount: number;
  creditAmount: number;
  runningBalance: number;
}

export interface SupplierStatement {
  supplierId: number;
  supplierCode: string;
  supplierName: string;
  currency: string;
  fromDate: string | null;
  toDate: string | null;
  openingBalance: number;
  totalInvoiced: number;
  totalPaid: number;
  totalReturns: number;
  closingBalance: number;
  lines: SupplierStatementLine[];
}

export interface GetSupplierStatementParams {
  supplierId: number;
  fromDate?: string;
  toDate?: string;
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

    /** Available unallocated advance payments for a supplier. */
    getAvailableAdvances: build.query<SupplierPayment[], number>({
      query: (supplierId) => ({
        url: `${BASE_URL}/advances/available`,
        method: "GET",
        params: { supplierId },
      }),
      providesTags: (_result, _error, supplierId) => [
        { type: "SupplierPayment", id: `supplier-advances-${supplierId}` },
        "SupplierPayment",
      ],
    }),

    /** Apply available advance credit to settle/offset an approved purchase invoice. */
    applyAdvance: build.mutation<SupplierPayment, ApplyAdvanceRequest>({
      query: (body) => ({
        url: `${BASE_URL}/apply-advance`,
        method: "POST",
        data: body,
      }),
      invalidatesTags: ["SupplierPayment", "PurchaseInvoice"],
    }),

    /** Live balance summary (total outstanding payable & available advance credit) for a supplier. */
    getSupplierBalanceSummary: build.query<SupplierBalanceSummary, number>({
      query: (supplierId) => ({
        url: `${BASE_URL}/suppliers/${supplierId}/balance-summary`,
        method: "GET",
      }),
      providesTags: (_result, _error, supplierId) => [
        { type: "SupplierPayment", id: `supplier-balance-${supplierId}` },
        "SupplierPayment",
        "PurchaseInvoice",
      ],
    }),

    /** Chronological running Statement of Account for a supplier. */
    getSupplierStatement: build.query<SupplierStatement, GetSupplierStatementParams>({
      query: ({ supplierId, fromDate, toDate }) => ({
        url: `${BASE_URL}/suppliers/${supplierId}/statement`,
        method: "GET",
        params: {
          ...(fromDate ? { fromDate } : {}),
          ...(toDate ? { toDate } : {}),
        },
      }),
      providesTags: (_result, _error, { supplierId }) => [
        { type: "SupplierPayment", id: `supplier-statement-${supplierId}` },
        "SupplierPayment",
        "PurchaseInvoice",
      ],
    }),
  }),
});

export const {
  useGetSupplierPaymentsQuery,
  useGetSupplierPaymentQuery,
  useGetSupplierPaymentsByInvoiceQuery,
  useGetAvailableAdvancesQuery,
  useGetSupplierBalanceSummaryQuery,
  useGetSupplierStatementQuery,
  usePaySupplierMutation,
  useApplyAdvanceMutation,
} = supplierPaymentApi;
