"use client";

import * as React from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Printer, Download, Loader2, CheckCircle2 } from "lucide-react";
import { useGetTaxInvoiceViewQuery } from "@/services/sales-invoice.service";
import { formatMoney, formatQty, formatDate, amountToWords } from "@/lib/sales-format";

interface TaxInvoiceViewDialogProps {
  invoiceId: number | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function TaxInvoiceViewDialog({
  invoiceId,
  open,
  onOpenChange,
}: TaxInvoiceViewDialogProps) {
  const { data, isLoading, error } = useGetTaxInvoiceViewQuery(invoiceId!, {
    skip: !open || !invoiceId,
  });

  const [printCount, setPrintCount] = React.useState(0);

  const handlePrint = () => {
    setPrintCount((prev) => prev + 1);
    setTimeout(() => {
      window.print();
    }, 150);
  };

  if (!open || !invoiceId) return null;

  const isReprint = printCount > 0;
  const currency = data?.currency ?? "NPR";

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-4xl max-h-[92vh] overflow-y-auto p-4 sm:p-6 print:p-0 print:border-none print:shadow-none print:max-w-none print:m-0">
        <DialogHeader className="print:hidden">
          <div className="flex items-center justify-between">
            <div>
              <DialogTitle className="text-lg flex items-center gap-2">
                <Printer className="h-5 w-5 text-primary" />
                Nepal IRD Tax Invoice (Schedule 5 / अनुसूची ५)
              </DialogTitle>
              <DialogDescription>
                Official legal tax invoice format compliant with Nepal VAT Act, 2052.
              </DialogDescription>
            </div>
            <Button onClick={handlePrint} size="sm" className="gap-1.5">
              <Printer className="h-4 w-4" />
              Print Invoice
            </Button>
          </div>
        </DialogHeader>

        {isLoading ? (
          <div className="flex flex-col items-center justify-center p-12 text-muted-foreground gap-3">
            <Loader2 className="h-8 w-8 animate-spin text-primary" />
            <p>Loading legal tax invoice details…</p>
          </div>
        ) : error || !data ? (
          <div className="p-8 text-center text-red-600">
            Failed to load tax invoice. Please check the network connection.
          </div>
        ) : (
          <div className="print-tax-invoice border p-6 rounded-lg bg-background text-foreground space-y-4 print:border-none print:p-0 text-xs sm:text-sm">
            {/* Watermark / Copy indicator */}
            <div className="flex justify-between items-center text-xs pb-1 border-b">
              <span className="font-semibold uppercase tracking-wider text-muted-foreground">
                Schedule - 5 (Related to Rule 17(1))
              </span>
              <span
                className={`px-2 py-0.5 rounded font-bold uppercase text-[11px] ${
                  isReprint
                    ? "bg-amber-100 text-amber-800 border border-amber-300"
                    : "bg-emerald-100 text-emerald-800 border border-emerald-300"
                }`}
              >
                {isReprint ? `प्रतिलिपि (Copy of Original - Print #${printCount})` : "सक्कल (Original)"}
              </span>
            </div>

            {/* Header: Seller Details */}
            <div className="text-center space-y-0.5 pt-1">
              <h1 className="text-lg sm:text-xl font-bold uppercase tracking-tight">
                {data.sellerName || "ERP ENTERPRISE NEPAL PVT. LTD."}
              </h1>
              <p className="text-xs text-muted-foreground">
                {data.sellerAddress || "Kathmandu, Nepal"}
              </p>
              <p className="text-xs font-semibold">
                PAN / VAT No: <span className="font-mono text-sm tracking-wider">{data.sellerPan || "123456789"}</span>
              </p>
              <div className="pt-2">
                <span className="inline-block px-4 py-1 border-2 border-foreground font-bold text-sm tracking-widest uppercase">
                  {data.documentTitle} / कर बीजक
                </span>
              </div>
            </div>

            {/* Meta details: Invoice #, Date, Buyer */}
            <div className="grid grid-cols-2 gap-4 pt-2 border-t text-xs">
              <div className="space-y-1">
                <div className="flex gap-2">
                  <span className="font-semibold w-24">Invoice No:</span>
                  <span className="font-mono font-bold text-sm">{data.invoiceNumber}</span>
                </div>
                <div className="flex gap-2">
                  <span className="font-semibold w-24">Date (B.S.):</span>
                  <span>{data.invoiceDateNp} वि.सं.</span>
                </div>
                <div className="flex gap-2">
                  <span className="font-semibold w-24">Date (A.D.):</span>
                  <span>{formatDate(data.invoiceDateAd.toString())}</span>
                </div>
                <div className="flex gap-2">
                  <span className="font-semibold w-24">Transaction:</span>
                  <span>Credit / Bank Transfer</span>
                </div>
              </div>

              <div className="space-y-1 bg-muted/30 p-2.5 rounded border text-xs">
                <div className="flex gap-1.5">
                  <span className="font-semibold w-20">Buyer:</span>
                  <span className="font-medium text-foreground">{data.buyerName}</span>
                </div>
                <div className="flex gap-1.5">
                  <span className="font-semibold w-20">Buyer PAN:</span>
                  <span className="font-mono font-bold tracking-wider">
                    {data.buyerPan || "Consumer (Non-PAN)"}
                  </span>
                </div>
                <div className="flex gap-1.5">
                  <span className="font-semibold w-20">Address:</span>
                  <span>{data.buyerAddress || "Nepal"}</span>
                </div>
              </div>
            </div>

            {/* Line Items Table */}
            <div className="overflow-x-auto pt-2">
              <table className="w-full border-collapse border border-foreground/30 text-xs">
                <thead>
                  <tr className="bg-muted/70 text-center font-semibold">
                    <th className="border border-foreground/30 p-1.5 w-10">S.N.</th>
                    <th className="border border-foreground/30 p-1.5 text-left">Description of Goods / Services</th>
                    <th className="border border-foreground/30 p-1.5 w-14">UOM</th>
                    <th className="border border-foreground/30 p-1.5 w-16 text-right">Qty</th>
                    <th className="border border-foreground/30 p-1.5 w-24 text-right">Rate</th>
                    <th className="border border-foreground/30 p-1.5 w-24 text-right">Amount</th>
                    <th className="border border-foreground/30 p-1.5 w-16 text-right">Disc %</th>
                    <th className="border border-foreground/30 p-1.5 w-24 text-right">Taxable Amt</th>
                  </tr>
                </thead>
                <tbody>
                  {data.lines.map((l) => {
                    const gross = l.quantity * l.unitPrice;
                    const disc = (gross * (l.discountPercentage || 0)) / 100;
                    return (
                      <tr key={l.lineNumber} className="divide-x divide-foreground/30">
                        <td className="border border-foreground/30 p-1.5 text-center">{l.lineNumber}</td>
                        <td className="border border-foreground/30 p-1.5 text-left font-medium">
                          {l.itemCode} — {l.itemName}
                        </td>
                        <td className="border border-foreground/30 p-1.5 text-center">{l.uomCode}</td>
                        <td className="border border-foreground/30 p-1.5 text-right font-mono">{formatQty(l.quantity)}</td>
                        <td className="border border-foreground/30 p-1.5 text-right font-mono">{formatMoney(l.unitPrice)}</td>
                        <td className="border border-foreground/30 p-1.5 text-right font-mono">{formatMoney(gross)}</td>
                        <td className="border border-foreground/30 p-1.5 text-right font-mono">
                          {l.discountPercentage ? `${l.discountPercentage}%` : "—"}
                        </td>
                        <td className="border border-foreground/30 p-1.5 text-right font-mono font-medium">
                          {formatMoney(l.lineAmount)}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>

            {/* Calculations & Totals */}
            <div className="grid grid-cols-12 gap-4 pt-1">
              {/* Left side: QR Code & Verification */}
              <div className="col-span-7 flex flex-col justify-between p-2 rounded border bg-muted/10 text-xs">
                <div>
                  <p className="font-semibold mb-1">In Words (अक्षरेपी):</p>
                  <p className="italic font-medium text-foreground capitalize">
                    {amountToWords(data.total, currency)}
                  </p>
                </div>
                {data.qrPayload && (
                  <div className="pt-2 border-t mt-2 flex items-center gap-2 text-[11px] text-muted-foreground">
                    <div className="border p-1 bg-white inline-block rounded">
                      <div className="w-14 h-14 bg-foreground/10 border border-foreground/30 flex items-center justify-center text-[10px] text-center font-mono">
                        IRD QR
                      </div>
                    </div>
                    <div className="space-y-0.5">
                      <p className="font-semibold text-foreground">Verified Digital Invoice</p>
                      <p className="font-mono text-[10px] break-all">{data.qrPayload}</p>
                    </div>
                  </div>
                )}
              </div>

              {/* Right side: Calculations */}
              <div className="col-span-5 border rounded p-2 text-xs divide-y divide-muted space-y-1">
                <div className="flex justify-between pb-1">
                  <span className="text-muted-foreground">Subtotal (Net):</span>
                  <span className="font-mono font-medium">{formatMoney(data.subtotal, currency)}</span>
                </div>
                <div className="flex justify-between py-1">
                  <span className="text-muted-foreground">Taxable Sales:</span>
                  <span className="font-mono font-medium">{formatMoney(data.subtotal, currency)}</span>
                </div>
                <div className="flex justify-between py-1">
                  <span className="text-muted-foreground">VAT 13%:</span>
                  <span className="font-mono font-medium">{formatMoney(data.vatAmount, currency)}</span>
                </div>
                <div className="flex justify-between pt-1.5 font-bold text-sm border-t border-foreground/30">
                  <span>Grand Total:</span>
                  <span className="font-mono text-base">{formatMoney(data.total, currency)}</span>
                </div>
              </div>
            </div>

            {/* Signatures */}
            <div className="grid grid-cols-2 pt-10 text-xs text-center border-t border-dashed">
              <div className="space-y-1">
                <div className="w-40 border-t border-foreground/40 mx-auto" />
                <p className="font-semibold">बुझिलिनेको दस्तखत</p>
                <p className="text-[11px] text-muted-foreground">(Customer / Received By)</p>
              </div>
              <div className="space-y-1">
                <div className="w-40 border-t border-foreground/40 mx-auto" />
                <p className="font-semibold">फाँटवाला / अख्तियार प्राप्त दस्तखत</p>
                <p className="text-[11px] text-muted-foreground">(For Seller / Authorized Signature)</p>
              </div>
            </div>

            {/* Footer legal text */}
            <div className="text-[10px] text-center text-muted-foreground pt-2 border-t">
              This tax invoice is issued electronically and maintained pursuant to Section 14Ka of the Value Added Tax Act, 2052.
            </div>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
