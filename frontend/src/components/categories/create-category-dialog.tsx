"use client";

import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Loader2, Tag } from "lucide-react";
import { toast } from "sonner";
import { useCreateCategoryMutation } from "@/services/category.service";
import { getApiErrorMessage } from "@/services/error-handler";
import { useSelectedCompany } from "@/config/company-context";
import { ParentCategoryPicker } from "@/components/categories/parent-category-picker";

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

const createCategorySchema = z.object({
  name: z
    .string()
    .min(1, "Category name is required")
    .max(255, "Category name must not exceed 255 characters"),
  slug: z
    .string()
    .min(1, "Category slug is required")
    .max(255, "Category slug must not exceed 255 characters")
    .regex(/^[a-z0-9-]+$/, "Category slug must contain only lowercase letters, numbers, and hyphens"),
  description: z
    .string()
    .max(65535, "Description must not exceed 65535 characters")
    .optional()
    .or(z.literal("")),
  // The picker emits number | null rather than a free-text value, so this is a
  // plain optional number with no coercion. "No parent" is null, not "".
  parentId: z.number().int().positive().nullable().optional(),
});

type CreateCategoryFormValues = z.infer<typeof createCategorySchema>;

interface CreateCategoryDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function CreateCategoryDialog({ open, onOpenChange }: CreateCategoryDialogProps) {
  const [createCategory, { isLoading }] = useCreateCategoryMutation();
  const selectedCompany = useSelectedCompany();

  const form = useForm<CreateCategoryFormValues>({
    resolver: zodResolver(createCategorySchema),
    defaultValues: {
      name: "",
      slug: "",
      description: "",
      parentId: null,
    },
  });

  // Reset the form whenever the dialog is closed so stale input does not leak
  // into the next open cycle.
  useEffect(() => {
    if (!open) {
      form.reset();
    }
  }, [open, form]);

  const onSubmit = async (values: CreateCategoryFormValues) => {
    if (!selectedCompany) {
      toast.error("No company selected", {
        description: "Select a company from the header before creating a category.",
      });
      return;
    }

    try {
      const payload = {
        ...values,
        description: values.description || undefined,
        parentId: values.parentId ?? undefined,
        // No companyId in the body: the backend scopes the create to the
        // company carried by the X-Company-Id header, which the interceptor
        // sets from the header-selected company.
      };
      const result = await createCategory(payload).unwrap();
      toast.success(`Category "${result.name}" created successfully!`, {
        description: `Slug: ${result.slug}`,
      });
      form.reset();
      onOpenChange(false);
    } catch (err) {
      toast.error("Failed to create category", {
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
              <Tag className="size-5" />
            </div>
            <div>
              <DialogTitle>Add New Category</DialogTitle>
              <DialogDescription>
                Create a new category for your inventory. The category will be
                scoped to the company selected in the header.
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4 py-2">
            <div className="grid grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="name"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Category Name *</FormLabel>
                    <FormControl>
                      <Input placeholder="Electronics" disabled={isLoading} {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="slug"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Category Slug *</FormLabel>
                    <FormControl>
                      <Input placeholder="electronics" disabled={isLoading} {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            <FormField
              control={form.control}
              name="description"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Description</FormLabel>
                  <FormControl>
                    <Input placeholder="Category description" disabled={isLoading} {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="parentId"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Parent Category</FormLabel>
                  <FormControl>
                    <ParentCategoryPicker
                      value={field.value ?? null}
                      onChange={field.onChange}
                      disabled={isLoading}
                    />
                  </FormControl>
                  <FormMessage />
                  <p className="text-xs text-muted-foreground">
                    Pick from the categories that already exist in this company.
                    Leave as &ldquo;No parent&rdquo; to create a root category.
                  </p>
                </FormItem>
              )}
            />

            <DialogFooter className="pt-4">
              <Button type="button" variant="outline" onClick={() => onOpenChange(false)} disabled={isLoading}>
                Cancel
              </Button>
              <Button type="submit" disabled={isLoading || !selectedCompany}>
                {isLoading && <Loader2 className="mr-2 size-4 animate-spin" />}
                {isLoading ? "Creating..." : "Create Category"}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}
