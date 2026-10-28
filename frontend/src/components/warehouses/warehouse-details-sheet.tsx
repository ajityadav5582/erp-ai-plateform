"use client";

import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetDescription,
} from "@/components/ui/sheet";
import { WarehouseStatusBadge } from "./warehouse-status-badge";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import {
  Warehouse as WarehouseIcon,
  CheckCircle2,
  PauseCircle,
  Pencil,
  Star,
  Loader2,
  Building2,
  Phone,
  MapPin,
  Boxes,
} from "lucide-react";
import { toast } from "sonner";
import {
  useGetWarehouseQuery,
  useActivateWarehouseMutation,
  useDeactivateWarehouseMutation,
  useSetDefaultWarehouseMutation,
  formatWarehouseType,
} from "@/services/warehouse.service";
import { getApiErrorMessage } from "@/services/error-handler";

interface WarehouseDetailsSheetProps {
  warehouseId: number | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onEdit: () => void;
}

export function WarehouseDetailsSheet({
  warehouseId,
  open,
  onOpenChange,
  onEdit,
}: WarehouseDetailsSheetProps) {
  const { data: warehouse, isLoading, isError } = useGetWarehouseQuery(
    warehouseId ?? 0,
    { skip: !warehouseId || !open }
  );

  const [activateWarehouse, { isLoading: isActivating }] =
    useActivateWarehouseMutation();
  const [deactivateWarehouse, { isLoading: isDeactivating }] =
    useDeactivateWarehouseMutation();
  const [setDefaultWarehouse, { isLoading: isSettingDefault }] =
    useSetDefaultWarehouseMutation();

  const handleActivate = async () => {
    if (!warehouse) return;
    try {
      await activateWarehouse(warehouse.id).unwrap();
      toast.success(`Warehouse "${warehouse.name}" activated successfully!`);
    } catch (err) {
      toast.error("Failed to activate warehouse", {
        description: getApiErrorMessage(err),
      });
    }
  };

  const handleDeactivate = async () => {
    if (!warehouse) return;
    try {
      await deactivateWarehouse(warehouse.id).unwrap();
      toast.success(`Warehouse "${warehouse.name}" deactivated.`);
    } catch (err) {
      toast.error("Failed to deactivate warehouse", {
        description: getApiErrorMessage(err),
      });
    }
  };

  const handleSetDefault = async () => {
    if (!warehouse) return;
    try {
      await setDefaultWarehouse(warehouse.id).unwrap();
      toast.success(
        `"${warehouse.name}" is now the default warehouse.`,
        {
          description:
            "Goods received with no explicit destination will land here.",
        }
      );
    } catch (err) {
      toast.error("Failed to set default warehouse", {
        description: getApiErrorMessage(err),
      });
    }
  };

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent className="sm:max-w-md overflow-y-auto">
        <SheetHeader className="sticky top-0 z-10 border-b border-border bg-background">
          <SheetTitle>Warehouse Details</SheetTitle>
          <SheetDescription>
            Storage location, default routing, and metadata
          </SheetDescription>
        </SheetHeader>

        {isLoading && (
          <div className="flex h-64 items-center justify-center">
            <Loader2 className="size-8 animate-spin text-muted-foreground" />
          </div>
        )}

        {isError && (
          <div className="rounded-md bg-destructive/10 p-4 text-center text-sm text-destructive">
            Failed to load warehouse details.
          </div>
        )}

        {warehouse && (
          <div className="space-y-6">
            {/* Header profile card */}
            <div className="flex items-center gap-4 rounded-xl border border-border bg-card p-4 shadow-sm">
              <div className="flex size-14 items-center justify-center rounded-lg bg-primary/10 text-primary">
                <WarehouseIcon className="size-7" />
              </div>
              <div className="min-w-0 flex-1">
                <h3 className="truncate text-lg font-semibold text-foreground">
                  {warehouse.name}
                </h3>
                <p className="truncate font-mono text-sm text-muted-foreground">
                  {warehouse.code}
                </p>
                <div className="mt-2 flex flex-wrap items-center gap-2">
                  <WarehouseStatusBadge isActive={warehouse.isActive} />
                  {warehouse.type && (
                    <span className="inline-flex items-center rounded-full bg-primary/10 px-2.5 py-0.5 text-xs font-medium text-primary">
                      {formatWarehouseType(warehouse.type)}
                    </span>
                  )}
                  {warehouse.isDefault && (
                    <span className="inline-flex items-center gap-1 rounded-full bg-amber-500/10 px-2.5 py-0.5 text-xs font-medium text-amber-600 dark:bg-amber-500/20 dark:text-amber-400 border border-amber-500/20">
                      <Star className="size-3.5" />
                      Default
                    </span>
                  )}
                </div>
              </div>
            </div>

            {/*
              The default warehouse cannot be deactivated (the backend refuses),
              so that option is hidden rather than left to fail. Showing a
              control that is guaranteed to 409 wastes the user's click and
              teaches them that this screen is unreliable.
            */}
            <div className="flex flex-wrap items-center gap-2">
              {warehouse.isActive && !warehouse.isDefault ? (
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
              ) : !warehouse.isActive ? (
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
              ) : null}

              {!warehouse.isDefault && (
                <Button
                  size="sm"
                  variant="outline"
                  onClick={handleSetDefault}
                  disabled={isSettingDefault}
                >
                  {isSettingDefault ? (
                    <Loader2 className="mr-1.5 size-4 animate-spin" />
                  ) : (
                    <Star className="mr-1.5 size-4" />
                  )}
                  Set as default
                </Button>
              )}

              <Button size="sm" variant="outline" onClick={onEdit}>
                <Pencil className="mr-1.5 size-4" />
                Edit
              </Button>
            </div>

            {warehouse.isDefault && (
              <p className="rounded-lg border border-amber-500/20 bg-amber-500/5 p-3 text-xs text-amber-700 dark:text-amber-400">
                This is the company&rsquo;s default warehouse. Goods received
                with no explicit destination are routed here, and it cannot be
                deactivated while it holds that role.
              </p>
            )}

            <Separator />

            {/* Location & contact info */}
            <div className="space-y-3 text-sm">
              {warehouse.phone && (
                <div className="flex items-center gap-3">
                  <Phone className="size-4 shrink-0 text-muted-foreground" />
                  <span className="text-foreground">{warehouse.phone}</span>
                </div>
              )}

              {warehouse.address && (
                <div className="flex items-start gap-3">
                  <MapPin className="mt-0.5 size-4 shrink-0 text-muted-foreground" />
                  <span className="text-foreground">{warehouse.address}</span>
                </div>
              )}

              <div className="flex items-center gap-3">
                <Building2 className="size-4 shrink-0 text-muted-foreground" />
                <span className="text-foreground">
                  {warehouse.branchId != null
                    ? `Branch ID: ${warehouse.branchId}`
                    : "Company-wide (no branch)"}
                </span>
              </div>

              <div className="flex items-center gap-3">
                <Boxes className="size-4 shrink-0 text-muted-foreground" />
                <span className="text-foreground">
                  Company ID: {warehouse.companyId}
                </span>
              </div>
            </div>

            <Separator />

            {/* Metadata & Timestamps */}
            <div className="space-y-2 rounded-lg bg-muted/50 p-3 text-xs text-muted-foreground">
              <div className="flex justify-between">
                <span>Warehouse ID:</span>
                <span className="font-mono">{warehouse.id}</span>
              </div>
              <div className="flex justify-between">
                <span>Tenant ID:</span>
                <span>{warehouse.tenantId}</span>
              </div>
              <div className="flex justify-between">
                <span>Version:</span>
                <span>{warehouse.version}</span>
              </div>
              {warehouse.createdAt && (
                <div className="flex justify-between">
                  <span>Created At:</span>
                  <span>
                    {new Date(warehouse.createdAt).toLocaleDateString()}
                  </span>
                </div>
              )}
              {warehouse.updatedAt && (
                <div className="flex justify-between">
                  <span>Updated At:</span>
                  <span>
                    {new Date(warehouse.updatedAt).toLocaleDateString()}
                  </span>
                </div>
              )}
            </div>
          </div>
        )}
      </SheetContent>
    </Sheet>
  );
}
