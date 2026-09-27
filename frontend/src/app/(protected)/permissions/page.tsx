"use client";

import { useState } from "react";
import {
  Plus,
  Search,
  RefreshCw,
  MoreVertical,
  Eye,
  Pencil,
  Trash2,
  ChevronLeft,
  ChevronRight,
  Filter,
} from "lucide-react";
import {
  useGetPermissionsQuery,
  useGetPermissionQuery,
  type PermissionResponse,
  type PermissionStatus,
} from "@/services/permission.service";
import { PermissionStatusBadge } from "@/components/permissions/permission-status-badge";
import { CreatePermissionDialog } from "@/components/permissions/create-permission-dialog";
import { EditPermissionDialog } from "@/components/permissions/edit-permission-dialog";
import { PermissionDetailsSheet } from "@/components/permissions/permission-details-sheet";
import { DeletePermissionDialog } from "@/components/permissions/delete-permission-dialog";
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

export default function PermissionsPage() {
  const [page, setPage] = useState(0);
  const [pageSize] = useState(10);
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState<PermissionStatus | undefined>(undefined);

  // Modals state
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [selectedPermissionIdForDetails, setSelectedPermissionIdForDetails] = useState<number | null>(null);
  const [selectedPermissionIdForEdit, setSelectedPermissionIdForEdit] = useState<number | null>(null);
  const [permissionToDelete, setPermissionToDelete] = useState<PermissionResponse | null>(null);

  // Query permissions list
  const { data, isLoading, isFetching, refetch } = useGetPermissionsQuery({
    page,
    size: pageSize,
    search: searchTerm.trim() || undefined,
    status: statusFilter,
    sort: "createdAt,desc",
  });

  // Fetch full permission for edit modal
  const { data: permissionToEdit } = useGetPermissionQuery(selectedPermissionIdForEdit ?? 0, {
    skip: !selectedPermissionIdForEdit,
  });

  const totalPages = data?.totalPages ?? 0;
  const permissions = data?.permissions ?? [];
  const totalElements = data?.totalElements ?? 0;

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Permissions</h1>
          <p className="text-muted-foreground">
            Manage access control permissions for your organization.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="icon"
            onClick={() => refetch()}
            disabled={isFetching}
          >
            <RefreshCw className={`size-4 ${isFetching ? "animate-spin" : ""}`} />
          </Button>
          <Button onClick={() => setIsCreateOpen(true)}>
            <Plus className="mr-2 size-4" />
            Add Permission
          </Button>
        </div>
      </div>

      {/* Filters */}
      <div className="flex flex-col gap-4 sm:flex-row">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            placeholder="Search permissions..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="pl-9"
          />
        </div>
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="outline" className="w-full sm:w-auto">
              <Filter className="mr-2 size-4" />
              {statusFilter ? `Status: ${statusFilter}` : "All Statuses"}
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-[180px]">
            <DropdownMenuItem onClick={() => setStatusFilter(undefined)}>
              All Statuses
            </DropdownMenuItem>
            <DropdownMenuItem onClick={() => setStatusFilter("ACTIVE")}>
              Active
            </DropdownMenuItem>
            <DropdownMenuItem onClick={() => setStatusFilter("INACTIVE")}>
              Inactive
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>

      {/* Data Table */}
      <div className="rounded-md border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Permission Code</TableHead>
              <TableHead>Resource</TableHead>
              <TableHead>Action</TableHead>
              <TableHead>Description</TableHead>
              <TableHead>Status</TableHead>
              <TableHead className="w-[80px]">Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading ? (
              Array.from({ length: pageSize }).map((_, i) => (
                <TableRow key={i}>
                  <TableCell><Skeleton className="h-4 w-24" /></TableCell>
                  <TableCell><Skeleton className="h-4 w-20" /></TableCell>
                  <TableCell><Skeleton className="h-4 w-20" /></TableCell>
                  <TableCell><Skeleton className="h-4 w-40" /></TableCell>
                  <TableCell><Skeleton className="h-4 w-16" /></TableCell>
                  <TableCell><Skeleton className="h-8 w-8" /></TableCell>
                </TableRow>
              ))
            ) : permissions.length === 0 ? (
              <TableRow>
                <TableCell colSpan={6} className="h-24 text-center">
                  No permissions found.
                </TableCell>
              </TableRow>
            ) : (
              permissions.map((permission) => (
                <TableRow key={permission.id}>
                  <TableCell>
                    <span className="font-mono text-sm font-medium">
                      {permission.permissionCode}
                    </span>
                  </TableCell>
                  <TableCell>{permission.resource}</TableCell>
                  <TableCell>{permission.action}</TableCell>
                  <TableCell className="max-w-[200px] truncate">
                    {permission.description || "-"}
                  </TableCell>
                  <TableCell>
                    <PermissionStatusBadge status={permission.status} />
                  </TableCell>
                  <TableCell>
                    <DropdownMenu modal={false}>
                      <DropdownMenuTrigger asChild>
                        <Button variant="ghost" size="icon" className="size-8">
                          <MoreVertical className="size-4" />
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end" className="z-[9999]">
                        <DropdownMenuItem
                          onClick={() => setSelectedPermissionIdForDetails(permission.id)}
                        >
                          <Eye className="mr-2 size-4" />
                          View Details
                        </DropdownMenuItem>
                        <DropdownMenuItem
                          onClick={() => setSelectedPermissionIdForEdit(permission.id)}
                        >
                          <Pencil className="mr-2 size-4" />
                          Edit
                        </DropdownMenuItem>
                        <DropdownMenuSeparator />
                        <DropdownMenuItem
                          onClick={() => setPermissionToDelete(permission)}
                          className="text-destructive"
                        >
                          <Trash2 className="mr-2 size-4" />
                          Delete
                        </DropdownMenuItem>
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between">
          <p className="text-sm text-muted-foreground">
            Showing {permissions.length} of {totalElements} permissions
          </p>
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0 || isLoading}
            >
              <ChevronLeft className="size-4" />
              Previous
            </Button>
            <span className="text-sm text-muted-foreground">
              Page {page + 1} of {totalPages}
            </span>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
              disabled={page >= totalPages - 1 || isLoading}
            >
              Next
              <ChevronRight className="size-4" />
            </Button>
          </div>
        </div>
      )}

      {/* Modals */}
      <CreatePermissionDialog open={isCreateOpen} onOpenChange={setIsCreateOpen} />
      <EditPermissionDialog
        open={!!selectedPermissionIdForEdit}
        onOpenChange={(open) => !open && setSelectedPermissionIdForEdit(null)}
        permission={permissionToEdit ?? null}
      />
      <PermissionDetailsSheet
        open={!!selectedPermissionIdForDetails}
        onOpenChange={(open) => !open && setSelectedPermissionIdForDetails(null)}
        permission={permissions.find((p) => p.id === selectedPermissionIdForDetails) ?? null}
      />
      <DeletePermissionDialog
        open={!!permissionToDelete}
        onOpenChange={(open) => !open && setPermissionToDelete(null)}
        permission={permissionToDelete}
      />
    </div>
  );
}
