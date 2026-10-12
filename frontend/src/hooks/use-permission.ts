import { useMemo } from "react";
import { useAppSelector } from "@/store/hooks";

/**
 * Checks if the current authenticated user has a specific permission.
 *
 * @param permission - The permission code to check (e.g. "PR_VIEW", "PURCHASE_ORDER_CREATE")
 * @returns true if the user has the permission, false otherwise
 */
export function useHasPermission(permission: string): boolean {
  const permissions = useAppSelector((state) => state.auth.user?.permissions ?? []);
  return useMemo(
    () => permissions.includes(permission),
    [permissions, permission]
  );
}

/**
 * Checks if the current authenticated user has any of the specified permissions.
 *
 * @param permissions - Array of permission codes to check
 * @returns true if the user has at least one of the permissions
 */
export function useHasAnyPermission(permissions: string[]): boolean {
  const userPermissions = useAppSelector((state) => state.auth.user?.permissions ?? []);
  return useMemo(
    () => permissions.some((p) => userPermissions.includes(p)),
    [userPermissions, permissions]
  );
}

/**
 * Checks if the current authenticated user has all of the specified permissions.
 *
 * @param permissions - Array of permission codes to check
 * @returns true if the user has all of the permissions
 */
export function useHasAllPermissions(permissions: string[]): boolean {
  const userPermissions = useAppSelector((state) => state.auth.user?.permissions ?? []);
  return useMemo(
    () => permissions.every((p) => userPermissions.includes(p)),
    [userPermissions, permissions]
  );
}
