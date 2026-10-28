import { useMemo } from "react";
import { useAppSelector } from "@/store/hooks";

/**
 * Checks if the current authenticated user has a specific permission.
 *
 * @param permission - The permission code to check (e.g. "PR_VIEW", "PURCHASE_ORDER_CREATE")
 * @returns true if the user has the permission, false otherwise
 */
const ADMIN_ROLES = new Set(["SUPER_ADMIN", "TENANT_ADMIN", "OWNER", "ADMIN"]);

export function useHasPermission(permission: string): boolean {
  const permissions = useAppSelector((state) => state.auth.user?.permissions ?? []);
  const roles = useAppSelector((state) => state.auth.user?.roles ?? []);
  return useMemo(
    () => {
      // Backend treats any administrative role as holding every permission,
      // so mirror that here to keep the UI in sync (e.g. tenant admins see
      // every module even when a specific code is absent from their token).
      if (roles.some((role) => ADMIN_ROLES.has(role))) return true;
      if (!permission) return true;
      return permissions.includes(permission);
    },
    [permissions, roles, permission]
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
  const roles = useAppSelector((state) => state.auth.user?.roles ?? []);
  return useMemo(() => {
    if (roles.some((role) => ADMIN_ROLES.has(role))) return true;
    if (permissions.length === 0) return true;
    return permissions.some((p) => userPermissions.includes(p));
  }, [userPermissions, roles, permissions]);
}

/**
 * Checks if the current authenticated user has all of the specified permissions.
 *
 * @param permissions - Array of permission codes to check
 * @returns true if the user has all of the permissions
 */
export function useHasAllPermissions(permissions: string[]): boolean {
  const userPermissions = useAppSelector((state) => state.auth.user?.permissions ?? []);
  const roles = useAppSelector((state) => state.auth.user?.roles ?? []);
  return useMemo(() => {
    if (roles.some((role) => ADMIN_ROLES.has(role))) return true;
    if (permissions.length === 0) return true;
    return permissions.every((p) => userPermissions.includes(p));
  }, [userPermissions, roles, permissions]);
}
