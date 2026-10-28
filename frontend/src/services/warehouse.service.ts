import { api } from "./api";
import type { PageResponse } from "@/types/api";

/**
 * Mirrors the backend `WarehouseType` enum. The backend deserialises this with
 * Jackson, so any value outside this union is rejected with a 400.
 */
export const WAREHOUSE_TYPES = [
  "GENERAL",
  "COLD_STORAGE",
  "TRANSIT",
  "QUARANTINE",
  "RETURNS",
  "SCRAP",
] as const;

export type WarehouseType = (typeof WAREHOUSE_TYPES)[number];

/** Human-readable labels for the warehouse type enum, used by the UI selects. */
export const WAREHOUSE_TYPE_LABELS: Record<WarehouseType, string> = {
  GENERAL: "General",
  COLD_STORAGE: "Cold Storage",
  TRANSIT: "In Transit",
  QUARANTINE: "Quarantine",
  RETURNS: "Returns",
  SCRAP: "Scrap",
};

/**
 * Storage kinds that must never be offered as a goods-receipt destination.
 *
 * `TRANSIT` holds goods the business has not taken ownership of yet and
 * `QUARANTINE` holds goods deliberately withheld from use, so receiving *into*
 * either is a category error rather than a mistake the user can be warned out
 * of. `SCRAP` is excluded because stock posted there is written off, not held.
 */
export const NON_RECEIVING_WAREHOUSE_TYPES: readonly WarehouseType[] = [
  "TRANSIT",
  "QUARANTINE",
  "SCRAP",
];

export function formatWarehouseType(type: string | null | undefined): string {
  if (!type) return "—";
  return WAREHOUSE_TYPE_LABELS[type as WarehouseType] ?? type;
}

/** Mirrors the backend `WarehouseResponse` record. */
export interface Warehouse {
  id: number;
  tenantId: number;
  companyId: number;
  code: string;
  name: string;
  type: WarehouseType | null;
  /** The administrative branch this warehouse sits under, or null if company-wide. */
  branchId: number | null;
  address: string | null;
  phone: string | null;
  isActive: boolean;
  /** True for the company's single fallback warehouse. */
  isDefault: boolean;
  createdAt: string;
  updatedAt: string;
  createdBy: string;
  updatedBy: string;
  version: number;
}

/**
 * Mirrors the backend `CreateWarehouseRequest`.
 *
 * There is deliberately no `isDefault`: adding a second warehouse adds to the
 * network, it does not repoint where goods land. Promotion is the separate
 * `setDefaultWarehouse` mutation.
 */
export interface CreateWarehouseRequest {
  code: string;
  name: string;
  type: WarehouseType;
  branchId?: number | null;
  address?: string;
  phone?: string;
}

/**
 * Mirrors the backend `UpdateWarehouseRequest`.
 *
 * `branchId` is absent because the backend's partial update cannot distinguish
 * "leave the branch alone" from "clear the branch", and guessing wrong would
 * detach a warehouse from the branch whose approval chain governs it.
 */
export interface UpdateWarehouseRequest {
  code?: string;
  name?: string;
  type?: WarehouseType;
  address?: string | null;
  phone?: string | null;
}

/**
 * Query parameters for the warehouse list endpoint.
 *
 * **Only `page`, `size` and `sort` reach the server** - the backend signature is
 * `listWarehouses(Pageable pageable)`, so Spring binds the pageable and discards
 * anything else without complaint. Search and status filtering therefore have to
 * happen on the returned page in the caller, exactly as they do for suppliers.
 */
export interface GetWarehousesParams {
  page?: number;
  size?: number;
  sort?: string;
}

export interface GetDefaultWarehouseParams {
  /**
   * Optional branch to attach the warehouse to when one is minted on first call.
   * Ignored once the company already has a default.
   */
  branchId?: number;
}

/**
 * Warehouse API endpoints with RTK Query caching and invalidation.
 *
 * None of these endpoints take a `companyId`: the backend scopes every call to
 * the company in the `X-Company-Id` header, which the Axios request interceptor
 * attaches from the user's current company/fiscal-year selection.
 *
 * There is no delete mutation. A warehouse that has ever received stock cannot be
 * removed without orphaning every movement posted against it; retiring it is what
 * `deactivateWarehouse` is for.
 */
export const warehouseApi = api.injectEndpoints({
  endpoints: (build) => ({
    getWarehouses: build.query<PageResponse<Warehouse>, GetWarehousesParams | void>({
      query: (params) => ({
        url: "/inventory/warehouses",
        method: "GET",
        params: params ?? {},
      }),
      providesTags: ["Warehouse"],
    }),

    /** Every active warehouse, unpaginated - the shape pickers want. */
    getActiveWarehouses: build.query<Warehouse[], void>({
      query: () => ({
        url: "/inventory/warehouses/active",
        method: "GET",
      }),
      providesTags: ["Warehouse"],
    }),

    getWarehousesByBranch: build.query<Warehouse[], number>({
      query: (branchId) => ({
        url: `/inventory/warehouses/by-branch/${branchId}`,
        method: "GET",
      }),
      providesTags: ["Warehouse"],
    }),

    getWarehouse: build.query<Warehouse, number>({
      query: (warehouseId) => ({
        url: `/inventory/warehouses/${warehouseId}`,
        method: "GET",
      }),
      providesTags: (_result, _error, warehouseId) => [
        { type: "Warehouse", id: warehouseId },
      ],
    }),

    getWarehouseByCode: build.query<Warehouse, string>({
      query: (code) => ({
        url: "/inventory/warehouses/by-code",
        method: "GET",
        params: { code },
      }),
      providesTags: (_result, _error, code) => [
        { type: "Warehouse", id: `code-${code}` },
      ],
    }),

    /**
     * Reads the company's default warehouse, creating one on first call.
     *
     * A GET that can write is unusual, and is done here so a brand-new company
     * can receive goods without anyone configuring a warehouse first. The backend
     * call is idempotent, but it still writes, so callers that must not trigger
     * creation should pass `skip` and handle the empty case.
     */
    getDefaultWarehouse: build.query<
      Warehouse,
      GetDefaultWarehouseParams | void
    >({
      query: (params) => ({
        url: "/inventory/warehouses/default",
        method: "GET",
        params: params ?? {},
      }),
      providesTags: ["Warehouse"],
    }),

    createWarehouse: build.mutation<Warehouse, CreateWarehouseRequest>({
      query: (body) => ({
        url: "/inventory/warehouses",
        method: "POST",
        data: body,
      }),
      invalidatesTags: ["Warehouse"],
    }),

    updateWarehouse: build.mutation<
      Warehouse,
      { warehouseId: number; data: UpdateWarehouseRequest }
    >({
      query: ({ warehouseId, data }) => ({
        url: `/inventory/warehouses/${warehouseId}`,
        method: "PUT",
        data,
      }),
      invalidatesTags: (_result, _error, { warehouseId }) => [
        { type: "Warehouse", id: warehouseId },
        "Warehouse",
      ],
    }),

    patchWarehouse: build.mutation<
      Warehouse,
      { warehouseId: number; data: UpdateWarehouseRequest }
    >({
      query: ({ warehouseId, data }) => ({
        url: `/inventory/warehouses/${warehouseId}`,
        method: "PATCH",
        data,
      }),
      invalidatesTags: (_result, _error, { warehouseId }) => [
        { type: "Warehouse", id: warehouseId },
        "Warehouse",
      ],
    }),

    /** Promotes a warehouse to be the company default, demoting the previous one. */
    setDefaultWarehouse: build.mutation<Warehouse, number>({
      query: (warehouseId) => ({
        url: `/inventory/warehouses/${warehouseId}/default`,
        method: "POST",
      }),
      invalidatesTags: (_result, _error, warehouseId) => [
        { type: "Warehouse", id: warehouseId },
        "Warehouse",
      ],
    }),

    activateWarehouse: build.mutation<Warehouse, number>({
      query: (warehouseId) => ({
        url: `/inventory/warehouses/${warehouseId}/activate`,
        method: "POST",
      }),
      invalidatesTags: (_result, _error, warehouseId) => [
        { type: "Warehouse", id: warehouseId },
        "Warehouse",
      ],
    }),

    deactivateWarehouse: build.mutation<Warehouse, number>({
      query: (warehouseId) => ({
        url: `/inventory/warehouses/${warehouseId}/deactivate`,
        method: "POST",
      }),
      invalidatesTags: (_result, _error, warehouseId) => [
        { type: "Warehouse", id: warehouseId },
        "Warehouse",
      ],
    }),
  }),
});

export const {
  useGetWarehousesQuery,
  useGetActiveWarehousesQuery,
  useGetWarehousesByBranchQuery,
  useGetWarehouseQuery,
  useGetWarehouseByCodeQuery,
  useGetDefaultWarehouseQuery,
  useCreateWarehouseMutation,
  useUpdateWarehouseMutation,
  usePatchWarehouseMutation,
  useSetDefaultWarehouseMutation,
  useActivateWarehouseMutation,
  useDeactivateWarehouseMutation,
} = warehouseApi;
