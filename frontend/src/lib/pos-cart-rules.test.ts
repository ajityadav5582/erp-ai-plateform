import {
  clampDiscount,
  mergeCartLine,
  normalizeQuantity,
  sanitizeCartLines,
} from "./pos-cart-rules";
import type { CartLine } from "@/hooks/use-pos-cart";

function assert(condition: boolean, message: string): void {
  if (!condition) throw new Error(message);
}

const baseLine: CartLine = {
  itemId: 1,
  itemCode: "ITEM-1",
  itemName: "Item 1",
  uomCode: "PCS",
  quantity: 2,
  discountPercentage: 10,
  unitPrice: 100,
  vatType: "TAXABLE",
  vatRate: 0.13,
};

export function runPosCartRuleTests(): void {
  assert(normalizeQuantity(3.9) === 3, "normalizeQuantity floors");
  assert(normalizeQuantity(0) === 0, "normalizeQuantity keeps zero");
  assert(normalizeQuantity(-2) === 0, "normalizeQuantity clamps negatives");
  assert(normalizeQuantity(NaN) === 0, "normalizeQuantity handles NaN");

  assert(clampDiscount(150) === 100, "clampDiscount upper bound");
  assert(clampDiscount(-5) === 0, "clampDiscount lower bound");
  assert(clampDiscount(25.5) === 25.5, "clampDiscount keeps valid");

  const merged = mergeCartLine([baseLine], { ...baseLine, quantity: 1 });
  assert(merged.length === 1, "mergeCartLine does not duplicate");
  assert(merged[0]!.quantity === 3, "mergeCartLine bumps quantity");

  const added = mergeCartLine([], { ...baseLine, itemId: 2, quantity: 0 });
  assert(added.length === 1 && added[0]!.quantity === 1, "mergeCartLine normalises invalid quantity");

  const sanitized = sanitizeCartLines([
    { ...baseLine, quantity: 0 },
    { ...baseLine, itemId: 2, quantity: 2, discountPercentage: 200 },
  ]);
  assert(sanitized.length === 1, "sanitizeCartLines removes invalid lines");
  assert(sanitized[0]!.discountPercentage === 100, "sanitizeCartLines clamps discount");
}

// Intentionally no auto-run; invoke runPosCartRuleTests() in a test runner.
