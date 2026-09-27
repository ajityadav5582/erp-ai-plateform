"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Loader2, Key } from "lucide-react";
import { toast } from "sonner";
import { useUpdatePermissionMutation } from "@/services/permission.service";
import { getApiErrorMessage } from "@/services/error-handler";
import type { PermissionResponse } from "@/services/permission.service";

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

const editPermissionSchema = z.object({
  description: z
    .string()
    .max(255, "Description must not exceed 255 characters")
    .optional()
    .or(z.literal("")),
});

type EditPermissionFormValues = z.infer<typeof editPermissionSchema>;

interface EditPermissionDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  permission: PermissionResponse | null;
}

export function EditPermissionDialog({ open, onOpenChange, permission }: EditPermissionDialogProps) {
  const [updatePermission, { isLoading }] = useUpdatePermissionMutation();

  const form = useForm<EditPermissionFormValues>({
    resolver: zodResolver(editPermissionSchema),
    defaultValues: {
      description: permission?.description || "",
    },
  });

  const onSubmit = async (values: EditPermissionFormValues) => {
    if (!permission) return;
    try {
      await updatePermission({
        permissionId: permission.id,
        data: {
          description: values.description || undefined,
        },
      }).unwrap();
      toast.success(`Permission "${permission.permissionCode}" updated successfully!`);
      onOpenChange(false);
    } catch (err) {
      toast.error("Failed to update permission", {
        description: getApiErrorMessage(err),
      });
    }
  };

  if (!permission) return null;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-xl">
        <DialogHeader>
          <div className="flex items-center gap-2">
            <div className="flex size-9 items-center justify-center rounded-lg bg-primary/10 text-primary">
              <Key className="size-5" />
            </div>
            <div>
              <DialogTitle>Edit Permission</DialogTitle>
              <DialogDescription>
                Update permission description.
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4 py-2">
            <div className="rounded-md border p-3 bg-muted/50">
              <p className="text-sm font-medium">Permission Code</p>
              <p className="font-mono text-sm text-muted-foreground">{permission.permissionCode}</p>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="rounded-md border p-3">
                <p className="text-sm font-medium">Resource</p>
                <p className="text-sm text-muted-foreground">{permission.resource}</p>
              </div>
              <div className="rounded-md border p-3">
                <p className="text-sm font-medium">Action</p>
                <p className="text-sm text-muted-foreground">{permission.action}</p>
              </div>
            </div>

            <FormField
              control={form.control}
              name="description"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Description</FormLabel>
                  <FormControl>
                    <Input placeholder="Permission description" disabled={isLoading} {...field} />
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
