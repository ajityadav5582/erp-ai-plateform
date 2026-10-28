"use client";

import { useState } from "react";
import { Receipt } from "lucide-react";
import { toast } from "sonner";
import { useGetGoodsReceiptQuery, useRetryGoodsReceiptStockPostMutation } from "@/services/goods-receipt.service";
import {
  formatAmount,
  formatDate,
  formatMoney,
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
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { GoodsReceiptStatusBadge } from "@/components/procurement/procurement-status-badge";
import { RecordPurchaseInvoiceDialog } from "@/components/procurement/record-purchase-invoice-dialog";
import type { PurchaseInvoice } from "@/services/purchase-invoice.service";

interface GoodsReceiptDetailsSheetProps {
  goodsReceiptId: number | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
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
 * Read-only view of a goods receipt note.
 *
 * A receipt has no lifecycle actions of its own: it is created already posted, so the
 * only thing this screen can legitimately do is hand the delivery on to invoicing. That
 * is why the only button here is "record invoice" - a receipt that has been billed is
 * finished, and one that has not still has work outstanding against it.
 *
 * The warehouse shown here is the note's frozen snapshot, not a live lookup. A
 * warehouse renamed after this receipt was posted must not change what the printed
 * document says it received, which is the same reason the supplier code is frozen too.
 */
export function GoodsReceiptDetailsSheet({
  goodsReceiptId,
  open,
  onOpenChange,
}: GoodsReceiptDetailsSheetProps) {
  const [invoiceTarget, setInvoiceTarget] =
    useState<{ receiptId: number } | null>(null);

  const [retryStockPost, { isLoading: isRetrying }] = useRetryGoodsReceiptStockPostMutation();

  const {
    data: receipt,
    isLoading,
    error,
    refetch,
  } = useGetGoodsReceiptQuery(goodsReceiptId ?? 0, {
    skip: !goodsReceiptId || !open,
  });

  const handleRetryStockPost = async () => {
    if (!receipt) return;
    try {
      await retryStockPost(receipt.id).unwrap();
      toast.success("Stock posted successfully");
      refetch();
    } catch (e) {
      toast.error("Retry failed");
    }
  };

  const handleInvoiceCreated = (invoice: PurchaseInvoice) => {
    toast.success(`Supplier invoice ${invoice.invoiceNumber} recorded`, {
      description: `${formatMoney(
        invoice.totalAmount,
        invoice.currency
      )} is now payable for ${invoice.grnNumber}.`,
    });
    refetch();
  };

  return (
    <>
      <Sheet open={open} onOpenChange={onOpenChange}>
        <SheetContent className="w-full overflow-y-auto sm:max-w-3xl">
          <SheetHeader className="sticky top-0 z-10 border-b border-border bg-background">
            <SheetTitle>
              {receipt ? `Goods receipt ${receipt.grnNumber}` : "Goods receipt"}
            </SheetTitle>
            <SheetDescription>
              {receipt
                ? `Against purchase order ${receipt.poNumber}`
                : "Loading the receipt…"}
            </SheetDescription>
          </SheetHeader>

          {isLoading && (
            <div className="space-y-3 px-4 py-4">
              <Skeleton className="h-6 w-48" />
              <Skeleton className="h-24 w-full" />
              <Skeleton className="h-32 w-full" />
            </div>
          )}

          {!isLoading && error && (
            <div className="py-8 text-center">
              <p className="text-sm text-muted-foreground">
                This receipt could not be loaded. It may belong to another company or may
                have been deleted.
              </p>
              <Button variant="outline" size="sm" className="mt-3" onClick={refetch}>
                Try again
              </Button>
            </div>
          )}

          {!isLoading && !error && receipt && (
            <div className="px-4 pb-6">
              <div className="mb-4 flex flex-wrap gap-2">
                <GoodsReceiptStatusBadge status={receipt.status} />
              </div>

              {receipt.stockPostStatus === "FAILED" && (
                <div className="mb-4 rounded-md border border-destructive/40 bg-destructive/5 p-3 text-sm">
                  <p className="font-medium text-destructive">Stock post failed</p>
                  {receipt.stockPostError && (
                    <p className="mt-1 text-xs text-muted-foreground">{receipt.stockPostError}</p>
                  )}
                  <Button
                    size="sm"
                    className="mt-2"
                    onClick={handleRetryStockPost}
                    disabled={isRetrying}
                  >
                    {isRetrying ? "Retrying…" : "Retry stock post"}
                  </Button>
                </div>
              )}

              <div className="space-y-4 py-2">
                <div>
                  <h4 className="mb-1 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                    Receipt
                  </h4>
                  <DetailRow
                    label="Receipt date"
                    value={formatDate(receipt.receiptDate)}
                  />
                  <DetailRow
                    label="Purchase order"
                    value={receipt.poNumber}
                  />
                  <DetailRow
                    label="Supplier"
                    value={`${receipt.supplierCode} — ${receipt.supplierName}`}
                  />
                  <DetailRow
                    label="Received into"
                    value={
                      receipt.warehouseName
                        ? `${receipt.warehouseName} (${receipt.warehouseCode})`
                        : "—"
                    }
                  />
                  <DetailRow
                    label="Delivery challan"
                    value={receipt.deliveryChallanNo ?? "—"}
                  />
                  <DetailRow
                    label="Received value"
                    value={
                      <span className="text-base">
                        {formatMoney(receipt.receivedAmount, receipt.currency)}
                      </span>
                    }
                  />
                  <DetailRow
                    label="Posted"
                    value={formatTimestamp(receipt.postedAt)}
                  />
                  <DetailRow label="Posted by" value={receipt.postedBy ?? "—"} />
                  {receipt.cancellationReason && (
                    <DetailRow
                      label="Cancellation reason"
                      value={receipt.cancellationReason}
                    />
                  )}
                </div>

                <div>
                  <h4 className="mb-2 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                    Lines received
                  </h4>
                  <div className="overflow-hidden rounded-lg border border-border">
                    <Table>
                      <TableHeader className="bg-muted/40">
                        <TableRow>
                          <TableHead>#</TableHead>
                          <TableHead>Item</TableHead>
                          <TableHead className="text-right">Qty</TableHead>
                          <TableHead className="text-right">Price</TableHead>
                          <TableHead className="text-right">Amount</TableHead>
                        </TableRow>
                      </TableHeader>
                      <TableBody>
                        {receipt.lines.map((line) => (
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
                              {/*
                                A line whose warehouse differs from the note's means
                                one challan was offloaded at two sites. It is called out
                                rather than silently absorbed: stock has landed in two
                                places, and a balance report will not reconcile until
                                somebody knows that happened.
                              */}
                              {line.warehouseId !== receipt.warehouseId && (
                                <span className="block text-xs font-medium text-amber-600 dark:text-amber-400">
                                  Split delivery — received elsewhere
                                </span>
                              )}
                            </TableCell>
                            <TableCell className="text-right">
                              {formatQuantity(line.receivedQuantity)}
                            </TableCell>
                            <TableCell className="text-right">
                              {formatAmount(line.unitPrice)}
                            </TableCell>
                            <TableCell className="text-right font-medium">
                              {formatAmount(line.lineAmount)}
                            </TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </div>
                </div>

                {receipt.notes && (
                  <div>
                    <h4 className="mb-1 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                      Notes
                    </h4>
                    <p className="whitespace-pre-wrap text-sm text-foreground">
                      {receipt.notes}
                    </p>
                  </div>
                )}
              </div>
            </div>
          )}

           {!isLoading && !error && receipt && receipt.status === "POSTED" && !receipt.invoiceNumber && (
             <SheetFooter className="border-t border-border pt-4">
               <Button
                 size="sm"
                 onClick={() => setInvoiceTarget({ receiptId: receipt.id })}
               >
                 <Receipt className="mr-2 size-4" aria-hidden="true" />
                 Record supplier invoice
               </Button>
               <p className="text-xs text-muted-foreground">
                 An invoice is raised against a delivery, so it needs this receipt.
               </p>
             </SheetFooter>
           )}
           {!isLoading && !error && receipt && receipt.invoiceNumber && (
             <SheetFooter className="border-t border-border pt-4">
               <p className="text-xs text-muted-foreground">
                 Already invoiced as {receipt.invoiceNumber}.
               </p>
             </SheetFooter>
           )}
        </SheetContent>
      </Sheet>

      {invoiceTarget && (
        <RecordPurchaseInvoiceDialog
          goodsReceiptId={invoiceTarget.receiptId}
          open={invoiceTarget !== null}
          onOpenChange={(next) => !next && setInvoiceTarget(null)}
          onCreated={handleInvoiceCreated}
        />
      )}
    </>
  );
}
