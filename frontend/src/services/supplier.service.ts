import { api } from "./api";
import type { PageResponse } from "@/types/api";

export type SupplierStatus = "ACTIVE" | "INACTIVE";

/**
 * Mirrors the backend `SupplierType` enum. The backend deserialises this with
 * Jackson, so any value outside this union is rejected with a 400.
 */
export const SUPPLIER_TYPES = ["MANUFACTURER", "DISTRIBUTOR", "SERVICE", "OTHER"] as const;

export type SupplierType = (typeof SUPPLIER_TYPES)[number];

/** Human-readable labels for the supplier type enum, used by the UI selects. */
export const SUPPLIER_TYPE_LABELS: Record<SupplierType, string> = {
  MANUFACTURER: "Manufacturer",
  DISTRIBUTOR: "Distributor",
  SERVICE: "Service",
  OTHER: "Other",
};

export function formatSupplierType(type: string | null | undefined): string {
  if (!type) return "—";
  return SUPPLIER_TYPE_LABELS[type as SupplierType] ?? type;
}

/**
 * Projection of `SupplierResponse` used by the paginated list endpoint. The
 * backend returns the full record here too, so this type is intentionally a
 * structural subset that the list table relies on.
 */
export interface SupplierListResponse {
  id: number;
  tenantId: number;
  companyId: number;
  name: string;
  code: string;
  type: SupplierType | null;
  email: string | null;
  phone: string | null;
  address: string | null;
  taxId: string | null;
  paymentTermsDays: number | null;
  currency: string | null;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
  createdBy: string;
  updatedBy: string;
  version: number;
}

/** Mirrors the backend `SupplierResponse` record returned by the detail endpoints. */
export type Supplier = SupplierListResponse;

/** Mirrors the backend `SupplierCreateRequest` record. */
export interface CreateSupplierRequest {
  name: string;
  code: string;
  type?: SupplierType;
  email?: string;
  phone?: string;
  address?: string;
  taxId?: string;
  paymentTermsDays?: number;
  currency?: string;
}

/**
 * Mirrors the backend `SupplierUpdateRequest`. Every field is optional: an
 * omitted field is left untouched by the backend, so edits only send the
 * fields that actually changed.
 */
export interface UpdateSupplierRequest {
  name?: string;
  code?: string;
  type?: SupplierType;
  email?: string | null;
  phone?: string | null;
  address?: string | null;
  taxId?: string | null;
  paymentTermsDays?: number | null;
  currency?: string | null;
}

export interface GetSuppliersParams {
  page?: number;
  size?: number;
  sort?: string;
  search?: string;
  status?: boolean;
}

/**
 * Supplier API endpoints with RTK Query caching and invalidation.
 *
 * None of these endpoints take a `companyId`: the backend scopes every call to
 * the company in the `X-Company-Id` header, which the Axios request interceptor
 * attaches from the user's current company/fiscal-year selection.
 */
export const supplierApi = api.injectEndpoints({
  endpoints: (build) => ({
    getSuppliers: build.query<PageResponse<SupplierListResponse>, GetSuppliersParams | void>({
      query: (params) => ({
        url: "/inventory/suppliers",
        method: "GET",
        params: params ?? {},
      }),
      providesTags: ["Supplier"],
    }),

    getSupplier: build.query<Supplier, string>({
      query: (supplierId) => ({
        url: `/inventory/suppliers/${supplierId}`,
        method: "GET",
      }),
      providesTags: (_result, _error, supplierId) => [{ type: "Supplier", id: supplierId }],
    }),

    getSupplierByCode: build.query<Supplier, string>({
      query: (code) => ({
        url: "/inventory/suppliers/by-code",
        method: "GET",
        params: { code },
      }),
      providesTags: (_result, _error, code) => [{ type: "Supplier", id: `code-${code}` }],
    }),

    createSupplier: build.mutation<Supplier, CreateSupplierRequest>({
      query: (body) => ({
        url: "/inventory/suppliers",
        method: "POST",
        data: body,
      }),
      invalidatesTags: ["Supplier"],
    }),

    updateSupplier: build.mutation<Supplier, { supplierId: number; data: UpdateSupplierRequest }>({
      query: ({ supplierId, data }) => ({
        url: `/inventory/suppliers/${supplierId}`,
        method: "PUT",
        data,
      }),
      invalidatesTags: (_result, _error, { supplierId }) => [
        { type: "Supplier", id: supplierId.toString() },
        "Supplier",
      ],
    }),

    patchSupplier: build.mutation<Supplier, { supplierId: number; data: UpdateSupplierRequest }>({
      query: ({ supplierId, data }) => ({
        url: `/inventory/suppliers/${supplierId}`,
        method: "PATCH",
        data,
      }),
      invalidatesTags: (_result, _error, { supplierId }) => [
        { type: "Supplier", id: supplierId.toString() },
        "Supplier",
      ],
    }),

    activateSupplier: build.mutation<Supplier, number>({
      query: (supplierId) => ({
        url: `/inventory/suppliers/${supplierId}/activate`,
        method: "POST",
      }),
      invalidatesTags: (_result, _error, supplierId) => [
        { type: "Supplier", id: supplierId.toString() },
        "Supplier",
      ],
    }),

    deactivateSupplier: build.mutation<Supplier, number>({
      query: (supplierId) => ({
        url: `/inventory/suppliers/${supplierId}/deactivate`,
        method: "POST",
      }),
      invalidatesTags: (_result, _error, supplierId) => [
        { type: "Supplier", id: supplierId.toString() },
        "Supplier",
      ],
    }),

    deleteSupplier: build.mutation<void, number>({
      query: (supplierId) => ({
        url: `/inventory/suppliers/${supplierId}`,
        method: "DELETE",
      }),
      invalidatesTags: ["Supplier"],
    }),
  }),
});

export const {
  useGetSuppliersQuery,
  useGetSupplierQuery,
  useGetSupplierByCodeQuery,
  useCreateSupplierMutation,
  useUpdateSupplierMutation,
  usePatchSupplierMutation,
  useActivateSupplierMutation,
  useDeactivateSupplierMutation,
  useDeleteSupplierMutation,
} = supplierApi;
