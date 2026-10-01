"use client";

import { useState } from "react";
import {
  Ruler,
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
  useGetUnitsQuery,
  useGetUnitQuery,
  useActivateUnitMutation,
  useDeactivateUnitMutation,
  formatUnitDimension,
  type Unit,
} from "@/services/unit.service";
import { UnitStatusBadge } from "@/components/units/unit-status-badge";
import { CreateUnitDialog } from "@/components/units/create-unit-dialog";
import { EditUnitDialog } from "@/components/units/edit-unit-dialog";
import { UnitDetailsSheet } from "@/components/units/unit-details-sheet";
import { DeleteUnitDialog } from "@/components/units/delete-unit-dialog";
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

export default function UnitsPage() {
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [searchTerm, setSearchTerm] = useState("");

  // Modals state
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [selectedUnitIdForDetails, setSelectedUnitIdForDetails] = useState<string | null>(null);
  const [selectedUnitIdForEdit, setSelectedUnitIdForEdit] = useState<string | null>(null);
  const [unitToDelete, setUnitToDelete] = useState<Unit | null>(null);

  // Query units list. The backend's /units endpoint already filters by the free
  // text term across both name and code, so no client-side filtering is needed.
  const { data, isLoading, isFetching, refetch } = useGetUnitsQuery({
    page,
    size: pageSize,
    search: searchTerm.trim() || undefined,
    sort: "name,asc",
  });

  // Fetch full unit for edit modal
  const { data: unitToEdit } = useGetUnitQuery(selectedUnitIdForEdit ?? "", {
    skip: !selectedUnitIdForEdit,
  });

  const [activateUnit] = useActivateUnitMutation();
  const [deactivateUnit] = useDeactivateUnitMutation();

  const handleActivate = async (unit: Unit) => {
    try {
      await activateUnit(unit.id).unwrap();
      toast.success(`Unit "${unit.name}" activated!`);
    } catch (err) {
      toast.error("Failed to activate unit", { description: getApiErrorMessage(err) });
    }
  };

  const handleDeactivate = async (unit: Unit) => {
    try {
      await deactivateUnit(unit.id).unwrap();
      toast.success(`Unit "${unit.name}" deactivated.`);
    } catch (err) {
      toast.error("Failed to deactivate unit", { description: getApiErrorMessage(err) });
    }
  };

  const units = data?.content ?? [];
  const totalElements = data?.totalElements ?? 0;
  const totalPages = data?.totalPages ?? 1;

  return (
    <div className="flex flex-1 flex-col space-y-6 p-4 sm:p-6 lg:p-8">
      {/* Top Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold tracking-tight text-foreground sm:text-3xl">
              Unit Management
            </h1>
            <span className="inline-flex items-center rounded-full bg-primary/10 px-2.5 py-0.5 text-xs font-semibold text-primary">
              {totalElements} Units
            </span>
          </div>
          <p className="mt-1 text-sm text-muted-foreground">
            Manage the units of measure inventory quantities are recorded in.
          </p>
        </div>

        <Button onClick={() => setIsCreateOpen(true)} className="gap-2 self-start sm:self-auto shadow-sm">
          <Plus className="size-4" />
          Add Unit
        </Button>
      </div>

      {/* Filter Toolbar */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between rounded-xl border border-border bg-card p-3 shadow-sm">
        <div className="flex flex-1 items-center gap-3">
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

      {/* Main Units Table */}
      <div className="rounded-xl border border-border bg-card shadow-sm overflow-hidden">
        <Table>
          <TableHeader className="bg-muted/40">
            <TableRow>
              <TableHead>Unit</TableHead>
              <TableHead>Code</TableHead>
              <TableHead>Dimension</TableHead>
              <TableHead>Symbol</TableHead>
              <TableHead>Decimal Places</TableHead>
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
                  <TableCell><Skeleton className="h-4 w-16" /></TableCell>
                  <TableCell><Skeleton className="h-4 w-24" /></TableCell>
                  <TableCell><Skeleton className="h-4 w-12" /></TableCell>
                  <TableCell><Skeleton className="h-4 w-16" /></TableCell>
                  <TableCell><Skeleton className="h-5 w-20 rounded-full" /></TableCell>
                  <TableCell className="text-right"><Skeleton className="size-8 rounded-md ml-auto" /></TableCell>
                </TableRow>
              ))
            ) : units.length === 0 ? (
              <TableRow>
                <TableCell colSpan={7} className="h-64 text-center">
                  <div className="flex flex-col items-center justify-center gap-2">
                    <div className="flex size-12 items-center justify-center rounded-full bg-muted">
                      <Ruler className="size-6 text-muted-foreground" />
                    </div>
                    <p className="text-base font-semibold text-foreground">No units found</p>
                    <p className="text-sm text-muted-foreground max-w-sm">
                      {searchTerm
                        ? "No units match your search."
                        : "No units created yet. Click 'Add Unit' to define the first one."}
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
              units.map((unit) => {
                const initials = unit.name?.[0]?.toUpperCase() ?? "U";
                return (
                  <TableRow key={unit.id} className="group transition-colors hover:bg-muted/50">
                    <TableCell>
                      <div className="flex items-center gap-3">
                        <Avatar className="size-9 border border-border">
                          <AvatarFallback className="bg-primary/10 text-xs font-bold text-primary">
                            {initials}
                          </AvatarFallback>
                        </Avatar>
                        <div className="min-w-0">
                          <p className="font-medium text-foreground truncate">{unit.name}</p>
                          <p className="text-xs text-muted-foreground truncate">ID: {unit.id}</p>
                        </div>
                      </div>
                    </TableCell>

                    <TableCell className="text-sm font-mono text-muted-foreground">
                      {unit.code}
                    </TableCell>

                    <TableCell className="text-sm text-foreground">
                      {formatUnitDimension(unit.dimension)}
                    </TableCell>

                    <TableCell className="text-sm text-muted-foreground">
                      {unit.symbol ?? "—"}
                    </TableCell>

                    <TableCell className="text-sm text-muted-foreground">
                      {unit.decimalScale ?? 0}
                    </TableCell>

                    <TableCell>
                      <UnitStatusBadge isActive={unit.isActive} />
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
                          <DropdownMenuItem onClick={() => setSelectedUnitIdForDetails(unit.id.toString())}>
                            <Eye className="mr-2 size-4 text-muted-foreground" />
                            View Details
                          </DropdownMenuItem>

                          <DropdownMenuItem onClick={() => setSelectedUnitIdForEdit(unit.id.toString())}>
                            <Pencil className="mr-2 size-4 text-muted-foreground" />
                            Edit Unit
                          </DropdownMenuItem>

                          <DropdownMenuSeparator />

                          {!unit.isActive ? (
                            <DropdownMenuItem onClick={() => handleActivate(unit)}>
                              <CheckCircle2 className="mr-2 size-4 text-emerald-600" />
                              Activate Unit
                            </DropdownMenuItem>
                          ) : (
                            <DropdownMenuItem onClick={() => handleDeactivate(unit)}>
                              <PauseCircle className="mr-2 size-4 text-amber-600" />
                              Deactivate Unit
                            </DropdownMenuItem>
                          )}

                          <DropdownMenuSeparator />

                          <DropdownMenuItem
                            className="text-destructive focus:text-destructive"
                            onClick={() => setUnitToDelete(unit)}
                          >
                            <Trash2 className="mr-2 size-4" />
                            Delete Unit
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
            Showing <span className="font-medium text-foreground">{units.length}</span> of{" "}
            <span className="font-medium text-foreground">{totalElements}</span> total units
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
      <CreateUnitDialog open={isCreateOpen} onOpenChange={setIsCreateOpen} />

      <EditUnitDialog
        unit={unitToEdit ?? null}
        open={!!selectedUnitIdForEdit}
        onOpenChange={(open) => {
          if (!open) setSelectedUnitIdForEdit(null);
        }}
      />

      <UnitDetailsSheet
        unitId={selectedUnitIdForDetails}
        open={!!selectedUnitIdForDetails}
        onOpenChange={(open) => {
          if (!open) setSelectedUnitIdForDetails(null);
        }}
        onEdit={() => {
          const uid = selectedUnitIdForDetails;
          setSelectedUnitIdForDetails(null);
          setSelectedUnitIdForEdit(uid);
        }}
        onDelete={() => {
          const target = units.find((u) => u.id.toString() === selectedUnitIdForDetails);
          setSelectedUnitIdForDetails(null);
          if (target) setUnitToDelete(target);
        }}
      />

      <DeleteUnitDialog
        unit={unitToDelete}
        open={!!unitToDelete}
        onOpenChange={(open) => {
          if (!open) setUnitToDelete(null);
        }}
      />
    </div>
  );
}
