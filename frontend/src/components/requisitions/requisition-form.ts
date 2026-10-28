import { z } from "zod";
import { formatBs, safeAdToBs, todayISO } from "@/lib/nepali-date";
import {
  REQUISITION_PRIORITIES,
  type CreateRequisitionRequest,
  type Requisition,
  type UpdateRequisitionRequest,
} from "@/services/purchase-requisition.service";

/**
 * Mirrors the bean validation on `CreateRequisitionRequest` and the invariants in
 * `PurchaseRequisition.draft` / `PurchaseRequisitionLine.create`.
 *
 * The pattern is duplicated from the backend on purpose: catching "a
 * justification is required for URGENT" inline beats a toast the user reads after
 * the round trip.
 *
 * Numeric inputs are held as strings so the controlled `<Input type="number">`
 * stays simple; `toCreateRequisitionRequest` converts them on submit.
 */
const optionalText = (max: number, label: string) =>
  z
    .string()
    .max(max, `${label} must not exceed ${max} characters`)
    .optional()
    .or(z.literal(""));

/** A required decimal entered as a string, with an explicit message on failure. */
const requiredDecimal = (label: string) =>
  z
    .string()
    .min(1, `${label} is required`)
    .refine((v) => !Number.isNaN(Number(v)), { message: `${label} must be a number` });

/** Quantity and price are positive; `PurchaseRequisitionLine.create` rejects <= 0. */
const positiveDecimal = (label: string) =>
  requiredDecimal(label).refine((v) => Number(v) > 0, {
    message: `${label} must be greater than zero`,
  });

/** Unit price may legitimately be zero for a not-yet-priced item. */
const nonNegativeDecimal = (label: string) =>
  requiredDecimal(label).refine((v) => Number(v) >= 0, {
    message: `${label} cannot be negative`,
  });

const lineSchema = z.object({
  itemId: z.string().min(1, "Select an item"),
  quantity: positiveDecimal("Quantity"),
  estimatedUnitPrice: nonNegativeDecimal("Estimated unit price"),
  taxRate: z
    .string()
    .min(1, "Tax rate is required")
    .refine((v) => !Number.isNaN(Number(v)), { message: "Tax rate must be a number" })
    .refine((v) => Number(v) >= 0, { message: "Tax rate cannot be negative" })
    .refine((v) => Number(v) <= 100, { message: "Tax rate must not exceed 100" }),
  remarks: optionalText(1000, "Line remarks"),
});

export type RequisitionLineFormValues = z.infer<typeof lineSchema>;

export const requisitionFormSchema = z
  .object({
    // --- Scope ---
    branchId: z.string().min(1, "Select the branch raising this demand"),
    departmentId: z.string().optional(),

    // --- Dates ---
    // requestDate is the canonical Gregorian value the backend stores and derives
    // the Nepali projection from. requestDateNp is sent alongside purely as a
    // cross-check: the server rejects a disagreement rather than silently
    // overwriting, so a picker bug surfaces here instead of becoming bad data.
    requestDate: z.string().min(1, "Request date is required"),
    requestDateNp: z.string().optional(),
    requiredByDate: z.string().optional(),

    // --- Urgency ---
    priority: z.enum(REQUISITION_PRIORITIES, {
      errorMap: () => ({ message: "Select a priority" }),
    }),
    currency: z.string().length(3, "Currency must be a 3-letter code"),

    // --- Delivery ---
    /**
     * A nomination, not a delivery instruction. The requester frequently cannot
     * say which site will serve the request, so this is optional - but the code
     * and name are derived from it by the dialog rather than typed, which is what
     * keeps the frozen snapshot the document relies on from being half-filled.
     */
    warehouseId: z.string().optional(),
    justification: optionalText(2000, "Justification"),
    notes: optionalText(2000, "Notes"),

    // --- Lines ---
    lines: z
      .array(lineSchema)
      .min(1, "A requisition needs at least one line")
      .max(50, "A requisition cannot exceed 50 lines"),
  })
  .superRefine((values, ctx) => {
    // Catch an AD/BS disagreement in the browser too, so the user gets an inline
    // message instead of a round-trip 400.
    if (values.requestDateNp && values.requestDateNp !== bsOf(values.requestDate)) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["requestDateNp"],
        message: "Nepali date does not match the selected Gregorian date",
      });
    }

    // A required-by date before the request date is rejected by
    // `PurchaseRequisition.draft`, so catch it here rather than on submit.
    if (
      values.requiredByDate &&
      values.requiredByDate < values.requestDate
    ) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["requiredByDate"],
        message: "Required-by date cannot be before the request date",
      });
    }

    // HIGH and URGENT ask to jump the approval queue, so the aggregate requires
    // a justification for them.
    if (
      (values.priority === "HIGH" || values.priority === "URGENT") &&
      !values.justification?.trim()
    ) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["justification"],
        message: `A justification is required for ${values.priority} requisitions`,
      });
    }
  });

export type RequisitionFormValues = z.infer<typeof requisitionFormSchema>;

/**
 * Today's date in `yyyy-MM-dd`, matching the ISO format the backend's
 * `LocalDate` binding expects.
 *
 * Re-exported from `@/lib/nepali-date` so there is a single definition. That one is
 * deliberately local-time, not UTC: Nepal is UTC+05:45, so `toISOString()` would
 * report the previous day for the first 5h45m of every morning.
 */
export { todayISO };

/**
 * The Bikram Sambat form of a Gregorian `yyyy-MM-dd` string.
 *
 * @returns the BS date as `yyyy-MM-dd`, or `null` when the date falls outside the
 *          published calendar range. Returns `null` rather than throwing because the
 *          validation rule must compare against a value, not blow up mid-submit.
 */
function bsOf(adISO: string): string | null {
  const bs = safeAdToBs(adISO);
  return bs ? formatBs(bs) : null;
}

export const requisitionFormDefaults: RequisitionFormValues = (() => {
  // Seed both projections from today so the form opens with a consistent pair.
  // Only the AD value is authoritative; the NP value is the server-side cross-check.
  const todayAd = todayISO();
  return {
    branchId: "",
    departmentId: "",
    requestDate: todayAd,
    requestDateNp: bsOf(todayAd) ?? "",
    requiredByDate: "",
    priority: "NORMAL",
    currency: "NPR",
    warehouseId: "",
    justification: "",
    notes: "",
    lines: [],
  };
})();

/** An empty line row, used by the "Add Line" action in the create dialog. */
export function emptyLineValues(): RequisitionLineFormValues {
  return {
    itemId: "",
    quantity: "",
    estimatedUnitPrice: "",
    taxRate: "13",
    remarks: "",
  };
}

/** Empty string -> undefined, so the backend stores null rather than "". */
function optionalTextValue(value?: string): string | undefined {
  const trimmed = value?.trim();
  return trimmed ? trimmed : undefined;
}

function parsePositiveNumber(value?: string | number | null): number | undefined {
  if (value === null || value === undefined) return undefined;
  const str = String(value).trim();
  if (!str || str === "none" || str === "0") return undefined;
  const num = Number(str);
  return !Number.isNaN(num) && num > 0 ? num : undefined;
}

export function toCreateRequisitionRequest(
  values: RequisitionFormValues,
  /** Catalogue snapshot for each selected item, keyed by item id. */
  itemSnapshot: Map<number, { code: string; name: string; uom: string }>,
  /** The nominated warehouse's snapshot, resolved from the picker. */
  warehouseSnapshot?: { code: string; name: string } | null
): CreateRequisitionRequest {
  const warehouseId = parsePositiveNumber(values.warehouseId);
  const branchId = parsePositiveNumber(values.branchId);

  if (!branchId) {
    throw new Error("Please select a valid branch for this requisition.");
  }

  return {
    branchId,
    departmentId: parsePositiveNumber(values.departmentId),
    requestDate: values.requestDate,
    // Sent for the server-side cross-check only; the stored value is derived.
    requestDateNp: values.requestDateNp || undefined,
    requiredByDate: values.requiredByDate || undefined,
    priority: values.priority,
    currency: values.currency.toUpperCase(),
    // All three or none. A selection with no snapshot would be refused by the
    // server, and rightly: the code and name are what the document prints once the
    // warehouse master is renamed, so a nomination without them is unusable.
    warehouseId,
    warehouseCode: warehouseId ? warehouseSnapshot?.code : undefined,
    warehouseName: warehouseId ? warehouseSnapshot?.name : undefined,
    justification: optionalTextValue(values.justification),
    notes: optionalTextValue(values.notes),
    lines: values.lines.map((line, idx) => {
      const itemId = parsePositiveNumber(line.itemId);
      if (!itemId) {
        throw new Error(`Line ${idx + 1}: Please select an item.`);
      }
      const snapshot = itemSnapshot.get(itemId);
      if (!snapshot) {
        throw new Error(
          `Line ${idx + 1}: Item details not found in catalogue. Please re-select the item.`
        );
      }
      return {
        itemId,
        // The backend stores these as a frozen snapshot rather than resolving
        // them live, so a later catalogue rename cannot rewrite history.
        itemCode: snapshot.code,
        itemName: snapshot.name,
        uomCode: snapshot.uom,
        quantity: Number(line.quantity),
        estimatedUnitPrice: Number(line.estimatedUnitPrice),
        taxRate: Number(line.taxRate),
        remarks: optionalTextValue(line.remarks),
      };
    }),
  };
}


/**
 * Seeds the form from an existing draft or returned requisition.
 *
 * `branchId` is carried through even though the edit form never shows it, so the
 * same schema validates create and edit. The update request drops it again —
 * re-pointing a document at another branch would move it under a different
 * approval policy, which is not something an edit should be able to do.
 */
export function requisitionEditDefaults(requisition: Requisition): RequisitionFormValues {
  return {
    branchId: String(requisition.branchId),
    departmentId: requisition.departmentId ? String(requisition.departmentId) : "",
    requestDate: requisition.requestDate,
    requestDateNp: requisition.requestDateNp ?? "",
    requiredByDate: requisition.requiredByDate ?? "",
    priority: requisition.priority,
    currency: requisition.currency,
    warehouseId: requisition.warehouseId ? String(requisition.warehouseId) : "",
    justification: requisition.justification ?? "",
    notes: requisition.notes ?? "",
    lines: requisition.lines.map((line) => ({
      itemId: String(line.itemId),
      quantity: String(line.requestedQuantity),
      estimatedUnitPrice: String(line.estimatedUnitPrice),
      taxRate: String(line.taxRate),
      remarks: line.remarks ?? "",
    })),
  };
}

/**
 * The catalogue snapshot for an existing document, keyed by item id.
 *
 * Built from the requisition's own lines rather than re-fetched from the item
 * catalogue: the line stores a frozen copy of the code/name/UoM as it stood when
 * the document was raised, and re-reading the live catalogue would silently
 * rewrite history the moment an item is renamed.
 */
export function snapshotFromRequisition(
  requisition: Requisition
): Map<number, { code: string; name: string; uom: string }> {
  return new Map(
    requisition.lines.map((line) => [
      line.itemId,
      { code: line.itemCode, name: line.itemName, uom: line.uomCode },
    ])
  );
}

/**
 * The PUT body for an edit.
 *
 * Reuses the create conversion and then removes the fields the edit contract
 * does not carry. Branch and department are dropped rather than ignored: sending
 * them would be accepted-and-dropped by the server, which is exactly the sort of
 * silent no-op that makes an API hard to reason about.
 */
export function toUpdateRequisitionRequest(
  values: RequisitionFormValues,
  itemSnapshot: Map<number, { code: string; name: string; uom: string }>,
  /** The nominated warehouse's snapshot, resolved from the picker. */
  warehouseSnapshot?: { code: string; name: string } | null
): UpdateRequisitionRequest {
  const { branchId: _branchId, departmentId: _departmentId, ...editable } =
    toCreateRequisitionRequest(values, itemSnapshot, warehouseSnapshot);

  // The create contract types priority and currency as optional because the
  // server defaults them. This one states both, because the form always knows
  // them and a full replacement should say what it is replacing. The schema
  // already guarantees that, so they are restated rather than cast.
  //
  // The server treats a missing priority as "leave it alone" and a missing
  // currency the same way - it does NOT reset currency to NPR. Clearing it
  // would re-denominate every amount on the document, turning a 5,000 rupee
  // estimate into a 5,000 dollar one with no number changing. Sending it
  // explicitly is still the right thing to do, because a requester correcting
  // a mis-picked currency is a real edit and it has to actually land.
  return {
    ...editable,
    priority: values.priority,
    currency: values.currency.toUpperCase(),
  };
}
