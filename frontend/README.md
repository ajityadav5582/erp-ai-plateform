# ERP AI Platform — Frontend

Enterprise-grade frontend for the **ERP AI Platform**, built with the modern
Next.js App Router stack.

## Tech Stack

| Concern            | Choice                                              |
| ------------------ | --------------------------------------------------- |
| Framework          | Next.js 15 (App Router)                             |
| UI Library         | React 19                                            |
| Language           | TypeScript (strict)                                |
| Styling            | Tailwind CSS v4 (`@tailwindcss/postcss`)            |
| Linting            | ESLint 9 (flat config) + `eslint-config-next`       |
| Formatting         | Prettier 3 (+ `prettier-plugin-tailwindcss`)        |
| Theming            | `next-themes` (class-based dark mode)              |
| Env Validation     | Zod                                                |
| Output             | Standalone (Docker-ready)                          |

> Authentication is intentionally **not** implemented in this scaffold.

## Folder Structure

```text
frontend/
├── .env.example            # Environment variable template
├── .prettierrc.json        # Prettier configuration
├── .prettierignore
├── eslint.config.mjs       # ESLint flat config
├── next.config.ts          # Next.js configuration (headers, standalone)
├── postcss.config.mjs      # Tailwind CSS v4 PostCSS plugin
├── tsconfig.json           # TypeScript + path aliases (@/*)
├── Dockerfile              # Multi-stage production image
├── public/                 # Static assets
└── src/
    ├── app/                # App Router segments & routes
    │   ├── layout.tsx      # Root layout (metadata, providers, header/footer)
    │   ├── page.tsx        # Home page
    │   ├── error.tsx       # Route-level error boundary
    │   ├── global-error.tsx# Root-level error boundary
    │   ├── loading.tsx     # Route-level loading UI (Suspense)
    │   ├── not-found.tsx   # 404 page
    │   ├── manifest.ts     # Web app manifest
    │   ├── robots.ts       # robots.txt
    │   ├── sitemap.ts      # sitemap.xml
    │   └── icon.svg        # App icon / favicon
    ├── components/
    │   ├── layout/         # SiteHeader, SiteFooter
    │   ├── providers/      # ThemeProvider
    │   ├── ui/             # Reusable primitives (Button)
    │   └── theme-toggle.tsx
    ├── config/
    │   ├── env.ts          # Zod-validated environment access
    │   └── site.ts         # Site/metadata configuration
    ├── lib/
    │   └── utils.ts        # cn(), formatters
    ├── styles/
    │   └── globals.css     # Tailwind v4 entry + design tokens
    └── types/              # Shared TypeScript types
```

## Path Aliases (Absolute Imports)

Configured in [`tsconfig.json`](tsconfig.json):

```ts
import { Button } from "@/components/ui/button";
import { getEnv } from "@/config/env";
```

`@/*` maps to `src/*`.

## Environment Configuration

Copy the template and adjust values:

```bash
cp .env.example .env.local
```

Variables are validated at startup via [`src/config/env.ts`](src/config/env.ts)
using Zod. Public (browser-exposed) variables use the `NEXT_PUBLIC_` prefix;
server-only variables are never bundled into the client.

| Variable                       | Scope   | Default                       | Description              |
| ------------------------------ | ------- | ----------------------------- | ------------------------ |
| `NEXT_PUBLIC_APP_URL`          | Public  | `http://localhost:3000`       | Canonical app URL        |
| `NEXT_PUBLIC_ENABLE_DEBUG_LOGGING` | Public | `false`                | Verbose client logging   |
| `NEXT_PUBLIC_ANALYTICS_KEY`    | Public  | _(empty)_                     | Analytics project key    |
| `NODE_ENV`                     | Server  | `development`                 | Deployment environment   |
| `API_BASE_URL`                 | Server  | `http://localhost:8080/api/v1`| Backend API base URL     |
| `API_TIMEOUT_MS`               | Server  | `30000`                       | API request timeout      |

## Scripts

```bash
npm run dev        # Start the dev server (http://localhost:3000)
npm run build      # Production build
npm run start      # Start the production server
npm run lint       # ESLint
npm run lint:fix   # ESLint with --fix
npm run format     # Prettier write
npm run format:check # Prettier check (CI)
npm run typecheck  # tsc --noEmit
```

## Conventions

- **Server vs Client Components:** Components are Server Components by default.
  Add `"use client"` only when interactivity (state, effects, event handlers)
  is required (e.g. `theme-toggle.tsx`, `error.tsx`).
- **Styling:** Utility classes come from Tailwind v4 design tokens defined in
  `globals.css` (`@theme`). Use the `cn()` helper from `@/lib/utils` to merge
  conditional classes.
- **Metadata:** Centralized in `src/config/site.ts` and applied in the root
  `layout.tsx`. Per-route overrides use `metadata` / `generateMetadata`.
- **Error Handling:** `error.tsx` (route-level) and `global-error.tsx`
  (root-level) provide recoverable UIs. `not-found.tsx` handles 404s.
- **Loading:** `loading.tsx` renders a skeleton via React Suspense while a
  segment streams.

## Docker

Build and run the standalone production image:

```bash
docker build -t erp-ai-frontend .
docker run -p 3000:3000 \
  -e NEXT_PUBLIC_APP_URL=https://app.example.com \
  -e API_BASE_URL=https://api.example.com/api/v1 \
  erp-ai-frontend
```

The image uses the `output: "standalone"` build for a minimal runtime.
