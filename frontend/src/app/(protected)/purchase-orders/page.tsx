"use client";

import { Suspense, useEffect, useMemo, useState } from "react";
import dynamic from "next/dynamic";
import Link from "next/link";
import {
  Eye,
  MoreVertical,
  PackageCheck,
  PackageOpen,
  Search,
  ShoppingCart,
  Truck,
} from "lucide-react";
import { useSearchParams } from "next/navigation";
import {
  useGetRequisitionsQuery,
  type RequisitionListResponse,
} from "@/services/purchase-requisition.service";
import {
  useGetPurchaseOrdersQuery,
  type GetPurchaseOrdersParams,
  type PurchaseOrder,
  type PurchaseOrderStatus,
} from "@/services/purchase-order.service";
import { RequisitionStatusBadge } from "@/components/requisitions/requisition-status-badge";
import { PurchaseOrderStatusBadge } from "@/components/procurement/procurement-status-badge";
import { ListPageHeader } from "@/components/common/list-page-header";
import { ListToolbar } from "@/components/common/list-toolbar";
import { ListPagination } from "@/components/common/list-pagination";
import { PageLoader } from "@/components/common/page-loading";
import { ScopeTabs } from "@/components/common/scope-tabs";
import {
  TableEmptyRow,
  TableErrorRow,
  TableSkeletonRows,
} from "@/components/common/table-state";
import { formatMoney, formatDate } from "@/lib/procurement-format";

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
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

/**
 * These three weigh ~44 kB of JS combined and stay closed for the whole time
 * this list is on screen. Code-splitting them keeps them out of the initial
 * payload, so the order list becomes interactive without parsing them.
 * `loading: () => null` matches the closed state, so opening one causes no
 * layout shift.
 */
const ConvertToPurchaseOrderDialog = dynamic(
  () =>
    import("@/components/procurement/convert-to-purchase-order-dialog").then(
      (mod) => ({ default: mod.ConvertToPurchaseOrderDialog })
    ),
  { ssr: false, loading: () => null }
);

const PurchaseOrderDetailsSheet = dynamic(
  () =>
    import("@/components/procurement/purchase-order-details-sheet").then((mod) => ({
      default: mod.PurchaseOrderDetailsSheet,
    })),
  { ssr: false, loading: () => null }
);

const RecordGoodsReceiptDialog = dynamic(
  () =>
    import("@/components/procurement/record-goods-receipt-dialog").then((mod) => ({
      default: mod.RecordGoodsReceiptDialog,
    })),
  { ssr: false, loading: () => null }
);

/**
 * Statuses in which a delivery can no longer be booked against the order. Offering
 * "record receipt" on a closed or cancelled order would only produce a rejection
 * from the server after the operator has already typed the delivery out.
 */
const RECEIPT_BLOCKED_STATUSES: PurchaseOrderStatus[] = ["REJECTED", "CANCELLED", "CLOSED"];

type OrderScope = "orders" | "awaiting";

/**
 * Purchase orders: the order book, and the approved requisitions waiting to become one.
 *
 * Two queues, because a buyer does two different jobs here:
 *
 * 1. **Orders** - every order the company has issued, paged from `GET /purchase-orders`.
 *    This is the screen people actually spend their day on: what did we commit to
 *    suppliers, what has arrived, and what is still owed.
 * 2. **Awaiting an order** - approved requisitions not yet converted. This is the work
 *    queue: an order takes its item, quantity, price and tax from the requisition, so
 *    raising one is a single step.
 *
 * They were two stacked tables and are now two scopes on one table, matching how
 * requisitions and invoices already switch between queues. Both queues answer "what
 * should I do next", and stacking them meant the page grew without bound while the
 * second table sat below the fold on every screen short of a large monitor.
 *
 * **Why search filters what is loaded rather than asking the server.** The backend
 * `Pageable` binder only reads `page`, `size` and `sort`, so a `search` parameter would
 * be accepted and silently dropped - the same trap that made the supplier picker appear
 * broken earlier. Filtering the current page client-side is honest about what it covers,
 * and the toolbar says how many rows are hidden.
 */
export default function PurchaseOrdersPage() {
  // `useSearchParams` opts a route out of static prerendering, so the reading part of
  // this page sits behind a Suspense boundary with a matching fallback. Without it the
  // production build fails on a route that otherwise renders fine.
  return (
    <Suspense fallback={<PurchaseOrdersFallback />}>
      <PurchaseOrdersView />
    </Suspense>
  );
}

function PurchaseOrdersFallback() {
  return <PageLoader showTabs columns={7} label="Loading purchase orders…" />;
}

function PurchaseOrdersView() {
  const searchParams = useSearchParams();

  const [scope, setScope] = useState<OrderScope>("orders");
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [searchTerm, setSearchTerm] = useState("");

  const [convertTarget, setConvertTarget] =
    useState<RequisitionListResponse | null>(null);
  const [detailsId, setDetailsId] = useState<number | null>(null);
  /** The order a delivery is being recorded against. */
  const [recordOrder, setRecordOrder] = useState<PurchaseOrder | null>(null);

  /**
   * Opens an order passed in the URL.
   *
   * The goods receipts screen links here as `/purchase-orders?id=42`, because an order
   * can only be fetched by its numeric id and the receipt screen has nothing else to go
   * on. Honouring that link is what makes "record a delivery" reachable from the screen
   * an operator lands on first. A non-numeric or absent id is ignored rather than
   * treated as an error, so an ordinary visit to `/purchase-orders` still works.
   */
  const linkedOrderId = searchParams.get("id");
  useEffect(() => {
    if (linkedOrderId && /^\d+$/.test(linkedOrderId)) {
      setDetailsId(Number(linkedOrderId));
    }
  }, [linkedOrderId]);

  const orderParams = useMemo<GetPurchaseOrdersParams>(
    () => ({ page, size: pageSize, sort: "orderDate,desc" }),
    [page, pageSize]
  );

  const {
    data: orderData,
    isLoading: ordersLoading,
    isFetching: ordersFetching,
    isError: ordersError,
    refetch: refetchOrders,
  } = useGetPurchaseOrdersQuery(orderParams, { skip: scope !== "orders" });

  const requisitionParams = useMemo(
    () => ({
      page,
      size: pageSize,
      sort: "requestDate,desc",
      status: "APPROVED" as const,
    }),
    [page, pageSize]
  );

  const {
    data: requisitionData,
    isLoading: requisitionsLoading,
    isFetching: requisitionsFetching,
    isError: requisitionsError,
    refetch: refetchRequisitions,
  } = useGetRequisitionsQuery(requisitionParams, { skip: scope !== "awaiting" });

  // Memoised because `?? []` builds a fresh array on every render, which would make the
  // filtering useMemo below recompute on every render for no reason.
  const orders = useMemo(() => orderData?.content ?? [], [orderData]);
  const requisitions = useMemo(
    () => requisitionData?.content ?? [],
    [requisitionData]
  );

  const normalizedTerm = searchTerm.trim().toLowerCase();

  const visibleOrders = useMemo(() => {
    if (!normalizedTerm) return orders;
    return orders.filter(
      (order) =>
        order.poNumber.toLowerCase().includes(normalizedTerm) ||
        order.supplierName.toLowerCase().includes(normalizedTerm) ||
        order.supplierCode.toLowerCase().includes(normalizedTerm)
    );
  }, [orders, normalizedTerm]);

  const visibleRequisitions = useMemo(() => {
    if (!normalizedTerm) return requisitions;
    return requisitions.filter((requisition) =>
      requisition.prNumber.toLowerCase().includes(normalizedTerm)
    );
  }, [requisitions, normalizedTerm]);

  const isAwaiting = scope === "awaiting";
  const rows = isAwaiting ? visibleRequisitions : visibleOrders;
  const loadedRows = isAwaiting ? requisitions : orders;
  const activeData = isAwaiting ? requisitionData : orderData;
  const isLoading = isAwaiting ? requisitionsLoading : ordersLoading;
  const isFetching = isAwaiting ? requisitionsFetching : ordersFetching;
  const isError = isAwaiting ? requisitionsError : ordersError;

  const handleConverted = (order: PurchaseOrder) => {
    // Open the new draft straight away: the buyer has just committed spend and needs
    // to see what was actually created, especially since only they chose the supplier.
    setDetailsId(order.id);
  };

  const handleScopeChange = (next: string) => {
    setScope(next as OrderScope);
    // The two queues page independently, so a page index that was valid for the queue
    // being left is meaningless - and often out of range - for the one being opened.
    setPage(0);
  };

  const handlePageSizeChange = (size: number) => {
    setPageSize(size);
    setPage(0);
  };

  return (
    <div className="flex flex-1 flex-col space-y-6 p-4 sm:p-6 lg:p-8">
      <ListPageHeader
        title="Purchase Orders"
        description="Track what has been committed to suppliers, and turn approved requisitions into orders."
        count={orderData?.totalElements}
        countLabel="orders"
      />

      <ScopeTabs
        ariaLabel="Order scope"
        value={scope}
        onValueChange={handleScopeChange}
        tabs={[
          {
            value: "orders",
            label: "Orders",
            icon: ShoppingCart,
            hint: "Every order this company has issued.",
          },
          {
            value: "awaiting",
            label: "Awaiting an order",
            icon: PackageCheck,
            hint:
              "Approved requisitions that have not been converted to an order yet.",
            badge: requisitionData?.totalElements,
          },
        ]}
      />

      <ListToolbar
        searchValue={searchTerm}
        onSearchChange={setSearchTerm}
        searchPlaceholder={
          isAwaiting
            ? "Filter requisitions by number on this page..."
            : "Filter orders by number or supplier on this page..."
        }
        isFetching={ordersFetching || requisitionsFetching}
        onRefresh={() => {
          refetchOrders();
          refetchRequisitions();
        }}
      />

      {normalizedTerm && (
        <p className="-mt-3 text-sm text-muted-foreground">
          Showing {rows.length} of {loadedRows.length}{" "}
          {isAwaiting ? "requisition" : "order"}
          {loadedRows.length === 1 ? "" : "s"} on this page.
          {rows.length === 0 && " No match on this page - try another page or clear the filter."}
        </p>
      )}

      <div className="overflow-hidden rounded-xl border border-border bg-card shadow-sm">
        <Table>
          <TableHeader className="bg-muted/40">
            {isAwaiting ? (
              <TableRow>
                <TableHead>Requisition</TableHead>
                <TableHead>Requested</TableHead>
                <TableHead>Needed by</TableHead>
                <TableHead>Status</TableHead>
                <TableHead className="text-right">Approved value</TableHead>
                <TableHead className="text-right">Actions</TableHead>
              </TableRow>
            ) : (
              <TableRow>
                <TableHead>Order</TableHead>
                <TableHead>Supplier</TableHead>
                <TableHead>Order date</TableHead>
                <TableHead>Status</TableHead>
                <TableHead className="text-right">Total</TableHead>
                <TableHead className="text-right">Outstanding</TableHead>
                <TableHead className="text-right">Actions</TableHead>
              </TableRow>
            )}
          </TableHeader>

          <TableBody>
            {isLoading ? (
              <TableSkeletonRows columns={isAwaiting ? 6 : 7} />
            ) : isError ? (
              <TableErrorRow
                columns={isAwaiting ? 6 : 7}
                title={isAwaiting ? "Could not load requisitions" : "Could not load orders"}
                description={
                  isAwaiting
                    ? "The approved requisition list could not be loaded."
                    : "The order list could not be loaded. Check your connection and try again."
                }
                onRetry={isAwaiting ? refetchRequisitions : refetchOrders}
              />
            ) : loadedRows.length === 0 ? (
              isAwaiting ? (
                <TableEmptyRow
                  icon={PackageCheck}
                  title="No approved requisitions waiting"
                  description="When a requisition is approved it appears here, ready to be turned into a purchase order."
                  columns={6}
                />
              ) : (
                <TableEmptyRow
                  icon={ShoppingCart}
                  title="No purchase orders yet"
                  description="Once an approved requisition is converted into an order, it appears here with its supplier, value and delivery progress."
                  columns={7}
                />
              )
            ) : rows.length === 0 ? (
              <TableEmptyRow
                icon={Search}
                title={isAwaiting ? "No requisition matches" : "No order matches that search"}
                description="Nothing on this page matches. Try a different term, clear the filter, or look at another page."
                columns={isAwaiting ? 6 : 7}
                action={
                  <Button variant="link" size="sm" onClick={() => setSearchTerm("")}>
                    Clear search
                  </Button>
                }
              />
            ) : isAwaiting ? (
              visibleRequisitions.map((requisition) => (
                <TableRow
                  key={requisition.id}
                  className="group transition-colors hover:bg-muted/50"
                >
                  <TableCell>
                    <p className="font-mono text-sm font-medium text-foreground">
                      {requisition.prNumber}
                    </p>
                    <p className="text-xs text-muted-foreground">
                      Priority: {requisition.priority.toLowerCase()}
                    </p>
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {formatDate(requisition.requestDate)}
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {formatDate(requisition.requiredByDate)}
                  </TableCell>
                  <TableCell>
                    <RequisitionStatusBadge status={requisition.status} />
                  </TableCell>
                  <TableCell className="text-right text-sm font-medium text-foreground">
                    {formatMoney(requisition.approvedTotalAmount, requisition.currency)}
                  </TableCell>
                  <TableCell className="text-right">
                    <DropdownMenu modal>
                      <DropdownMenuTrigger asChild>
                        <Button variant="ghost" size="icon" className="size-8">
                          <MoreVertical className="size-4" />
                          <span className="sr-only">Open menu</span>
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end" className="z-[9999] w-52">
                        <DropdownMenuItem onClick={() => setConvertTarget(requisition)}>
                          <ShoppingCart className="mr-2 size-4 text-muted-foreground" />
                          Raise order
                        </DropdownMenuItem>
                        <DropdownMenuSeparator />
                        <DropdownMenuItem asChild>
                          <Link href="/purchase-requisitions">
                            <PackageOpen className="mr-2 size-4 text-muted-foreground" />
                            All requisitions
                          </Link>
                        </DropdownMenuItem>
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </TableCell>
                </TableRow>
              ))
            ) : (
              visibleOrders.map((order) => (
                <TableRow
                  key={order.id}
                  className="group transition-colors hover:bg-muted/50"
                >
                  <TableCell>
                    <button
                      type="button"
                      onClick={() => setDetailsId(order.id)}
                      className="font-mono text-sm font-medium text-foreground underline-offset-4 hover:underline"
                    >
                      {order.poNumber}
                    </button>
                    <span className="block text-xs text-muted-foreground">
                      {order.lines.length} line{order.lines.length === 1 ? "" : "s"}
                    </span>
                  </TableCell>
                  <TableCell className="text-sm text-foreground">
                    {order.supplierName}
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {formatDate(order.orderDate)}
                  </TableCell>
                  <TableCell>
                    <PurchaseOrderStatusBadge status={order.status} />
                  </TableCell>
                  <TableCell className="text-right text-sm font-medium text-foreground">
                    {formatMoney(order.totalAmount, order.currency)}
                  </TableCell>
                  <TableCell className="text-right text-sm text-muted-foreground">
                    {formatMoney(order.outstandingAmount, order.currency)}
                  </TableCell>
                  <TableCell className="text-right">
                    <DropdownMenu modal>
                      <DropdownMenuTrigger asChild>
                        <Button variant="ghost" size="icon" className="size-8">
                          <MoreVertical className="size-4" />
                          <span className="sr-only">Open menu</span>
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end" className="z-[9999] w-56">
                        <DropdownMenuItem onClick={() => setDetailsId(order.id)}>
                          <Eye className="mr-2 size-4 text-muted-foreground" />
                          View details
                        </DropdownMenuItem>
                        {!RECEIPT_BLOCKED_STATUSES.includes(order.status) && (
                          <DropdownMenuItem onClick={() => setRecordOrder(order)}>
                            <Truck className="mr-2 size-4 text-muted-foreground" />
                            Record goods receipt
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

        <ListPagination
          page={page}
          pageSize={pageSize}
          onPageChange={setPage}
          onPageSizeChange={handlePageSizeChange}
          totalPages={activeData?.totalPages ?? 1}
          totalElements={activeData?.totalElements ?? 0}
          itemLabel={isAwaiting ? "approved requisitions" : "orders"}
          isFetching={isFetching}
        />
      </div>

      <ConvertToPurchaseOrderDialog
        requisition={convertTarget}
        open={convertTarget !== null}
        onOpenChange={(open) => !open && setConvertTarget(null)}
        onConverted={handleConverted}
      />

      <PurchaseOrderDetailsSheet
        purchaseOrderId={detailsId}
        open={detailsId !== null}
        onOpenChange={(open) => {
          if (!open) setDetailsId(null);
        }}
        onRecordReceipt={setRecordOrder}
      />

      <RecordGoodsReceiptDialog
        order={recordOrder}
        open={recordOrder !== null}
        onOpenChange={(open) => !open && setRecordOrder(null)}
        onRecorded={(receipt) => {
          // The order has just moved to PARTIALLY_RECEIVED or RECEIVED, so re-open it:
          // the operator needs to see which lines the delivery cleared.
          setRecordOrder(null);
          setDetailsId(null);
          setTimeout(() => setDetailsId(receipt.purchaseOrderId), 0);
        }}
      />
    </div>
  );
}
