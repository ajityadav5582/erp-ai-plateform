"use client";

import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetDescription,
} from "@/components/ui/sheet";
import { DepartmentStatusBadge } from "./department-status-badge";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import {
  FolderTree,
  CheckCircle2,
  PauseCircle,
  Pencil,
  Trash2,
  Loader2,
  Building2,
  User,
} from "lucide-react";
import { toast } from "sonner";
import {
  useGetDepartmentQuery,
  useActivateDepartmentMutation,
  useDeactivateDepartmentMutation,
} from "@/services/department.service";
import { getApiErrorMessage } from "@/services/error-handler";

interface DepartmentDetailsSheetProps {
  departmentId: string | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onEdit: () => void;
  onDelete: () => void;
}

export function DepartmentDetailsSheet({
  departmentId,
  open,
  onOpenChange,
  onEdit,
  onDelete,
}: DepartmentDetailsSheetProps) {
  const { data: department, isLoading, isError } = useGetDepartmentQuery(departmentId ?? "", {
    skip: !departmentId || !open,
  });

  const [activateDepartment, { isLoading: isActivating }] = useActivateDepartmentMutation();
  const [deactivateDepartment, { isLoading: isDeactivating }] = useDeactivateDepartmentMutation();

  const handleActivate = async () => {
    if (!department) return;
    try {
      await activateDepartment(department.departmentId).unwrap();
      toast.success(`Department "${department.departmentName}" activated successfully!`);
    } catch (err) {
      toast.error("Failed to activate department", {
        description: getApiErrorMessage(err),
      });
    }
  };

  const handleDeactivate = async () => {
    if (!department) return;
    try {
      await deactivateDepartment(department.departmentId).unwrap();
      toast.success(`Department "${department.departmentName}" deactivated.`);
    } catch (err) {
      toast.error("Failed to deactivate department", {
        description: getApiErrorMessage(err),
      });
    }
  };

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent className="sm:max-w-md overflow-y-auto">
        <SheetHeader className="pb-4">
          <SheetTitle>Department Details</SheetTitle>
          <SheetDescription>
            Comprehensive department information and metadata
          </SheetDescription>
        </SheetHeader>

        {isLoading && (
          <div className="flex h-64 items-center justify-center">
            <Loader2 className="size-8 animate-spin text-muted-foreground" />
          </div>
        )}

        {isError && (
          <div className="rounded-md bg-destructive/10 p-4 text-center text-sm text-destructive">
            Failed to load department details.
          </div>
        )}

        {department && (
          <div className="space-y-6">
            {/* Header profile card */}
            <div className="flex items-center gap-4 rounded-xl border border-border bg-card p-4 shadow-sm">
              <div className="flex size-14 items-center justify-center rounded-lg bg-primary/10 text-primary">
                <FolderTree className="size-7" />
              </div>
              <div className="min-w-0 flex-1">
                <h3 className="truncate text-lg font-semibold text-foreground">
                  {department.departmentName}
                </h3>
                <p className="truncate text-sm text-muted-foreground font-mono">
                  {department.departmentCode}
                </p>
                <div className="mt-2">
                  <DepartmentStatusBadge status={department.status} />
                </div>
              </div>
            </div>

            {/* Actions Toolbar */}
            <div className="flex items-center gap-2">
              {department.status === "INACTIVE" ? (
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
              ) : department.status === "ACTIVE" ? (
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

            {/* Department Info */}
            <div className="space-y-3 text-sm">
              {department.description && (
                <div className="flex items-start gap-3">
                  <FolderTree className="size-4 shrink-0 text-muted-foreground mt-0.5" />
                  <span className="text-foreground">{department.description}</span>
                </div>
              )}

              <div className="flex items-center gap-3">
                <Building2 className="size-4 shrink-0 text-muted-foreground" />
                <span className="text-foreground">Branch ID: {department.branchId}</span>
              </div>

              {department.managerId && (
                <div className="flex items-center gap-3">
                  <User className="size-4 shrink-0 text-muted-foreground" />
                  <span className="text-foreground">Manager ID: {department.managerId}</span>
                </div>
              )}

              {department.parentDepartmentId && (
                <div className="flex items-center gap-3">
                  <FolderTree className="size-4 shrink-0 text-muted-foreground" />
                  <span className="text-foreground">Parent Department ID: {department.parentDepartmentId}</span>
                </div>
              )}
            </div>

            <Separator />

            {/* Metadata & Timestamps */}
            <div className="space-y-2 rounded-lg bg-muted/50 p-3 text-xs text-muted-foreground">
              <div className="flex justify-between">
                <span>Department UUID:</span>
                <span className="font-mono">{department.departmentId}</span>
              </div>
              <div className="flex justify-between">
                <span>Tenant ID:</span>
                <span>{department.tenantId}</span>
              </div>
              <div className="flex justify-between">
                <span>Version:</span>
                <span>{department.version}</span>
              </div>
              {department.createdAt && (
                <div className="flex justify-between">
                  <span>Created At:</span>
                  <span>{new Date(department.createdAt).toLocaleDateString()}</span>
                </div>
              )}
              {department.updatedAt && (
                <div className="flex justify-between">
                  <span>Updated At:</span>
                  <span>{new Date(department.updatedAt).toLocaleDateString()}</span>
                </div>
              )}
            </div>
          </div>
        )}
      </SheetContent>
    </Sheet>
  );
}
