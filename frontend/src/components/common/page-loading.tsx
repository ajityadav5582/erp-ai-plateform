"use client";

import * as React from "react";
import { Loader2 } from "lucide-react";
import { cn } from "@/lib/utils";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Table,
  TableBody,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { TableSkeletonRows } from "@/components/common/table-state";

/**
 * The one place loading UI is defined.
 *
 * Loading had grown seven unrelated implementations: two hand-written skeletons in
 * `loading.tsx` files, an inline two-blob fallback on the orders page, per-column
 * skeleton arrays copy-pasted into eight inventory tables, a centred spinner pasted
 * into six detail sheets, and plain-text "Loading…" cards on two others. They did
 * not match each other in padding, height, or even animation - the route skeleton
 * used `mx-auto max-w-7xl px-4` while the pages it stood in for used
 * `flex flex-1 p-4 sm:p-6 lg:p-8`, so every navigation visibly re-laid-out.
 *
 * These components are structural rather than decorative: `PageLoader` builds a real
 * `Table` with the same wrapper classes as a list page, so the placeholder occupies
 * the space the loaded table will and nothing jumps when data arrives.
 *
 * Which one to use:
 *
 * | Situation | Component |
 * |---|---|
 * | Route-level Suspense fallback (`loading.tsx`, inline `<Suspense>`) | `PageLoader` |
 * | Moving between routes, especially a cached one that never suspends | `NavigationProgress` |
 * | Table body awaiting its first page | `TableSkeletonRows` (from `table-state`) |
 * | Sheet or dialog awaiting its record | `SheetLoader` |
 * | A select, picker or small region awaiting data | `InlineLoader` |
 */

/** Standard list-page geometry. Every page under `(protected)` uses exactly this. */
const PAGE_SHELL = "flex flex-1 flex-col space-y-6 p-4 sm:p-6 lg:p-8";

/**
 * Wraps any loader in the status semantics a screen reader needs.
 *
 * `role="status"` plus `aria-live="polite"` announces the wait without stealing focus,
 * and `aria-busy` lets assistive tech mark the region as still settling. The visible
 * text is hidden because the skeleton is decorative - a screen reader user is told
 * "Loading" once, not once per grey rectangle.
 */
function StatusRegion({
  label,
  className,
  children,
}: {
  label: string;
  className?: string;
  children: React.ReactNode;
}) {
  return (
    <div
      role="status"
      aria-live="polite"
      aria-busy="true"
      aria-label={label}
      className={className}
    >
      <span className="sr-only">{label}</span>
      {children}
    </div>
  );
}

/**
 * The page-level loader for a list screen.
 *
 * `variant="list"` is the default and mirrors a list page exactly: heading, filter
 * toolbar, then the table card. `variant="table"` omits the heading and toolbar for
 * pages that render into a shell of their own.
 */
export function PageLoader({
  variant = "list",
  columns = 6,
  rows = 8,
  showToolbar,
  showTabs,
  label = "Loading page…",
  className,
}: {
  variant?: "list" | "table";
  columns?: number;
  rows?: number;
  /** Defaults to `true` for `list`, `false` for `table`. */
  showToolbar?: boolean;
  showTabs?: boolean;
  label?: string;
  className?: string;
}) {
  const withToolbar = showToolbar ?? variant === "list";

  return (
    <StatusRegion label={label} className={cn(PAGE_SHELL, className)}>
      {variant === "list" && (
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex flex-col gap-2">
            <Skeleton className="h-8 w-56" />
            <Skeleton className="h-4 w-80 max-w-full" />
          </div>
          <Skeleton className="h-9 w-32" />
        </div>
      )}

      {showTabs && (
        <div className="flex flex-wrap gap-1 rounded-xl border border-border bg-card p-1 shadow-sm">
          {Array.from({ length: 3 }).map((_, index) => (
            <Skeleton key={index} className="h-7 w-28" />
          ))}
        </div>
      )}

      {withToolbar && (
        <div className="flex flex-col gap-3 rounded-xl border border-border bg-card p-3 shadow-sm sm:flex-row sm:items-center sm:justify-between">
          <Skeleton className="h-9 w-64" />
          <Skeleton className="h-9 w-28" />
        </div>
      )}

      {/* Same wrapper classes as the live table card, so nothing shifts on arrival. */}
      <div className="overflow-hidden rounded-xl border border-border bg-card shadow-sm">
        <Table>
          <TableHeader className="bg-muted/40">
            <TableRow>
              {Array.from({ length: columns }).map((_, index) => (
                <TableHead key={index}>
                  <Skeleton className="h-3.5 w-24" />
                </TableHead>
              ))}
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableSkeletonRows columns={columns} rows={rows} />
          </TableBody>
        </Table>
      </div>
    </StatusRegion>
  );
}

/**
 * The page-level loader for the public (unauthenticated) routes.
 *
 * Those pages are marketing-style: prose and a card grid, no table. Kept separate
 * from `PageLoader` so the list geometry never leaks onto the sign-in screens, which
 * sit outside the app shell entirely.
 */
export function MarketingPageLoader({
  label = "Loading page…",
}: {
  label?: string;
}) {
  return (
    <StatusRegion
      label={label}
      className="mx-auto w-full max-w-7xl px-4 py-16 sm:px-6 lg:px-8"
    >
      <div className="flex flex-col gap-6">
        <Skeleton className="h-10 w-2/3" />
        <Skeleton className="h-4 w-full max-w-2xl" />
        <Skeleton className="h-4 w-5/6 max-w-2xl" />
        <div className="mt-8 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 6 }).map((_, index) => (
            <Skeleton key={index} className="h-40 rounded-lg border border-border" />
          ))}
        </div>
      </div>
    </StatusRegion>
  );
}

/**
 * The loader for a sheet or dialog awaiting its record.
 *
 * Reserving `height` matters more here than anywhere else: the sheet sizes itself to
 * its content, so an unreserved spinner collapses the panel to a sliver and the whole
 * dialog jumps taller a moment later.
 */
export function SheetLoader({
  label = "Loading details…",
  height = "h-64",
  className,
}: {
  label?: string;
  height?: string;
  className?: string;
}) {
  return (
    <StatusRegion
      label={label}
      className={cn(
        "flex items-center justify-center gap-2 text-sm text-muted-foreground",
        height,
        className
      )}
    >
      <Loader2 className="size-5 animate-spin" aria-hidden="true" />
      <span>{label}</span>
    </StatusRegion>
  );
}

/**
 * A one-line loader for a small region: a supplier picker, a balance, a branch list.
 *
 * Named text rather than a bare spinner because "Loading suppliers…" tells the user
 * *what* is taking the time; an unlabelled spinner next to empty space just looks
 * broken.
 */
export function InlineLoader({
  label = "Loading…",
  className,
}: {
  label?: string;
  className?: string;
}) {
  return (
    <StatusRegion
      label={label}
      className={cn(
        "flex items-center justify-center gap-2 py-6 text-sm text-muted-foreground",
        className
      )}
    >
      <Loader2 className="size-4 animate-spin" aria-hidden="true" />
      <span>{label}</span>
    </StatusRegion>
  );
}

/**
 * A centred loader with a caption, for a whole route or panel that is not a list.
 *
 * Used where there is no structure worth mimicking - verifying a session, loading
 * companies - so a skeleton would be a lie about content that is not coming.
 */
export function CenteredLoader({
  label = "Loading…",
  className,
  minHeight = "min-h-[50vh]",
}: {
  label?: string;
  className?: string;
  minHeight?: string;
}) {
  return (
    <StatusRegion
      label={label}
      className={cn(
        "flex items-center justify-center",
        minHeight,
        className
      )}
    >
      <div className="flex flex-col items-center gap-3 text-muted-foreground">
        <Loader2 className="size-6 animate-spin" aria-hidden="true" />
        <p className="text-sm">{label}</p>
      </div>
    </StatusRegion>
  );
}
