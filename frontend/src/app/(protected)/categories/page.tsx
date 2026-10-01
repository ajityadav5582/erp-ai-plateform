"use client";

import { useState } from "react";
import {
  Tag,
  Plus,
  Search,
  RefreshCw,
  MoreVertical,
  Eye,
  Pencil,
  Trash2,
  CheckCircle2,
  PauseCircle,
  ChevronLeft,
  ChevronRight,
  Filter,
} from "lucide-react";
import {
  useGetCategoriesQuery,
  useGetCategoryQuery,
  useActivateCategoryMutation,
  useDeactivateCategoryMutation,
  type CategoryListResponse,
} from "@/services/category.service";
import { CompanySelect } from "@/components/common/company-select";
import { CategoryStatusBadge } from "@/components/categories/category-status-badge";
import { CreateCategoryDialog } from "@/components/categories/create-category-dialog";
import { EditCategoryDialog } from "@/components/categories/edit-category-dialog";
import { CategoryDetailsSheet } from "@/components/categories/category-details-sheet";
import { DeleteCategoryDialog } from "@/components/categories/delete-category-dialog";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { toast } from "sonner";
import { getApiErrorMessage } from "@/services/error-handler";

export default function CategoriesPage() {
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState<boolean | undefined>(undefined);
  const [companyFilter, setCompanyFilter] = useState<number | undefined>(undefined);

  // Modals state
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [selectedCategoryIdForDetails, setSelectedCategoryIdForDetails] = useState<string | null>(null);
  const [selectedCategoryIdForEdit, setSelectedCategoryIdForEdit] = useState<string | null>(null);
  const [categoryToDelete, setCategoryToDelete] = useState<CategoryListResponse | null>(null);

  // Query categories list
  const { data, isLoading, isFetching, refetch } = useGetCategoriesQuery({
    page,
    size: pageSize,
    search: searchTerm.trim() || undefined,
    status: statusFilter,
    sort: "createdAt,desc",
  });

  // Fetch full category for edit modal
  const { data: categoryToEdit } = useGetCategoryQuery(selectedCategoryIdForEdit ?? "", {
    skip: !selectedCategoryIdForEdit,
  });

  const [activateCategory] = useActivateCategoryMutation();
  const [deactivateCategory] = useDeactivateCategoryMutation();

  const handleActivate = async (category: CategoryListResponse) => {
    try {
      await activateCategory(category.id).unwrap();
      toast.success(`Category "${category.name}" activated!`);
    } catch (err) {
      toast.error("Failed to activate category", { description: getApiErrorMessage(err) });
    }
  };

  const handleDeactivate = async (category: CategoryListResponse) => {
    try {
      await deactivateCategory(category.id).unwrap();
      toast.success(`Category "${category.name}" deactivated.`);
    } catch (err) {
      toast.error("Failed to deactivate category", { description: getApiErrorMessage(err) });
    }
  };

  const categories = data?.content ?? [];
  const totalElements = data?.totalElements ?? 0;
  const totalPages = data?.totalPages ?? 1;

  return (
    <div className="flex flex-1 flex-col space-y-6 p-4 sm:p-6 lg:p-8">
      {/* Top Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold tracking-tight text-foreground sm:text-3xl">
              Category Management
            </h1>
            <span className="inline-flex items-center rounded-full bg-primary/10 px-2.5 py-0.5 text-xs font-semibold text-primary">
              {totalElements} Categories
            </span>
          </div>
          <p className="mt-1 text-sm text-muted-foreground">
            Manage inventory categories, hierarchy, and operational status.
          </p>
        </div>

        <Button onClick={() => setIsCreateOpen(true)} className="gap-2 self-start sm:self-auto shadow-sm">
          <Plus className="size-4" />
          Add Category
        </Button>
      </div>

      {/* Filter Toolbar */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between rounded-xl border border-border bg-card p-3 shadow-sm">
        <div className="flex flex-1 items-center gap-3">
          {/* Search bar */}
          <div className="relative flex-1 max-w-sm">
            <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder="Search by name or slug..."
              value={searchTerm}
              onChange={(e) => {
                setSearchTerm(e.target.value);
                setPage(0);
              }}
              className="pl-9 h-9"
            />
          </div>

          {/* Status Filter */}
          <div className="flex items-center gap-1.5">
            <Filter className="size-4 text-muted-foreground hidden sm:block" />
            <select
              value={statusFilter === undefined ? "" : statusFilter.toString()}
              onChange={(e) => {
                const val = e.target.value;
                setStatusFilter(val === "" ? undefined : val === "true");
                setPage(0);
              }}
              className="h-9 rounded-md border border-input bg-background px-3 py-1 text-sm shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
            >
              <option value="">All Statuses</option>
              <option value="true">Active</option>
              <option value="false">Inactive</option>
            </select>
          </div>

          {/* Company Filter */}
          <div className="flex items-center gap-1.5">
            <CompanySelect
              value={companyFilter}
              onChange={(val) => {
                setCompanyFilter(val);
                setPage(0);
              }}
              placeholder="All Companies"
            />
          </div>
        </div>

        <Button
          variant="outline"
          size="sm"
          onClick={() => refetch()}
          disabled={isFetching}
          className="gap-1.5 h-9 self-end sm:self-auto"
        >
          <RefreshCw className={`size-3.5 ${isFetching ? "animate-spin" : ""}`} />
          Refresh
        </Button>
      </div>

      {/* Main Categories Table */}
      <div className="rounded-xl border border-border bg-card shadow-sm overflow-hidden">
        <Table>
          <TableHeader className="bg-muted/40">
            <TableRow>
              <TableHead>Category</TableHead>
              <TableHead>Slug</TableHead>
              <TableHead>Parent Category</TableHead>
              <TableHead>Status</TableHead>
              <TableHead>Created</TableHead>
              <TableHead className="text-right">Actions</TableHead>
            </TableRow>
          </TableHeader>

          <TableBody>
            {isLoading ? (
              Array.from({ length: 5 }).map((_, idx) => (
                <TableRow key={idx}>
                  <TableCell>
                    <div className="flex items-center gap-3">
                      <Skeleton className="size-9 rounded-lg" />
                      <div className="space-y-1">
                        <Skeleton className="h-4 w-32" />
                        <Skeleton className="h-3 w-24" />
                      </div>
                    </div>
                  </TableCell>
                  <TableCell><Skeleton className="h-4 w-24" /></TableCell>
                  <TableCell><Skeleton className="h-4 w-24" /></TableCell>
                  <TableCell><Skeleton className="h-5 w-20 rounded-full" /></TableCell>
                  <TableCell><Skeleton className="h-4 w-24" /></TableCell>
                  <TableCell className="text-right"><Skeleton className="size-8 rounded-md ml-auto" /></TableCell>
                </TableRow>
              ))
            ) : categories.length === 0 ? (
              <TableRow>
                <TableCell colSpan={6} className="h-64 text-center">
                  <div className="flex flex-col items-center justify-center gap-2">
                    <div className="flex size-12 items-center justify-center rounded-full bg-muted">
                      <Tag className="size-6 text-muted-foreground" />
                    </div>
                    <p className="text-base font-semibold text-foreground">No categories found</p>
                    <p className="text-sm text-muted-foreground max-w-sm">
                      {searchTerm || statusFilter !== undefined || companyFilter
                        ? "No categories match your active search or filters."
                        : "No categories created yet. Click 'Add Category' to create the first category."}
                    </p>
                    {(searchTerm || statusFilter !== undefined || companyFilter) && (
                      <Button
                        variant="link"
                        size="sm"
                        onClick={() => {
                          setSearchTerm("");
                          setStatusFilter(undefined);
                          setCompanyFilter(undefined);
                        }}
                      >
                        Clear Filters
                      </Button>
                    )}
                  </div>
                </TableCell>
              </TableRow>
            ) : (
              categories.map((category) => {
                const initials = category.name?.[0]?.toUpperCase() ?? "C";
                return (
                  <TableRow key={category.id} className="group transition-colors hover:bg-muted/50">
                    <TableCell>
                      <div className="flex items-center gap-3">
                        <Avatar className="size-9 border border-border">
                          <AvatarFallback className="bg-primary/10 text-xs font-bold text-primary">
                            {initials}
                          </AvatarFallback>
                        </Avatar>
                        <div className="min-w-0">
                          <p className="font-medium text-foreground truncate">{category.name}</p>
                          <p className="text-xs text-muted-foreground truncate">ID: {category.id}</p>
                        </div>
                      </div>
                    </TableCell>

                    <TableCell className="text-sm font-mono text-muted-foreground">
                      {category.slug}
                    </TableCell>

                    <TableCell className="text-sm text-foreground">
                      {category.parentName ?? "—"}
                    </TableCell>

                    <TableCell>
                      <CategoryStatusBadge isActive={category.isActive} />
                    </TableCell>

                    <TableCell className="text-xs text-muted-foreground">
                      {category.createdAt ? new Date(category.createdAt).toLocaleDateString() : "—"}
                    </TableCell>

                    <TableCell className="text-right">
                      <DropdownMenu modal>
                        <DropdownMenuTrigger asChild>
                          <Button variant="ghost" size="icon" className="size-8">
                            <MoreVertical className="size-4" />
                            <span className="sr-only">Open menu</span>
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end" className="z-[9999] w-48">
                          <DropdownMenuItem onClick={() => setSelectedCategoryIdForDetails(category.id.toString())}>
                            <Eye className="mr-2 size-4 text-muted-foreground" />
                            View Details
                          </DropdownMenuItem>

                          <DropdownMenuItem onClick={() => setSelectedCategoryIdForEdit(category.id.toString())}>
                            <Pencil className="mr-2 size-4 text-muted-foreground" />
                            Edit Category
                          </DropdownMenuItem>

                          <DropdownMenuSeparator />

                          {!category.isActive ? (
                            <DropdownMenuItem onClick={() => handleActivate(category)}>
                              <CheckCircle2 className="mr-2 size-4 text-emerald-600" />
                              Activate Category
                            </DropdownMenuItem>
                          ) : (
                            <DropdownMenuItem onClick={() => handleDeactivate(category)}>
                              <PauseCircle className="mr-2 size-4 text-amber-600" />
                              Deactivate Category
                            </DropdownMenuItem>
                          )}

                          <DropdownMenuSeparator />

                          <DropdownMenuItem
                            className="text-destructive focus:text-destructive"
                            onClick={() => setCategoryToDelete(category)}
                          >
                            <Trash2 className="mr-2 size-4" />
                            Delete Category
                          </DropdownMenuItem>
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </TableCell>
                  </TableRow>
                );
              })
            )}
          </TableBody>
        </Table>

        {/* Pagination Bar */}
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between border-t border-border px-4 py-3 bg-muted/20 text-sm">
          <div className="text-muted-foreground">
            Showing <span className="font-medium text-foreground">{categories.length}</span> of{" "}
            <span className="font-medium text-foreground">{totalElements}</span> total categories
          </div>

          <div className="flex items-center gap-4">
            <div className="flex items-center gap-2">
              <span className="text-xs text-muted-foreground">Per page:</span>
              <select
                value={pageSize}
                onChange={(e) => {
                  setPageSize(Number(e.target.value));
                  setPage(0);
                }}
                className="h-8 rounded border border-input bg-background px-2 py-0.5 text-xs shadow-sm"
              >
                <option value="5">5</option>
                <option value="10">10</option>
                <option value="20">20</option>
                <option value="50">50</option>
              </select>
            </div>

            <div className="flex items-center gap-1">
              <span className="text-xs text-muted-foreground mr-2">
                Page {page + 1} of {totalPages || 1}
              </span>
              <Button
                variant="outline"
                size="icon"
                className="size-8"
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0 || isLoading}
              >
                <ChevronLeft className="size-4" />
              </Button>
              <Button
                variant="outline"
                size="icon"
                className="size-8"
                onClick={() => setPage((p) => p + 1)}
                disabled={page >= totalPages - 1 || isLoading}
              >
                <ChevronRight className="size-4" />
              </Button>
            </div>
          </div>
        </div>
      </div>

      {/* Dialog Modals */}
      <CreateCategoryDialog open={isCreateOpen} onOpenChange={setIsCreateOpen} />

      <EditCategoryDialog
        category={categoryToEdit ?? null}
        open={!!selectedCategoryIdForEdit}
        onOpenChange={(open) => {
          if (!open) setSelectedCategoryIdForEdit(null);
        }}
      />

      <CategoryDetailsSheet
        categoryId={selectedCategoryIdForDetails}
        open={!!selectedCategoryIdForDetails}
        onOpenChange={(open) => {
          if (!open) setSelectedCategoryIdForDetails(null);
        }}
        onEdit={() => {
          const did = selectedCategoryIdForDetails;
          setSelectedCategoryIdForDetails(null);
          setSelectedCategoryIdForEdit(did);
        }}
        onDelete={() => {
          const target = categories.find((c) => c.id.toString() === selectedCategoryIdForDetails);
          setSelectedCategoryIdForDetails(null);
          if (target) setCategoryToDelete(target);
        }}
      />

      <DeleteCategoryDialog
        category={categoryToDelete}
        open={!!categoryToDelete}
        onOpenChange={(open) => {
          if (!open) setCategoryToDelete(null);
        }}
      />
    </div>
  );
}
