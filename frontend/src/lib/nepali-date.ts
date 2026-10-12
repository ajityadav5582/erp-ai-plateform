/**
 * Bikram Sambat (BS / Nepali) date conversion for the browser.
 *
 * Mirrors `libraries/common-nepali-date` (Java) exactly, so the calendar the user
 * picks in the UI is the same date the backend stores and reports.
 *
 * ## The one rule: AD is canonical
 *
 * Forms hold `yyyy-MM-dd` **Gregorian** strings because that is what the API and the
 * database use. BS is a display projection. `NepaliDatePicker` shows the BS calendar
 * but calls `onChange` with the Gregorian string, so nothing downstream needs to know
 * a Nepali calendar was involved.
 */

import {
  AD_MONTH_NAMES_EN,
  BS_DAY_NAMES_EN,
  BS_DAY_NAMES_NP,
  BS_DAY_SHORT_NP,
  BS_END_YEAR,
  BS_EPOCH_AD,
  BS_MONTH_NAMES_EN,
  BS_MONTH_NAMES_EN_SHORT,
  BS_MONTH_NAMES_NP,
  BS_PROVISIONAL_FROM_YEAR,
  BS_START_YEAR,
  MONTH_DAYS,
} from "./nepali-calendar-data";

export {
  BS_DAY_NAMES_EN,
  BS_DAY_NAMES_NP,
  BS_DAY_SHORT_NP,
  BS_MONTH_NAMES_EN,
  BS_MONTH_NAMES_EN_SHORT,
  BS_MONTH_NAMES_NP,
  BS_START_YEAR,
  BS_END_YEAR,
  BS_PROVISIONAL_FROM_YEAR,
};

/** A Bikram Sambat date. */
export interface NepaliDate {
  year: number;
  /** 1..12 */
  month: number;
  day: number;
}

/** A Gregorian date plus its BS equivalent and display metadata. */
export interface NepaliDateDetails extends NepaliDate {
  /** Canonical Gregorian date as `yyyy-MM-dd`. */
  adDate: string;
  /** BS date as `yyyy-MM-dd`. */
  bsDate: string;
  /** BS date rendered with Devanagari digits. */
  bsDateNp: string;
  monthNameEn: string;
  monthNameNp: string;
  /** 0 = Monday .. 6 = Sunday, matching {@link JS_DAY}. */
  weekday: number;
  dayNameEn: string;
  dayNameNp: string;
  weekend: boolean;
  /** True when the BS year relies on projected, not published, month lengths. */
  provisional: boolean;
}

/** JS `Date.getDay()`: 0 = Sunday .. 6 = Saturday. */
const JS_DAY = (ad: Date): number => ad.getDay();

/** Our weekday index: 0 = Monday .. 6 = Sunday. */
const mondayFirst = (ad: Date): number => (ad.getDay() + 6) % 7;

const MS_PER_DAY = 86_400_000;

/** Days from {@link BS_EPOCH_AD} to Baisakh 1 of each supported year. */
const YEAR_START_OFFSET: number[] = (() => {
  const offsets: number[] = [];
  let running = 0;
  for (let y = BS_START_YEAR; y <= BS_END_YEAR; y++) {
    offsets.push(running);
    for (const days of MONTH_DAYS[y - BS_START_YEAR]!) running += days;
  }
  return offsets;
})();

const EPOCH_MS = Date.parse(`${BS_EPOCH_AD}T00:00:00Z`);

/** Total days covered by the table. */
export const BS_TOTAL_DAYS = YEAR_START_OFFSET[YEAR_START_OFFSET.length - 1]! + MONTH_DAYS[BS_END_YEAR - BS_START_YEAR]!.reduce((a, b) => a + b, 0);

/** Earliest convertible Gregorian date as `yyyy-MM-dd`. */
export const BS_MIN_AD = BS_EPOCH_AD;

/** Latest convertible Gregorian date as `yyyy-MM-dd`. */
export const BS_MAX_AD = toAdISO(BS_TOTAL_DAYS - 1);

/** @returns `true` when `year` is within the supported table range. */
export function isSupportedBsYear(year: number): boolean {
  return year >= BS_START_YEAR && year <= BS_END_YEAR;
}

/** @returns `true` when `year` is supported but only provisionally defined. */
export function isProvisionalBsYear(year: number): boolean {
  return year >= BS_PROVISIONAL_FROM_YEAR;
}

/**
 * Number of days in a Bikram Sambat month.
 *
 * @throws RangeError when the year or month is out of range
 */
export function bsDaysInMonth(year: number, month: number): number {
  if (!isSupportedBsYear(year)) {
    throw new RangeError(
      `Unsupported BS year ${year} (supported ${BS_START_YEAR}..${BS_END_YEAR})`,
    );
  }
  if (month < 1 || month > 12) {
    throw new RangeError(`BS month must be 1..12 but was ${month}`);
  }
  return MONTH_DAYS[year - BS_START_YEAR]![month - 1]!;
}

/** Days from {@link BS_EPOCH_AD} to `year`-01-01 BS. */
function yearStartOffset(year: number): number {
  return YEAR_START_OFFSET[year - BS_START_YEAR]!;
}

function toAdISO(offsetDays: number): string {
  return new Date(EPOCH_MS + offsetDays * MS_PER_DAY).toISOString().slice(0, 10);
}

/**
 * Converts a Gregorian `yyyy-MM-dd` string to Bikram Sambat.
 *
 * @throws RangeError when the string is malformed, not a real date, or outside the
 *   supported range
 */
export function adToBs(adISO: string): NepaliDate {
  const ms = Date.parse(`${adISO}T00:00:00Z`);
  if (Number.isNaN(ms)) {
    throw new RangeError(`Malformed AD date "${adISO}": expected yyyy-MM-dd`);
  }
  // Reject strings that parsed but are not a real calendar date, e.g. 2024-02-31.
  if (new Date(ms).toISOString().slice(0, 10) !== adISO) {
    throw new RangeError(`"${adISO}" is not a valid calendar date`);
  }

  let offset = Math.round((ms - EPOCH_MS) / MS_PER_DAY);
  if (offset < 0 || offset >= BS_TOTAL_DAYS) {
    throw new RangeError(
      `AD date ${adISO} is outside the convertible range ${BS_MIN_AD}..${BS_MAX_AD}`,
    );
  }

  let year = BS_START_YEAR;
  while (year <= BS_END_YEAR && offset >= MONTH_DAYS[year - BS_START_YEAR]!.reduce((a, b) => a + b, 0)) {
    offset -= MONTH_DAYS[year - BS_START_YEAR]!.reduce((a, b) => a + b, 0);
    year++;
  }

  const months = MONTH_DAYS[year - BS_START_YEAR]!;
  let month = 1;
  while (month <= 12 && offset >= months[month - 1]!) {
    offset -= months[month - 1]!;
    month++;
  }

  return { year, month, day: offset + 1 };
}

/** Converts Bikram Sambat components to a Gregorian `yyyy-MM-dd` string. */
export function bsToAd(year: number, month: number, day: number): string {
  const maxDay = bsDaysInMonth(year, month);
  if (day < 1 || day > maxDay) {
    throw new RangeError(
      `BS day must be 1..${maxDay} for ${year}-${month} but was ${day}`,
    );
  }
  let offset = yearStartOffset(year);
  const months = MONTH_DAYS[year - BS_START_YEAR]!;
  for (let m = 1; m < month; m++) offset += months[m - 1]!;
  return toAdISO(offset + day - 1);
}

/**
 * Full conversion with display metadata.
 *
 * This is what the picker renders and what any API response should mirror.
 */
export function bsDetails(adISO: string): NepaliDateDetails {
  const bs = adToBs(adISO);
  const ad = new Date(`${adISO}T00:00:00Z`);
  const weekday = mondayFirst(ad);
  const bsDate = formatBs(bs);
  return {
    ...bs,
    adDate: adISO,
    bsDate,
    bsDateNp: toNepaliDigits(bsDate),
    monthNameEn: BS_MONTH_NAMES_EN[bs.month]!,
    monthNameNp: BS_MONTH_NAMES_NP[bs.month]!,
    weekday,
    dayNameEn: BS_DAY_NAMES_EN[weekday]!,
    dayNameNp: BS_DAY_NAMES_NP[weekday]!,
    weekend: JS_DAY(ad) === 0 || JS_DAY(ad) === 6,
    provisional: isProvisionalBsYear(bs.year),
  };
}

/** Formats a BS date as `yyyy-MM-dd`. */
export function formatBs(bs: NepaliDate): string {
  const y = String(bs.year).padStart(4, "0");
  const m = String(bs.month).padStart(2, "0");
  const d = String(bs.day).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

/** Renders a Gregorian date the way Nepali business documents do. */
export function formatBsLong(adISO: string): string {
  const d = bsDetails(adISO);
  return `${d.year} ${d.monthNameEn} ${String(d.day).padStart(2, "0")} (${d.dayNameEn})`;
}

/**
 * Fully Nepali rendering: `२०८३ आश्विन १६, शुक्रबार`.
 *
 * Devanagari month name, Devanagari digits and the Nepali weekday, which is how a
 * date is read aloud and written on Nepali paperwork. Pair this with
 * {@link formatAdLong} so the Gregorian equivalent stays legible beside it.
 */
export function formatBsLongNp(adISO: string): string {
  const d = bsDetails(adISO);
  return `${toNepaliDigits(String(d.year))} ${d.monthNameNp} ${toNepaliDigits(String(d.day))}, ${d.dayNameNp}`;
}

/** Gregorian long form: `October 2, 2026`. */
export function formatAdLong(adISO: string): string {
  const d = bsDetails(adISO);
  const [y, m, day] = d.adDate.split("-");
  const monthEn = AD_MONTH_NAMES_EN[Number(m) - 1] ?? "";
  return `${monthEn} ${Number(day)}, ${y}`;
}

/** Converts ASCII digits to Devanagari digits. */
export function toNepaliDigits(text: string): string {
  return text.replace(/[0-9]/g, (d) => String.fromCharCode(0x0966 + Number(d)));
}

/** Converts Devanagari digits to ASCII digits. */
export function toAsciiDigits(text: string): string {
  return text.replace(/[०-९]/g, (d) => String.fromCharCode(0x30 + d.charCodeAt(0) - 0x0966));
}

/** @returns `true` when `adISO` is a real date inside the supported range. */
export function isConvertible(adISO: string | null | undefined): boolean {
  if (!adISO) return false;
  try {
    adToBs(adISO);
    return true;
  } catch {
    return false;
  }
}

/** Converts without throwing; returns `null` for unusable input. */
export function safeAdToBs(adISO: string | null | undefined): NepaliDate | null {
  if (!adISO) return null;
  try {
    return adToBs(adISO);
  } catch {
    return null;
  }
}

/** Gregorian `yyyy-MM-dd` to BS `yyyy-MM-dd`, or `null` when not convertible. */
export function safeAdToBsISO(adISO: string | null | undefined): string | null {
  const bs = safeAdToBs(adISO);
  return bs ? formatBs(bs) : null;
}

/**
 * Parses a BS `yyyy-MM-dd` string into its components.
 *
 * @throws RangeError when the string is malformed or names a day the month does not have
 */
export function parseBs(bsISO: string): NepaliDate {
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(bsISO);
  if (!m) {
    throw new RangeError(`Malformed BS date "${bsISO}": expected yyyy-MM-dd`);
  }
  const year = Number(m[1]);
  const month = Number(m[2]);
  const day = Number(m[3]);
  if (!isSupportedBsYear(year)) {
    throw new RangeError(
      `Unsupported BS year ${year} (supported ${BS_START_YEAR}..${BS_END_YEAR})`,
    );
  }
  // Throws when the month is out of range, which is exactly the validation wanted here.
  const maxDay = bsDaysInMonth(year, month);
  if (day < 1 || day > maxDay) {
    throw new RangeError(
      `BS day must be 1..${maxDay} for ${year}-${month} but was ${day}`,
    );
  }
  return { year, month, day };
}

/**
 * BS `yyyy-MM-dd` to Gregorian `yyyy-MM-dd`, without throwing.
 *
 * The inverse of {@link safeAdToBsISO}. This lets a form hold the BS date itself while
 * the picker - which speaks Gregorian - is driven by a projection of that value.
 */
export function safeBsToAd(bsISO: string | null | undefined): string | null {
  if (!bsISO) return null;
  try {
    const bs = parseBs(bsISO);
    return bsToAd(bs.year, bs.month, bs.day);
  } catch {
    return null;
  }
}

/** Today's Gregorian date in local time as `yyyy-MM-dd`. */
export function todayISO(): string {
  const now = new Date();
  const y = now.getFullYear();
  const m = String(now.getMonth() + 1).padStart(2, "0");
  const d = String(now.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

/**
 * Every day of a Bikram Sambat month, for rendering the calendar grid.
 *
 * Each entry carries the Gregorian date so selection can hand a plain `yyyy-MM-dd`
 * back to the form.
 */
export function monthGrid(
  year: number,
  month: number,
): Array<NepaliDateDetails & { adISO: string }> {
  const total = bsDaysInMonth(year, month);
  const out: Array<NepaliDateDetails & { adISO: string }> = [];
  for (let day = 1; day <= total; day++) {
    const adISO = bsToAd(year, month, day);
    out.push({ ...bsDetails(adISO), adISO });
  }
  return out;
}

/**
 * Leading blanks so the grid starts on the correct weekday column (0 = Monday).
 * Nepali weeks run Sunday to Saturday in common usage, so the grid is Sunday-first.
 */
export function monthLeadingBlanks(year: number, month: number): number {
  const first = bsToAd(year, month, 1);
  // Sunday-first column index.
  return new Date(`${first}T00:00:00Z`).getUTCDay();
}
