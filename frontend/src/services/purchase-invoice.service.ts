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

export type PurchaseInvoiceType = "GOODS_RECEIPT" | "DIRECT";

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
  goodsReceiptLineId: number | null;
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
  taxableAmount: number | null;
  description: string | null;
}

/** Mirrors the backend `PurchaseInvoiceResponse` record. */
export interface PurchaseInvoice {
  id: number;
  invoiceNumber: string;
  tenantId: number;
  companyId: number;
  branchId: number | null;
  fiscalYearId: number | null;
  invoiceType?: PurchaseInvoiceType;
  /** The order whose goods were billed, or null for direct invoices. */
  purchaseOrderId: number | null;
  poNumber: string | null;
  /** The delivery being billed, or null for direct invoices. */
  goodsReceiptId: number | null;
  grnNumber: string | null;
  invoiceDate: string;
  dueDate: string | null;
  supplierId: number;
  supplierCode: string;
  supplierName: string;
  supplierVatRegistrationStatus: string | null;
  supplierVatRegistrationNumber: string | null;
  supplierTaxId: string | null;
  currency: string;
  status: PurchaseInvoiceStatus;
  subtotalAmount: number;
  taxAmount: number;
  totalAmount: number;
  isTdsApplicable?: boolean;
  tdsRate?: number;
  tdsAmount?: number;
  netPayableAmount?: number;
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
  goodsReceiptLineId?: number | null;
  itemId?: number;
  itemCode?: string;
  itemName?: string;
  uomCode?: string;
  /** Omitted, the received quantity is used. */
  quantity?: number;
  /** Omitted, the received unit price is used. */
  unitPrice?: number;
  discountPercentage?: number;
  taxRate?: number;
  description?: string;
  taxCategory?: "TAXABLE" | "EXEMPT" | "ZERO_RATED" | "NON_TAXABLE";
}

/**
 * Mirrors the backend `CreateInvoiceRequest` record.
 */
export interface CreateInvoiceRequest {
  goodsReceiptId?: number | null;
  invoiceType?: PurchaseInvoiceType;
  supplierId?: number;
  branchId?: number | null;
  fiscalYearId?: number | null;
  currency?: string;
  /** The supplier's own invoice number, not a number this system issued. */
  invoiceNumber: string;
  /** Defaults to today server-side when omitted. */
  invoiceDate?: string;
  dueDate?: string;
  isTdsApplicable?: boolean;
  tdsRate?: number;
  lines: CreateInvoiceLineRequest[];
  notes?: string;
}

export interface GetPurchaseInvoicesParams {
  page?: number;
  size?: number;
  sort?: string;
  supplierId?: number;
}

export interface FastMatchPoRequest {
  purchaseOrderId: number;
  invoiceNumber: string;
  invoiceDate?: string;
  dueDate?: string;
  warehouseId?: number;
  deliveryChallanNo?: string;
  notes?: string;
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

    /** Fast-match flow: simultaneously creates Goods Receipt and Purchase Invoice against approved PO. */
    fastMatchPo: build.mutation<PurchaseInvoice, FastMatchPoRequest>({
      query: (body) => ({
        url: `${BASE_URL}/fast-match-po`,
        method: "POST",
        data: body,
      }),
      invalidatesTags: ["PurchaseInvoice", "GoodsReceipt", "PurchaseOrder"],
    }),
  }),
});

export const {
  useGetPurchaseInvoicesQuery,
  useGetPurchaseInvoiceQuery,
  useGetOutstandingInvoicesQuery,
  useCreatePurchaseInvoiceMutation,
  useFastMatchPoMutation,
} = purchaseInvoiceApi;
