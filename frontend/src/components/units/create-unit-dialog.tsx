"use client";

import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Loader2, Ruler } from "lucide-react";
import { toast } from "sonner";
import {
  useCreateUnitMutation,
  UNIT_DIMENSIONS,
  UNIT_DIMENSION_LABELS,
} from "@/services/unit.service";
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
 * Mirrors the bean validation on the backend `UnitCreateRequest`.
 *
 * The code pattern is duplicated deliberately: catching "KG!" here produces an
 * inline field message, whereas letting it reach the API produces a toast the
 * user has to read after the fact.
 */
const createUnitSchema = z.object({
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
  // Kept as a string so the controlled <Input type="number"> stays simple;
  // parsed to a number on submit. "0" is valid and meaningful for counts.
  decimalScale: z
    .string()
    .regex(/^\d{1}$/, "Enter a whole number of decimal places (0-6)")
    .refine((v) => Number(v) <= 6, "Decimal scale must not exceed 6 digits"),
});

type CreateUnitFormValues = z.infer<typeof createUnitSchema>;

interface CreateUnitDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function CreateUnitDialog({ open, onOpenChange }: CreateUnitDialogProps) {
  const [createUnit, { isLoading }] = useCreateUnitMutation();
  const selectedCompany = useSelectedCompany();

  const form = useForm<CreateUnitFormValues>({
    resolver: zodResolver(createUnitSchema),
    defaultValues: {
      name: "",
      code: "",
      // No dimension default: the dimension is the one field where guessing
      // wrong produces silently wrong inventory arithmetic later, so it must
      // be an explicit choice.
      dimension: undefined as unknown as CreateUnitFormValues["dimension"],
      symbol: "",
      decimalScale: "0",
    },
  });

  // Reset the form whenever the dialog closes so stale input does not leak into
  // the next open cycle.
  useEffect(() => {
    if (!open) {
      form.reset();
    }
  }, [open, form]);

  const onSubmit = async (values: CreateUnitFormValues) => {
    if (!selectedCompany) {
      toast.error("No company selected", {
        description: "Select a company from the header before creating a unit.",
      });
      return;
    }

    try {
      // No companyId in the body: the backend scopes the create to the company
      // carried by the X-Company-Id header.
      const result = await createUnit({
        name: values.name,
        code: values.code,
        dimension: values.dimension,
        symbol: values.symbol || undefined,
        decimalScale: Number(values.decimalScale),
      }).unwrap();

      toast.success(`Unit "${result.name}" created successfully!`, {
        description: `Code: ${result.code}`,
      });
      form.reset();
      onOpenChange(false);
    } catch (err) {
      toast.error("Failed to create unit", {
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
              <DialogTitle>Add New Unit</DialogTitle>
              <DialogDescription>
                Create a unit of measure. The unit will be scoped to the company
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
                    The quantity this unit measures. Units of different dimensions can
                    never be added to or compared against each other.
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
                    <p className="text-xs text-muted-foreground">
                      How many decimal places a quantity in this unit may carry. Use 0 for
                      pieces and boxes, 2 or 3 for kilograms and litres.
                    </p>
                  </FormItem>
                )}
              />
            </div>

            <DialogFooter className="pt-4">
              <Button type="button" variant="outline" onClick={() => onOpenChange(false)} disabled={isLoading}>
                Cancel
              </Button>
              <Button type="submit" disabled={isLoading || !selectedCompany}>
                {isLoading && <Loader2 className="mr-2 size-4 animate-spin" />}
                {isLoading ? "Creating..." : "Create Unit"}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}
