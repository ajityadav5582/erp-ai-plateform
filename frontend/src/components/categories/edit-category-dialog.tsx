"use client";

import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Loader2, Tag } from "lucide-react";
import { toast } from "sonner";
import { useUpdateCategoryMutation, type Category } from "@/services/category.service";
import { getApiErrorMessage } from "@/services/error-handler";
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

const editCategorySchema = z.object({
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
  // The picker emits number | null so no coercion is needed here.
  parentId: z.number().int().positive().nullable(),
});

type EditCategoryFormValues = z.infer<typeof editCategorySchema>;

interface EditCategoryDialogProps {
  category: Category | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function EditCategoryDialog({ category, open, onOpenChange }: EditCategoryDialogProps) {
  const [updateCategory, { isLoading }] = useUpdateCategoryMutation();

  const form = useForm<EditCategoryFormValues>({
    resolver: zodResolver(editCategorySchema),
    defaultValues: {
      name: "",
      slug: "",
      description: "",
      parentId: null,
    },
  });

  useEffect(() => {
    if (category) {
      form.reset({
        name: category.name,
        slug: category.slug,
        description: category.description || "",
        parentId: category.parentId ?? null,
      });
    }
  }, [category, form]);

  const onSubmit = async (values: EditCategoryFormValues) => {
    if (!category) return;
    try {
      await updateCategory({
        categoryId: category.id,
        data: {
          ...values,
          description: values.description || undefined,
          // null is sent deliberately. An absent parentId means "leave unchanged",
          // an explicit null means "move to root". Normalising null to undefined
          // here would make it impossible to promote a child to a root category.
          parentId: values.parentId ?? null,
        },
      }).unwrap();
      toast.success(`Category "${values.name}" updated successfully!`);
      onOpenChange(false);
    } catch (err) {
      toast.error("Failed to update category", {
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
              <DialogTitle>Edit Category</DialogTitle>
              <DialogDescription>
                Update details for {category?.name} ({category?.slug})
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
                      // This category and everything beneath it are filtered out of the list,
                      // because the backend rejects them as circular references.
                      excludeCategoryId={category?.id}
                    />
                  </FormControl>
                  <FormMessage />
                  <p className="text-xs text-muted-foreground">
                    Pick &ldquo;No parent (root category)&rdquo; to make this a root category. This category
                    and its sub-categories are not listed.
                  </p>
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
