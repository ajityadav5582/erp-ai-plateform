"use client";

import { useCallback } from "react";
import { useLoginMutation, useLogoutMutation, useGetCurrentUserQuery } from "@/services/auth.service";
import type { LoginRequest } from "@/services/auth.service";

/**
 * Authentication hook.
 *
 * Provides:
 * - `user` — the current authenticated user (from RTK Query)
 * - `isLoading` — loading state for the current user query
 * - `isAuthenticated` — derived boolean
 * - `login` — mutation to authenticate
 * - `logout` — mutation to end the session
 * - `error` — latest auth error
 */
export function useAuth() {
  const {
    data: user,
    isLoading,
    error,
    refetch,
  } = useGetCurrentUserQuery(undefined, {
    // Skip the query if we don't have a token (avoids 401 noise)
    skip: typeof document === "undefined" || !document.cookie.includes("access_token="),
  });

  const [login, loginResult] = useLoginMutation();
  const [logout, logoutResult] = useLogoutMutation();

  const handleLogin = useCallback(
    async (credentials: LoginRequest) => {
      const result = await login(credentials).unwrap();
      // Store tokens in httpOnly cookies via a server action or API response
      // For now, the interceptor handles token storage
      return result;
    },
    [login]
  );

  const handleLogout = useCallback(async () => {
    await logout().unwrap();
    // Clear client-side state
    if (typeof window !== "undefined") {
      document.cookie =
        "access_token=; refresh_token=; tenant_id=; path=/; max-age=0; SameSite=Strict";
    }
  }, [logout]);

  return {
    user,
    isLoading,
    isAuthenticated: !!user && !error,
    error: error as Error | null,
    login: handleLogin,
    logout: handleLogout,
    refetch,
    loginResult,
    logoutResult,
  };
}
