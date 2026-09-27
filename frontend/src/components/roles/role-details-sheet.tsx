"use client";

import { useState } from "react";
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetDescription,
} from "@/components/ui/sheet";
import { RoleStatusBadge } from "./role-status-badge";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import {
  Shield,
  Pencil,
  Trash2,
  Loader2,
  Hash,
  FileText,
  Plus,
  X,
} from "lucide-react";
import { toast } from "sonner";
import {
  useGetRoleQuery,
} from "@/services/role.service";
import {
  useGetRolePermissionsQuery,
  useAssignPermissionMutation,
  useRemovePermissionMutation,
} from "@/services/role-permission.service";
import { useGetPermissionsQuery } from "@/services/permission.service";
import { getApiErrorMessage } from "@/services/error-handler";
import type { RolePermissionResponse } from "@/services/role-permission.service";

interface RoleDetailsSheetProps {
  roleId: number | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onEdit: () => void;
  onDelete: () => void;
}

export function RoleDetailsSheet({
  roleId,
  open,
  onOpenChange,
  onEdit,
  onDelete,
}: RoleDetailsSheetProps) {
  const { data: role, isLoading, isError } = useGetRoleQuery(roleId ?? 0, {
    skip: !roleId || !open,
  });

  const { data: rolePermissionsData } = useGetRolePermissionsQuery(
    roleId ? { roleId, params: { size: 100 } } : { roleId: 0 },
    { skip: !roleId || !open }
  );

  const { data: allPermissionsData } = useGetPermissionsQuery({
    size: 100,
    sort: "resource,asc",
  });

  const [assignPermission, { isLoading: isAssigning }] = useAssignPermissionMutation();
  const [removePermission, { isLoading: isRemoving }] = useRemovePermissionMutation();

  const [selectedPermissionId, setSelectedPermissionId] = useState<number | null>(null);
  const [searchTerm] = useState("");

  const handleAssignPermission = async () => {
    if (!role || !selectedPermissionId) return;
    try {
      await assignPermission({
        roleId: role.id,
        permissionId: selectedPermissionId,
      }).unwrap();
      toast.success("Permission assigned successfully!");
      setSelectedPermissionId(null);
    } catch (err) {
      toast.error("Failed to assign permission", {
        description: getApiErrorMessage(err),
      });
    }
  };

  const handleRemovePermission = async (permission: RolePermissionResponse) => {
    if (!role) return;
    try {
      await removePermission({
        roleId: role.id,
        permissionId: permission.id,
      }).unwrap();
      toast.success(`Permission "${permission.permissionCode}" removed successfully!`);
    } catch (err) {
      toast.error("Failed to remove permission", {
        description: getApiErrorMessage(err),
      });
    }
  };

  const assignedPermissions = rolePermissionsData?.content ?? [];
  const allPermissions = allPermissionsData?.permissions ?? [];

  // Filter out already assigned permissions
  const availablePermissions = allPermissions.filter(
    (p) => !assignedPermissions.some((ap) => ap.permissionId === p.id)
  );

  // Filter by search term
  const filteredAvailable = availablePermissions.filter(
    (p) =>
      p.permissionCode.toLowerCase().includes(searchTerm.toLowerCase()) ||
      p.resource.toLowerCase().includes(searchTerm.toLowerCase()) ||
      p.action.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent className="sm:max-w-md overflow-y-auto">
        <SheetHeader className="pb-4">
          <SheetTitle>Role Details</SheetTitle>
          <SheetDescription>
            Comprehensive role information and metadata
          </SheetDescription>
        </SheetHeader>

        {isLoading && (
          <div className="flex h-64 items-center justify-center">
            <Loader2 className="size-8 animate-spin text-muted-foreground" />
          </div>
        )}

        {isError && (
          <div className="rounded-md bg-destructive/10 p-4 text-center text-sm text-destructive">
            Failed to load role details.
          </div>
        )}

        {role && (
          <div className="space-y-6">
            {/* Header profile card */}
            <div className="flex items-center gap-4 rounded-xl border border-border bg-card p-4 shadow-sm">
              <div className="flex size-14 items-center justify-center rounded-lg bg-primary/10 text-primary">
                <Shield className="size-7" />
              </div>
              <div className="min-w-0 flex-1">
                <h3 className="truncate text-lg font-semibold text-foreground">
                  {role.roleName}
                </h3>
                <p className="truncate text-sm text-muted-foreground font-mono">
                  {role.roleCode}
                </p>
                <div className="mt-2">
                  <RoleStatusBadge status={role.isActive ? "ACTIVE" : "INACTIVE"} roleType={role.roleType} />
                </div>
              </div>
            </div>

            {/* Actions Toolbar */}
            <div className="flex items-center gap-2">
              <Button size="sm" variant="outline" onClick={onEdit}>
                <Pencil className="mr-1.5 size-4" />
                Edit
              </Button>

              {!role.isSystemRole && (
                <Button size="sm" variant="destructive" onClick={onDelete}>
                  <Trash2 className="size-4" />
                </Button>
              )}
            </div>

            <Separator />

            {/* Role Info */}
            <div className="space-y-3 text-sm">
              {role.description && (
                <div className="flex items-start gap-3">
                  <FileText className="size-4 shrink-0 text-muted-foreground mt-0.5" />
                  <span className="text-foreground">{role.description}</span>
                </div>
              )}

              <div className="flex items-center gap-3">
                <Hash className="size-4 shrink-0 text-muted-foreground" />
                <span className="text-foreground">Role Code: {role.roleCode}</span>
              </div>

              <div className="flex items-center gap-3">
                <Shield className="size-4 shrink-0 text-muted-foreground" />
                <span className="text-foreground">Type: {role.roleType}</span>
              </div>
            </div>

            <Separator />

            {/* Permissions Section */}
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <h4 className="text-sm font-medium">Permissions ({assignedPermissions.length})</h4>
              </div>

              {/* Assigned Permissions */}
              <div className="space-y-2">
                {assignedPermissions.length === 0 ? (
                  <p className="text-sm text-muted-foreground">No permissions assigned.</p>
                ) : (
                  assignedPermissions.map((permission) => (
                    <div
                      key={permission.id}
                      className="flex items-center justify-between rounded-md border p-2"
                    >
                      <div className="min-w-0 flex-1">
                        <p className="font-mono text-sm font-medium">{permission.permissionCode}</p>
                        <p className="text-xs text-muted-foreground">
                          {permission.resource} - {permission.action}
                        </p>
                      </div>
                      <Button
                        variant="ghost"
                        size="icon"
                        className="size-7 shrink-0"
                        onClick={() => handleRemovePermission(permission)}
                        disabled={isRemoving}
                      >
                        {isRemoving ? (
                          <Loader2 className="size-3.5 animate-spin" />
                        ) : (
                          <X className="size-3.5 text-destructive" />
                        )}
                      </Button>
                    </div>
                  ))
                )}
              </div>

              {/* Assign Permission */}
              <div className="space-y-2">
                <label className="text-sm font-medium">Assign Permission</label>
                <div className="flex gap-2">
                  <select
                    value={selectedPermissionId ?? ""}
                    onChange={(e) => setSelectedPermissionId(e.target.value ? Number(e.target.value) : null)}
                    className="h-9 flex-1 rounded-md border border-input bg-background px-3 py-1 text-sm shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                    disabled={isAssigning || filteredAvailable.length === 0}
                  >
                    <option value="">Select permission to assign</option>
                    {filteredAvailable.map((permission) => (
                      <option key={permission.id} value={permission.id}>
                        {permission.permissionCode}
                      </option>
                    ))}
                  </select>
                  <Button
                    size="sm"
                    onClick={handleAssignPermission}
                    disabled={!selectedPermissionId || isAssigning}
                  >
                    {isAssigning ? (
                      <Loader2 className="size-4 animate-spin" />
                    ) : (
                      <Plus className="size-4" />
                    )}
                  </Button>
                </div>
                {filteredAvailable.length === 0 && (
                  <p className="text-xs text-muted-foreground">No more permissions available to assign.</p>
                )}
              </div>
            </div>

            <Separator />

            {/* Role metadata */}
            <div className="space-y-2 rounded-lg bg-muted/50 p-3 text-xs text-muted-foreground">
              <div className="flex justify-between">
                <span>Role ID:</span>
                <span className="font-mono">{role.id}</span>
              </div>
              <div className="flex justify-between">
                <span>Tenant ID:</span>
                <span>{role.tenantId}</span>
              </div>
              <div className="flex justify-between">
                <span>System Role:</span>
                <span>{role.isSystemRole ? "Yes" : "No"}</span>
              </div>
            </div>
          </div>
        )}
      </SheetContent>
    </Sheet>
  );
}
