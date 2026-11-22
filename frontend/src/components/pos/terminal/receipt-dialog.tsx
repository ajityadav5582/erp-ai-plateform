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
import { useSelectedCompany } from "@/config/company-context";
import { useGetCompanyQuery } from "@/services/company.service";
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
  const selectedCompany = useSelectedCompany();
  const { data: fullCompany } = useGetCompanyQuery(selectedCompany?.id ?? 0, {
    skip: !selectedCompany?.id,
  });

  React.useEffect(() => {
    if (!open) return;
    const handler = (event: KeyboardEvent) => {
      if (event.key.toLowerCase() === "p" && !event.metaKey && !event.ctrlKey) {
        event.preventDefault();
        window.print();
      }
    };
    window.addEventListener("keydown", handler);
    return () => window.removeEventListener("keydown", handler);
  }, [open]);

  if (!receipt) return null;
  const { transaction, sessionNumber, registerCode, registerName } = receipt;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader className="print:hidden">
          <DialogTitle className="flex items-center gap-2 text-emerald-600">
            <CheckCircle2 className="h-5 w-5" />
            Sale complete
          </DialogTitle>
        </DialogHeader>

        <div id="pos-thermal-receipt" className="space-y-3 text-sm">
          <div className="text-center">
            {selectedCompany && (
              <>
                <p className="text-base font-bold uppercase">{fullCompany?.companyName || selectedCompany.name}</p>
                {fullCompany?.panNumber && (
                  <p className="font-mono text-xs text-muted-foreground">TPIN/PAN: {fullCompany.panNumber}</p>
                )}
                {fullCompany?.address && (
                  <p className="text-xs text-muted-foreground">{fullCompany.address}</p>
                )}
                {fullCompany?.phone && (
                  <p className="text-xs text-muted-foreground">Tel: {fullCompany.phone}</p>
                )}
                <Separator className="my-2" />
              </>
            )}
            <p className="font-semibold text-xs tracking-wider uppercase">
              {transaction.isTaxInvoice ? "कर बिजक (TAX INVOICE)" : "संक्षिप्त कर बिजक (CASH BILL)"}
            </p>
            {transaction.printCount && transaction.printCount > 1 && (
              <p className="text-xs font-bold text-red-600 print:text-black uppercase tracking-wider">
                प्रतिलिपि / COPY (Copy #{transaction.printCount - 1})
              </p>
            )}
            <p className="text-base font-bold font-mono">{transaction.transactionNumber}</p>
            <p className="text-xs text-muted-foreground">
              Date: {transaction.transactionDateNp} ({transaction.transactionDate})
            </p>
            <p className="text-xs text-muted-foreground">
              Counter: {registerName} ({registerCode}) · Shift: {sessionNumber}
            </p>
            <p className="text-xs text-muted-foreground">
              Cashier: {transaction.cashierName}
            </p>
            {transaction.customerName && transaction.customerName !== "Walk-in Customer" && (
              <div className="mt-1 border-t border-dashed pt-1 text-left text-xs">
                <p>Buyer: <span className="font-medium">{transaction.customerName}</span></p>
                {transaction.customerPanNumber && (
                  <p className="font-mono">Buyer PAN: {transaction.customerPanNumber}</p>
                )}
              </div>
            )}
          </div>

          <Separator />

          <ul className="space-y-1">
            {transaction.lines.map((line) => (
              <li key={line.id} className="flex justify-between gap-2">
                <span className="min-w-0 truncate">
                  {line.quantity} {line.uomCode} × {line.itemName}
                </span>
                <span className="shrink-0">{currency(line.lineAmount)}</span>
              </li>
            ))}
          </ul>

          <Separator />

          <div className="flex justify-between text-xs">
            <span className="text-muted-foreground">Subtotal</span>
            <span>{currency(transaction.subtotalAmount)}</span>
          </div>
          {transaction.discountAmount != null && transaction.discountAmount > 0 && (
            <div className="flex justify-between text-xs text-emerald-600">
              <span>Discount</span>
              <span>-{currency(transaction.discountAmount)}</span>
            </div>
          )}
          <div className="flex justify-between text-xs">
            <span className="text-muted-foreground">Taxable Amount</span>
            <span>{currency(transaction.taxableAmount)}</span>
          </div>
          <div className="flex justify-between text-xs">
            <span className="text-muted-foreground">VAT (13%)</span>
            <span>{currency(transaction.vatAmount)}</span>
          </div>
          <div className="flex justify-between text-base font-bold border-t border-b border-dashed py-1">
            <span>Grand Total</span>
            <span>{currency(transaction.totalAmount)}</span>
          </div>

          {transaction.paymentMethod && (
            <div className="space-y-0.5 text-xs">
              <div className="flex justify-between">
                <span className="text-muted-foreground">
                  Payment: {POS_PAYMENT_METHOD_LABELS[transaction.paymentMethod]}
                </span>
                <span>{currency(transaction.amountTendered ?? transaction.totalAmount)}</span>
              </div>
              {transaction.changeAmount != null && transaction.changeAmount > 0 && (
                <div className="flex justify-between font-medium">
                  <span>Change Due</span>
                  <span>{currency(transaction.changeAmount)}</span>
                </div>
              )}
            </div>
          )}

          <p className="text-center text-xs text-muted-foreground pt-1">
            ** Thank You, Visit Again! **
          </p>
        </div>

        <div className="flex gap-2 print:hidden">
          <Button variant="outline" className="flex-1" onClick={() => window.print()}>
            <Printer className="mr-2 h-4 w-4" />
            Print Slip
          </Button>
          <Button className="flex-1" autoFocus onClick={() => onOpenChange(false)}>
            New sale
          </Button>
        </div>

        <style jsx global>{`
          @media print {
            body * {
              visibility: hidden;
            }
            #pos-thermal-receipt,
            #pos-thermal-receipt * {
              visibility: visible;
            }
            #pos-thermal-receipt {
              position: absolute;
              left: 0;
              top: 0;
              width: 80mm;
              max-width: 80mm;
              margin: 0;
              padding: 4mm;
              font-family: monospace;
              font-size: 11px;
              color: #000;
              background: #fff;
            }
          }
        `}</style>
      </DialogContent>
    </Dialog>
  );
}
