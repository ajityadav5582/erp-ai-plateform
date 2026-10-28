"use client";

import { useMemo, useState } from "react";
import {
  Warehouse as WarehouseIcon,
  Plus,
  Search,
  RefreshCw,
  MoreVertical,
  Eye,
  Pencil,
  CheckCircle2,
  PauseCircle,
  ChevronLeft,
  ChevronRight,
  Filter,
  Star,
} from "lucide-react";
import {
  useGetWarehousesQuery,
  useGetWarehouseQuery,
  useActivateWarehouseMutation,
  useDeactivateWarehouseMutation,
  useSetDefaultWarehouseMutation,
  formatWarehouseType,
  type Warehouse,
} from "@/services/warehouse.service";
import { WarehouseStatusBadge } from "@/components/warehouses/warehouse-status-badge";
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
import dynamic from "next/dynamic";

/**
 * Code-split modals. These components stay closed for the whole time this
 * list is on screen, so keeping them out of the initial payload lets the page
 * become interactive without parsing them. `loading: () => null` matches the
 * closed state, so opening one causes no layout shift.
 */
const CreateWarehouseDialog = dynamic(
  () =>
    import("@/components/warehouses/create-warehouse-dialog").then((mod) => ({
      default: mod.CreateWarehouseDialog,
    })),
  { ssr: false, loading: () => null }
);
const EditWarehouseDialog = dynamic(
  () =>
    import("@/components/warehouses/edit-warehouse-dialog").then((mod) => ({
      default: mod.EditWarehouseDialog,
    })),
  { ssr: false, loading: () => null }
);
const WarehouseDetailsSheet = dynamic(
  () =>
    import("@/components/warehouses/warehouse-details-sheet").then((mod) => ({
      default: mod.WarehouseDetailsSheet,
    })),
  { ssr: false, loading: () => null }
);

/**
 * Stable empty array for the `?? []` fallback.
 *
 * A literal `[]` allocates a new array on every render, which changes the
 * identity of the value the filtering memo depends on and defeats it entirely -
 * the memo would re-run on every parent render regardless of its inputs.
 */
const NO_WAREHOUSES: Warehouse[] = [];

export default function WarehousesPage() {
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState<boolean | undefined>(undefined);

  // Modals state
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [selectedWarehouseIdForDetails, setSelectedWarehouseIdForDetails] =
    useState<number | null>(null);
  const [selectedWarehouseIdForEdit, setSelectedWarehouseIdForEdit] =
    useState<number | null>(null);

  // Query warehouses list
  const { data, isLoading, isFetching, refetch } = useGetWarehousesQuery({
    page,
    size: pageSize,
    sort: "name,asc",
  });

  // Fetch full warehouse for edit modal
  const { data: warehouseToEdit } = useGetWarehouseQuery(
    selectedWarehouseIdForEdit ?? 0,
    { skip: !selectedWarehouseIdForEdit }
  );

  const [activateWarehouse] = useActivateWarehouseMutation();
  const [deactivateWarehouse] = useDeactivateWarehouseMutation();
  const [setDefaultWarehouse] = useSetDefaultWarehouseMutation();

  /**
   * Search and status filtering happen here, not on the server.
   *
   * The backend signature is `listWarehouses(Pageable pageable)`, so `search`
   * and `status` sent as query params are bound away and silently ignored - a
   * request carrying them returns 200 with unfiltered rows. Filtering the
   * returned page is the only honest option until the endpoint accepts filters;
   * the tradeoff (a match on another page is not found) is surfaced below.
   */
  const term = searchTerm.trim().toLowerCase();
  const { allWarehouses, warehouses } = useMemo(() => {
    const all = data?.content ?? NO_WAREHOUSES;
    const visible = all.filter((warehouse) => {
      if (statusFilter !== undefined && warehouse.isActive !== statusFilter) {
        return false;
      }
      if (!term) return true;
      return (
        warehouse.name.toLowerCase().includes(term) ||
        warehouse.code.toLowerCase().includes(term)
      );
    });
    return { allWarehouses: all, warehouses: visible };
  }, [data?.content, statusFilter, term]);

  /**
   * Whether a filter is hiding matches that exist on a later page.
   *
   * Reported rather than hidden: a silently truncated result set looks exactly
   * like a warehouse that does not exist, which is the wrong conclusion for
   * someone deciding where to receive goods.
   */
  const isFilteredPage =
    term.length > 0 || statusFilter !== undefined;

  const handleActivate = async (warehouse: Warehouse) => {
    try {
      await activateWarehouse(warehouse.id).unwrap();
      toast.success(`Warehouse "${warehouse.name}" activated!`);
    } catch (err) {
      toast.error("Failed to activate warehouse", {
        description: getApiErrorMessage(err),
      });
    }
  };

  const handleDeactivate = async (warehouse: Warehouse) => {
    try {
      await deactivateWarehouse(warehouse.id).unwrap();
      toast.success(`Warehouse "${warehouse.name}" deactivated.`);
    } catch (err) {
      toast.error("Failed to deactivate warehouse", {
        description: getApiErrorMessage(err),
      });
    }
  };

  const handleSetDefault = async (warehouse: Warehouse) => {
    try {
      await setDefaultWarehouse(warehouse.id).unwrap();
      toast.success(`"${warehouse.name}" is now the default warehouse.`, {
        description:
          "Goods received with no explicit destination will land here.",
      });
    } catch (err) {
      toast.error("Failed to set default warehouse", {
        description: getApiErrorMessage(err),
      });
    }
  };

  const totalElements = data?.totalElements ?? 0;
  const totalPages = data?.totalPages ?? 1;

  return (
    <div className="flex flex-1 flex-col space-y-6 p-4 sm:p-6 lg:p-8">
      {/* Top Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold tracking-tight text-foreground sm:text-3xl">
              Warehouse Management
            </h1>
            <span className="inline-flex items-center rounded-full bg-primary/10 px-2.5 py-0.5 text-xs font-semibold text-primary">
              {totalElements} Warehouses
            </span>
          </div>
          <p className="mt-1 text-sm text-muted-foreground">
            Physical storage locations, and where goods land when no
            destination is given.
          </p>
        </div>

        <Button
          onClick={() => setIsCreateOpen(true)}
          className="gap-2 self-start shadow-sm sm:self-auto"
        >
          <Plus className="size-4" />
          Add Warehouse
        </Button>
      </div>

      {/* Filter Toolbar */}
      <div className="flex flex-col gap-3 rounded-xl border border-border bg-card p-3 shadow-sm sm:flex-row sm:items-center sm:justify-between">
        <div className="flex flex-1 items-center gap-3">
          {/* Search bar */}
          <div className="relative max-w-sm flex-1">
            <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder="Search by name or code..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="h-9 pl-9"
            />
          </div>

          {/* Status Filter */}
          <div className="flex items-center gap-1.5">
            <Filter className="hidden size-4 text-muted-foreground sm:block" />
            <select
              value={statusFilter === undefined ? "" : statusFilter.toString()}
              onChange={(e) => {
                const val = e.target.value;
                setStatusFilter(val === "" ? undefined : val === "true");
              }}
              className="h-9 rounded-md border border-input bg-background px-3 py-1 text-sm shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
            >
              <option value="">All Statuses</option>
              <option value="true">Active</option>
              <option value="false">Inactive</option>
            </select>
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

      {/* Main Warehouses Table */}
      <div className="overflow-hidden rounded-xl border border-border bg-card shadow-sm">
        <Table>
          <TableHeader className="bg-muted/40">
            <TableRow>
              <TableHead>Warehouse</TableHead>
              <TableHead>Code</TableHead>
              <TableHead>Type</TableHead>
              <TableHead>Branch</TableHead>
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
                  <TableCell>
                    <Skeleton className="h-4 w-24" />
                  </TableCell>
                  <TableCell>
                    <Skeleton className="h-4 w-24" />
                  </TableCell>
                  <TableCell>
                    <Skeleton className="h-4 w-24" />
                  </TableCell>
                  <TableCell>
                    <Skeleton className="h-5 w-20 rounded-full" />
                  </TableCell>
                  <TableCell>
                    <Skeleton className="h-4 w-24" />
                  </TableCell>
                  <TableCell className="text-right">
                    <Skeleton className="ml-auto size-8 rounded-md" />
                  </TableCell>
                </TableRow>
              ))
            ) : warehouses.length === 0 ? (
              <TableRow>
                <TableCell colSpan={7} className="h-64 text-center">
                  <div className="flex flex-col items-center justify-center gap-2">
                    <div className="flex size-12 items-center justify-center rounded-full bg-muted">
                      <WarehouseIcon className="size-6 text-muted-foreground" />
                    </div>
                    <p className="text-base font-semibold text-foreground">
                      No warehouses found
                    </p>
                    <p className="max-w-sm text-sm text-muted-foreground">
                      {isFilteredPage
                        ? "No warehouses on this page match your active search or filters. Matching warehouses may exist on another page."
                        : "No warehouses yet. One is created automatically the first time the company needs a destination for goods; you can also add one now."}
                    </p>
                    {isFilteredPage ? (
                      <Button
                        variant="link"
                        size="sm"
                        onClick={() => {
                          setSearchTerm("");
                          setStatusFilter(undefined);
                        }}
                      >
                        Clear Filters
                      </Button>
                    ) : (
                      <Button
                        variant="link"
                        size="sm"
                        onClick={() => setIsCreateOpen(true)}
                      >
                        Add Warehouse
                      </Button>
                    )}
                  </div>
                </TableCell>
              </TableRow>
            ) : (
              <>
                {warehouses.map((warehouse) => {
                  const initials = warehouse.name?.[0]?.toUpperCase() ?? "W";
                  return (
                    <TableRow
                      key={warehouse.id}
                      className="group transition-colors hover:bg-muted/50"
                    >
                      <TableCell>
                        <div className="flex items-center gap-3">
                          <Avatar className="size-9 border border-border">
                            <AvatarFallback className="bg-primary/10 text-xs font-bold text-primary">
                              {initials}
                            </AvatarFallback>
                          </Avatar>
                          <div className="min-w-0">
                            <p className="flex items-center gap-1.5 truncate font-medium text-foreground">
                              {warehouse.name}
                              {warehouse.isDefault && (
                                <Star
                                  className="size-3.5 shrink-0 fill-amber-400 text-amber-500"
                                  aria-label="Default warehouse"
                                />
                              )}
                            </p>
                            <p className="truncate text-xs text-muted-foreground">
                              ID: {warehouse.id}
                            </p>
                          </div>
                        </div>
                      </TableCell>

                      <TableCell className="font-mono text-sm text-muted-foreground">
                        {warehouse.code}
                      </TableCell>

                      <TableCell className="text-sm text-foreground">
                        {formatWarehouseType(warehouse.type)}
                      </TableCell>

                      <TableCell className="text-sm text-muted-foreground">
                        {warehouse.branchId != null
                          ? `#${warehouse.branchId}`
                          : "Company-wide"}
                      </TableCell>

                      <TableCell>
                        <WarehouseStatusBadge isActive={warehouse.isActive} />
                      </TableCell>

                      <TableCell className="text-xs text-muted-foreground">
                        {warehouse.createdAt
                          ? new Date(warehouse.createdAt).toLocaleDateString()
                          : "—"}
                      </TableCell>

                      <TableCell className="text-right">
                        <DropdownMenu modal>
                          <DropdownMenuTrigger asChild>
                            <Button
                              variant="ghost"
                              size="icon"
                              className="size-8"
                            >
                              <MoreVertical className="size-4" />
                              <span className="sr-only">Open menu</span>
                            </Button>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent
                            align="end"
                            className="z-[9999] w-48"
                          >
                            <DropdownMenuItem
                              onClick={() =>
                                setSelectedWarehouseIdForDetails(warehouse.id)
                              }
                            >
                              <Eye className="mr-2 size-4 text-muted-foreground" />
                              View Details
                            </DropdownMenuItem>

                            <DropdownMenuItem
                              onClick={() =>
                                setSelectedWarehouseIdForEdit(warehouse.id)
                              }
                            >
                              <Pencil className="mr-2 size-4 text-muted-foreground" />
                              Edit Warehouse
                            </DropdownMenuItem>

                            {!warehouse.isDefault && (
                              <>
                                <DropdownMenuSeparator />
                                <DropdownMenuItem
                                  onClick={() =>
                                    handleSetDefault(warehouse)
                                  }
                                >
                                  <Star className="mr-2 size-4 text-amber-500" />
                                  Set as Default
                                </DropdownMenuItem>
                              </>
                            )}

                            <DropdownMenuSeparator />

                            {/*
                              The backend refuses to deactivate a default
                              warehouse, so that action is withheld instead of
                              offered and then failed.
                            */}
                            {warehouse.isActive ? (
                              !warehouse.isDefault && (
                                <DropdownMenuItem
                                  onClick={() =>
                                    handleDeactivate(warehouse)
                                  }
                                >
                                  <PauseCircle className="mr-2 size-4 text-amber-600" />
                                  Deactivate Warehouse
                                </DropdownMenuItem>
                              )
                            ) : (
                              <DropdownMenuItem
                                onClick={() => handleActivate(warehouse)}
                              >
                                <CheckCircle2 className="mr-2 size-4 text-emerald-600" />
                                Activate Warehouse
                              </DropdownMenuItem>
                            )}
                          </DropdownMenuContent>
                        </DropdownMenu>
                      </TableCell>
                    </TableRow>
                  );
                })}

                {isFilteredPage && (
                  <TableRow>
                    <TableCell
                      colSpan={7}
                      className="bg-muted/30 text-xs text-muted-foreground"
                    >
                      Showing {warehouses.length} of {allWarehouses.length}{" "}
                      warehouses on page {page + 1}. Filters apply to the
                      current page only.
                    </TableCell>
                  </TableRow>
                )}
              </>
            )}
          </TableBody>
        </Table>

        {/* Pagination Bar */}
        <div className="flex flex-col gap-3 border-t border-border bg-muted/20 px-4 py-3 text-sm sm:flex-row sm:items-center sm:justify-between">
          <div className="text-muted-foreground">
            Showing{" "}
            <span className="font-medium text-foreground">
              {warehouses.length}
            </span>{" "}
            of{" "}
            <span className="font-medium text-foreground">
              {totalElements}
            </span>{" "}
            total warehouses
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
      <CreateWarehouseDialog
        open={isCreateOpen}
        onOpenChange={setIsCreateOpen}
      />

      <EditWarehouseDialog
        warehouse={warehouseToEdit ?? null}
        open={!!selectedWarehouseIdForEdit}
        onOpenChange={(open) => {
          if (!open) setSelectedWarehouseIdForEdit(null);
        }}
      />

      <WarehouseDetailsSheet
        warehouseId={selectedWarehouseIdForDetails}
        open={!!selectedWarehouseIdForDetails}
        onOpenChange={(open) => {
          if (!open) setSelectedWarehouseIdForDetails(null);
        }}
        onEdit={() => {
          const wid = selectedWarehouseIdForDetails;
          setSelectedWarehouseIdForDetails(null);
          setSelectedWarehouseIdForEdit(wid);
        }}
      />
    </div>
  );
}
