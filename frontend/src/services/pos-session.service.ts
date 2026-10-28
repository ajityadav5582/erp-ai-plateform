import { api } from "./api";
import type { PageResponse } from "@/types/api";

/** Mirrors the backend `PosSessionStatus` enum. */
export const POS_SESSION_STATUSES = ["OPEN", "CLOSED"] as const;
export type PosSessionStatus = (typeof POS_SESSION_STATUSES)[number];

export const POS_SESSION_STATUS_LABELS: Record<PosSessionStatus, string> = {
  OPEN: "Open",
  CLOSED: "Closed",
};

/** Mirrors the backend `PosSessionResponse` record. */
export interface PosSession {
  id: number;
  tenantId: number;
  companyId: number;
  sessionNumber: string;
  registerId: number;
  registerCode: string;
  registerName: string;
  fiscalYearId: number | null;
  cashierId: string;
  cashierName: string;
  openedAt: string;
  closedAt: string | null;
  openingCashAmount: number;
  closingCashAmount: number | null;
  expectedCash: number | null;
  cashVariance: number | null;
  totalTransactions: number;
  totalVoided: number;
  totalCashSales: number;
  totalCardSales: number;
  totalWalletSales: number;
  totalOtherSales: number;
  totalVatCollected: number;
  grossSales: number;
  status: PosSessionStatus;
  notes: string | null;
  closeNotes: string | null;
  version: number;
}

export interface OpenSessionRequest {
  registerId: number;
  openingCashAmount: number;
  notes?: string;
}

export interface CloseSessionRequest {
  closingCashAmount: number;
  closeNotes?: string;
}

export interface GetPosSessionsParams {
  page?: number;
  size?: number;
  sort?: string;
  status?: PosSessionStatus;
  registerId?: number;
  term?: string;
}

const BASE_URL = "/pos/sessions";

export const posSessionApi = api.injectEndpoints({
  endpoints: (build) => ({
    getPosSessions: build.query<PageResponse<PosSession>, GetPosSessionsParams | void>({
      query: (params) => ({ url: BASE_URL, method: "GET", params: params ?? {} }),
      providesTags: (result) => [
        ...(result?.content.map((s) => ({ type: "PosSession" as const, id: s.id })) ?? []),
        "PosSession",
      ],
    }),

    getPosSession: build.query<PosSession, number>({
      query: (id) => ({ url: `${BASE_URL}/${id}`, method: "GET" }),
      providesTags: (_r, _e, id) => [{ type: "PosSession", id }, "PosSession"],
    }),

    /** The current cashier's open sessions — the terminal's session picker source. */
    getMyOpenPosSessions: build.query<PosSession[], void>({
      query: () => ({ url: `${BASE_URL}/my-open`, method: "GET" }),
      providesTags: ["PosSession"],
    }),

    /** The open session on one register, or 204 when the register is idle. */
    getOpenPosSessionForRegister: build.query<PosSession | null, number>({
      query: (registerId) => ({
        url: `${BASE_URL}/open-for-register`,
        method: "GET",
        params: { registerId },
      }),
      providesTags: ["PosSession"],
    }),

    openPosSession: build.mutation<PosSession, OpenSessionRequest>({
      query: (body) => ({ url: BASE_URL, method: "POST", data: body }),
      invalidatesTags: ["PosSession", "PosRegister"],
    }),

    closePosSession: build.mutation<PosSession, { id: number; body: CloseSessionRequest }>({
      query: ({ id, body }) => ({ url: `${BASE_URL}/${id}/close`, method: "POST", data: body }),
      invalidatesTags: (_r, _e, { id }) => [{ type: "PosSession", id }, "PosSession", "PosRegister"],
    }),
  }),
});

export const {
  useGetPosSessionsQuery,
  useGetPosSessionQuery,
  useGetMyOpenPosSessionsQuery,
  useGetOpenPosSessionForRegisterQuery,
  useOpenPosSessionMutation,
  useClosePosSessionMutation,
} = posSessionApi;
