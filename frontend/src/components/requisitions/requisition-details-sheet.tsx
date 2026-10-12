"use client";

import { useMemo } from "react";
import {
  useGetRequisitionQuery,
  formatRequisitionStatus,
  type Requisition,
} from "@/services/purchase-requisition.service";
import { useGetBranchesQuery } from "@/services/branch.service";
import {
  RequisitionLineStatusBadge,
  RequisitionPriorityBadge,
  RequisitionStatusBadge,
} from "@/components/requisitions/requisition-status-badge";

import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet";
import { Skeleton } from "@/components/ui/skeleton";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";

interface RequisitionDetailsSheetProps {
  requisitionId: number | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

function formatMoney(value: number, currency: string): string {
  return `${currency} ${Number(value).toLocaleString(undefined, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`;
}

function formatQuantity(value: number): string {
  return Number(value).toLocaleString(undefined, {
    maximumFractionDigits: 6,
  });
}

function formatTimestamp(value: string | null): string {
  if (!value) return "—";
  return new Date(value).toLocaleString();
}

function DetailRow({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="flex items-start justify-between gap-3 border-b border-border py-2 last:border-0">
      <span className="text-xs text-muted-foreground">{label}</span>
      <span className="text-right text-sm font-medium text-foreground">
        {value}
      </span>
    </div>
  );
}

/**
 * Read-only view of a requisition.
 *
 * Three tabs rather than one long scroll, because the three audiences differ: a
 * requester checks the lines, an approver checks the chain and their own step,
 * and an auditor checks the history. The approved/ordered split is shown per
 * line because that is the only way to answer "how much of what was approved is
 * still un-ordered".
 */
export function RequisitionDetailsSheet({
  requisitionId,
  open,
  onOpenChange,
}: RequisitionDetailsSheetProps) {
  const { data, isLoading, error } = useGetRequisitionQuery(requisitionId ?? 0, {
    skip: !requisitionId || !open,
  });

  // The list page already fetches the active branch list with this exact query
  // key, so this resolves from the RTK Query cache rather than the network. It
  // lets the sheet label the branch by name instead of a raw id.
  const { data: branchData } = useGetBranchesQuery(
    { page: 0, size: 100, status: "ACTIVE", sort: "branchName,asc" },
    { skip: !open }
  );
  const branchNameById = useMemo(
    () =>
      new Map(
        (branchData?.content ?? []).map((branch) => [branch.id, branch.branchName])
      ),
    [branchData]
  );

  const requisition = data as Requisition | undefined;
  const branchName = requisition
    ? branchNameById.get(requisition.branchId)
    : undefined;

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent className="w-full overflow-y-auto sm:max-w-3xl">
        {/* Sticky header so the title and the sheet's close button stay
            visible while the body scrolls underneath. */}
        <SheetHeader className="sticky top-0 z-10 border-b border-border bg-background">
          <SheetTitle>{requisition?.prNumber ?? "Requisition"}</SheetTitle>
          <SheetDescription>
            {requisition
              ? `Raised on ${requisition.requestDate}${
                  requisition.requestDateNp
                    ? ` (${requisition.requestDateNp} BS)`
                    : ""
                } · ${branchName ?? `Branch #${requisition.branchId}`}`
              : "Loading requisition details..."}
          </SheetDescription>
        </SheetHeader>

        {isLoading && (
          <div className="space-y-3 px-4">
            <Skeleton className="h-5 w-48" />
            <Skeleton className="h-4 w-64" />
            <Skeleton className="h-28 w-full" />
            <Skeleton className="h-64 w-full" />
          </div>
        )}

        {error && (
          <p className="px-4 text-sm text-destructive">
            Could not load this requisition.
          </p>
        )}

        {requisition && (
          <div className="px-4 pb-6">
            <div className="mb-4 flex flex-wrap gap-2">
              <RequisitionStatusBadge status={requisition.status} />
              <RequisitionPriorityBadge priority={requisition.priority} />
            </div>

            <Tabs defaultValue="lines">
              <TabsList className="w-full">
                <TabsTrigger value="lines" className="flex-1">
                  Lines
                </TabsTrigger>
                <TabsTrigger value="approvals" className="flex-1">
                  Approvals
                </TabsTrigger>
                <TabsTrigger value="history" className="flex-1">
                  History
                </TabsTrigger>
              </TabsList>

              <TabsContent value="lines" className="mt-4 space-y-4">
                <div className="rounded-lg border border-border px-3 py-1">
                  <DetailRow label="Currency" value={requisition.currency} />
                  <DetailRow
                    label="Estimated Total"
                    value={formatMoney(
                      requisition.estimatedTotalAmount,
                      requisition.currency
                    )}
                  />
                  <DetailRow
                    label="Approved Total"
                    value={formatMoney(
                      requisition.approvedTotalAmount,
                      requisition.currency
                    )}
                  />
                  <DetailRow
                    label="Ordered Total"
                    value={formatMoney(
                      requisition.orderedTotalAmount,
                      requisition.currency
                    )}
                  />
                  <DetailRow
                    label="Outstanding"
                    value={formatMoney(
                      requisition.outstandingValue,
                      requisition.currency
                    )}
                  />
                  <DetailRow
                    label="Required By"
                    value={requisition.requiredByDate ?? "—"}
                  />
                  {requisition.justification && (
                    <DetailRow
                      label="Justification"
                      value={requisition.justification}
                    />
                  )}
                  {requisition.warehouseName && (
                    <DetailRow
                      label="Receiving Warehouse"
                      value={`${requisition.warehouseName} (${requisition.warehouseCode})`}
                    />
                  )}
                </div>

                <div className="overflow-hidden rounded-lg border border-border">
                  <Table>
                    <TableHeader className="bg-muted/40">
                      <TableRow>
                        <TableHead>#</TableHead>
                        <TableHead>Item</TableHead>
                        <TableHead className="text-right">Requested</TableHead>
                        <TableHead className="text-right">Approved</TableHead>
                        <TableHead className="text-right">Ordered</TableHead>
                        <TableHead>Status</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {requisition.lines.map((line) => (
                        <TableRow key={line.id}>
                          <TableCell className="text-sm text-muted-foreground">
                            {line.lineNumber}
                          </TableCell>
                          <TableCell>
                            <p className="font-medium text-foreground">
                              {line.itemName}
                            </p>
                            <p className="text-xs text-muted-foreground">
                              {line.itemCode} · {line.uomCode}
                            </p>
                          </TableCell>
                          <TableCell className="text-right text-sm">
                            {formatQuantity(line.requestedQuantity)}
                          </TableCell>
                          <TableCell className="text-right text-sm">
                            {formatQuantity(line.approvedQuantity)}
                          </TableCell>
                          <TableCell className="text-right text-sm">
                            {formatQuantity(line.orderedQuantity)}
                          </TableCell>
                          <TableCell>
                            <RequisitionLineStatusBadge status={line.status} />
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </div>
              </TabsContent>

              <TabsContent value="approvals" className="mt-4 space-y-3">
                {requisition.approvals.length === 0 ? (
                  <p className="py-8 text-center text-sm text-muted-foreground">
                    Not submitted yet. The approval chain is built when the
                    requisition is submitted.
                  </p>
                ) : (
                  requisition.approvals.map((step) => (
                    <div
                      key={step.id}
                      className="rounded-lg border border-border p-3"
                    >
                      <div className="flex items-center justify-between">
                        <p className="text-sm font-medium text-foreground">
                          Step {step.sequenceNo}:{" "}
                          {step.approverRole ?? `User #${step.approverId}`}
                        </p>
                        <span className="text-xs font-medium text-muted-foreground">
                          {step.action.replace(/_/g, " ")}
                        </span>
                      </div>
                      {step.amountThreshold !== null && (
                        <p className="mt-1 text-xs text-muted-foreground">
                          Applies from{" "}
                          {formatMoney(step.amountThreshold, requisition.currency)}
                        </p>
                      )}
                      {step.actedAt && (
                        <p className="mt-1 text-xs text-muted-foreground">
                          Decided by {step.actedBy} on{" "}
                          {formatTimestamp(step.actedAt)}
                        </p>
                      )}
                      {step.comments && (
                        <p className="mt-2 rounded bg-muted/50 p-2 text-xs text-foreground">
                          {step.comments}
                        </p>
                      )}
                    </div>
                  ))
                )}
              </TabsContent>

              <TabsContent value="history" className="mt-4 space-y-3">
                {requisition.statusHistory.length === 0 ? (
                  <p className="py-8 text-center text-sm text-muted-foreground">
                    No transitions recorded yet.
                  </p>
                ) : (
                  requisition.statusHistory.map((entry) => (
                    <div
                      key={entry.id}
                      className="border-l-2 border-border pl-3"
                    >
                      <p className="text-sm text-foreground">
                        {entry.fromStatus
                          ? `${formatRequisitionStatus(entry.fromStatus)} → ${formatRequisitionStatus(entry.toStatus)}`
                          : `Created as ${formatRequisitionStatus(entry.toStatus)}`}
                      </p>
                      <p className="text-xs text-muted-foreground">
                        {formatTimestamp(entry.changedAt)}
                        {entry.changedBy ? ` by ${entry.changedBy}` : ""}
                      </p>
                      {entry.reason && (
                        <p className="mt-1 text-xs text-muted-foreground">
                          {entry.reason}
                        </p>
                      )}
                    </div>
                  ))
                )}
              </TabsContent>
            </Tabs>
          </div>
        )}
      </SheetContent>
    </Sheet>
  );
}
