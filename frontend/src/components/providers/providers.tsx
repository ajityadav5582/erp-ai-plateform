"use client";

import { Provider } from "react-redux";
import { makeStore } from "@/store";
import { ThemeProvider } from "@/components/providers/theme-provider";
import { Toaster } from "sonner";

/**
 * Combined providers component.
 *
 * Wraps the application with:
 * - Redux Provider (for state management)
 * - Theme Provider (for dark/light mode)
 * - Sonner Toaster (for toast notifications)
 */
export function Providers({ children }: { children: React.ReactNode }) {
  // Note: In Next.js App Router, we create the store on the client side.
  // For SSR/SSG compatibility, we use a lazy initialization pattern.
  // The store is created once per client session.
  const store = makeStore();

  return (
    <Provider store={store}>
      <ThemeProvider
        attribute="class"
        defaultTheme="system"
        enableSystem
        disableTransitionOnChange
      >
        {children}
        <Toaster
          position="top-right"
          richColors
          closeButton
          duration={4000}
          theme="system"
        />
      </ThemeProvider>
    </Provider>
  );
}
