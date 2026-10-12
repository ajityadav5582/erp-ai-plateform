"use client";

import { useMemo, useState } from "react";
import dynamic from "next/dynamic";
import {
  FileText,
  Plus,
  Inbox,
} from "lucide-react";
import {
  useGetRequisitionsQuery,
  useGetMyRequisitionsQuery,
  useGetApprovalInboxQuery,
  useGetRequisitionQuery,
  REQUISITION_STATUSES,
  REQUISITION_STATUS_LABELS,
  type RequisitionListResponse,
  type RequisitionStatus,
} from "@/services/purchase-requisition.service";
import { useGetBranchesQuery } from "@/services/branch.service";
import {
  RequisitionPriorityBadge,
  RequisitionStatusBadge,
} from "@/components/requisitions/requisition-status-badge";
import { RequisitionRowActions } from "@/components/requisitions/requisition-row-actions";
import { ListPageHeader } from "@/components/common/list-page-header";
import { ListToolbar } from "@/components/common/list-toolbar";
import { ListPagination } from "@/components/common/list-pagination";
import { ScopeTabs, type ScopeTab } from "@/components/common/scope-tabs";
import {
  TableEmptyRow,
  TableSkeletonRows,
} from "@/components/common/table-state";

/**
 * The dialogs below together weigh ~55 kB of JS (the create form alone is ~28 kB)
 * and are closed for the entire time this list is on screen. Statically importing
 * them forced every visit to parse and execute all of it before the list could be
 * interactive, so they are code-split and fetched only when actually opened.
 *
 * `loading: () => null` matches the closed state, so there is no layout shift or
 * flash when a dialog is opened.
 */
const CreateRequisitionDialog = dynamic(
  () =>
    import("@/components/requisitions/create-requisition-dialog").then((mod) => ({
      default: mod.CreateRequisitionDialog,
    })),
  { ssr: false, loading: () => null }
);

const RequisitionDecisionDialog = dynamic(
  () =>
    import("@/components/requisitions/requisition-decision-dialog").then((mod) => ({
      default: mod.RequisitionDecisionDialog,
    })),
  { ssr: false, loading: () => null }
);

const CancelRequisitionDialog = dynamic(
  () =>
    import("@/components/requisitions/cancel-requisition-dialog").then((mod) => ({
      default: mod.CancelRequisitionDialog,
    })),
  { ssr: false, loading: () => null }
);

const RequisitionDetailsSheet = dynamic(
  () =>
    import("@/components/requisitions/requisition-details-sheet").then((mod) => ({
      default: mod.RequisitionDetailsSheet,
    })),
  { ssr: false, loading: () => null }
);
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";

/**
 * Which slice of the requisition book the table is showing.
 *
 * Three lists rather than one with filters, because they answer three different
 * questions and are backed by three different access paths: what the company
 * raised, what I raised, and what is mine to decide. Mixing them into one screen
 * with checkboxes makes the approver's queue impossible to find.
 */
type RequisitionScope = "all" | "mine" | "inbox";

const SCOPE_TABS: ScopeTab[] = [
  {
    value: "all",
    label: "All requisitions",
    hint: "Everything in this company",
  },
  { value: "mine", label: "Raised by me", hint: "Requisitions you submitted" },
  {
    value: "inbox",
    label: "Awaiting my approval",
    icon: Inbox,
    hint: "Requisitions waiting on your decision right now",
  },
];

export default function PurchaseRequisitionsPage() {
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [branchFilter, setBranchFilter] = useState<number | undefined>();
  const [statusFilter, setStatusFilter] = useState<RequisitionStatus | undefined>();
  const [scope, setScope] = useState<RequisitionScope>("all");

  // Dialogs state
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [detailsId, setDetailsId] = useState<number | null>(null);
  const [decisionId, setDecisionId] = useState<number | null>(null);
  const [editId, setEditId] = useState<number | null>(null);
  const [toCancel, setToCancel] = useState<RequisitionListResponse | null>(null);

  const listParams = {
    page,
    size: pageSize,
    sort: "requestDate,desc",
    branchId: branchFilter,
    status: statusFilter,
  };

  // All three queries are mounted and switched between rather than selected
  // conditionally. RTK Query keeps the unselected ones' data cached, so
  // flipping to "Awaiting my approval" and back does not re-hit the network,
  // and the inbox count stays live while the user is looking at another tab.
  const allQuery = useGetRequisitionsQuery(listParams, { skip: scope !== "all" });
  const mineQuery = useGetMyRequisitionsQuery(listParams, { skip: scope !== "mine" });
  const inboxQuery = useGetApprovalInboxQuery(listParams, {
    skip: scope !== "inbox",
  });

  // A separate single-row call purely for the tab badge. Keeping it independent
  // of the paged query is what lets the badge stay correct while the user is on
  // another tab, which is the whole point of putting a count on it.
  //
  // The one exception: while the user is already on the inbox tab with no
  // branch/status filters, the paged query's `totalElements` IS the unfiltered
  // inbox count, so the extra round-trip is redundant. Skip it and derive the
  // badge from the list already being fetched. Filters change the total, so the
  // moment a filter is applied we fall back to the dedicated count call.
  const hasListFilters =
    branchFilter !== undefined || statusFilter !== undefined;
  const canDeriveInboxCount = scope === "inbox" && !hasListFilters;
  const inboxCountQuery = useGetApprovalInboxQuery(
    { page: 0, size: 1, sort: "requestDate,desc" },
    { skip: canDeriveInboxCount }
  );

  const activeQuery =
    scope === "mine" ? mineQuery : scope === "inbox" ? inboxQuery : allQuery;
  const data = activeQuery.data;
  const isLoading = activeQuery.isLoading;
  const isFetching = activeQuery.isFetching;
  const refetch = activeQuery.refetch;

  const { data: branchData } = useGetBranchesQuery(
    { page: 0, size: 100, status: "ACTIVE", sort: "branchName,asc" }
  );
  const branches = useMemo(() => branchData?.content ?? [], [branchData]);

  // The list projection carries only branchId, so names are resolved from the
  // same branch list that backs the filter dropdown rather than widening the
  // requisition response. Falls back to the raw id for branches outside the
  // active page (inactive or archived branches still appear on old requisitions).
  const branchNameById = useMemo(
    () => new Map(branches.map((branch) => [branch.id, branch.branchName])),
    [branches]
  );
  const getBranchName = (branchId: number) =>
    branchNameById.get(branchId) ?? `#${branchId}`;

  // The decision, edit and cancel dialogs all need the full aggregate (lines
  // and the approval chain), not the list projection, so the detail is fetched
  // on demand rather than widening the list response for everyone.
  const { data: requisitionToDecide } = useGetRequisitionQuery(decisionId ?? 0, {
    skip: !decisionId,
  });
  const { data: requisitionToEdit } = useGetRequisitionQuery(editId ?? 0, {
    skip: !editId,
  });

  const closeEdit = () => setEditId(null);

  const requisitions = data?.content ?? [];
  const totalElements = data?.totalElements ?? 0;
  const totalPages = data?.totalPages ?? 1;
  const inboxCount = inboxCountQuery.data?.totalElements ?? 0;

  const [searchQuery, setSearchQuery] = useState("");

  const filteredRequisitions = useMemo(() => {
    if (!searchQuery.trim()) return requisitions;
    const query = searchQuery.toLowerCase().trim();
    return requisitions.filter(
      (r) =>
        r.prNumber.toLowerCase().includes(query) ||
        (r.requestDateNp && r.requestDateNp.includes(query)) ||
        (r.requestDate && r.requestDate.includes(query))
    );
  }, [requisitions, searchQuery]);

  // Compute status metrics from current dataset
  const metrics = useMemo(() => {
    const totalCount = totalElements;
    const pendingCount = inboxCount;
    const draftCount = requisitions.filter((r) => r.status === "DRAFT").length;
    const approvedCount = requisitions.filter((r) => r.status === "APPROVED").length;
    return { totalCount, pendingCount, draftCount, approvedCount };
  }, [totalElements, inboxCount, requisitions]);

  return (
    <div className="flex flex-1 flex-col space-y-6 p-4 sm:p-6 lg:p-8">
      <ListPageHeader
        title="Purchase Requisitions"
        description="Branch requests to buy. Manage demand, view approvals, and issue purchase orders."
        count={totalElements}
        countLabel="requisitions"
        actions={
          <Button
            onClick={() => setIsCreateOpen(true)}
            className="gap-2 shadow-sm"
          >
            <Plus className="size-4" />
            New Requisition
          </Button>
        }
      />

      {/* KPI Stats Cards */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div className="rounded-xl border border-border bg-card p-4 shadow-sm transition-all hover:shadow-md">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-muted-foreground">Total Requisitions</span>
            <div className="flex size-8 items-center justify-center rounded-lg bg-primary/10 text-primary">
              <FileText className="size-4" />
            </div>
          </div>
          <div className="mt-2 text-2xl font-bold text-foreground">{metrics.totalCount}</div>
          <p className="text-xs text-muted-foreground mt-1">Across active company</p>
        </div>

        <div className="rounded-xl border border-border bg-card p-4 shadow-sm transition-all hover:shadow-md">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-muted-foreground">Pending Approval</span>
            <div className="flex size-8 items-center justify-center rounded-lg bg-amber-500/10 text-amber-600 dark:text-amber-400">
              <Inbox className="size-4" />
            </div>
          </div>
          <div className="mt-2 text-2xl font-bold text-foreground">{metrics.pendingCount}</div>
          <p className="text-xs text-muted-foreground mt-1">Awaiting decision</p>
        </div>

        <div className="rounded-xl border border-border bg-card p-4 shadow-sm transition-all hover:shadow-md">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-muted-foreground">Approved Demand</span>
            <div className="flex size-8 items-center justify-center rounded-lg bg-emerald-500/10 text-emerald-600 dark:text-emerald-400">
              <FileText className="size-4" />
            </div>
          </div>
          <div className="mt-2 text-2xl font-bold text-foreground">{metrics.approvedCount}</div>
          <p className="text-xs text-muted-foreground mt-1">Ready for Purchase Orders</p>
        </div>

        <div className="rounded-xl border border-border bg-card p-4 shadow-sm transition-all hover:shadow-md">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-muted-foreground">Draft Requests</span>
            <div className="flex size-8 items-center justify-center rounded-lg bg-slate-500/10 text-slate-600 dark:text-slate-400">
              <FileText className="size-4" />
            </div>
          </div>
          <div className="mt-2 text-2xl font-bold text-foreground">{metrics.draftCount}</div>
          <p className="text-xs text-muted-foreground mt-1">In preparation</p>
        </div>
      </div>

      <ScopeTabs
        ariaLabel="Requisition scope"
        value={scope}
        onValueChange={(value) => {
          setScope(value as RequisitionScope);
          setPage(0);
        }}
        tabs={SCOPE_TABS.map((tab) =>
          tab.value === "inbox" ? { ...tab, badge: inboxCount } : tab
        )}
      />

      <ListToolbar
        isFetching={isFetching || inboxCountQuery.isFetching}
        onRefresh={() => {
          refetch();
          inboxCountQuery.refetch();
        }}
      >
        <div className="relative flex-1 max-w-sm">
          <input
            type="text"
            placeholder="Search PR number or date..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="h-9 w-full rounded-md border border-input bg-background px-3 py-1 text-sm shadow-sm transition-colors placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
          />
        </div>

        <Select
          value={branchFilter ? String(branchFilter) : "all"}
          onValueChange={(value) => {
            setBranchFilter(value === "all" ? undefined : Number(value));
            setPage(0);
          }}
        >
          <SelectTrigger size="sm" className="h-9 w-48">
            <SelectValue placeholder="All branches" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All branches</SelectItem>
            {branches.map((branch) => (
              <SelectItem key={branch.id} value={String(branch.id)}>
                {branch.branchName}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>

        <Select
          value={statusFilter ?? "all"}
          onValueChange={(value) => {
            setStatusFilter(value === "all" ? undefined : (value as RequisitionStatus));
            setPage(0);
          }}
        >
          <SelectTrigger size="sm" className="h-9 w-48">
            <SelectValue placeholder="All statuses" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All statuses</SelectItem>
            {REQUISITION_STATUSES.map((status) => (
              <SelectItem key={status} value={status}>
                {REQUISITION_STATUS_LABELS[status]}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </ListToolbar>

      {/* Requisitions Table */}
      <div className="overflow-hidden rounded-xl border border-border bg-card shadow-sm">
        <Table>
          <TableHeader className="bg-muted/40">
            <TableRow>
              <TableHead>Number</TableHead>
              <TableHead>Branch</TableHead>
              <TableHead>Priority</TableHead>
              <TableHead>Status</TableHead>
              <TableHead className="text-right">Estimated</TableHead>
              <TableHead className="text-right">Approved</TableHead>
              <TableHead className="text-right">Outstanding</TableHead>
              <TableHead className="text-right">Actions</TableHead>
            </TableRow>
          </TableHeader>

          <TableBody>
            {isLoading ? (
              <TableSkeletonRows columns={8} />
            ) : filteredRequisitions.length === 0 ? (
              <TableEmptyRow
                icon={FileText}
                title="No requisitions found"
                description={
                  branchFilter || statusFilter || searchQuery
                    ? "No requisitions match the selected filters or search query."
                    : "No requisitions raised yet. Click 'New Requisition' to raise the first one."
                }
                columns={8}
              />
            ) : (
              filteredRequisitions.map((requisition) => (
                <TableRow
                  key={requisition.id}
                  className="group transition-colors hover:bg-muted/50"
                >
                  <TableCell>
                    <p className="font-mono text-sm font-medium text-foreground">
                      {requisition.prNumber}
                    </p>
                    {/* Both calendars: the stored AD value and the BS projection the
                        backend persists alongside it. */}
                    <p className="text-xs text-muted-foreground">
                      {requisition.requestDate}
                    </p>
                    {requisition.requestDateNp ? (
                      <p className="font-nepali text-xs text-muted-foreground">
                        {requisition.requestDateNp}
                      </p>
                    ) : null}
                  </TableCell>

                  <TableCell className="text-sm text-muted-foreground">
                    {getBranchName(requisition.branchId)}
                  </TableCell>

                  <TableCell>
                    <RequisitionPriorityBadge priority={requisition.priority} />
                  </TableCell>

                  <TableCell>
                    <RequisitionStatusBadge status={requisition.status} />
                  </TableCell>

                  <TableCell className="text-right text-sm text-muted-foreground">
                    {Number(requisition.estimatedTotalAmount).toLocaleString()}
                  </TableCell>

                  <TableCell className="text-right text-sm font-medium text-foreground">
                    {Number(requisition.approvedTotalAmount).toLocaleString()}
                  </TableCell>

                  <TableCell className="text-right text-sm text-muted-foreground">
                    {Number(requisition.outstandingValue).toLocaleString()}
                  </TableCell>

                  <TableCell className="text-right">
                    <RequisitionRowActions
                      requisition={requisition}
                      onView={setDetailsId}
                      onEdit={setEditId}
                      onDecide={setDecisionId}
                      onCancel={setToCancel}
                    />
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>

        <ListPagination
          page={page}
          pageSize={pageSize}
          onPageChange={setPage}
          onPageSizeChange={(size) => {
            setPageSize(size);
            setPage(0);
          }}
          totalPages={totalPages}
          totalElements={totalElements}
          itemLabel="requisitions"
          isFetching={isFetching}
        />
      </div>

      {/* Dialogs */}
      <CreateRequisitionDialog
        open={isCreateOpen || !!editId}
        onOpenChange={(open) => {
          if (!open) {
            setIsCreateOpen(false);
            closeEdit();
          }
        }}
        defaultBranchId={branchFilter}
        editing={requisitionToEdit ?? null}
      />

      <RequisitionDetailsSheet
        requisitionId={detailsId}
        open={!!detailsId}
        onOpenChange={(open) => {
          if (!open) setDetailsId(null);
        }}
      />

      <RequisitionDecisionDialog
        requisition={requisitionToDecide ?? null}
        open={!!decisionId}
        onOpenChange={(open) => {
          if (!open) setDecisionId(null);
        }}
      />

      <CancelRequisitionDialog
        requisition={toCancel}
        open={!!toCancel}
        onOpenChange={(open) => {
          if (!open) setToCancel(null);
        }}
      />
    </div>
  );
}

