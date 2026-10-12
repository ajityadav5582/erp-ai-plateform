/**
 * Shared formatting for the procurement screens.
 *
 * Money, quantities and dates appear in four tables and four detail sheets. Each one
 * formatting its own would produce the same number as `1,234.50` in one place and
 * `1234.5` in another, which is exactly the kind of drift that makes a user stop
 * trusting the figures on screen.
 *
 * The rules encoded here:
 * - **Money always carries its currency.** A bare amount in a column headed "Amount"
 *   is ambiguous the moment a company trades in more than one currency, and the
 *   currency is only available from the document.
 * - **Quantities are not rounded to integers.** Stock is counted in decimals - 12.5 kg
 *   of rebar, 0.25 of a machine hour - so the display keeps up to six decimals and
 *   drops the noise beyond that.
 * - **Missing is "—", never "0" or blank.** A zero says "nothing was received"; a dash
 *   says "there is no value here". Those are different facts and the table must not
 *   conflate them.
 */

/** Renders a money amount with its currency code, or a dash when absent. */
export function formatMoney(
  value: number | null | undefined,
  currency: string | null | undefined
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

/** Renders a money amount without a currency, for columns headed with the currency. */
export function formatAmount(value: number | null | undefined): string {
  return formatMoney(value, null);
}

/**
 * Sums amounts that may be denominated in more than one currency, keeping them apart.
 *
 * Adding across currencies produces a number that means nothing: "NPR 100,000 plus USD
 * 900" is not a figure any ledger can settle against, and a total like that is worse
 * than no total at all because it looks authoritative. Grouping by currency gives one
 * honest total per currency instead, and a single-currency company still gets the plain
 * single total it expects.
 *
 * Non-finite amounts are skipped rather than poisoning the sum with `NaN`.
 */
export function sumByCurrency(
  entries: { currency?: string | null; amount: number | null | undefined }[]
): { currency: string | null; amount: number }[] {
  const totals = new Map<string | null, number>();
  for (const entry of entries) {
    const value = Number(entry.amount ?? 0);
    if (!Number.isFinite(value)) continue;
    const key = entry.currency ?? null;
    totals.set(key, (totals.get(key) ?? 0) + value);
  }
  return [...totals.entries()]
    .map(([currency, amount]) => ({ currency, amount }))
    .sort((a, b) => (b.currency ?? "").localeCompare(a.currency ?? ""));
}

/** Renders per-currency totals as `NPR 1,000.00 · USD 20.00`, or a dash when empty. */
export function formatCurrencyTotals(
  totals: { currency: string | null; amount: number }[]
): string {
  if (totals.length === 0) return "—";
  return totals
    .map((total) => formatMoney(total.amount, total.currency))
    .join(" · ");
}

/** Renders a percentage, or a dash when absent. */
export function formatPercentage(value: number | null | undefined): string {
  if (value === null || value === undefined) return "—";
  const amount = Number(value);
  if (Number.isNaN(amount)) return "—";
  return `${amount.toLocaleString(undefined, { maximumFractionDigits: 3 })}%`;
}

/**
 * Renders a quantity, keeping the decimals that matter.
 *
 * Six is the scale the backend itself uses, so a quantity that looks fractional here is
 * fractional in the database too rather than a rounding artefact of the display.
 */
export function formatQuantity(value: number | null | undefined): string {
  if (value === null || value === undefined) return "—";
  const amount = Number(value);
  if (Number.isNaN(amount)) return "—";
  return amount.toLocaleString(undefined, { maximumFractionDigits: 6 });
}

/** Renders a `yyyy-MM-dd` date as-is: it is already the canonical wire format. */
export function formatDate(value: string | null | undefined): string {
  if (!value) return "—";
  return value;
}

/**
 * Renders an ISO-8601 instant in the viewer's locale.
 *
 * Parsed defensively: an unparseable timestamp renders as a dash rather than
 * "Invalid Date", because a broken timestamp should not be mistaken for a broken record.
 */
export function formatTimestamp(value: string | null | undefined): string {
  if (!value) return "—";
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return "—";
  return parsed.toLocaleString();
}

/**
 * Whether a due date has passed.
 *
 * Only a true overdue flag is highlighted. A date that is due *today* is not late, and
 * colouring it as though it were would train people to ignore the highlight.
 */
export function isOverdue(
  dueDate: string | null | undefined,
  status?: string | null
): boolean {
  if (!dueDate) return false;
  if (status === "PAID" || status === "CANCELLED") return false;
  const due = new Date(`${dueDate}T00:00:00`);
  if (Number.isNaN(due.getTime())) return false;
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return due < today;
}
