/**
 * Route-level loading UI. Rendered via React Suspense while the segment's
 * server components are streaming. Uses a skeleton to avoid layout shift.
 */
export default function Loading() {
  return (
    <div
      role="status"
      aria-live="polite"
      aria-busy="true"
      className="mx-auto max-w-7xl px-4 py-16 sm:px-6 lg:px-8"
    >
      <span className="sr-only">Loading…</span>
      <div className="flex flex-col gap-6">
        <div className="h-10 w-2/3 animate-pulse rounded-md bg-muted" />
        <div className="h-4 w-full max-w-2xl animate-pulse rounded-md bg-muted" />
        <div className="h-4 w-5/6 max-w-2xl animate-pulse rounded-md bg-muted" />
        <div className="mt-8 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 6 }).map((_, index) => (
            <div
              key={index}
              className="h-40 animate-pulse rounded-lg border border-border bg-card"
            />
          ))}
        </div>
      </div>
    </div>
  );
}
