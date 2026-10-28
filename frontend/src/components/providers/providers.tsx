"use client";

import * as React from "react";
import { Provider } from "react-redux";
import { setupListeners } from "@reduxjs/toolkit/query";
import { store } from "@/store";
import { ThemeProvider } from "@/components/providers/theme-provider";
import { AuthProvider } from "@/components/providers/auth-provider";
import { Toaster } from "sonner";

/**
 * Combined providers component.
 *
 * Wraps the application with:
 * - Redux Provider (for state management, including the auth slice)
 * - Auth Provider (session restoration, token refresh wiring, auth context)
 * - Theme Provider (for dark/light mode)
 * - Sonner Toaster (for toast notifications)
 */
export function Providers({ children }: { children: React.ReactNode }) {
  // Wire RTK Query's online/focus listeners once so
  // `refetchOnReconnect: true` (see `services/api.ts`) actually
  // re-fetches subscribed queries after a network reconnect —
  // the freshness safety net for `refetchOnMountOrArgChange: false`.
  // `setupListeners` returns its own unsubscribe, which React
  // uses as the effect cleanup. Effects only run in the browser,
  // so this is SSR-safe.
  React.useEffect(() => setupListeners(store.dispatch), []);

  return (
    <Provider store={store}>
      <ThemeProvider
        attribute="class"
        defaultTheme="system"
        enableSystem
        disableTransitionOnChange
      >
        <AuthProvider>
          {children}
          <Toaster
            position="top-right"
            richColors
            closeButton
            duration={4000}
            theme="system"
          />
        </AuthProvider>
      </ThemeProvider>
    </Provider>
  );
}
