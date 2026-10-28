import { api } from "./api";
import type { PageResponse } from "@/types/api";

export type UnitStatus = "ACTIVE" | "INACTIVE";

/**
 * Mirrors the backend `UnitDimension` enum. Jackson rejects any value outside
 * this union with a 400, so the union is the contract, not a convenience.
 */
export const UNIT_DIMENSIONS = ["MASS", "VOLUME", "LENGTH", "AREA", "COUNT", "TIME"] as const;

export type UnitDimension = (typeof UNIT_DIMENSIONS)[number];

/** Human-readable labels used by the dimension selects and table cells. */
export const UNIT_DIMENSION_LABELS: Record<UnitDimension, string> = {
  MASS: "Mass",
  VOLUME: "Volume",
  LENGTH: "Length",
  AREA: "Area",
  COUNT: "Count",
  TIME: "Time",
};

export function formatUnitDimension(dimension: string | null | undefined): string {
  if (!dimension) return "—";
  return UNIT_DIMENSION_LABELS[dimension as UnitDimension] ?? dimension;
}

/** Mirrors the backend `UnitResponse` record; the list endpoint returns the same shape. */
export interface Unit {
  id: number;
  tenantId: number;
  companyId: number;
  name: string;
  code: string;
  dimension: UnitDimension;
  symbol: string | null;
  decimalScale: number;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
  createdBy: string;
  updatedBy: string;
  version: number;
}

/** Mirrors the backend `UnitCreateRequest` record. */
export interface CreateUnitRequest {
  name: string;
  code: string;
  dimension: UnitDimension;
  symbol?: string;
  decimalScale?: number;
}

/**
 * Mirrors the backend `UnitUpdateRequest`.
 *
 * `symbol` accepts null to clear it; the other fields are only sent when the
 * user actually changed them, matching the backend's partial-update semantics.
 */
export interface UpdateUnitRequest {
  name?: string;
  code?: string;
  dimension?: UnitDimension;
  symbol?: string | null;
  decimalScale?: number | null;
}

export interface GetUnitsParams {
  page?: number;
  size?: number;
  sort?: string;
  search?: string;
}

/**
 * Unit API endpoints with RTK Query caching and invalidation.
 *
 * None of these endpoints take a `companyId`: the backend scopes every call to
 * the company in the `X-Company-Id` header, which the Axios request interceptor
 * attaches from the user's current company/fiscal-year selection.
 */
export const unitApi = api.injectEndpoints({
  endpoints: (build) => ({
    getUnits: build.query<PageResponse<Unit>, GetUnitsParams | void>({
      query: (params) => ({
        url: "/inventory/units",
        method: "GET",
        params: params ?? {},
      }),
      providesTags: ["Unit"],
    }),

    getUnit: build.query<Unit, string>({
      query: (unitId) => ({
        url: `/inventory/units/${unitId}`,
        method: "GET",
      }),
      providesTags: (_result, _error, unitId) => [{ type: "Unit", id: unitId }],
    }),

    getUnitByCode: build.query<Unit, string>({
      query: (code) => ({
        url: "/inventory/units/by-code",
        method: "GET",
        params: { code },
      }),
      providesTags: (_result, _error, code) => [{ type: "Unit", id: `code-${code}` }],
    }),

    getActiveUnits: build.query<Unit[], void>({
      query: () => ({
        url: "/inventory/units/active",
        method: "GET",
      }),
      providesTags: ["Unit"],
    }),

    createUnit: build.mutation<Unit, CreateUnitRequest>({
      query: (body) => ({
        url: "/inventory/units",
        method: "POST",
        data: body,
      }),
      invalidatesTags: ["Unit"],
    }),

    updateUnit: build.mutation<Unit, { unitId: number; data: UpdateUnitRequest }>({
      query: ({ unitId, data }) => ({
        url: `/inventory/units/${unitId}`,
        method: "PUT",
        data,
      }),
      invalidatesTags: (_result, _error, { unitId }) => [
        { type: "Unit", id: unitId.toString() },
        "Unit",
      ],
    }),

    patchUnit: build.mutation<Unit, { unitId: number; data: UpdateUnitRequest }>({
      query: ({ unitId, data }) => ({
        url: `/inventory/units/${unitId}`,
        method: "PATCH",
        data,
      }),
      invalidatesTags: (_result, _error, { unitId }) => [
        { type: "Unit", id: unitId.toString() },
        "Unit",
      ],
    }),

    activateUnit: build.mutation<Unit, number>({
      query: (unitId) => ({
        url: `/inventory/units/${unitId}/activate`,
        method: "POST",
      }),
      invalidatesTags: (_result, _error, unitId) => [
        { type: "Unit", id: unitId.toString() },
        "Unit",
      ],
    }),

    deactivateUnit: build.mutation<Unit, number>({
      query: (unitId) => ({
        url: `/inventory/units/${unitId}/deactivate`,
        method: "POST",
      }),
      invalidatesTags: (_result, _error, unitId) => [
        { type: "Unit", id: unitId.toString() },
        "Unit",
      ],
    }),

    deleteUnit: build.mutation<void, number>({
      query: (unitId) => ({
        url: `/inventory/units/${unitId}`,
        method: "DELETE",
      }),
      invalidatesTags: ["Unit"],
    }),
  }),
});

export const {
  useGetUnitsQuery,
  useGetUnitQuery,
  useGetUnitByCodeQuery,
  useGetActiveUnitsQuery,
  useCreateUnitMutation,
  useUpdateUnitMutation,
  usePatchUnitMutation,
  useActivateUnitMutation,
  useDeactivateUnitMutation,
  useDeleteUnitMutation,
} = unitApi;
