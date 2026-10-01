"use client";

import { Loader2, AlertTriangle } from "lucide-react";
import { toast } from "sonner";
import { useDeleteItemMutation, type ItemListResponse } from "@/services/item.service";
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

interface DeleteItemDialogProps {
  item: ItemListResponse | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function DeleteItemDialog({ item, open, onOpenChange }: DeleteItemDialogProps) {
  const [deleteItem, { isLoading }] = useDeleteItemMutation();

  const handleDelete = async () => {
    if (!item) return;
    try {
      await deleteItem(item.id).unwrap();
      toast.success(`Item "${item.nameEn}" deleted successfully!`);
      onOpenChange(false);
    } catch (err) {
      toast.error("Failed to delete item", {
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
              <DialogTitle>Delete Item</DialogTitle>
              <DialogDescription className="mt-1">
                Are you sure you want to delete item{" "}
                <span className="font-semibold text-foreground">
                  {item?.nameEn} ({item?.sku})
                </span>
                ?
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        <p className="text-sm text-muted-foreground">
          This action will permanently remove the item. Purchase orders, stock movements
          and invoices that reference it will be affected. Consider deactivating the item
          instead if you only want to stop selling it.
        </p>

        <DialogFooter className="gap-2 sm:gap-0 pt-2">
          <Button variant="outline" onClick={() => onOpenChange(false)} disabled={isLoading}>
            Cancel
          </Button>
          <Button variant="destructive" onClick={handleDelete} disabled={isLoading}>
            {isLoading && <Loader2 className="mr-2 size-4 animate-spin" />}
            {isLoading ? "Deleting..." : "Delete Item"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
