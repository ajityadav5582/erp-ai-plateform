"use client";

import * as React from "react";
import {
  ArrowDown,
  ArrowUp,
  LayoutGrid,
  Loader2,
  Pencil,
  Plus,
  RefreshCw,
  Trash2,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { TableSkeletonRows } from "@/components/common/table-state";
import { toast } from "sonner";
import {
  useGetPosQuickItemsQuery,
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
import { TILE_ICONS } from "@/components/pos/terminal/quick-item-grid";

const COLOR_PRESETS = ["#3b82f6", "#10b981", "#f59e0b", "#ef4444", "#8b5cf6", "#ec4899"];
const ICON_OPTIONS = Object.keys(TILE_ICONS);
const SHARED_SCOPE = "shared";
const NO_ICON = "none";

interface QuickItemForm {
  itemId: string;
  registerId: string;
  defaultPrice: string;
  gridRow: string;
  gridCol: string;
  displayColor: string;
  displayIcon: string;
  sortOrder: string;
}

const EMPTY_FORM: QuickItemForm = {
  itemId: "",
  registerId: "",
  defaultPrice: "",
  gridRow: "0",
  gridCol: "0",
  displayColor: "",
  displayIcon: "",
  sortOrder: "0",
};

/**
 * Quick-item grid administration — mobile first.
 *
 * Tiles are terminal configuration: point one at a catalogue item, give it a
 * grid position, colour and icon, and it appears on the terminal. A tile is
 * either company-shared (every register) or pinned to one register. The item
 * snapshot is resolved server-side, so only the item id is entered here.
 */
export default function PosQuickItemsPage() {
  const [page, setPage] = React.useState(0);
  const [editing, setEditing] = React.useState<PosQuickItem | null>(null);
  const [isCreateOpen, setIsCreateOpen] = React.useState(false);
  const [form, setForm] = React.useState<QuickItemForm>(EMPTY_FORM);

  const { data, isLoading, isFetching, refetch } = useGetPosQuickItemsQuery({ page, size: 20 });
  const [createItem, { isLoading: isCreating }] = useCreatePosQuickItemMutation();
  const [updateItem, { isLoading: isUpdating }] = useUpdatePosQuickItemMutation();
  const [deleteItem] = useDeletePosQuickItemMutation();
  const [activateItem] = useActivatePosQuickItemMutation();
  const [deactivateItem] = useDeactivatePosQuickItemMutation();
  const [reorderItems] = useReorderPosQuickItemsMutation();
  const { data: itemsPage, isLoading: itemsLoading } = useGetActiveItemsQuery(
    { size: 100 },
    { skip: !isCreateOpen }
  );
  const { data: registers } = useGetPosRegistersQuery({ size: 100 });

  // Catalogue options for the item picker. When editing, the tile's current item
  // is always included even if it falls outside the first page of active items,
  // so the select shows its name rather than an empty placeholder.
  const itemOptions: { id: number; sku: string; nameEn: string }[] = React.useMemo(() => {
    const list = (itemsPage?.content ?? []).map((it) => ({
      id: it.id,
      sku: it.sku,
      nameEn: it.nameEn,
    }));
    if (editing && !list.some((it) => it.id === editing.itemId)) {
      return [{ id: editing.itemId, sku: editing.itemCode, nameEn: editing.itemName }, ...list];
    }
    return list;
  }, [itemsPage, editing]);

  const registerLabel = React.useCallback(
    (id: number | null) => {
      if (id == null) return "Company";
      const reg = registers?.content.find((r) => r.id === id);
      return reg ? reg.registerName : `Register ${id}`;
    },
    [registers]
  );

  const openCreate = () => {
    setEditing(null);
    setForm(EMPTY_FORM);
    setIsCreateOpen(true);
  };

  const openEdit = (item: PosQuickItem) => {
    setEditing(item);
    setForm({
      itemId: String(item.itemId),
      registerId: item.registerId != null ? String(item.registerId) : "",
      defaultPrice: item.defaultPrice != null ? String(item.defaultPrice) : "",
      gridRow: String(item.gridRow),
      gridCol: String(item.gridCol),
      displayColor: item.displayColor ?? "",
      displayIcon: item.displayIcon ?? "",
      sortOrder: String(item.sortOrder),
    });
    setIsCreateOpen(true);
  };

  const onSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    const payload = {
      itemId: Number(form.itemId),
      defaultPrice: form.defaultPrice.trim() ? Number(form.defaultPrice) : null,
      gridRow: Number(form.gridRow) || 0,
      gridCol: Number(form.gridCol) || 0,
      displayColor: form.displayColor.trim() || null,
      displayIcon: form.displayIcon || null,
      sortOrder: Number(form.sortOrder) || 0,
    };
    try {
      if (editing) {
        await updateItem({ id: editing.id, body: payload }).unwrap();
        toast.success("Quick item updated.");
      } else {
        await createItem({
          ...payload,
          registerId: form.registerId ? Number(form.registerId) : null,
        }).unwrap();
        toast.success("Quick item created.");
      }
      setIsCreateOpen(false);
    } catch (error) {
      toast.error(getApiErrorMessage(error) || "Could not save the quick item.");
    }
  };

  const onDelete = async (item: PosQuickItem) => {
    try {
      await deleteItem(item.id).unwrap();
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

  // Swap a tile's sort order with its neighbour. The list is sorted by
  // sortOrder, so this is a one-step move up or down the grid.
  const moveItem = async (index: number, direction: -1 | 1) => {
    const target = index + direction;
    if (target < 0 || target >= items.length) return;
    const a = items[index];
    const b = items[target];
    if (!a || !b) return;
    try {
      await reorderItems({
        placements: [
          { id: a.id, gridRow: a.gridRow, gridCol: a.gridCol, sortOrder: b.sortOrder },
          { id: b.id, gridRow: b.gridRow, gridCol: b.gridCol, sortOrder: a.sortOrder },
        ],
      }).unwrap();
    } catch (error) {
      toast.error(getApiErrorMessage(error) || "Could not reorder the tiles.");
    }
  };

  const items = data?.content ?? [];

  const reorderButtons = (index: number) => (
    <div className="flex items-center gap-1">
      <Button
        variant="ghost"
        size="icon"
        className="h-8 w-8"
        disabled={index === 0}
        onClick={() => void moveItem(index, -1)}
        aria-label="Move up"
      >
        <ArrowUp className="h-4 w-4" />
      </Button>
      <Button
        variant="ghost"
        size="icon"
        className="h-8 w-8"
        disabled={index === items.length - 1}
        onClick={() => void moveItem(index, 1)}
        aria-label="Move down"
      >
        <ArrowDown className="h-4 w-4" />
      </Button>
    </div>
  );

  return (
    <div className="space-y-4 p-4">
      <header className="flex flex-wrap items-center justify-between gap-2">
        <div>
          <h1 className="text-xl font-semibold">POS Quick Items</h1>
          <p className="text-sm text-muted-foreground">
            The terminal&apos;s quick-access grid. Company-shared tiles appear on every register.
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" size="icon" onClick={() => void refetch()} disabled={isFetching}>
            <RefreshCw className={isFetching ? "h-4 w-4 animate-spin" : "h-4 w-4"} />
          </Button>
          <Button onClick={openCreate}>
            <Plus className="mr-1 h-4 w-4" />
            New tile
          </Button>
        </div>
      </header>

      {isLoading ? (
        <TableSkeletonRows columns={5} rows={5} />
      ) : items.length === 0 ? (
        <div className="flex flex-col items-center gap-2 rounded-lg border border-dashed py-10">
          <LayoutGrid className="h-8 w-8 text-muted-foreground" />
          <p className="text-sm text-muted-foreground">No quick items yet.</p>
        </div>
      ) : (
        <>
          {/* Mobile: cards */}
          <ul className="space-y-2 md:hidden">
            {items.map((item, index) => (
              <li key={item.id} className="rounded-lg border p-3">
                <div className="flex items-start justify-between gap-2">
                  <div>
                    <p className="font-medium">{item.itemName}</p>
                    <p className="text-xs text-muted-foreground">
                      {item.itemCode} · row {item.gridRow}, col {item.gridCol} · {registerLabel(item.registerId)}
                    </p>
                  </div>
                  <span
                    className={
                      item.isActive
                        ? "rounded-full bg-emerald-100 px-2 py-0.5 text-xs text-emerald-700"
                        : "rounded-full bg-slate-100 px-2 py-0.5 text-xs text-slate-600"
                    }
                  >
                    {item.isActive ? "Active" : "Hidden"}
                  </span>
                </div>
                <div className="mt-2 flex flex-wrap items-center gap-2">
                  <Button size="sm" variant="outline" onClick={() => openEdit(item)}>
                    <Pencil className="mr-1 h-4 w-4" /> Edit
                  </Button>
                  <Button size="sm" variant="outline" onClick={() => void onToggleActive(item)}>
                    {item.isActive ? "Hide" : "Show"}
                  </Button>
                  <Button size="sm" variant="outline" onClick={() => void onDelete(item)}>
                    <Trash2 className="h-4 w-4" />
                  </Button>
                  {reorderButtons(index)}
                </div>
              </li>
            ))}
          </ul>

          {/* Tablet and up: table */}
          <div className="hidden rounded-lg border md:block">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Item</TableHead>
                  <TableHead>Code</TableHead>
                  <TableHead>Position</TableHead>
                  <TableHead>Scope</TableHead>
                  <TableHead>State</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {items.map((item, index) => (
                  <TableRow key={item.id}>
                    <TableCell className="font-medium">{item.itemName}</TableCell>
                    <TableCell className="font-mono text-xs">{item.itemCode}</TableCell>
                    <TableCell>
                      r{item.gridRow} · c{item.gridCol}
                    </TableCell>
                    <TableCell>{registerLabel(item.registerId)}</TableCell>
                    <TableCell>{item.isActive ? "Active" : "Hidden"}</TableCell>
                    <TableCell className="text-right">
                      <div className="flex items-center justify-end gap-1">
                        {reorderButtons(index)}
                        <Button variant="ghost" size="sm" onClick={() => openEdit(item)}>
                          Edit
                        </Button>
                        <Button variant="ghost" size="sm" onClick={() => void onToggleActive(item)}>
                          {item.isActive ? "Hide" : "Show"}
                        </Button>
                        <Button variant="ghost" size="sm" onClick={() => void onDelete(item)}>
                          Delete
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        </>
      )}

      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between">
          <Button variant="outline" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
            Previous
          </Button>
          <span className="text-sm text-muted-foreground">
            Page {data.number + 1} of {data.totalPages}
          </span>
          <Button variant="outline" disabled={data.last} onClick={() => setPage((p) => p + 1)}>
            Next
          </Button>
        </div>
      )}

      <Dialog open={isCreateOpen} onOpenChange={setIsCreateOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{editing ? "Edit quick item" : "New quick item"}</DialogTitle>
          </DialogHeader>
          <form onSubmit={onSubmit} className="space-y-3">
            <div className="space-y-1">
              <Label htmlFor="item-id">Item</Label>
              <Select
                value={form.itemId}
                onValueChange={(value) => setForm({ ...form, itemId: value })}
              >
                <SelectTrigger id="item-id">
                  <SelectValue placeholder={itemsLoading ? "Loading…" : "Select item"} />
                </SelectTrigger>
                <SelectContent>
                  {itemsLoading ? (
                    <div className="flex items-center gap-2 px-2 py-1.5 text-muted-foreground">
                      <Loader2 className="h-3 w-3 animate-spin" /> Loading…
                    </div>
                  ) : itemOptions.length === 0 ? (
                    <div className="px-2 py-1.5 text-muted-foreground">No items yet</div>
                  ) : (
                    itemOptions.map((it) => (
                      <SelectItem key={it.id} value={String(it.id)}>
                        {it.sku} — {it.nameEn}
                      </SelectItem>
                    ))
                  )}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-1">
              <Label htmlFor="register">Scope</Label>
              <Select
                value={form.registerId || SHARED_SCOPE}
                onValueChange={(value) =>
                  setForm({ ...form, registerId: value === SHARED_SCOPE ? "" : value })
                }
                disabled={editing !== null}
              >
                <SelectTrigger id="register">
                  <SelectValue placeholder="Company-shared (all registers)" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={SHARED_SCOPE}>Company-shared (all registers)</SelectItem>
                  {(registers?.content ?? []).map((register) => (
                    <SelectItem key={register.id} value={String(register.id)}>
                      {register.registerName} ({register.registerCode})
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {editing !== null && (
                <p className="text-xs text-muted-foreground">
                  Scope is fixed at creation — delete and recreate to move a tile.
                </p>
              )}
            </div>

            <div className="space-y-1">
              <Label htmlFor="default-price">Price override (optional)</Label>
              <Input
                id="default-price"
                type="number"
                min={0}
                step="0.01"
                value={form.defaultPrice}
                onChange={(e) => setForm({ ...form, defaultPrice: e.target.value })}
                placeholder="Leave blank to use the catalogue price"
              />
            </div>
            <div className="grid grid-cols-3 gap-2">
              <div className="space-y-1">
                <Label htmlFor="grid-row">Row</Label>
                <Input
                  id="grid-row"
                  type="number"
                  min={0}
                  value={form.gridRow}
                  onChange={(e) => setForm({ ...form, gridRow: e.target.value })}
                />
              </div>
              <div className="space-y-1">
                <Label htmlFor="grid-col">Column</Label>
                <Input
                  id="grid-col"
                  type="number"
                  min={0}
                  value={form.gridCol}
                  onChange={(e) => setForm({ ...form, gridCol: e.target.value })}
                />
              </div>
              <div className="space-y-1">
                <Label htmlFor="sort-order">Sort</Label>
                <Input
                  id="sort-order"
                  type="number"
                  min={0}
                  value={form.sortOrder}
                  onChange={(e) => setForm({ ...form, sortOrder: e.target.value })}
                />
              </div>
            </div>
            <div className="space-y-1">
              <Label htmlFor="display-color">Tile colour</Label>
              <div className="flex items-center gap-2">
                <Input
                  id="display-color"
                  type="color"
                  value={form.displayColor || "#3b82f6"}
                  onChange={(e) => setForm({ ...form, displayColor: e.target.value })}
                  className="h-9 w-14 p-1"
                  aria-label="Tile colour"
                />
                <div className="flex flex-wrap items-center gap-1">
                  {COLOR_PRESETS.map((color) => (
                    <button
                      key={color}
                      type="button"
                      onClick={() => setForm({ ...form, displayColor: color })}
                      className="h-6 w-6 rounded-full border"
                      style={{ backgroundColor: color }}
                      aria-label={`Use colour ${color}`}
                    />
                  ))}
                  {form.displayColor && (
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      onClick={() => setForm({ ...form, displayColor: "" })}
                    >
                      Clear
                    </Button>
                  )}
                </div>
              </div>
            </div>
            <div className="space-y-1">
              <Label htmlFor="display-icon">Icon</Label>
              <Select
                value={form.displayIcon || NO_ICON}
                onValueChange={(value) =>
                  setForm({ ...form, displayIcon: value === NO_ICON ? "" : value })
                }
              >
                <SelectTrigger id="display-icon">
                  <SelectValue placeholder="No icon" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={NO_ICON}>No icon</SelectItem>
                  {ICON_OPTIONS.map((name) => (
                    <SelectItem key={name} value={name}>
                      {name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <Button type="submit" className="w-full" disabled={isCreating || isUpdating || !form.itemId}>
              {editing ? "Save changes" : "Create tile"}
            </Button>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}
