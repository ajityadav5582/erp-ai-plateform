import { api } from "./api";
import type { PageResponse } from "@/types/api";

/** Mirrors the backend `PosRegisterStatus` enum. */
export const POS_REGISTER_STATUSES = ["ACTIVE", "INACTIVE"] as const;
export type PosRegisterStatus = (typeof POS_REGISTER_STATUSES)[number];

export const POS_REGISTER_STATUS_LABELS: Record<PosRegisterStatus, string> = {
  ACTIVE: "Active",
  INACTIVE: "Inactive",
};

/** Mirrors the backend `PosRegisterResponse` record. */
export interface PosRegister {
  id: number;
  tenantId: number;
  companyId: number;
  registerCode: string;
  registerName: string;
  branchId: number | null;
  branchName: string | null;
  warehouseId: number;
  warehouseCode: string;
  warehouseName: string;
  status: PosRegisterStatus;
  currentSessionId: number | null;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface CreatePosRegisterRequest {
  registerCode: string;
  registerName: string;
  warehouseId: number;
  warehouseCode: string;
  warehouseName: string;
  branchId?: number;
  branchName?: string;
  notes?: string;
}

export interface UpdatePosRegisterRequest {
  registerName?: string;
  branchId?: number | null;
  branchName?: string | null;
  notes?: string | null;
}

export interface GetPosRegistersParams {
  page?: number;
  size?: number;
  sort?: string;
  status?: PosRegisterStatus;
  term?: string;
}

const BASE_URL = "/pos/registers";

export const posRegisterApi = api.injectEndpoints({
  endpoints: (build) => ({
    getPosRegisters: build.query<PageResponse<PosRegister>, GetPosRegistersParams | void>({
      query: (params) => ({ url: BASE_URL, method: "GET", params: params ?? {} }),
      providesTags: (result) => [
        ...(result?.content.map((r) => ({ type: "PosRegister" as const, id: r.id })) ?? []),
        "PosRegister",
      ],
    }),

    getPosRegister: build.query<PosRegister, number>({
      query: (id) => ({ url: `${BASE_URL}/${id}`, method: "GET" }),
      providesTags: (_r, _e, id) => [{ type: "PosRegister", id }, "PosRegister"],
    }),

    createPosRegister: build.mutation<PosRegister, CreatePosRegisterRequest>({
      query: (body) => ({ url: BASE_URL, method: "POST", data: body }),
      invalidatesTags: ["PosRegister"],
    }),

    updatePosRegister: build.mutation<PosRegister, { id: number; body: UpdatePosRegisterRequest }>({
      query: ({ id, body }) => ({ url: `${BASE_URL}/${id}`, method: "PUT", data: body }),
      invalidatesTags: (_r, _e, { id }) => [{ type: "PosRegister", id }, "PosRegister"],
    }),

    activatePosRegister: build.mutation<PosRegister, number>({
      query: (id) => ({ url: `${BASE_URL}/${id}/activate`, method: "POST" }),
      invalidatesTags: (_r, _e, id) => [{ type: "PosRegister", id }, "PosRegister"],
    }),

    deactivatePosRegister: build.mutation<PosRegister, number>({
      query: (id) => ({ url: `${BASE_URL}/${id}/deactivate`, method: "POST" }),
      invalidatesTags: (_r, _e, id) => [{ type: "PosRegister", id }, "PosRegister"],
    }),
  }),
});

export const {
  useGetPosRegistersQuery,
  useGetPosRegisterQuery,
  useCreatePosRegisterMutation,
  useUpdatePosRegisterMutation,
  useActivatePosRegisterMutation,
  useDeactivatePosRegisterMutation,
} = posRegisterApi;
