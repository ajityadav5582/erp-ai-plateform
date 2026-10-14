import * as React from "react";
import { usePreviewPosPricesMutation, type PricePreview } from "@/services/pos-scan.service";
import type { PosVatType } from "@/services/pos-scan.service";

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
export function usePosCart() {
  const [lines, setLines] = React.useState<CartLine[]>([]);
  const [preview, setPreview] = React.useState<PricePreview>(EMPTY_PREVIEW);
  const [previewPrices, previewState] = usePreviewPosPricesMutation();

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
    setLines((current) => {
      const existing = current.find((l) => l.itemId === line.itemId);
      if (existing) {
        // Same item scanned twice: bump the quantity rather than duplicating.
        return current.map((l) =>
          l.itemId === line.itemId ? { ...l, quantity: l.quantity + line.quantity } : l
        );
      }
      return [...current, line];
    });
  }, []);

  const setQuantity = React.useCallback((itemId: number, quantity: number) => {
    setLines((current) =>
      quantity <= 0
        ? current.filter((l) => l.itemId !== itemId)
        : current.map((l) => (l.itemId === itemId ? { ...l, quantity } : l))
    );
  }, []);

  const setDiscount = React.useCallback((itemId: number, discountPercentage: number) => {
    setLines((current) =>
      current.map((l) => (l.itemId === itemId ? { ...l, discountPercentage } : l))
    );
  }, []);

  const removeLine = React.useCallback((itemId: number) => {
    setLines((current) => current.filter((l) => l.itemId !== itemId));
  }, []);

  const clear = React.useCallback(() => {
    setLines([]);
    setPreview(EMPTY_PREVIEW);
  }, []);

  /** Replaces the cart with a parked transaction's lines (resume). */
  const loadLines = React.useCallback((next: CartLine[]) => {
    setLines(next);
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
