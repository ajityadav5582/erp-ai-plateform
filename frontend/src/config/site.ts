import type { Metadata } from "next";
import { getEnv } from "@/config/env";

/**
 * Centralized, type-safe site/metadata configuration consumed by the root
 * layout and any per-route metadata overrides.
 */

const appUrl = getEnv().NEXT_PUBLIC_APP_URL;

export const siteConfig = {
  name: "ERP AI Platform",
  shortName: "ERP AI",
  description:
    "Enterprise resource planning platform with embedded AI capabilities for finance, HR, inventory, manufacturing, procurement, and sales.",
  url: appUrl,
  locale: "en_US",
  themeColor: {
    light: "#ffffff",
    dark: "#0a0a0a",
  },
  authors: [{ name: "ERP AI Platform Team" }],
  keywords: [
    "ERP",
    "enterprise",
    "AI",
    "finance",
    "HR",
    "inventory",
    "manufacturing",
    "procurement",
    "sales",
  ],
  social: {
    twitter: "@erpaplatform",
  },
};

export type SiteConfig = typeof siteConfig;

/**
 * Default metadata applied at the root layout. Individual routes can extend or
 * override these values via their own `generateMetadata` or static `metadata`.
 */
export const baseMetadata: Metadata = {
  metadataBase: new URL(appUrl),
  title: {
    default: `${siteConfig.name} — Enterprise Operations, Augmented by AI`,
    template: `%s · ${siteConfig.name}`,
  },
  description: siteConfig.description,
  applicationName: siteConfig.name,
  authors: siteConfig.authors,
  keywords: siteConfig.keywords,
  creator: siteConfig.authors[0]?.name,
  publisher: siteConfig.name,
  formatDetection: {
    email: false,
    address: false,
    telephone: false,
  },
  alternates: {
    canonical: "/",
  },
  openGraph: {
    type: "website",
    locale: siteConfig.locale,
    url: appUrl,
    siteName: siteConfig.name,
    title: siteConfig.name,
    description: siteConfig.description,
  },
  twitter: {
    card: "summary_large_image",
    title: siteConfig.name,
    description: siteConfig.description,
    creator: siteConfig.social.twitter,
  },
  robots: {
    index: true,
    follow: true,
    googleBot: {
      index: true,
      follow: true,
      "max-image-preview": "large",
      "max-snippet": -1,
    },
  },
  category: "business",
};
