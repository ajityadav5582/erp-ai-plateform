"use client";

import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Loader2, Truck } from "lucide-react";
import { toast } from "sonner";
import {
  useUpdateSupplierMutation,
  SUPPLIER_TYPES,
  SUPPLIER_TYPE_LABELS,
  type Supplier,
} from "@/services/supplier.service";
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
 * Mirrors the backend `SupplierUpdateRequest`, where every field is optional and
 * length-capped. The form always sends the full set of editable fields, so a
 * cleared text input is sent as an explicit null to clear the stored value.
 */
const editSupplierSchema = z.object({
  name: z
    .string()
    .min(1, "Supplier name is required")
    .max(255, "Supplier name must not exceed 255 characters"),
  code: z
    .string()
    .min(1, "Supplier code is required")
    .max(64, "Supplier code must not exceed 64 characters"),
  type: z.enum(SUPPLIER_TYPES).optional(),
  email: z
    .string()
    .email("Enter a valid email address")
    .max(255, "Email must not exceed 255 characters")
    .optional()
    .or(z.literal("")),
  phone: z
    .string()
    .max(32, "Phone must not exceed 32 characters")
    .optional()
    .or(z.literal("")),
  address: z
    .string()
    .max(65535, "Address must not exceed 65535 characters")
    .optional()
    .or(z.literal("")),
  taxId: z
    .string()
    .max(64, "Tax ID must not exceed 64 characters")
    .optional()
    .or(z.literal("")),
  paymentTermsDays: z
    .string()
    .regex(/^\d{1,3}$/, "Payment terms must be a whole number of days (0-365)")
    .refine((v) => Number(v) <= 365, "Payment terms must not exceed 365 days"),
  currency: z
    .string()
    .regex(/^$|^[A-Za-z]{3}$/, "Currency must be a 3-letter ISO code"),
});

type EditSupplierFormValues = z.infer<typeof editSupplierSchema>;

interface EditSupplierDialogProps {
  supplier: Supplier | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function EditSupplierDialog({ supplier, open, onOpenChange }: EditSupplierDialogProps) {
  const [updateSupplier, { isLoading }] = useUpdateSupplierMutation();

  const form = useForm<EditSupplierFormValues>({
    resolver: zodResolver(editSupplierSchema),
    defaultValues: {
      name: "",
      code: "",
      type: undefined,
      email: "",
      phone: "",
      address: "",
      taxId: "",
      paymentTermsDays: "",
      currency: "",
    },
  });

  // Re-seed the form from the fetched supplier record each time it is opened.
  useEffect(() => {
    if (supplier) {
      form.reset({
        name: supplier.name,
        code: supplier.code,
        type: supplier.type ?? undefined,
        email: supplier.email ?? "",
        phone: supplier.phone ?? "",
        address: supplier.address ?? "",
        taxId: supplier.taxId ?? "",
        paymentTermsDays:
          supplier.paymentTermsDays !== null && supplier.paymentTermsDays !== undefined
            ? String(supplier.paymentTermsDays)
            : "",
        currency: supplier.currency ?? "",
      });
    }
  }, [supplier, form]);

  const onSubmit = async (values: EditSupplierFormValues) => {
    if (!supplier) return;

    try {
      await updateSupplier({
        supplierId: supplier.id,
        data: {
          name: values.name,
          code: values.code,
          type: values.type,
          // Empty strings become explicit nulls so the backend clears the field
          // rather than silently keeping the previous value.
          email: values.email || null,
          phone: values.phone || null,
          address: values.address || null,
          taxId: values.taxId || null,
          paymentTermsDays: values.paymentTermsDays === "" ? null : Number(values.paymentTermsDays),
          currency: values.currency || null,
        },
      }).unwrap();
      toast.success(`Supplier "${values.name}" updated successfully!`);
      onOpenChange(false);
    } catch (err) {
      toast.error("Failed to update supplier", {
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
              <Truck className="size-5" />
            </div>
            <div>
              <DialogTitle>Edit Supplier</DialogTitle>
              <DialogDescription>
                Update details for {supplier?.name} ({supplier?.code})
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
                    <FormLabel>Supplier Name *</FormLabel>
                    <FormControl>
                      <Input placeholder="Acme Industrial Supplies" disabled={isLoading} {...field} />
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
                    <FormLabel>Supplier Code *</FormLabel>
                    <FormControl>
                      <Input placeholder="SUP-001" disabled={isLoading} {...field} />
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
                    <FormLabel>Supplier Type</FormLabel>
                    <Select
                      value={field.value ?? ""}
                      onValueChange={(value) => field.onChange(value || undefined)}
                      disabled={isLoading}
                    >
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="Select type" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        {SUPPLIER_TYPES.map((type) => (
                          <SelectItem key={type} value={type}>
                            {SUPPLIER_TYPE_LABELS[type]}
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
                name="currency"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Currency</FormLabel>
                    <FormControl>
                      <Input placeholder="USD" disabled={isLoading} {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="email"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Email</FormLabel>
                    <FormControl>
                      <Input type="email" placeholder="purchasing@acme.com" disabled={isLoading} {...field} />
                    </FormControl>
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
                      <Input placeholder="+1 555 0100" disabled={isLoading} {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="taxId"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Tax ID</FormLabel>
                    <FormControl>
                      <Input placeholder="TAX-123456" disabled={isLoading} {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="paymentTermsDays"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Payment Terms (days)</FormLabel>
                    <FormControl>
                      <Input
                        type="number"
                        min={0}
                        max={365}
                        step={1}
                        placeholder="30"
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
                    <Input placeholder="Street, city, country" disabled={isLoading} {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <DialogFooter className="pt-4">
              <Button type="button" variant="outline" onClick={() => onOpenChange(false)} disabled={isLoading}>
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
