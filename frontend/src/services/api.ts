import { createApi } from "@reduxjs/toolkit/query/react";
import type { BaseQueryFn } from "@reduxjs/toolkit/query";
import axios, { type AxiosError, type AxiosRequestConfig, type InternalAxiosRequestConfig } from "axios";
import { getEnv } from "@/config/env";

/**
 * Axios instance configured for the ERP AI Platform API.
 *
 * - Base URL comes from `API_BASE_URL` env var.
 * - Timeout comes from `API_TIMEOUT_MS` env var.
 * - JSON content type is set by default.
 */
export const axiosInstance = axios.create({
  baseURL: getEnv().API_BASE_URL,
  timeout: getEnv().API_TIMEOUT_MS,
  headers: {
    "Content-Type": "application/json",
  },
});

/**
 * Request interceptor: injects the access token from cookies (if present)
 * and the tenant identifier header required by the multi-tenant backend.
 */
axiosInstance.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // Attach access token from httpOnly cookie via a helper (server can read it,
    // client cannot; this is a placeholder for a token-refresh flow or a
    // client-side token store if using a non-httpOnly strategy).
    const token = document.cookie
      .split("; ")
      .find((row) => row.startsWith("access_token="))
      ?.split("=")[1];

    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    // Multi-tenant header: the backend expects the tenant ID in this header.
    // The value is set by the tenant resolver middleware on the server.
    const tenantId = document.cookie
      .split("; ")
      .find((row) => row.startsWith("tenant_id="))
      ?.split("=")[1];

    if (tenantId && config.headers) {
      config.headers["X-Tenant-ID"] = tenantId;
    }

    return config;
  },
  (error: AxiosError) => Promise.reject(error)
);

/**
 * Response interceptor: normalizes error handling and refreshes the access
 * token when a 401 is received (if a refresh token is available).
 */
axiosInstance.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as AxiosRequestConfig & { _retry?: boolean };

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        const refreshToken = document.cookie
          .split("; ")
          .find((row) => row.startsWith("refresh_token="))
          ?.split("=")[1];

        if (refreshToken) {
          const { data } = await axios.post(
            `${getEnv().API_BASE_URL}/auth/refresh`,
            { refreshToken }
          );

          const newAccessToken = data?.access_token;
          if (newAccessToken) {
            document.cookie = `access_token=${newAccessToken}; path=/; max-age=900; SameSite=Strict`;
            if (originalRequest.headers) {
              originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
            }
            return axiosInstance(originalRequest);
          }
        }
      } catch {
        // Refresh failed — clear tokens and redirect to login
        document.cookie =
          "access_token=; refresh_token=; tenant_id=; path=/; max-age=0; SameSite=Strict";
        window.location.href = "/login";
      }
    }

    return Promise.reject(error);
  }
);

/**
 * API error type returned by the custom base query.
 */
export interface ApiError {
  status?: number;
  data?: { message?: string };
  error: string;
}

/**
 * Custom base query that wraps Axios for RTK Query.
 *
 * This gives us full control over request/response handling while still
 * leveraging RTK Query's caching, polling, and optimistic updates.
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
    "Role",
    "Permission",
    "Department",
    "Tenant",
    "Finance",
    "Inventory",
    "HR",
    "Manufacturing",
    "AI",
  ],
  endpoints: () => ({}),
});
