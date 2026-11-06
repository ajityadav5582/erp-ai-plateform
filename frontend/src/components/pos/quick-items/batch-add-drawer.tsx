"use client";

import * as React from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetDescription,
} from "@/components/ui/sheet";
import { useGetActiveItemsQuery } from "@/services/item.service";
import { Loader2 } from "lucide-react";

interface BatchAddDrawerProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onAdd: (itemIds: number[]) => Promise<void>;
  isSubmitting: boolean;
}

export function BatchAddDrawer({ open, onOpenChange, onAdd, isSubmitting }: BatchAddDrawerProps) {
  const [search, setSearch] = React.useState("");
  const [selected, setSelected] = React.useState<Set<number>>(new Set());
  const { data, isLoading } = useGetActiveItemsQuery({ size: 100, search: search.trim() || undefined });

  const items = data?.content ?? [];

  const toggle = (id: number) => {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const handleAdd = async () => {
    await onAdd(Array.from(selected));
    setSelected(new Set());
    onOpenChange(false);
  };

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent side="right" className="w-full sm:max-w-md">
        <SheetHeader>
          <SheetTitle>Batch add quick items</SheetTitle>
          <SheetDescription>Select multiple catalogue items to auto-fill empty slots.</SheetDescription>
        </SheetHeader>
        <div className="mt-4 space-y-3">
          <Input
            placeholder="Search items…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
          <div className="max-h-[60vh] space-y-1 overflow-y-auto">
            {isLoading ? (
              <div className="flex items-center gap-2 py-4 text-muted-foreground">
                <Loader2 className="h-4 w-4 animate-spin" /> Loading…
              </div>
            ) : items.length === 0 ? (
              <p className="py-4 text-sm text-muted-foreground">No items found.</p>
            ) : (
              items.map((it) => (
                <label
                  key={it.id}
                  className="flex cursor-pointer items-center gap-2 rounded-md border p-2 hover:bg-muted"
                >
                  <Checkbox
                    checked={selected.has(it.id)}
                    onCheckedChange={() => toggle(it.id)}
                  />
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium">{it.nameEn}</p>
                    <p className="text-xs text-muted-foreground">
                      {it.sku} · {it.sellingPrice}
                    </p>
                  </div>
                </label>
              ))
            )}
          </div>
          <Button
            className="w-full"
            disabled={selected.size === 0 || isSubmitting}
            onClick={() => void handleAdd()}
          >
            {isSubmitting ? "Adding…" : `Add ${selected.size} item(s)`}
          </Button>
        </div>
      </SheetContent>
    </Sheet>
  );
}
