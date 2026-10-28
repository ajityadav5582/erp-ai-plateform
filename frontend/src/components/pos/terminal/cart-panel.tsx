"use client";

import * as React from "react";
import { Minus, Plus, Trash2, ShoppingCart } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Separator } from "@/components/ui/separator";
import type { PricePreview } from "@/services/pos-scan.service";
import type { CartLine } from "@/hooks/use-pos-cart";
import { formatPosCurrency as currency } from "@/lib/pos-format";

interface CartPanelProps {
  lines: CartLine[];
  preview: PricePreview;
  isPreviewing: boolean;
  onQuantityChange: (itemId: number, quantity: number) => void;
  onDiscountChange: (itemId: number, discountPercentage: number) => void;
  onRemove: (itemId: number) => void;
  onClear: () => void;
  /** Rendered under the totals — the checkout / park actions. */
  actions?: React.ReactNode;
}

/**
 * The terminal's cart: editable lines with the server's authoritative totals.
 *
 * Used two ways: as the desktop side panel, and inside the mobile bottom sheet.
 * Quantity steppers are sized for thumbs; removing a line is one tap. Each line
 * shows the server-priced net amount (discount already applied by the preview)
 * and carries an inline discount control that re-prices the whole cart, so the
 * figure on screen is always what checkout will persist.
 */
export function CartPanel({
  lines,
  preview,
  isPreviewing,
  onQuantityChange,
  onDiscountChange,
  onRemove,
  onClear,
  actions,
}: CartPanelProps) {
  // The preview is authoritative: match each cart line to its priced line so the
  // on-screen amount equals what checkout persists. Falls back to the local
  // snapshot only for the instant before a preview round-trip lands.
  const pricedByItem = React.useMemo(
    () => new Map(preview.lines.map((l) => [l.itemId, l])),
    [preview.lines]
  );

  if (lines.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center gap-3 py-10 text-center">
        <div className="flex h-12 w-12 items-center justify-center rounded-full bg-muted">
          <ShoppingCart className="h-6 w-6 text-muted-foreground" aria-hidden="true" />
        </div>
        <div className="flex flex-col gap-1">
          <p className="text-sm font-semibold text-foreground">Cart is empty</p>
          <p className="text-xs text-muted-foreground max-w-xs">
            Scan a barcode or tap a quick item to start a sale.
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="flex h-full flex-col">
      <div className="flex items-center justify-between">
        <h2 className="text-sm font-semibold">
          Cart <span className="text-muted-foreground">({lines.length})</span>
        </h2>
        <Button variant="ghost" size="sm" onClick={onClear} className="text-muted-foreground">
          Clear
        </Button>
      </div>

      <ul className="mt-2 flex-1 space-y-3 overflow-y-auto">
        {lines.map((line) => {
          const priced = pricedByItem.get(line.itemId);
          const lineTotal = priced?.lineAmount ?? line.quantity * line.unitPrice;
          const saved = priced ? priced.unitPrice * line.quantity - priced.lineAmount : 0;
          return (
            <li key={line.itemId} className="rounded-lg border p-3">
              <div className="flex items-start justify-between gap-2">
                <div className="min-w-0">
                  <p className="truncate text-sm font-medium">{line.itemName}</p>
                  <p className="text-xs text-muted-foreground">
                    {currency(line.unitPrice)} · {line.uomCode}
                  </p>
                </div>
                <Button
                  variant="ghost"
                  size="icon"
                  className="h-8 w-8 shrink-0 text-muted-foreground"
                  onClick={() => onRemove(line.itemId)}
                  aria-label={`Remove ${line.itemName}`}
                >
                  <Trash2 className="h-4 w-4" />
                </Button>
              </div>

              <div className="mt-2 flex items-center justify-between">
                <div className="flex items-center gap-1">
                  <Button
                    variant="outline"
                    size="icon"
                    className="h-9 w-9"
                    onClick={() => onQuantityChange(line.itemId, line.quantity - 1)}
                    aria-label={`Decrease ${line.itemName} quantity`}
                  >
                    <Minus className="h-4 w-4" />
                  </Button>
                  <Input
                    type="number"
                    min={1}
                    value={line.quantity}
                    onChange={(event) => {
                      const next = Number(event.target.value);
                      if (Number.isFinite(next)) onQuantityChange(line.itemId, next);
                    }}
                    className="h-9 w-14 text-center"
                    aria-label={`${line.itemName} quantity`}
                  />
                  <Button
                    variant="outline"
                    size="icon"
                    className="h-9 w-9"
                    onClick={() => onQuantityChange(line.itemId, line.quantity + 1)}
                    aria-label={`Increase ${line.itemName} quantity`}
                  >
                    <Plus className="h-4 w-4" />
                  </Button>
                </div>
                <p className="text-sm font-semibold">{currency(lineTotal)}</p>
              </div>

              <div className="mt-2 flex items-center justify-between gap-2">
                <Label
                  htmlFor={`discount-${line.itemId}`}
                  className="text-xs text-muted-foreground"
                >
                  Discount %
                </Label>
                <div className="flex items-center gap-2">
                  {saved > 0 && (
                    <span className="text-xs text-emerald-600">−{currency(saved)}</span>
                  )}
                  <Input
                    id={`discount-${line.itemId}`}
                    type="number"
                    min={0}
                    max={100}
                    step={1}
                    value={line.discountPercentage}
                    onChange={(event) => {
                      const next = Number(event.target.value);
                      if (Number.isFinite(next)) {
                        onDiscountChange(line.itemId, Math.min(100, Math.max(0, next)));
                      }
                    }}
                    className="h-8 w-16 text-center text-xs"
                    aria-label={`${line.itemName} discount percentage`}
                  />
                </div>
              </div>
            </li>
          );
        })}
      </ul>

      <div className="mt-3 space-y-1 text-sm">
        <div className="flex justify-between">
          <span className="text-muted-foreground">Subtotal</span>
          <span>{currency(preview.subtotal)}</span>
        </div>
        <div className="flex justify-between">
          <span className="text-muted-foreground">VAT</span>
          <span>{currency(preview.vatTotal)}</span>
        </div>
        <Separator className="my-1" />
        <div className="flex items-center justify-between">
          <span className="text-base font-semibold">Total</span>
          <span className="text-xl font-bold" data-testid="cart-total">
            {currency(preview.total)}
          </span>
        </div>
        {isPreviewing && (
          <p className="text-right text-xs text-muted-foreground">Re-pricing…</p>
        )}
      </div>

      {actions && <div className="mt-3 space-y-2">{actions}</div>}
    </div>
  );
}
