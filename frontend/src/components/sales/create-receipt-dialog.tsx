"use client";

import * as React from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Receipt, Loader2, AlertCircle } from "lucide-react";
import {
  useCreateReceiptMutation,
  type CreateReceiptRequest,
  type PaymentMethod,
  PAYMENT_METHODS,
  PAYMENT_METHOD_LABELS,
} from "@/services/customer-receipt.service";
import {
  useGetOutstandingInvoicesQuery,
  type SalesInvoice,
} from "@/services/sales-invoice.service";
import { formatMoney, formatDate } from "@/lib/sales-format";
import { getApiErrorMessage } from "@/services/error-handler";

interface CreateReceiptDialogProps {
  invoice?: SalesInvoice | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSuccess?: () => void;
}

export function CreateReceiptDialog({
  invoice,
  open,
  onOpenChange,
  onSuccess,
}: CreateReceiptDialogProps) {
  const [createReceipt, { isLoading }] = useCreateReceiptMutation();
  const { data: outstandingInvoices, isLoading: invoicesLoading } =
    useGetOutstandingInvoicesQuery(undefined, { skip: !open });

  const [selectedInvoiceId, setSelectedInvoiceId] = React.useState<string>("");
  const [amount, setAmount] = React.useState<number>(0);
  const [paymentMethod, setPaymentMethod] = React.useState<PaymentMethod>("BANK_TRANSFER");
  const [receiptDate, setReceiptDate] = React.useState<string>(
    () => new Date().toISOString().slice(0, 10)
  );
  const [reference, setReference] = React.useState<string>("");
  const [notes, setNotes] = React.useState<string>("");
  const [error, setError] = React.useState<string | null>(null);

  const activeInvoice: SalesInvoice | undefined = React.useMemo(() => {
    if (invoice) return invoice;
    if (selectedInvoiceId) {
      return outstandingInvoices?.find((i) => String(i.id) === selectedInvoiceId);
    }
    return undefined;
  }, [invoice, selectedInvoiceId, outstandingInvoices]);

  React.useEffect(() => {
    if (open) {
      setError(null);
      setReceiptDate(new Date().toISOString().slice(0, 10));
      setReference("");
      setNotes("");
      setPaymentMethod("BANK_TRANSFER");

      if (invoice) {
        setSelectedInvoiceId(String(invoice.id));
        setAmount(invoice.balanceAmount);
      } else {
        setSelectedInvoiceId("");
        setAmount(0);
      }
    }
  }, [open, invoice]);

  React.useEffect(() => {
    if (!invoice && activeInvoice) {
      setAmount(activeInvoice.balanceAmount);
    }
  }, [activeInvoice, invoice]);

  const currency = activeInvoice?.currency ?? "NPR";
  const maxAmount = activeInvoice?.balanceAmount ?? 0;
  const isDraftInvoice = activeInvoice?.status === "DRAFT";

  const canSubmit =
    !!activeInvoice &&
    !isDraftInvoice &&
    amount > 0 &&
    amount <= maxAmount + 0.0001 &&
    !isLoading;

  const handleSubmit = async () => {
    setError(null);
    if (!activeInvoice) {
      return setError("Please select an invoice.");
    }
    if (isDraftInvoice) {
      return setError(
        `Invoice ${activeInvoice.invoiceNumber} is in DRAFT. You must Issue the invoice before recording payment.`
      );
    }
    if (amount <= 0) {
      return setError("Payment amount must be greater than zero.");
    }
    if (amount > maxAmount) {
      return setError(
        `Amount cannot exceed the remaining invoice balance of ${formatMoney(maxAmount, currency)}.`
      );
    }

    const payload: CreateReceiptRequest = {
      invoiceId: activeInvoice.id,
      amount,
      paymentMethod,
      receiptDate,
      reference: reference || undefined,
      notes: notes || undefined,
    };

    try {
      await createReceipt(payload).unwrap();
      onOpenChange(false);
      onSuccess?.();
    } catch (err: unknown) {
      setError(getApiErrorMessage(err));
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-lg max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Receipt className="h-5 w-5 text-primary" />
            Record Customer Receipt
          </DialogTitle>
          <DialogDescription>
            Record payment received against an issued tax invoice to settle customer accounts receivable.
          </DialogDescription>
        </DialogHeader>

        <div className="grid gap-4 text-sm py-2">
          {!invoice && (
            <div className="grid gap-1.5">
              <Label>Outstanding Tax Invoice</Label>
              <Select value={selectedInvoiceId} onValueChange={setSelectedInvoiceId}>
                <SelectTrigger>
                  <SelectValue
                    placeholder={
                      invoicesLoading
                        ? "Loading outstanding invoices…"
                        : "Select an issued invoice to receive payment"
                    }
                  />
                </SelectTrigger>
                <SelectContent>
                  {invoicesLoading ? (
                    <div className="flex items-center gap-2 px-2 py-1.5 text-muted-foreground">
                      <Loader2 className="h-3 w-3 animate-spin" /> Loading…
                    </div>
                  ) : (outstandingInvoices ?? []).length === 0 ? (
                    <div className="px-2 py-1.5 text-muted-foreground">
                      No issued invoices awaiting payment
                    </div>
                  ) : (
                    (outstandingInvoices ?? []).map((inv) => (
                      <SelectItem key={inv.id} value={String(inv.id)}>
                        {inv.invoiceNumber} — {inv.customerName} (Bal: {formatMoney(inv.balanceAmount, inv.currency)})
                      </SelectItem>
                    ))
                  )}
                </SelectContent>
              </Select>
            </div>
          )}

          {activeInvoice && (
            <div className="rounded-lg border bg-muted/40 p-3 space-y-1.5 text-xs">
              <div className="flex justify-between items-center">
                <span className="font-semibold text-foreground text-sm">
                  {activeInvoice.invoiceNumber}
                </span>
                <span className="font-medium text-muted-foreground">
                  {activeInvoice.customerName} ({activeInvoice.customerCode})
                </span>
              </div>
              <div className="flex justify-between text-muted-foreground">
                <span>Invoice Date: {formatDate(activeInvoice.invoiceDate)}</span>
                <span>SO Ref: {activeInvoice.soNumber}</span>
              </div>
              <div className="pt-1.5 border-t flex justify-between items-center text-xs">
                <span>Total Invoice: {formatMoney(activeInvoice.totalAmount, currency)}</span>
                <span className="font-semibold text-emerald-700 dark:text-emerald-400">
                  Balance Due: {formatMoney(activeInvoice.balanceAmount, currency)}
                </span>
              </div>

              {isDraftInvoice && (
                <div className="flex items-center gap-1.5 p-2 rounded bg-amber-500/10 text-amber-600 dark:text-amber-400 text-xs mt-1">
                  <AlertCircle className="h-4 w-4 shrink-0" />
                  <span>
                    This invoice is in DRAFT. You must <strong>Issue</strong> it first before recording payments.
                  </span>
                </div>
              )}
            </div>
          )}

          <div className="grid grid-cols-2 gap-3">
            <div className="grid gap-1.5">
              <Label>Payment Amount ({currency})</Label>
              <Input
                type="number"
                min="0.01"
                max={maxAmount}
                step="any"
                value={amount || ""}
                onChange={(e) => setAmount(Number(e.target.value))}
                placeholder="0.00"
              />
              {activeInvoice && maxAmount > 0 && amount !== maxAmount && (
                <button
                  type="button"
                  onClick={() => setAmount(maxAmount)}
                  className="text-xs text-primary hover:underline text-left mt-0.5"
                >
                  Pay full balance ({formatMoney(maxAmount, currency)})
                </button>
              )}
            </div>

            <div className="grid gap-1.5">
              <Label>Payment Method</Label>
              <Select
                value={paymentMethod}
                onValueChange={(val: PaymentMethod) => setPaymentMethod(val)}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {PAYMENT_METHODS.map((pm) => (
                    <SelectItem key={pm} value={pm}>
                      {PAYMENT_METHOD_LABELS[pm]}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="grid gap-1.5">
              <Label>Receipt Date</Label>
              <Input
                type="date"
                value={receiptDate}
                onChange={(e) => setReceiptDate(e.target.value)}
              />
            </div>

            <div className="grid gap-1.5">
              <Label>Reference / Cheque # / Tx ID</Label>
              <Input
                placeholder="e.g. CHQ-9901, E-Sewa Ref"
                value={reference}
                onChange={(e) => setReference(e.target.value)}
              />
            </div>
          </div>

          <div className="grid gap-1.5">
            <Label>Notes (Optional)</Label>
            <Input
              placeholder="e.g. Deposited in Nabil Bank Account"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
            />
          </div>

          {error && <p className="text-sm text-red-600 font-medium">{error}</p>}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button onClick={handleSubmit} disabled={!canSubmit}>
            {isLoading ? "Posting…" : "Confirm Receipt"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
