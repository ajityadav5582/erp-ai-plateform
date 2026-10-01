import { z } from "zod";
import {
  EXCISE_TYPES,
  ITEM_TYPES,
  TAXABILITY_TYPES,
  DEFAULT_VAT_RATE,
  type CreateItemRequest,
  type UpdateItemRequest,
  type Item,
} from "@/services/item.service";

/**
 * Mirrors the bean validation and the domain invariants in `Item.applyChanges`.
 *
 * The pattern is duplicated from the backend on purpose: catching "Selling price
 * is required" inline beats a toast the user reads after the round trip.
 *
 * Numeric inputs are held as strings so the controlled `<Input type="number">`
 * stays simple; `toCreateItemRequest` converts them on submit.
 */
const optionalText = (max: number, label: string) =>
  z
    .string()
    .max(max, `${label} must not exceed ${max} characters`)
    .optional()
    .or(z.literal(""));

/** Non-negative decimal, entered as a string. Empty string means "not provided". */
const nonNegativeDecimal = (label: string) =>
  z
    .string()
    .refine((v) => v === "" || (!Number.isNaN(Number(v)) && Number(v) >= 0), {
      message: `${label} must be zero or greater`,
    })
    .refine((v) => v === "" || Number.isFinite(Number(v)), {
      message: `${label} must be a number`,
    });

export const itemFormSchema = z
  .object({
    // --- Identification ---
    sku: z
      .string()
      .min(1, "Item SKU is required")
      .max(100, "SKU must not exceed 100 characters"),
    nameEn: z
      .string()
      .min(1, "English item name is required")
      .max(255, "English item name must not exceed 255 characters"),
    nameNp: optionalText(255, "Nepali item name"),
    barcode: optionalText(100, "Barcode"),
    hsCode: optionalText(20, "HS Code"),
    descriptionEn: optionalText(1000, "English description"),
    descriptionNp: optionalText(1000, "Nepali description"),

    // --- Classification ---
    // Empty string means "no category"; the backend treats null as unset.
    categoryId: z.string().optional(),
    uomId: z
      .string()
      .min(1, "Primary unit of measure is required"),
    itemType: z.enum(ITEM_TYPES, {
      errorMap: () => ({ message: "Select whether this is a good or a service" }),
    }),

    // --- Tax ---
    taxabilityType: z.enum(TAXABILITY_TYPES, {
      errorMap: () => ({ message: "Select the VAT treatment" }),
    }),
    vatRate: z
      .string()
      .refine((v) => v === "" || (!Number.isNaN(Number(v)) && Number(v) >= 0), {
        message: "VAT rate must be zero or greater",
      })
      .refine((v) => v === "" || Number(v) <= 100, {
        message: "VAT rate must not exceed 100",
      }),
    exciseType: z.enum(EXCISE_TYPES, {
      errorMap: () => ({ message: "Select the excise treatment" }),
    }),
    exciseRate: nonNegativeDecimal("Excise rate"),
    exciseAmount: nonNegativeDecimal("Excise amount"),

    // --- Pricing ---
    sellingPrice: z
      .string()
      .min(1, "Selling price is required")
      .refine((v) => !Number.isNaN(Number(v)), { message: "Selling price must be a number" })
      .refine((v) => Number(v) >= 0, { message: "Selling price must be zero or greater" }),
    purchasePrice: nonNegativeDecimal("Purchase price"),
    mrp: nonNegativeDecimal("MRP"),
    isVatInclusive: z.boolean(),

    // --- Inventory tracking ---
    isInventoryTracked: z.boolean(),
    isBatchTracked: z.boolean(),
    isExpiryTracked: z.boolean(),
    minStockLevel: nonNegativeDecimal("Minimum stock level"),
    reorderLevel: nonNegativeDecimal("Reorder level"),
    reorderQuantity: nonNegativeDecimal("Reorder quantity"),
  })
  .superRefine((values, ctx) => {
    const vatRate = values.vatRate === "" ? undefined : Number(values.vatRate);
    const exciseRate = values.exciseRate === "" ? undefined : Number(values.exciseRate);
    const exciseAmount = values.exciseAmount === "" ? undefined : Number(values.exciseAmount);

    // Exempt and zero-rated supplies must not carry a positive VAT rate. The
    // backend derives 0 when the field is omitted, so an empty value is fine.
    if (
      (values.taxabilityType === "EXEMPT" || values.taxabilityType === "ZERO_RATED") &&
      vatRate !== undefined &&
      vatRate > 0
    ) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["vatRate"],
        message: "Exempt and zero-rated items must have a VAT rate of 0",
      });
    }

    // A taxable supply with no explicit rate defaults to the standard 13% rate;
    // make that visible instead of letting the server decide silently.
    if (values.taxabilityType === "TAXABLE" && vatRate === undefined) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["vatRate"],
        message: `VAT rate is required for taxable items (standard rate is ${DEFAULT_VAT_RATE}%)`,
      });
    }

    if (values.exciseType === "PERCENTAGE") {
      if (exciseRate === undefined || exciseRate <= 0) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          path: ["exciseRate"],
          message: "A percentage excise rate above 0 is required",
        });
      }
    } else if (values.exciseType === "SPECIFIC_AMOUNT") {
      if (exciseAmount === undefined || exciseAmount <= 0) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          path: ["exciseAmount"],
          message: "A specific excise amount above 0 is required",
        });
      }
    }
  });

export type ItemFormValues = z.infer<typeof itemFormSchema>;

export const itemFormDefaults: ItemFormValues = {
  sku: "",
  nameEn: "",
  nameNp: "",
  barcode: "",
  hsCode: "",
  descriptionEn: "",
  descriptionNp: "",
  categoryId: "",
  uomId: "",
  itemType: "GOODS",
  taxabilityType: "TAXABLE",
  vatRate: String(DEFAULT_VAT_RATE),
  exciseType: "NONE",
  exciseRate: "",
  exciseAmount: "",
  sellingPrice: "",
  purchasePrice: "",
  mrp: "",
  isVatInclusive: true,
  isInventoryTracked: true,
  isBatchTracked: false,
  isExpiryTracked: false,
  minStockLevel: "",
  reorderLevel: "",
  reorderQuantity: "",
};

/** Empty string -> undefined, so the backend keeps its stored value. */
function optionalNumber(value: string): number | undefined {
  return value === "" ? undefined : Number(value);
}

/** Empty string -> undefined, so the backend stores null for that field. */
function optionalTextValue(value?: string): string | undefined {
  const trimmed = value?.trim();
  return trimmed ? trimmed : undefined;
}

/**
 * Expiry tracking is only meaningful with inventory *and* batch tracking on.
 * The checkboxes are disabled otherwise, so a stale `true` can survive in the
 * form state — normalising here keeps it from reaching the API.
 */
function resolveTrackingFlags(values: ItemFormValues) {
  const isInventoryTracked = values.isInventoryTracked;
  const isBatchTracked = isInventoryTracked && values.isBatchTracked;
  return {
    isInventoryTracked,
    isBatchTracked,
    isExpiryTracked: isInventoryTracked && isBatchTracked && values.isExpiryTracked,
  };
}

export function toCreateItemRequest(values: ItemFormValues): CreateItemRequest {
  const tracking = resolveTrackingFlags(values);
  return {
    sku: values.sku.trim(),
    nameEn: values.nameEn.trim(),
    uomId: Number(values.uomId),
    sellingPrice: Number(values.sellingPrice),
    barcode: optionalTextValue(values.barcode),
    hsCode: optionalTextValue(values.hsCode),
    nameNp: optionalTextValue(values.nameNp),
    descriptionEn: optionalTextValue(values.descriptionEn),
    descriptionNp: optionalTextValue(values.descriptionNp),
    categoryId: values.categoryId ? Number(values.categoryId) : undefined,
    itemType: values.itemType,
    taxabilityType: values.taxabilityType,
    vatRate: optionalNumber(values.vatRate),
    exciseType: values.exciseType,
    exciseRate: optionalNumber(values.exciseRate),
    exciseAmount: optionalNumber(values.exciseAmount),
    purchasePrice: optionalNumber(values.purchasePrice),
    mrp: optionalNumber(values.mrp),
    isVatInclusive: values.isVatInclusive,
    ...tracking,
    minStockLevel: optionalNumber(values.minStockLevel),
    reorderLevel: optionalNumber(values.reorderLevel),
    reorderQuantity: optionalNumber(values.reorderQuantity),
  };
}

/**
 * The result of diffing the form against the stored record.
 *
 * `unsavable` lists fields the user cleared that the API cannot clear. It is not
 * an error: those values are simply left as they were. Surfacing them keeps the
 * "updated successfully" toast honest instead of silently discarding an edit.
 */
export interface ItemUpdateDiff {
  payload: UpdateItemRequest;
  unsavable: string[];
}

/**
 * Diffs the form against the stored record to build a minimal patch.
 *
 * Two backend quirks drive the shape of this function:
 *
 * 1. `null` means "leave unchanged" in `UpdateItemRequest` and in
 *    `Item.applyChanges`, so it can never be used to clear a field. Text fields
 *    are cleared by sending `""`, which the entity runs through `trimToNull`.
 * 2. Optional numbers and the category have no clearing path at all — the
 *    entity only assigns them when the incoming value is non-null — so clearing
 *    them is reported through `unsavable` instead of being silently dropped.
 */
export function toUpdateItemDiff(
  values: ItemFormValues,
  original: Item
): ItemUpdateDiff {
  const payload: UpdateItemRequest = {};
  const unsavable: string[] = [];

  const sku = values.sku.trim();
  const nameEn = values.nameEn.trim();
  const uomId = Number(values.uomId);
  const sellingPrice = Number(values.sellingPrice);

  if (sku !== original.sku) payload.sku = sku;
  if (nameEn !== original.nameEn) payload.nameEn = nameEn;
  if (uomId !== original.uomId) payload.uomId = uomId;
  if (sellingPrice !== Number(original.sellingPrice)) payload.sellingPrice = sellingPrice;

  const textFields = [
    ["barcode", values.barcode, original.barcode],
    ["hsCode", values.hsCode, original.hsCode],
    ["nameNp", values.nameNp, original.nameNp],
    ["descriptionEn", values.descriptionEn, original.descriptionEn],
    ["descriptionNp", values.descriptionNp, original.descriptionNp],
  ] as const;

  for (const [field, next, current] of textFields) {
    const trimmed = next?.trim() ?? "";
    // An empty string is the entity's "clear this" signal, not a no-op.
    if (trimmed !== (current ?? "")) {
      payload[field] = trimmed;
    }
  }

  const categoryId = values.categoryId ? Number(values.categoryId) : null;
  if (categoryId !== (original.categoryId ?? null)) {
    if (categoryId === null) {
      unsavable.push("category");
    } else {
      payload.categoryId = categoryId;
    }
  }

  if (values.itemType !== original.itemType) payload.itemType = values.itemType;
  if (values.taxabilityType !== original.taxabilityType) {
    payload.taxabilityType = values.taxabilityType;
  }
  if (values.exciseType !== original.exciseType) payload.exciseType = values.exciseType;

  const numberFields = [
    ["vatRate", values.vatRate, original.vatRate, "VAT rate"],
    ["exciseRate", values.exciseRate, original.exciseRate, "excise rate"],
    ["exciseAmount", values.exciseAmount, original.exciseAmount, "excise amount"],
    ["purchasePrice", values.purchasePrice, original.purchasePrice, "purchase price"],
    ["mrp", values.mrp, original.mrp, "MRP"],
    ["minStockLevel", values.minStockLevel, original.minStockLevel, "minimum stock level"],
    ["reorderLevel", values.reorderLevel, original.reorderLevel, "reorder level"],
    [
      "reorderQuantity",
      values.reorderQuantity,
      original.reorderQuantity,
      "reorder quantity",
    ],
  ] as const;

  for (const [field, next, current, label] of numberFields) {
    const parsed = optionalNumber(next);
    const currentValue =
      current === null || current === undefined ? undefined : Number(current);
    if (parsed === currentValue) continue;

    if (parsed === undefined) {
      // No API path clears an optional number, so keep the stored value.
      unsavable.push(label);
    } else {
      payload[field] = parsed;
    }
  }

  const tracking = resolveTrackingFlags(values);
  const flagFields = [
    ["isVatInclusive", values.isVatInclusive, original.isVatInclusive],
    ["isInventoryTracked", tracking.isInventoryTracked, original.isInventoryTracked],
    ["isBatchTracked", tracking.isBatchTracked, original.isBatchTracked],
    ["isExpiryTracked", tracking.isExpiryTracked, original.isExpiryTracked],
  ] as const;

  for (const [field, next, current] of flagFields) {
    if (next !== current) payload[field] = next;
  }

  return { payload, unsavable };
}

/** Seeds the edit form from an existing record, using "" for null numerics. */
export function itemToFormValues(item: Item): ItemFormValues {
  return {
    sku: item.sku,
    nameEn: item.nameEn,
    nameNp: item.nameNp ?? "",
    barcode: item.barcode ?? "",
    hsCode: item.hsCode ?? "",
    descriptionEn: item.descriptionEn ?? "",
    descriptionNp: item.descriptionNp ?? "",
    categoryId: item.categoryId?.toString() ?? "",
    uomId: item.uomId.toString(),
    itemType: item.itemType ?? "GOODS",
    taxabilityType: item.taxabilityType ?? "TAXABLE",
    vatRate: item.vatRate === null ? "" : String(item.vatRate),
    exciseType: item.exciseType ?? "NONE",
    exciseRate: item.exciseRate === null ? "" : String(item.exciseRate),
    exciseAmount: item.exciseAmount === null ? "" : String(item.exciseAmount),
    sellingPrice: String(item.sellingPrice ?? ""),
    purchasePrice: item.purchasePrice === null ? "" : String(item.purchasePrice),
    mrp: item.mrp === null ? "" : String(item.mrp),
    isVatInclusive: item.isVatInclusive ?? true,
    isInventoryTracked: item.isInventoryTracked ?? true,
    isBatchTracked: item.isBatchTracked ?? false,
    isExpiryTracked: item.isExpiryTracked ?? false,
    minStockLevel: item.minStockLevel === null ? "" : String(item.minStockLevel),
    reorderLevel: item.reorderLevel === null ? "" : String(item.reorderLevel),
    reorderQuantity: item.reorderQuantity === null ? "" : String(item.reorderQuantity),
  };
}
