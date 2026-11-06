import { api } from "./api";
import type { ApiError } from "./api";

export interface AuditorBill {
  billNumber: string;
  billDate: string;
  buyerName: string;
  buyerPan: string;
  total: number;
  taxable: number;
  vat: number;
  status: string;
  gap: boolean;
}

export interface AuditorVoid {
  creditNoteNo: string;
  billDate: string;
  originalInvoice: string;
  buyerName: string;
  buyerPan: string;
  returnAmount: number;
  taxable: number;
  vat: number;
  reason: string;
  cancelledAt: string;
}

export interface AuditorAuditLog {
  id: number;
  createdAt: string;
  userName: string;
  actionType: string;
  entityName: string;
  entityId: number;
  details: string;
  ipAddress: string;
}

export interface AuditorCbmsLog {
  invoiceNumber: string;
  responseCode: string;
  responseBody: string;
  saleTime: string;
  syncTime: string;
  status: string;
}

export const posAuditorApi = api.injectEndpoints({
  endpoints: (build) => ({
    getAuditorBills: build.query<{ bills: AuditorBill[]; gaps: number[] }, { companyId: number; from?: string; to?: string }>(
      {
        query: ({ companyId, from, to }) => ({ url: `/pos/auditor/bills`, method: "GET", params: { companyId, from, to } }),
      },
    ),
    getAuditorVoids: build.query<AuditorVoid[], { companyId: number; from?: string; to?: string }>(
      {
        query: ({ companyId, from, to }) => ({ url: `/pos/auditor/voids`, method: "GET", params: { companyId, from, to } }),
      },
    ),
    getAuditorAuditTrail: build.query<AuditorAuditLog[], { companyId: number; from?: string; to?: string }>(
      {
        query: ({ companyId, from, to }) => ({ url: `/pos/auditor/audit-trail`, method: "GET", params: { companyId, from, to } }),
      },
    ),
    getAuditorCbmsLogs: build.query<AuditorCbmsLog[], { companyId: number; from?: string; to?: string }>(
      {
        query: ({ companyId, from, to }) => ({ url: `/pos/auditor/cbms-logs`, method: "GET", params: { companyId, from, to } }),
      },
    ),
    downloadDossier: build.mutation<Blob, { companyId: number; from?: string; to?: string }>(
      {
        queryFn: async ({ companyId, from, to }) => {
          try {
            const response = await fetch(`/api/v1/pos/auditor/dossier?companyId=${companyId}${from ? `&from=${from}` : ``}${to ? `&to=${to}` : ``}`, {
              headers: { Authorization: `Bearer ${localStorage.getItem("token")}` },
            });
            if (!response.ok) return { error: { status: response.status, error: "Failed to download dossier" } as ApiError };
            const blob = await response.blob();
            return { data: blob };
          } catch (error) {
            return { error: { error: String(error) } as ApiError };
          }
        },
      },
    ),
  }),
});

export const {
  useGetAuditorBillsQuery,
  useGetAuditorVoidsQuery,
  useGetAuditorAuditTrailQuery,
  useGetAuditorCbmsLogsQuery,
  useDownloadDossierMutation,
} = posAuditorApi;
