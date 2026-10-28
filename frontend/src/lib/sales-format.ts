/**
 * Shared formatting for the sales screens.
 *
 * Money, quantities and dates appear across seven tables and their detail sheets.
 * Centralising them keeps `1,234.50` from becoming `1234.5` in one place, and keeps
 * the IRD figures (VAT, totals) rendered identically everywhere a user reads them.
 */

import { formatMoney, formatDate } from "./format";
export { formatMoney, formatDate };

/** Renders a quantity, keeping up to six decimals and dropping trailing noise. */
export function formatQty(value: number | null | undefined): string {
  if (value === null || value === undefined) return "—";
  const n = Number(value);
  if (Number.isNaN(n)) return "—";
  return n.toLocaleString(undefined, { maximumFractionDigits: 6 });
}

const ONES = [
  "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
  "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
  "Seventeen", "Eighteen", "Nineteen",
];

const TENS = [
  "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety",
];

function convertLessThanOneThousand(n: number): string {
  let str = "";
  if (n >= 100) {
    str += `${ONES[Math.floor(n / 100)]} Hundred `;
    n %= 100;
  }
  if (n >= 20) {
    str += `${TENS[Math.floor(n / 10)]} `;
    n %= 10;
  }
  if (n > 0) {
    str += `${ONES[n]} `;
  }
  return str.trim();
}

/**
 * Converts a numeric amount into words using the South Asian system (Crores, Lakhs, Thousands).
 * e.g., 125000.50 => "One Lakh Twenty-Five Thousand and Fifty Paisa Only"
 */
export function amountToWords(amount: number | null | undefined, currency = "NPR"): string {
  if (amount === null || amount === undefined || isNaN(amount)) return "Zero";
  const absAmount = Math.abs(amount);
  const integerPart = Math.floor(absAmount);
  const paisaPart = Math.round((absAmount - integerPart) * 100);

  if (integerPart === 0 && paisaPart === 0) return "Zero Only";

  let remaining = integerPart;
  const parts: string[] = [];

  // Crores (10,000,000)
  if (remaining >= 10000000) {
    const crores = Math.floor(remaining / 10000000);
    parts.push(`${convertLessThanOneThousand(crores)} Crore`);
    remaining %= 10000000;
  }

  // Lakhs (100,000)
  if (remaining >= 100000) {
    const lakhs = Math.floor(remaining / 100000);
    parts.push(`${convertLessThanOneThousand(lakhs)} Lakh`);
    remaining %= 100000;
  }

  // Thousands (1,000)
  if (remaining >= 1000) {
    const thousands = Math.floor(remaining / 1000);
    parts.push(`${convertLessThanOneThousand(thousands)} Thousand`);
    remaining %= 1000;
  }

  // Hundreds & below
  if (remaining > 0) {
    parts.push(convertLessThanOneThousand(remaining));
  }

  let words = parts.join(" ").trim();
  if (!words) words = "Zero";

  words = `${words} ${currency}`;

  if (paisaPart > 0) {
    words += ` and ${convertLessThanOneThousand(paisaPart)} Paisa`;
  }

  return `${words} Only`;
}

