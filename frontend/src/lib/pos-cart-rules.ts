import type { CartLine } from "@/hooks/use-pos-cart";

/** Normalises a user-entered quantity: integers only, never negative. */
export function normalizeQuantity(quantity: number): number {
  if (!Number.isFinite(quantity)) return 0;
  const floored = Math.floor(quantity);
  return floored < 0 ? 0 : floored;
}

/** Clamps a discount percentage to the valid 0-100 range. */
export function clampDiscount(discountPercentage: number): number {
  if (!Number.isFinite(discountPercentage)) return 0;
  return Math.min(100, Math.max(0, discountPercentage));
}

/** Adds a line, bumping the quantity when the same item is already in the cart. */
export function mergeCartLine(lines: CartLine[], line: CartLine): CartLine[] {
  const quantity = Math.max(1, Math.floor(Number.isFinite(line.quantity) ? line.quantity : 1));
  const normalizedLine: CartLine = { ...line, quantity, discountPercentage: clampDiscount(line.discountPercentage) };

  const existing = lines.find((current) => current.itemId === line.itemId);
  if (existing) {
    return lines.map((current) =>
      current.itemId === line.itemId
        ? { ...current, quantity: current.quantity + quantity }
        : current
    );
  }
  return [...lines, normalizedLine];
}

/** Removes invalid lines and normalises quantities/discounts. */
export function sanitizeCartLines(lines: CartLine[]): CartLine[] {
  return lines
    .map((line) => ({
      ...line,
      quantity: normalizeQuantity(line.quantity),
      discountPercentage: clampDiscount(line.discountPercentage),
    }))
    .filter((line) => line.quantity > 0);
}
