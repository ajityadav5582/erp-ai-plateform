"use client";

import * as React from "react";

/**
 * localStorage key under which the active branch selection is persisted so
 * it survives page reloads and is restored by the app shell.
 */
export const ACTIVE_BRANCH_SELECTION_STORAGE_KEY = "erpai.activeBranchSelection";

/**
 * Shape of the active branch selection carried by the app shell header.
 *
 * The selected branch is chosen by the user in the topbar and is the
 * implicit context for all branch-scoped operations (sales orders, purchase
 * orders, POS items, etc.). When {@code branchId} is {@code null} the
 * request is treated as "all branches" and the backend filters by company
 * instead of a single branch.
 */
export interface BranchSelectionValue {
  branchId: number | null;
  branchName: string | null;
}

const BranchSelectionContext =
  React.createContext<BranchSelectionValue | null>(null);

/**
 * Provides the currently selected branch to descendant components.
 *
 * Must be mounted inside `AppLayout` so that every page rendered within the
 * app shell has access to the header-selected branch.
 */
export function BranchProvider({
  branch,
  children,
}: {
  branch: BranchSelectionValue | null;
  children: React.ReactNode;
}) {
  const value = React.useMemo<BranchSelectionValue | null>(() => branch, [branch]);

  return (
    <BranchSelectionContext.Provider value={value}>
      {children}
    </BranchSelectionContext.Provider>
  );
}

/**
 * Reads the currently selected branch from the app shell header.
 *
 * @throws {Error} if used outside of a `BranchProvider`.
 */
export function useBranchContext(): BranchSelectionValue | null {
  const ctx = React.useContext(BranchSelectionContext);
  if (!ctx) {
    throw new Error("useBranchContext must be used within a <BranchProvider>");
  }
  return ctx;
}

/** Convenience accessor for the selected branch id (or null). */
export function useSelectedBranch() {
  return useBranchContext();
}

/**
 * Returns the persisted branch selection from localStorage, or null when
 * nothing is selected or the stored value is malformed.
 *
 * Used by the Axios interceptor to attach the `X-Branch-Id` header on every
 * outgoing request while staying free of React/Redux dependencies.
 */
export function readActiveBranchSelection(): BranchSelectionValue | null {
  if (typeof window === "undefined") return null;

  try {
    const stored = window.localStorage.getItem(ACTIVE_BRANCH_SELECTION_STORAGE_KEY);
    if (!stored) return null;

    const candidate = JSON.parse(stored) as Partial<BranchSelectionValue>;
    return {
      branchId: candidate.branchId ?? null,
      branchName: candidate.branchName ?? null,
    };
  } catch {
    return null;
  }
}
