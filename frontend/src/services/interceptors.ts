import type {
  AxiosError,
  AxiosResponse,
  InternalAxiosRequestConfig,
} from "axios";
import { axiosInstance } from "./http-client";
import {
  getAccessToken,
  executeTokenRefresh,
  notifyUnauthenticated,
} from "./token-store";

/**
 * Internal request config extension used by the auth-refresh flow.
 */
interface AuthRequestConfig extends InternalAxiosRequestConfig {
  /** Marks a request that has already been retried after a 401. */
  _retry?: boolean;
  /** Opt-out flag so refresh/me calls don't trigger the refresh loop. */
  _skipAuthRefresh?: boolean;
}

/**
 * Attaches the JWT request/response interceptors to the shared Axios instance.
 *
 * - Request: injects the current access token (Bearer). Tenant information
 *   is carried inside the JWT claims, so no separate X-Tenant-ID header is
 *   sent.
 * - Response: on a 401, transparently refreshes the access token once and
 *   replays the original request. If refresh fails, the app is notified so it
 *   can clear the session and redirect to login.
 *
 * This must be called once at application startup (wired from `services/api.ts`).
 */
export function setupInterceptors(): void {
  axiosInstance.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
      const token = getAccessToken();
      if (token && config.headers) {
        config.headers.Authorization = `Bearer ${token}`;
      }

      return config;
    },
    (error: AxiosError) => Promise.reject(error)
  );

  axiosInstance.interceptors.response.use(
    (response: AxiosResponse) => response,
    async (error: AxiosError) => {
      const originalRequest = error.config as AuthRequestConfig | undefined;

      const isUnauthorized = error.response?.status === 401;
      const canRetry =
        !!originalRequest &&
        !originalRequest._retry &&
        !originalRequest._skipAuthRefresh;

      if (isUnauthorized && canRetry) {
        originalRequest._retry = true;

        const newAccessToken = await executeTokenRefresh();
        if (newAccessToken && originalRequest.headers) {
          originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
          return axiosInstance(originalRequest);
        }

        // Refresh failed (or no handler) — session is unrecoverable.
        notifyUnauthenticated();
      }

      return Promise.reject(error);
    }
  );
}
