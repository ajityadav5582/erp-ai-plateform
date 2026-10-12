import { MarketingPageLoader } from "@/components/common/page-loading";

/**
 * Route-level loading UI for the public (unauthenticated) routes.
 *
 * Rendered via React Suspense while the segment's server components stream. Those
 * routes are marketing-style - prose and a card grid - so they use
 * `MarketingPageLoader` rather than the list-page skeleton, which would imply a table
 * that is never coming.
 */
export default function Loading() {
  return <MarketingPageLoader />;
}
