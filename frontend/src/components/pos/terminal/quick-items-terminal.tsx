"use client";

import * as React from "react";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { useGetPosQuickItemGridQuery, type PosQuickItem } from "@/services/pos-quick-item.service";
import { TILE_ICONS } from "@/components/pos/terminal/quick-item-grid";
import { formatPosCurrency } from "@/lib/pos-format";
import { cn } from "@/lib/utils";

interface QuickItemsTerminalProps {
  registerId: number | null;
  onSelect: (quickItem: PosQuickItem) => void;
  disabled?: boolean;
}

export function QuickItemsTerminal({ registerId, onSelect, disabled }: QuickItemsTerminalProps) {
  const { data, isLoading } = useGetPosQuickItemGridQuery(registerId ?? 0, {
    skip: registerId === null,
  });

  const items = data ?? [];
  const tabs = React.useMemo(() => {
    const set = new Set<string>();
    items.forEach((it) => set.add(it.tabName || "All Items"));
    if (set.size === 0) set.add("All Items");
    return Array.from(set);
  }, [items]);

  const [activeTab, setActiveTab] = React.useState(tabs[0] ?? "All Items");
  const [page, setPage] = React.useState(1);
  const [folderOpen, setFolderOpen] = React.useState<PosQuickItem | null>(null);

  React.useEffect(() => {
    if (!tabs.includes(activeTab)) setActiveTab(tabs[0] ?? "All Items");
  }, [tabs, activeTab]);

  const pages = React.useMemo(() => {
    const set = new Set<number>();
    items
      .filter((it) => (it.tabName || "All Items") === activeTab)
      .forEach((it) => set.add(it.pageNumber || 1));
    if (set.size === 0) set.add(1);
    return Array.from(set).sort((a, b) => a - b);
  }, [items, activeTab]);

  React.useEffect(() => {
    if (!pages.includes(page)) setPage(pages[0] ?? 1);
  }, [pages, page]);

  const visible = items.filter(
    (it) => (it.tabName || "All Items") === activeTab && (it.pageNumber || 1) === page,
  );

  const folderChildren = React.useMemo(() => {
    if (!folderOpen) return [];
    return items.filter((it) => it.parentId === folderOpen.id);
  }, [folderOpen, items]);

  // Hotkey listener
  React.useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if (disabled) return;
      const key = e.key;
      const tile = items.find((it) => it.hotkey && it.hotkey.toLowerCase() === key.toLowerCase());
      if (tile) {
        e.preventDefault();
        if (tile.itemType === "FOLDER") {
          setFolderOpen(tile);
        } else {
          onSelect(tile);
        }
      }
    };
    window.addEventListener("keydown", handler);
    return () => window.removeEventListener("keydown", handler);
  }, [items, disabled, onSelect]);

  const handleTileClick = (tile: PosQuickItem) => {
    if (disabled) return;
    if (tile.itemType === "FOLDER") {
      setFolderOpen(tile);
    } else {
      onSelect(tile);
    }
  };

  if (registerId === null) {
    return (
      <div className="flex flex-col items-center justify-center gap-2 rounded-lg border border-dashed py-10 text-center">
        <p className="text-sm text-muted-foreground">Open a session to load quick items.</p>
      </div>
    );
  }

  if (isLoading) {
    return (
      <div className="grid grid-cols-3 gap-2 sm:grid-cols-6">
        {Array.from({ length: 18 }).map((_, i) => (
          <div key={i} className="h-20 animate-pulse rounded-lg bg-muted" />
        ))}
      </div>
    );
  }

  return (
    <div className="space-y-2">
      {/* Tabs */}
      <div className="flex flex-wrap gap-1">
        {tabs.map((t) => (
          <Button
            key={t}
            size="sm"
            variant={activeTab === t ? "default" : "outline"}
            onClick={() => {
              setActiveTab(t);
              setPage(1);
            }}
          >
            {t}
          </Button>
        ))}
      </div>
      {/* Pages */}
      <div className="flex flex-wrap gap-1">
        {pages.map((p) => (
          <Button
            key={p}
            size="sm"
            variant={page === p ? "default" : "outline"}
            onClick={() => setPage(p)}
          >
            Page {p}
          </Button>
        ))}
      </div>
      {/* Grid */}
      <div className="grid grid-cols-3 gap-2 sm:grid-cols-6">
        {visible.map((tile) => {
          const TileIcon = tile.displayIcon ? TILE_ICONS[tile.displayIcon] : undefined;
          const outOfStock = false; // placeholder for live inventory
          return (
            <button
              key={tile.id}
              type="button"
              disabled={disabled}
              onClick={() => handleTileClick(tile)}
              className={cn(
                "flex h-20 flex-col items-start justify-between gap-1 rounded-lg border p-2 text-left transition active:scale-95",
                outOfStock && "opacity-50",
              )}
              style={
                tile.displayColor
                  ? { borderColor: tile.displayColor, borderLeftWidth: 4, borderLeftColor: tile.displayColor }
                  : undefined
              }
            >
              {TileIcon && <TileIcon className="h-4 w-4 text-muted-foreground" />}
              <span className="line-clamp-2 text-xs font-medium leading-tight">{tile.itemName}</span>
              <span className="text-[10px] text-muted-foreground">
                {tile.defaultPrice != null ? formatPosCurrency(tile.defaultPrice) : tile.itemCode}
              </span>
              {tile.hotkey && (
                <span className="absolute right-1 top-1 rounded bg-foreground px-1 text-[9px] font-bold text-background">
                  {tile.hotkey}
                </span>
              )}
              {outOfStock && (
                <span className="rounded bg-destructive/10 px-1 text-[9px] text-destructive">Out of stock</span>
              )}
            </button>
          );
        })}
      </div>

      <Dialog open={folderOpen !== null} onOpenChange={(o) => !o && setFolderOpen(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{folderOpen?.itemName ?? "Folder"}</DialogTitle>
          </DialogHeader>
          <div className="grid grid-cols-2 gap-2">
            {folderChildren.map((child) => (
              <Button
                key={child.id}
                variant="outline"
                onClick={() => {
                  onSelect(child);
                  setFolderOpen(null);
                }}
              >
                {child.itemName}
              </Button>
            ))}
            {folderChildren.length === 0 && (
              <p className="text-sm text-muted-foreground">No items in this folder.</p>
            )}
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}
