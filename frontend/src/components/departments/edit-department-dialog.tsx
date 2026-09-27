"use client";

import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Loader2, FolderTree } from "lucide-react";
import { toast } from "sonner";
import { useUpdateDepartmentMutation, type Department } from "@/services/department.service";
import { getApiErrorMessage } from "@/services/error-handler";
import { BranchSelect } from "@/components/common/branch-select";

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

const editDepartmentSchema = z.object({
  departmentCode: z
    .string()
    .min(1, "Department code is required")
    .max(50, "Department code must not exceed 50 characters"),
  departmentName: z
    .string()
    .min(1, "Department name is required")
    .max(200, "Department name must not exceed 200 characters"),
  description: z
    .string()
    .max(1000, "Description must not exceed 1000 characters")
    .optional()
    .or(z.literal("")),
  branchId: z.coerce.number().min(1, "Branch ID is required"),
  managerId: z.coerce.number().optional(),
  parentDepartmentId: z.coerce.number().optional(),
});

type EditDepartmentFormValues = z.infer<typeof editDepartmentSchema>;

interface EditDepartmentDialogProps {
  department: Department | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function EditDepartmentDialog({ department, open, onOpenChange }: EditDepartmentDialogProps) {
  const [updateDepartment, { isLoading }] = useUpdateDepartmentMutation();

  const form = useForm<EditDepartmentFormValues>({
    resolver: zodResolver(editDepartmentSchema),
    defaultValues: {
      departmentCode: "",
      departmentName: "",
      description: "",
      branchId: 0,
      managerId: undefined,
      parentDepartmentId: undefined,
    },
  });

  useEffect(() => {
    if (department) {
      form.reset({
        departmentCode: department.departmentCode,
        departmentName: department.departmentName,
        description: department.description || "",
        branchId: department.branchId,
        managerId: department.managerId ?? undefined,
        parentDepartmentId: department.parentDepartmentId ?? undefined,
      });
    }
  }, [department, form]);

  const onSubmit = async (values: EditDepartmentFormValues) => {
    if (!department) return;
    try {
      const payload = {
        ...values,
        description: values.description || undefined,
        managerId: values.managerId || undefined,
        parentDepartmentId: values.parentDepartmentId || undefined,
      };
      const result = await updateDepartment({
        departmentId: department.departmentId,
        data: payload,
      }).unwrap();
      toast.success(`Department "${result.departmentName}" updated successfully!`);
      onOpenChange(false);
    } catch (err) {
      toast.error("Failed to update department", {
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
              <FolderTree className="size-5" />
            </div>
            <div>
              <DialogTitle>Edit Department</DialogTitle>
              <DialogDescription>
                Update details for {department?.departmentName} ({department?.departmentCode})
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4 py-2">
            <div className="grid grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="departmentCode"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Department Code *</FormLabel>
                    <FormControl>
                      <Input placeholder="ENG-01" disabled={isLoading} {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="departmentName"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Department Name *</FormLabel>
                    <FormControl>
                      <Input placeholder="Engineering" disabled={isLoading} {...field} />
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
                    <Input placeholder="Department description" disabled={isLoading} {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <div className="grid grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="branchId"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Branch *</FormLabel>
                    <FormControl>
                      <BranchSelect
                        value={field.value || undefined}
                        onChange={(val) => field.onChange(val ?? 0)}
                        placeholder="Select branch"
                        disabled={isLoading}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="managerId"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Manager ID</FormLabel>
                    <FormControl>
                      <Input type="number" placeholder="1" disabled={isLoading} {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            <FormField
              control={form.control}
              name="parentDepartmentId"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Parent Department ID</FormLabel>
                  <FormControl>
                    <Input type="number" placeholder="Leave empty for root department" disabled={isLoading} {...field} />
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
