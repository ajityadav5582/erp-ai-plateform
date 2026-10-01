"use client";

import { useState } from "react";
import {
  Package,
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
} from "lucide-react";
import {
  useGetItemsQuery,
  useGetItemQuery,
  useActivateItemMutation,
  useDeactivateItemMutation,
  formatTaxabilityType,
  type ItemListResponse,
} from "@/services/item.service";
import { ItemStatusBadge } from "@/components/items/item-status-badge";
import { CreateItemDialog } from "@/components/items/create-item-dialog";
import { EditItemDialog } from "@/components/items/edit-item-dialog";
import { ItemDetailsSheet } from "@/components/items/item-details-sheet";
import { DeleteItemDialog } from "@/components/items/delete-item-dialog";
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

export default function ItemsPage() {
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [searchTerm, setSearchTerm] = useState("");

  // Modals state
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [selectedItemIdForDetails, setSelectedItemIdForDetails] = useState<string | null>(null);
  const [selectedItemIdForEdit, setSelectedItemIdForEdit] = useState<string | null>(null);
  const [itemToDelete, setItemToDelete] = useState<ItemListResponse | null>(null);

  // The service routes to /items/search when a term is present and to /items
  // otherwise, because the backend rejects a blank or overlong search term.
  const { data, isLoading, isFetching, refetch } = useGetItemsQuery({
    page,
    size: pageSize,
    search: searchTerm.trim() || undefined,
    sort: "nameEn,asc",
  });

  // Fetch full item for edit modal
  const { data: itemToEdit } = useGetItemQuery(selectedItemIdForEdit ?? "", {
    skip: !selectedItemIdForEdit,
  });

  const [activateItem] = useActivateItemMutation();
  const [deactivateItem] = useDeactivateItemMutation();

  const handleActivate = async (item: ItemListResponse) => {
    try {
      await activateItem(item.id).unwrap();
      toast.success(`Item "${item.nameEn}" activated!`);
    } catch (err) {
      toast.error("Failed to activate item", { description: getApiErrorMessage(err) });
    }
  };

  const handleDeactivate = async (item: ItemListResponse) => {
    try {
      await deactivateItem(item.id).unwrap();
      toast.success(`Item "${item.nameEn}" deactivated.`);
    } catch (err) {
      toast.error("Failed to deactivate item", { description: getApiErrorMessage(err) });
    }
  };

  const items = data?.content ?? [];
  const totalElements = data?.totalElements ?? 0;
  const totalPages = data?.totalPages ?? 1;

  return (
    <div className="flex flex-1 flex-col space-y-6 p-4 sm:p-6 lg:p-8">
      {/* Top Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold tracking-tight text-foreground sm:text-3xl">
              Item Management
            </h1>
            <span className="inline-flex items-center rounded-full bg-primary/10 px-2.5 py-0.5 text-xs font-semibold text-primary">
              {totalElements} Items
            </span>
          </div>
          <p className="mt-1 text-sm text-muted-foreground">
            Manage the goods and services your company sells, buys and stocks.
          </p>
        </div>

        <Button
          onClick={() => setIsCreateOpen(true)}
          className="gap-2 self-start shadow-sm sm:self-auto"
        >
          <Plus className="size-4" />
          Add Item
        </Button>
      </div>

      {/* Filter Toolbar */}
      <div className="flex flex-col gap-3 rounded-xl border border-border bg-card p-3 shadow-sm sm:flex-row sm:items-center sm:justify-between">
        <div className="flex flex-1 items-center gap-3">
          <div className="relative max-w-sm flex-1">
            <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder="Search by name, SKU or HS code..."
              value={searchTerm}
              onChange={(e) => {
                setSearchTerm(e.target.value);
                setPage(0);
              }}
              className="h-9 pl-9"
            />
          </div>
        </div>

        <Button
          variant="outline"
          size="sm"
          onClick={() => refetch()}
          disabled={isFetching}
          className="h-9 gap-1.5 self-end sm:self-auto"
        >
          <RefreshCw className={`size-3.5 ${isFetching ? "animate-spin" : ""}`} />
          Refresh
        </Button>
      </div>

      {/* Main Items Table */}
      <div className="overflow-hidden rounded-xl border border-border bg-card shadow-sm">
        <Table>
          <TableHeader className="bg-muted/40">
            <TableRow>
              <TableHead>Item</TableHead>
              <TableHead>SKU</TableHead>
              <TableHead>Category</TableHead>
              <TableHead>UOM</TableHead>
              <TableHead className="text-right">Selling Price</TableHead>
              <TableHead>VAT</TableHead>
              <TableHead>Status</TableHead>
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
                  <TableCell><Skeleton className="h-4 w-20" /></TableCell>
                  <TableCell><Skeleton className="h-4 w-24" /></TableCell>
                  <TableCell><Skeleton className="h-4 w-12" /></TableCell>
                  <TableCell><Skeleton className="h-4 w-20" /></TableCell>
                  <TableCell><Skeleton className="h-4 w-16" /></TableCell>
                  <TableCell><Skeleton className="h-5 w-20 rounded-full" /></TableCell>
                  <TableCell className="text-right">
                    <Skeleton className="ml-auto size-8 rounded-md" />
                  </TableCell>
                </TableRow>
              ))
            ) : items.length === 0 ? (
              <TableRow>
                <TableCell colSpan={8} className="h-64 text-center">
                  <div className="flex flex-col items-center justify-center gap-2">
                    <div className="flex size-12 items-center justify-center rounded-full bg-muted">
                      <Package className="size-6 text-muted-foreground" />
                    </div>
                    <p className="text-base font-semibold text-foreground">No items found</p>
                    <p className="max-w-sm text-sm text-muted-foreground">
                      {searchTerm
                        ? "No items match your search."
                        : "No items created yet. Click 'Add Item' to create the first one."}
                    </p>
                    {searchTerm && (
                      <Button variant="link" size="sm" onClick={() => setSearchTerm("")}>
                        Clear Search
                      </Button>
                    )}
                  </div>
                </TableCell>
              </TableRow>
            ) : (
              items.map((item) => {
                const initials = item.nameEn?.[0]?.toUpperCase() ?? "I";
                return (
                  <TableRow key={item.id} className="group transition-colors hover:bg-muted/50">
                    <TableCell>
                      <div className="flex items-center gap-3">
                        <Avatar className="size-9 border border-border">
                          <AvatarFallback className="bg-primary/10 text-xs font-bold text-primary">
                            {initials}
                          </AvatarFallback>
                        </Avatar>
                        <div className="min-w-0">
                          <p className="truncate font-medium text-foreground">{item.nameEn}</p>
                          <p className="truncate text-xs text-muted-foreground">
                            {item.nameNp ?? `ID: ${item.id}`}
                          </p>
                        </div>
                      </div>
                    </TableCell>

                    <TableCell className="font-mono text-sm text-muted-foreground">
                      {item.sku}
                    </TableCell>

                    <TableCell className="text-sm text-foreground">
                      {item.categoryName ?? "—"}
                    </TableCell>

                    <TableCell className="text-sm text-muted-foreground">
                      {item.uomCode ?? "—"}
                    </TableCell>

                    <TableCell className="text-right text-sm font-medium text-foreground">
                      {item.sellingPrice?.toLocaleString()}
                    </TableCell>

                    <TableCell className="text-sm text-muted-foreground">
                      {item.taxabilityType
                        ? `${formatTaxabilityType(item.taxabilityType)}${
                            item.vatRate !== null && Number(item.vatRate) > 0
                              ? ` ${Number(item.vatRate)}%`
                              : ""
                          }`
                        : "—"}
                    </TableCell>

                    <TableCell>
                      <ItemStatusBadge isActive={item.isActive} />
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
                          <DropdownMenuItem
                            onClick={() => setSelectedItemIdForDetails(item.id.toString())}
                          >
                            <Eye className="mr-2 size-4 text-muted-foreground" />
                            View Details
                          </DropdownMenuItem>

                          <DropdownMenuItem
                            onClick={() => setSelectedItemIdForEdit(item.id.toString())}
                          >
                            <Pencil className="mr-2 size-4 text-muted-foreground" />
                            Edit Item
                          </DropdownMenuItem>

                          <DropdownMenuSeparator />

                          {!item.isActive ? (
                            <DropdownMenuItem onClick={() => handleActivate(item)}>
                              <CheckCircle2 className="mr-2 size-4 text-emerald-600" />
                              Activate Item
                            </DropdownMenuItem>
                          ) : (
                            <DropdownMenuItem onClick={() => handleDeactivate(item)}>
                              <PauseCircle className="mr-2 size-4 text-amber-600" />
                              Deactivate Item
                            </DropdownMenuItem>
                          )}

                          <DropdownMenuSeparator />

                          <DropdownMenuItem
                            className="text-destructive focus:text-destructive"
                            onClick={() => setItemToDelete(item)}
                          >
                            <Trash2 className="mr-2 size-4" />
                            Delete Item
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
        <div className="flex flex-col gap-3 border-t border-border bg-muted/20 px-4 py-3 text-sm sm:flex-row sm:items-center sm:justify-between">
          <div className="text-muted-foreground">
            Showing <span className="font-medium text-foreground">{items.length}</span> of{" "}
            <span className="font-medium text-foreground">{totalElements}</span> total items
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
              <span className="mr-2 text-xs text-muted-foreground">
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
      <CreateItemDialog open={isCreateOpen} onOpenChange={setIsCreateOpen} />

      <EditItemDialog
        item={itemToEdit ?? null}
        open={!!selectedItemIdForEdit}
        onOpenChange={(open) => {
          if (!open) setSelectedItemIdForEdit(null);
        }}
      />

      <ItemDetailsSheet
        itemId={selectedItemIdForDetails}
        open={!!selectedItemIdForDetails}
        onOpenChange={(open) => {
          if (!open) setSelectedItemIdForDetails(null);
        }}
        onEdit={() => {
          const iid = selectedItemIdForDetails;
          setSelectedItemIdForDetails(null);
          setSelectedItemIdForEdit(iid);
        }}
        onDelete={() => {
          const target = items.find((i) => i.id.toString() === selectedItemIdForDetails);
          setSelectedItemIdForDetails(null);
          if (target) setItemToDelete(target);
        }}
      />

      <DeleteItemDialog
        item={itemToDelete}
        open={!!itemToDelete}
        onOpenChange={(open) => {
          if (!open) setItemToDelete(null);
        }}
      />
    </div>
  );
}
