"use client";

import { cn } from "@/lib/utils";
import { NepaliDatePicker } from "@/components/ui/nepali-date-picker";
import { safeAdToBsISO, safeBsToAd } from "@/lib/nepali-date";

export interface BsDateFieldProps {
  /** Bikram Sambat date as `yyyy-MM-dd`. Empty string means no date. */
  value: string;
  /** Receives the Bikram Sambat date as `yyyy-MM-dd`. */
  onChange: (bsISO: string) => void;
  disabled?: boolean;
  placeholder?: string;
  id?: string;
  className?: string;
}

/**
 * A form control whose value is a Bikram Sambat date, not a Gregorian one.
 *
 * {@link NepaliDatePicker} speaks Gregorian because it was built for canonical-date
 * forms. This wraps it for the dual-calendar forms, where the user asked for the AD
 * and NP dates to live in separate fields.
 *
 * The picker still renders the Nepali calendar; the translation happens at this
 * boundary only. `value` is the source of truth, so an unconvertible value simply
 * shows the placeholder instead of throwing mid-render.
 */
export function BsDateField({
  value,
  onChange,
  disabled,
  placeholder,
  id,
  className,
}: BsDateFieldProps) {
  // The picker needs a Gregorian value; project the BS value into one. Falling back to
  // an empty string makes the picker show its placeholder rather than a stale date.
  const adValue = safeBsToAd(value) ?? "";

  return (
    <NepaliDatePicker
      id={id}
      // The AD line is redundant here: the sibling field already carries it, and
      // repeating it inside the trigger is what made the two fields read as one.
      showAd={false}
      value={adValue}
      onChange={(adISO) => onChange(safeAdToBsISO(adISO) ?? "")}
      disabled={disabled}
      placeholder={placeholder}
      className={cn(className)}
    />
  );
}

export default BsDateField;
