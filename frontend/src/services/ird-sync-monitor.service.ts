import { api } from "./api";

export interface SyncStatusDto {
  syncedCount: number;
  simulatedCount: number;
  pendingCount: number;
  failedCount: number;
  deadLetterCount: number;
  lastSyncedAt: string | null;
}

export interface IrdSyncFailure {
  id: string;
  tenantId: number;
  companyId: number;
  sourceModule: string;
  sourceTransactionId: number;
  invoiceNumber: string;
  invoiceType: string;
  syncStatus: string;
  retryCount: number;
  maxRetries: number;
  nextRetryAt: string;
  lastAttemptedAt: string;
  lastResponseCode: number | null;
  lastErrorMessage: string;
}

export const irdSyncMonitorApi = api.injectEndpoints({
  endpoints: (build) => ({
    getSyncStatus: build.query<SyncStatusDto, number>({
      query: (companyId) => ({ url: `/integration/ird/companies/${companyId}/sync-status`, method: "GET" }),
    }),
    getFailures: build.query<IrdSyncFailure[], number>({
      query: (companyId) => ({ url: `/integration/ird/companies/${companyId}/failures`, method: "GET" }),
    }),
    retryAllFailed: build.mutation<void, number>({
      query: (companyId) => ({ url: `/integration/ird/companies/${companyId}/retry-all-failed`, method: "POST" }),
    }),
  }),
});

export const { useGetSyncStatusQuery, useGetFailuresQuery, useRetryAllFailedMutation } = irdSyncMonitorApi;
