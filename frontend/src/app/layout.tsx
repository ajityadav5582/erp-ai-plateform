import type { Metadata, Viewport } from "next";
import { Providers } from "@/components/providers/providers";
import { AppLayout } from "@/components/layout/app-layout";
import { baseMetadata, siteConfig } from "@/config/site";
import { TooltipProvider } from "@/components/ui/tooltip";
import "@/styles/globals.css";

export const metadata: Metadata = baseMetadata;

export const viewport: Viewport = {
  themeColor: [
    { media: "(prefers-color-scheme: light)", color: siteConfig.themeColor.light },
    { media: "(prefers-color-scheme: dark)", color: siteConfig.themeColor.dark },
  ],
  width: "device-width",
  initialScale: 1,
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body className="min-h-screen bg-background font-sans text-foreground antialiased">
        <Providers>
          <TooltipProvider>
            <a
              href="#main-content"
              className="sr-only focus:not-sr-only focus:absolute focus:left-4 focus:top-4 focus:z-50 focus:rounded-md focus:bg-primary focus:px-4 focus:py-2 focus:text-primary-foreground"
            >
              Skip to content
            </a>
            <AppLayout>
              {children}
            </AppLayout>
          </TooltipProvider>
        </Providers>
      </body>
    </html>
  );
}
