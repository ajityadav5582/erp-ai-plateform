"use client";

import { useState } from "react";
import {
  Shield,
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
  useGetRolesQuery,
  useGetRoleQuery,
  type RoleListResponse,
  type RoleType,
} from "@/services/role.service";
import { RoleStatusBadge } from "@/components/roles/role-status-badge";
import { CreateRoleDialog } from "@/components/roles/create-role-dialog";
import { EditRoleDialog } from "@/components/roles/edit-role-dialog";
import { RoleDetailsSheet } from "@/components/roles/role-details-sheet";
import { DeleteRoleDialog } from "@/components/roles/delete-role-dialog";
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

export default function RolesPage() {
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [searchTerm, setSearchTerm] = useState("");
  const [roleTypeFilter, setRoleTypeFilter] = useState<RoleType | undefined>(undefined);

  // Modals state
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [selectedRoleIdForDetails, setSelectedRoleIdForDetails] = useState<number | null>(null);
  const [selectedRoleIdForEdit, setSelectedRoleIdForEdit] = useState<number | null>(null);
  const [roleToDelete, setRoleToDelete] = useState<RoleListResponse | null>(null);

  // Query roles list
  const { data, isLoading, isFetching, refetch } = useGetRolesQuery({
    page,
    size: pageSize,
    search: searchTerm.trim() || undefined,
    roleType: roleTypeFilter,
    sort: "roleName,asc",
  });

  // Fetch full role for edit modal
  const { data: roleToEdit } = useGetRoleQuery(selectedRoleIdForEdit ?? 0, {
    skip: !selectedRoleIdForEdit,
  });

  const roles = data?.content ?? [];
  const totalElements = data?.totalElements ?? 0;
  const totalPages = data?.totalPages ?? 1;

  return (
    <div className="flex flex-1 flex-col space-y-6 p-4 sm:p-6 lg:p-8">
      {/* Top Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold tracking-tight text-foreground sm:text-3xl">
              Role Management
            </h1>
            <span className="inline-flex items-center rounded-full bg-primary/10 px-2.5 py-0.5 text-xs font-semibold text-primary">
              {totalElements} Roles
            </span>
          </div>
          <p className="mt-1 text-sm text-muted-foreground">
            Manage system and custom roles for access control.
          </p>
        </div>

        <Button onClick={() => setIsCreateOpen(true)} className="gap-2 self-start sm:self-auto shadow-sm">
          <Plus className="size-4" />
          Add Role
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

          {/* Role Type Filter */}
          <div className="flex items-center gap-1.5">
            <Filter className="size-4 text-muted-foreground hidden sm:block" />
            <select
              value={roleTypeFilter ?? ""}
              onChange={(e) => {
                const val = e.target.value;
                setRoleTypeFilter(val ? (val as RoleType) : undefined);
                setPage(0);
              }}
              className="h-9 rounded-md border border-input bg-background px-3 py-1 text-sm shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
            >
              <option value="">All Types</option>
              <option value="SYSTEM">System</option>
              <option value="CUSTOM">Custom</option>
            </select>
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

      {/* Main Roles Table */}
      <div className="rounded-xl border border-border bg-card shadow-sm overflow-hidden">
        <Table>
          <TableHeader className="bg-muted/40">
            <TableRow>
              <TableHead>Role</TableHead>
              <TableHead>Code</TableHead>
              <TableHead>Type</TableHead>
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
                  <TableCell><Skeleton className="h-5 w-20 rounded-full" /></TableCell>
                  <TableCell><Skeleton className="h-5 w-20 rounded-full" /></TableCell>
                  <TableCell className="text-right"><Skeleton className="size-8 rounded-md ml-auto" /></TableCell>
                </TableRow>
              ))
            ) : roles.length === 0 ? (
              <TableRow>
                <TableCell colSpan={5} className="h-64 text-center">
                  <div className="flex flex-col items-center justify-center gap-2">
                    <div className="flex size-12 items-center justify-center rounded-full bg-muted">
                      <Shield className="size-6 text-muted-foreground" />
                    </div>
                    <p className="text-base font-semibold text-foreground">No roles found</p>
                    <p className="text-sm text-muted-foreground max-w-sm">
                      {searchTerm || roleTypeFilter
                        ? "No roles match your active search or type filters."
                        : "No roles created yet. Click 'Add Role' to create the first role."}
                    </p>
                    {(searchTerm || roleTypeFilter) && (
                      <Button
                        variant="link"
                        size="sm"
                        onClick={() => {
                          setSearchTerm("");
                          setRoleTypeFilter(undefined);
                        }}
                      >
                        Clear Filters
                      </Button>
                    )}
                  </div>
                </TableCell>
              </TableRow>
            ) : (
              roles.map((role) => {
                const initials = role.roleName?.[0]?.toUpperCase() ?? "R";
                return (
                  <TableRow key={role.id} className="group transition-colors hover:bg-muted/50">
                    <TableCell>
                      <div className="flex items-center gap-3">
                        <Avatar className="size-9 border border-border">
                          <AvatarFallback className="bg-primary/10 text-xs font-bold text-primary">
                            {initials}
                          </AvatarFallback>
                        </Avatar>
                        <div className="min-w-0">
                          <p className="font-medium text-foreground truncate">{role.roleName}</p>
                          <p className="text-xs text-muted-foreground truncate">ID: {role.id}</p>
                        </div>
                      </div>
                    </TableCell>

                    <TableCell className="text-sm font-mono text-muted-foreground">
                      {role.roleCode}
                    </TableCell>

                    <TableCell className="text-sm text-foreground">
                      {role.roleType}
                    </TableCell>

                    <TableCell>
                      <RoleStatusBadge status={role.isActive ? "ACTIVE" : "INACTIVE"} roleType={role.roleType} />
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
                          <DropdownMenuItem onClick={() => setSelectedRoleIdForDetails(role.id)}>
                            <Eye className="mr-2 size-4 text-muted-foreground" />
                            View Details
                          </DropdownMenuItem>

                          {!role.isSystemRole && (
                            <DropdownMenuItem onClick={() => setSelectedRoleIdForEdit(role.id)}>
                              <Pencil className="mr-2 size-4 text-muted-foreground" />
                              Edit Role
                            </DropdownMenuItem>
                          )}

                          <DropdownMenuSeparator />

                          {!role.isSystemRole && (
                            <DropdownMenuItem
                              className="text-destructive focus:text-destructive"
                              onClick={() => setRoleToDelete(role)}
                            >
                              <Trash2 className="mr-2 size-4" />
                              Delete Role
                            </DropdownMenuItem>
                          )}
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
            Showing <span className="font-medium text-foreground">{roles.length}</span> of{" "}
            <span className="font-medium text-foreground">{totalElements}</span> total roles
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
      <CreateRoleDialog open={isCreateOpen} onOpenChange={setIsCreateOpen} />

      <EditRoleDialog
        role={roleToEdit ?? null}
        open={!!selectedRoleIdForEdit}
        onOpenChange={(open) => {
          if (!open) setSelectedRoleIdForEdit(null);
        }}
      />

      <RoleDetailsSheet
        roleId={selectedRoleIdForDetails}
        open={!!selectedRoleIdForDetails}
        onOpenChange={(open) => {
          if (!open) setSelectedRoleIdForDetails(null);
        }}
        onEdit={() => {
          const rid = selectedRoleIdForDetails;
          setSelectedRoleIdForDetails(null);
          setSelectedRoleIdForEdit(rid);
        }}
        onDelete={() => {
          const target = roles.find((r) => r.id === selectedRoleIdForDetails);
          setSelectedRoleIdForDetails(null);
          if (target) setRoleToDelete(target);
        }}
      />

      <DeleteRoleDialog
        role={roleToDelete}
        open={!!roleToDelete}
        onOpenChange={(open) => {
          if (!open) setRoleToDelete(null);
        }}
      />
    </div>
  );
}
