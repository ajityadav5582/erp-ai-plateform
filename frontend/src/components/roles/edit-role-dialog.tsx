"use client";

import { useEffect, useState, useMemo } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Loader2, Shield, Search, CheckSquare, Square, ChevronDown, ChevronRight, Lock } from "lucide-react";
import { toast } from "sonner";
import { useUpdateRoleMutation, type Role } from "@/services/role.service";
import { useGetPermissionsQuery, type PermissionResponse } from "@/services/permission.service";
import { useGetRolePermissionsQuery } from "@/services/role-permission.service";
import { getApiErrorMessage } from "@/services/error-handler";

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
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
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";

const editRoleSchema = z.object({
  roleName: z
    .string()
    .min(1, "Role name is required")
    .max(100, "Role name must not exceed 100 characters"),
  description: z.string().max(500, "Description must not exceed 500 characters").optional(),
  permissionIds: z.array(z.number()),
});

type EditRoleFormValues = z.infer<typeof editRoleSchema>;

interface EditRoleDialogProps {
  role: Role | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function EditRoleDialog({ role, open, onOpenChange }: EditRoleDialogProps) {
  const [updateRole, { isLoading }] = useUpdateRoleMutation();

  const { data: permissionsData, isLoading: isPermissionsLoading } = useGetPermissionsQuery(
    { size: 500 },
    { skip: !open }
  );

  const { data: rolePermissionsData } = useGetRolePermissionsQuery(
    role?.id ? { roleId: role.id, params: { size: 500 } } : { roleId: 0 },
    { skip: !role?.id || !open }
  );

  const [permissionSearch, setPermissionSearch] = useState("");
  const [collapsedGroups, setCollapsedGroups] = useState<Record<string, boolean>>({});

  const form = useForm<EditRoleFormValues>({
    resolver: zodResolver(editRoleSchema),
    defaultValues: {
      roleName: "",
      description: "",
      permissionIds: [],
    },
  });

  useEffect(() => {
    if (role && open) {
      let initialPermIds: number[] = [];
      if (Array.isArray(role.permissionIds)) {
        initialPermIds = role.permissionIds;
      } else if (rolePermissionsData?.content) {
        initialPermIds = rolePermissionsData.content.map((rp) => rp.permissionId);
      }

      form.reset({
        roleName: role.roleName,
        description: role.description || "",
        permissionIds: initialPermIds,
      });
    }
  }, [role, open, rolePermissionsData, form]);

  const selectedPermissionIds = form.watch("permissionIds") || [];

  const allPermissions = useMemo(() => {
    return permissionsData?.permissions ?? [];
  }, [permissionsData]);

  // Group permissions by resource
  const groupedPermissions = useMemo(() => {
    const groups: Record<string, PermissionResponse[]> = {};
    const searchLower = permissionSearch.toLowerCase().trim();

    allPermissions.forEach((p) => {
      if (
        searchLower &&
        !p.permissionCode.toLowerCase().includes(searchLower) &&
        !p.resource.toLowerCase().includes(searchLower) &&
        !p.action.toLowerCase().includes(searchLower)
      ) {
        return;
      }
      const groupKey = p.resource || "OTHER";
      if (!groups[groupKey]) {
        groups[groupKey] = [];
      }
      groups[groupKey].push(p);
    });

    return groups;
  }, [allPermissions, permissionSearch]);

  const togglePermission = (id: number) => {
    const current = new Set(selectedPermissionIds);
    if (current.has(id)) {
      current.delete(id);
    } else {
      current.add(id);
    }
    form.setValue("permissionIds", Array.from(current), { shouldValidate: true });
  };

  const toggleGroup = (groupName: string, groupPerms: PermissionResponse[]) => {
    const current = new Set(selectedPermissionIds);
    const allGroupIds = groupPerms.map((p) => p.id);
    const isAllSelected = allGroupIds.every((id) => current.has(id));

    if (isAllSelected) {
      allGroupIds.forEach((id) => current.delete(id));
    } else {
      allGroupIds.forEach((id) => current.add(id));
    }
    form.setValue("permissionIds", Array.from(current), { shouldValidate: true });
  };

  const toggleCollapseGroup = (groupName: string) => {
    setCollapsedGroups((prev) => ({
      ...prev,
      [groupName]: !prev[groupName],
    }));
  };

  const onSubmit = async (values: EditRoleFormValues) => {
    if (!role) return;
    try {
      const payload = {
        roleName: values.roleName,
        description: values.description || undefined,
        permissionIds: values.permissionIds,
      };
      const result = await updateRole({
        roleId: role.id,
        data: payload,
      }).unwrap();
      toast.success(`Role "${result.roleName}" updated successfully!`, {
        description: `Updated with ${values.permissionIds.length} permissions assigned`,
      });
      onOpenChange(false);
    } catch (err) {
      toast.error("Failed to update role", {
        description: getApiErrorMessage(err),
      });
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-3xl max-h-[90vh] flex flex-col overflow-hidden">
        <DialogHeader className="px-1 pt-1 pb-3 border-b">
          <div className="flex items-center gap-2.5">
            <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10 text-primary shadow-sm">
              <Shield className="size-5" />
            </div>
            <div>
              <DialogTitle className="text-xl font-bold">Edit Role</DialogTitle>
              <DialogDescription>
                Update role details and permissions for {role?.roleName} ({role?.roleCode})
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="flex flex-1 flex-col overflow-hidden space-y-4 pt-4">
            <div className="flex-1 overflow-y-auto pr-2 space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <FormLabel className="font-semibold text-xs text-muted-foreground uppercase">Role Code</FormLabel>
                  <div className="h-9 px-3 py-1.5 rounded-md border border-input bg-muted/50 font-mono text-sm font-medium flex items-center text-muted-foreground mt-1">
                    {role?.roleCode}
                  </div>
                </div>

                <FormField
                  control={form.control}
                  name="roleName"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel className="font-semibold">Role Name *</FormLabel>
                      <FormControl>
                        <Input placeholder="e.g. Project Manager" disabled={isLoading} {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </div>

              <FormField
                control={form.control}
                name="description"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel className="font-semibold">Description</FormLabel>
                    <FormControl>
                      <Input placeholder="Role description and responsibilities" disabled={isLoading} {...field} value={field.value ?? ""} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              {/* Permission Assignment Section */}
              <div className="space-y-3 pt-2">
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b pb-2">
                  <div className="flex items-center gap-2">
                    <Lock className="size-4 text-primary" />
                    <h3 className="font-bold text-sm text-foreground">Assigned Permissions</h3>
                    <span className="inline-flex items-center rounded-full bg-primary/10 px-2.5 py-0.5 text-xs font-semibold text-primary">
                      {selectedPermissionIds.length} Selected
                    </span>
                  </div>

                  <div className="relative max-w-xs w-full">
                    <Search className="absolute left-2.5 top-1/2 size-3.5 -translate-y-1/2 text-muted-foreground" />
                    <Input
                      placeholder="Filter permissions..."
                      value={permissionSearch}
                      onChange={(e) => setPermissionSearch(e.target.value)}
                      className="h-8 pl-8 text-xs"
                    />
                  </div>
                </div>

                {isPermissionsLoading ? (
                  <div className="flex h-32 items-center justify-center">
                    <Loader2 className="size-6 animate-spin text-muted-foreground" />
                  </div>
                ) : Object.keys(groupedPermissions).length === 0 ? (
                  <div className="rounded-lg border border-dashed p-6 text-center text-xs text-muted-foreground">
                    No permissions match your filter.
                  </div>
                ) : (
                  <div className="space-y-3 max-h-[300px] overflow-y-auto pr-1">
                    {Object.entries(groupedPermissions).map(([resourceGroup, groupPerms]) => {
                      const isCollapsed = collapsedGroups[resourceGroup];
                      const allGroupSelected = groupPerms.every((p) => selectedPermissionIds.includes(p.id));

                      return (
                        <div key={resourceGroup} className="rounded-lg border border-border bg-card overflow-hidden shadow-2xs">
                          {/* Group Header */}
                          <div className="flex items-center justify-between bg-muted/40 px-3 py-2 text-xs font-semibold">
                            <button
                              type="button"
                              onClick={() => toggleCollapseGroup(resourceGroup)}
                              className="flex items-center gap-2 text-foreground hover:text-primary transition-colors"
                            >
                              {isCollapsed ? <ChevronRight className="size-3.5" /> : <ChevronDown className="size-3.5" />}
                              <span>{resourceGroup}</span>
                              <span className="text-muted-foreground text-[11px] font-normal">
                                ({groupPerms.filter((p) => selectedPermissionIds.includes(p.id)).length}/
                                {groupPerms.length})
                              </span>
                            </button>

                            <Button
                              type="button"
                              variant="ghost"
                              size="sm"
                              onClick={() => toggleGroup(resourceGroup, groupPerms)}
                              className="h-6 px-2 text-[11px] hover:bg-primary/10 hover:text-primary"
                            >
                              {allGroupSelected ? "Deselect All" : "Select All"}
                            </Button>
                          </div>

                          {/* Group Permissions Grid */}
                          {!isCollapsed && (
                            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 p-2.5 bg-background/50">
                              {groupPerms.map((perm) => {
                                const isChecked = selectedPermissionIds.includes(perm.id);
                                return (
                                  <div
                                    key={perm.id}
                                    onClick={() => togglePermission(perm.id)}
                                    className={`flex items-start gap-2.5 rounded-md border p-2 text-xs cursor-pointer transition-all ${
                                      isChecked
                                        ? "border-primary/50 bg-primary/5 shadow-2xs"
                                        : "border-border/60 hover:border-border hover:bg-muted/30"
                                    }`}
                                  >
                                    <div className="mt-0.5 shrink-0 text-primary">
                                      {isChecked ? (
                                        <CheckSquare className="size-4 fill-primary/10 text-primary" />
                                      ) : (
                                        <Square className="size-4 text-muted-foreground" />
                                      )}
                                    </div>
                                    <div className="min-w-0 flex-1">
                                      <p className="font-mono font-medium text-foreground truncate">
                                        {perm.permissionCode}
                                      </p>
                                      {perm.description && (
                                        <p className="text-[11px] text-muted-foreground line-clamp-1 mt-0.5">
                                          {perm.description}
                                        </p>
                                      )}
                                    </div>
                                  </div>
                                );
                              })}
                            </div>
                          )}
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>
            </div>

            <DialogFooter className="pt-3 border-t">
              <Button type="button" variant="outline" onClick={() => onOpenChange(false)} disabled={isLoading}>
                Cancel
              </Button>
              <Button type="submit" disabled={isLoading} className="shadow-sm">
                {isLoading && <Loader2 className="mr-2 size-4 animate-spin" />}
                {isLoading ? "Saving..." : "Save Changes"}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}
