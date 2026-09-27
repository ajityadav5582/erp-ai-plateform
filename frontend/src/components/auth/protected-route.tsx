"use client";

import * as React from "react";
import { useRouter } from "next/navigation";
import { useAuthContext } from "@/components/providers/auth-provider";
import { LoadingSpinner } from "@/components/common/loading-spinner";

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

  React.useEffect(() => {
    if (isInitialized && !isAuthenticated) {
      router.replace(redirectTo);
    }
  }, [isInitialized, isAuthenticated, redirectTo, router]);

  if (!isInitialized || isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        {fallback ?? (
          <div className="flex flex-col items-center gap-3 text-muted-foreground">
            <LoadingSpinner size="lg" />
            <p className="text-sm">Verifying your session…</p>
          </div>
        )}
      </div>
    );
  }

  if (!isAuthenticated) {
    return null;
  }

  return <>{children}</>;
}
