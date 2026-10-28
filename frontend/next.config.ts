import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  reactStrictMode: true,
  poweredByHeader: false,
  experimental: {
    // Rewrites barrel imports to deep imports so only the icons actually used
    // are bundled. Icons are imported pervasively across every page.
    //
    // `radix-ui` is deliberately NOT on this list. Its published `exports`
    // map only exposes `.` (the barrel) and `./*` -> `./dist/*.mjs`, and there
    // is no `dist/select.mjs`, `dist/dialog.mjs`, `dist/sheet.mjs` etc. — those
    // components live inside `dist/index.mjs` alongside everything else. The
    // deep-import rewrite turns `import { Select } from "radix-ui"` into
    // `import { Select } from "radix-ui/select"`, which does not resolve, and
    // every Select/Dialog/Sheet/Popover in the app silently breaks (the
    // trigger renders but the dropdown/panel never mounts). Keep importing
    // through the barrel; the bundle is small enough that the win from
    // tree-shaking is not worth a broken UI.
    optimizePackageImports: ["next-themes", "lucide-react"],
    // Client-side Router Cache tuning.
    //
    // Every App Router navigation reads a cached RSC payload keyed by
    // `?_rsc=<cacheKey>` (hence the `?_rsc=...` requests in the Network tab).
    // If an entry is still fresh the router renders it instantly with no
    // network round-trip. Next.js defaults `dynamic` to 0, which means any
    // non-prerendered route refetches its payload on every single click.
    // These values keep recently-visited pages instant.
    staleTimes: {
      // Seconds a dynamic (non-prerendered) route stays usable from cache.
      dynamic: 30,
      // Seconds a statically prerendered route stays usable from cache.
      static: 300,
    },
    // optimizeCss: true requires 'critters' package - disabled for now
    // optimizeCss: true,
  },
  turbopack: {
    resolveAlias: {
      // Prevent duplicate bundling of commonly used packages
      "lucide-react": "lucide-react",
      "radix-ui": "radix-ui",
    },
  },
  async headers() {
    return [
      {
        source: "/:path*",
        headers: [
          { key: "X-Content-Type-Options", value: "nosniff" },
          { key: "X-Frame-Options", value: "DENY" },
          { key: "Referrer-Policy", value: "strict-origin-when-cross-origin" },
          { key: "Permissions-Policy", value: "camera=(), microphone=(), geolocation=()" },
        ],
      },
    ];
  },
};

export default nextConfig;
