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
  getTenantId,
} from "@/services/token-store";
import {
  getAccessToken,
  getRefreshToken,
  getTenantId as getTenantIdFromCookie,
  setAuthCookies,
  clearAuthCookies,
} from "@/lib/auth";
import { isAccessTokenExpired } from "@/lib/jwt";
import { getApiErrorMessage, isApiErrorCode } from "@/services/error-handler";

const ACCESS_TOKEN_MAX_AGE = 900; // 15 minutes
const REFRESH_TOKEN_MAX_AGE = 604800; // 7 days
const REMEMBER_ME_REFRESH_MAX_AGE = 2592000; // 30 days

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
 *   the backend (`/auth/me`), refreshing once if the access token expired.
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
        return data.accessToken;
      } catch {
        return null;
      }
    });

    registerUnauthenticatedHandler(() => {
      dispatch(logoutAction());
      clearTokenSnapshot();
      clearAuthCookies();
      if (typeof window !== "undefined") {
        window.location.assign("/login");
      }
    });
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

      // Proactively refresh if the access token is expired or about to expire
      // (within 60 seconds). This avoids a failing /auth/me request and reduces
      // the session restoration from up to 2 sequential calls to 1.
      if (isAccessTokenExpired(access, 60)) {
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
        } catch {
          if (active) {
            dispatch(logoutAction());
            clearTokenSnapshot();
            clearAuthCookies();
            dispatch(finishInitialization());
            return;
          }
          return;
        }
      }

      try {
        const currentUser = await dispatch(fetchCurrentUserThunk()).unwrap();
        if (active) dispatch(setUser(currentUser));
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
          dispatch(setUser(currentUser));
        } catch {
          if (active) {
            dispatch(logoutAction());
            clearTokenSnapshot();
            clearAuthCookies();
          }
        }
      } finally {
        if (active) dispatch(finishInitialization());
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
    try {
      const refreshToken = getRefreshToken();
      if (refreshToken) {
        await logoutMutation({ refreshToken }).unwrap();
      }
    } catch {
      // Ignore server-side errors; always clear the local session.
    } finally {
      dispatch(logoutAction());
      clearTokenSnapshot();
      clearAuthCookies();
      toast.success("You have been signed out.", {
        description: "See you next time!",
      });
      router.push("/login");
      router.refresh();
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
