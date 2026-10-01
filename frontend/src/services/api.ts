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
    "HR",
    "Manufacturing",
    "AI",
    "Province",
    "District",
    "LocalLevel",
  ],
  endpoints: () => ({}),
});
