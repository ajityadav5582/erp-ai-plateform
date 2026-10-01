/**
 * Reads the active company / fiscal-year selection outside of React.
 *
 * The app shell (`AppLayout`) persists the user's selection in localStorage.
 * The Axios request interceptor needs the same value to attach the
 * `X-Company-Id` / `X-Fiscal-Year-Id` headers on every outgoing request, and
 * the interceptor must stay free of React/Redux dependencies, so it reads
 * localStorage directly through this module.
 */

/** localStorage key holding the active company + fiscal year selection. */
export const ACTIVE_FISCAL_SELECTION_STORAGE_KEY = "erpai.activeFiscalSelection";

/** The company/fiscal-year pair the user is currently working in. */
export interface ActiveFiscalSelection {
  companyId: number;
  fiscalYearId: number;
  companyName: string;
  fiscalYearName: string;
}

/**
 * Returns the persisted selection, or null when nothing is selected or the
 * stored value is malformed.
 */
export function readActiveFiscalSelection(): ActiveFiscalSelection | null {
  if (typeof window === "undefined") return null;

  try {
    const stored = window.localStorage.getItem(ACTIVE_FISCAL_SELECTION_STORAGE_KEY);
    if (!stored) return null;

    const candidate = JSON.parse(stored) as Partial<ActiveFiscalSelection>;
    if (!candidate.companyId || !candidate.fiscalYearId) return null;

    return {
      companyId: candidate.companyId,
      fiscalYearId: candidate.fiscalYearId,
      companyName: candidate.companyName || "Selected company",
      fiscalYearName: candidate.fiscalYearName || "Active fiscal year",
    };
  } catch {
    return null;
  }
}
