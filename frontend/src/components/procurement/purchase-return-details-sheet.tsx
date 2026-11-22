"use client";

import { useState } from "react";
import {
  Ban,
  CheckCircle2,
  FileCheck2,
  Loader2,
  Printer,
  RefreshCw,
  RotateCcw,
} from "lucide-react";
import { toast } from "sonner";
import {
  useApprovePurchaseReturnMutation,
  useCancelPurchaseReturnMutation,
  useGetPurchaseReturnQuery,
  usePostPurchaseReturnMutation,
  useRetryStockPostMutation,
} from "@/services/purchase-return.service";
import { getApiErrorMessage } from "@/services/error-handler";
import {
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
} from "@/components/ui/sheet";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Skeleton } from "@/components/ui/skeleton";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { PurchaseReturnStatusBadge } from "@/components/procurement/procurement-status-badge";

interface PurchaseReturnDetailsSheetProps {
  returnId: number | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onUpdated?: () => void;
}

function DetailRow({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="flex items-start justify-between gap-3 border-b border-border/60 py-2 last:border-0 text-xs">
      <span className="text-muted-foreground">{label}</span>
      <span className="text-right font-medium text-foreground">{value}</span>
    </div>
  );
}

export function PurchaseReturnDetailsSheet({
  returnId,
  open,
  onOpenChange,
  onUpdated,
}: PurchaseReturnDetailsSheetProps) {
  const [cancelModalOpen, setCancelModalOpen] = useState(false);
  const [cancelReason, setCancelReason] = useState("");

  const { data: purchaseReturn, isLoading, isError } = useGetPurchaseReturnQuery(
    returnId ?? 0,
    { skip: !open || !returnId }
  );

  const [approveReturn, { isLoading: isApproving }] = useApprovePurchaseReturnMutation();
  const [postReturn, { isLoading: isPosting }] = usePostPurchaseReturnMutation();
  const [cancelReturn, { isLoading: isCancelling }] = useCancelPurchaseReturnMutation();
  const [retryStock, { isLoading: isRetryingStock }] = useRetryStockPostMutation();

  const handleApprove = async () => {
    if (!returnId) return;
    try {
      await approveReturn(returnId).unwrap();
      toast.success("Purchase return approved successfully.");
      onUpdated?.();
    } catch (err) {
      toast.error(getApiErrorMessage(err));
    }
  };

  const handlePost = async () => {
    if (!returnId) return;
    try {
      await postReturn(returnId).unwrap();
      toast.success("Debit note posted successfully! Stock dispatched and AP adjusted.");
      onUpdated?.();
    } catch (err) {
      toast.error(getApiErrorMessage(err));
    }
  };

  const handleRetryStock = async () => {
    if (!returnId) return;
    try {
      await retryStock(returnId).unwrap();
      toast.success("Stock movement retried successfully.");
      onUpdated?.();
    } catch (err) {
      toast.error(getApiErrorMessage(err));
    }
  };

  const handleConfirmCancel = async () => {
    if (!returnId || !cancelReason.trim()) {
      toast.error("Please state a cancellation reason.");
      return;
    }
    try {
      await cancelReturn({ id: returnId, data: { reason: cancelReason.trim() } }).unwrap();
      toast.success("Purchase return cancelled successfully.");
      setCancelModalOpen(false);
      setCancelReason("");
      onUpdated?.();
    } catch (err) {
      toast.error(getApiErrorMessage(err));
    }
  };

  const handlePrint = () => {
    window.print();
  };

  return (
    <>
      <Sheet open={open} onOpenChange={onOpenChange}>
        <SheetContent className="w-full overflow-y-auto sm:max-w-2xl lg:max-w-3xl">
          <SheetHeader className="border-b pb-4">
            <div className="flex items-center justify-between gap-3">
              <div className="flex items-center gap-2">
                <div className="flex size-9 items-center justify-center rounded-lg bg-destructive/10 text-destructive">
                  <RotateCcw className="size-5" />
                </div>
                <div>
                  <SheetTitle className="text-lg font-bold font-mono">
                    {purchaseReturn?.debitNoteNumber ?? "Debit Note"}
                  </SheetTitle>
                  <SheetDescription className="text-xs">
                    Purchase Return & Statutory Debit Note
                  </SheetDescription>
                </div>
              </div>
              {purchaseReturn && (
                <PurchaseReturnStatusBadge status={purchaseReturn.status} />
              )}
            </div>
          </SheetHeader>

          {isLoading ? (
            <div className="space-y-4 py-6">
              <Skeleton className="h-20 w-full" />
              <Skeleton className="h-40 w-full" />
            </div>
          ) : isError || !purchaseReturn ? (
            <div className="py-12 text-center text-sm text-muted-foreground">
              Failed to load purchase return details.
            </div>
          ) : (
            <div className="space-y-6 py-4">
              {/* Top Banner: Key Meta */}
              <div className="rounded-xl border border-border/80 bg-muted/30 p-4">
                <div className="grid grid-cols-2 gap-4 sm:grid-cols-4 text-xs">
                  <div>
                    <span className="text-[10px] uppercase font-semibold text-muted-foreground">
                      Return Date
                    </span>
                    <p className="font-semibold text-foreground">{formatDate(purchaseReturn.returnDate)}</p>
                    <p className="text-[10px] text-muted-foreground font-mono">
                      BS: {purchaseReturn.returnDateNp || "—"}
                    </p>
                  </div>
                  <div>
                    <span className="text-[10px] uppercase font-semibold text-muted-foreground">
                      Source Invoice
                    </span>
                    <p className="font-semibold font-mono text-primary">
                      {purchaseReturn.invoiceNumber}
                    </p>
                    <p className="text-[10px] text-muted-foreground">
                      GRN: {purchaseReturn.grnNumber || "—"}
                    </p>
                  </div>
                  <div>
                    <span className="text-[10px] uppercase font-semibold text-muted-foreground">
                      Supplier
                    </span>
                    <p className="font-semibold text-foreground truncate">
                      {purchaseReturn.supplierName}
                    </p>
                    <p className="text-[10px] text-muted-foreground">
                      PAN: {purchaseReturn.supplierPanNumber || "—"}
                    </p>
                  </div>
                  <div>
                    <span className="text-[10px] uppercase font-semibold text-muted-foreground">
                      Debit Note Value
                    </span>
                    <p className="font-bold text-sm text-destructive font-mono">
                      {formatMoney(purchaseReturn.totalAmount, purchaseReturn.currency)}
                    </p>
                    <p className="text-[10px] text-muted-foreground">
                      VAT: {formatMoney(purchaseReturn.taxAmount, purchaseReturn.currency)}
                    </p>
                  </div>
                </div>
              </div>

              {/* Stock Movement Status Alert if posted or failed */}
              {purchaseReturn.status === "POSTED" && (
                <div
                  className={`flex items-center justify-between rounded-lg border p-3 text-xs ${
                    purchaseReturn.stockPostStatus === "POSTED"
                      ? "border-emerald-500/20 bg-emerald-500/10 text-emerald-800 dark:text-emerald-300"
                      : purchaseReturn.stockPostStatus === "FAILED"
                      ? "border-red-500/20 bg-red-500/10 text-red-800 dark:text-red-300"
                      : "border-amber-500/20 bg-amber-500/10 text-amber-800 dark:text-amber-300"
                  }`}
                >
                  <div className="flex items-center gap-2">
                    <span className="font-semibold">Warehouse Stock Movement:</span>
                    <Badge variant="outline" className="text-[10px]">
                      {purchaseReturn.stockPostStatus}
                    </Badge>
                    {purchaseReturn.stockPostError && (
                      <span className="text-[11px] text-destructive">
                        ({purchaseReturn.stockPostError})
                      </span>
                    )}
                  </div>
                  {purchaseReturn.stockPostStatus === "FAILED" && (
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={handleRetryStock}
                      disabled={isRetryingStock}
                      className="h-7 text-xs gap-1"
                    >
                      {isRetryingStock ? (
                        <Loader2 className="size-3 animate-spin" />
                      ) : (
                        <RefreshCw className="size-3" />
                      )}
                      <span>Retry Dispatch</span>
                    </Button>
                  )}
                </div>
              )}

              {/* Action Toolbar */}
              <div className="flex flex-wrap items-center gap-2 border-y py-2.5">
                {purchaseReturn.status === "DRAFT" && (
                  <>
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={handleApprove}
                      disabled={isApproving}
                      className="text-xs gap-1"
                    >
                      {isApproving ? <Loader2 className="size-3 animate-spin" /> : <FileCheck2 className="size-3" />}
                      <span>Approve Return</span>
                    </Button>
                    <Button
                      size="sm"
                      onClick={handlePost}
                      disabled={isPosting}
                      className="text-xs gap-1 bg-destructive text-destructive-foreground hover:bg-destructive/90"
                    >
                      {isPosting ? <Loader2 className="size-3 animate-spin" /> : <CheckCircle2 className="size-3" />}
                      <span>Post Debit Note</span>
                    </Button>
                  </>
                )}

                {purchaseReturn.status === "APPROVED" && (
                  <Button
                    size="sm"
                    onClick={handlePost}
                    disabled={isPosting}
                    className="text-xs gap-1 bg-destructive text-destructive-foreground hover:bg-destructive/90"
                  >
                    {isPosting ? <Loader2 className="size-3 animate-spin" /> : <CheckCircle2 className="size-3" />}
                    <span>Post Debit Note</span>
                  </Button>
                )}

                {purchaseReturn.status !== "CANCELLED" && (
                  <Button
                    size="sm"
                    variant="ghost"
                    onClick={() => setCancelModalOpen(true)}
                    disabled={isCancelling}
                    className="text-xs gap-1 text-muted-foreground hover:text-destructive"
                  >
                    <Ban className="size-3" />
                    <span>Cancel Return</span>
                  </Button>
                )}

                <Button
                  size="sm"
                  variant="outline"
                  onClick={handlePrint}
                  className="text-xs gap-1 ml-auto"
                >
                  <Printer className="size-3" />
                  <span>Print Debit Note</span>
                </Button>
              </div>

              {/* Line Items Table */}
              <div className="space-y-2">
                <h4 className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
                  Returned Line Items ({purchaseReturn.lines?.length ?? 0})
                </h4>
                <div className="overflow-hidden rounded-xl border border-border">
                  <table className="w-full text-left text-xs">
                    <thead className="border-b bg-muted/60 text-[11px] font-semibold text-muted-foreground">
                      <tr>
                        <th className="py-2 pl-3 pr-2 w-[8%] text-center">#</th>
                        <th className="py-2 px-2 w-[42%]">Item</th>
                        <th className="py-2 px-2 w-[14%] text-right">Return Qty</th>
                        <th className="py-2 px-2 w-[16%] text-right">Unit Price</th>
                        <th className="py-2 pl-2 pr-3 w-[20%] text-right">Line Total</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-border/60">
                      {purchaseReturn.lines?.map((line) => (
                        <tr key={line.id} className="hover:bg-muted/10">
                          <td className="py-2 pl-3 pr-2 text-center text-muted-foreground">
                            {line.lineNumber}
                          </td>
                          <td className="py-2 px-2">
                            <span className="font-medium text-foreground">{line.itemName}</span>
                            <div className="text-[10px] text-muted-foreground font-mono">
                              {line.itemCode} • {line.uomCode}
                            </div>
                            {line.remarks && (
                              <div className="text-[10px] text-muted-foreground italic mt-0.5">
                                Note: {line.remarks}
                              </div>
                            )}
                          </td>
                          <td className="py-2 px-2 text-right font-mono font-medium">
                            {formatQuantity(line.returnQuantity)}
                          </td>
                          <td className="py-2 px-2 text-right font-mono text-muted-foreground">
                            {formatMoney(line.unitPrice, purchaseReturn.currency)}
                          </td>
                          <td className="py-2 pl-2 pr-3 text-right font-mono font-semibold text-foreground">
                            {formatMoney(line.totalAmount, purchaseReturn.currency)}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>

              {/* Document Financial Breakdown & Audit */}
              <div className="grid grid-cols-1 gap-4 lg:grid-cols-2 text-xs">
                <div className="space-y-1 rounded-xl border p-3 bg-muted/10">
                  <span className="text-[10px] uppercase font-semibold text-muted-foreground">
                    Document Audit & Justification
                  </span>
                  <DetailRow label="Statutory Reason" value={purchaseReturn.reason} />
                  {purchaseReturn.notes && (
                    <DetailRow label="Notes" value={purchaseReturn.notes} />
                  )}
                  <DetailRow label="Warehouse" value={purchaseReturn.warehouseName || "Main"} />
                  {purchaseReturn.postedAt && (
                    <DetailRow
                      label="Posted At"
                      value={formatTimestamp(purchaseReturn.postedAt)}
                    />
                  )}
                  {purchaseReturn.cancellationReason && (
                    <DetailRow
                      label="Cancellation Reason"
                      value={<span className="text-destructive">{purchaseReturn.cancellationReason}</span>}
                    />
                  )}
                </div>

                <div className="space-y-1.5 rounded-xl border border-destructive/20 bg-destructive/5 p-3.5">
                  <div className="flex justify-between text-xs text-muted-foreground">
                    <span>Gross Subtotal:</span>
                    <span className="font-mono text-foreground font-medium">
                      {formatMoney(purchaseReturn.subtotalAmount, purchaseReturn.currency)}
                    </span>
                  </div>
                  <div className="flex justify-between text-xs text-muted-foreground">
                    <span>Reversed Input VAT:</span>
                    <span className="font-mono text-foreground font-medium">
                      +{formatMoney(purchaseReturn.taxAmount, purchaseReturn.currency)}
                    </span>
                  </div>
                  <div className="border-t border-destructive/20 pt-2 flex justify-between text-sm font-bold text-foreground">
                    <span>Total Debit Note:</span>
                    <span className="font-mono text-destructive text-base font-bold">
                      {formatMoney(purchaseReturn.totalAmount, purchaseReturn.currency)}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          )}
        </SheetContent>
      </Sheet>

      {/* Cancellation Confirmation Dialog */}
      <Dialog open={cancelModalOpen} onOpenChange={setCancelModalOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle className="text-base font-bold">Cancel Purchase Return</DialogTitle>
            <DialogDescription className="text-xs">
              This will void Debit Note {purchaseReturn?.debitNoteNumber}. If posted, it will reverse the accounts payable adjustment and return stock to warehouse.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-2 py-2">
            <Label className="text-xs font-semibold">Cancellation Reason *</Label>
            <Input
              placeholder="Why is this return being voided…"
              value={cancelReason}
              onChange={(e) => setCancelReason(e.target.value)}
              className="text-xs"
            />
          </div>
          <DialogFooter className="gap-2 sm:justify-end">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => setCancelModalOpen(false)}
              className="text-xs"
            >
              Back
            </Button>
            <Button
              type="button"
              variant="destructive"
              size="sm"
              onClick={handleConfirmCancel}
              disabled={isCancelling || !cancelReason.trim()}
              className="text-xs"
            >
              {isCancelling ? <Loader2 className="size-3.5 animate-spin" /> : <Ban className="size-3.5" />}
              <span>Confirm Void</span>
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}

