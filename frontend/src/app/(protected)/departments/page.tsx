"use client";

import { useState } from "react";
import {
  FolderTree,
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
  useGetDepartmentsQuery,
  useGetDepartmentQuery,
  useActivateDepartmentMutation,
  useDeactivateDepartmentMutation,
  type DepartmentListResponse,
  type DepartmentStatus,
} from "@/services/department.service";
import { BranchSelect } from "@/components/common/branch-select";
import { DepartmentStatusBadge } from "@/components/departments/department-status-badge";
import { CreateDepartmentDialog } from "@/components/departments/create-department-dialog";
import { EditDepartmentDialog } from "@/components/departments/edit-department-dialog";
import { DepartmentDetailsSheet } from "@/components/departments/department-details-sheet";
import { DeleteDepartmentDialog } from "@/components/departments/delete-department-dialog";
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

export default function DepartmentsPage() {
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState<DepartmentStatus | undefined>(undefined);
  const [branchFilter, setBranchFilter] = useState<number | undefined>(undefined);

  // Modals state
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [selectedDepartmentIdForDetails, setSelectedDepartmentIdForDetails] = useState<string | null>(null);
  const [selectedDepartmentIdForEdit, setSelectedDepartmentIdForEdit] = useState<string | null>(null);
  const [departmentToDelete, setDepartmentToDelete] = useState<DepartmentListResponse | null>(null);

  // Query departments list
  const { data, isLoading, isFetching, refetch } = useGetDepartmentsQuery({
    page,
    size: pageSize,
    search: searchTerm.trim() || undefined,
    status: statusFilter,
    branchId: branchFilter,
    sort: "createdAt,desc",
  });

  // Fetch full department for edit modal
  const { data: departmentToEdit } = useGetDepartmentQuery(selectedDepartmentIdForEdit ?? "", {
    skip: !selectedDepartmentIdForEdit,
  });

  const [activateDepartment] = useActivateDepartmentMutation();
  const [deactivateDepartment] = useDeactivateDepartmentMutation();

  const handleActivate = async (department: DepartmentListResponse) => {
    try {
      await activateDepartment(department.departmentId).unwrap();
      toast.success(`Department "${department.departmentName}" activated!`);
    } catch (err) {
      toast.error("Failed to activate department", { description: getApiErrorMessage(err) });
    }
  };

  const handleDeactivate = async (department: DepartmentListResponse) => {
    try {
      await deactivateDepartment(department.departmentId).unwrap();
      toast.success(`Department "${department.departmentName}" deactivated.`);
    } catch (err) {
      toast.error("Failed to deactivate department", { description: getApiErrorMessage(err) });
    }
  };

  const departments = data?.content ?? [];
  const totalElements = data?.totalElements ?? 0;
  const totalPages = data?.totalPages ?? 1;

  return (
    <div className="flex flex-1 flex-col space-y-6 p-4 sm:p-6 lg:p-8">
      {/* Top Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold tracking-tight text-foreground sm:text-3xl">
              Department Management
            </h1>
            <span className="inline-flex items-center rounded-full bg-primary/10 px-2.5 py-0.5 text-xs font-semibold text-primary">
              {totalElements} Departments
            </span>
          </div>
          <p className="mt-1 text-sm text-muted-foreground">
            Manage organizational departments, hierarchy, and operational status.
          </p>
        </div>

        <Button onClick={() => setIsCreateOpen(true)} className="gap-2 self-start sm:self-auto shadow-sm">
          <Plus className="size-4" />
          Add Department
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
              value={statusFilter ?? ""}
              onChange={(e) => {
                const val = e.target.value;
                setStatusFilter(val ? (val as DepartmentStatus) : undefined);
                setPage(0);
              }}
              className="h-9 rounded-md border border-input bg-background px-3 py-1 text-sm shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
            >
              <option value="">All Statuses</option>
              <option value="ACTIVE">Active</option>
              <option value="INACTIVE">Inactive</option>
            </select>
          </div>

          {/* Branch Filter */}
          <div className="flex items-center gap-1.5">
            <BranchSelect
              value={branchFilter}
              onChange={(val) => {
                setBranchFilter(val);
                setPage(0);
              }}
              placeholder="All Branches"
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

      {/* Main Departments Table */}
      <div className="rounded-xl border border-border bg-card shadow-sm overflow-hidden">
        <Table>
          <TableHeader className="bg-muted/40">
            <TableRow>
              <TableHead>Department</TableHead>
              <TableHead>Code</TableHead>
              <TableHead>Branch ID</TableHead>
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
                  <TableCell><Skeleton className="h-4 w-20" /></TableCell>
                  <TableCell><Skeleton className="h-4 w-16" /></TableCell>
                  <TableCell><Skeleton className="h-5 w-20 rounded-full" /></TableCell>
                  <TableCell><Skeleton className="h-4 w-24" /></TableCell>
                  <TableCell className="text-right"><Skeleton className="size-8 rounded-md ml-auto" /></TableCell>
                </TableRow>
              ))
            ) : departments.length === 0 ? (
              <TableRow>
                <TableCell colSpan={6} className="h-64 text-center">
                  <div className="flex flex-col items-center justify-center gap-2">
                    <div className="flex size-12 items-center justify-center rounded-full bg-muted">
                      <FolderTree className="size-6 text-muted-foreground" />
                    </div>
                    <p className="text-base font-semibold text-foreground">No departments found</p>
                    <p className="text-sm text-muted-foreground max-w-sm">
                      {searchTerm || statusFilter
                        ? "No departments match your active search or status filters."
                        : "No departments created yet. Click 'Add Department' to create the first department."}
                    </p>
                    {(searchTerm || statusFilter) && (
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
                    )}
                  </div>
                </TableCell>
              </TableRow>
            ) : (
              departments.map((department) => {
                const initials = department.departmentName?.[0]?.toUpperCase() ?? "D";
                return (
                  <TableRow key={department.departmentId} className="group transition-colors hover:bg-muted/50">
                    <TableCell>
                      <div className="flex items-center gap-3">
                        <Avatar className="size-9 border border-border">
                          <AvatarFallback className="bg-primary/10 text-xs font-bold text-primary">
                            {initials}
                          </AvatarFallback>
                        </Avatar>
                        <div className="min-w-0">
                          <p className="font-medium text-foreground truncate">{department.departmentName}</p>
                          <p className="text-xs text-muted-foreground truncate">ID: {department.id}</p>
                        </div>
                      </div>
                    </TableCell>

                    <TableCell className="text-sm font-mono text-muted-foreground">
                      {department.departmentCode}
                    </TableCell>

                    <TableCell className="text-sm text-foreground">
                      {department.branchId ?? "—"}
                    </TableCell>

                    <TableCell>
                      <DepartmentStatusBadge status={department.status} />
                    </TableCell>

                    <TableCell className="text-xs text-muted-foreground">
                      {department.createdAt ? new Date(department.createdAt).toLocaleDateString() : "—"}
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
                          <DropdownMenuItem onClick={() => setSelectedDepartmentIdForDetails(department.departmentId)}>
                            <Eye className="mr-2 size-4 text-muted-foreground" />
                            View Details
                          </DropdownMenuItem>

                          <DropdownMenuItem onClick={() => setSelectedDepartmentIdForEdit(department.departmentId)}>
                            <Pencil className="mr-2 size-4 text-muted-foreground" />
                            Edit Department
                          </DropdownMenuItem>

                          <DropdownMenuSeparator />

                          {department.status === "INACTIVE" ? (
                            <DropdownMenuItem onClick={() => handleActivate(department)}>
                              <CheckCircle2 className="mr-2 size-4 text-emerald-600" />
                              Activate Department
                            </DropdownMenuItem>
                          ) : department.status === "ACTIVE" ? (
                            <DropdownMenuItem onClick={() => handleDeactivate(department)}>
                              <PauseCircle className="mr-2 size-4 text-amber-600" />
                              Deactivate Department
                            </DropdownMenuItem>
                          ) : null}

                          <DropdownMenuSeparator />

                          <DropdownMenuItem
                            className="text-destructive focus:text-destructive"
                            onClick={() => setDepartmentToDelete(department)}
                          >
                            <Trash2 className="mr-2 size-4" />
                            Delete Department
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
            Showing <span className="font-medium text-foreground">{departments.length}</span> of{" "}
            <span className="font-medium text-foreground">{totalElements}</span> total departments
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
      <CreateDepartmentDialog open={isCreateOpen} onOpenChange={setIsCreateOpen} />

      <EditDepartmentDialog
        department={departmentToEdit ?? null}
        open={!!selectedDepartmentIdForEdit}
        onOpenChange={(open) => {
          if (!open) setSelectedDepartmentIdForEdit(null);
        }}
      />

      <DepartmentDetailsSheet
        departmentId={selectedDepartmentIdForDetails}
        open={!!selectedDepartmentIdForDetails}
        onOpenChange={(open) => {
          if (!open) setSelectedDepartmentIdForDetails(null);
        }}
        onEdit={() => {
          const did = selectedDepartmentIdForDetails;
          setSelectedDepartmentIdForDetails(null);
          setSelectedDepartmentIdForEdit(did);
        }}
        onDelete={() => {
          const target = departments.find((d) => d.departmentId === selectedDepartmentIdForDetails);
          setSelectedDepartmentIdForDetails(null);
          if (target) setDepartmentToDelete(target);
        }}
      />

      <DeleteDepartmentDialog
        department={departmentToDelete}
        open={!!departmentToDelete}
        onOpenChange={(open) => {
          if (!open) setDepartmentToDelete(null);
        }}
      />
    </div>
  );
}
