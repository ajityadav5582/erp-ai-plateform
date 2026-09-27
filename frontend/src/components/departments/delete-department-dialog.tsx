"use client";

import { Loader2, AlertTriangle } from "lucide-react";
import { toast } from "sonner";
import { useDeleteDepartmentMutation, type DepartmentListResponse } from "@/services/department.service";
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

interface DeleteDepartmentDialogProps {
  department: DepartmentListResponse | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function DeleteDepartmentDialog({ department, open, onOpenChange }: DeleteDepartmentDialogProps) {
  const [deleteDepartment, { isLoading }] = useDeleteDepartmentMutation();

  const handleDelete = async () => {
    if (!department) return;
    try {
      await deleteDepartment(department.departmentId).unwrap();
      toast.success(`Department "${department.departmentName}" deleted successfully!`);
      onOpenChange(false);
    } catch (err) {
      toast.error("Failed to delete department", {
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
              <DialogTitle>Delete Department</DialogTitle>
              <DialogDescription className="mt-1">
                Are you sure you want to delete department{" "}
                <span className="font-semibold text-foreground">
                  {department?.departmentName} ({department?.departmentCode})
                </span>
                ?
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        <p className="text-sm text-muted-foreground">
          This action will permanently remove the department. Child departments and user assignments may be affected. This action cannot be undone.
        </p>

        <DialogFooter className="gap-2 sm:gap-0 pt-2">
          <Button variant="outline" onClick={() => onOpenChange(false)} disabled={isLoading}>
            Cancel
          </Button>
          <Button variant="destructive" onClick={handleDelete} disabled={isLoading}>
            {isLoading && <Loader2 className="mr-2 size-4 animate-spin" />}
            {isLoading ? "Deleting..." : "Delete Department"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
