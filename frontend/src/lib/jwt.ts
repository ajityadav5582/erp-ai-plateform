/**
 * JWT utility functions.
 *
 * Provides lightweight JWT decoding and expiration checking without
 * any external dependencies. Uses base64url decoding to read the payload.
 */

interface JwtPayload {
  exp?: number;
  [key: string]: unknown;
}

/**
 * Decodes a JWT payload without verification.
 *
 * Only reads the payload segment; does not validate the signature.
 * Returns null if the token is malformed.
 */
export function decodeJwtPayload(token: string): JwtPayload | null {
  try {
    const parts = token.split(".");
    if (parts.length !== 3) return null;

    const payload = parts[1]!;
    // base64url decode
    const base64 = payload.replace(/-/g, "+").replace(/_/g, "/");
    const json = atob(base64);
    return JSON.parse(json) as JwtPayload;
  } catch {
    return null;
  }
}

/**
 * Checks if a JWT access token is expired or will expire within the
 * given buffer window (in seconds).
 *
 * Returns true if the token is expired or missing.
 */
export function isAccessTokenExpired(token: string | null, bufferSeconds = 60): boolean {
  if (!token) return true;

  const payload = decodeJwtPayload(token);
  if (!payload) return true;

  const exp = payload.exp;
  if (typeof exp !== "number") return true;

  const now = Math.floor(Date.now() / 1000);
  return exp! <= now + bufferSeconds;
}
