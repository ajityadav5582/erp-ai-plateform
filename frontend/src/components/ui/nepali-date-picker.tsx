"use client";

import * as React from "react";
import { CalendarIcon, ChevronLeft, ChevronRight } from "lucide-react";

import { Button } from "@/components/ui/button";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { cn } from "@/lib/utils";
import {
  BS_DAY_SHORT_NP,
  BS_END_YEAR,
  BS_MONTH_NAMES_EN,
  BS_MONTH_NAMES_NP,
  BS_PROVISIONAL_FROM_YEAR,
  BS_START_YEAR,
  bsDaysInMonth,
  bsToAd,
  formatAdLong,
  formatBs,
  formatBsLongNp,
  isProvisionalBsYear,
  isSupportedBsYear,
  monthGrid,
  monthLeadingBlanks,
  safeAdToBs,
  toNepaliDigits,
  todayISO,
  type NepaliDate,
} from "@/lib/nepali-date";

/**
 * Nepali weeks are printed Sunday-first, so the grid columns are rotated from the
 * Monday-first `BS_DAY_SHORT_NP` table.
 */
const WEEK_HEADER = [
  BS_DAY_SHORT_NP[6], // Sunday
  BS_DAY_SHORT_NP[0], // Monday
  BS_DAY_SHORT_NP[1],
  BS_DAY_SHORT_NP[2],
  BS_DAY_SHORT_NP[3],
  BS_DAY_SHORT_NP[4],
  BS_DAY_SHORT_NP[5],
];

/**
 * Devanagari stack for Nepali calendar surfaces.
 *
 * Tailwind v4 turns the `--font-nepali` theme token in `globals.css` into the
 * `font-nepali` utility. The app-wide Geist sans family carries no Devanagari
 * coverage, so without this the month names render with inconsistent glyph weights
 * and conjuncts do not combine properly.
 */
const FONT_NEPALI = "font-nepali";

export interface NepaliDatePickerProps {
  /** Canonical Gregorian value, `yyyy-MM-dd`. Empty string means no date. */
  value: string;
  /** Receives the canonical Gregorian `yyyy-MM-dd` string. */
  onChange: (value: string) => void;
  /** Show the Gregorian date alongside the Nepali one. Default true. */
  showAd?: boolean;
  /** Language for the trigger. The calendar grid is always Nepali. Default "np". */
  locale?: "np" | "en";
  disabled?: boolean;
  placeholder?: string;
  id?: string;
  className?: string;
}

/**
 * Bikram Sambat date picker.
 *
 * The user picks a Nepali date, but the value handed to the form stays a Gregorian
 * `yyyy-MM-dd` string. That keeps the API contract, the database columns and every
 * existing consumer unchanged: only the input surface becomes Nepali.
 *
 * Everything inside the calendar - month, year and day numbers - renders in
 * Devanagari script with Nepali digits, matching a printed Nepali calendar. The
 * Gregorian date is shown separately and stays in ASCII, because that is the value
 * the backend receives and it must be readable at a glance.
 *
 * Conversion uses `src/lib/nepali-date.ts`, which mirrors the validated
 * `libraries/common-nepali-date` Java library exactly.
 */
export function NepaliDatePicker({
  value,
  onChange,
  showAd = true,
  locale = "np",
  disabled,
  placeholder = "मिति छान्नुहोस्",
  id,
  className,
}: NepaliDatePickerProps) {
  const [open, setOpen] = React.useState(false);
  const today = React.useMemo(() => todayISO(), []);
  const todayBs = React.useMemo(() => safeAdToBs(today), [today]);

  // The panel opens on the selected month, else the current month.
  // Memoised on `value` so the effect below is keyed on a stable identity rather
  // than a fresh object literal on every render.
  const selectedBs = React.useMemo(() => safeAdToBs(value), [value]);
  const [viewYear, setViewYear] = React.useState(
    selectedBs?.year ?? todayBs?.year ?? BS_PROVISIONAL_FROM_YEAR - 3,
  );
  const [viewMonth, setViewMonth] = React.useState(
    selectedBs?.month ?? todayBs?.month ?? 1,
  );

  // Follow the value when it changes from outside (for example a form reset).
  React.useEffect(() => {
    if (selectedBs) {
      setViewYear(selectedBs.year);
      setViewMonth(selectedBs.month);
    }
  }, [selectedBs]);

  const days = React.useMemo(
    () => monthGrid(viewYear, viewMonth),
    [viewYear, viewMonth],
  );
  const blanks = React.useMemo(
    () => monthLeadingBlanks(viewYear, viewMonth),
    [viewYear, viewMonth],
  );

  const shiftMonth = (delta: number) => {
    let m = viewMonth + delta;
    let y = viewYear;
    if (m < 1) {
      m = 12;
      y -= 1;
    } else if (m > 12) {
      m = 1;
      y += 1;
    }
    if (!isSupportedBsYear(y)) return;
    setViewYear(y);
    setViewMonth(m);
  };

  const canGoPrev = isSupportedBsYear(viewYear - 1);
  const canGoNext = isSupportedBsYear(viewYear + 1);

  const provisional = isProvisionalBsYear(viewYear);

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button
          id={id}
          type="button"
          variant="outline"
          disabled={disabled}
          className={cn(
            "h-auto w-full justify-start gap-2 px-3 py-2 text-left font-normal",
            !value && "text-muted-foreground",
            className,
          )}
        >
          <CalendarIcon className="size-4 shrink-0 opacity-70" />
          <NepaliTriggerLabel
            value={value}
            locale={locale}
            showAd={showAd}
            placeholder={placeholder}
          />
        </Button>
      </PopoverTrigger>

      <PopoverContent
        className="w-[20.5rem] p-3"
        align="start"
        sideOffset={4}
      >
        {/* Month / year header. Both selects render Nepali month names and
            Devanagari year digits; the underlying option values stay numeric. */}
        <div className="mb-2 flex items-center justify-between gap-1">
          <Button
            type="button"
            variant="ghost"
            size="icon"
            className="size-7 shrink-0"
            disabled={!canGoPrev}
            onClick={() => shiftMonth(-1)}
            aria-label="अघिल्लो महिना"
          >
            <ChevronLeft className="size-4" />
          </Button>

          <div className={cn("flex min-w-0 flex-1 items-center justify-center gap-1.5", FONT_NEPALI)}>
            <select
              aria-label="नेपाली महिना"
              className="h-7 min-w-0 rounded-md border border-input bg-background px-1.5 text-sm font-medium"
              value={viewMonth}
              onChange={(e) => setViewMonth(Number(e.target.value))}
            >
              {Array.from({ length: 12 }, (_, i) => i + 1).map((m) => (
                <option key={m} value={m}>
                  {BS_MONTH_NAMES_NP[m]}
                </option>
              ))}
            </select>
            <select
              aria-label="नेपाली वर्ष"
              className="h-7 rounded-md border border-input bg-background px-1.5 text-sm font-medium"
              value={viewYear}
              onChange={(e) => setViewYear(Number(e.target.value))}
            >
              {Array.from(
                { length: BS_END_YEAR - BS_START_YEAR + 1 },
                (_, i) => BS_START_YEAR + i,
              ).map((y) => (
                <option key={y} value={y}>
                  {toNepaliDigits(String(y))}
                  {isProvisionalBsYear(y) ? " *" : ""}
                </option>
              ))}
            </select>
          </div>

          <Button
            type="button"
            variant="ghost"
            size="icon"
            className="size-7 shrink-0"
            disabled={!canGoNext}
            onClick={() => shiftMonth(1)}
            aria-label="अर्को महिना"
          >
            <ChevronRight className="size-4" />
          </Button>
        </div>

        {/* Nepali month name with the Gregorian equivalent in parentheses. */}
        <div className="mb-2 text-center">
          <div className={cn("text-base font-semibold", FONT_NEPALI)}>
            {BS_MONTH_NAMES_NP[viewMonth]}{" "}
            <span className="font-normal">{toNepaliDigits(String(viewYear))}</span>
            <span className="ml-1.5 text-xs font-normal text-muted-foreground">
              {BS_MONTH_NAMES_EN[viewMonth]}
            </span>
          </div>
        </div>

        <div className={cn("grid grid-cols-7 gap-0.5 text-center", FONT_NEPALI)}>
          {WEEK_HEADER.map((d, i) => (
            <div
              key={`${d}-${i}`}
              className={cn(
                "pb-1 text-[0.7rem] font-medium",
                i === 0 ? "text-destructive/80" : "text-muted-foreground",
              )}
            >
              {d}
            </div>
          ))}

          {Array.from({ length: blanks }, (_, i) => (
            <div key={`blank-${i}`} aria-hidden />
          ))}

          {days.map((d) => {
            const isSelected = d.adISO === value;
            const isToday = d.adISO === today;
            return (
              <button
                key={d.adISO}
                type="button"
                onClick={() => {
                  onChange(d.adISO);
                  setOpen(false);
                }}
                className={cn(
                  "h-8 rounded-md text-sm font-medium transition-colors",
                  "hover:bg-accent hover:text-accent-foreground",
                  isSelected && "bg-primary text-primary-foreground hover:bg-primary",
                  !isSelected && isToday && "ring-1 ring-primary",
                  d.weekend && !isSelected && "text-muted-foreground",
                )}
                // Screen readers get the ASCII form, which reads out more reliably
                // than Devanagari digits.
                aria-label={`${formatBs(d)} ${BS_MONTH_NAMES_EN[d.month]} ${d.day}, ${d.dayNameEn}`}
                aria-current={isToday ? "date" : undefined}
              >
                {toNepaliDigits(String(d.day))}
              </button>
            );
          })}
        </div>

        <div className="mt-2 flex items-center justify-between gap-2 border-t pt-2 text-[0.7rem] text-muted-foreground">
          <span className={FONT_NEPALI}>
            {toNepaliDigits(String(bsDaysInMonth(viewYear, viewMonth)))} दिन
          </span>
          {provisional ? (
            <span
              className="text-amber-600 dark:text-amber-500"
              title="Month lengths for this Nepali year are projected, not officially published. Avoid using it for tax or statutory dates."
            >
              * अनुमानित
            </span>
          ) : (
            <span>AD {days[0]?.adISO?.slice(0, 4)}</span>
          )}
        </div>

        <div className="mt-2 flex gap-2">
          <Button
            type="button"
            variant="outline"
            size="sm"
            className={cn("h-7 flex-1 text-xs", FONT_NEPALI)}
            onClick={() => {
              onChange(today);
              setOpen(false);
            }}
          >
            आज
          </Button>
          <Button
            type="button"
            variant="ghost"
            size="sm"
            className={cn("h-7 flex-1 text-xs", FONT_NEPALI)}
            onClick={() => {
              onChange("");
              setOpen(false);
            }}
          >
            हटाउनुहोस्
          </Button>
        </div>
      </PopoverContent>
    </Popover>
  );
}

/**
 * Closed trigger. Nepali and Gregorian sit on separate lines so neither is buried,
 * and the Nepali line carries the Devanagari script and digits throughout.
 */
function NepaliTriggerLabel({
  value,
  locale,
  showAd,
  placeholder,
}: {
  value: string;
  locale: "np" | "en";
  showAd: boolean;
  placeholder: string;
}) {
  const bs = safeAdToBs(value);
  if (!bs) {
    return <span className="truncate text-sm">{placeholder}</span>;
  }

  return (
    <span className="flex min-w-0 flex-col gap-0.5">
      {locale === "np" ? (
        <span className={cn("truncate text-sm font-semibold", FONT_NEPALI)}>
          {formatBsLongNp(value)}
        </span>
      ) : (
        <span className="truncate text-sm font-semibold">
          {formatBs(bs)} ({BS_MONTH_NAMES_EN[bs.month]})
        </span>
      )}
      {showAd && (
        <span className="truncate text-[0.7rem] text-muted-foreground">
          AD {formatAdLong(value)}
        </span>
      )}
    </span>
  );
}

export default NepaliDatePicker;

/** Re-exported so callers can build a BS value without importing the whole lib. */
export { bsToAd, formatBs };
export type { NepaliDate };
