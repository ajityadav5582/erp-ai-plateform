"use client";

import * as React from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { icons, type LucideIcon } from "lucide-react";
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
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { cn } from "@/lib/utils";
import { formatPosCurrency } from "@/lib/pos-format";
import { TILE_ICONS } from "@/components/pos/terminal/quick-item-grid";
import { useGetItemsQuery, useGetActiveItemsQuery, type ItemListResponse } from "@/services/item.service";
import type { PosQuickItem } from "@/services/pos-quick-item.service";
import type { PosRegister } from "@/services/pos-register.service";

const COLOR_PALETTE = [
  // Vibrant
  "#3b82f6", "#10b981", "#f59e0b", "#ef4444", "#8b5cf6", "#ec4899", "#06b6d4", "#84cc16",
  // Pastel
  "#bfdbfe", "#bbf7d0", "#fde68a", "#fecaca", "#ddd6fe", "#fbcfe8", "#a5f3fc", "#d9f99d",
];

const HOTKEYS = ["1", "2", "3", "4", "5", "6", "7", "8", "9"];

const UNITS = ["Piece", "KG", "Gram", "Box", "Pack", "Bottle", "Cup"];

const formSchema = z.object({
  itemId: z.string().min(1, "Select an item"),
  registerId: z.string().optional(),
  defaultPrice: z.string().optional(),
  displayColor: z.string().optional(),
  displayIcon: z.string().optional(),
  imageMode: z.enum(["color", "image"]).default("color"),
  imageData: z.string().optional(),
  unitMode: z.enum(["default", "prompt"]).default("default"),
  unit: z.string().optional(),
  hotkey: z.string().optional(),
});

export type QuickItemFormValues = z.infer<typeof formSchema>;

interface QuickItemFormDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  editing: PosQuickItem | null;
  position: { row: number; col: number } | null;
  registers: PosRegister[] | undefined;
  selectedRegisterId: number | null;
  isSubmitting: boolean;
  onSubmit: (values: QuickItemFormValues) => Promise<void>;
}

function useDebounced(value: string, delay = 300) {
  const [debounced, setDebounced] = React.useState(value);
  React.useEffect(() => {
    const t = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(t);
  }, [value, delay]);
  return debounced;
}

export function QuickItemFormDialog({
  open,
  onOpenChange,
  editing,
  position,
  registers,
  selectedRegisterId,
  isSubmitting,
  onSubmit,
}: QuickItemFormDialogProps) {
  const [search, setSearch] = React.useState("");
  const [iconSearch, setIconSearch] = React.useState("");
  const [selectedItem, setSelectedItem] = React.useState<ItemListResponse | null>(null);
  const [comboboxOpen, setComboboxOpen] = React.useState(false);
  const debouncedSearch = useDebounced(search);

  const form = useForm<QuickItemFormValues>({
    resolver: zodResolver(formSchema) as never,
    defaultValues: {
      itemId: "",
      registerId: "",
      defaultPrice: "",
      displayColor: "",
      displayIcon: "",
      imageMode: "color",
      imageData: "",
      unitMode: "default",
      unit: "",
      hotkey: "",
    },
  });

  const watched = form.watch();

  // Reset form when dialog opens
  React.useEffect(() => {
    if (!open) return;
    form.reset({
      itemId: editing ? String(editing.itemId) : "",
      registerId: editing
        ? editing.registerId != null
          ? String(editing.registerId)
          : ""
        : selectedRegisterId != null
          ? String(selectedRegisterId)
          : "",
      defaultPrice: editing?.defaultPrice != null ? String(editing.defaultPrice) : "",
      displayColor: editing?.displayColor ?? "",
      displayIcon: editing?.displayIcon ?? "",
      imageMode: "color",
      imageData: "",
      unitMode: "default",
      unit: "",
      hotkey: "",
    });
    setSearch("");
    setIconSearch("");
    setSelectedItem(null);
  }, [open, editing, selectedRegisterId, form]);

  // Item queries
  const { data: searchResults } = useGetItemsQuery(
    { search: debouncedSearch, size: 20 },
    { skip: !open || !debouncedSearch.trim() },
  );
  const { data: activeItems } = useGetActiveItemsQuery(
    { size: 20 },
    { skip: !open || !!debouncedSearch.trim() },
  );

  const results = debouncedSearch.trim()
    ? (searchResults?.content ?? [])
    : (activeItems?.content ?? []);

  // Keep selected item in sync when editing
  React.useEffect(() => {
    if (editing && !selectedItem) {
      setSelectedItem({
        id: editing.itemId,
        sku: editing.itemCode,
        nameEn: editing.itemName,
        nameNp: null,
        barcode: null,
        hsCode: null,
        categoryId: null,
        categoryName: null,
        uomId: 0,
        uomCode: editing.uomCode,
        itemType: null,
        taxabilityType: null,
        vatRate: null,
        sellingPrice: editing.defaultPrice ?? 0,
        purchasePrice: null,
        mrp: null,
        isActive: true,
      });
    }
  }, [editing, selectedItem]);

  const handleImageUpload = (file: File | null) => {
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => {
      form.setValue("imageData", String(reader.result ?? ""), { shouldDirty: true });
      form.setValue("imageMode", "image");
    };
    reader.readAsDataURL(file);
  };

  const handleSubmit = async (values: QuickItemFormValues) => {
    await onSubmit(values);
  };

  const filteredIcons = React.useMemo(() => {
    const term = iconSearch.trim().toLowerCase();
    const keys = Object.keys(icons);
    if (!term) return keys.slice(0, 60);
    return keys.filter((k) => k.toLowerCase().includes(term)).slice(0, 60);
  }, [iconSearch]);

  const previewColor = watched.displayColor || "#3b82f6";
  const PreviewIcon = watched.displayIcon ? (icons[watched.displayIcon as keyof typeof icons] as LucideIcon | undefined) ?? TILE_ICONS[watched.displayIcon] : undefined;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>{editing ? "Edit quick item" : "Add quick item"}</DialogTitle>
        </DialogHeader>

        <Form {...form}>
          <form onSubmit={form.handleSubmit(handleSubmit)} className="space-y-4">
            {/* Item combobox */}
            <FormField
              control={form.control}
              name="itemId"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Item</FormLabel>
                  <Popover open={comboboxOpen} onOpenChange={setComboboxOpen}>
                    <PopoverTrigger asChild>
                      <Button
                        type="button"
                        variant="outline"
                        role="combobox"
                        className="w-full justify-between"
                      >
                        {selectedItem
                          ? `${selectedItem.sku} — ${selectedItem.nameEn}`
                          : editing
                            ? `${editing.itemCode} — ${editing.itemName}`
                            : "Search by name, SKU or barcode…"}
                        <span className="text-muted-foreground">▾</span>
                      </Button>
                    </PopoverTrigger>
                    <PopoverContent className="w-[360px] p-2" align="start">
                      <Input
                        placeholder="Type to search…"
                        value={search}
                        onChange={(e) => setSearch(e.target.value)}
                        className="mb-2"
                      />
                      <div className="max-h-60 overflow-y-auto">
                        {results.length === 0 ? (
                          <p className="px-2 py-1.5 text-sm text-muted-foreground">No matching items</p>
                        ) : (
                          results.map((it) => (
                            <button
                              key={it.id}
                              type="button"
                              className={cn(
                                "flex w-full flex-col items-start gap-0.5 rounded-md px-2 py-1.5 text-left text-sm hover:bg-muted",
                                String(it.id) === field.value && "bg-muted",
                              )}
                              onClick={() => {
                                field.onChange(String(it.id));
                                setSelectedItem(it);
                                setComboboxOpen(false);
                              }}
                            >
                              <span className="font-medium">{it.sku} — {it.nameEn}</span>
                              <span className="text-xs text-muted-foreground">
                                Price: {formatPosCurrency(it.sellingPrice)}
                                {it.vatRate != null ? ` · Tax: ${it.vatRate}%` : ""}
                                {it.barcode ? ` · Barcode: ${it.barcode}` : ""}
                              </span>
                            </button>
                          ))
                        )}
                      </div>
                    </PopoverContent>
                  </Popover>
                  <FormMessage />
                </FormItem>
              )}
            />

            {/* Scope */}
            <FormField
              control={form.control}
              name="registerId"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Scope</FormLabel>
                  <Select
                    value={field.value || "shared"}
                    onValueChange={(v) => field.onChange(v === "shared" ? "" : v)}
                    disabled={editing !== null}
                  >
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder="Company-shared (all registers)" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      <SelectItem value="shared">Company-shared (all registers)</SelectItem>
                      {(registers ?? []).map((r) => (
                        <SelectItem key={r.id} value={String(r.id)}>
                          {r.registerName} ({r.registerCode})
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )}
            />

            {/* Price override */}
            <FormField
              control={form.control}
              name="defaultPrice"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Price override (optional)</FormLabel>
                  <FormControl>
                    <Input type="number" min={0} step="0.01" placeholder="Catalogue price" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            {/* Unit / variant */}
            <div className="grid grid-cols-2 gap-3">
              <FormField
                control={form.control}
                name="unitMode"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Unit handling</FormLabel>
                    <Select value={field.value} onValueChange={field.onChange}>
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        <SelectItem value="default">Use default unit</SelectItem>
                        <SelectItem value="prompt">Prompt cashier for portion on tap</SelectItem>
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="unit"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Default unit</FormLabel>
                    <Select value={field.value || "none"} onValueChange={(v) => field.onChange(v === "none" ? "" : v)}>
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="Select unit" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        <SelectItem value="none">Catalogue default</SelectItem>
                        {UNITS.map((u) => (
                          <SelectItem key={u} value={u}>
                            {u}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            {/* Visual customizer */}
            <div className="space-y-2">
              <Label>Appearance</Label>
              <FormField
                control={form.control}
                name="imageMode"
                render={({ field }) => (
                  <FormItem>
                    <Select value={field.value} onValueChange={field.onChange}>
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        <SelectItem value="color">Color + icon</SelectItem>
                        <SelectItem value="image">Product thumbnail</SelectItem>
                      </SelectContent>
                    </Select>
                  </FormItem>
                )}
              />

              {watched.imageMode === "color" ? (
                <>
                  <FormField
                    control={form.control}
                    name="displayColor"
                    render={({ field }) => (
                      <FormItem>
                        <div className="flex flex-wrap gap-1.5">
                          {COLOR_PALETTE.map((color) => (
                            <button
                              key={color}
                              type="button"
                              onClick={() => field.onChange(color)}
                              className={cn(
                                "h-7 w-7 rounded-full border-2 transition",
                                field.value === color ? "border-foreground scale-110" : "border-transparent",
                              )}
                              style={{ backgroundColor: color }}
                              aria-label={`Use colour ${color}`}
                            />
                          ))}
                        </div>
                        <FormMessage />
                      </FormItem>
                    )}
                  />

                  <FormField
                    control={form.control}
                    name="displayIcon"
                    render={({ field }) => (
                      <FormItem>
                        <Input
                          placeholder="Search icons…"
                          value={iconSearch}
                          onChange={(e) => setIconSearch(e.target.value)}
                          className="mb-2"
                        />
                        <div className="grid max-h-40 grid-cols-8 gap-1 overflow-y-auto rounded-md border p-1">
                          {filteredIcons.map((name) => {
                            const Icon = icons[name as keyof typeof icons] as LucideIcon;
                            return (
                              <button
                                key={name}
                                type="button"
                                onClick={() => field.onChange(name)}
                                className={cn(
                                  "flex h-8 w-8 items-center justify-center rounded-md hover:bg-muted",
                                  field.value === name && "bg-primary text-primary-foreground",
                                )}
                                title={name}
                              >
                                <Icon className="h-4 w-4" />
                              </button>
                            );
                          })}
                        </div>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                </>
              ) : (
                <FormField
                  control={form.control}
                  name="imageData"
                  render={({ field }) => (
                    <FormItem>
                      <FormControl>
                        <Input
                          type="file"
                          accept="image/*"
                          onChange={(e) => handleImageUpload(e.target.files?.[0] ?? null)}
                        />
                      </FormControl>
                      {field.value && (
                        <img src={field.value} alt="preview" className="mt-2 h-16 w-16 rounded-md object-cover" />
                      )}
                      <FormMessage />
                    </FormItem>
                  )}
                />
              )}
            </div>

            {/* Hotkey */}
            <FormField
              control={form.control}
              name="hotkey"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Hotkey (optional)</FormLabel>
                  <Select value={field.value || "none"} onValueChange={(v) => field.onChange(v === "none" ? "" : v)}>
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder="None" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      <SelectItem value="none">None</SelectItem>
                      {HOTKEYS.map((k) => (
                        <SelectItem key={k} value={k}>
                          Num {k}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )}
            />

            {/* Live preview */}
            <div className="rounded-lg border bg-muted/30 p-3">
              <p className="mb-2 text-xs font-medium text-muted-foreground">Live preview</p>
              <div className="relative mx-auto h-24 w-24">
                <div
                  className="flex h-full w-full flex-col items-start justify-between gap-1 rounded-lg p-2 text-white shadow-sm"
                  style={{ backgroundColor: watched.imageMode === "image" && watched.imageData ? undefined : previewColor }}
                >
                  {watched.imageMode === "image" && watched.imageData ? (
                    <img src={watched.imageData} alt="tile" className="h-full w-full rounded-lg object-cover" />
                  ) : (
                    <>
                      <div className="flex w-full items-start justify-between gap-1">
                        {PreviewIcon ? <PreviewIcon className="h-4 w-4 shrink-0" /> : null}
                        <span className="line-clamp-2 text-xs font-semibold leading-tight">
                          {selectedItem?.nameEn ?? editing?.itemName ?? "Item name"}
                        </span>
                      </div>
                      <div className="w-full space-y-0.5">
                        <p className="truncate text-[10px] opacity-80">
                          {selectedItem?.sku ?? editing?.itemCode ?? ""}
                        </p>
                        <p className="text-[11px] font-bold">
                          {watched.defaultPrice
                            ? formatPosCurrency(Number(watched.defaultPrice))
                            : selectedItem
                              ? formatPosCurrency(selectedItem.sellingPrice)
                              : "—"}
                        </p>
                      </div>
                    </>
                  )}
                </div>
                {watched.hotkey && (
                  <span className="absolute -right-1 -top-1 flex h-5 w-5 items-center justify-center rounded-full bg-foreground text-[10px] font-bold text-background">
                    {watched.hotkey}
                  </span>
                )}
              </div>
              {position && (
                <p className="mt-2 text-center text-xs text-muted-foreground">
                  Slot: row {position.row + 1}, column {position.col + 1}
                </p>
              )}
            </div>

            <Button type="submit" className="w-full" disabled={isSubmitting || !form.watch("itemId")}>
              {editing ? "Save changes" : "Create tile"}
            </Button>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}
