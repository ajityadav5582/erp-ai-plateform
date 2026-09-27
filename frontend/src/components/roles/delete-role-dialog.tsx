"use client";

import { Loader2, AlertTriangle } from "lucide-react";
import { toast } from "sonner";
import { useDeleteRoleMutation, type RoleListResponse } from "@/services/role.service";
import { getApiErrorMessage } from "@/services/error-handler";

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";

interface DeleteRoleDialogProps {
  role: RoleListResponse | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function DeleteRoleDialog({ role, open, onOpenChange }: DeleteRoleDialogProps) {
  const [deleteRole, { isLoading }] = useDeleteRoleMutation();

  const handleDelete = async () => {
    if (!role) return;
    try {
      await deleteRole(role.id).unwrap();
      toast.success(`Role "${role.roleName}" deleted successfully!`);
      onOpenChange(false);
    } catch (err) {
      toast.error("Failed to delete role", {
        description: getApiErrorMessage(err),
      });
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <div className="flex items-center gap-3 text-destructive">
            <div className="flex size-10 items-center justify-center rounded-full bg-destructive/10">
              <AlertTriangle className="size-5" />
            </div>
            <div>
              <DialogTitle>Delete Role</DialogTitle>
              <DialogDescription className="mt-1">
                Are you sure you want to delete role{" "}
                <span className="font-semibold text-foreground">
                  {role?.roleName} ({role?.roleCode})
                </span>
                ?
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        <p className="text-sm text-muted-foreground">
          This action will deactivate the role. Users assigned to this role may lose access. This action cannot be undone.
        </p>

        <DialogFooter className="gap-2 sm:gap-0 pt-2">
          <Button variant="outline" onClick={() => onOpenChange(false)} disabled={isLoading}>
            Cancel
          </Button>
          <Button variant="destructive" onClick={handleDelete} disabled={isLoading}>
            {isLoading && <Loader2 className="mr-2 size-4 animate-spin" />}
            {isLoading ? "Deleting..." : "Delete Role"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
