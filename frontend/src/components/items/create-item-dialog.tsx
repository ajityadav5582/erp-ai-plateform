"use client";

import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Loader2, Package } from "lucide-react";
import { toast } from "sonner";
import { useCreateItemMutation } from "@/services/item.service";
import { getApiErrorMessage } from "@/services/error-handler";
import { useSelectedCompany } from "@/config/company-context";
import {
  itemFormDefaults,
  itemFormSchema,
  toCreateItemRequest,
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

interface CreateItemDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function CreateItemDialog({ open, onOpenChange }: CreateItemDialogProps) {
  const [createItem, { isLoading }] = useCreateItemMutation();
  const selectedCompany = useSelectedCompany();

  const form = useForm<ItemFormValues>({
    resolver: zodResolver(itemFormSchema),
    defaultValues: itemFormDefaults,
  });

  // Reset whenever the dialog closes so stale input does not leak into the next
  // open cycle.
  useEffect(() => {
    if (!open) {
      form.reset(itemFormDefaults);
    }
  }, [open, form]);

  const onSubmit = async (values: ItemFormValues) => {
    if (!selectedCompany) {
      toast.error("No company selected", {
        description: "Select a company from the header before creating an item.",
      });
      return;
    }

    try {
      // No companyId in the body: the backend scopes the create to the company
      // carried by the X-Company-Id header.
      const result = await createItem(toCreateItemRequest(values)).unwrap();

      toast.success(`Item "${result.nameEn}" created successfully!`, {
        description: `SKU: ${result.sku}`,
      });
      form.reset(itemFormDefaults);
      onOpenChange(false);
    } catch (err) {
      toast.error("Failed to create item", {
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
              <DialogTitle>Add New Item</DialogTitle>
              <DialogDescription>
                Create a goods or service item. The item will be scoped to the company
                selected in the header.
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
              <Button type="submit" disabled={isLoading || !selectedCompany}>
                {isLoading && <Loader2 className="mr-2 size-4 animate-spin" />}
                {isLoading ? "Creating..." : "Create Item"}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}
