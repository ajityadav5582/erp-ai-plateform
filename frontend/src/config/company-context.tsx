"use client";

import * as React from "react";

/**
 * localStorage key under which the active company + fiscal year selection is
 * persisted so it survives page reloads and is restored by the app shell.
 */
export const ACTIVE_FISCAL_SELECTION_STORAGE_KEY = "erpai.activeFiscalSelection";

/**
 * Shape of the active company selection carried by the app shell header.
 *
 * The selected company is chosen by the user in the header/sidebar and is
 * the implicit context for all company-scoped operations (e.g. creating or
 * updating categories) instead of requiring a per-form company dropdown.
 */
export interface CompanyContextValue {
  /** The currently selected company id, or null while loading/none selected. */
  company: { id: number; name: string } | null;
}

const CompanyContext = React.createContext<CompanyContextValue | null>(null);

/**
 * Provides the currently selected company to descendant components.
 *
 * Must be mounted inside `AppLayout` so that every page rendered within the
 * app shell has access to the header-selected company.
 */
export function CompanyProvider({
  company,
  children,
}: {
  company: CompanyContextValue["company"];
  children: React.ReactNode;
}) {
  const value = React.useMemo<CompanyContextValue>(
    () => ({ company }),
    [company]
  );

  return (
    <CompanyContext.Provider value={value}>{children}</CompanyContext.Provider>
  );
}

/**
 * Reads the currently selected company from the app shell header.
 *
 * @throws {Error} if used outside of a `CompanyProvider`.
 */
export function useCompanyContext(): CompanyContextValue {
  const ctx = React.useContext(CompanyContext);
  if (!ctx) {
    throw new Error("useCompanyContext must be used within a <CompanyProvider>");
  }
  return ctx;
}

/** Convenience accessor for the selected company id (or null). */
export function useSelectedCompany() {
  return useCompanyContext().company;
}
