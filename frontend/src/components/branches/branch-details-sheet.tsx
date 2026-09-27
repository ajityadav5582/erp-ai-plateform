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
  Mail,
  Phone,
  MapPin,
  Building2,
  CheckCircle2,
  PauseCircle,
  Pencil,
  Trash2,
  Loader2,
} from "lucide-react";
import { toast } from "sonner";
import {
  useGetBranchQuery,
  useActivateBranchMutation,
  useDeactivateBranchMutation,
} from "@/services/branch.service";
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
      <SheetContent className="sm:max-w-md overflow-y-auto">
        <SheetHeader className="pb-4">
          <SheetTitle>Branch Details</SheetTitle>
          <SheetDescription>
            Comprehensive branch information and metadata
          </SheetDescription>
        </SheetHeader>

        {isLoading && (
          <div className="flex h-64 items-center justify-center">
            <Loader2 className="size-8 animate-spin text-muted-foreground" />
          </div>
        )}

        {isError && (
          <div className="rounded-md bg-destructive/10 p-4 text-center text-sm text-destructive">
            Failed to load branch details.
          </div>
        )}

        {branch && (
          <div className="space-y-6">
            {/* Header profile card */}
            <div className="flex items-center gap-4 rounded-xl border border-border bg-card p-4 shadow-sm">
              <div className="flex size-14 items-center justify-center rounded-lg bg-primary/10 text-primary">
                <Building2 className="size-7" />
              </div>
              <div className="min-w-0 flex-1">
                <h3 className="truncate text-lg font-semibold text-foreground">
                  {branch.branchName}
                </h3>
                <p className="truncate text-sm text-muted-foreground font-mono">
                  {branch.branchCode}
                </p>
                <div className="mt-2">
                  <BranchStatusBadge status={branch.status} />
                </div>
              </div>
            </div>

            {/* Actions Toolbar */}
            <div className="flex items-center gap-2">
              {branch.status === "INACTIVE" ? (
                <Button
                  size="sm"
                  variant="outline"
                  className="flex-1 border-emerald-500/30 text-emerald-600 hover:bg-emerald-50 hover:text-emerald-700 dark:hover:bg-emerald-950/30"
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
                  className="flex-1 border-amber-500/30 text-amber-600 hover:bg-amber-50 hover:text-amber-700 dark:hover:bg-amber-950/30"
                  onClick={handleDeactivate}
                  disabled={isDeactivating}
                >
                  {isDeactivating ? <Loader2 className="mr-1.5 size-4 animate-spin" /> : <PauseCircle className="mr-1.5 size-4" />}
                  Deactivate
                </Button>
              ) : null}

              <Button size="sm" variant="outline" onClick={onEdit}>
                <Pencil className="mr-1.5 size-4" />
                Edit
              </Button>

              <Button size="sm" variant="destructive" onClick={onDelete}>
                <Trash2 className="size-4" />
              </Button>
            </div>

            <Separator />

            {/* Contact & Location fields */}
            <div className="space-y-3 text-sm">
              {branch.email && (
                <div className="flex items-center gap-3">
                  <Mail className="size-4 shrink-0 text-muted-foreground" />
                  <span className="text-foreground">{branch.email}</span>
                </div>
              )}

              {branch.phone && (
                <div className="flex items-center gap-3">
                  <Phone className="size-4 shrink-0 text-muted-foreground" />
                  <span className="text-foreground">{branch.phone}</span>
                </div>
              )}

              {branch.address && (
                <div className="flex items-start gap-3">
                  <MapPin className="size-4 shrink-0 text-muted-foreground mt-0.5" />
                  <span className="text-foreground">{branch.address}</span>
                </div>
              )}
            </div>

            <Separator />

            {/* Metadata & Timestamps */}
            <div className="space-y-2 rounded-lg bg-muted/50 p-3 text-xs text-muted-foreground">
              <div className="flex justify-between">
                <span>Branch ID:</span>
                <span className="font-mono">{branch.id}</span>
              </div>
              <div className="flex justify-between">
                <span>Tenant ID:</span>
                <span>{branch.tenantId}</span>
              </div>
              <div className="flex justify-between">
                <span>Version:</span>
                <span>{branch.version}</span>
              </div>
              {branch.createdAt && (
                <div className="flex justify-between">
                  <span>Created At:</span>
                  <span>{new Date(branch.createdAt).toLocaleDateString()}</span>
                </div>
              )}
              {branch.updatedAt && (
                <div className="flex justify-between">
                  <span>Updated At:</span>
                  <span>{new Date(branch.updatedAt).toLocaleDateString()}</span>
                </div>
              )}
            </div>
          </div>
        )}
      </SheetContent>
    </Sheet>
  );
}
