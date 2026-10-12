"use client";

import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Loader2, Warehouse as WarehouseIcon } from "lucide-react";
import { toast } from "sonner";
import {
  useUpdateWarehouseMutation,
  WAREHOUSE_TYPES,
  WAREHOUSE_TYPE_LABELS,
  type Warehouse,
} from "@/services/warehouse.service";
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

/**
 * Mirrors the backend `UpdateWarehouseRequest`, where every field is optional and
 * length-capped. The form always sends the full set of editable fields, so a
 * cleared text input is sent as an explicit null to clear the stored value.
 *
 * `branchId` is absent from both the schema and the form, matching the backend.
 * The backend deliberately has no `branchId` on a partial update because it
 * cannot distinguish "leave the branch alone" from "clear the branch"; exposing
 * it here would let the UI claim a capability the API does not have.
 */
const editWarehouseSchema = z.object({
  name: z
    .string()
    .trim()
    .min(1, "Warehouse name is required")
    .max(255, "Warehouse name must not exceed 255 characters"),
  code: z
    .string()
    .trim()
    .min(1, "Warehouse code is required")
    .max(64, "Warehouse code must not exceed 64 characters"),
  type: z.enum(WAREHOUSE_TYPES),
  address: z
    .string()
    .max(500, "Warehouse address must not exceed 500 characters")
    .optional()
    .or(z.literal("")),
  phone: z
    .string()
    .max(32, "Phone must not exceed 32 characters")
    .optional()
    .or(z.literal("")),
});

type EditWarehouseFormValues = z.infer<typeof editWarehouseSchema>;

interface EditWarehouseDialogProps {
  warehouse: Warehouse | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function EditWarehouseDialog({
  warehouse,
  open,
  onOpenChange,
}: EditWarehouseDialogProps) {
  const [updateWarehouse, { isLoading }] = useUpdateWarehouseMutation();

  const form = useForm<EditWarehouseFormValues>({
    resolver: zodResolver(editWarehouseSchema),
    defaultValues: {
      name: "",
      code: "",
      type: "GENERAL",
      address: "",
      phone: "",
    },
  });

  // Re-seed the form from the fetched warehouse record each time it is opened.
  useEffect(() => {
    if (warehouse) {
      form.reset({
        name: warehouse.name,
        code: warehouse.code,
        type: warehouse.type ?? "GENERAL",
        address: warehouse.address ?? "",
        phone: warehouse.phone ?? "",
      });
    }
  }, [warehouse, form]);

  const onSubmit = async (values: EditWarehouseFormValues) => {
    if (!warehouse) return;

    try {
      await updateWarehouse({
        warehouseId: warehouse.id,
        data: {
          name: values.name,
          code: values.code,
          type: values.type,
          // Empty strings become explicit nulls so the backend clears the field
          // rather than silently keeping the previous value.
          address: values.address || null,
          phone: values.phone || null,
        },
      }).unwrap();
      toast.success(`Warehouse "${values.name}" updated successfully!`);
      onOpenChange(false);
    } catch (err) {
      toast.error("Failed to update warehouse", {
        description: getApiErrorMessage(err),
      });
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-2xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <div className="flex items-center gap-2">
            <div className="flex size-9 items-center justify-center rounded-lg bg-primary/10 text-primary">
              <WarehouseIcon className="size-5" />
            </div>
            <div>
              <DialogTitle>Edit Warehouse</DialogTitle>
              <DialogDescription>
                Update details for {warehouse?.name} ({warehouse?.code})
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4 py-2">
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <FormField
                control={form.control}
                name="name"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Warehouse Name *</FormLabel>
                    <FormControl>
                      <Input
                        placeholder="Kathmandu Central Depot"
                        disabled={isLoading}
                        {...field}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="code"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Warehouse Code *</FormLabel>
                    <FormControl>
                      <Input
                        placeholder="KTM-01"
                        disabled={isLoading}
                        {...field}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="type"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Warehouse Type *</FormLabel>
                    <Select
                      value={field.value}
                      onValueChange={field.onChange}
                      disabled={isLoading}
                    >
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="Select type" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        {WAREHOUSE_TYPES.map((type) => (
                          <SelectItem key={type} value={type}>
                            {WAREHOUSE_TYPE_LABELS[type]}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="phone"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Phone</FormLabel>
                    <FormControl>
                      <Input
                        placeholder="+977 1 555 0100"
                        disabled={isLoading}
                        {...field}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            <FormField
              control={form.control}
              name="address"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Address</FormLabel>
                  <FormControl>
                    <Input
                      placeholder="Street, city, country"
                      disabled={isLoading}
                      {...field}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            {/*
              The branch is intentionally not editable here, and saying so is
              better than omitting the field silently: users look for it here
              first and assume the association does not exist.
            */}
            {warehouse?.branchId != null && (
              <div className="rounded-lg border border-border bg-muted/40 p-3 text-xs text-muted-foreground">
                This warehouse is attached to branch #{warehouse.branchId}.
                Branch assignment cannot be changed from this form &mdash; the
                backend treats detaching as its own explicit operation, so a
                routine rename cannot silently move a site out from under the
                approval chain that governs it.
              </div>
            )}

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
