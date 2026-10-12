import { api } from "./api";
import type { PageResponse } from "@/types/api";

/** Mirrors the backend `SalesInvoiceStatus` enum. */
export const SALES_INVOICE_STATUSES = [
  "DRAFT",
  "ISSUED",
  "PARTIALLY_PAID",
  "PAID",
  "CANCELLED",
] as const;
export type SalesInvoiceStatus = (typeof SALES_INVOICE_STATUSES)[number];

export const SALES_INVOICE_STATUS_LABELS: Record<SalesInvoiceStatus, string> = {
  DRAFT: "Draft",
  ISSUED: "Issued",
  PARTIALLY_PAID: "Partially Paid",
  PAID: "Paid",
  CANCELLED: "Cancelled",
};

/** Mirrors the backend `VatType` enum. */
export const VAT_TYPES = ["TAXABLE", "EXEMPT", "ZERO_RATED", "EXPORT"] as const;
export type VatType = (typeof VAT_TYPES)[number];

export const VAT_TYPE_LABELS: Record<VatType, string> = {
  TAXABLE: "Taxable (13%)",
  EXEMPT: "Exempt",
  ZERO_RATED: "Zero-rated",
  EXPORT: "Export",
};

export interface SalesInvoiceLine {
  id: number;
  lineNumber: number;
  itemId: number;
  itemCode: string;
  itemName: string;
  uomCode: string;
  quantity: number;
  unitPrice: number;
  discountPercentage: number;
  vatType: VatType;
  vatRate: number;
  lineAmount: number;
  vatAmount: number;
  description: string | null;
}

export interface SalesInvoice {
  id: number;
  tenantId: number;
  companyId: number;
  invoiceNumber: string;
  fiscalYearId: number | null;
  branchId: number | null;
  salesOrderId: number;
  soNumber: string;
  deliveryId: number | null;
  deliveryNumber: string | null;
  customerId: number;
  customerCode: string;
  customerName: string;
  customerPanNumber: string | null;
  invoiceDate: string;
  invoiceDateNp: string;
  dueDate: string | null;
  isTaxInvoice: boolean;
  currency: string;
  status: SalesInvoiceStatus;
  subtotalAmount: number;
  taxableAmount: number;
  exemptAmount: number;
  zeroRatedAmount: number;
  vatAmount: number;
  totalAmount: number;
  paidAmount: number;
  balanceAmount: number;
  notes: string | null;
  lines: SalesInvoiceLine[];
  version: number;
}

/** The printable IRD tax-invoice view (mandatory fields + QR payload). */
export interface TaxInvoiceLineView {
  lineNumber: number;
  itemCode: string;
  itemName: string;
  uomCode: string;
  quantity: number;
  unitPrice: number;
  discountPercentage: number;
  vatType: VatType;
  vatRate: number;
  lineAmount: number;
  vatAmount: number;
}

export interface TaxInvoiceView {
  documentTitle: string;
  sellerName: string;
  sellerPan: string;
  sellerAddress: string;
  buyerName: string;
  buyerPan: string;
  buyerAddress: string;
  invoiceNumber: string;
  invoiceDateAd: string;
  invoiceDateNp: string;
  status: SalesInvoiceStatus;
  currency: string;
  lines: TaxInvoiceLineView[];
  subtotal: number;
  vatAmount: number;
  total: number;
  qrPayload: string;
}

export interface InvoiceLineRequest {
  itemId: number;
  uomCode: string;
  quantity: number;
  unitPrice: number;
  discountPercentage?: number;
  vatType: VatType;
  vatRate?: number;
  description?: string;
}

export interface CreateInvoiceRequest {
  salesOrderId: number;
  deliveryId?: number;
  invoiceDate: string;
  dueDate?: string;
  notes?: string;
  lines: InvoiceLineRequest[];
}

export interface InvoiceDecisionRequest {
  reason: string;
}

export interface GetInvoicesParams {
  page?: number;
  size?: number;
  sort?: string;
}

const BASE_URL = "/sales/invoices";

export const salesInvoiceApi = api.injectEndpoints({
  endpoints: (build) => ({
    getInvoices: build.query<PageResponse<SalesInvoice>, GetInvoicesParams | void>({
      query: (params) => ({ url: BASE_URL, method: "GET", params: params ?? {} }),
      providesTags: (result) => [
        ...(result?.content.map((i) => ({ type: "SalesInvoice" as const, id: i.id })) ?? []),
        "SalesInvoice",
      ],
    }),

    getOutstandingInvoices: build.query<SalesInvoice[], void>({
      query: () => ({ url: `${BASE_URL}/outstanding`, method: "GET" }),
      providesTags: ["SalesInvoice"],
    }),

    getInvoicedOrderIds: build.query<number[], void>({
      query: () => ({ url: `${BASE_URL}/invoiced-order-ids`, method: "GET" }),
      providesTags: ["SalesInvoice"],
    }),

    getInvoice: build.query<SalesInvoice, number>({
      query: (id) => ({ url: `${BASE_URL}/${id}`, method: "GET" }),
      providesTags: (_r, _e, id) => [{ type: "SalesInvoice", id }, "SalesInvoice"],
    }),

    getTaxInvoiceView: build.query<TaxInvoiceView, number>({
      query: (id) => ({ url: `${BASE_URL}/${id}/tax-invoice`, method: "GET" }),
      providesTags: (_r, _e, id) => [{ type: "SalesInvoice", id }],
    }),

    createInvoice: build.mutation<SalesInvoice, CreateInvoiceRequest>({
      query: (body) => ({ url: BASE_URL, method: "POST", data: body }),
      invalidatesTags: ["SalesInvoice", "SalesOrder"],
    }),

    issueInvoice: build.mutation<SalesInvoice, number>({
      query: (id) => ({ url: `${BASE_URL}/${id}/issue`, method: "POST" }),
      invalidatesTags: (_r, _e, id) => [{ type: "SalesInvoice", id }, "SalesInvoice"],
    }),

    cancelInvoice: build.mutation<
      SalesInvoice,
      { id: number; data: InvoiceDecisionRequest }
    >({
      query: ({ id, data }) => ({ url: `${BASE_URL}/${id}/cancel`, method: "POST", data }),
      invalidatesTags: (_r, _e, { id }) => [{ type: "SalesInvoice", id }, "SalesInvoice"],
    }),
  }),
});

export const {
  useGetInvoicesQuery,
  useGetOutstandingInvoicesQuery,
  useGetInvoicedOrderIdsQuery,
  useGetInvoiceQuery,
  useGetTaxInvoiceViewQuery,
  useCreateInvoiceMutation,
  useIssueInvoiceMutation,
  useCancelInvoiceMutation,
} = salesInvoiceApi;
