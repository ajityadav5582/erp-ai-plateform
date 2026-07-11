"use client";

import { useEffect } from "react";
import { Button } from "@/components/ui/button";

/**
 * Route-level error boundary. Catches errors thrown during rendering of a
 * segment and its children, showing a recoverable UI with a reset action.
 */
export default function Error({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    // Replace with your observability/error-reporting integration.
    console.error("Route error:", error);
  }, [error]);

  return (
    <div className="mx-auto flex max-w-md flex-col items-center gap-6 px-4 py-24 text-center">
      <h1 className="text-2xl font-semibold text-foreground">
        Something went wrong
      </h1>
      <p className="text-muted-foreground">
        An unexpected error occurred while rendering this page. You can try
        again or return to the homepage.
      </p>
      {error.digest ? (
        <p className="text-xs text-muted-foreground">Error reference: {error.digest}</p>
      ) : null}
      <div className="flex items-center gap-3">
        <Button onClick={reset}>Try again</Button>
        <Button variant="outline" onClick={() => (window.location.href = "/")}>
          Go home
        </Button>
      </div>
    </div>
  );
}
