import { api } from "./api";

/** Mirrors the backend `PosCbmsSyncResult` record. */
export interface PosCbmsSyncResult {
  success: boolean;
  statusCode: number;
  message: string;
  responseBody: string | null;
}

/** Mirrors the backend CBMS status gauge. */
export interface CbmsStatus {
  pendingTransactions: number;
  enabled: boolean;
}

/**
 * CBMS (Nepal IRD) operations for POS.
 *
 * The routine path is the server's scheduled outbox worker; these are the
 * operator controls — a manual push for a stuck bill and a depth gauge for
 * monitoring how many completed sales are still waiting to sync.
 */
export const posCbmsApi = api.injectEndpoints({
  endpoints: (build) => ({
    /** Manually push one completed transaction to CBMS. */
    syncPosTransaction: build.mutation<PosCbmsSyncResult, number>({
      query: (id) => ({ url: `/pos/cbms/transactions/${id}/sync`, method: "POST" }),
      invalidatesTags: ["PosTransaction"],
    }),

    /** Pending outbox depth and whether CBMS is enabled. */
    getCbmsStatus: build.query<CbmsStatus, void>({
      query: () => ({ url: "/pos/cbms/status", method: "GET" }),
      providesTags: ["PosTransaction"],
    }),
  }),
});

export const { useSyncPosTransactionMutation, useGetCbmsStatusQuery } = posCbmsApi;
