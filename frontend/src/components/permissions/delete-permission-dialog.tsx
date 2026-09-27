"use client";

import { useState } from "react";
import { Loader2, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { useDeletePermissionMutation } from "@/services/permission.service";
import { getApiErrorMessage } from "@/services/error-handler";
import type { PermissionResponse } from "@/services/permission.service";

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";

interface DeletePermissionDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  permission: PermissionResponse | null;
}

export function DeletePermissionDialog({ open, onOpenChange, permission }: DeletePermissionDialogProps) {
  const [deletePermission, { isLoading }] = useDeletePermissionMutation();
  const [reason, setReason] = useState("");

  if (!permission) return null;

  const permissionCode = permission.permissionCode;

  const handleDelete = async () => {
    try {
      await deletePermission(permission.id).unwrap();
      toast.success(`Permission "${permissionCode}" deleted successfully!`);
      setReason("");
      onOpenChange(false);
    } catch (err) {
      toast.error("Failed to delete permission", {
        description: getApiErrorMessage(err),
      });
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <div className="flex items-center gap-2">
            <div className="flex size-9 items-center justify-center rounded-lg bg-destructive/10 text-destructive">
              <Trash2 className="size-5" />
            </div>
            <DialogTitle>Delete Permission</DialogTitle>
          </div>
          <DialogDescription className="pt-2">
            Are you sure you want to delete the permission{" "}
            <span className="font-mono font-semibold">{permissionCode}</span>?
            {permission.description && (
              <span className="block mt-1 text-muted-foreground">
                Description: {permission.description}
              </span>
            )}
            <span className="block mt-2">
              This action cannot be undone. The permission will be permanently removed.
            </span>
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-2 py-2">
          <label htmlFor="reason" className="text-sm font-medium">
            Reason for deletion (optional)
          </label>
          <textarea
            id="reason"
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            placeholder="Enter reason for deleting this permission..."
            className="h-20 w-full rounded-md border border-input bg-background px-3 py-2 text-sm shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
            disabled={isLoading}
          />
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)} disabled={isLoading}>
            Cancel
          </Button>
          <Button
            variant="destructive"
            onClick={handleDelete}
            disabled={isLoading}
          >
            {isLoading && <Loader2 className="mr-2 size-4 animate-spin" />}
            {isLoading ? "Deleting..." : "Delete Permission"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
