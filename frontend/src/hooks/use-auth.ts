"use client";

import { useAuthContext } from "@/components/providers/auth-provider";

/**
 * Authentication hook.
 *
 * Thin wrapper around the `AuthProvider` context. Provides the current user,
 * authentication status, and the `login` / `logout` / `refresh` actions.
 *
 * Must be used within the `AuthProvider` (already mounted in the root
 * `Providers` tree).
 */
export function useAuth() {
  return useAuthContext();
}
