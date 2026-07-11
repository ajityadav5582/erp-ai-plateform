/**
 * Authentication utility functions.
 *
 * Provides helpers for managing authentication state in the browser,
 * including token storage, retrieval, and cleanup.
 */

/**
 * Retrieves the access token from cookies.
 */
export function getAccessToken(): string | null {
  if (typeof document === "undefined") return null;

  return document.cookie
    .split("; ")
    .find((row) => row.startsWith("access_token="))
    ?.split("=")[1] ?? null;
}

/**
 * Retrieves the refresh token from cookies.
 */
export function getRefreshToken(): string | null {
  if (typeof document === "undefined") return null;

  return document.cookie
    .split("; ")
    .find((row) => row.startsWith("refresh_token="))
    ?.split("=")[1] ?? null;
}

/**
 * Retrieves the tenant ID from cookies.
 */
export function getTenantId(): string | null {
  if (typeof document === "undefined") return null;

  return document.cookie
    .split("; ")
    .find((row) => row.startsWith("tenant_id="))
    ?.split("=")[1] ?? null;
}

/**
 * Checks if the user is authenticated (has a valid access token).
 */
export function isAuthenticated(): boolean {
  return !!getAccessToken();
}

/**
 * Clears all authentication cookies.
 */
export function clearAuthCookies(): void {
  if (typeof document === "undefined") return;

  document.cookie =
    "access_token=; refresh_token=; tenant_id=; path=/; max-age=0; SameSite=Strict";
}

/**
 * Sets authentication cookies.
 *
 * Note: In production, httpOnly cookies should be set by the server.
 * This function is provided for development/testing purposes.
 */
export function setAuthCookies(accessToken: string, refreshToken: string, tenantId: string): void {
  if (typeof document === "undefined") return;

  const isProduction = process.env.NODE_ENV === "production";
  const secure = isProduction ? "; Secure" : "";
  const sameSite = "; SameSite=Strict";

  document.cookie = `access_token=${accessToken}; path=/; max-age=900${secure}${sameSite}`;
  document.cookie = `refresh_token=${refreshToken}; path=/; max-age=604800${secure}${sameSite}`;
  document.cookie = `tenant_id=${tenantId}; path=/; max-age=604800${secure}${sameSite}`;
}
