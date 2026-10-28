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
import { Printer, Truck, FileText } from "lucide-react";
import type { SalesDelivery } from "@/services/sales-delivery.service";
import { formatDate, formatQty } from "@/lib/sales-format";

interface DeliveryChallanViewDialogProps {
  delivery: SalesDelivery | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function DeliveryChallanViewDialog({
  delivery,
  open,
  onOpenChange,
}: DeliveryChallanViewDialogProps) {
  const [printCount, setPrintCount] = React.useState(0);

  const handlePrint = () => {
    setPrintCount((prev) => prev + 1);
    setTimeout(() => {
      window.print();
    }, 150);
  };

  if (!open || !delivery) return null;

  const totalQuantity = (delivery.lines ?? []).reduce((sum, l) => sum + (l.quantity || 0), 0);

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-4xl max-h-[92vh] overflow-y-auto p-4 sm:p-6 print:p-0 print:border-none print:shadow-none print:max-w-none print:m-0">
        <DialogHeader className="print:hidden">
          <div className="flex items-center justify-between">
            <div>
              <DialogTitle className="text-lg flex items-center gap-2">
                <Truck className="h-5 w-5 text-primary" />
                Delivery Challan (ढुवानी चलानी / Dispatch Slip)
              </DialogTitle>
              <DialogDescription>
                Official goods dispatch slip for transport and customer receipt acknowledgment.
              </DialogDescription>
            </div>
            <Button onClick={handlePrint} size="sm" className="gap-1.5">
              <Printer className="h-4 w-4" />
              Print Challan
            </Button>
          </div>
        </DialogHeader>

        <div className="print-challan border p-6 rounded-lg bg-background text-foreground space-y-4 print:border-none print:p-0 text-xs sm:text-sm">
          {/* Header Bar */}
          <div className="flex justify-between items-center text-xs pb-1 border-b">
            <span className="font-semibold uppercase tracking-wider text-muted-foreground flex items-center gap-1.5">
              <FileText className="h-3.5 w-3.5 text-primary" />
              Goods Dispatch Slip / चलानी पत्र
            </span>
            <span
              className={`px-2 py-0.5 rounded font-bold uppercase text-[11px] ${
                printCount > 0
                  ? "bg-amber-100 text-amber-800 border border-amber-300"
                  : "bg-emerald-100 text-emerald-800 border border-emerald-300"
              }`}
            >
              {printCount > 0 ? `प्रतिलिपि (Challan Copy #${printCount})` : "सक्कल (Original Dispatch Slip)"}
            </span>
          </div>

          {/* Seller / Company Details */}
          <div className="text-center space-y-0.5 pt-1">
            <h1 className="text-lg sm:text-xl font-bold uppercase tracking-tight">
              ERP ENTERPRISE NEPAL PVT. LTD.
            </h1>
            <p className="text-xs text-muted-foreground">
              Kathmandu, Bagmati, Nepal • Tel: +977-1-4400000 • PAN: 601234567
            </p>
            <div className="pt-1">
              <span className="inline-block border-2 border-primary/60 px-4 py-0.5 rounded font-bold uppercase text-xs tracking-wider bg-primary/5">
                DELIVERY CHALLAN / ढुवानी चलानी
              </span>
            </div>
          </div>

          {/* Metadata Section: 2 Columns */}
          <div className="grid grid-cols-2 gap-4 border rounded p-3 text-xs bg-muted/20">
            <div className="space-y-1">
              <div className="flex">
                <span className="w-28 text-muted-foreground">Challan No.:</span>
                <span className="font-bold text-foreground">{delivery.deliveryNumber}</span>
              </div>
              <div className="flex">
                <span className="w-28 text-muted-foreground">Order Ref (SO):</span>
                <span className="font-medium text-foreground">{delivery.soNumber}</span>
              </div>
              <div className="flex">
                <span className="w-28 text-muted-foreground">Dispatch Date:</span>
                <span className="font-medium text-foreground">{formatDate(delivery.deliveryDate)}</span>
              </div>
              <div className="flex">
                <span className="w-28 text-muted-foreground">Dispatch Warehouse:</span>
                <span className="font-medium text-foreground">{delivery.warehouseName || `Warehouse #${delivery.warehouseId}`}</span>
              </div>
            </div>

            <div className="space-y-1">
              <div className="flex">
                <span className="w-28 text-muted-foreground">Customer:</span>
                <span className="font-bold text-foreground">{delivery.customerName}</span>
              </div>
              <div className="flex">
                <span className="w-28 text-muted-foreground">Customer Code:</span>
                <span className="font-mono text-foreground">{delivery.customerCode}</span>
              </div>
              <div className="flex">
                <span className="w-28 text-muted-foreground">Delivery Status:</span>
                <span className="font-semibold text-emerald-700">{delivery.status}</span>
              </div>
              {delivery.notes && (
                <div className="flex">
                  <span className="w-28 text-muted-foreground">Instructions:</span>
                  <span className="italic text-foreground">{delivery.notes}</span>
                </div>
              )}
            </div>
          </div>

          {/* Transport / Vehicle Details */}
          <div className="grid grid-cols-3 gap-2 border rounded p-2 text-xs bg-muted/10 text-muted-foreground">
            <div>
              <span className="font-semibold text-foreground">Vehicle / Truck No.:</span> _________________
            </div>
            <div>
              <span className="font-semibold text-foreground">Driver Name:</span> _________________
            </div>
            <div>
              <span className="font-semibold text-foreground">Driver Phone:</span> _________________
            </div>
          </div>

          {/* Line Items Table */}
          <div className="border rounded overflow-hidden">
            <table className="w-full text-xs">
              <thead className="bg-muted/60 text-muted-foreground font-semibold border-b">
                <tr>
                  <th className="p-2 text-center w-12 border-r">क्र.सं. (S.N.)</th>
                  <th className="p-2 text-left border-r">Item Code</th>
                  <th className="p-2 text-left border-r">Description of Dispatched Goods</th>
                  <th className="p-2 text-center border-r w-20">Unit</th>
                  <th className="p-2 text-right border-r w-24">Dispatched Qty</th>
                  <th className="p-2 text-left w-36">Remarks</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {delivery.lines.map((l, index) => (
                  <tr key={l.id} className="hover:bg-muted/10">
                    <td className="p-2 text-center border-r font-mono">{index + 1}</td>
                    <td className="p-2 border-r font-mono">{l.itemCode}</td>
                    <td className="p-2 border-r font-medium">
                      <div>{l.itemName}</div>
                      {l.description && (
                        <div className="text-[11px] text-muted-foreground">{l.description}</div>
                      )}
                    </td>
                    <td className="p-2 text-center border-r uppercase">{l.uomCode}</td>
                    <td className="p-2 text-right border-r font-bold">{formatQty(l.quantity)}</td>
                    <td className="p-2 text-muted-foreground text-[11px]">Good condition</td>
                  </tr>
                ))}
              </tbody>
              <tfoot className="bg-muted/30 border-t font-semibold">
                <tr>
                  <td colSpan={4} className="p-2 text-right border-r">
                    Total Dispatched Items / कुल परिमाण:
                  </td>
                  <td className="p-2 text-right border-r font-bold text-primary">
                    {formatQty(totalQuantity)}
                  </td>
                  <td className="p-2"></td>
                </tr>
              </tfoot>
            </table>
          </div>

          {/* Acknowledgement clause */}
          <div className="rounded border border-dashed p-2.5 text-[11px] text-muted-foreground leading-relaxed bg-muted/10">
            <p className="font-medium text-foreground">Receiving Clause / सामान बुझिलिने सर्त:</p>
            <p>
              माथि उल्लेखित सम्पूर्ण सामानहरू ठीक अवस्था र परिमाणमा बुझिलियौँ। कुनै क्षति वा फरक परेमा २४ घण्टाभित्र जानकारी गराउनु पर्नेछ।
              (Received the above goods in sound condition and correct quantity. Discrepancies, if any, must be reported within 24 hours).
            </p>
          </div>

          {/* Signatures Grid */}
          <div className="grid grid-cols-4 gap-4 pt-10 text-center text-xs">
            <div className="space-y-1">
              <div className="border-t border-foreground/40 pt-1 font-semibold">
                तयार गर्ने (Prepared By)
              </div>
              <p className="text-[11px] text-muted-foreground">Storekeeper</p>
            </div>
            <div className="space-y-1">
              <div className="border-t border-foreground/40 pt-1 font-semibold">
                जाँच गर्ने (Checked By)
              </div>
              <p className="text-[11px] text-muted-foreground">Dispatch In-Charge</p>
            </div>
            <div className="space-y-1">
              <div className="border-t border-foreground/40 pt-1 font-semibold">
                ढुवानीकर्ता (Driver / Carrier)
              </div>
              <p className="text-[11px] text-muted-foreground">Signature & Vehicle No.</p>
            </div>
            <div className="space-y-1">
              <div className="border-t border-foreground/40 pt-1 font-semibold">
                सामान बुझ्ने (Received By)
              </div>
              <p className="text-[11px] text-muted-foreground">Receiver Signature & Stamp</p>
            </div>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
