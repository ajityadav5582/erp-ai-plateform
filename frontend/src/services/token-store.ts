/**
 * In-memory token registry that decouples the Axios interceptors from the
 * Redux store and React tree.
 *
 * The interceptors (see `services/interceptors.ts`) need to read the current
 * access token and trigger a refresh without importing the Redux store
 * directly (which would create a circular dependency). The `AuthProvider`
 * registers the actual implementations (backed by the Redux slice) when it
 * mounts, and the interceptors simply call into this registry at request time.
 */

export interface TokenSnapshot {
  accessToken: string | null;
  refreshToken: string | null;
  tenantId: string | null;
}

/** Returns a fresh access token after a successful refresh, or null on failure. */
type RefreshHandler = () => Promise<string | null>;

/** Invoked when the session can no longer be recovered (redirect to login). */
type UnauthenticatedHandler = () => void;

let tokens: TokenSnapshot = {
  accessToken: null,
  refreshToken: null,
  tenantId: null,
};

let refreshHandler: RefreshHandler | null = null;
let unauthenticatedHandler: UnauthenticatedHandler | null = null;

/** Replace part (or all) of the in-memory token snapshot. */
export function setTokenSnapshot(next: Partial<TokenSnapshot>): void {
  tokens = { ...tokens, ...next };
}

export function getAccessToken(): string | null {
  return tokens.accessToken;
}

export function getRefreshToken(): string | null {
  return tokens.refreshToken;
}

export function getTenantId(): string | null {
  return tokens.tenantId;
}

/** Wipe the in-memory snapshot (e.g. on logout or session expiry). */
export function clearTokenSnapshot(): void {
  tokens = { accessToken: null, refreshToken: null, tenantId: null };
}

/** Register the function used to obtain a new access token during a 401. */
export function registerRefreshHandler(handler: RefreshHandler): void {
  refreshHandler = handler;
}

/** Register the function invoked when the session is unrecoverable. */
export function registerUnauthenticatedHandler(
  handler: UnauthenticatedHandler
): void {
  unauthenticatedHandler = handler;
}

/** Attempt a token refresh via the registered handler. */
export async function executeTokenRefresh(): Promise<string | null> {
  if (!refreshHandler) return null;
  try {
    return await refreshHandler();
  } catch {
    return null;
  }
}

/** Notify the app that the user is no longer authenticated. */
export function notifyUnauthenticated(): void {
  unauthenticatedHandler?.();
}
