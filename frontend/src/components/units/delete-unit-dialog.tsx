"use client";

import { Loader2, AlertTriangle } from "lucide-react";
import { toast } from "sonner";
import { useDeleteUnitMutation, type Unit } from "@/services/unit.service";
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

interface DeleteUnitDialogProps {
  unit: Unit | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function DeleteUnitDialog({ unit, open, onOpenChange }: DeleteUnitDialogProps) {
  const [deleteUnit, { isLoading }] = useDeleteUnitMutation();

  const handleDelete = async () => {
    if (!unit) return;
    try {
      await deleteUnit(unit.id).unwrap();
      toast.success(`Unit "${unit.name}" deleted successfully!`);
      onOpenChange(false);
    } catch (err) {
      toast.error("Failed to delete unit", {
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
              <DialogTitle>Delete Unit</DialogTitle>
              <DialogDescription className="mt-1">
                Are you sure you want to delete unit{" "}
                <span className="font-semibold text-foreground">
                  {unit?.name} ({unit?.code})
                </span>
                ?
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        <p className="text-sm text-muted-foreground">
          This action will permanently remove the unit. Products and stock movements that
          reference it will be affected. Consider deactivating the unit instead if you only
          want to stop using it.
        </p>

        <DialogFooter className="gap-2 sm:gap-0 pt-2">
          <Button variant="outline" onClick={() => onOpenChange(false)} disabled={isLoading}>
            Cancel
          </Button>
          <Button variant="destructive" onClick={handleDelete} disabled={isLoading}>
            {isLoading && <Loader2 className="mr-2 size-4 animate-spin" />}
            {isLoading ? "Deleting..." : "Delete Unit"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
