"use client";

import * as React from "react";
import {
  DndContext,
  DragOverlay,
  KeyboardSensor,
  PointerSensor,
  useSensor,
  useSensors,
  type DragEndEvent,
  type DragStartEvent,
} from "@dnd-kit/core";
import { CSS } from "@dnd-kit/utilities";
import { useDraggable, useDroppable } from "@dnd-kit/core";
import { GripVertical, Plus } from "lucide-react";
import { cn } from "@/lib/utils";
import { formatPosCurrency } from "@/lib/pos-format";
import { TILE_ICONS } from "@/components/pos/terminal/quick-item-grid";
import type { PosQuickItem } from "@/services/pos-quick-item.service";
import type { QuickItemFolder } from "@/types/pos-quick-items";

export const QUICK_GRID_ROWS = 4;
export const QUICK_GRID_COLS = 6;

export interface QuickItemsCanvasProps {
  items: PosQuickItem[];
  folders?: QuickItemFolder[];
  /** 0-based page offset applied to rows (page * QUICK_GRID_ROWS). */
  pageOffset?: number;
  onSlotClick: (row: number, col: number) => void;
  onTileEdit?: (item: PosQuickItem) => void;
  onFolderClick?: (folder: QuickItemFolder) => void;
  onReorder: (
    placements: { id: number; gridRow: number; gridCol: number; sortOrder: number }[],
  ) => Promise<void>;
  isSaving?: boolean;
}

interface SlotData {
  row: number;
  col: number;
}

function TileFace({ item, dragging }: { item: PosQuickItem; dragging?: boolean }) {
  const TileIcon = item.displayIcon ? TILE_ICONS[item.displayIcon] : undefined;
  const bg = item.displayColor || "#3b82f6";
  return (
    <div
      className={cn(
        "flex h-full w-full flex-col items-start justify-between gap-1 rounded-lg p-2 text-left text-white shadow-sm transition",
        dragging && "opacity-60",
      )}
      style={{ backgroundColor: bg }}
    >
      <div className="flex w-full items-start justify-between gap-1">
        {TileIcon ? <TileIcon className="h-4 w-4 shrink-0" /> : <GripVertical className="h-4 w-4 shrink-0 opacity-60" />}
        <span className="line-clamp-2 text-xs font-semibold leading-tight">{item.itemName}</span>
      </div>
      <div className="w-full space-y-0.5">
        <p className="truncate text-[10px] opacity-80">{item.itemCode}</p>
        <p className="text-[11px] font-bold">
          {item.defaultPrice != null ? formatPosCurrency(item.defaultPrice) : "—"}
        </p>
      </div>
    </div>
  );
}

function DraggableTile({
  item,
  onEdit,
}: {
  item: PosQuickItem;
  onEdit?: (item: PosQuickItem) => void;
}) {
  const { attributes, listeners, setNodeRef, transform, isDragging } = useDraggable({
    id: item.id,
    data: { item },
  });
  const style: React.CSSProperties = {
    transform: CSS.Transform.toString(transform),
    zIndex: isDragging ? 50 : undefined,
  };
  return (
    <button
      ref={setNodeRef}
      style={style}
      type="button"
      onClick={() => onEdit?.(item)}
      className={cn(
        "h-full w-full cursor-grab touch-none focus:outline-none",
        isDragging && "cursor-grabbing",
      )}
      {...listeners}
      {...attributes}
      aria-label={`Edit ${item.itemName}`}
    >
      <TileFace item={item} dragging={isDragging} />
    </button>
  );
}

function DroppableSlot({
  row,
  col,
  item,
  isOver,
  onSlotClick,
  onTileEdit,
}: {
  row: number;
  col: number;
  item: PosQuickItem | undefined;
  isOver: boolean;
  onSlotClick: (row: number, col: number) => void;
  onTileEdit?: (item: PosQuickItem) => void;
}) {
  const { setNodeRef } = useDroppable({
    id: `${row}-${col}`,
    data: { row, col } satisfies SlotData,
  });

  if (item) {
    return (
      <div
        ref={setNodeRef}
        className={cn(
          "relative aspect-square rounded-lg border-2 transition",
          isOver ? "border-primary ring-2 ring-primary/40" : "border-transparent",
        )}
      >
        <DraggableTile item={item} onEdit={onTileEdit} />
      </div>
    );
  }

  return (
    <button
      ref={setNodeRef}
      type="button"
      onClick={() => onSlotClick(row, col)}
      className={cn(
        "flex aspect-square flex-col items-center justify-center gap-1 rounded-lg border-2 border-dashed transition",
        isOver
          ? "border-primary bg-primary/10 text-primary"
          : "border-muted-foreground/30 text-muted-foreground/60 hover:border-primary/50 hover:text-primary/70",
      )}
      aria-label={`Add quick item at row ${row + 1}, column ${col + 1}`}
    >
      <Plus className="h-5 w-5" />
      <span className="text-[10px] font-medium">
        R{row + 1}·C{col + 1}
      </span>
    </button>
  );
}

export function QuickItemsCanvas({
  items,
  folders = [],
  pageOffset = 0,
  onSlotClick,
  onTileEdit,
  onFolderClick,
  onReorder,
  isSaving,
}: QuickItemsCanvasProps) {
  const [activeItem, setActiveItem] = React.useState<PosQuickItem | null>(null);
  const [overSlot, setOverSlot] = React.useState<string | null>(null);
  const [localItems, setLocalItems] = React.useState<PosQuickItem[]>(items);

  React.useEffect(() => {
    setLocalItems(items);
  }, [items]);

  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 6 } }),
    useSensor(KeyboardSensor),
  );

  const itemBySlot = React.useMemo(() => {
    const map = new Map<string, PosQuickItem>();
    for (const it of localItems) {
      const r = it.gridRow - pageOffset;
      if (r < 0 || r >= QUICK_GRID_ROWS) continue;
      if (it.gridCol < 0 || it.gridCol >= QUICK_GRID_COLS) continue;
      const key = `${r}-${it.gridCol}`;
      if (!map.has(key)) map.set(key, it);
    }
    return map;
  }, [localItems, pageOffset]);

  const folderBySlot = React.useMemo(() => {
    const map = new Map<string, QuickItemFolder>();
    for (const f of folders) {
      const r = f.gridRow - pageOffset;
      if (r < 0 || r >= QUICK_GRID_ROWS) continue;
      if (f.gridCol < 0 || f.gridCol >= QUICK_GRID_COLS) continue;
      const key = `${r}-${f.gridCol}`;
      if (!map.has(key)) map.set(key, f);
    }
    return map;
  }, [folders, pageOffset]);

  const handleDragStart = (event: DragStartEvent) => {
    const item = localItems.find((i) => i.id === event.active.id);
    setActiveItem(item ?? null);
  };

  const handleDragOver = (event: { over: { id: string | number } | null }) => {
    setOverSlot(event.over ? String(event.over.id) : null);
  };

  const handleDragEnd = async (event: DragEndEvent) => {
    const { active, over } = event;
    setActiveItem(null);
    setOverSlot(null);
    if (!over) return;

    const source = localItems.find((i) => i.id === active.id);
    if (!source) return;

    const targetRow = Number((over.data.current as SlotData | undefined)?.row);
    const targetCol = Number((over.data.current as SlotData | undefined)?.col);
    if (Number.isNaN(targetRow) || Number.isNaN(targetCol)) return;

    const actualTargetRow = targetRow + pageOffset;
    if (source.gridRow === actualTargetRow && source.gridCol === targetCol) return;

    const target = localItems.find(
      (i) => i.gridRow === actualTargetRow && i.gridCol === targetCol,
    );

    const placements: { id: number; gridRow: number; gridCol: number; sortOrder: number }[] = [];
    if (target) {
      placements.push({
        id: source.id,
        gridRow: actualTargetRow,
        gridCol: targetCol,
        sortOrder: actualTargetRow * QUICK_GRID_COLS + targetCol,
      });
      placements.push({
        id: target.id,
        gridRow: source.gridRow,
        gridCol: source.gridCol,
        sortOrder: source.gridRow * QUICK_GRID_COLS + source.gridCol,
      });
    } else {
      placements.push({
        id: source.id,
        gridRow: actualTargetRow,
        gridCol: targetCol,
        sortOrder: actualTargetRow * QUICK_GRID_COLS + targetCol,
      });
    }

    // Optimistic update
    setLocalItems((prev) =>
      prev.map((it) => {
        const p = placements.find((pl) => pl.id === it.id);
        if (!p) return it;
        return { ...it, gridRow: p.gridRow, gridCol: p.gridCol, sortOrder: p.sortOrder };
      }),
    );

    try {
      await onReorder(placements);
    } catch {
      setLocalItems(items);
    }
  };

  return (
    <DndContext
      sensors={sensors}
      onDragStart={handleDragStart}
      onDragOver={handleDragOver}
      onDragEnd={handleDragEnd}
    >
      <div
        className={cn(
          "grid gap-2",
          "grid-cols-3 sm:grid-cols-4 md:grid-cols-6",
        )}
        style={{ gridTemplateColumns: `repeat(${QUICK_GRID_COLS}, minmax(0, 1fr))` }}
      >
        {Array.from({ length: QUICK_GRID_ROWS }).map((_, r) =>
          Array.from({ length: QUICK_GRID_COLS }).map((_, c) => {
            const item = itemBySlot.get(`${r}-${c}`);
            const folder = folderBySlot.get(`${r}-${c}`);
            const slotKey = `${r}-${c}`;
            if (folder) {
              return (
                <div
                  key={slotKey}
                  className={cn(
                    "flex aspect-square flex-col items-center justify-center gap-1 rounded-lg border-2 border-dashed border-primary/40 bg-primary/5 p-2 text-center transition",
                    overSlot === slotKey && "ring-2 ring-primary/40",
                  )}
                >
                  <button
                    type="button"
                    onClick={() => onFolderClick?.(folder)}
                    className="flex h-full w-full flex-col items-center justify-center gap-1"
                  >
                    <span className="text-xs font-semibold">{folder.name}</span>
                    <span className="text-[10px] text-muted-foreground">
                      {folder.children.length} items
                    </span>
                  </button>
                </div>
              );
            }
            return (
              <DroppableSlot
                key={slotKey}
                row={r}
                col={c}
                item={item}
                isOver={overSlot === slotKey}
                onSlotClick={(rr, cc) => onSlotClick(rr + pageOffset, cc)}
                onTileEdit={onTileEdit}
              />
            );
          }),
        )}
      </div>

      {isSaving && (
        <p className="mt-2 text-xs text-muted-foreground">Saving grid changes…</p>
      )}

      <DragOverlay>
        {activeItem ? (
          <div className="h-24 w-24">
            <TileFace item={activeItem} />
          </div>
        ) : null}
      </DragOverlay>
    </DndContext>
  );
}
