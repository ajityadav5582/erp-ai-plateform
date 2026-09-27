import { configureStore } from "@reduxjs/toolkit";
import { api } from "@/services/api";
import authReducer from "@/store/slices/auth.slice";

/**
 * Root Redux store configuration.
 *
 * Uses Redux Toolkit's `configureStore` which:
 * - Combines all reducers (RTK Query API + feature slices)
 * - Adds Redux DevTools extension support (disabled in production)
 * - Includes thunk middleware by default
 * - Enables Redux Toolkit Query (RTK Query) for data fetching
 */
export const makeStore = () => {
  return configureStore({
    reducer: {
      // RTK Query API reducer
      [api.reducerPath]: api.reducer,
      // Feature slices
      auth: authReducer,
    },
    middleware: (getDefaultMiddleware) =>
      getDefaultMiddleware({
        // Enable serializable check for production safety
        serializableCheck: {
          ignoredActions: ["api/executeQuery/pending"],
        },
        // Enable immutable check for production safety
        immutableCheck: {
          ignoredPaths: ["api.queries"],
        },
      }).concat(api.middleware),
    devTools: process.env.NODE_ENV !== "production",
  });
};

/**
 * Singleton store instance.
 *
 * A single store is shared across the client application so authentication
 * and other state survive re-renders. (In a server-rendered context you would
 * create a store per request; for this client-only SPA a singleton is correct.)
 */
export const store = makeStore();

// Infer the `RootState` and `AppDispatch` types from the store itself
export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
