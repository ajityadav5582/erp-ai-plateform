"use client";

import { useEffect, useMemo, useState } from "react";
import { AlertCircle, CheckCircle2, Coins, Loader2, Sparkles } from "lucide-react";
import { toast } from "sonner";
import type { PurchaseInvoice } from "@/services/purchase-invoice.service";
import {
  useGetAvailableAdvancesQuery,
  useApplyAdvanceMutation,
  type SupplierPayment,
} from "@/services/supplier-payment.service";
import { getApiErrorMessage } from "@/services/error-handler";
import { formatDate, formatMoney } from "@/lib/procurement-format";

import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";

interface ApplyAdvanceCreditDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  invoice: PurchaseInvoice | null;
  onApplied?: (payment: SupplierPayment) => void;
}

export function ApplyAdvanceCreditDialog({
  open,
  onOpenChange,
  invoice,
  onApplied,
}: ApplyAdvanceCreditDialogProps) {
  const supplierId = invoice?.supplierId ?? 0;
  const balanceDue = Number(invoice?.balanceAmount ?? 0);

  const {
    data: advances = [],
    isLoading: advancesLoading,
  } = useGetAvailableAdvancesQuery(supplierId, {
    skip: !open || !supplierId || balanceDue <= 0,
  });

  const [selectedAdvanceId, setSelectedAdvanceId] = useState<number | null>(null);
  const [amountToApply, setAmountToApply] = useState("");
  const [notes, setNotes] = useState("");

  const [applyAdvance, { isLoading: isSubmitting }] = useApplyAdvanceMutation();

  // Pick first available advance by default when list arrives
  useEffect(() => {
    if (advances.length > 0) {
      // Pick first advance or keep current if still valid
      if (!selectedAdvanceId || !advances.some((a) => a.id === selectedAdvanceId)) {
        const first = advances[0];
        if (first) {
          setSelectedAdvanceId(first.id);
          const maxApplicable = Math.min(balanceDue, Number(first.unallocatedAmount));
          setAmountToApply(maxApplicable.toFixed(2));
        }
      }
    } else {
      setSelectedAdvanceId(null);
      setAmountToApply("");
    }
  }, [advances, balanceDue, selectedAdvanceId]);

  // Selected advance object
  const selectedAdvance = useMemo(
    () => advances.find((a) => a.id === selectedAdvanceId) ?? null,
    [advances, selectedAdvanceId]
  );

  const unallocatedAvailable = Number(selectedAdvance?.unallocatedAmount ?? 0);
  const maxApplicableAmount = Math.min(balanceDue, unallocatedAvailable);
  const enteredAmountNum = parseFloat(amountToApply) || 0;

  const handleSelectAdvance = (adv: SupplierPayment) => {
    setSelectedAdvanceId(adv.id);
    const maxVal = Math.min(balanceDue, Number(adv.unallocatedAmount));
    setAmountToApply(maxVal.toFixed(2));
  };

  const handleMaxApply = () => {
    setAmountToApply(maxApplicableAmount.toFixed(2));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!invoice) return;

    if (!selectedAdvanceId) {
      toast.error("Please select an advance payment to apply.");
      return;
    }

    if (!Number.isFinite(enteredAmountNum) || enteredAmountNum <= 0) {
      toast.error("Amount to apply must be greater than zero.");
      return;
    }

    if (enteredAmountNum > unallocatedAvailable + 0.0001) {
      toast.error(
        `Amount cannot exceed available advance credit (NPR ${unallocatedAvailable.toLocaleString()}).`
      );
      return;
    }

    if (enteredAmountNum > balanceDue + 0.0001) {
      toast.error(
        `Amount cannot exceed invoice outstanding balance (NPR ${balanceDue.toLocaleString()}).`
      );
      return;
    }

    try {
      const res = await applyAdvance({
        advancePaymentId: selectedAdvanceId,
        purchaseInvoiceId: invoice.id,
        amountToApply: enteredAmountNum,
        notes: notes.trim() || undefined,
      }).unwrap();

      toast.success(
        `Applied NPR ${enteredAmountNum.toLocaleString()} advance credit from ${res.paymentNumber} to invoice ${invoice.invoiceNumber}.`
      );
      onApplied?.(res);
      onOpenChange(false);
    } catch (err) {
      toast.error(getApiErrorMessage(err));
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90vh] w-[95vw] max-w-xl overflow-y-auto p-4 sm:p-6">
        <form onSubmit={handleSubmit} className="space-y-5">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2 text-lg font-bold tracking-tight">
              <Coins className="size-5 text-amber-500" />
              Apply Supplier Advance Credit
            </DialogTitle>
            <DialogDescription>
              Offset outstanding invoice balance using unallocated advance credit previously disbursed to this supplier.
            </DialogDescription>
          </DialogHeader>

          {/* Invoice Summary Box */}
          {invoice && (
            <div className="rounded-lg border border-border/80 bg-muted/30 p-3 sm:p-4 text-xs space-y-1.5">
              <div className="flex items-center justify-between font-medium">
                <span className="text-muted-foreground">Target Invoice:</span>
                <span className="font-semibold text-foreground">{invoice.invoiceNumber}</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-muted-foreground">Supplier:</span>
                <span className="text-foreground">{invoice.supplierName} ({invoice.supplierCode})</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-muted-foreground">Total Invoiced:</span>
                <span className="text-foreground">{formatMoney(invoice.totalAmount, invoice.currency)}</span>
              </div>
              <div className="flex items-center justify-between pt-1 border-t border-border/60">
                <span className="font-semibold text-foreground">Current Outstanding Balance:</span>
                <span className="font-bold text-amber-600 dark:text-amber-400 text-sm">
                  {formatMoney(invoice.balanceAmount, invoice.currency)}
                </span>
              </div>
            </div>
          )}

          {/* Available Advances Section */}
          <div className="space-y-2">
            <Label className="text-xs font-semibold uppercase text-muted-foreground">
              Select Available Advance Credit
            </Label>

            {advancesLoading ? (
              <div className="flex items-center justify-center p-6 border rounded-lg border-dashed">
                <Loader2 className="size-5 animate-spin text-muted-foreground mr-2" />
                <span className="text-xs text-muted-foreground">Checking supplier advance balances…</span>
              </div>
            ) : advances.length === 0 ? (
              <div className="rounded-lg border border-dashed border-amber-500/40 bg-amber-500/5 p-4 text-center">
                <AlertCircle className="mx-auto size-6 text-amber-500 mb-1" />
                <p className="text-sm font-medium text-foreground">No Available Advance Credits</p>
                <p className="text-xs text-muted-foreground mt-0.5">
                  This supplier has no unallocated advance disbursements. You can record a payment or upfront advance from the Supplier Payments screen.
                </p>
              </div>
            ) : (
              <div className="space-y-2 max-h-48 overflow-y-auto pr-1">
                {advances.map((adv) => {
                  const isSelected = adv.id === selectedAdvanceId;
                  return (
                    <div
                      key={adv.id}
                      onClick={() => handleSelectAdvance(adv)}
                      className={`flex cursor-pointer items-center justify-between rounded-lg border p-3 text-xs transition-all ${
                        isSelected
                          ? "border-primary bg-primary/5 ring-1 ring-primary shadow-xs"
                          : "border-border/80 hover:border-primary/50 hover:bg-muted/30"
                      }`}
                    >
                      <div className="space-y-0.5">
                        <div className="flex items-center gap-2">
                          <span className="font-semibold text-foreground text-sm">
                            {adv.paymentNumber}
                          </span>
                          <Badge variant="outline" className="text-[10px] px-1.5 py-0">
                            {adv.paymentMethod}
                          </Badge>
                          {adv.referenceNo && (
                            <span className="text-muted-foreground text-[11px]">
                              Ref: {adv.referenceNo}
                            </span>
                          )}
                        </div>
                        <div className="text-muted-foreground text-[11px]">
                          Disbursed on {formatDate(adv.paymentDate)}
                        </div>
                      </div>

                      <div className="text-right">
                        <div className="text-[11px] text-muted-foreground">Available Advance</div>
                        <div className="font-bold text-emerald-600 dark:text-emerald-400 text-sm">
                          {formatMoney(adv.unallocatedAmount, adv.currency)}
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>

          {/* Amount to Apply */}
          {selectedAdvance && (
            <div className="space-y-3 rounded-lg border border-border/80 bg-muted/10 p-3 sm:p-4">
              <div className="flex items-center justify-between">
                <Label htmlFor="amountToApply" className="text-xs font-semibold uppercase text-muted-foreground">
                  Amount to Apply (NPR) <span className="text-destructive">*</span>
                </Label>
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  onClick={handleMaxApply}
                  className="h-6 text-xs text-primary hover:text-primary font-medium px-2"
                >
                  <Sparkles className="size-3 mr-1" />
                  Apply Max (NPR {maxApplicableAmount.toLocaleString()})
                </Button>
              </div>

              <Input
                id="amountToApply"
                type="number"
                step="0.01"
                min="0.01"
                max={maxApplicableAmount}
                required
                value={amountToApply}
                onChange={(e) => setAmountToApply(e.target.value)}
                placeholder="0.00"
                className="text-base font-semibold"
              />

              <div className="flex items-center justify-between text-[11px] text-muted-foreground pt-1">
                <span>Remaining on Advance: NPR {Math.max(0, unallocatedAvailable - enteredAmountNum).toLocaleString()}</span>
                <span>Remaining on Invoice: NPR {Math.max(0, balanceDue - enteredAmountNum).toLocaleString()}</span>
              </div>
            </div>
          )}

          {/* Notes */}
          <div className="space-y-1.5">
            <Label htmlFor="notes" className="text-xs font-semibold uppercase text-muted-foreground">
              Notes / Remarks
            </Label>
            <Textarea
              id="notes"
              rows={2}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder="e.g. Offset against advance PO deposit"
              className="text-xs"
            />
          </div>

          <DialogFooter className="gap-2 sm:gap-0">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => onOpenChange(false)}
              disabled={isSubmitting}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              size="sm"
              disabled={
                isSubmitting ||
                advances.length === 0 ||
                !selectedAdvanceId ||
                enteredAmountNum <= 0 ||
                enteredAmountNum > maxApplicableAmount + 0.0001
              }
            >
              {isSubmitting ? (
                <>
                  <Loader2 className="mr-2 size-4 animate-spin" />
                  Applying Credit…
                </>
              ) : (
                <>
                  <CheckCircle2 className="mr-2 size-4" />
                  Confirm Offset
                </>
              )}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

