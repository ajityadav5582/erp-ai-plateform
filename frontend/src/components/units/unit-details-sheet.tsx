"use client";

import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetDescription,
} from "@/components/ui/sheet";
import { UnitStatusBadge } from "./unit-status-badge";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import {
  Ruler,
  CheckCircle2,
  PauseCircle,
  Pencil,
  Trash2,
  Loader2,
  Building2,
  Hash,
  Layers,
} from "lucide-react";
import { toast } from "sonner";
import {
  useGetUnitQuery,
  useActivateUnitMutation,
  useDeactivateUnitMutation,
  formatUnitDimension,
} from "@/services/unit.service";
import { getApiErrorMessage } from "@/services/error-handler";

interface UnitDetailsSheetProps {
  unitId: string | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onEdit: () => void;
  onDelete: () => void;
}

export function UnitDetailsSheet({
  unitId,
  open,
  onOpenChange,
  onEdit,
  onDelete,
}: UnitDetailsSheetProps) {
  const { data: unit, isLoading, isError } = useGetUnitQuery(unitId ?? "", {
    skip: !unitId || !open,
  });

  const [activateUnit, { isLoading: isActivating }] = useActivateUnitMutation();
  const [deactivateUnit, { isLoading: isDeactivating }] = useDeactivateUnitMutation();

  const handleActivate = async () => {
    if (!unit) return;
    try {
      await activateUnit(unit.id).unwrap();
      toast.success(`Unit "${unit.name}" activated successfully!`);
    } catch (err) {
      toast.error("Failed to activate unit", {
        description: getApiErrorMessage(err),
      });
    }
  };

  const handleDeactivate = async () => {
    if (!unit) return;
    try {
      await deactivateUnit(unit.id).unwrap();
      toast.success(`Unit "${unit.name}" deactivated.`);
    } catch (err) {
      toast.error("Failed to deactivate unit", {
        description: getApiErrorMessage(err),
      });
    }
  };

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent className="sm:max-w-md overflow-y-auto">
        <SheetHeader className="pb-4">
          <SheetTitle>Unit Details</SheetTitle>
          <SheetDescription>
            Comprehensive unit information and metadata
          </SheetDescription>
        </SheetHeader>

        {isLoading && (
          <div className="flex h-64 items-center justify-center">
            <Loader2 className="size-8 animate-spin text-muted-foreground" />
          </div>
        )}

        {isError && (
          <div className="rounded-md bg-destructive/10 p-4 text-center text-sm text-destructive">
            Failed to load unit details.
          </div>
        )}

        {unit && (
          <div className="space-y-6">
            {/* Header profile card */}
            <div className="flex items-center gap-4 rounded-xl border border-border bg-card p-4 shadow-sm">
              <div className="flex size-14 items-center justify-center rounded-lg bg-primary/10 text-primary">
                <Ruler className="size-7" />
              </div>
              <div className="min-w-0 flex-1">
                <h3 className="truncate text-lg font-semibold text-foreground">{unit.name}</h3>
                <p className="truncate text-sm text-muted-foreground font-mono">{unit.code}</p>
                <div className="mt-2 flex flex-wrap items-center gap-2">
                  <UnitStatusBadge isActive={unit.isActive} />
                  <span className="inline-flex items-center rounded-full bg-primary/10 px-2.5 py-0.5 text-xs font-medium text-primary">
                    {formatUnitDimension(unit.dimension)}
                  </span>
                </div>
              </div>
            </div>

            {/* Actions Toolbar */}
            <div className="flex items-center gap-2">
              {!unit.isActive ? (
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

            {/* Unit info */}
            <div className="space-y-3 text-sm">
              <div className="flex items-center gap-3">
                <Layers className="size-4 shrink-0 text-muted-foreground" />
                <span className="text-foreground">
                  Dimension: {formatUnitDimension(unit.dimension)}
                </span>
              </div>

              <div className="flex items-center gap-3">
                <Hash className="size-4 shrink-0 text-muted-foreground" />
                <span className="text-foreground">
                  Decimal places: {unit.decimalScale ?? 0}
                </span>
              </div>

              {unit.symbol && (
                <div className="flex items-center gap-3">
                  <Ruler className="size-4 shrink-0 text-muted-foreground" />
                  <span className="text-foreground">Symbol: {unit.symbol}</span>
                </div>
              )}

              <div className="flex items-center gap-3">
                <Building2 className="size-4 shrink-0 text-muted-foreground" />
                <span className="text-foreground">Company ID: {unit.companyId}</span>
              </div>
            </div>

            <Separator />

            {/* Metadata & Timestamps */}
            <div className="space-y-2 rounded-lg bg-muted/50 p-3 text-xs text-muted-foreground">
              <div className="flex justify-between">
                <span>Unit ID:</span>
                <span className="font-mono">{unit.id}</span>
              </div>
              <div className="flex justify-between">
                <span>Tenant ID:</span>
                <span>{unit.tenantId}</span>
              </div>
              <div className="flex justify-between">
                <span>Version:</span>
                <span>{unit.version}</span>
              </div>
              {unit.createdAt && (
                <div className="flex justify-between">
                  <span>Created At:</span>
                  <span>{new Date(unit.createdAt).toLocaleDateString()}</span>
                </div>
              )}
              {unit.updatedAt && (
                <div className="flex justify-between">
                  <span>Updated At:</span>
                  <span>{new Date(unit.updatedAt).toLocaleDateString()}</span>
                </div>
              )}
            </div>
          </div>
        )}
      </SheetContent>
    </Sheet>
  );
}
