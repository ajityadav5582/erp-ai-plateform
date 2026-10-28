import { api } from "./api";
import type { PageResponse } from "@/types/api";

/** Mirrors the backend `CustomerType` enum. */
export const CUSTOMER_TYPES = [
  "RETAIL",
  "WHOLESALE",
  "EXPORT",
  "GOVERNMENT",
] as const;
export type CustomerType = (typeof CUSTOMER_TYPES)[number];

export const CUSTOMER_TYPE_LABELS: Record<CustomerType, string> = {
  RETAIL: "Retail",
  WHOLESALE: "Wholesale",
  EXPORT: "Export",
  GOVERNMENT: "Government",
};

/** Mirrors the backend `CustomerResponse` record. */
export interface Customer {
  id: number;
  tenantId: number;
  companyId: number;
  name: string;
  code: string;
  type: CustomerType;
  email: string | null;
  phone: string | null;
  address: string | null;
  panNumber: string | null;
  vatRegistered: boolean;
  creditLimit: number | null;
  paymentTermsDays: number | null;
  currency: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

/** Mirrors the backend `CreateCustomerRequest` record. */
export interface CreateCustomerRequest {
  name: string;
  code: string;
  type: CustomerType;
  email?: string;
  phone?: string;
  address?: string;
  panNumber?: string;
  vatRegistered: boolean;
  creditLimit?: number;
  paymentTermsDays?: number;
  currency?: string;
}

/** Mirrors the backend `UpdateCustomerRequest` record. */
export interface UpdateCustomerRequest {
  name?: string;
  code?: string;
  type?: CustomerType;
  email?: string;
  phone?: string;
  address?: string;
  panNumber?: string;
  vatRegistered?: boolean;
  creditLimit?: number;
  paymentTermsDays?: number;
  currency?: string;
}

export interface GetCustomersParams {
  page?: number;
  size?: number;
  sort?: string;
}

const BASE_URL = "/sales/customers";

/**
 * Customer master API. All reads are company-scoped server-side; the tenant and
 * company come from the authenticated request, never the payload.
 */
export const customerApi = api.injectEndpoints({
  endpoints: (build) => ({
    getCustomers: build.query<PageResponse<Customer>, GetCustomersParams | void>({
      query: (params) => ({ url: BASE_URL, method: "GET", params: params ?? {} }),
      providesTags: (result) => [
        ...(result?.content.map((c) => ({ type: "Customer" as const, id: c.id })) ?? []),
        "Customer",
      ],
    }),

    searchCustomers: build.query<
      PageResponse<Customer>,
      { name: string; page?: number; size?: number }
    >({
      query: ({ name, page = 0, size = 20 }) => ({
        url: `${BASE_URL}/search`,
        method: "GET",
        params: { name, page, size },
      }),
      providesTags: ["Customer"],
    }),

    getActiveCustomers: build.query<Customer[], void>({
      query: () => ({ url: `${BASE_URL}/active`, method: "GET" }),
      providesTags: ["Customer"],
    }),

    getCustomer: build.query<Customer, number>({
      query: (id) => ({ url: `${BASE_URL}/${id}`, method: "GET" }),
      providesTags: (_r, _e, id) => [{ type: "Customer", id }, "Customer"],
    }),

    createCustomer: build.mutation<Customer, CreateCustomerRequest>({
      query: (body) => ({ url: BASE_URL, method: "POST", data: body }),
      invalidatesTags: ["Customer"],
    }),

    updateCustomer: build.mutation<
      Customer,
      { id: number; data: UpdateCustomerRequest }
    >({
      query: ({ id, data }) => ({ url: `${BASE_URL}/${id}`, method: "PUT", data }),
      invalidatesTags: (_r, _e, { id }) => [{ type: "Customer", id }, "Customer"],
    }),

    activateCustomer: build.mutation<Customer, number>({
      query: (id) => ({ url: `${BASE_URL}/${id}/activate`, method: "PATCH" }),
      invalidatesTags: (_r, _e, id) => [{ type: "Customer", id }, "Customer"],
    }),

    deactivateCustomer: build.mutation<Customer, number>({
      query: (id) => ({ url: `${BASE_URL}/${id}/deactivate`, method: "PATCH" }),
      invalidatesTags: (_r, _e, id) => [{ type: "Customer", id }, "Customer"],
    }),

    deleteCustomer: build.mutation<void, number>({
      query: (id) => ({ url: `${BASE_URL}/${id}`, method: "DELETE" }),
      invalidatesTags: (_r, _e, id) => [{ type: "Customer", id }, "Customer"],
    }),
  }),
});

export const {
  useGetCustomersQuery,
  useSearchCustomersQuery,
  useGetActiveCustomersQuery,
  useGetCustomerQuery,
  useCreateCustomerMutation,
  useUpdateCustomerMutation,
  useActivateCustomerMutation,
  useDeactivateCustomerMutation,
  useDeleteCustomerMutation,
} = customerApi;
