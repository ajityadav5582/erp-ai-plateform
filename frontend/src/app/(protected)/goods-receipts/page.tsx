"use client";

import { useEffect, useMemo, useState } from "react";
import { useSearchParams, useRouter } from "next/navigation";
import { Eye, MoreVertical, PackageOpen, Search } from "lucide-react";
import {
  useGetGoodsReceiptsQuery,
  type GoodsReceipt,
} from "@/services/goods-receipt.service";
import { GoodsReceiptStatusBadge } from "@/components/procurement/procurement-status-badge";
import { ListPageHeader } from "@/components/common/list-page-header";
import { ListToolbar } from "@/components/common/list-toolbar";
import { ListPagination } from "@/components/common/list-pagination";
import {
  TableEmptyRow,
  TableErrorRow,
  TableSkeletonRows,
} from "@/components/common/table-state";
import { formatDate, formatMoney } from "@/lib/procurement-format";

import { Button } from "@/components/ui/button";
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

/**
 * Code-split modals. These components stay closed for the whole time this
 * list is on screen, so keeping them out of the initial payload lets the page
 * become interactive without parsing them. `loading: () => null` matches the
 * closed state, so opening one causes no layout shift.
 */
const GoodsReceiptDetailsSheet = dynamic(
  () =>
    import("@/components/procurement/goods-receipt-details-sheet").then((mod) => ({
      default: mod.GoodsReceiptDetailsSheet,
    })),
  { ssr: false, loading: () => null }
);

const COLUMNS = 8;

/**
 * Goods receipt notes: the deliveries recorded against purchase orders.
 *
 * A receipt cannot be raised from a supplier or a date alone - it is always "this
 * arrived against that order". Since orders are only fetchable by id, recording starts
 * from an order id, and the dialog then shows that order's outstanding lines so the
 * operator is choosing from the real demand rather than retyping it.
 *
 * That lookup moved out of its own card and into the toolbar's search field: it was a
 * one-off navigation, not a filter over this list, and having a second search box with
 * different rules directly above a third one is the inconsistency that made this screen
 * feel unlike its neighbours.
 */
export default function GoodsReceiptsPage() {
  const router = useRouter();
  const searchParams = useSearchParams();

  const [page, setPage] = useState(Number(searchParams.get("page") ?? 0));
  const [pageSize, setPageSize] = useState(Number(searchParams.get("size") ?? 10));
  const [searchTerm, setSearchTerm] = useState(searchParams.get("q") ?? "");
  const [detailsId, setDetailsId] = useState<number | null>(null);

  /** The order the operator wants to receive against, as typed. */
  const [orderLookupError, setOrderLookupError] = useState<string | null>(null);

  const listParams = useMemo(
    () => ({ page, size: pageSize, sort: "receiptDate,desc" }),
    [page, pageSize]
  );

  // Keep the URL in sync with the active filters so views are shareable/bookmarkable.
  useEffect(() => {
    const params = new URLSearchParams();
    if (page > 0) params.set("page", String(page));
    if (pageSize !== 10) params.set("size", String(pageSize));
    if (searchTerm.trim()) params.set("q", searchTerm.trim());
    const qs = params.toString();
    router.replace(qs ? `?${qs}` : "", { scroll: false });
  }, [page, pageSize, searchTerm, router]);

  const {
    data,
    isLoading,
    isFetching,
    isError,
    refetch,
  } = useGetGoodsReceiptsQuery(listParams);

  // Memoised because `?? []` builds a fresh array on every render, which would make the
  // filtering below recompute on every render for no reason.
  const receipts = useMemo(() => data?.content ?? [], [data]);

  const normalizedTerm = searchTerm.trim().toLowerCase();

  /**
   * A bare number is an order id, and the order is only reachable by that id - so
   * typing one navigates rather than filtering. Anything else narrows the loaded page,
   * which is all the backend supports: its `Pageable` binder reads `page`, `size` and
   * `sort` and silently drops a `search` parameter.
   */
  const visibleReceipts = useMemo(() => {
    const term = normalizedTerm;
    if (!term) return receipts;

    if (/^\d+$/.test(term)) {
      const id = Number(term);
      return receipts.filter(
        (receipt) => receipt.id === id || String(receipt.id).includes(term)
      );
    }

    return receipts.filter(
      (receipt) =>
        receipt.grnNumber.toLowerCase().includes(term) ||
        receipt.poNumber.toLowerCase().includes(term) ||
        receipt.supplierName.toLowerCase().includes(term) ||
        receipt.supplierCode.toLowerCase().includes(term)
    );
  }, [receipts, normalizedTerm]);

  /**
   * Resolves a typed order id into an opened order.
   *
   * The receipt dialog needs the order's lines to present what is outstanding, so
   * navigating to `/purchase-orders?id=…` is what hands that data over. Failing here
   * means the dialog never opens against an order that does not exist in this company.
   */
  const handleFindOrder = () => {
    const term = searchTerm.trim();
    if (!term) {
      setOrderLookupError("Enter the purchase order id first");
      return;
    }
    if (!/^\d+$/.test(term)) {
      setOrderLookupError(
        "Orders are fetched by their numeric id. Copy it from the Purchase Orders screen."
      );
      return;
    }
    setOrderLookupError(null);
    router.push(`/purchase-orders?id=${term}`);
  };

  const handlePageSizeChange = (size: number) => {
    setPageSize(size);
    setPage(0);
  };

  return (
    <div className="flex flex-1 flex-col space-y-4 p-3 sm:space-y-6 sm:p-6 lg:p-8">
      <ListPageHeader
        title="Goods Receipts"
        description="Record deliveries as they arrive and see what has been received against each order."
        count={data?.totalElements}
        countLabel="receipts"
        actions={
          <Button
            variant="outline"
            size="sm"
            className="h-9 gap-1.5"
            onClick={handleFindOrder}
            disabled={!normalizedTerm}
            title="Open the purchase order with this id to record a delivery"
          >
            Record delivery
          </Button>
        }
      />

      <ListToolbar
        searchValue={searchTerm}
        onSearchChange={(value) => {
          setSearchTerm(value);
          setOrderLookupError(null);
        }}
        searchPlaceholder="Filter receipts by GRN, order, or supplier — or type an order id"
        isFetching={isFetching}
        onRefresh={() => {
          setPage(0);
          refetch();
        }}
      />

      {orderLookupError && (
        <p className="-mt-3 text-sm text-destructive">{orderLookupError}</p>
      )}

      {normalizedTerm && !orderLookupError && (
        <p className="-mt-3 text-sm text-muted-foreground">
          Showing {visibleReceipts.length} of {receipts.length} receipt
          {receipts.length === 1 ? "" : "s"} on this page.
          {visibleReceipts.length === 0 &&
            " No match on this page - try another page or clear the filter."}
        </p>
      )}

      <div className="overflow-hidden rounded-xl border border-border bg-card shadow-sm">
        <Table>
          <TableHeader className="bg-muted/40">
            <TableRow>
              <TableHead>GRN</TableHead>
              <TableHead>Order</TableHead>
              <TableHead>Supplier</TableHead>
              <TableHead>Receipt date</TableHead>
              <TableHead>Challan</TableHead>
              <TableHead>Status</TableHead>
              <TableHead className="text-right">Value</TableHead>
              <TableHead className="text-right">Actions</TableHead>
            </TableRow>
          </TableHeader>

          <TableBody>
            {isLoading ? (
              <TableSkeletonRows columns={COLUMNS} />
            ) : isError ? (
              <TableErrorRow
                columns={COLUMNS}
                title="Could not load goods receipts"
                description="The goods receipt list could not be loaded."
                onRetry={refetch}
              />
            ) : receipts.length === 0 ? (
              <TableEmptyRow
                icon={PackageOpen}
                title="No deliveries recorded yet"
                description="Once goods arrive against a purchase order, the receipt note appears here."
                columns={COLUMNS}
              />
            ) : visibleReceipts.length === 0 ? (
              <TableEmptyRow
                icon={Search}
                title="No receipt matches that search"
                description="Nothing on this page matches. Try a different term, clear the filter, or look at another page."
                columns={COLUMNS}
                action={
                  <Button variant="link" size="sm" onClick={() => setSearchTerm("")}>
                    Clear search
                  </Button>
                }
              />
            ) : (
              visibleReceipts.map((receipt: GoodsReceipt) => (
                <TableRow
                  key={receipt.id}
                  className="group transition-colors hover:bg-muted/50"
                >
                  <TableCell>
                    <p className="font-mono text-sm font-medium text-foreground">
                      {receipt.grnNumber}
                    </p>
                  </TableCell>
                  <TableCell className="font-mono text-sm text-muted-foreground">
                    {receipt.poNumber}
                  </TableCell>
                  <TableCell>
                    <span className="block text-sm text-foreground">
                      {receipt.supplierName}
                    </span>
                    <span className="block text-xs text-muted-foreground">
                      {receipt.supplierCode}
                    </span>
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {formatDate(receipt.receiptDate)}
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {receipt.deliveryChallanNo ?? "—"}
                  </TableCell>
                  <TableCell>
                    <div className="flex flex-col gap-1">
                      <GoodsReceiptStatusBadge status={receipt.status} />
                      {receipt.stockPostStatus === "FAILED" && (
                        <span className="text-xs font-medium text-destructive">
                          Stock post failed
                        </span>
                      )}
                      {receipt.stockPostStatus === "PENDING" && (
                        <span className="text-xs text-muted-foreground">
                          Stock pending
                        </span>
                      )}
                    </div>
                  </TableCell>
                  <TableCell className="text-right text-sm font-medium text-foreground">
                    {formatMoney(receipt.receivedAmount, receipt.currency)}
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
                        <DropdownMenuItem onClick={() => setDetailsId(receipt.id)}>
                          <Eye className="mr-2 size-4 text-muted-foreground" />
                          View details
                        </DropdownMenuItem>
                      </DropdownMenuContent>
                    </DropdownMenu>
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
          onPageSizeChange={handlePageSizeChange}
          totalPages={data?.totalPages ?? 1}
          totalElements={data?.totalElements ?? 0}
          itemLabel="receipts"
          isFetching={isFetching}
        />
      </div>

      <GoodsReceiptDetailsSheet
        goodsReceiptId={detailsId}
        open={detailsId !== null}
        onOpenChange={(open) => {
          if (!open) setDetailsId(null);
        }}
      />
    </div>
  );
}
