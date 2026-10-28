"use client";

import * as React from "react";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/utils";

/**
 * The thin bar along the top of the viewport that appears while the router is
 * moving between routes.
 *
 * The route-level `loading.tsx` files already stream a skeleton, and they cover
 * the *slow* navigation well. They say nothing at all about the fast one: when
 * the RSC payload has been prefetched, React commits the new page with no
 * fallback in between, and the user is left with a link that appears to do
 * nothing for the ~150ms the transition takes. This bar is the acknowledgement
 * for that case, and it starts on the click rather than on the arrival.
 *
 * Three behaviours do the real work, and all three are about *not* lying:
 *
 * 1. **It waits before appearing** (`APPEAR_AFTER_MS`). A navigation resolved
 *    from the prefetch cache finishes faster than this, and the bar never draws
 *    at all. Showing a progress indicator for work nobody perceived is noise.
 * 2. **It stays once shown** (`MIN_VISIBLE_MS`). A bar that appears and is
 *    yanked away 40ms later reads as a glitch rather than as progress, and it
 *    draws attention to the fact that nothing happened. So the visible duration
 *    has a floor, even when the route arrived long ago.
 * 3. **It creeps instead of guessing** (`creep()`). Nothing in the App Router
 *    reports how much of the payload has loaded, so the width is deliberately
 *    indeterminate: it advances in shrinking steps towards a ceiling it never
 *    reaches, then snaps to 100% on arrival. A bar that jumps 0 -> 100% when the
 *    page is already resolved is worse than no bar.
 *
 * Detection is deliberately conservative. `usePathname()` sees route changes but
 * not query-string-only ones, and a capture-phase click listener covers every
 * `<Link>` without any of them having to opt in. `history.pushState` is patched
 * as the completion signal: Next commits every navigation through it, so it
 * catches the programmatic `router.push` calls that never produce an anchor
 * click and stops the bar being left running if a navigation somehow finishes
 * without either of the other signals firing.
 *
 * `MAX_VISIBLE_MS` is the last line of defence. If all of the above is bypassed
 * - a click handler that calls `preventDefault`, an error boundary replacing the
 * route - the bar still retires itself instead of pinning across the top of the
 * screen forever.
 */

/** How long a navigation must run before the bar is drawn at all. */
const APPEAR_AFTER_MS = 180;

/** Floor on how long the bar stays on screen once drawn. */
const MIN_VISIBLE_MS = 420;

/** Time the 100% fill is given to animate before the bar is removed. */
const SETTLE_MS = 160;

/** Cadence of the indeterminate creep. */
const CREEP_INTERVAL_MS = 200;

/** Failsafe: the bar can never outlive a navigation, however it ended. */
const MAX_VISIBLE_MS = 15_000;

/** Width the bar is born at, as a percentage. */
const START_PERCENT = 12;

/**
 * The creep asymptote. It never reaches 100% on its own, so the jump to full on
 * arrival stays a readable "that was the end of it" rather than something the
 * bar had already telegraphed.
 */
const CEILING_PERCENT = 88;

/**
 * `waiting` - a navigation is in flight but the bar has not earned the right to
 * be visible yet. `running` - drawn. `leaving` - arrived, finishing its animation.
 */
type Phase = "idle" | "waiting" | "running" | "leaving";

/**
 * Tracks the OS-level "reduce motion" setting.
 *
 * Kept separate from the component because it is also needed as a ref inside
 * `start()`: that callback is installed as a document listener and must keep its
 * identity for as long as the page lives, otherwise every re-render would tear
 * the listener down and re-add it. The subscription updates on change because a
 * user can flip the setting mid-session.
 */
function usePrefersReducedMotion(): boolean {
  const [reduceMotion, setReduceMotion] = React.useState(false);

  React.useEffect(() => {
    const query = window.matchMedia("(prefers-reduced-motion: reduce)");
    setReduceMotion(query.matches);

    const handleChange = (event: MediaQueryListEvent) => setReduceMotion(event.matches);
    query.addEventListener("change", handleChange);
    return () => query.removeEventListener("change", handleChange);
  }, []);

  return reduceMotion;
}

/**
 * True for anchor activations the router will actually navigate for.
 *
 * Filters out everything the browser or the user intends to handle some other
 * way - a new tab, a download, an external host, a bare fragment, a
 * middle-click - because starting the bar for any of those would leave it
 * running until `MAX_VISIBLE_MS` expired.
 */
function isInternalNavigation(event: MouseEvent): boolean {
  if (event.defaultPrevented || event.button !== 0) return false;
  if (event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return false;

  const target = event.target;
  if (!(target instanceof Element)) return false;

  const anchor = target.closest("a");
  if (!anchor) return false;
  if (anchor.hasAttribute("download")) return false;

  const anchorTarget = anchor.getAttribute("target");
  if (anchorTarget && anchorTarget !== "_self") return false;

  const href = anchor.getAttribute("href");
  if (!href) return false;
  if (href.startsWith("#")) return false;
  if (/^[a-z][a-z0-9+.-]*:/i.test(href) && !href.startsWith(window.location.protocol)) {
    // mailto:, tel:, javascript: and any other custom scheme.
    return false;
  }

  let url: URL;
  try {
    url = new URL(href, window.location.href);
  } catch {
    return false;
  }
  if (url.origin !== window.location.origin) return false;

  // Clicking the page you are already on resolves instantly and changes nothing.
  return (
    url.pathname !== window.location.pathname || url.search !== window.location.search
  );
}

export function NavigationProgress({ className }: { className?: string }) {
  const pathname = usePathname();
  const reduceMotion = usePrefersReducedMotion();

  const [phase, setPhase] = React.useState<Phase>("idle");
  const [progress, setProgress] = React.useState(0);

  /**
   * Mutable mirrors of the state above.
   *
   * The scheduling callbacks below are registered as window/document listeners,
   * so they outlive many renders. Reading `phase` from a closure would make them
   * act on a value from whenever they were created, which is exactly how a
   * progress bar ends up stuck.
   */
  const phaseRef = React.useRef<Phase>("idle");
  const shownAtRef = React.useRef(0);
  const timeoutIdsRef = React.useRef<ReturnType<typeof setTimeout>[]>([]);
  const creepIntervalRef = React.useRef<ReturnType<typeof setInterval> | null>(null);
  const reduceMotionRef = React.useRef(false);

  React.useEffect(() => {
    reduceMotionRef.current = reduceMotion;
  }, [reduceMotion]);

  const setPhaseSafely = React.useCallback((next: Phase) => {
    phaseRef.current = next;
    setPhase(next);
  }, []);

  /** Cancels every pending timer so a superseded navigation cannot resurrect. */
  const cancelScheduledWork = React.useCallback(() => {
    timeoutIdsRef.current.forEach(clearTimeout);
    timeoutIdsRef.current = [];
    if (creepIntervalRef.current !== null) {
      clearInterval(creepIntervalRef.current);
      creepIntervalRef.current = null;
    }
  }, []);

  /** Advance the bar. Steps shrink as it approaches the ceiling, so it decelerates. */
  const creep = React.useCallback(() => {
    setProgress((current) =>
      current >= CEILING_PERCENT
        ? current
        : current + (CEILING_PERCENT - current) * 0.18 + 1.5
    );
  }, []);

  const finish = React.useCallback(() => {
    if (phaseRef.current === "idle") return;
    cancelScheduledWork();

    // Finished inside the delay window: the navigation was quick enough that the
    // bar never drew, and drawing it now would be feedback for work nobody saw.
    if (phaseRef.current === "waiting") {
      setPhaseSafely("idle");
      return;
    }

    const visibleFor = Date.now() - shownAtRef.current;
    const stillToShow = Math.max(0, MIN_VISIBLE_MS - visibleFor);

    setPhaseSafely("leaving");
    setProgress(100);
    timeoutIdsRef.current.push(
      setTimeout(() => setPhaseSafely("idle"), stillToShow + SETTLE_MS)
    );
  }, [cancelScheduledWork, setPhaseSafely]);

  const start = React.useCallback(() => {
    // A bar that is already on screen does not need restarting; one that is
    // animating out should not be yanked back by the next click either.
    if (phaseRef.current === "running" || phaseRef.current === "leaving") return;

    cancelScheduledWork();
    setProgress(0);
    setPhaseSafely("waiting");

    timeoutIdsRef.current.push(
      setTimeout(() => {
        shownAtRef.current = Date.now();
        setPhaseSafely("running");
        // Materialising at zero would make the bar look like it was filling from
        // nothing, so it opens at a width that already reads as "under way".
        setProgress(START_PERCENT);
        // The creep *is* the feedback under reduced motion: it tells the user
        // the app is working rather than stuck. A static bar would not, so the
        // sheen and the deceleration are dropped but the movement stays.
        creepIntervalRef.current = setInterval(creep, CREEP_INTERVAL_MS);
      }, APPEAR_AFTER_MS)
    );

    timeoutIdsRef.current.push(setTimeout(finish, MAX_VISIBLE_MS));
  }, [cancelScheduledWork, creep, finish, setPhaseSafely]);

  // Route committed: this is the completion signal, and it is also what makes a
  // navigation that never produced a click (a `router.push` from a button) at
  // least retire its bar.
  React.useEffect(() => {
    finish();
  }, [pathname, finish]);

  // Query-string-only navigations leave `usePathname()` untouched, so the History
  // API is patched for the commit. Applied after the original call so the router's
  // own work is never delayed by this listener.
  React.useEffect(() => {
    const originalPushState = window.history.pushState;
    const originalReplaceState = window.history.replaceState;

    window.history.pushState = function pushState(
      this: History,
      ...args: Parameters<History["pushState"]>
    ) {
      const result = originalPushState.apply(this, args);
      finish();
      return result;
    };

    window.history.replaceState = function replaceState(
      this: History,
      ...args: Parameters<History["replaceState"]>
    ) {
      const result = originalReplaceState.apply(this, args);
      finish();
      return result;
    };

    return () => {
      window.history.pushState = originalPushState;
      window.history.replaceState = originalReplaceState;
    };
  }, [finish]);

  // Start on link activation and on back/forward. Capture phase, so a link
  // inside a sheet or dropdown that stops propagation still counts.
  React.useEffect(() => {
    const handleClick = (event: MouseEvent) => {
      if (isInternalNavigation(event)) start();
    };
    const handlePopState = () => start();

    document.addEventListener("click", handleClick, true);
    window.addEventListener("popstate", handlePopState);
    return () => {
      document.removeEventListener("click", handleClick, true);
      window.removeEventListener("popstate", handlePopState);
    };
  }, [start]);

  // Nothing is scheduled outside a navigation, but a pending interval would keep
  // a torn-down component re-rendering.
  React.useEffect(() => cancelScheduledWork, [cancelScheduledWork]);

  if (phase === "idle") return null;

  return (
    <>
      <div
        role="progressbar"
        aria-label="Loading page"
        aria-busy={phase !== "leaving"}
        aria-valuemin={0}
        aria-valuemax={100}
        aria-valuenow={Math.round(progress)}
        className={cn(
          // Fixed to the viewport rather than the app shell: the bar must not
          // inherit the inset main's margins, rounded corners or transform,
          // and it must stay above the sticky header rather than under it.
          "pointer-events-none fixed inset-x-0 top-0 z-[100] h-0.5 overflow-hidden",
          className
        )}
      >
        <div
          className={cn(
            "h-full w-full origin-left bg-primary",
            reduceMotion
              ? "transition-none"
              : "transition-transform duration-300 ease-out motion-reduce:transition-none"
          )}
          style={{ transform: `scaleX(${progress / 100})` }}
        >
          {!reduceMotion && (
            <span
              aria-hidden="true"
              className="absolute inset-y-0 left-0 w-1/3 animate-nav-progress-sheen bg-gradient-to-r from-transparent via-white/50 to-transparent"
            />
          )}
        </div>
      </div>

      {/* The bar itself is decorative; this is what actually gets announced. */}
      <p role="status" aria-live="polite" className="sr-only">
        Loading page
      </p>
    </>
  );
}
