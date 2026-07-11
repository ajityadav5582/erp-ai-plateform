"use client";

import { ThemeProvider as NextThemesProvider } from "next-themes";
import type { ComponentProps } from "react";

/**
 * Application theme provider built on `next-themes`.
 *
 * Uses the `class` attribute strategy so Tailwind CSS v4's `@custom-variant dark`
 * can toggle dark mode via a `.dark` class on the <html> element. Themes are
 * persisted in `localStorage` and default to the system preference.
 */
export function ThemeProvider({
  children,
  ...props
}: ComponentProps<typeof NextThemesProvider>) {
  return (
    <NextThemesProvider
      attribute="class"
      defaultTheme="system"
      enableSystem
      disableTransitionOnChange
      {...props}
    >
      {children}
    </NextThemesProvider>
  );
}
