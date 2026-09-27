import { api } from "./api";
import type { AuthUser } from "@/store/slices/auth.slice";

/**
 * Authentication API request/response types.
 *
 * These mirror the backend DTOs in `platform/identity` so the frontend
 * contract stays in sync with the server.
 */

/**
 * Request to authenticate a user.
 *
 * Matches the backend `LoginRequest` record:
 * - `tenantId` is optional; if omitted the backend searches across all tenants
 * - `username` accepts either a username or an email address
 * - `password` is the raw password (never stored or logged client-side)
 */
export interface LoginRequest {
  tenantId?: number;
  username: string;
  password: string;
  deviceInfo?: string;
  ipAddress?: string;
}

export interface RegisterRequest {
  tenantId?: number;
  username: string;
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phoneNumber?: string;
  jobTitle?: string;
  profileImageUrl?: string;
  branchId?: number;
  departmentId?: number;
  deviceInfo?: string;
  ipAddress?: string;
}

/**
 * Response returned after a successful login or token refresh.
 *
 * Mirrors the backend `TokenResponse` record.
 */
export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  accessTokenExpiresIn: number;
  refreshTokenExpiresIn: number;
  userId: number;
  tenantId: number;
  username: string;
  email: string;
  fullName: string;
  roles: string[];
}

export interface OnboardingRequest {
  firstName: string;
  lastName: string;
  email: string;
  mobile: string;
  password: string;
  confirmPassword: string;
}

export interface OnboardingResponse {
  userId: number;
  username: string;
  email: string;
  fullName: string;
  message: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface RefreshTokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  accessTokenExpiresIn: number;
  refreshTokenExpiresIn: number;
  userId: number;
  tenantId: number;
  username: string;
  email: string;
  fullName: string;
  roles: string[];
}

/**
 * Authentication API endpoints.
 *
 * All endpoints are typed and cached by RTK Query.
 * Tags are used for cache invalidation.
 */

export const authApi = api.injectEndpoints({
  endpoints: (build) => ({
    login: build.mutation<LoginResponse, LoginRequest>({
      query: (body) => ({
        url: "/auth/login",
        method: "POST",
        data: body,
      }),
      invalidatesTags: ["Auth"],
    }),

    register: build.mutation<LoginResponse, RegisterRequest>({
      query: (body) => ({
        url: "/auth/register",
        method: "POST",
        data: body,
      }),
      invalidatesTags: ["Auth"],
    }),

    onboard: build.mutation<OnboardingResponse, OnboardingRequest>({
      query: (body) => ({
        url: "/onboarding",
        method: "POST",
        data: body,
      }),
      invalidatesTags: ["Auth"],
    }),

    logout: build.mutation<void, { refreshToken: string }>({
      query: (body) => ({
        url: "/auth/logout",
        method: "POST",
        data: body,
      }),
      invalidatesTags: ["Auth"],
    }),

    refreshToken: build.mutation<RefreshTokenResponse, RefreshTokenRequest>({
      query: (body) => ({
        url: "/auth/refresh",
        method: "POST",
        data: body,
      }),
    }),

    getCurrentUser: build.query<AuthUser, void>({
      query: () => ({
        url: "/auth/me",
        method: "GET",
      }),
      providesTags: ["Auth"],
    }),

    forgotPassword: build.mutation<
      { message: string; resetToken?: string },
      { tenantId?: number; email: string }
    >({
      query: (body) => ({
        url: "/auth/forgot-password",
        method: "POST",
        data: body,
      }),
    }),

    resetPassword: build.mutation<void, { token: string; password: string }>({
      query: (body) => ({
        url: "/auth/reset-password",
        method: "POST",
        data: body,
      }),
    }),
  }),
});

export const {
  useLoginMutation,
  useRegisterMutation,
  useOnboardMutation,
  useLogoutMutation,
  useRefreshTokenMutation,
  useGetCurrentUserQuery,
  useForgotPasswordMutation,
  useResetPasswordMutation,
} = authApi;
