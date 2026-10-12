"use client";

import type { ReactNode } from "react";
import { useHasPermission, useHasAnyPermission, useHasAllPermissions } from "@/hooks/use-permission";

interface PermissionGateProps {
  /** Single permission code to check */
  permission?: string;
  /** Array of permission codes - user needs ANY of them (OR logic) */
  anyOf?: string[];
  /** Array of permission codes - user needs ALL of them (AND logic) */
  allOf?: string[];
  /** Content to render when permission is granted */
  children: ReactNode;
  /** Content to render when permission is denied (optional) */
  fallback?: ReactNode;
}

/**
 * Conditionally renders children based on the current user's permissions.
 *
 * @example
 * // Single permission
 * <PermissionGate permission="PR_VIEW">
 *   <Button>View Requisition</Button>
 * </PermissionGate>
 *
 * @example
 * // Any of multiple permissions
 * <PermissionGate anyOf={["PR_APPROVE", "PR_REJECT"]}>
 *   <Button>Take Action</Button>
 * </PermissionGate>
 *
 * @example
 * // All permissions required
 * <PermissionGate allOf={["PURCHASE_ORDER_CREATE", "PURCHASE_ORDER_APPROVE"]}>
 *   <Button>Create & Approve PO</Button>
 * </PermissionGate>
 */
export function PermissionGate({
  permission,
  anyOf,
  allOf,
  children,
  fallback = null,
}: PermissionGateProps) {
  // Call all hooks unconditionally to satisfy React Hooks rules
  const singlePermission = useHasPermission(permission ?? "");
  const anyPermission = useHasAnyPermission(anyOf ?? []);
  const allPermissions = useHasAllPermissions(allOf ?? []);

  let hasAccess = false;

  if (permission) {
    hasAccess = singlePermission;
  } else if (anyOf && anyOf.length > 0) {
    hasAccess = anyPermission;
  } else if (allOf && allOf.length > 0) {
    hasAccess = allPermissions;
  }

  if (!hasAccess) {
    return <>{fallback}</>;
  }

  return <>{children}</>;
}
