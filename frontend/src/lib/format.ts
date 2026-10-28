/**
 * Shared formatting helpers used across all modules.
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
