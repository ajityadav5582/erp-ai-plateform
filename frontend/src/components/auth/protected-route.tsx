"use client";

import * as React from "react";
import { useRouter, usePathname } from "next/navigation";
import { useAuthContext } from "@/components/providers/auth-provider";
import { CenteredLoader } from "@/components/common/page-loading";

interface ProtectedRouteProps {
  children: React.ReactNode;
  /** Where to send unauthenticated users. Defaults to `/login`. */
  redirectTo?: string;
  /** Custom UI shown while the session is being resolved. */
  fallback?: React.ReactNode;
}

/**
 * Route guard that only renders `children` for authenticated users.
 *
 * - While the session is still being resolved (`isLoading`), a spinner is
 *   shown to avoid a flash of protected content or a premature redirect.
 * - Once resolved, unauthenticated users are redirected to `redirectTo`.
 *
 * Pair this with a `(protected)` route-group layout to guard entire sections
 * of the application.
 */
export function ProtectedRoute({
  children,
  redirectTo = "/login",
  fallback,
}: ProtectedRouteProps) {
  const router = useRouter();
  const { isAuthenticated, isInitialized, isLoading } = useAuthContext();
  const pathname = usePathname();

  React.useEffect(() => {
    if (isInitialized && !isAuthenticated) {
      router.replace(redirectTo);
    }
  }, [isInitialized, isAuthenticated, redirectTo, router]);

  // Already on the destination page — do not bounce between routes.
  if (pathname === redirectTo) {
    return null;
  }

  if (!isInitialized || isLoading) {
    // Rendered inline (not `min-h-screen`) so the surrounding app shell stays
    // mounted instead of collapsing to a full-page spinner on every navigation.
    // `CenteredLoader` already reserves `min-h-[50vh]`, so the wrapper is dropped
    // here rather than nested inside it.
    return (
      fallback ?? <CenteredLoader label="Verifying your session…" />
    );
  }

  if (!isAuthenticated) {
    return null;
  }

  return <>{children}</>;
}
