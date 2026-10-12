/**
 * Authentication utility functions.
 *
 * Provides helpers for managing authentication state in the browser,
 * including token storage, retrieval, and cleanup.
 */

/**
 * Reads a single cookie value by name.
 *
 * Cookie values may legitimately contain `=` (base64 padding, and JWT segments
 * produced by some encoders). The previous implementation used
 * `split("=")[1]`, which silently truncated such values and produced a
 * corrupt token that the server rejected — logging the user out on refresh.
 * Splitting on the first `=` only, and URL-decoding, fixes both cases.
 */
function readCookie(name: string): string | null {
  if (typeof document === "undefined") return null;

  const prefix = `${name}=`;
  for (const part of document.cookie.split(";")) {
    const row = part.trim();
    if (row.startsWith(prefix)) {
      const raw = row.slice(prefix.length);
      if (!raw) return null;
      try {
        return decodeURIComponent(raw);
      } catch {
        // Value was not URI-encoded; return it verbatim.
        return raw;
      }
    }
  }
  return null;
}

/**
 * Retrieves the access token from cookies.
 */
export function getAccessToken(): string | null {
  return readCookie("access_token");
}

/**
 * Retrieves the refresh token from cookies.
 */
export function getRefreshToken(): string | null {
  return readCookie("refresh_token");
}

/**
 * Retrieves the tenant ID from cookies.
 */
export function getTenantId(): string | null {
  return readCookie("tenant_id");
}

/**
 * Checks if the user is authenticated (has a valid access token).
 */
export function isAuthenticated(): boolean {
  return !!getAccessToken();
}

/**
 * Clears all authentication cookies.
 *
 * Each cookie is expired individually so that a cookie written with the
 * `Secure` attribute in production is still matched and removed.
 */
export function clearAuthCookies(): void {
  if (typeof document === "undefined") return;

  const secure =
    process.env.NODE_ENV === "production" ? "; Secure" : "";
  for (const name of ["access_token", "refresh_token", "tenant_id"]) {
    document.cookie = `${name}=; path=/; max-age=0; SameSite=Strict${secure}`;
  }
}

/**
 * Options for {@link setAuthCookies}.
 */
export interface SetAuthCookiesOptions {
  /** Access-token lifetime in seconds (default 900 = 15 minutes). */
  accessTokenMaxAge?: number;
  /** Refresh-token lifetime in seconds (default 604800 = 7 days). */
  refreshTokenMaxAge?: number;
}

/**
 * Sets authentication cookies.
 *
 * Note: In production, httpOnly cookies should be set by the server.
 * This function is provided for development/testing purposes.
 */
export function setAuthCookies(
  accessToken: string,
  refreshToken: string,
  tenantId: string,
  options?: SetAuthCookiesOptions
): void {
  if (typeof document === "undefined") return;

  const isProduction = process.env.NODE_ENV === "production";
  const secure = isProduction ? "; Secure" : "";
  const sameSite = "; SameSite=Strict";

  const accessTokenMaxAge = options?.accessTokenMaxAge ?? 900;
  const refreshTokenMaxAge = options?.refreshTokenMaxAge ?? 604800;

  document.cookie = `access_token=${accessToken}; path=/; max-age=${accessTokenMaxAge}${secure}${sameSite}`;
  document.cookie = `refresh_token=${refreshToken}; path=/; max-age=${refreshTokenMaxAge}${secure}${sameSite}`;
  document.cookie = `tenant_id=${tenantId}; path=/; max-age=${refreshTokenMaxAge}${secure}${sameSite}`;
}
