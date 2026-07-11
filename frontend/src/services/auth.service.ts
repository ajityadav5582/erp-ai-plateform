import { api } from "./api";

/**
 * Authentication API request/response types.
 */
export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  access_token: string;
  refresh_token: string;
  user: unknown;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface RefreshTokenResponse {
  access_token: string;
  refresh_token: string;
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

    logout: build.mutation<void, void>({
      query: () => ({
        url: "/auth/logout",
        method: "POST",
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

    getCurrentUser: build.query<unknown, void>({
      query: () => ({
        url: "/auth/me",
        method: "GET",
      }),
      providesTags: ["Auth"],
    }),

    forgotPassword: build.mutation<void, { email: string }>({
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
  useLogoutMutation,
  useRefreshTokenMutation,
  useGetCurrentUserQuery,
  useForgotPasswordMutation,
  useResetPasswordMutation,
} = authApi;
