import * as React from "react";
import { usePreviewPosPricesMutation, type PricePreview } from "@/services/pos-scan.service";
import type { PosVatType } from "@/services/pos-scan.service";
import { clampDiscount, mergeCartLine, normalizeQuantity, sanitizeCartLines } from "@/lib/pos-cart-rules";

/** One line in the terminal's working cart. */
export interface CartLine {
  itemId: number;
  itemCode: string;
  itemName: string;
  uomCode: string;
  quantity: number;
  discountPercentage: number;
  /** Snapshot of the price at add time; the authoritative figure is the preview. */
  unitPrice: number;
  vatType: PosVatType;
  vatRate: number;
}

const EMPTY_PREVIEW: PricePreview = { lines: [], subtotal: 0, vatTotal: 0, total: 0 };

/**
 * The terminal's working cart.
 *
 * Prices live on the server: every cart change re-prices the whole cart through
 * the pricing preview endpoint, so the totals on screen are the same figures
 * checkout will persist. The local `unitPrice` snapshot exists only to render a
 * line immediately after a scan, before the preview round-trip lands.
 */
export function usePosCart(companyId?: number | null) {
  const [lines, setLines] = React.useState<CartLine[]>([]);
  const [preview, setPreview] = React.useState<PricePreview>(EMPTY_PREVIEW);
  const [previewPrices, previewState] = usePreviewPosPricesMutation();

  const storageKey = companyId ? `pos_retail_cart_${companyId}` : "pos_retail_cart";

  // Restore draft cart from localStorage on mount or company change
  React.useEffect(() => {
    try {
      const saved = localStorage.getItem(storageKey);
      if (saved) {
        const parsed = JSON.parse(saved);
        if (Array.isArray(parsed) && parsed.length > 0 && lines.length === 0) {
          setLines(sanitizeCartLines(parsed));
        }
      }
    } catch {
      // ignore
    }
  }, [storageKey, lines.length]);

  // Persist draft cart to localStorage on change
  React.useEffect(() => {
    try {
      if (lines.length > 0) {
        localStorage.setItem(storageKey, JSON.stringify(lines));
      } else {
        localStorage.removeItem(storageKey);
      }
    } catch {
      // ignore
    }
  }, [lines, storageKey]);

  // Re-price the cart whenever it changes. Debounced so a fast scan burst
  // collapses into one request instead of one per item.
  React.useEffect(() => {
    if (lines.length === 0) {
      setPreview(EMPTY_PREVIEW);
      return;
    }
    const handle = setTimeout(() => {
      void previewPrices(
        lines.map((line) => ({
          itemId: line.itemId,
          quantity: line.quantity,
          discountPercentage: line.discountPercentage,
        }))
      )
        .unwrap()
        .then(setPreview)
        .catch(() => {
          // A failed preview leaves the last good totals on screen; checkout
          // re-prices authoritatively and will surface a real error.
        });
    }, 150);
    return () => clearTimeout(handle);
  }, [lines, previewPrices]);

  const addLine = React.useCallback((line: CartLine) => {
    setLines((current) => mergeCartLine(current, line));
  }, []);

  const setQuantity = React.useCallback((itemId: number, quantity: number) => {
    const normalized = normalizeQuantity(quantity);
    setLines((current) =>
      normalized <= 0
        ? current.filter((l) => l.itemId !== itemId)
        : current.map((l) => (l.itemId === itemId ? { ...l, quantity: normalized } : l))
    );
  }, []);

  const setDiscount = React.useCallback((itemId: number, discountPercentage: number) => {
    const clamped = clampDiscount(discountPercentage);
    setLines((current) =>
      current.map((l) => (l.itemId === itemId ? { ...l, discountPercentage: clamped } : l))
    );
  }, []);

  const removeLine = React.useCallback((itemId: number) => {
    setLines((current) => current.filter((l) => l.itemId !== itemId));
  }, []);

  const clear = React.useCallback(() => {
    setLines([]);
    setPreview(EMPTY_PREVIEW);
    try {
      localStorage.removeItem(storageKey);
    } catch {
      // ignore
    }
  }, [storageKey]);

  /** Replaces the cart with a parked transaction's lines (resume). */
  const loadLines = React.useCallback((next: CartLine[]) => {
    setLines(sanitizeCartLines(next));
  }, []);

  return {
    lines,
    preview,
    isPreviewing: previewState.isLoading,
    addLine,
    setQuantity,
    setDiscount,
    removeLine,
    clear,
    loadLines,
  };
}
