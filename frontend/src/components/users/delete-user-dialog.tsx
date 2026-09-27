"use client";

import { Loader2, AlertTriangle } from "lucide-react";
import { toast } from "sonner";
import { useDeleteUserMutation, type UserListResponse } from "@/services/user.service";
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

interface DeleteUserDialogProps {
  user: UserListResponse | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function DeleteUserDialog({ user, open, onOpenChange }: DeleteUserDialogProps) {
  const [deleteUser, { isLoading }] = useDeleteUserMutation();

  const handleDelete = async () => {
    if (!user) return;
    try {
      await deleteUser(user.userId).unwrap();
      toast.success(`User @${user.username} deleted successfully!`);
      onOpenChange(false);
    } catch (err) {
      toast.error("Failed to delete user", {
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
              <DialogTitle>Delete User Account</DialogTitle>
              <DialogDescription className="mt-1">
                Are you sure you want to delete user{" "}
                <span className="font-semibold text-foreground">
                  {user?.fullName} (@{user?.username})
                </span>
                ?
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        <p className="text-sm text-muted-foreground">
          This action will archive the user account and revoke all active sessions. The user will no longer be able to log in to the ERP platform.
        </p>

        <DialogFooter className="gap-2 sm:gap-0 pt-2">
          <Button variant="outline" onClick={() => onOpenChange(false)} disabled={isLoading}>
            Cancel
          </Button>
          <Button variant="destructive" onClick={handleDelete} disabled={isLoading}>
            {isLoading && <Loader2 className="mr-2 size-4 animate-spin" />}
            {isLoading ? "Deleting..." : "Delete User"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
