"use client";

import { useState } from "react";
import {
  Ban,
  CheckCircle2,
  Loader2,
  Lock,
  Receipt,
  Send,
  Truck,
  XCircle,
} from "lucide-react";
import { toast } from "sonner";
import {
  useGetPurchaseOrderQuery,
  useSubmitPurchaseOrderMutation,
  useApprovePurchaseOrderMutation,
  useRejectPurchaseOrderMutation,
  useCancelPurchaseOrderMutation,
  useClosePurchaseOrderMutation,
  CANCELLABLE_ORDER_STATUSES,
  CLOSABLE_ORDER_STATUSES,
  DECIDABLE_ORDER_STATUSES,
  SUBMITTABLE_ORDER_STATUSES,
  type PurchaseOrder,
} from "@/services/purchase-order.service";
import { getApiErrorMessage } from "@/services/error-handler";
import {
  formatAmount,
  formatDate,
  formatMoney,
  formatPercentage,
  formatQuantity,
  formatTimestamp,
} from "@/lib/procurement-format";

import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
  SheetFooter,
} from "@/components/ui/sheet";
import { Skeleton } from "@/components/ui/skeleton";
import { Button } from "@/components/ui/button";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  PurchaseOrderLineStatusBadge,
  PurchaseOrderStatusBadge,
} from "@/components/procurement/procurement-status-badge";
import { OrderReasonDialog } from "@/components/procurement/order-reason-dialog";

interface PurchaseOrderDetailsSheetProps {
  purchaseOrderId: number | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  /** Opens the goods receipt form pre-loaded with this order. */
  onRecordReceipt?: (order: PurchaseOrder) => void;
}

function DetailRow({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="flex items-start justify-between gap-3 border-b border-border py-2 last:border-0">
      <span className="text-xs text-muted-foreground">{label}</span>
      <span className="text-right text-sm font-medium text-foreground">{value}</span>
    </div>
  );
}

/**
 * Read-only view of a purchase order, plus the lifecycle actions available on it.
 *
 * Which buttons appear is decided by the status **and** re-checked server-side on every
 * call. The status check here is a convenience that stops the screen offering a button
 * the aggregate will refuse; it is not the authorisation. That distinction matters
 * because these transitions are one-way: a REJECTED or CANCELLED order can never be
 * revived, so an action offered in error is not an undoable mistake.
 *
 * Reject and cancel both demand a reason, collected in {@link OrderReasonDialog}. An
 * unexplained refusal is not auditable - the supplier asks why and the buyer cannot
 * answer - so the reason is not optional anywhere in this flow.
 */
export function PurchaseOrderDetailsSheet({
  purchaseOrderId,
  open,
  onOpenChange,
  onRecordReceipt,
}: PurchaseOrderDetailsSheetProps) {
  const [reasonAction, setReasonAction] = useState<"reject" | "cancel" | null>(null);

  const {
    data: order,
    isLoading,
    isFetching,
    error,
    refetch,
  } = useGetPurchaseOrderQuery(purchaseOrderId ?? 0, {
    skip: !purchaseOrderId || !open,
  });

  const [submit, { isLoading: isSubmitting }] = useSubmitPurchaseOrderMutation();
  const [approve, { isLoading: isApproving }] = useApprovePurchaseOrderMutation();
  const [reject, { isLoading: isRejecting }] = useRejectPurchaseOrderMutation();
  const [cancel, { isLoading: isCancelling }] = useCancelPurchaseOrderMutation();
  const [close, { isLoading: isClosing }] = useClosePurchaseOrderMutation();

  const busy =
    isSubmitting || isApproving || isRejecting || isCancelling || isClosing || isFetching;

  const runAction = async (
    label: string,
    action: () => Promise<PurchaseOrder>
  ) => {
    try {
      await action();
      toast.success(label);
    } catch (err) {
      toast.error("Action failed", { description: getApiErrorMessage(err) });
    }
  };

  const canSubmit = order
    ? SUBMITTABLE_ORDER_STATUSES.includes(order.status)
    : false;
  const canDecide = order
    ? DECIDABLE_ORDER_STATUSES.includes(order.status)
    : false;
  const canCancel = order
    ? CANCELLABLE_ORDER_STATUSES.includes(order.status)
    : false;
  const canClose = order ? CLOSABLE_ORDER_STATUSES.includes(order.status) : false;

  /**
   * A 404 is not a transient failure, so it must not be dressed up as one.
   *
   * Offering "Try again" on an order that does not exist sends someone round a loop
   * that can never succeed, and hides the only useful fact: the id is wrong. Because
   * ids come from a sequence, a rolled-back conversion burns one, so the first order a
   * company raises is frequently id 2 rather than 1 - which is exactly the guess people
   * make and exactly why guessing produces a 404.
   */
  const isNotFound = (error as { status?: number } | undefined)?.status === 404;

  /**
   * Receiving is offered only once the order is issued to the supplier. A draft or
   * submitted order is not yet a commitment, so goods arriving against it would be
   * received against nothing.
   */
  const canReceive =
    order !== undefined &&
    ["APPROVED", "PARTIALLY_RECEIVED"].includes(order.status);

  return (
    <>
      <Sheet open={open} onOpenChange={onOpenChange}>
        <SheetContent className="w-full overflow-y-auto sm:max-w-3xl">
          <SheetHeader className="sticky top-0 z-10 border-b border-border bg-background">
            <SheetTitle>
              {order ? `Purchase order ${order.poNumber}` : "Purchase order"}
            </SheetTitle>
            <SheetDescription>
              {order
                ? `${order.supplierCode} — ${order.supplierName}`
                : "Loading the order…"}
            </SheetDescription>
          </SheetHeader>

          {isLoading && (
            <div className="space-y-3 px-4 py-4">
              <Skeleton className="h-6 w-48" />
              <Skeleton className="h-24 w-full" />
              <Skeleton className="h-40 w-full" />
            </div>
          )}

          {!isLoading && error && (
            isNotFound ? (
              <div className="py-8 text-center">
                <p className="text-sm text-muted-foreground">
                  There is no purchase order with id {purchaseOrderId}.
                </p>
                <p className="mt-1 text-sm text-muted-foreground">
                  Ids are not continuous - a conversion that was rolled back still
                  consumes one - so the number people guess is often simply unused. Close
                  this panel and pick the order from the list by its number, such as
                  PO-000001.
                </p>
              </div>
            ) : (
              <div className="py-8 text-center">
                <p className="text-sm text-muted-foreground">
                  This order could not be loaded. It may belong to another company or may
                  have been deleted.
                </p>
                <Button variant="outline" size="sm" className="mt-3" onClick={refetch}>
                  Try again
                </Button>
              </div>
            )
          )}

          {!isLoading && !error && order && (
            <div className="px-4 pb-6">
              <div className="mb-4 flex flex-wrap gap-2">
                <PurchaseOrderStatusBadge status={order.status} />
                {order.sourcePurchaseRequisitionId && (
                  <span className="text-xs text-muted-foreground">
                    Converted from requisition #{order.sourcePurchaseRequisitionId}
                  </span>
                )}
              </div>

              <Tabs defaultValue="summary">
                <TabsList>
                  <TabsTrigger value="summary">Summary</TabsTrigger>
                  <TabsTrigger value="lines">Lines ({order.lines.length})</TabsTrigger>
                </TabsList>

                <TabsContent value="summary" className="space-y-4 py-3">
                  <div>
                    <h4 className="mb-1 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                      Order
                    </h4>
                    <DetailRow label="Order date" value={formatDate(order.orderDate)} />
                    <DetailRow
                      label="Expected delivery"
                      value={formatDate(order.expectedDeliveryDate)}
                    />
                    <DetailRow
                      label="Receiving warehouse"
                      value={
                        order.warehouseName
                          ? `${order.warehouseName} (${order.warehouseCode})`
                          : "Not nominated"
                      }
                    />
                    <DetailRow label="Reference" value={order.reference ?? "—"} />
                    <DetailRow label="Branch" value={order.branchId ?? "—"} />
                    <DetailRow
                      label="Created"
                      value={formatTimestamp(order.createdAt)}
                    />
                  </div>

                  <div>
                    <h4 className="mb-1 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                      Amounts
                    </h4>
                    <DetailRow
                      label="Subtotal"
                      value={formatMoney(order.subtotalAmount, order.currency)}
                    />
                    <DetailRow
                      label="Discount"
                      value={formatMoney(order.discountAmount, order.currency)}
                    />
                    <DetailRow
                      label="Tax"
                      value={formatMoney(order.taxAmount, order.currency)}
                    />
                    <DetailRow
                      label="Shipping"
                      value={formatMoney(order.shippingAmount, order.currency)}
                    />
                    <DetailRow
                      label="Total"
                      value={
                        <span className="text-base">
                          {formatMoney(order.totalAmount, order.currency)}
                        </span>
                      }
                    />
                    <DetailRow
                      label="Received"
                      value={formatMoney(order.receivedAmount, order.currency)}
                    />
                    <DetailRow
                      label="Outstanding"
                      value={
                        <span
                          className={
                            Number(order.outstandingAmount) > 0
                              ? "text-amber-600 dark:text-amber-400"
                              : "text-emerald-600 dark:text-emerald-400"
                          }
                        >
                          {formatMoney(order.outstandingAmount, order.currency)}
                        </span>
                      }
                    />
                  </div>

                  {order.notes && (
                    <div>
                      <h4 className="mb-1 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                        Notes
                      </h4>
                      <p className="whitespace-pre-wrap text-sm text-foreground">
                        {order.notes}
                      </p>
                    </div>
                  )}
                </TabsContent>

                <TabsContent value="lines" className="py-3">
                  <div className="overflow-hidden rounded-lg border border-border">
                    <Table>
                      <TableHeader className="bg-muted/40">
                        <TableRow>
                          <TableHead>#</TableHead>
                          <TableHead>Item</TableHead>
                          <TableHead className="text-right">Qty</TableHead>
                          <TableHead className="text-right">Received</TableHead>
                          <TableHead className="text-right">Price</TableHead>
                          <TableHead className="text-right">Disc %</TableHead>
                          <TableHead className="text-right">Tax %</TableHead>
                          <TableHead className="text-right">Amount</TableHead>
                          <TableHead>Status</TableHead>
                        </TableRow>
                      </TableHeader>
                      <TableBody>
                        {order.lines.map((line) => (
                          <TableRow key={line.id}>
                            <TableCell className="text-muted-foreground">
                              {line.lineNumber}
                            </TableCell>
                            <TableCell>
                              <span className="block font-medium text-foreground">
                                {line.itemName}
                              </span>
                              <span className="block text-xs text-muted-foreground">
                                {line.itemCode} · {line.uomCode}
                              </span>
                            </TableCell>
                            <TableCell className="text-right">
                              {formatQuantity(line.quantity)}
                            </TableCell>
                            <TableCell className="text-right">
                              {formatQuantity(line.receivedQuantity)}
                            </TableCell>
                            <TableCell className="text-right">
                              {formatAmount(line.unitPrice)}
                            </TableCell>
                            <TableCell className="text-right">
                              {formatPercentage(line.discountPercentage)}
                            </TableCell>
                            <TableCell className="text-right">
                              {formatPercentage(line.taxRate)}
                            </TableCell>
                            <TableCell className="text-right font-medium">
                              {formatAmount(line.lineAmount)}
                            </TableCell>
                            <TableCell>
                              <PurchaseOrderLineStatusBadge status={line.status} />
                            </TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </div>
                </TabsContent>
              </Tabs>
            </div>
          )}

          {!isLoading && !error && order && (
            <SheetFooter className="flex-row flex-wrap gap-2 border-t border-border pt-4">
              {canSubmit && (
                <Button
                  size="sm"
                  disabled={busy}
                  onClick={() =>
                    runAction(
                      `Purchase order ${order.poNumber} submitted for approval`,
                      () => submit(order.id).unwrap()
                    )
                  }
                >
                  {isSubmitting ? (
                    <Loader2 className="mr-2 size-4 animate-spin" aria-hidden="true" />
                  ) : (
                    <Send className="mr-2 size-4" aria-hidden="true" />
                  )}
                  Submit for approval
                </Button>
              )}

              {canDecide && (
                <>
                  <Button
                    size="sm"
                    disabled={busy}
                    onClick={() =>
                      runAction(
                        `Purchase order ${order.poNumber} approved`,
                        () => approve(order.id).unwrap()
                      )
                    }
                  >
                    {isApproving ? (
                      <Loader2 className="mr-2 size-4 animate-spin" aria-hidden="true" />
                    ) : (
                      <CheckCircle2 className="mr-2 size-4" aria-hidden="true" />
                    )}
                    Approve
                  </Button>
                  <Button
                    size="sm"
                    variant="outline"
                    disabled={busy}
                    onClick={() => setReasonAction("reject")}
                  >
                    <XCircle className="mr-2 size-4" aria-hidden="true" />
                    Reject
                  </Button>
                </>
              )}

              {canReceive && onRecordReceipt && (
                <Button
                  size="sm"
                  variant="outline"
                  disabled={busy}
                  onClick={() => onRecordReceipt(order)}
                >
                  <Truck className="mr-2 size-4" aria-hidden="true" />
                  Record goods receipt
                </Button>
              )}

              {canClose && (
                <Button
                  size="sm"
                  variant="outline"
                  disabled={busy}
                  onClick={() =>
                    runAction(
                      `Purchase order ${order.poNumber} closed`,
                      () => close(order.id).unwrap()
                    )
                  }
                >
                  {isClosing ? (
                    <Loader2 className="mr-2 size-4 animate-spin" aria-hidden="true" />
                  ) : (
                    <Lock className="mr-2 size-4" aria-hidden="true" />
                  )}
                  Close order
                </Button>
              )}

              {canCancel && (
                <Button
                  size="sm"
                  variant="outline"
                  disabled={busy}
                  onClick={() => setReasonAction("cancel")}
                >
                  <Ban className="mr-2 size-4" aria-hidden="true" />
                  Cancel order
                </Button>
              )}

              {!canSubmit &&
                !canDecide &&
                !canCancel &&
                !canClose &&
                !canReceive && (
                  <p className="flex items-center gap-1.5 text-xs text-muted-foreground">
                    <Receipt className="size-3.5" aria-hidden="true" />
                    No action is available on this order in its current state.
                  </p>
                )}
            </SheetFooter>
          )}
        </SheetContent>
      </Sheet>

      {order && (
        <OrderReasonDialog
          open={reasonAction !== null}
          onOpenChange={(next) => !next && setReasonAction(null)}
          mode={reasonAction ?? "reject"}
          poNumber={order.poNumber}
          onConfirm={async (reason) => {
            if (reasonAction === "reject") {
              await runAction(`Purchase order ${order.poNumber} rejected`, () =>
                reject({ purchaseOrderId: order.id, data: { reason } }).unwrap()
              );
            } else if (reasonAction === "cancel") {
              await runAction(`Purchase order ${order.poNumber} cancelled`, () =>
                cancel({ purchaseOrderId: order.id, data: { reason } }).unwrap()
              );
            }
            setReasonAction(null);
          }}
        />
      )}
    </>
  );
}
