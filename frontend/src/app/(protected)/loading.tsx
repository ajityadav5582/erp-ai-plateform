import { PageLoader } from "@/components/common/page-loading";

/**
 * Loading boundary for the authenticated route group.
 *
 * Without a `loading.tsx` at this level, the only Suspense boundary sits at
 * the app root, so a client-side navigation must download the *entire* RSC
 * payload for the incoming route before React can commit the transition.
 *
 * Declaring it here creates a boundary one level below the root, which lets
 * the router prefetch only the shell and stream the rest — shrinking the
 * `?_rsc=` payload that every navigation fetches.
 *
 * The markup is `PageLoader`'s so this boundary and the pages it stands in for share
 * one set of dimensions. It previously used `mx-auto max-w-7xl px-4` while the list
 * pages used `flex flex-1 p-4 sm:p-6 lg:p-8`, so every navigation visibly re-laid-out
 * the content as it arrived.
 *
 * No `showTabs` here: this boundary stands in for every authenticated page, and
 * none of them render a tab strip, so drawing one made the skeleton taller than
 * the page it was about to be replaced by.
 *
 * This boundary only appears on a navigation slow enough to stream. The fast,
 * cache-served one never suspends and so never renders this — that case is
 * acknowledged by `NavigationProgress` at the top of the viewport instead.
 */
export default function ProtectedLoading() {
  return <PageLoader rows={8} columns={6} />;
}
