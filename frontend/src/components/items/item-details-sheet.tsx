"use client";

import type { ReactNode } from "react";
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetDescription,
} from "@/components/ui/sheet";
import { ItemStatusBadge } from "@/components/items/item-status-badge";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import {
  Package,
  CheckCircle2,
  PauseCircle,
  Pencil,
  Trash2,
  Loader2,
  Barcode,
  Hash,
  Tags,
  Boxes,
  Percent,
  Banknote,
} from "lucide-react";
import { toast } from "sonner";
import {
  useGetItemQuery,
  useActivateItemMutation,
  useDeactivateItemMutation,
  formatExciseType,
  formatItemType,
  formatTaxabilityType,
} from "@/services/item.service";
import { getApiErrorMessage } from "@/services/error-handler";

interface ItemDetailsSheetProps {
  itemId: string | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onEdit: () => void;
  onDelete: () => void;
}

/** Renders a label/value row, or nothing when the value is absent. */
function DetailRow({
  icon,
  label,
  value,
}: {
  icon: ReactNode;
  label: string;
  value: ReactNode;
}) {
  if (value === null || value === undefined || value === "" || value === "—") {
    return null;
  }
  return (
    <div className="flex items-start gap-3">
      <span className="mt-0.5 shrink-0 text-muted-foreground">{icon}</span>
      <span className="text-foreground">
        <span className="text-muted-foreground">{label}: </span>
        {value}
      </span>
    </div>
  );
}

export function ItemDetailsSheet({
  itemId,
  open,
  onOpenChange,
  onEdit,
  onDelete,
}: ItemDetailsSheetProps) {
  const { data: item, isLoading, isError } = useGetItemQuery(itemId ?? "", {
    skip: !itemId || !open,
  });

  const [activateItem, { isLoading: isActivating }] = useActivateItemMutation();
  const [deactivateItem, { isLoading: isDeactivating }] = useDeactivateItemMutation();

  const handleActivate = async () => {
    if (!item) return;
    try {
      await activateItem(item.id).unwrap();
      toast.success(`Item "${item.nameEn}" activated successfully!`);
    } catch (err) {
      toast.error("Failed to activate item", {
        description: getApiErrorMessage(err),
      });
    }
  };

  const handleDeactivate = async () => {
    if (!item) return;
    try {
      await deactivateItem(item.id).unwrap();
      toast.success(`Item "${item.nameEn}" deactivated.`);
    } catch (err) {
      toast.error("Failed to deactivate item", {
        description: getApiErrorMessage(err),
      });
    }
  };

  const trackingFlags = item
    ? [
        item.isInventoryTracked ? "Inventory tracked" : null,
        item.isBatchTracked ? "Batch tracked" : null,
        item.isExpiryTracked ? "Expiry tracked" : null,
        item.isVatInclusive ? "VAT inclusive" : null,
      ].filter(Boolean)
    : [];

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent className="overflow-y-auto sm:max-w-lg">
        <SheetHeader className="pb-4">
          <SheetTitle>Item Details</SheetTitle>
          <SheetDescription>
            Comprehensive item information and metadata
          </SheetDescription>
        </SheetHeader>

        {isLoading && (
          <div className="flex h-64 items-center justify-center">
            <Loader2 className="size-8 animate-spin text-muted-foreground" />
          </div>
        )}

        {isError && (
          <div className="rounded-md bg-destructive/10 p-4 text-center text-sm text-destructive">
            Failed to load item details.
          </div>
        )}

        {item && (
          <div className="space-y-6">
            {/* Header profile card */}
            <div className="flex items-center gap-4 rounded-xl border border-border bg-card p-4 shadow-sm">
              <div className="flex size-14 items-center justify-center rounded-lg bg-primary/10 text-primary">
                <Package className="size-7" />
              </div>
              <div className="min-w-0 flex-1">
                <h3 className="truncate text-lg font-semibold text-foreground">
                  {item.nameEn}
                </h3>
                <p className="truncate font-mono text-sm text-muted-foreground">
                  {item.sku}
                </p>
                <div className="mt-2 flex flex-wrap items-center gap-2">
                  <ItemStatusBadge isActive={item.isActive} />
                  <span className="inline-flex items-center rounded-full bg-primary/10 px-2.5 py-0.5 text-xs font-medium text-primary">
                    {formatItemType(item.itemType)}
                  </span>
                </div>
              </div>
            </div>

            {/* Actions Toolbar */}
            <div className="flex items-center gap-2">
              {!item.isActive ? (
                <Button
                  size="sm"
                  variant="outline"
                  className="flex-1 border-emerald-500/30 text-emerald-600 hover:bg-emerald-50 hover:text-emerald-700 dark:hover:bg-emerald-950/30"
                  onClick={handleActivate}
                  disabled={isActivating}
                >
                  {isActivating ? (
                    <Loader2 className="mr-1.5 size-4 animate-spin" />
                  ) : (
                    <CheckCircle2 className="mr-1.5 size-4" />
                  )}
                  Activate
                </Button>
              ) : (
                <Button
                  size="sm"
                  variant="outline"
                  className="flex-1 border-amber-500/30 text-amber-600 hover:bg-amber-50 hover:text-amber-700 dark:hover:bg-amber-950/30"
                  onClick={handleDeactivate}
                  disabled={isDeactivating}
                >
                  {isDeactivating ? (
                    <Loader2 className="mr-1.5 size-4 animate-spin" />
                  ) : (
                    <PauseCircle className="mr-1.5 size-4" />
                  )}
                  Deactivate
                </Button>
              )}

              <Button size="sm" variant="outline" onClick={onEdit}>
                <Pencil className="mr-1.5 size-4" />
                Edit
              </Button>

              <Button size="sm" variant="destructive" onClick={onDelete}>
                <Trash2 className="size-4" />
              </Button>
            </div>

            <Separator />

            {/* Identification */}
            <div className="space-y-3 text-sm">
              <DetailRow
                icon={<Barcode className="size-4" />}
                label="Barcode"
                value={item.barcode}
              />
              <DetailRow icon={<Hash className="size-4" />} label="HS Code" value={item.hsCode} />
              <DetailRow
                icon={<Package className="size-4" />}
                label="Nepali name"
                value={item.nameNp}
              />
              <DetailRow
                icon={<Tags className="size-4" />}
                label="Category"
                value={item.categoryName}
              />
              <DetailRow
                icon={<Boxes className="size-4" />}
                label="Unit"
                value={
                  item.uomName
                    ? `${item.uomName}${item.uomCode ? ` (${item.uomCode})` : ""}`
                    : item.uomCode
                }
              />
              <DetailRow
                icon={<Package className="size-4" />}
                label="Description"
                value={item.descriptionEn}
              />
            </div>

            <Separator />

            {/* Pricing */}
            <div className="space-y-3 text-sm">
              <DetailRow
                icon={<Banknote className="size-4" />}
                label="Selling price"
                value={item.sellingPrice}
              />
              <DetailRow
                icon={<Banknote className="size-4" />}
                label="Purchase price"
                value={item.purchasePrice}
              />
              <DetailRow icon={<Banknote className="size-4" />} label="MRP" value={item.mrp} />
            </div>

            <Separator />

            {/* Tax */}
            <div className="space-y-3 text-sm">
              <DetailRow
                icon={<Percent className="size-4" />}
                label="VAT treatment"
                value={formatTaxabilityType(item.taxabilityType)}
              />
              <DetailRow
                icon={<Percent className="size-4" />}
                label="VAT rate"
                value={item.vatRate === null ? null : `${item.vatRate}%`}
              />
              <DetailRow
                icon={<Percent className="size-4" />}
                label="Excise"
                value={
                  item.exciseType && item.exciseType !== "NONE"
                    ? item.exciseType === "PERCENTAGE"
                      ? `${formatExciseType(item.exciseType)} — ${item.exciseRate ?? 0}%`
                      : `${formatExciseType(item.exciseType)} — ${item.exciseAmount ?? 0}`
                    : null
                }
              />
            </div>

            <Separator />

            {/* Inventory */}
            <div className="space-y-3 text-sm">
              <DetailRow
                icon={<Boxes className="size-4" />}
                label="Minimum stock level"
                value={item.minStockLevel}
              />
              <DetailRow
                icon={<Boxes className="size-4" />}
                label="Reorder level"
                value={item.reorderLevel}
              />
              <DetailRow
                icon={<Boxes className="size-4" />}
                label="Reorder quantity"
                value={item.reorderQuantity}
              />
              {trackingFlags.length > 0 && (
                <div className="flex flex-wrap gap-1.5 pt-1">
                  {trackingFlags.map((flag) => (
                    <span
                      key={flag}
                      className="inline-flex items-center rounded-full bg-muted px-2.5 py-0.5 text-xs font-medium text-muted-foreground"
                    >
                      {flag}
                    </span>
                  ))}
                </div>
              )}
            </div>

            <Separator />

            {/* Metadata & Timestamps */}
            <div className="space-y-2 rounded-lg bg-muted/50 p-3 text-xs text-muted-foreground">
              <div className="flex justify-between">
                <span>Item ID:</span>
                <span className="font-mono">{item.id}</span>
              </div>
              <div className="flex justify-between">
                <span>Company ID:</span>
                <span>{item.companyId}</span>
              </div>
              <div className="flex justify-between">
                <span>Tenant ID:</span>
                <span>{item.tenantId}</span>
              </div>
              <div className="flex justify-between">
                <span>Version:</span>
                <span>{item.version}</span>
              </div>
              {item.createdAt && (
                <div className="flex justify-between">
                  <span>Created At:</span>
                  <span>{new Date(item.createdAt).toLocaleDateString()}</span>
                </div>
              )}
              {item.updatedAt && (
                <div className="flex justify-between">
                  <span>Updated At:</span>
                  <span>{new Date(item.updatedAt).toLocaleDateString()}</span>
                </div>
              )}
            </div>
          </div>
        )}
      </SheetContent>
    </Sheet>
  );
}
