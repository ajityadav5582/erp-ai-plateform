import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

/**
 * Merges conditional class names and resolves Tailwind CSS conflicts.
 *
 * @example
 *   cn("px-2", condition && "px-4", "bg-red-500") // -> "px-4 bg-red-500"
 */
export function cn(...inputs: ClassValue[]): string {
  return twMerge(clsx(inputs));
}

/** Formats a number as currency using the provided locale and currency code. */
export function formatCurrency(
  value: number,
  currency: string = "USD",
  locale: string = "en-US",
): string {
  return new Intl.NumberFormat(locale, {
    style: "currency",
    currency,
  }).format(value);
}

/** Safely accesses an array element with a fallback (avoids undefined indexing). */
export function at<T>(array: readonly T[], index: number, fallback: T): T {
  return array[index] ?? fallback;
}
