"use client";

import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Loader2, Ruler } from "lucide-react";
import { toast } from "sonner";
import {
  useUpdateUnitMutation,
  UNIT_DIMENSIONS,
  UNIT_DIMENSION_LABELS,
  type Unit,
} from "@/services/unit.service";
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

/** Mirrors the backend `UnitUpdateRequest`; all fields are optional there too. */
const editUnitSchema = z.object({
  name: z
    .string()
    .min(1, "Unit name is required")
    .max(100, "Unit name must not exceed 100 characters"),
  code: z
    .string()
    .min(1, "Unit code is required")
    .max(32, "Unit code must not exceed 32 characters")
    .regex(
      /^[A-Za-z0-9][A-Za-z0-9_\-/]*$/,
      "Code may contain only letters, digits, spaces, and the characters _ - /"
    ),
  dimension: z.enum(UNIT_DIMENSIONS, {
    errorMap: () => ({ message: "Select the quantity this unit measures" }),
  }),
  symbol: z
    .string()
    .max(16, "Symbol must not exceed 16 characters")
    .optional()
    .or(z.literal("")),
  decimalScale: z
    .string()
    .regex(/^\d{1}$/, "Enter a whole number of decimal places (0-6)")
    .refine((v) => Number(v) <= 6, "Decimal scale must not exceed 6 digits"),
});

type EditUnitFormValues = z.infer<typeof editUnitSchema>;

interface EditUnitDialogProps {
  unit: Unit | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function EditUnitDialog({ unit, open, onOpenChange }: EditUnitDialogProps) {
  const [updateUnit, { isLoading }] = useUpdateUnitMutation();

  const form = useForm<EditUnitFormValues>({
    resolver: zodResolver(editUnitSchema),
    defaultValues: {
      name: "",
      code: "",
      dimension: undefined as unknown as EditUnitFormValues["dimension"],
      symbol: "",
      decimalScale: "0",
    },
  });

  // Re-seed the form from the fetched unit record each time it is opened.
  useEffect(() => {
    if (unit) {
      form.reset({
        name: unit.name,
        code: unit.code,
        dimension: unit.dimension,
        symbol: unit.symbol ?? "",
        decimalScale: String(unit.decimalScale ?? 0),
      });
    }
  }, [unit, form]);

  const onSubmit = async (values: EditUnitFormValues) => {
    if (!unit) return;

    try {
      await updateUnit({
        unitId: unit.id,
        data: {
          name: values.name,
          code: values.code,
          dimension: values.dimension,
          // An empty symbol is sent as an explicit null so the backend clears
          // the stored value rather than keeping the previous one.
          symbol: values.symbol || null,
          decimalScale: Number(values.decimalScale),
        },
      }).unwrap();
      toast.success(`Unit "${values.name}" updated successfully!`);
      onOpenChange(false);
    } catch (err) {
      toast.error("Failed to update unit", {
        description: getApiErrorMessage(err),
      });
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-xl">
        <DialogHeader>
          <div className="flex items-center gap-2">
            <div className="flex size-9 items-center justify-center rounded-lg bg-primary/10 text-primary">
              <Ruler className="size-5" />
            </div>
            <div>
              <DialogTitle>Edit Unit</DialogTitle>
              <DialogDescription>
                Update details for {unit?.name} ({unit?.code})
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
                    <FormLabel>Unit Name *</FormLabel>
                    <FormControl>
                      <Input placeholder="Kilogram" disabled={isLoading} {...field} />
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
                    <FormLabel>Unit Code *</FormLabel>
                    <FormControl>
                      <Input placeholder="KG" disabled={isLoading} {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            <FormField
              control={form.control}
              name="dimension"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Dimension *</FormLabel>
                  <Select
                    value={field.value ?? ""}
                    onValueChange={field.onChange}
                    disabled={isLoading}
                  >
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder="Select dimension" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      {UNIT_DIMENSIONS.map((dimension) => (
                        <SelectItem key={dimension} value={dimension}>
                          {UNIT_DIMENSION_LABELS[dimension]}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <FormMessage />
                  <p className="text-xs text-muted-foreground">
                    Changing the dimension reclassifies this unit. Existing stock recorded
                    against it should be reviewed before you do.
                  </p>
                </FormItem>
              )}
            />

            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <FormField
                control={form.control}
                name="symbol"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Symbol</FormLabel>
                    <FormControl>
                      <Input placeholder="kg" disabled={isLoading} {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="decimalScale"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Decimal Places *</FormLabel>
                    <FormControl>
                      <Input
                        type="number"
                        min={0}
                        max={6}
                        step={1}
                        placeholder="0"
                        disabled={isLoading}
                        {...field}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

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
