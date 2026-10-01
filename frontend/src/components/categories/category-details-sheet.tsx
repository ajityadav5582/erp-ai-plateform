"use client";

import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetDescription,
} from "@/components/ui/sheet";
import { CategoryStatusBadge } from "./category-status-badge";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import {
  Tag,
  CheckCircle2,
  PauseCircle,
  Pencil,
  Trash2,
  Loader2,
  Building2,
  FolderTree,
} from "lucide-react";
import { toast } from "sonner";
import {
  useGetCategoryQuery,
  useActivateCategoryMutation,
  useDeactivateCategoryMutation,
} from "@/services/category.service";
import { getApiErrorMessage } from "@/services/error-handler";

interface CategoryDetailsSheetProps {
  categoryId: string | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onEdit: () => void;
  onDelete: () => void;
}

export function CategoryDetailsSheet({
  categoryId,
  open,
  onOpenChange,
  onEdit,
  onDelete,
}: CategoryDetailsSheetProps) {
  const { data: category, isLoading, isError } = useGetCategoryQuery(categoryId ?? "", {
    skip: !categoryId || !open,
  });

  const [activateCategory, { isLoading: isActivating }] = useActivateCategoryMutation();
  const [deactivateCategory, { isLoading: isDeactivating }] = useDeactivateCategoryMutation();

  const handleActivate = async () => {
    if (!category) return;
    try {
      await activateCategory(category.id).unwrap();
      toast.success(`Category "${category.name}" activated successfully!`);
    } catch (err) {
      toast.error("Failed to activate category", {
        description: getApiErrorMessage(err),
      });
    }
  };

  const handleDeactivate = async () => {
    if (!category) return;
    try {
      await deactivateCategory(category.id).unwrap();
      toast.success(`Category "${category.name}" deactivated.`);
    } catch (err) {
      toast.error("Failed to deactivate category", {
        description: getApiErrorMessage(err),
      });
    }
  };

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent className="sm:max-w-md overflow-y-auto">
        <SheetHeader className="pb-4">
          <SheetTitle>Category Details</SheetTitle>
          <SheetDescription>
            Comprehensive category information and metadata
          </SheetDescription>
        </SheetHeader>

        {isLoading && (
          <div className="flex h-64 items-center justify-center">
            <Loader2 className="size-8 animate-spin text-muted-foreground" />
          </div>
        )}

        {isError && (
          <div className="rounded-md bg-destructive/10 p-4 text-center text-sm text-destructive">
            Failed to load category details.
          </div>
        )}

        {category && (
          <div className="space-y-6">
            {/* Header profile card */}
            <div className="flex items-center gap-4 rounded-xl border border-border bg-card p-4 shadow-sm">
              <div className="flex size-14 items-center justify-center rounded-lg bg-primary/10 text-primary">
                <Tag className="size-7" />
              </div>
              <div className="min-w-0 flex-1">
                <h3 className="truncate text-lg font-semibold text-foreground">
                  {category.name}
                </h3>
                <p className="truncate text-sm text-muted-foreground font-mono">
                  {category.slug}
                </p>
                <div className="mt-2">
                  <CategoryStatusBadge isActive={category.isActive} />
                </div>
              </div>
            </div>

            {/* Actions Toolbar */}
            <div className="flex items-center gap-2">
              {!category.isActive ? (
                <Button
                  size="sm"
                  variant="outline"
                  className="flex-1 border-emerald-500/30 text-emerald-600 hover:bg-emerald-50 hover:text-emerald-700 dark:hover:bg-emerald-950/30"
                  onClick={handleActivate}
                  disabled={isActivating}
                >
                  {isActivating ? <Loader2 className="mr-1.5 size-4 animate-spin" /> : <CheckCircle2 className="mr-1.5 size-4" />}
                  Activate
                </Button>
              ) : (
                <Button
                  size="sm"
                  variant="outline"
                  className="flex-1 border-amber-500/30 text-amber-600 hover:bg-amber-50 hover:text-amber-700 dark:hover:bg-amber-950/30"
                  onClick={handleDeactivate}
                  disabled={isDeactivating}
                >
                  {isDeactivating ? <Loader2 className="mr-1.5 size-4 animate-spin" /> : <PauseCircle className="mr-1.5 size-4" />}
                  Deactivate
                </Button>
              )}

              <Button size="sm" variant="outline" onClick={onEdit}>
                <Pencil className="mr-1.5 size-4" />
                Edit
              </Button>

              <Button size="sm" variant="destructive" onClick={onDelete}>
                <Trash2 className="size-4" />
              </Button>
            </div>

            <Separator />

            {/* Category Info */}
            <div className="space-y-3 text-sm">
              {category.description && (
                <div className="flex items-start gap-3">
                  <Tag className="size-4 shrink-0 text-muted-foreground mt-0.5" />
                  <span className="text-foreground">{category.description}</span>
                </div>
              )}

              <div className="flex items-center gap-3">
                <Building2 className="size-4 shrink-0 text-muted-foreground" />
                <span className="text-foreground">Company ID: {category.companyId}</span>
              </div>

              {category.parentId && (
                <div className="flex items-center gap-3">
                  <FolderTree className="size-4 shrink-0 text-muted-foreground" />
                  <span className="text-foreground">Parent Category ID: {category.parentId}</span>
                </div>
              )}
            </div>

            <Separator />

            {/* Metadata & Timestamps */}
            <div className="space-y-2 rounded-lg bg-muted/50 p-3 text-xs text-muted-foreground">
              <div className="flex justify-between">
                <span>Category ID:</span>
                <span className="font-mono">{category.id}</span>
              </div>
              <div className="flex justify-between">
                <span>Tenant ID:</span>
                <span>{category.tenantId}</span>
              </div>
              <div className="flex justify-between">
                <span>Version:</span>
                <span>{category.version}</span>
              </div>
              {category.createdAt && (
                <div className="flex justify-between">
                  <span>Created At:</span>
                  <span>{new Date(category.createdAt).toLocaleDateString()}</span>
                </div>
              )}
              {category.updatedAt && (
                <div className="flex justify-between">
                  <span>Updated At:</span>
                  <span>{new Date(category.updatedAt).toLocaleDateString()}</span>
                </div>
              )}
            </div>
          </div>
        )}
      </SheetContent>
    </Sheet>
  );
}
