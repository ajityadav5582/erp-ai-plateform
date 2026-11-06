"use client";

import * as React from "react";
import {
  LayoutGrid,
  Loader2,
  Pencil,
  Plus,
  RefreshCw,
  Trash2,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { toast } from "sonner";
import {
  useGetPosQuickItemsQuery,
  useGetPosQuickItemGridQuery,
  useCreatePosQuickItemMutation,
  useUpdatePosQuickItemMutation,
  useDeletePosQuickItemMutation,
  useActivatePosQuickItemMutation,
  useDeactivatePosQuickItemMutation,
  useReorderPosQuickItemsMutation,
  type PosQuickItem,
} from "@/services/pos-quick-item.service";
import { useGetActiveItemsQuery } from "@/services/item.service";
import { useGetPosRegistersQuery } from "@/services/pos-register.service";
import { getApiErrorMessage } from "@/services/error-handler";
import {
  QuickItemsCanvas,
  QUICK_GRID_ROWS,
} from "@/components/pos/quick-items/quick-items-canvas";
import {
  QuickItemFormDialog,
  type QuickItemFormValues,
} from "@/components/pos/quick-items/quick-item-form-dialog";
import {
  QuickItemsTabs,
  type QuickTab,
} from "@/components/pos/quick-items/quick-items-tabs";
import { BatchAddDrawer } from "@/components/pos/quick-items/batch-add-drawer";
import type { QuickItemFolder } from "@/types/pos-quick-items";

const SHARED_SCOPE = "shared";
const PAGE_COUNT = 4;

export default function PosQuickItemsPage() {
  const [editing, setEditing] = React.useState<PosQuickItem | null>(null);
  const [isCreateOpen, setIsCreateOpen] = React.useState(false);
  const [position, setPosition] = React.useState<{ row: number; col: number } | null>(null);
  const [isBatchOpen, setIsBatchOpen] = React.useState(false);

  const [selectedRegisterId, setSelectedRegisterId] = React.useState<number | null>(null);
  const [page, setPage] = React.useState(0);

  // Tabbed grid state (client-side organization layer).
  const [tabs, setTabs] = React.useState<QuickTab[]>([{ id: "default", name: "All Items" }]);
  const [activeTabId, setActiveTabId] = React.useState("default");
  const [tabItems, setTabItems] = React.useState<Record<string, PosQuickItem[]>>({ default: [] });
  const [foldersByTab, setFoldersByTab] = React.useState<Record<string, QuickItemFolder[]>>({});

  const { data: allItemsPage, isLoading: allLoading, isFetching, refetch } = useGetPosQuickItemsQuery({
    page: 0,
    size: 100,
  });
  const { data: gridItems, isLoading: gridLoading } = useGetPosQuickItemGridQuery(
    selectedRegisterId ?? 0,
    { skip: selectedRegisterId === null },
  );

  const [createItem, { isLoading: isCreating }] = useCreatePosQuickItemMutation();
  const [updateItem, { isLoading: isUpdating }] = useUpdatePosQuickItemMutation();
  const [deleteItem] = useDeletePosQuickItemMutation();
  const [activateItem] = useActivatePosQuickItemMutation();
  const [deactivateItem] = useDeactivatePosQuickItemMutation();
  const [reorderItems, { isLoading: isReordering }] = useReorderPosQuickItemsMutation();

  const { data: registers } = useGetPosRegistersQuery({ size: 100 });
  const { data: activeCatalog } = useGetActiveItemsQuery({ size: 100 });

  const allItems = allItemsPage?.content ?? [];
  const serverItems: PosQuickItem[] =
    selectedRegisterId === null
      ? allItems.filter((it) => it.registerId == null)
      : (gridItems ?? []);

  // Sync server items into the active tab on first load.
  React.useEffect(() => {
    setTabItems((prev) => {
      if ((prev[activeTabId] ?? []).length === 0 && serverItems.length > 0) {
        return { ...prev, [activeTabId]: serverItems };
      }
      return prev;
    });
  }, [serverItems, activeTabId]);

  const items = tabItems[activeTabId] ?? [];
  const isLoading = selectedRegisterId === null ? allLoading : gridLoading;

  const registerLabel = React.useCallback(
    (id: number | null) => {
      if (id == null) return "Company";
      const reg = registers?.content.find((r) => r.id === id);
      return reg ? reg.registerName : `Register ${id}`;
    },
    [registers],
  );

  const openCreateAt = (row: number, col: number) => {
    setEditing(null);
    setPosition({ row, col });
    setIsCreateOpen(true);
  };

  const openEdit = (item: PosQuickItem) => {
    setEditing(item);
    setPosition({ row: item.gridRow, col: item.gridCol });
    const itemPage = Math.floor(item.gridRow / QUICK_GRID_ROWS);
    if (itemPage >= 0 && itemPage < PAGE_COUNT) setPage(itemPage);
    setIsCreateOpen(true);
  };

  const handleFormSubmit = async (values: QuickItemFormValues) => {
    const duplicate = items.some(
      (it) =>
        it.itemId === Number(values.itemId) &&
        it.registerId === (values.registerId ? Number(values.registerId) : null) &&
        (!editing || it.id !== editing.id),
    );
    if (duplicate) {
      toast.error("This product is already a quick item in this scope.");
      return;
    }

    const row = editing ? editing.gridRow : (position?.row ?? 0);
    const col = editing ? editing.gridCol : (position?.col ?? 0);

    const payload = {
      itemId: Number(values.itemId),
      defaultPrice: values.defaultPrice?.trim() ? Number(values.defaultPrice) : null,
      gridRow: row,
      gridCol: col,
      displayColor: values.displayColor?.trim() || null,
      displayIcon: values.displayIcon || null,
      sortOrder: row * 6 + col,
    };

    try {
      let saved: PosQuickItem;
      if (editing) {
        saved = await updateItem({ id: editing.id, body: payload }).unwrap();
        toast.success("Quick item updated.");
      } else {
        saved = await createItem({
          ...payload,
          registerId: values.registerId ? Number(values.registerId) : null,
        }).unwrap();
        toast.success("Quick item created.");
      }
      setTabItems((prev) => {
        const list = prev[activeTabId] ?? [];
        const next = editing
          ? list.map((it) => (it.id === saved.id ? saved : it))
          : [...list, saved];
        return { ...prev, [activeTabId]: next };
      });
      setIsCreateOpen(false);
      setPosition(null);
      setEditing(null);
    } catch (error) {
      toast.error(getApiErrorMessage(error) || "Could not save the quick item.");
    }
  };

  const handleBatchAdd = async (itemIds: number[]) => {
    const catalog = activeCatalog?.content ?? [];
    const activeItems = tabItems[activeTabId] ?? [];
    const occupied = new Set(activeItems.map((it) => `${it.gridRow}-${it.gridCol}`));
    const slots: { row: number; col: number }[] = [];
    for (let r = 0; r < 4; r++) {
      for (let c = 0; c < 6; c++) {
        if (!occupied.has(`${r}-${c}`)) slots.push({ row: r, col: c });
      }
    }

    const created: PosQuickItem[] = [];
    for (let i = 0; i < itemIds.length; i++) {
      const slot = slots[i];
      if (!slot) break;
      const catItem = catalog.find((ci) => ci.id === itemIds[i]);
      if (!catItem) continue;
      try {
        const saved = await createItem({
          itemId: catItem.id,
          defaultPrice: null,
          gridRow: slot.row,
          gridCol: slot.col,
          displayColor: null,
          displayIcon: null,
          sortOrder: slot.row * 6 + slot.col,
          registerId: selectedRegisterId,
        }).unwrap();
        created.push(saved);
      } catch (error) {
        toast.error(getApiErrorMessage(error) || "Could not add item.");
      }
    }
    if (created.length) {
      setTabItems((prev) => ({
        ...prev,
        [activeTabId]: [...(prev[activeTabId] ?? []), ...created],
      }));
      toast.success(`Added ${created.length} item(s).`);
    }
  };

  const onDelete = async (item: PosQuickItem) => {
    try {
      await deleteItem(item.id).unwrap();
      setTabItems((prev) => ({
        ...prev,
        [activeTabId]: (prev[activeTabId] ?? []).filter((it) => it.id !== item.id),
      }));
      toast.success(`${item.itemName} removed from the grid.`);
    } catch (error) {
      toast.error(getApiErrorMessage(error) || "Could not delete the quick item.");
    }
  };

  const onToggleActive = async (item: PosQuickItem) => {
    try {
      if (item.isActive) {
        await deactivateItem(item.id).unwrap();
      } else {
        await activateItem(item.id).unwrap();
      }
    } catch (error) {
      toast.error(getApiErrorMessage(error) || "Could not change the tile state.");
    }
  };

  const onReorder = async (
    placements: { id: number; gridRow: number; gridCol: number; sortOrder: number }[],
  ) => {
    try {
      await reorderItems({ placements }).unwrap();
      setTabItems((prev) => {
        const list = (prev[activeTabId] ?? []).map((it) => {
          const p = placements.find((pl) => pl.id === it.id);
          if (!p) return it;
          return { ...it, gridRow: p.gridRow, gridCol: p.gridCol, sortOrder: p.sortOrder };
        });
        return { ...prev, [activeTabId]: list };
      });
    } catch (error) {
      toast.error(getApiErrorMessage(error) || "Could not reorder the tiles.");
      throw error;
    }
  };

  const handleAddTab = (name: string) => {
    const id = `tab-${Date.now()}`;
    setTabs((prev) => [...prev, { id, name }]);
    setTabItems((prev) => ({ ...prev, [id]: [] }));
    setActiveTabId(id);
  };

  const handleRemoveTab = (id: string) => {
    setTabs((prev) => prev.filter((t) => t.id !== id));
    setTabItems((prev) => {
      const next = { ...prev };
      delete next[id];
      return next;
    });
    if (activeTabId === id) {
      setActiveTabId(tabs.find((t) => t.id !== id)?.id ?? "default");
    }
  };

  const handleRenameTab = (id: string, name: string) => {
    setTabs((prev) => prev.map((t) => (t.id === id ? { ...t, name } : t)));
  };

  const pageOffset = page * QUICK_GRID_ROWS;

  return (
    <div className="space-y-4 p-4">
      <header className="flex flex-wrap items-center justify-between gap-2">
        <div>
          <h1 className="text-xl font-semibold">POS Quick Items</h1>
          <p className="text-sm text-muted-foreground">
            Tabbed terminal grids. Click an empty slot to add a tile; drag tiles to reorder.
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" size="icon" onClick={() => void refetch()} disabled={isFetching}>
            <RefreshCw className={isFetching ? "h-4 w-4 animate-spin" : "h-4 w-4"} />
          </Button>
          <Button variant="outline" onClick={() => setIsBatchOpen(true)}>
            <Plus className="mr-1 h-4 w-4" />
            Batch add
          </Button>
          <Button
            variant="outline"
            onClick={() => {
              const name = window.prompt("Folder name");
              if (!name) return;
              const folders = foldersByTab[activeTabId] ?? [];
              const occupied = new Set([
                ...items.map((it) => `${it.gridRow}-${it.gridCol}`),
                ...folders.map((f) => `${f.gridRow}-${f.gridCol}`),
              ]);
              let slot: { row: number; col: number } | null = null;
              for (let r = 0; r < 4 && !slot; r++) {
                for (let c = 0; c < 6; c++) {
                  if (!occupied.has(`${r}-${c}`)) {
                    slot = { row: r, col: c };
                    break;
                  }
                }
              }
              if (!slot) {
                toast.error("No empty slot for folder.");
                return;
              }
              const folder: QuickItemFolder = {
                id: `folder-${Date.now()}`,
                name,
                displayColor: "#8b5cf6",
                displayIcon: "folder",
                gridRow: slot.row,
                gridCol: slot.col,
                sortOrder: slot.row * 6 + slot.col,
                children: [],
              };
              setFoldersByTab((prev) => ({
                ...prev,
                [activeTabId]: [...(prev[activeTabId] ?? []), folder],
              }));
            }}
          >
            <Plus className="mr-1 h-4 w-4" />
            New folder
          </Button>
          <Button onClick={() => openCreateAt(pageOffset, 0)}>
            <Plus className="mr-1 h-4 w-4" />
            Add quick item
          </Button>
        </div>
      </header>

      <QuickItemsTabs
        tabs={tabs}
        activeTabId={activeTabId}
        onSelect={setActiveTabId}
        onAdd={handleAddTab}
        onRemove={handleRemoveTab}
        onRename={handleRenameTab}
      />

      <div className="flex flex-wrap items-center gap-3 rounded-lg border bg-card p-3">
        <div className="flex items-center gap-2">
          <label htmlFor="register-select" className="text-xs font-medium">
            Register
          </label>
          <select
            id="register-select"
            className="h-9 rounded-md border bg-background px-2 text-sm"
            value={selectedRegisterId == null ? SHARED_SCOPE : String(selectedRegisterId)}
            onChange={(e) => {
              const v = e.target.value;
              setSelectedRegisterId(v === SHARED_SCOPE ? null : Number(v));
              setPage(0);
            }}
          >
            <option value={SHARED_SCOPE}>Company-Wide Default</option>
            {(registers?.content ?? []).map((register) => (
              <option key={register.id} value={String(register.id)}>
                {register.registerName} ({register.registerCode})
              </option>
            ))}
          </select>
        </div>

        <div className="flex items-center gap-1">
          <span className="text-xs font-medium text-muted-foreground">Page</span>
          {Array.from({ length: PAGE_COUNT }).map((_, i) => (
            <Button
              key={i}
              size="sm"
              variant={page === i ? "default" : "outline"}
              onClick={() => setPage(i)}
            >
              {i + 1}
            </Button>
          ))}
        </div>
      </div>

      {isLoading ? (
        <div className="flex items-center justify-center py-10">
          <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
        </div>
      ) : (
        <QuickItemsCanvas
          items={items}
          folders={foldersByTab[activeTabId] ?? []}
          pageOffset={pageOffset}
          onSlotClick={openCreateAt}
          onTileEdit={openEdit}
          onFolderClick={(folder) => toast.info(`Folder "${folder.name}" contains ${folder.children.length} items.`)}
          onReorder={onReorder}
          isSaving={isReordering}
        />
      )}

      {!isLoading && items.length === 0 && (
        <div className="flex flex-col items-center gap-2 rounded-lg border border-dashed py-6">
          <LayoutGrid className="h-8 w-8 text-muted-foreground" />
          <p className="text-sm text-muted-foreground">No quick items yet. Click a slot to add one.</p>
        </div>
      )}

      {items.length > 0 && (
        <div className="rounded-lg border">
          <div className="border-b px-3 py-2 text-sm font-medium">Tiles on this view</div>
          <ul className="divide-y">
            {items
              .filter((it) => Math.floor(it.gridRow / QUICK_GRID_ROWS) === page)
              .map((item) => (
                <li key={item.id} className="flex items-center justify-between gap-2 px-3 py-2">
                  <div className="min-w-0">
                    <p className="truncate text-sm font-medium">{item.itemName}</p>
                    <p className="text-xs text-muted-foreground">
                      {item.itemCode} · r{item.gridRow + 1} c{item.gridCol + 1} · {registerLabel(item.registerId)}
                    </p>
                  </div>
                  <div className="flex items-center gap-1">
                    <Button size="sm" variant="ghost" onClick={() => openEdit(item)}>
                      <Pencil className="h-4 w-4" />
                    </Button>
                    <Button size="sm" variant="ghost" onClick={() => void onToggleActive(item)}>
                      {item.isActive ? "Hide" : "Show"}
                    </Button>
                    <Button size="sm" variant="ghost" onClick={() => void onDelete(item)}>
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>
                </li>
              ))}
          </ul>
        </div>
      )}

      <QuickItemFormDialog
        open={isCreateOpen}
        onOpenChange={(o) => {
          setIsCreateOpen(o);
          if (!o) {
            setEditing(null);
            setPosition(null);
          }
        }}
        editing={editing}
        position={position}
        registers={registers?.content}
        selectedRegisterId={selectedRegisterId}
        isSubmitting={isCreating || isUpdating}
        onSubmit={handleFormSubmit}
      />

      <BatchAddDrawer
        open={isBatchOpen}
        onOpenChange={setIsBatchOpen}
        onAdd={handleBatchAdd}
        isSubmitting={isCreating}
      />
    </div>
  );
}
