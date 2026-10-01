"use client";

import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Loader2, Truck } from "lucide-react";
import { toast } from "sonner";
import {
  useCreateSupplierMutation,
  SUPPLIER_TYPES,
  SUPPLIER_TYPE_LABELS,
} from "@/services/supplier.service";
import { getApiErrorMessage } from "@/services/error-handler";
import { useSelectedCompany } from "@/config/company-context";

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
 * Mirrors the bean validation on the backend `SupplierCreateRequest`: `name` and
 * `code` are required and length-capped, everything else is optional. Only the
 * constraints the backend actually enforces are repeated here so the form can
 * fail fast without duplicating server rules.
 */
const createSupplierSchema = z.object({
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
  // Kept as a string so the controlled <Input type="number"> stays simple;
  // it is parsed to a number on submit.
  paymentTermsDays: z
    .string()
    .regex(/^\d{1,3}$/, "Payment terms must be a whole number of days (0-365)")
    .refine((v) => Number(v) <= 365, "Payment terms must not exceed 365 days"),
  currency: z
    .string()
    .regex(/^$|^[A-Za-z]{3}$/, "Currency must be a 3-letter ISO code"),
});

type CreateSupplierFormValues = z.infer<typeof createSupplierSchema>;

interface CreateSupplierDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function CreateSupplierDialog({ open, onOpenChange }: CreateSupplierDialogProps) {
  const [createSupplier, { isLoading }] = useCreateSupplierMutation();
  const selectedCompany = useSelectedCompany();

  const form = useForm<CreateSupplierFormValues>({
    resolver: zodResolver(createSupplierSchema),
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

  // Reset the form whenever the dialog closes so stale input does not leak into
  // the next open cycle.
  useEffect(() => {
    if (!open) {
      form.reset();
    }
  }, [open, form]);

  const onSubmit = async (values: CreateSupplierFormValues) => {
    if (!selectedCompany) {
      toast.error("No company selected", {
        description: "Select a company from the header before creating a supplier.",
      });
      return;
    }

    try {
      // Empty strings are dropped so the backend falls back to its defaults
      // rather than persisting blank values. No companyId is sent: the backend
      // scopes the create to the company carried by the X-Company-Id header.
      const payload = {
        name: values.name,
        code: values.code,
        type: values.type,
        email: values.email || undefined,
        phone: values.phone || undefined,
        address: values.address || undefined,
        taxId: values.taxId || undefined,
        paymentTermsDays: values.paymentTermsDays === "" ? undefined : Number(values.paymentTermsDays),
        currency: values.currency || undefined,
      };

      const result = await createSupplier(payload).unwrap();
      toast.success(`Supplier "${result.name}" created successfully!`, {
        description: `Code: ${result.code}`,
      });
      form.reset();
      onOpenChange(false);
    } catch (err) {
      toast.error("Failed to create supplier", {
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
              <DialogTitle>Add New Supplier</DialogTitle>
              <DialogDescription>
                Create a new supplier. The supplier will be scoped to the company
                selected in the header.
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
              <Button type="submit" disabled={isLoading || !selectedCompany}>
                {isLoading && <Loader2 className="mr-2 size-4 animate-spin" />}
                {isLoading ? "Creating..." : "Create Supplier"}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}
