/**
 * Shared formatting for the POS screens.
 *
 * Money and timestamps appear across the terminal, the session/transaction
 * history and the register/quick-item admin tables. Centralising them keeps
 * `Rs 1,500.00` from drifting to `Rs 1500.0` in one place, and keeps the IRD
 * figures (VAT, totals, cash variance) rendered identically everywhere a user
 * reads them.
 */

/** Renders a money amount in NPR, or a dash when absent. */
export function formatPosCurrency(value: number | null | undefined): string {
  if (value === null || value === undefined) return "—";
  const amount = Number(value);
  if (Number.isNaN(amount)) return "—";
  return `Rs ${amount.toLocaleString(undefined, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`;
}

/** Renders a signed cash variance, e.g. `+Rs 20.00` / `−Rs 20.00` / `Rs 0.00`. */
export function formatPosVariance(value: number | null | undefined): string {
  if (value === null || value === undefined) return "—";
  const amount = Number(value);
  if (Number.isNaN(amount)) return "—";
  if (amount === 0) return formatPosCurrency(0);
  const sign = amount > 0 ? "+" : "−";
  return `${sign}${formatPosCurrency(Math.abs(amount))}`;
}

/** Renders an ISO timestamp as a locale date-time, or a dash when absent. */
export function formatPosDateTime(value: string | null | undefined): string {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString(undefined, {
    year: "numeric",
    month: "short",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}
