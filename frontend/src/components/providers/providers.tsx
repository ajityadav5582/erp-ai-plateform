"use client";

import { Provider } from "react-redux";
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
