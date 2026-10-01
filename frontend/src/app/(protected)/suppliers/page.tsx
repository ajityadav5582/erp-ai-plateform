"use client";

import { useState } from "react";
import {
  Truck,
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
  useGetSuppliersQuery,
  useGetSupplierQuery,
  useActivateSupplierMutation,
  useDeactivateSupplierMutation,
  formatSupplierType,
  type SupplierListResponse,
} from "@/services/supplier.service";
import { CompanySelect } from "@/components/common/company-select";
import { SupplierStatusBadge } from "@/components/suppliers/supplier-status-badge";
import { CreateSupplierDialog } from "@/components/suppliers/create-supplier-dialog";
import { EditSupplierDialog } from "@/components/suppliers/edit-supplier-dialog";
import { SupplierDetailsSheet } from "@/components/suppliers/supplier-details-sheet";
import { DeleteSupplierDialog } from "@/components/suppliers/delete-supplier-dialog";
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

export default function SuppliersPage() {
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState<boolean | undefined>(undefined);
  const [companyFilter, setCompanyFilter] = useState<number | undefined>(undefined);

  // Modals state
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [selectedSupplierIdForDetails, setSelectedSupplierIdForDetails] = useState<string | null>(null);
  const [selectedSupplierIdForEdit, setSelectedSupplierIdForEdit] = useState<string | null>(null);
  const [supplierToDelete, setSupplierToDelete] = useState<SupplierListResponse | null>(null);

  // Query suppliers list
  const { data, isLoading, isFetching, refetch } = useGetSuppliersQuery({
    page,
    size: pageSize,
    search: searchTerm.trim() || undefined,
    status: statusFilter,
    sort: "createdAt,desc",
  });

  // Fetch full supplier for edit modal
  const { data: supplierToEdit } = useGetSupplierQuery(selectedSupplierIdForEdit ?? "", {
    skip: !selectedSupplierIdForEdit,
  });

  const [activateSupplier] = useActivateSupplierMutation();
  const [deactivateSupplier] = useDeactivateSupplierMutation();

  const handleActivate = async (supplier: SupplierListResponse) => {
    try {
      await activateSupplier(supplier.id).unwrap();
      toast.success(`Supplier "${supplier.name}" activated!`);
    } catch (err) {
      toast.error("Failed to activate supplier", { description: getApiErrorMessage(err) });
    }
  };

  const handleDeactivate = async (supplier: SupplierListResponse) => {
    try {
      await deactivateSupplier(supplier.id).unwrap();
      toast.success(`Supplier "${supplier.name}" deactivated.`);
    } catch (err) {
      toast.error("Failed to deactivate supplier", { description: getApiErrorMessage(err) });
    }
  };

  const suppliers = data?.content ?? [];
  const totalElements = data?.totalElements ?? 0;
  const totalPages = data?.totalPages ?? 1;

  return (
    <div className="flex flex-1 flex-col space-y-6 p-4 sm:p-6 lg:p-8">
      {/* Top Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold tracking-tight text-foreground sm:text-3xl">
              Supplier Management
            </h1>
            <span className="inline-flex items-center rounded-full bg-primary/10 px-2.5 py-0.5 text-xs font-semibold text-primary">
              {totalElements} Suppliers
            </span>
          </div>
          <p className="mt-1 text-sm text-muted-foreground">
            Manage inventory suppliers, contact details, and commercial terms.
          </p>
        </div>

        <Button onClick={() => setIsCreateOpen(true)} className="gap-2 self-start sm:self-auto shadow-sm">
          <Plus className="size-4" />
          Add Supplier
        </Button>
      </div>

      {/* Filter Toolbar */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between rounded-xl border border-border bg-card p-3 shadow-sm">
        <div className="flex flex-1 items-center gap-3">
          {/* Search bar */}
          <div className="relative flex-1 max-w-sm">
            <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder="Search by name or code..."
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

      {/* Main Suppliers Table */}
      <div className="rounded-xl border border-border bg-card shadow-sm overflow-hidden">
        <Table>
          <TableHeader className="bg-muted/40">
            <TableRow>
              <TableHead>Supplier</TableHead>
              <TableHead>Code</TableHead>
              <TableHead>Type</TableHead>
              <TableHead>Contact</TableHead>
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
                  <TableCell><Skeleton className="h-4 w-40" /></TableCell>
                  <TableCell><Skeleton className="h-5 w-20 rounded-full" /></TableCell>
                  <TableCell><Skeleton className="h-4 w-24" /></TableCell>
                  <TableCell className="text-right"><Skeleton className="size-8 rounded-md ml-auto" /></TableCell>
                </TableRow>
              ))
            ) : suppliers.length === 0 ? (
              <TableRow>
                <TableCell colSpan={7} className="h-64 text-center">
                  <div className="flex flex-col items-center justify-center gap-2">
                    <div className="flex size-12 items-center justify-center rounded-full bg-muted">
                      <Truck className="size-6 text-muted-foreground" />
                    </div>
                    <p className="text-base font-semibold text-foreground">No suppliers found</p>
                    <p className="text-sm text-muted-foreground max-w-sm">
                      {searchTerm || statusFilter !== undefined || companyFilter
                        ? "No suppliers match your active search or filters."
                        : "No suppliers created yet. Click 'Add Supplier' to create the first supplier."}
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
              suppliers.map((supplier) => {
                const initials = supplier.name?.[0]?.toUpperCase() ?? "S";
                return (
                  <TableRow key={supplier.id} className="group transition-colors hover:bg-muted/50">
                    <TableCell>
                      <div className="flex items-center gap-3">
                        <Avatar className="size-9 border border-border">
                          <AvatarFallback className="bg-primary/10 text-xs font-bold text-primary">
                            {initials}
                          </AvatarFallback>
                        </Avatar>
                        <div className="min-w-0">
                          <p className="font-medium text-foreground truncate">{supplier.name}</p>
                          <p className="text-xs text-muted-foreground truncate">ID: {supplier.id}</p>
                        </div>
                      </div>
                    </TableCell>

                    <TableCell className="text-sm font-mono text-muted-foreground">
                      {supplier.code}
                    </TableCell>

                    <TableCell className="text-sm text-foreground">
                      {formatSupplierType(supplier.type)}
                    </TableCell>

                    <TableCell className="text-sm text-muted-foreground">
                      <div className="min-w-0">
                        <p className="truncate text-foreground">{supplier.email ?? "—"}</p>
                        <p className="text-xs truncate">{supplier.phone ?? "—"}</p>
                      </div>
                    </TableCell>

                    <TableCell>
                      <SupplierStatusBadge isActive={supplier.isActive} />
                    </TableCell>

                    <TableCell className="text-xs text-muted-foreground">
                      {supplier.createdAt ? new Date(supplier.createdAt).toLocaleDateString() : "—"}
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
                          <DropdownMenuItem onClick={() => setSelectedSupplierIdForDetails(supplier.id.toString())}>
                            <Eye className="mr-2 size-4 text-muted-foreground" />
                            View Details
                          </DropdownMenuItem>

                          <DropdownMenuItem onClick={() => setSelectedSupplierIdForEdit(supplier.id.toString())}>
                            <Pencil className="mr-2 size-4 text-muted-foreground" />
                            Edit Supplier
                          </DropdownMenuItem>

                          <DropdownMenuSeparator />

                          {!supplier.isActive ? (
                            <DropdownMenuItem onClick={() => handleActivate(supplier)}>
                              <CheckCircle2 className="mr-2 size-4 text-emerald-600" />
                              Activate Supplier
                            </DropdownMenuItem>
                          ) : (
                            <DropdownMenuItem onClick={() => handleDeactivate(supplier)}>
                              <PauseCircle className="mr-2 size-4 text-amber-600" />
                              Deactivate Supplier
                            </DropdownMenuItem>
                          )}

                          <DropdownMenuSeparator />

                          <DropdownMenuItem
                            className="text-destructive focus:text-destructive"
                            onClick={() => setSupplierToDelete(supplier)}
                          >
                            <Trash2 className="mr-2 size-4" />
                            Delete Supplier
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
            Showing <span className="font-medium text-foreground">{suppliers.length}</span> of{" "}
            <span className="font-medium text-foreground">{totalElements}</span> total suppliers
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
      <CreateSupplierDialog open={isCreateOpen} onOpenChange={setIsCreateOpen} />

      <EditSupplierDialog
        supplier={supplierToEdit ?? null}
        open={!!selectedSupplierIdForEdit}
        onOpenChange={(open) => {
          if (!open) setSelectedSupplierIdForEdit(null);
        }}
      />

      <SupplierDetailsSheet
        supplierId={selectedSupplierIdForDetails}
        open={!!selectedSupplierIdForDetails}
        onOpenChange={(open) => {
          if (!open) setSelectedSupplierIdForDetails(null);
        }}
        onEdit={() => {
          const sid = selectedSupplierIdForDetails;
          setSelectedSupplierIdForDetails(null);
          setSelectedSupplierIdForEdit(sid);
        }}
        onDelete={() => {
          const target = suppliers.find((s) => s.id.toString() === selectedSupplierIdForDetails);
          setSelectedSupplierIdForDetails(null);
          if (target) setSupplierToDelete(target);
        }}
      />

      <DeleteSupplierDialog
        supplier={supplierToDelete}
        open={!!supplierToDelete}
        onOpenChange={(open) => {
          if (!open) setSupplierToDelete(null);
        }}
      />
    </div>
  );
}
