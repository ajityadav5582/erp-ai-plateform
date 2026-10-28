"use client";

import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Loader2, Package } from "lucide-react";
import { toast } from "sonner";
import { useUpdateItemMutation, type Item } from "@/services/item.service";
import { getApiErrorMessage } from "@/services/error-handler";
import {
  itemFormSchema,
  itemToFormValues,
  toUpdateItemDiff,
  type ItemFormValues,
} from "@/components/items/item-form";
import { ItemFormFields } from "@/components/items/item-form-fields";

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Form } from "@/components/ui/form";
import { Button } from "@/components/ui/button";

interface EditItemDialogProps {
  item: Item | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function EditItemDialog({ item, open, onOpenChange }: EditItemDialogProps) {
  const [updateItem, { isLoading }] = useUpdateItemMutation();

  const form = useForm<ItemFormValues>({
    resolver: zodResolver(itemFormSchema),
    defaultValues: item ? itemToFormValues(item) : undefined,
  });

  // Re-seed the form from the fetched item record each time it is opened.
  useEffect(() => {
    if (item) {
      form.reset(itemToFormValues(item));
    }
  }, [item, form]);

  const onSubmit = async (values: ItemFormValues) => {
    if (!item) return;

    const { payload, unsavable } = toUpdateItemDiff(values, item);

    if (Object.keys(payload).length === 0) {
      toast.info("No changes to save.");
      onOpenChange(false);
      return;
    }

    try {
      await updateItem({ itemId: item.id, data: payload }).unwrap();
      toast.success(`Item "${values.nameEn}" updated successfully!`);
      // The API has no way to clear an optional number or the category, so a
      // cleared field keeps its previous value. Say so rather than let the
      // user assume the edit landed.
      if (unsavable.length > 0) {
        toast.warning("Some cleared fields were not saved", {
          description: `The API cannot clear ${unsavable.join(", ")}. ${
            unsavable.length === 1 ? "It still has" : "They still have"
          } the previous value.`,
        });
      }
      onOpenChange(false);
    } catch (err) {
      toast.error("Failed to update item", {
        description: getApiErrorMessage(err),
      });
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-3xl">
        <DialogHeader>
          <div className="flex items-center gap-2">
            <div className="flex size-9 items-center justify-center rounded-lg bg-primary/10 text-primary">
              <Package className="size-5" />
            </div>
            <div>
              <DialogTitle>Edit Item</DialogTitle>
              <DialogDescription>
                Update details for {item?.nameEn} ({item?.sku})
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4 py-2">
            <ItemFormFields
              control={form.control}
              watch={form.watch}
              disabled={isLoading}
            />

            <DialogFooter className="pt-4">
              <Button
                type="button"
                variant="outline"
                onClick={() => onOpenChange(false)}
                disabled={isLoading}
              >
                Cancel
              </Button>
              <Button type="submit" disabled={isLoading}>
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
