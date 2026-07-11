import { configureStore } from "@reduxjs/toolkit";
import { api } from "@/services/api";

/**
 * Root Redux store configuration.
 *
 * Uses Redux Toolkit's `configureStore` which:
 * - Combines all reducers
 * - Adds Redux DevTools extension support (disabled in production)
 * - Includes thunk middleware by default
 * - Enables Redux Toolkit Query (RTK Query) for data fetching
 */

export const makeStore = () => {
  return configureStore({
    reducer: {
      // RTK Query API reducer
      [api.reducerPath]: api.reducer,
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

// Infer the `RootState` and `AppDispatch` types from the store itself
export type RootState = ReturnType<ReturnType<typeof makeStore>["getState"]>;
export type AppDispatch = ReturnType<ReturnType<typeof makeStore>["dispatch"]>;
