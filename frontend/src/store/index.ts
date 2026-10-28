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
const isProduction = process.env.NODE_ENV === "production";

/**
 * RTK Query keeps a mutable, non-serializable bookkeeping subtree under
 * `api`. Excluding it keeps the development-time invariant checks meaningful
 * (they still guard our own slices) without drowning them in false positives.
 */
const RTK_QUERY_IGNORED_PATHS = [
  "api.queries",
  "api.mutations",
  "api.subscriptions",
  "api.config",
];

export const makeStore = () => {
  return configureStore({
    reducer: {
      // RTK Query API reducer
      [api.reducerPath]: api.reducer,
      // Feature slices
      auth: authReducer,
    },
    middleware: (getDefaultMiddleware) => {
      // These checks deep-walk the whole state tree on *every* dispatch. With
      // RTK Query that tree holds every cached response, so the walk cost grows
      // with the cache and becomes the dominant cost of a navigation. They are
      // development-only diagnostics, so they are removed in production.
      const defaultMiddleware = getDefaultMiddleware(
        isProduction
          ? {
              serializableCheck: false,
              immutableCheck: false,
            }
          : {
              serializableCheck: {
                ignoredActionPaths: [
                  "meta.arg",
                  "meta.baseQueryMeta",
                  "payload.headers",
                ],
                ignoredPaths: RTK_QUERY_IGNORED_PATHS,
              },
              immutableCheck: {
                ignoredPaths: RTK_QUERY_IGNORED_PATHS,
              },
            }
      );
      return defaultMiddleware.concat(api.middleware);
    },
    devTools: !isProduction,
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
