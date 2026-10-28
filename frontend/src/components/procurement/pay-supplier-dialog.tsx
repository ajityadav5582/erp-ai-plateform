"use client";

import { useEffect, useMemo, useState } from "react";
import { Loader2, Wallet } from "lucide-react";
import { toast } from "sonner";
import {
  useGetPurchaseInvoiceQuery,
} from "@/services/purchase-invoice.service";
import {
  PAYMENT_METHODS,
  PAYMENT_METHOD_LABELS,
  REFERENCE_METHODS,
  usePaySupplierMutation,
  type PaymentMethod,
  type SupplierPayment,
} from "@/services/supplier-payment.service";
import { getApiErrorMessage } from "@/services/error-handler";
import { formatMoney } from "@/lib/procurement-format";

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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";

interface PaySupplierDialogProps {
  /** The invoice whose balance this payment settles. */
  invoiceId: number;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onPaid?: (payment: SupplierPayment) => void;
}

/**
 * Records a payment against a supplier invoice.
 *
 * **The amount is typed, not defaulted to the balance.** Part-payment is ordinary —
 * settling an invoice across two runs, or paying what the bank has released — and
 * pre-filling the full balance turns an ordinary case into a silent overpayment. The
 * field is prefilled with "pay in full" only when it is unambiguous, and stays a plain
 * input otherwise so the remaining balance can be seen as it is typed.
 *
 * Supplier and currency are not editable: they belong to the invoice, and allowing them
 * to be set here would let money be recorded against an invoice in another currency.
 */
export function PaySupplierDialog({
  invoiceId,
  open,
  onOpenChange,
  onPaid,
}: PaySupplierDialogProps) {
  const [amount, setAmount] = useState("");
  const [method, setMethod] = useState<PaymentMethod>("BANK_TRANSFER");
  const [paymentDate, setPaymentDate] = useState("");
  const [referenceNo, setReferenceNo] = useState("");
  const [notes, setNotes] = useState("");

  const [pay, { isLoading }] = usePaySupplierMutation();

  const { data: invoice, isFetching } = useGetPurchaseInvoiceQuery(invoiceId, {
    skip: !open || !invoiceId,
  });

  const balance = Number(invoice?.balanceAmount ?? 0);

  useEffect(() => {
    if (open) {
      // Prefilling the full balance is a convenience, not a decision: the clerk still
      // has to confirm the figure before it posts, and the remaining balance updates as
      // they type.
      setAmount(balance > 0 ? String(balance) : "");
      setMethod("BANK_TRANSFER");
      setPaymentDate("");
      setReferenceNo("");
      setNotes("");
    }
    // Keyed on the invoice id only, so a background refetch of the same invoice does
    // not wipe whatever the clerk has already typed.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, invoiceId]);

  const amountValue = Number(amount);
  const amountError = useMemo(() => {
    if (amount.trim() === "") return "Enter the amount being paid";
    if (!Number.isFinite(amountValue) || amountValue <= 0) {
      return "The amount must be greater than zero";
    }
    if (balance > 0 && amountValue > balance) {
      return `More than the balance of ${formatMoney(balance, invoice?.currency)}`;
    }
    return null;
  }, [amount, amountValue, balance, invoice?.currency]);

  const remaining = balance - (Number.isFinite(amountValue) ? amountValue : 0);

  const referenceExpected = REFERENCE_METHODS.includes(method);

  const handlePay = async () => {
    if (amountError) {
      toast.error(amountError);
      return;
    }

    try {
      const payment = await pay({
        purchaseInvoiceId: invoiceId,
        amount: amountValue,
        paymentMethod: method,
        ...(paymentDate ? { paymentDate } : {}),
        ...(referenceNo.trim() ? { referenceNo: referenceNo.trim() } : {}),
        ...(notes.trim() ? { notes: notes.trim() } : {}),
      }).unwrap();

      toast.success(`Payment ${payment.paymentNumber} recorded`, {
        description: `${formatMoney(payment.amount, payment.currency)} to ${payment.supplierName}.`,
      });
      onPaid?.(payment);
      onOpenChange(false);
    } catch (err) {
      toast.error("Failed to record the payment", {
        description: getApiErrorMessage(err),
      });
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>Pay supplier</DialogTitle>
          <DialogDescription>
            {invoice
              ? `Settle part or all of invoice ${invoice.invoiceNumber} from ${invoice.supplierName}.`
              : "Loading the invoice…"}
          </DialogDescription>
        </DialogHeader>

        {isFetching && !invoice && (
          <div className="flex items-center justify-center gap-2 py-6 text-sm text-muted-foreground">
            <Loader2 className="size-4 animate-spin" aria-hidden="true" />
            Loading the balance…
          </div>
        )}

        {invoice && (
          <div className="space-y-4">
            <div className="rounded-md border border-border bg-muted/40 px-3 py-2 text-sm">
              <div className="flex items-center justify-between">
                <span className="text-muted-foreground">Outstanding balance</span>
                <span className="font-semibold text-foreground">
                  {formatMoney(balance, invoice.currency)}
                </span>
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="pay-amount">
                Amount <span className="text-destructive">*</span>
              </Label>
              <Input
                id="pay-amount"
                type="number"
                inputMode="decimal"
                step="0.01"
                min="0"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                aria-invalid={amountError !== null}
                placeholder="0.00"
              />
              {amountError ? (
                <p className="text-xs text-destructive">{amountError}</p>
              ) : remaining > 0 ? (
                <p className="text-xs text-muted-foreground">
                  {formatMoney(remaining, invoice.currency)} will remain outstanding.
                </p>
              ) : amountValue > 0 ? (
                <p className="text-xs text-emerald-600 dark:text-emerald-400">
                  This settles the invoice in full.
                </p>
              ) : null}
            </div>

            <div className="space-y-2">
              <Label htmlFor="pay-method">
                Payment method <span className="text-destructive">*</span>
              </Label>
              <Select
                value={method}
                onValueChange={(value) => setMethod(value as PaymentMethod)}
              >
                <SelectTrigger id="pay-method">
                  <SelectValue placeholder="Select a method" />
                </SelectTrigger>
                <SelectContent>
                  {PAYMENT_METHODS.map((option) => (
                    <SelectItem key={option} value={option}>
                      {PAYMENT_METHOD_LABELS[option]}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label htmlFor="pay-date">
                Payment date{" "}
                <span className="font-normal text-muted-foreground">
                  (defaults to today)
                </span>
              </Label>
              <Input
                id="pay-date"
                type="date"
                value={paymentDate}
                onChange={(e) => setPaymentDate(e.target.value)}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="pay-reference">
                Reference number{" "}
                <span className="font-normal text-muted-foreground">
                  {referenceExpected ? "(expected for this method)" : "(optional)"}
                </span>
              </Label>
              <Input
                id="pay-reference"
                value={referenceNo}
                onChange={(e) => setReferenceNo(e.target.value)}
                placeholder={
                  method === "CHEQUE"
                    ? "Cheque number"
                    : method === "BANK_TRANSFER"
                      ? "Bank transfer reference"
                      : "Any reference the supplier will quote"
                }
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="pay-notes">
                Notes{" "}
                <span className="font-normal text-muted-foreground">(optional)</span>
              </Label>
              <Textarea
                id="pay-notes"
                rows={2}
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                placeholder="Paid from which account, allocation across orders…"
              />
            </div>
          </div>
        )}

        <DialogFooter>
          <Button
            type="button"
            variant="outline"
            onClick={() => onOpenChange(false)}
            disabled={isLoading}
          >
            Cancel
          </Button>
          <Button
            type="button"
            onClick={handlePay}
            disabled={isLoading || !invoice || amountError !== null}
          >
            {isLoading ? (
              <Loader2 className="mr-2 size-4 animate-spin" aria-hidden="true" />
            ) : (
              <Wallet className="mr-2 size-4" aria-hidden="true" />
            )}
            Record payment
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
