"use client";

import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetDescription,
} from "@/components/ui/sheet";
import { BranchStatusBadge } from "./branch-status-badge";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import {
  MapPin,
  Building2,
  CheckCircle2,
  PauseCircle,
  Pencil,
  Trash2,
  Loader2,
  Warehouse,
  ShieldCheck,
  Layers,
} from "lucide-react";
import { toast } from "sonner";
import {
  useGetBranchQuery,
  useActivateBranchMutation,
  useDeactivateBranchMutation,
} from "@/services/branch.service";
import {
  useGetProvincesQuery,
  useGetDistrictsByProvinceQuery,
  useGetLocalLevelsByDistrictQuery,
} from "@/services/geography.service";
import { getApiErrorMessage } from "@/services/error-handler";

interface BranchDetailsSheetProps {
  branchId: number | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onEdit: () => void;
  onDelete: () => void;
}

export function BranchDetailsSheet({
  branchId,
  open,
  onOpenChange,
  onEdit,
  onDelete,
}: BranchDetailsSheetProps) {
  const { data: branch, isLoading, isError } = useGetBranchQuery(branchId ?? 0, {
    skip: !branchId || !open,
  });

  const [activateBranch, { isLoading: isActivating }] = useActivateBranchMutation();
  const [deactivateBranch, { isLoading: isDeactivating }] = useDeactivateBranchMutation();

  // Geography lookups
  const { data: provinces = [] } = useGetProvincesQuery(undefined, { skip: !branch?.provinceId });
  const { data: districts = [] } = useGetDistrictsByProvinceQuery(
    branch?.provinceId ?? 0,
    { skip: !branch?.provinceId }
  );
  const { data: localLevels = [] } = useGetLocalLevelsByDistrictQuery(
    branch?.districtId ?? 0,
    { skip: !branch?.districtId }
  );

  const province = provinces.find((p) => p.id === branch?.provinceId);
  const district = districts.find((d) => d.id === branch?.districtId);
  const localLevel = localLevels.find((l) => (l.id ?? Number(l.municipalityId)) === branch?.localLevelId);

  const handleActivate = async () => {
    if (!branch) return;
    try {
      await activateBranch(branch.id).unwrap();
      toast.success(`Branch "${branch.branchName}" activated successfully!`);
    } catch (err) {
      toast.error("Failed to activate branch", {
        description: getApiErrorMessage(err),
      });
    }
  };

  const handleDeactivate = async () => {
    if (!branch) return;
    try {
      await deactivateBranch(branch.id).unwrap();
      toast.success(`Branch "${branch.branchName}" deactivated.`);
    } catch (err) {
      toast.error("Failed to deactivate branch", {
        description: getApiErrorMessage(err),
      });
    }
  };

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent className="sm:max-w-lg overflow-y-auto p-4 sm:p-6">
        <SheetHeader className="sticky top-0 z-10 border-b border-border bg-background pb-3">
          <SheetTitle className="text-lg font-bold sm:text-xl">Branch Profile</SheetTitle>
          <SheetDescription className="text-xs sm:text-sm">
            Comprehensive branch information and administrative metadata
          </SheetDescription>
        </SheetHeader>

        {isLoading && (
          <div className="flex h-64 items-center justify-center">
            <Loader2 className="size-8 animate-spin text-muted-foreground" />
          </div>
        )}

        {isError && (
          <div className="rounded-lg bg-destructive/10 p-4 text-center text-sm text-destructive mt-4">
            Failed to load branch details. Please try again.
          </div>
        )}

        {branch && (
          <div className="space-y-6 pt-4">
            {/* Header profile card */}
            <div className="flex items-start gap-3.5 rounded-xl border border-border bg-card p-4 shadow-xs">
              <div className="flex size-12 shrink-0 items-center justify-center rounded-xl bg-primary/10 text-primary">
                <Building2 className="size-6" />
              </div>
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-2">
                  <h3 className="truncate text-base font-semibold text-foreground sm:text-lg">
                    {branch.branchName}
                  </h3>
                  {branch.isHeadOffice && (
                    <span className="inline-flex items-center gap-1 rounded-full bg-primary/10 px-2 py-0.5 text-xs font-semibold text-primary">
                      <ShieldCheck className="size-3" />
                      Head Office
                    </span>
                  )}
                </div>
                <p className="mt-0.5 text-xs font-mono text-muted-foreground">
                  Code: {branch.branchCode}
                </p>
                <div className="mt-2.5">
                  <BranchStatusBadge status={branch.status} />
                </div>
              </div>
            </div>

            {/* Actions Toolbar */}
            <div className="flex flex-wrap items-center gap-2">
              {branch.status === "INACTIVE" ? (
                <Button
                  size="sm"
                  variant="outline"
                  className="flex-1 border-emerald-500/30 text-emerald-600 hover:bg-emerald-50 hover:text-emerald-700 dark:hover:bg-emerald-950/30 h-9"
                  onClick={handleActivate}
                  disabled={isActivating}
                >
                  {isActivating ? <Loader2 className="mr-1.5 size-4 animate-spin" /> : <CheckCircle2 className="mr-1.5 size-4" />}
                  Activate
                </Button>
              ) : branch.status === "ACTIVE" ? (
                <Button
                  size="sm"
                  variant="outline"
                  className="flex-1 border-amber-500/30 text-amber-600 hover:bg-amber-50 hover:text-amber-700 dark:hover:bg-amber-950/30 h-9"
                  onClick={handleDeactivate}
                  disabled={isDeactivating}
                >
                  {isDeactivating ? <Loader2 className="mr-1.5 size-4 animate-spin" /> : <PauseCircle className="mr-1.5 size-4" />}
                  Deactivate
                </Button>
              ) : null}

              <Button size="sm" variant="outline" onClick={onEdit} className="h-9 gap-1.5">
                <Pencil className="size-3.5" />
                Edit
              </Button>

              <Button size="sm" variant="destructive" onClick={onDelete} className="h-9 px-3">
                <Trash2 className="size-3.5" />
              </Button>
            </div>

            <Separator />

            {/* Location & Administrative Geography */}
            <div className="space-y-3">
              <div className="flex items-center gap-2 text-sm font-semibold text-foreground">
                <MapPin className="size-4 text-primary" />
                <span>Geographic Location</span>
              </div>

              <div className="grid grid-cols-1 gap-2.5 rounded-xl border border-border bg-card p-3.5 text-xs sm:text-sm">
                <div className="flex justify-between py-1 border-b border-border/50">
                  <span className="text-muted-foreground">Province:</span>
                  <span className="font-medium text-foreground">
                    {province ? (
                      <>
                        {province.provinceName}{" "}
                        {province.nepaliName && (
                          <span className="text-muted-foreground">({province.nepaliName})</span>
                        )}
                      </>
                    ) : branch.provinceId ? (
                      `Province #${branch.provinceId}`
                    ) : (
                      "Not specified"
                    )}
                  </span>
                </div>

                <div className="flex justify-between py-1 border-b border-border/50">
                  <span className="text-muted-foreground">District:</span>
                  <span className="font-medium text-foreground">
                    {district ? (
                      <>
                        {district.districtName}{" "}
                        {district.nepaliName && (
                          <span className="text-muted-foreground">({district.nepaliName})</span>
                        )}
                      </>
                    ) : branch.districtId ? (
                      `District #${branch.districtId}`
                    ) : (
                      "Not specified"
                    )}
                  </span>
                </div>

                <div className="flex justify-between py-1 border-b border-border/50">
                  <span className="text-muted-foreground">Local Level (Palika):</span>
                  <span className="font-medium text-foreground">
                    {localLevel ? (
                      <>
                        {localLevel.name}{" "}
                        {localLevel.nepaliName && (
                          <span className="text-muted-foreground">({localLevel.nepaliName})</span>
                        )}
                      </>
                    ) : branch.localLevelId ? (
                      `Palika #${branch.localLevelId}`
                    ) : (
                      "Not specified"
                    )}
                  </span>
                </div>

                <div className="flex justify-between py-1 border-b border-border/50">
                  <span className="text-muted-foreground">Ward No:</span>
                  <span className="font-medium text-foreground">
                    {branch.wardNo ? `Ward ${branch.wardNo}` : "Not specified"}
                  </span>
                </div>

                <div className="flex justify-between py-1">
                  <span className="text-muted-foreground">Street / Address:</span>
                  <span className="font-medium text-foreground text-right max-w-[200px] truncate">
                    {branch.address || "Not specified"}
                  </span>
                </div>
              </div>
            </div>

            {/* Warehouse Configuration (if present) */}
            {branch.defaultWarehouseId && (
              <>
                <Separator />
                <div className="space-y-2">
                  <div className="flex items-center gap-2 text-sm font-semibold text-foreground">
                    <Warehouse className="size-4 text-primary" />
                    <span>Inventory Fulfillment</span>
                  </div>
                  <div className="flex items-center justify-between rounded-lg border border-border bg-card p-3 text-xs sm:text-sm">
                    <span className="text-muted-foreground">Default Warehouse ID:</span>
                    <span className="font-mono font-medium text-foreground">
                      #{branch.defaultWarehouseId}
                    </span>
                  </div>
                </div>
              </>
            )}

            <Separator />

            {/* Audit & System Metadata */}
            <div className="space-y-2 rounded-xl bg-muted/40 p-3.5 text-xs text-muted-foreground">
              <div className="flex items-center gap-1.5 font-medium text-foreground mb-2">
                <Layers className="size-3.5 text-muted-foreground" />
                <span>Audit & Metadata</span>
              </div>
              <div className="flex justify-between py-0.5">
                <span>Branch ID:</span>
                <span className="font-mono text-foreground">{branch.id}</span>
              </div>
              <div className="flex justify-between py-0.5">
                <span>Tenant ID:</span>
                <span className="font-mono text-foreground">{branch.tenantId}</span>
              </div>
              <div className="flex justify-between py-0.5">
                <span>Company ID:</span>
                <span className="font-mono text-foreground">{branch.companyId}</span>
              </div>
              <div className="flex justify-between py-0.5">
                <span>Version:</span>
                <span className="text-foreground">{branch.version}</span>
              </div>
              {branch.createdAt && (
                <div className="flex justify-between py-0.5">
                  <span>Created At:</span>
                  <span className="text-foreground">{new Date(branch.createdAt).toLocaleString()}</span>
                </div>
              )}
              {branch.updatedAt && (
                <div className="flex justify-between py-0.5">
                  <span>Updated At:</span>
                  <span className="text-foreground">{new Date(branch.updatedAt).toLocaleString()}</span>
                </div>
              )}
            </div>
          </div>
        )}
      </SheetContent>
    </Sheet>
  );
}
