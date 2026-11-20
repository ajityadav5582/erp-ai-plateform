"use client";

import { useMemo, useState } from "react";
import { Check, ChevronsUpDown, Package, Search, X } from "lucide-react";
import type { ItemListResponse } from "@/services/item.service";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";

export interface ItemComboboxProps {
  value?: string;
  items: ItemListResponse[];
  disabled?: boolean;
  placeholder?: string;
  onSelect: (item: ItemListResponse) => void;
  fallbackSnapshot?: { code: string; name: string; uom?: string };
  error?: boolean;
  className?: string;
}

export function ItemCombobox({
  value,
  items,
  disabled = false,
  placeholder = "Search and select item…",
  onSelect,
  fallbackSnapshot,
  error = false,
  className,
}: ItemComboboxProps) {
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState("");

  const selectedItem = useMemo(() => {
    if (!value) return null;
    return items.find((it) => String(it.id) === String(value)) ?? null;
  }, [items, value]);

  const display = useMemo(() => {
    if (selectedItem) {
      return {
        code: selectedItem.sku,
        name: selectedItem.nameEn,
        uom: selectedItem.uomCode,
      };
    }
    if (fallbackSnapshot?.code || fallbackSnapshot?.name) {
      return {
        code: fallbackSnapshot.code,
        name: fallbackSnapshot.name,
        uom: fallbackSnapshot.uom,
      };
    }
    return null;
  }, [selectedItem, fallbackSnapshot]);

  const filteredItems = useMemo(() => {
    const term = search.trim().toLowerCase();
    if (!term) return items.slice(0, 60);

    return items
      .filter((it) => {
        const nameEn = it.nameEn?.toLowerCase() ?? "";
        const nameNp = it.nameNp?.toLowerCase() ?? "";
        const sku = it.sku?.toLowerCase() ?? "";
        const barcode = it.barcode?.toLowerCase() ?? "";
        const category = it.categoryName?.toLowerCase() ?? "";
        return (
          nameEn.includes(term) ||
          nameNp.includes(term) ||
          sku.includes(term) ||
          barcode.includes(term) ||
          category.includes(term)
        );
      })
      .slice(0, 60);
  }, [items, search]);

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button
          type="button"
          variant="outline"
          role="combobox"
          aria-expanded={open}
          disabled={disabled}
          className={cn(
            "h-9 w-full justify-between px-2.5 font-normal text-left shadow-none transition-colors",
            error && "border-destructive focus-visible:ring-destructive",
            !display && "text-muted-foreground",
            className
          )}
        >
          {display ? (
            <div className="flex min-w-0 items-center gap-1.5 truncate">
              <span className="shrink-0 rounded bg-muted px-1.5 py-0.5 font-mono text-[11px] font-semibold text-foreground">
                {display.code}
              </span>
              <span className="truncate text-xs font-medium text-foreground sm:text-sm">
                {display.name}
              </span>
              {display.uom ? (
                <span className="shrink-0 text-[11px] text-muted-foreground">
                  ({display.uom})
                </span>
              ) : null}
            </div>
          ) : (
            <div className="flex items-center gap-2 text-xs text-muted-foreground sm:text-sm">
              <Search className="size-3.5 shrink-0 opacity-60" />
              <span className="truncate">{placeholder}</span>
            </div>
          )}
          <ChevronsUpDown className="ml-1 size-3.5 shrink-0 opacity-50" />
        </Button>
      </PopoverTrigger>

      <PopoverContent
        align="start"
        sideOffset={4}
        className="w-[calc(100vw-2.5rem)] p-0 sm:w-[380px]"
      >
        <div className="flex items-center border-b border-border px-2.5 py-2">
          <Search className="mr-2 size-4 shrink-0 text-muted-foreground" />
          <Input
            autoFocus
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search by name, SKU or code…"
            className="h-8 border-none bg-transparent p-0 text-xs shadow-none focus-visible:ring-0 sm:text-sm"
          />
          {search ? (
            <Button
              type="button"
              variant="ghost"
              size="icon"
              className="size-6 text-muted-foreground hover:text-foreground"
              onClick={() => setSearch("")}
            >
              <X className="size-3.5" />
            </Button>
          ) : null}
        </div>

        <div className="max-h-60 overflow-y-auto p-1 sm:max-h-68">
          {filteredItems.length === 0 ? (
            <div className="py-6 text-center text-xs text-muted-foreground">
              <Package className="mx-auto mb-1.5 size-5 opacity-40" />
              No items matching &ldquo;{search}&rdquo;
            </div>
          ) : (
            filteredItems.map((item) => {
              const isSelected = String(item.id) === String(value);
              return (
                <button
                  key={item.id}
                  type="button"
                  onClick={() => {
                    onSelect(item);
                    setOpen(false);
                    setSearch("");
                  }}
                  className={cn(
                    "flex w-full items-start justify-between gap-2 rounded-md px-2.5 py-2 text-left text-xs transition-colors hover:bg-accent",
                    isSelected && "bg-accent/80 font-medium"
                  )}
                >
                  <div className="flex min-w-0 flex-col gap-0.5">
                    <div className="flex items-center gap-1.5">
                      <span className="shrink-0 rounded bg-muted/80 px-1 py-0.2 font-mono text-[10px] font-semibold text-foreground">
                        {item.sku}
                      </span>
                      <span className="truncate font-medium text-foreground">
                        {item.nameEn}
                      </span>
                      {isSelected ? (
                        <Check className="size-3.5 shrink-0 text-primary" />
                      ) : null}
                    </div>
                    {item.nameNp ? (
                      <span className="truncate font-nepali text-[11px] text-muted-foreground">
                        {item.nameNp}
                      </span>
                    ) : item.categoryName ? (
                      <span className="text-[11px] text-muted-foreground">
                        {item.categoryName}
                      </span>
                    ) : null}
                  </div>

                  <div className="flex shrink-0 flex-col items-end gap-0.5 text-right text-[11px] text-muted-foreground">
                    {item.uomCode ? (
                      <span className="rounded bg-muted px-1.5 py-0.2 text-[10px] font-medium text-muted-foreground">
                        {item.uomCode}
                      </span>
                    ) : null}
                    {item.purchasePrice != null ? (
                      <span className="font-mono text-[10px] text-foreground">
                        रू {Number(item.purchasePrice).toLocaleString(undefined, {
                          minimumFractionDigits: 2,
                          maximumFractionDigits: 2,
                        })}
                      </span>
                    ) : null}
                  </div>
                </button>
              );
            })
          )}
        </div>
      </PopoverContent>
    </Popover>
  );
}

