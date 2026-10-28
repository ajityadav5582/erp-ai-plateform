"use client";

import * as React from "react";

/**
 * Guards a form/dialog against accidental data loss.
 *
 * Returns a `markDirty`/`markClean` pair plus a `guardClose` wrapper that
 * prompts the user (via `window.confirm`) before discarding unsaved changes.
 * Also registers a `beforeunload` handler while dirty so a tab close or
 * navigation away does not silently drop the in-progress work.
 *
 * Usage:
 *   const { isDirty, markDirty, markClean, guardClose } = useUnsavedChanges();
 *   // on field change: markDirty()
 *   // on successful submit: markClean()
 *   // on dialog close: if (!guardClose()) return;  // aborted
 */
export function useUnsavedChanges(message = "You have unsaved changes. Discard them?") {
  const [isDirty, setIsDirty] = React.useState(false);

  React.useEffect(() => {
    if (!isDirty) return;
    const handler = (event: BeforeUnloadEvent) => {
      event.preventDefault();
      event.returnValue = message;
      return message;
    };
    window.addEventListener("beforeunload", handler);
    return () => window.removeEventListener("beforeunload", handler);
  }, [isDirty, message]);

  const markDirty = React.useCallback(() => setIsDirty(true), []);
  const markClean = React.useCallback(() => setIsDirty(false), []);

  const guardClose = React.useCallback(() => {
    if (!isDirty) return true;
    const ok = window.confirm(message);
    if (ok) setIsDirty(false);
    return ok;
  }, [isDirty, message]);

  return { isDirty, markDirty, markClean, guardClose };
}
