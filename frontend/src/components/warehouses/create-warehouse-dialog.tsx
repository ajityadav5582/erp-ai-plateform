"use client";

import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Loader2, Warehouse as WarehouseIcon } from "lucide-react";
import { toast } from "sonner";
import {
  useCreateWarehouseMutation,
  WAREHOUSE_TYPES,
  WAREHOUSE_TYPE_LABELS,
} from "@/services/warehouse.service";
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
import { BranchSelect } from "@/components/common/branch-select";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

/**
 * Mirrors the bean validation on the backend `CreateWarehouseRequest`: `code`,
 * `name` and `type` are required and length-capped, everything else optional.
 * Only the constraints the backend actually enforces are repeated here so the
 * form can fail fast without duplicating server rules.
 *
 * `branchId` is held as a string because `BranchSelect` reports "no selection"
 * by handing back `undefined`. Round-tripping through a string keeps the empty
 * state representable in the form and converts cleanly on submit, instead of
 * treating `undefined` as "never set" the way a plain number field would.
 */
const createWarehouseSchema = z.object({
  code: z
    .string()
    .trim()
    .min(1, "Warehouse code is required")
    .max(64, "Warehouse code must not exceed 64 characters"),
  name: z
    .string()
    .trim()
    .min(1, "Warehouse name is required")
    .max(255, "Warehouse name must not exceed 255 characters"),
  type: z.enum(WAREHOUSE_TYPES),
  /** Empty string means company-wide; a digits-only string is a branch id. */
  branchId: z
    .string()
    .regex(/^$|^\d+$/, "Select a branch from the list")
    .optional()
    .or(z.literal("")),
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

type CreateWarehouseFormValues = z.infer<typeof createWarehouseSchema>;

interface CreateWarehouseDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function CreateWarehouseDialog({
  open,
  onOpenChange,
}: CreateWarehouseDialogProps) {
  const [createWarehouse, { isLoading }] = useCreateWarehouseMutation();
  const selectedCompany = useSelectedCompany();

  const form = useForm<CreateWarehouseFormValues>({
    resolver: zodResolver(createWarehouseSchema),
    defaultValues: {
      code: "",
      name: "",
      type: "GENERAL",
      branchId: "",
      address: "",
      phone: "",
    },
  });

  // Reset the form whenever the dialog closes so stale input does not leak into
  // the next open cycle.
  useEffect(() => {
    if (!open) {
      form.reset();
    }
  }, [open, form]);

  const onSubmit = async (values: CreateWarehouseFormValues) => {
    if (!selectedCompany) {
      toast.error("No company selected", {
        description:
          "Select a company from the header before creating a warehouse.",
      });
      return;
    }

    try {
      // Empty strings are dropped so the backend stores null rather than blank
      // values. No companyId is sent: the backend scopes the create to the
      // company carried by the X-Company-Id header.
      const payload = {
        code: values.code,
        name: values.name,
        type: values.type,
        branchId: values.branchId ? Number(values.branchId) : null,
        address: values.address || undefined,
        phone: values.phone || undefined,
      };

      const result = await createWarehouse(payload).unwrap();
      toast.success(`Warehouse "${result.name}" created successfully!`, {
        description: `Code: ${result.code}`,
      });
      form.reset();
      onOpenChange(false);
    } catch (err) {
      toast.error("Failed to create warehouse", {
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
              <DialogTitle>Add New Warehouse</DialogTitle>
              <DialogDescription>
                Add a physical storage location. The warehouse will be scoped to
                the company selected in the header.
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
              name="branchId"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Branch</FormLabel>
                  <FormControl>
                    <BranchSelect
                      value={
                        field.value ? Number(field.value) : undefined
                      }
                      onChange={(value) =>
                        field.onChange(value ? String(value) : "")
                      }
                      placeholder="Company-wide (no branch)"
                      disabled={isLoading}
                    />
                  </FormControl>
                  <p className="text-xs text-muted-foreground">
                    The branch whose approval chain governs documents raised
                    against this warehouse. Leave unset for a site shared across
                    the whole company.
                  </p>
                  <FormMessage />
                </FormItem>
              )}
            />

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
              Stated rather than left implicit: creating a warehouse adds to the
              network and does not move where goods land. Without this, users
              reasonably assume the site they just added becomes the default.
            */}
            <div className="rounded-lg border border-border bg-muted/40 p-3 text-xs text-muted-foreground">
              Creating a warehouse does not make it the default. The company
              keeps whichever warehouse is already marked default &mdash; use
              &ldquo;Set as default&rdquo; on the list to change where goods
              with no explicit destination are received.
            </div>

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
                {isLoading ? "Creating..." : "Create Warehouse"}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}
