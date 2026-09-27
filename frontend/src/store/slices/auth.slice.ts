import {
  createAsyncThunk,
  createSlice,
  type PayloadAction,
} from "@reduxjs/toolkit";
import { axiosInstance } from "@/services/http-client";
import type { RefreshTokenResponse } from "@/services/auth.service";

/**
 * Shape of the authenticated user as returned by the backend.
 *
 * Mirrors the fields from the backend `TokenResponse` and `/auth/me` endpoint.
 */
export interface AuthUser {
  id: number;
  email: string;
  username: string;
  fullName: string;
  roles: string[];
  tenantId: string;
  [key: string]: unknown;
}

export type AuthStatus =
  | "idle"
  | "loading"
  | "authenticated"
  | "unauthenticated";

export interface AuthState {
  user: AuthUser | null;
  accessToken: string | null;
  refreshToken: string | null;
  tenantId: string | null;
  status: AuthStatus;
  /** True once the provider has attempted to restore a session on app load. */
  initialized: boolean;
  error: string | null;
}

const initialState: AuthState = {
  user: null,
  accessToken: null,
  refreshToken: null,
  tenantId: null,
  status: "idle",
  initialized: false,
  error: null,
};

/**
 * Exchanges a refresh token for a new access/refresh pair.
 *
 * Uses the raw axios instance directly (bypassing the auth-refresh
 * interceptor via `_skipAuthRefresh`) so a failing refresh cannot
 * trigger an infinite retry loop.
 */
export const refreshTokenThunk = createAsyncThunk<
  RefreshTokenResponse,
  string,
  { rejectValue: string }
>("auth/refreshToken", async (refreshToken, { rejectWithValue }) => {
  try {
    const { data } = await axiosInstance.post<RefreshTokenResponse>(
      "/auth/refresh",
      { refreshToken },
      { _skipAuthRefresh: true } as never
    );
    return data;
  } catch (err) {
    const message =
      (err as { response?: { data?: { message?: string } } })?.response?.data
        ?.message ?? "Session expired. Please sign in again.";
    return rejectWithValue(message);
  }
});

/**
 * Fetches the currently authenticated user's profile.
 *
 * Skips the auth-refresh interceptor so an expired token surfaces as a
 * rejection that the caller can handle (e.g. by refreshing once).
 */
export const fetchCurrentUserThunk = createAsyncThunk<
  AuthUser,
  void,
  { rejectValue: string }
>("auth/fetchCurrentUser", async (_, { rejectWithValue }) => {
  try {
    const { data } = await axiosInstance.get<AuthUser>("/auth/me", {
      _skipAuthRefresh: true,
    } as never);
    return data;
  } catch (err) {
    const message =
      (err as { response?: { data?: { message?: string } } })?.response?.data
        ?.message ?? "Unable to load your profile.";
    return rejectWithValue(message);
  }
});

const authSlice = createSlice({
  name: "auth",
  initialState,
  reducers: {
    /** Persist a full authenticated session after a successful login. */
    setCredentials: (
      state,
      action: PayloadAction<{
        user: AuthUser;
        accessToken: string;
        refreshToken: string;
        tenantId?: string | null;
      }>
    ) => {
      state.user = action.payload.user;
      state.accessToken = action.payload.accessToken;
      state.refreshToken = action.payload.refreshToken;
      state.tenantId = action.payload.tenantId ?? state.tenantId;
      state.status = "authenticated";
      state.error = null;
    },

    /** Restore tokens from storage during app initialization. */
    setSession: (
      state,
      action: PayloadAction<{
        accessToken: string;
        refreshToken: string;
        tenantId?: string | null;
      }>
    ) => {
      state.accessToken = action.payload.accessToken;
      state.refreshToken = action.payload.refreshToken;
      state.tenantId = action.payload.tenantId ?? state.tenantId;
      state.status = "loading";
    },

    /** Update the user profile (e.g. after fetching /auth/me). */
    setUser: (state, action: PayloadAction<AuthUser>) => {
      state.user = action.payload;
      state.status = "authenticated";
      state.error = null;
    },

    /** Record an authentication failure (invalid credentials, etc.). */
    authFailure: (state, action: PayloadAction<string | null>) => {
      state.status = "unauthenticated";
      state.error = action.payload;
      state.user = null;
      state.accessToken = null;
      state.refreshToken = null;
    },

    /** Mark initialization complete; default to unauthenticated if idle. */
    finishInitialization: (state) => {
      state.initialized = true;
      if (state.status === "idle") {
        state.status = "unauthenticated";
      }
    },

    /** Clear all authentication state (used by logout / session expiry). */
    logout: () => {
      return { ...initialState, initialized: true };
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(refreshTokenThunk.fulfilled, (state, action) => {
        state.accessToken = action.payload.accessToken;
        state.refreshToken = action.payload.refreshToken;
      })
      .addCase(fetchCurrentUserThunk.fulfilled, (state, action) => {
        state.user = action.payload;
        state.status = "authenticated";
        state.error = null;
      })
      .addCase(fetchCurrentUserThunk.rejected, (state, action) => {
        state.error = action.payload ?? "Failed to load user profile.";
      });
  },
});

export const {
  setCredentials,
  setSession,
  setUser,
  authFailure,
  finishInitialization,
  logout,
} = authSlice.actions;

export default authSlice.reducer;
