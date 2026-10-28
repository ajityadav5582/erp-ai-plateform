"use client";

import * as React from "react";
import { toast } from "sonner";
import { useRouter } from "next/navigation";
import { useAppDispatch, useAppSelector } from "@/store/hooks";
import {
  setCredentials,
  setSession,
  setUser,
  finishInitialization,
  logout as logoutAction,
  fetchCurrentUserThunk,
  refreshTokenThunk,
  type AuthStatus,
  type AuthUser,
} from "@/store/slices/auth.slice";
import {
  useLoginMutation,
  useLogoutMutation,
  type LoginRequest,
} from "@/services/auth.service";
import {
  setTokenSnapshot,
  clearTokenSnapshot,
  registerRefreshHandler,
  registerUnauthenticatedHandler,
  resetUnauthenticatedNotification,
  executeTokenRefresh,
  getTenantId,
} from "@/services/token-store";
import {
  getAccessToken,
  getRefreshToken,
  getTenantId as getTenantIdFromCookie,
  setAuthCookies,
  clearAuthCookies,
} from "@/lib/auth";
import { isAccessTokenExpired, decodeJwtPayload } from "@/lib/jwt";
import { getApiErrorMessage, isApiErrorCode } from "@/services/error-handler";
import { api } from "@/services/api";

const ACCESS_TOKEN_MAX_AGE = 900; // 15 minutes
const REFRESH_TOKEN_MAX_AGE = 604800; // 7 days
const REMEMBER_ME_REFRESH_MAX_AGE = 2592000; // 30 days

/**
 * Renew the access token slightly before it expires.
 *
 * Without this, the first request issued after the access token lapses pays
 * for a 401 + `/auth/refresh` + replay round-trip, which is a large part of
 * the perceived slowness when navigating between pages.
 */
const PROACTIVE_REFRESH_LEEWAY_SECONDS = 120;

/** Handle for the scheduled proactive refresh. */
let proactiveRefreshTimer: ReturnType<typeof setTimeout> | null = null;

/** Cancels any pending proactive refresh (logout / unmount). */
function cancelProactiveRefresh(): void {
  if (proactiveRefreshTimer !== null) {
    clearTimeout(proactiveRefreshTimer);
    proactiveRefreshTimer = null;
  }
}

/**
 * Schedules a background refresh for the moment the given access token is
 * about to expire. Safe to call repeatedly; each call replaces the previous
 * schedule.
 */
function scheduleProactiveRefresh(accessToken: string | null): void {
  cancelProactiveRefresh();
  if (!accessToken || typeof window === "undefined") return;

  const payload = decodeJwtPayload(accessToken);
  const exp = typeof payload?.exp === "number" ? payload.exp : null;
  if (exp === null) return;

  const delayMs =
    (exp - PROACTIVE_REFRESH_LEEWAY_SECONDS - Date.now() / 1000) * 1000;

  // Never schedule in the past, and never spin faster than every 10s.
  const wait = Math.max(delayMs, 10_000);

  proactiveRefreshTimer = setTimeout(() => {
    proactiveRefreshTimer = null;
    void executeTokenRefresh();
  }, wait);
}

/**
 * localStorage key holding the last known user profile.
 *
 * Used only to paint the first frame after a hard refresh; the authoritative
 * profile always comes from `/auth/me`.
 */
const CACHED_USER_STORAGE_KEY = "erpai.cachedUser";

/** Reads the cached user profile, if one was persisted. */
function readCachedUser(): AuthUser | null {
  if (typeof window === "undefined") return null;
  try {
    const raw = window.localStorage.getItem(CACHED_USER_STORAGE_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as AuthUser;
    return parsed && typeof parsed === "object" && parsed.id != null
      ? parsed
      : null;
  } catch {
    return null;
  }
}

/** Persists the user profile for the next hard refresh. */
function writeCachedUser(user: AuthUser): void {
  if (typeof window === "undefined") return;
  try {
    window.localStorage.setItem(CACHED_USER_STORAGE_KEY, JSON.stringify(user));
  } catch {
    // Storage may be unavailable (private mode / quota); caching is optional.
  }
}

/** Removes the cached user profile. */
function clearCachedUser(): void {
  if (typeof window === "undefined") return;
  try {
    window.localStorage.removeItem(CACHED_USER_STORAGE_KEY);
  } catch {
    // Ignore.
  }
}

export interface LoginOptions {
  /** Extend the refresh-token lifetime (30 days instead of 7). */
  rememberMe?: boolean;
}

export interface AuthContextValue {
  user: AuthUser | null;
  isAuthenticated: boolean;
  /** True while a session is being established or verified. */
  isLoading: boolean;
  /** True once the provider has attempted to restore a session. */
  isInitialized: boolean;
  status: AuthStatus;
  error: string | null;
  login: (credentials: LoginRequest, options?: LoginOptions) => Promise<AuthUser>;
  logout: () => Promise<void>;
  /** Manually refresh the access token; resolves to success. */
  refresh: () => Promise<boolean>;
}

const AuthContext = React.createContext<AuthContextValue | null>(null);

/**
 * Provides authentication state and actions to the application.
 *
 * Responsibilities:
 * - Wires the Axios interceptors to the Redux-backed token store (refresh +
 *   unauthenticated handlers).
 * - Restores a session from cookies on first mount and validates it against
 *   the backend (`/identity/auth/me`), refreshing once if the access token
 *   expired. Validation happens in the background so the shell paints
 *   immediately instead of blocking on a "Verifying your session…" spinner.
 * - Exposes `login`, `logout`, and `refresh` actions via context.
 */
export function AuthProvider({ children }: { children: React.ReactNode }) {
  const dispatch = useAppDispatch();
  const router = useRouter();
  const { user, status, initialized, error } = useAppSelector(
    (state) => state.auth
  );

  const [loginMutation] = useLoginMutation();
  const [logoutMutation] = useLogoutMutation();

  // Register the interceptor handlers (refresh + session expiry) once.
  React.useEffect(() => {
    registerRefreshHandler(async (): Promise<string | null> => {
      const currentRefresh = getRefreshToken();
      if (!currentRefresh) return null;
      try {
        const data = await dispatch(
          refreshTokenThunk(currentRefresh)
        ).unwrap();
        setTokenSnapshot({
          accessToken: data.accessToken,
          refreshToken: data.refreshToken,
        });
        setAuthCookies(
          data.accessToken,
          data.refreshToken,
          getTenantId() ?? ""
        );
        scheduleProactiveRefresh(data.accessToken);
        return data.accessToken;
      } catch {
        return null;
      }
    });

    registerUnauthenticatedHandler(() => {
      dispatch(logoutAction());
      clearTokenSnapshot();
      clearAuthCookies();
      clearCachedUser();
      cancelProactiveRefresh();
      if (typeof window !== "undefined") {
        // Soft navigation instead of `window.location.assign`, which forced a
        // full document reload (re-download + re-parse of the whole bundle) and
        // was the slowest part of the expired-session redirect.
        if (!window.location.pathname.startsWith("/login")) {
          router.replace("/login");
        }
      }
    });
    // `router` is stable for the lifetime of the app shell.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [dispatch]);

  // Restore and validate any existing session on first mount.
  React.useEffect(() => {
    let active = true;

    const restoreSession = async () => {
      const access = getAccessToken();
      const refresh = getRefreshToken();
      const tenant = getTenantIdFromCookie();

      if (!access || !refresh) {
        dispatch(finishInitialization());
        return;
      }

      setTokenSnapshot({
        accessToken: access,
        refreshToken: refresh,
        tenantId: tenant,
      });
      dispatch(
        setSession({ accessToken: access, refreshToken: refresh, tenantId: tenant })
      );

      // Optimistically hydrate from the cached profile so the protected shell
      // renders on the first paint. Validation against `/auth/me` continues in
      // the background instead of blocking the whole app behind a spinner.
      const cachedUser = readCachedUser();
      if (cachedUser) {
        dispatch(setUser(cachedUser));
      }
      dispatch(finishInitialization());

      let currentAccess = access;

      // Proactively refresh if the access token is expired or about to expire
      // (within 60 seconds). This avoids a failing /auth/me request and reduces
      // the session restoration from up to 2 sequential calls to 1.
      if (isAccessTokenExpired(currentAccess, 60)) {
        try {
          const data = await dispatch(refreshTokenThunk(refresh)).unwrap();
          if (!active) return;
          currentAccess = data.accessToken;
          setTokenSnapshot({
            accessToken: currentAccess,
            refreshToken: data.refreshToken,
          });
          setAuthCookies(currentAccess, data.refreshToken, tenant ?? "");
        } catch {
          if (active) {
            dispatch(logoutAction());
            clearTokenSnapshot();
            clearAuthCookies();
            clearCachedUser();
            cancelProactiveRefresh();
            dispatch(finishInitialization());
            return;
          }
          return;
        }
      }

      try {
        const currentUser = await dispatch(fetchCurrentUserThunk()).unwrap();
        if (!active) return;
        dispatch(setUser(currentUser));
        writeCachedUser(currentUser);
        scheduleProactiveRefresh(currentAccess);
      } catch {
        // Access token invalid/expired — attempt a single refresh.
        try {
          const data = await dispatch(refreshTokenThunk(refresh)).unwrap();
          if (!active) return;
          setTokenSnapshot({
            accessToken: data.accessToken,
            refreshToken: data.refreshToken,
          });
          setAuthCookies(
            data.accessToken,
            data.refreshToken,
            tenant ?? ""
          );
          const currentUser = await dispatch(fetchCurrentUserThunk()).unwrap();
          if (!active) return;
          dispatch(setUser(currentUser));
          writeCachedUser(currentUser);
          scheduleProactiveRefresh(data.accessToken);
        } catch {
          if (active) {
            dispatch(logoutAction());
            clearTokenSnapshot();
            clearAuthCookies();
            clearCachedUser();
            cancelProactiveRefresh();
          }
        }
      }
    };

    void restoreSession();

    return () => {
      active = false;
    };
  }, [dispatch]);

  const login = React.useCallback(
    async (
      credentials: LoginRequest,
      options?: LoginOptions
    ): Promise<AuthUser> => {
      try {
        const result = await loginMutation(credentials).unwrap();

        // Map the backend TokenResponse to the frontend AuthUser shape.
        const authUser: AuthUser = {
          id: result.userId,
          email: result.email,
          username: result.username,
          fullName: result.fullName,
          roles: result.roles,
          permissions: result.permissions ?? [],
          tenantId: String(result.tenantId),
        };

        const refreshMaxAge = options?.rememberMe
          ? REMEMBER_ME_REFRESH_MAX_AGE
          : REFRESH_TOKEN_MAX_AGE;

        dispatch(
          setCredentials({
            user: authUser,
            accessToken: result.accessToken,
            refreshToken: result.refreshToken,
            tenantId: authUser.tenantId,
          })
        );
        setTokenSnapshot({
          accessToken: result.accessToken,
          refreshToken: result.refreshToken,
          tenantId: authUser.tenantId,
        });
        setAuthCookies(
          result.accessToken,
          result.refreshToken,
          authUser.tenantId,
          {
            accessTokenMaxAge: ACCESS_TOKEN_MAX_AGE,
            refreshTokenMaxAge: refreshMaxAge,
          }
        );
        writeCachedUser(authUser);
        scheduleProactiveRefresh(result.accessToken);
        resetUnauthenticatedNotification();

        toast.success(`Welcome back, ${result.fullName || result.username}!`, {
          description: "You have successfully signed in.",
        });

        return authUser;
      } catch (err) {
        const message = getApiErrorMessage(err);
        const isLocked = isApiErrorCode(err, "ACCOUNT_LOCKED");

        if (isLocked) {
          toast.error("Account locked", {
            description: message,
            duration: 6000,
          });
        } else {
          toast.error("Sign in failed", {
            description: message,
          });
        }

        throw err;
      }
    },
    [dispatch, loginMutation]
  );

  const logout = React.useCallback(async (): Promise<void> => {
    cancelProactiveRefresh();
    const refreshToken = getRefreshToken();

    // Clear the local session first so the sign-out toast and the
    // redirect to /login render immediately. The previous flow awaited
    // the server round-trip before touching the UI, so a slow (or
    // hung) backend left the app stuck on "Signing out…" — with no
    // popup and no redirect — for up to the 30s Axios timeout.
    dispatch(logoutAction());
    // Wipe the RTK Query cache along with the session. The cache
    // lives in the Redux store and survives client-side navigation,
    // so without this the next login (possibly a different user on a
    // shared device) would briefly see the previous user's cached
    // responses now that `refetchOnMountOrArgChange` is off.
    dispatch(api.util.resetApiState());
    clearTokenSnapshot();
    clearAuthCookies();
    clearCachedUser();

    toast.success("You have been signed out.", {
      description: "See you next time!",
    });

    // `router.replace` keeps the SPA alive; a full reload would
    // re-download and re-parse the whole bundle.
    router.replace("/login");

    // Revoke the refresh token server-side in the background. The
    // endpoint is public (`permitAll`) and authenticates via the
    // refresh token in the request body, so it works without the
    // cleared credentials. Never block the redirect on this call.
    if (refreshToken) {
      void logoutMutation({ refreshToken })
        .unwrap()
        .catch(() => {
          // Local session is already cleared; nothing left to do.
        });
    }
  }, [dispatch, logoutMutation, router]);

  const refresh = React.useCallback(async (): Promise<boolean> => {
    const currentRefresh = getRefreshToken();
    if (!currentRefresh) return false;
    try {
      const data = await dispatch(refreshTokenThunk(currentRefresh)).unwrap();
      setTokenSnapshot({
        accessToken: data.accessToken,
        refreshToken: data.refreshToken,
      });
      setAuthCookies(
        data.accessToken,
        data.refreshToken,
        getTenantId() ?? ""
      );
      scheduleProactiveRefresh(data.accessToken);
      return true;
    } catch {
      return false;
    }
  }, [dispatch]);

  const value = React.useMemo<AuthContextValue>(
    () => ({
      user,
      isAuthenticated: status === "authenticated" && !!user,
      isLoading: !initialized || status === "loading",
      isInitialized: initialized,
      status,
      error,
      login,
      logout,
      refresh,
    }),
    [user, status, initialized, error, login, logout, refresh]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

/**
 * Access the authentication context.
 *
 * Must be used within an `AuthProvider`.
 */
export function useAuthContext(): AuthContextValue {
  const ctx = React.useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuthContext must be used within an <AuthProvider>");
  }
  return ctx;
}
