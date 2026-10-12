/**
 * Shared formatting for the sales screens.
 *
 * Money, quantities and dates appear across seven tables and their detail sheets.
 * Centralising them keeps `1,234.50` from becoming `1234.5` in one place, and keeps
 * the IRD figures (VAT, totals) rendered identically everywhere a user reads them.
 */

/** Renders a money amount with its currency code, or a dash when absent. */
export function formatMoney(
  value: number | null | undefined,
  currency?: string | null
): string {
  if (value === null || value === undefined) return "—";
  const amount = Number(value);
  if (Number.isNaN(amount)) return "—";
  const formatted = amount.toLocaleString(undefined, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
  return currency ? `${currency} ${formatted}` : formatted;
}

/** Renders a quantity, keeping up to six decimals and dropping trailing noise. */
export function formatQty(value: number | null | undefined): string {
  if (value === null || value === undefined) return "—";
  const n = Number(value);
  if (Number.isNaN(n)) return "—";
  return n.toLocaleString(undefined, { maximumFractionDigits: 6 });
}

/** Renders an ISO date (yyyy-MM-dd) as a locale date, or a dash when absent. */
export function formatDate(value: string | null | undefined): string {
  if (!value) return "—";
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return value;
  return d.toLocaleDateString(undefined, {
    year: "numeric",
    month: "short",
    day: "2-digit",
  });
}
