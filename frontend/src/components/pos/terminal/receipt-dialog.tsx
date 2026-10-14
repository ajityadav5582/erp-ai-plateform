"use client";

import * as React from "react";
import { CheckCircle2, Printer } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Separator } from "@/components/ui/separator";
import {
  POS_PAYMENT_METHOD_LABELS,
  type PosReceipt,
} from "@/services/pos-transaction.service";
import { formatPosCurrency as currency } from "@/lib/pos-format";

interface ReceiptDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  receipt: PosReceipt | null;
}

/**
 * The post-sale receipt.
 *
 * Shows the frozen transaction snapshot plus the session and register context,
 * exactly what a printed slip carries. `window.print()` prints just the dialog
 * on a desktop terminal; on a phone the same view is the customer's copy.
 */
export function ReceiptDialog({ open, onOpenChange, receipt }: ReceiptDialogProps) {
  if (!receipt) return null;
  const { transaction, sessionNumber, registerCode, registerName } = receipt;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2 text-emerald-600">
            <CheckCircle2 className="h-5 w-5" />
            Sale complete
          </DialogTitle>
        </DialogHeader>

        <div className="space-y-3 text-sm">
          <div className="text-center">
            <p className="text-lg font-bold">{transaction.transactionNumber}</p>
            <p className="text-muted-foreground">
              {transaction.transactionDateNp} · {registerName} ({registerCode})
            </p>
            <p className="text-muted-foreground">
              Session {sessionNumber} · {transaction.cashierName}
            </p>
          </div>

          <Separator />

          <ul className="space-y-1">
            {transaction.lines.map((line) => (
              <li key={line.id} className="flex justify-between gap-2">
                <span className="min-w-0 truncate">
                  {line.quantity} × {line.itemName}
                </span>
                <span className="shrink-0">{currency(line.lineAmount)}</span>
              </li>
            ))}
          </ul>

          <Separator />

          <div className="flex justify-between">
            <span className="text-muted-foreground">Subtotal</span>
            <span>{currency(transaction.subtotalAmount)}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-muted-foreground">VAT</span>
            <span>{currency(transaction.vatAmount)}</span>
          </div>
          <div className="flex justify-between text-base font-bold">
            <span>Total</span>
            <span>{currency(transaction.totalAmount)}</span>
          </div>

          {transaction.paymentMethod && (
            <>
              <Separator />
              <div className="flex justify-between">
                <span className="text-muted-foreground">
                  {POS_PAYMENT_METHOD_LABELS[transaction.paymentMethod]}
                </span>
                <span>{currency(transaction.amountTendered ?? transaction.totalAmount)}</span>
              </div>
              {transaction.changeAmount != null && transaction.changeAmount > 0 && (
                <div className="flex justify-between font-medium">
                  <span>Change</span>
                  <span>{currency(transaction.changeAmount)}</span>
                </div>
              )}
            </>
          )}

          <p className="text-center text-xs text-muted-foreground">
            {transaction.isTaxInvoice ? "Tax invoice" : "Invoice"} · Thank you!
          </p>
        </div>

        <div className="flex gap-2">
          <Button variant="outline" className="flex-1" onClick={() => window.print()}>
            <Printer className="mr-2 h-4 w-4" />
            Print
          </Button>
          <Button className="flex-1" onClick={() => onOpenChange(false)}>
            New sale
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
