"use client";

import { useState, useMemo } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Loader2, Shield, Search, CheckSquare, Square, ChevronDown, ChevronRight, Lock } from "lucide-react";
import { toast } from "sonner";
import { useCreateRoleMutation } from "@/services/role.service";
import { useGetPermissionsQuery, type PermissionResponse } from "@/services/permission.service";
import { getApiErrorMessage } from "@/services/error-handler";
import { PREDEFINED_ROLES } from "@/config/roles";

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

const createRoleSchema = z.object({
  roleCode: z
    .string()
    .min(1, "Role code is required")
    .max(50, "Role code must not exceed 50 characters")
    .regex(/^[A-Z_]+$/, "Role code must contain only uppercase letters and underscores"),
  roleName: z
    .string()
    .min(1, "Role name is required")
    .max(100, "Role name must not exceed 100 characters"),
  description: z.string().max(500, "Description must not exceed 500 characters").optional(),
  permissionIds: z.array(z.number()),
});

type CreateRoleFormValues = z.infer<typeof createRoleSchema>;

interface CreateRoleDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function CreateRoleDialog({ open, onOpenChange }: CreateRoleDialogProps) {
  const [createRole, { isLoading }] = useCreateRoleMutation();
  const { data: permissionsData, isLoading: isPermissionsLoading } = useGetPermissionsQuery(
    { size: 500 },
    { skip: !open }
  );

  const [permissionSearch, setPermissionSearch] = useState("");
  const [collapsedGroups, setCollapsedGroups] = useState<Record<string, boolean>>({});

  const form = useForm<CreateRoleFormValues>({
    resolver: zodResolver(createRoleSchema),
    defaultValues: {
      roleCode: "",
      roleName: "",
      description: "",
      permissionIds: [],
    },
  });

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

  const onSubmit = async (values: CreateRoleFormValues) => {
    try {
      const payload = {
        roleCode: values.roleCode,
        roleName: values.roleName,
        description: values.description || undefined,
        permissionIds: values.permissionIds,
      };
      const result = await createRole(payload).unwrap();
      toast.success(`Role "${result.roleName}" created successfully!`, {
        description: `Code: ${result.roleCode} (${values.permissionIds.length} permissions assigned)`,
      });
      form.reset();
      onOpenChange(false);
    } catch (err) {
      toast.error("Failed to create role", {
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
              <DialogTitle className="text-xl font-bold">Add Custom Role</DialogTitle>
              <DialogDescription>
                Create a new role and grant precise module permissions for your organization.
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="flex flex-1 flex-col overflow-hidden space-y-4 pt-4">
            <div className="flex-1 overflow-y-auto pr-2 space-y-4">
              {/* Preset Selector */}
              <div className="space-y-1.5">
                <FormLabel className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
                  Quick Preset (Optional)
                </FormLabel>
                <select
                  onChange={(e) => {
                    const val = e.target.value;
                    if (!val) return;
                    const roleDef = PREDEFINED_ROLES.find((r) => r.code === val);
                    if (roleDef) {
                      form.setValue("roleCode", roleDef.code);
                      form.setValue("roleName", roleDef.name);
                      form.setValue("description", roleDef.description);
                    }
                  }}
                  className="h-9 w-full rounded-md border border-input bg-background px-3 py-1 text-sm shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                  disabled={isLoading}
                >
                  <option value="">Select standard ERP role template preset...</option>
                  {PREDEFINED_ROLES.map((role) => (
                    <option key={role.code} value={role.code}>
                      {role.name} ({role.code}) &mdash; {role.category}
                    </option>
                  ))}
                </select>
              </div>

              {/* Basic Details */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <FormField
                  control={form.control}
                  name="roleCode"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel className="font-semibold">Role Code *</FormLabel>
                      <FormControl>
                        <Input
                          placeholder="e.g. PROJECT_MANAGER"
                          disabled={isLoading}
                          {...field}
                          onChange={(e) => field.onChange(e.target.value.toUpperCase())}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

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
                      <Input placeholder="Detailed role responsibilities and access scope" disabled={isLoading} {...field} value={field.value ?? ""} />
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
                    <h3 className="font-bold text-sm text-foreground">Assign Permissions</h3>
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
                {isLoading ? "Creating Role..." : `Create Role (${selectedPermissionIds.length} Perms)`}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}
