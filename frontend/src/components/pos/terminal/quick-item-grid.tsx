"use client";

import * as React from "react";
import {
  Apple,
  Beer,
  Cake,
  Coffee,
  Cookie,
  CupSoda,
  LayoutGrid,
  Loader2,
  Milk,
  Pizza,
  Sandwich,
  ShoppingBag,
  Utensils,
  Wine,
  type LucideIcon,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { useGetPosQuickItemGridQuery, type PosQuickItem } from "@/services/pos-quick-item.service";
import { formatPosCurrency } from "@/lib/pos-format";

/**
 * The curated icon set a tile may carry. Kept small on purpose: a cashier
 * navigates by position and colour, so the icon is a hint, not a catalogue.
 * Shared with the admin picker so the two never drift.
 */
export const TILE_ICONS: Record<string, LucideIcon> = {
  coffee: Coffee,
  utensils: Utensils,
  "shopping-bag": ShoppingBag,
  cookie: Cookie,
  "cup-soda": CupSoda,
  pizza: Pizza,
  sandwich: Sandwich,
  apple: Apple,
  wine: Wine,
  beer: Beer,
  milk: Milk,
  cake: Cake,
};

interface QuickItemGridProps {
  registerId: number | null;
  /** Called with the tapped tile; the terminal resolves its price server-side. */
  onSelect: (quickItem: PosQuickItem) => void;
  disabled?: boolean;
}

/**
 * The terminal's quick-access item grid.
 *
 * Mobile-first: two columns on a phone (thumb-sized targets), scaling up to
 * four on a tablet and six on a desktop. Tiles carry their configured colour
 * and icon so the cashier navigates by position and colour, not by reading.
 */
export function QuickItemGrid({ registerId, onSelect, disabled }: QuickItemGridProps) {
  const { data, isLoading } = useGetPosQuickItemGridQuery(registerId ?? 0, {
    skip: registerId === null,
  });

  if (registerId === null) {
    return (
      <div className="flex flex-col items-center justify-center gap-2 rounded-lg border border-dashed py-10 text-center">
        <LayoutGrid className="h-8 w-8 text-muted-foreground" />
        <p className="text-sm text-muted-foreground">
          Open a session to load this register&apos;s quick items.
        </p>
      </div>
    );
  }

  if (isLoading) {
    return (
      <div className="grid grid-cols-2 gap-2 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-6">
        {Array.from({ length: 8 }).map((_, index) => (
          <Skeleton key={index} className="h-20 rounded-lg" />
        ))}
      </div>
    );
  }

  const tiles = data ?? [];

  if (tiles.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center gap-2 rounded-lg border border-dashed py-10 text-center">
        <LayoutGrid className="h-8 w-8 text-muted-foreground" />
        <p className="text-sm text-muted-foreground">
          No quick items yet. Add tiles from POS → Quick Items.
        </p>
      </div>
    );
  }

  return (
    <div className="grid grid-cols-2 gap-2 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-6">
      {tiles.map((tile) => {
        const TileIcon = tile.displayIcon ? TILE_ICONS[tile.displayIcon] : undefined;
        return (
          <Button
            key={tile.id}
            type="button"
            variant="outline"
            disabled={disabled}
            onClick={() => onSelect(tile)}
            className="flex h-20 flex-col items-start justify-start gap-1 p-3 text-left"
            style={
              tile.displayColor
                ? { borderColor: tile.displayColor, borderLeftWidth: 4, borderLeftColor: tile.displayColor }
                : undefined
            }
          >
            {TileIcon && <TileIcon className="h-4 w-4 text-muted-foreground" />}
            <span className="line-clamp-2 text-sm font-medium leading-tight">{tile.itemName}</span>
            <span className="mt-auto text-xs text-muted-foreground">
              {tile.defaultPrice != null ? formatPosCurrency(tile.defaultPrice) : tile.itemCode}
            </span>
          </Button>
        );
      })}
    </div>
  );
}

/** Small inline spinner used while a scan resolves. */
export function ScanSpinner() {
  return <Loader2 className="h-4 w-4 animate-spin text-muted-foreground" />;
}
