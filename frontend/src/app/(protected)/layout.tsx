import { ProtectedRoute } from "@/components/auth/protected-route";

/**
 * Layout for the `(protected)` route group.
 *
 * Every route nested under this group is guarded by `ProtectedRoute`, which
 * redirects unauthenticated users to `/login` and shows a loading state while
 * the session is being resolved. Route groups do not affect the URL, so the
 * dashboard remains served at `/`.
 */
export default function ProtectedLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return <ProtectedRoute>{children}</ProtectedRoute>;
}
