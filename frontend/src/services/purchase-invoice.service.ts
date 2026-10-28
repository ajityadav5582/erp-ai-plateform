import { api } from "./api";
import type { PageResponse } from "@/types/api";

/**
 * Mirrors the backend `PurchaseInvoiceStatus` enum.
 *
 * An invoice is created APPROVED rather than SUBMITTED: the supplier has already
 * issued it and the company has already received it, so there is nothing for an
 * internal approver to authorise. What is *not* assumed is that it matches the order
 * and the delivery, and comparing it against them is the point of the screen.
 */
export const PURCHASE_INVOICE_STATUSES = [
  "APPROVED",
  "PARTIALLY_PAID",
  "PAID",
  "CANCELLED",
] as const;
export type PurchaseInvoiceStatus = (typeof PURCHASE_INVOICE_STATUSES)[number];

export const PURCHASE_INVOICE_STATUS_LABELS: Record<PurchaseInvoiceStatus, string> = {
  APPROVED: "Approved",
  PARTIALLY_PAID: "Partially Paid",
  PAID: "Paid",
  CANCELLED: "Cancelled",
};

/** Statuses that still owe money. These are the rows a treasury screen cares about. */
export const OUTSTANDING_INVOICE_STATUSES: PurchaseInvoiceStatus[] = [
  "APPROVED",
  "PARTIALLY_PAID",
];

export function formatPurchaseInvoiceStatus(
  status: string | null | undefined
): string {
  if (!status) return "—";
  return PURCHASE_INVOICE_STATUS_LABELS[status as PurchaseInvoiceStatus] ?? status;
}

/** Mirrors the nested `PurchaseInvoiceResponse.Line` record. */
export interface PurchaseInvoiceLine {
  id: number;
  lineNumber: number;
  goodsReceiptLineId: number;
  itemId: number;
  itemCode: string;
  itemName: string;
  uomCode: string;
  quantity: number;
  unitPrice: number;
  discountPercentage: number;
  taxRate: number;
  lineAmount: number;
  lineTaxAmount: number;
  description: string | null;
}

/** Mirrors the backend `PurchaseInvoiceResponse` record. */
export interface PurchaseInvoice {
  id: number;
  invoiceNumber: string;
  tenantId: number;
  companyId: number;
  branchId: number;
  fiscalYearId: number;
  /** The order whose goods were billed. */
  purchaseOrderId: number;
  poNumber: string;
  /** The delivery being billed. One invoice per delivery. */
  goodsReceiptId: number;
  grnNumber: string;
  invoiceDate: string;
  dueDate: string | null;
  supplierId: number;
  supplierCode: string;
  supplierName: string;
  currency: string;
  status: PurchaseInvoiceStatus;
  subtotalAmount: number;
  taxAmount: number;
  totalAmount: number;
  /** How much has been settled so far. */
  paidAmount: number;
  /** What is still owed: total minus paid. */
  balanceAmount: number;
  notes: string | null;
  cancellationReason: string | null;
  createdAt: string;
  lines: PurchaseInvoiceLine[];
}

/** Mirrors the nested `CreateInvoiceRequest.Line` record. */
export interface CreateInvoiceLineRequest {
  goodsReceiptLineId: number;
  /** Omitted, the received quantity is used. */
  quantity?: number;
  /** Omitted, the received unit price is used. */
  unitPrice?: number;
  discountPercentage?: number;
  taxRate?: number;
  description?: string;
}

/**
 * Mirrors the backend `CreateInvoiceRequest` record.
 *
 * **Prices come from the supplier, not the order.** That is the whole point of a
 * three-way match: the amount owed is what the supplier says they charged, and holding
 * it against the order is what surfaces a discrepancy. A form that pre-filled the
 * ordered price and let the user accept it would make a mismatch impossible to see.
 * The form therefore pre-fills for *speed only* and every field stays editable.
 */
export interface CreateInvoiceRequest {
  goodsReceiptId: number;
  /** The supplier's own invoice number, not a number this system issued. */
  invoiceNumber: string;
  /** Defaults to today server-side when omitted. */
  invoiceDate?: string;
  dueDate?: string;
  lines: CreateInvoiceLineRequest[];
  notes?: string;
}

export interface GetPurchaseInvoicesParams {
  page?: number;
  size?: number;
  sort?: string;
}

const BASE_URL = "/procurement/purchase-invoices";

/**
 * Purchase invoice API endpoints with RTK Query caching and invalidation.
 *
 * Creating an invoice invalidates `GoodsReceipt` as well: an invoice is billed against
 * a specific delivery, so a receipt screen holding a pre-invoice copy would still offer
 * to bill the same delivery again.
 */
export const purchaseInvoiceApi = api.injectEndpoints({
  endpoints: (build) => ({
    getPurchaseInvoices: build.query<
      PageResponse<PurchaseInvoice>,
      GetPurchaseInvoicesParams | void
    >({
      query: (params) => ({
        url: BASE_URL,
        method: "GET",
        params: params ?? {},
      }),
      providesTags: ["PurchaseInvoice"],
    }),

    getPurchaseInvoice: build.query<PurchaseInvoice, number>({
      query: (invoiceId) => ({
        url: `${BASE_URL}/${invoiceId}`,
        method: "GET",
      }),
      providesTags: (_result, _error, invoiceId) => [
        { type: "PurchaseInvoice", id: invoiceId },
        "PurchaseInvoice",
      ],
    }),

    /** Invoices still owing money: the accounts payable worklist. */
    getOutstandingInvoices: build.query<
      PageResponse<PurchaseInvoice>,
      GetPurchaseInvoicesParams | void
    >({
      query: (params) => ({
        url: `${BASE_URL}/outstanding`,
        method: "GET",
        params: params ?? {},
      }),
      providesTags: ["PurchaseInvoice"],
    }),

    createPurchaseInvoice: build.mutation<
      PurchaseInvoice,
      CreateInvoiceRequest
    >({
      query: (body) => ({
        url: BASE_URL,
        method: "POST",
        data: body,
      }),
      invalidatesTags: ["PurchaseInvoice", "GoodsReceipt"],
    }),
  }),
});

export const {
  useGetPurchaseInvoicesQuery,
  useGetPurchaseInvoiceQuery,
  useGetOutstandingInvoicesQuery,
  useCreatePurchaseInvoiceMutation,
} = purchaseInvoiceApi;
