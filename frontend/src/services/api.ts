import { createApi } from "@reduxjs/toolkit/query/react";
import type { BaseQueryFn } from "@reduxjs/toolkit/query";
import type { AxiosError, AxiosRequestConfig } from "axios";
import { axiosInstance } from "./http-client";
import { setupInterceptors } from "./interceptors";

// Attach the JWT request/response interceptors (token injection + refresh)
// to the shared Axios instance exactly once, at module load.
setupInterceptors();

/**
 * API error type returned by the custom base query.
 *
 * Mirrors the backend `ApiError` record from `GlobalExceptionHandler`:
 * - `code`      machine-readable error code (e.g. "INVALID_CREDENTIALS")
 * - `message`   human-readable error message
 * - `path`      the request path that produced the error
 * - `timestamp` occurrence time (ISO-8601)
 */
export interface ApiError {
  status?: number;
  data?: {
    code?: string;
    message?: string;
    path?: string;
    timestamp?: string;
  };
  error: string;
}

/**
 * Custom base query that wraps Axios for RTK Query.
 *
 * This gives us full control over request/response handling while still
 * leveraging RTK Query's caching, polling, and optimistic updates. The
 * underlying Axios instance carries the auth interceptors, so every request
 * is automatically authenticated and transparently refreshed on expiry.
 */
const axiosBaseQuery =
  (): BaseQueryFn<
    {
      url: string;
      method: AxiosRequestConfig["method"];
      data?: unknown;
      params?: unknown;
      headers?: AxiosRequestConfig["headers"];
    },
    unknown,
    ApiError
  > =>
  async ({ url, method, data, params, headers }) => {
    try {
      const response = await axiosInstance({
        url,
        method,
        data,
        params,
        headers,
      });

      return { data: response.data };
    } catch (axiosError) {
      const err = axiosError as AxiosError<{ message?: string }>;
      return {
        error: {
          status: err.response?.status,
          data: err.response?.data,
          error: err.message,
        },
      };
    }
  };

/**
 * RTK Query API definition.
 *
 * All API endpoints are defined here as tags. Each service module can extend
 * this API with its own endpoints.
 */
export const api = createApi({
  reducerPath: "api",
  baseQuery: axiosBaseQuery(),
  // Retain query results after the last subscriber unmounts, so navigating
  // back to a page renders cached data instantly instead of re-fetching.
  // 5 minutes comfortably covers back/forward navigation within a session.
  keepUnusedDataFor: 300,
  // Do NOT re-fetch cached queries on every mount / arg change.
  //
  // The previous value (30) re-fetched any query whose cache was older
  // than 30 seconds whenever a component mounted. The app shell
  // (AppLayout + sidebar) subscribes to the companies, fiscal-years and
  // procurement-menu queries on every protected page, so each navigation
  // fired 3 background re-fetches before the page's own queries even
  // started — the dominant cause of sluggish navigation.
  //
  // With `false`, navigation reuses the cached data instantly. Freshness
  // is still guaranteed by:
  // - tag invalidation (mutations re-fetch the lists they affect),
  // - `refetchOnReconnect` (wired via `setupListeners` in Providers),
  // - `api.util.resetApiState()` on logout (clears the cache so the
  //   next login fetches fresh data instead of showing the previous
  //   user's cached responses),
  // - manual `refetch()` on error states ("Try again" buttons).
  refetchOnMountOrArgChange: false,
  refetchOnReconnect: true,
  tagTypes: [
    "Auth",
    "User",
    "Company",
    "CompanyFiscalYear",
    "Role",
    "Permission",
    "Department",
    "Branch",
    "Tenant",
    "Finance",
    "Inventory",
    "Category",
    "Supplier",
    "Unit",
    "Item",
    "Warehouse",
    "PurchaseRequisition",
    "PurchaseOrder",
    "GoodsReceipt",
    "PurchaseInvoice",
    "SupplierPayment",
    "Customer",
    "SalesOrder",
    "SalesDelivery",
    "SalesInvoice",
    "CustomerReceipt",
    "SalesReturn",
    "SalesRegister",
    "SalesMenu",
    "ProcurementMenu",
    "WorkflowConfiguration",
    "HR",
    "Manufacturing",
    "AI",
    "Province",
    "District",
    "LocalLevel",
  ],
  endpoints: () => ({}),
});
