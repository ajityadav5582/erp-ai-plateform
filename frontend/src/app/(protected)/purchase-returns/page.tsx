"use client";

import { useMemo, useState } from "react";
import { useSearchParams, useRouter } from "next/navigation";
import {
  CheckCircle2,
  Eye,
  FileCheck2,
  MoreVertical,
  Plus,
  RefreshCw,
  RotateCcw,
} from "lucide-react";
import { toast } from "sonner";
import {
  useApprovePurchaseReturnMutation,
  useGetPurchaseReturnsQuery,
  usePostPurchaseReturnMutation,
  useRetryStockPostMutation,
  type PurchaseReturnStatus,
} from "@/services/purchase-return.service";
import { getApiErrorMessage } from "@/services/error-handler";
import { formatDate, formatMoney } from "@/lib/procurement-format";
import { PurchaseReturnStatusBadge } from "@/components/procurement/procurement-status-badge";
import { ListPageHeader } from "@/components/common/list-page-header";
import { ListToolbar } from "@/components/common/list-toolbar";
import { ListPagination } from "@/components/common/list-pagination";
import { ScopeTabs, type ScopeTab } from "@/components/common/scope-tabs";
import { TableEmptyRow, TableSkeletonRows } from "@/components/common/table-state";

import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
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
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import dynamic from "next/dynamic";

const CreatePurchaseReturnDialog = dynamic(
  () =>
    import("@/components/procurement/create-purchase-return-dialog").then((mod) => ({
      default: mod.CreatePurchaseReturnDialog,
    })),
  { ssr: false, loading: () => null }
);

const PurchaseReturnDetailsSheet = dynamic(
  () =>
    import("@/components/procurement/purchase-return-details-sheet").then((mod) => ({
      default: mod.PurchaseReturnDetailsSheet,
    })),
  { ssr: false, loading: () => null }
);

type ReturnScope = "all" | "draft" | "approved" | "posted" | "cancelled";

const SCOPE_TABS: ScopeTab[] = [
  { value: "all", label: "All Returns" },
  { value: "draft", label: "Draft" },
  { value: "approved", label: "Approved" },
  { value: "posted", label: "Posted" },
  { value: "cancelled", label: "Cancelled" },
];

export default function PurchaseReturnsPage() {
  const router = useRouter();
  const searchParams = useSearchParams();

  const page = Number(searchParams.get("page") ?? "0");
  const [searchTerm, setSearchTerm] = useState("");
  const [activeScope, setActiveScope] = useState<ReturnScope>("all");

  const [createDialogOpen, setCreateDialogOpen] = useState(
    searchParams.get("action") === "new"
  );
  const [selectedReturnId, setSelectedReturnId] = useState<number | null>(null);
  const [detailsSheetOpen, setDetailsSheetOpen] = useState(false);

  const queryStatus: PurchaseReturnStatus | undefined =
    activeScope === "draft"
      ? "DRAFT"
      : activeScope === "approved"
      ? "APPROVED"
      : activeScope === "posted"
      ? "POSTED"
      : activeScope === "cancelled"
      ? "CANCELLED"
      : undefined;

  const { data, isLoading, isFetching, refetch } = useGetPurchaseReturnsQuery({
    page,
    size: 20,
    status: queryStatus,
    search: searchTerm.trim() || undefined,
  });

  const [approveReturn] = useApprovePurchaseReturnMutation();
  const [postReturn] = usePostPurchaseReturnMutation();
  const [retryStock] = useRetryStockPostMutation();

  const rawContent = data?.content;
  const returns = useMemo(() => rawContent ?? [], [rawContent]);
  const totalElements = data?.totalElements ?? 0;
  const totalPages = data?.totalPages ?? 1;

  // Summary statistics for active page
  const summary = useMemo(() => {
    let totalVal = 0;
    let totalVat = 0;
    let postedCount = 0;

    for (const r of returns) {
      if (r.status !== "CANCELLED") {
        totalVal += r.totalAmount;
        totalVat += r.taxAmount;
      }
      if (r.status === "POSTED") postedCount++;
    }

    return { totalVal, totalVat, postedCount };
  }, [returns]);

  const handleOpenDetails = (id: number) => {
    setSelectedReturnId(id);
    setDetailsSheetOpen(true);
  };

  const handleApprove = async (id: number) => {
    try {
      await approveReturn(id).unwrap();
      toast.success("Purchase return approved.");
      refetch();
    } catch (err) {
      toast.error(getApiErrorMessage(err));
    }
  };

  const handlePost = async (id: number) => {
    try {
      await postReturn(id).unwrap();
      toast.success("Debit note posted successfully! Stock dispatched and AP adjusted.");
      refetch();
    } catch (err) {
      toast.error(getApiErrorMessage(err));
    }
  };

  const handleRetryStock = async (id: number) => {
    try {
      await retryStock(id).unwrap();
      toast.success("Stock dispatch retried successfully.");
      refetch();
    } catch (err) {
      toast.error(getApiErrorMessage(err));
    }
  };

  return (
    <div className="flex flex-col gap-5 p-4 sm:p-6 lg:p-8">
      {/* Page Header */}
      <ListPageHeader
        title="Purchase Returns & Debit Notes"
        description="Manage returns to vendors (RTV), issue statutory debit notes, reverse accounts payable liability, and adjust input VAT."
        count={totalElements}
        countLabel="Returns"
        actions={
          <Button
            onClick={() => setCreateDialogOpen(true)}
            size="sm"
            className="gap-1.5 bg-destructive text-destructive-foreground hover:bg-destructive/90"
          >
            <Plus className="size-4" />
            <span>Create Return / Debit Note</span>
          </Button>
        }
      />

      {/* KPI Highlights Bar */}
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        <div className="rounded-xl border border-border/80 bg-card p-3.5 shadow-xs">
          <span className="text-[11px] font-semibold text-muted-foreground uppercase">
            Total Return Notes
          </span>
          <p className="mt-1 text-xl font-bold font-mono text-foreground">
            {totalElements}
          </p>
        </div>
        <div className="rounded-xl border border-border/80 bg-card p-3.5 shadow-xs">
          <span className="text-[11px] font-semibold text-muted-foreground uppercase">
            Total Value Returned
          </span>
          <p className="mt-1 text-xl font-bold font-mono text-destructive">
            {formatMoney(summary.totalVal, "NPR")}
          </p>
        </div>
        <div className="rounded-xl border border-border/80 bg-card p-3.5 shadow-xs">
          <span className="text-[11px] font-semibold text-muted-foreground uppercase">
            Reversed Input VAT
          </span>
          <p className="mt-1 text-xl font-bold font-mono text-foreground">
            {formatMoney(summary.totalVat, "NPR")}
          </p>
        </div>
        <div className="rounded-xl border border-border/80 bg-card p-3.5 shadow-xs">
          <span className="text-[11px] font-semibold text-muted-foreground uppercase">
            Posted & Dispatched
          </span>
          <p className="mt-1 text-xl font-bold font-mono text-emerald-600 dark:text-emerald-400">
            {summary.postedCount} Notes
          </p>
        </div>
      </div>

      {/* Scope Tabs & Search Toolbar */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <ScopeTabs
          tabs={SCOPE_TABS}
          value={activeScope}
          onValueChange={(val) => setActiveScope(val as ReturnScope)}
          ariaLabel="Filter purchase returns"
        />
        <ListToolbar
          searchPlaceholder="Search by debit note, invoice, or vendor…"
          searchValue={searchTerm}
          onSearchChange={setSearchTerm}
        />
      </div>

      {/* Main Returns Table */}
      <div className="overflow-hidden rounded-xl border border-border bg-card shadow-xs">
        <Table>
          <TableHeader>
            <TableRow className="bg-muted/50 hover:bg-muted/50">
              <TableHead className="w-[180px]">Debit Note No.</TableHead>
              <TableHead className="w-[130px]">Return Date</TableHead>
              <TableHead>Supplier</TableHead>
              <TableHead className="w-[150px]">Source Invoice</TableHead>
              <TableHead>Statutory Reason</TableHead>
              <TableHead className="text-right w-[140px]">Debit Amount</TableHead>
              <TableHead className="w-[120px] text-center">Status</TableHead>
              <TableHead className="w-[130px] text-center">Stock Dispatch</TableHead>
              <TableHead className="w-[50px] text-center"></TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading || isFetching ? (
              <TableSkeletonRows columns={9} rows={8} />
            ) : returns.length === 0 ? (
              <TableEmptyRow
                columns={9}
                title={
                  searchTerm
                    ? `No purchase returns found matching "${searchTerm}".`
                    : "No purchase returns or debit notes recorded yet."
                }
                description={
                  searchTerm
                    ? "Try clearing search or changing filters."
                    : "Click 'Create Return / Debit Note' to issue your first vendor return."
                }
              />
            ) : (
              returns.map((ret) => (
                <TableRow
                  key={ret.id}
                  onClick={() => handleOpenDetails(ret.id)}
                  className="cursor-pointer hover:bg-muted/40 transition-colors"
                >
                  {/* Debit note number */}
                  <TableCell className="font-mono font-semibold text-foreground">
                    <div className="flex items-center gap-1.5">
                      <RotateCcw className="size-3.5 text-destructive" />
                      <span>{ret.debitNoteNumber}</span>
                    </div>
                  </TableCell>

                  {/* Return Date */}
                  <TableCell className="text-xs">
                    <div>{formatDate(ret.returnDate)}</div>
                    <div className="text-[10px] text-muted-foreground font-mono">
                      BS: {ret.returnDateNp || "—"}
                    </div>
                  </TableCell>

                  {/* Supplier */}
                  <TableCell className="text-xs">
                    <div className="font-medium text-foreground">{ret.supplierName}</div>
                    <div className="text-[10px] text-muted-foreground font-mono">
                      PAN: {ret.supplierPanNumber || "—"}
                    </div>
                  </TableCell>

                  {/* Source Invoice */}
                  <TableCell className="text-xs font-mono">
                    <div className="text-primary font-medium">{ret.invoiceNumber}</div>
                    {ret.grnNumber && (
                      <div className="text-[10px] text-muted-foreground">
                        GRN: {ret.grnNumber}
                      </div>
                    )}
                  </TableCell>

                  {/* Reason */}
                  <TableCell className="text-xs text-muted-foreground truncate max-w-[200px]">
                    {ret.reason}
                  </TableCell>

                  {/* Amount */}
                  <TableCell className="text-right font-mono font-bold text-destructive">
                    {formatMoney(ret.totalAmount, ret.currency)}
                  </TableCell>

                  {/* Status */}
                  <TableCell className="text-center">
                    <PurchaseReturnStatusBadge status={ret.status} />
                  </TableCell>

                  {/* Stock status */}
                  <TableCell className="text-center">
                    {ret.status === "POSTED" ? (
                      <Badge
                        variant="outline"
                        className={`text-[10px] font-mono ${
                          ret.stockPostStatus === "POSTED"
                            ? "border-emerald-500/30 text-emerald-600 bg-emerald-500/10"
                            : ret.stockPostStatus === "FAILED"
                            ? "border-red-500/30 text-destructive bg-red-500/10"
                            : "border-amber-500/30 text-amber-600 bg-amber-500/10"
                        }`}
                      >
                        {ret.stockPostStatus}
                      </Badge>
                    ) : (
                      <span className="text-muted-foreground text-xs">—</span>
                    )}
                  </TableCell>

                  {/* Actions Dropdown */}
                  <TableCell
                    className="text-center"
                    onClick={(e) => e.stopPropagation()}
                  >
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button variant="ghost" size="icon" className="size-8">
                          <MoreVertical className="size-4" />
                          <span className="sr-only">Open actions</span>
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end">
                        <DropdownMenuItem onClick={() => handleOpenDetails(ret.id)}>
                          <Eye className="mr-2 size-4" />
                          <span>View Details</span>
                        </DropdownMenuItem>
                        {ret.status === "DRAFT" && (
                          <>
                            <DropdownMenuItem onClick={() => handleApprove(ret.id)}>
                              <FileCheck2 className="mr-2 size-4" />
                              <span>Approve Return</span>
                            </DropdownMenuItem>
                            <DropdownMenuItem onClick={() => handlePost(ret.id)}>
                              <CheckCircle2 className="mr-2 size-4 text-destructive" />
                              <span className="text-destructive font-medium">Post Debit Note</span>
                            </DropdownMenuItem>
                          </>
                        )}
                        {ret.status === "APPROVED" && (
                          <DropdownMenuItem onClick={() => handlePost(ret.id)}>
                            <CheckCircle2 className="mr-2 size-4 text-destructive" />
                            <span className="text-destructive font-medium">Post Debit Note</span>
                          </DropdownMenuItem>
                        )}
                        {ret.status === "POSTED" && ret.stockPostStatus === "FAILED" && (
                          <DropdownMenuItem onClick={() => handleRetryStock(ret.id)}>
                            <RefreshCw className="mr-2 size-4" />
                            <span>Retry Stock Dispatch</span>
                          </DropdownMenuItem>
                        )}
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
      <ListPagination
        page={page}
        pageSize={20}
        totalPages={totalPages}
        totalElements={totalElements}
        itemLabel="returns"
        onPageChange={(p) => {
          const params = new URLSearchParams(searchParams.toString());
          params.set("page", String(p));
          router.push(`?${params.toString()}`);
        }}
      />

      {/* Modals */}
      <CreatePurchaseReturnDialog
        open={createDialogOpen}
        onOpenChange={setCreateDialogOpen}
        onCreated={() => refetch()}
      />

      <PurchaseReturnDetailsSheet
        returnId={selectedReturnId}
        open={detailsSheetOpen}
        onOpenChange={setDetailsSheetOpen}
        onUpdated={() => refetch()}
      />
    </div>
  );
}
