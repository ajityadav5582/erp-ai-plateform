"use client";

import { useMemo, useState } from "react";
import {
  CheckCircle2,
  Loader2,
  PackageCheck,
  Sparkles,
} from "lucide-react";
import { toast } from "sonner";
import {
  useGetOutstandingPurchaseOrdersQuery,
} from "@/services/purchase-order.service";
import {
  useFastMatchPoMutation,
  type PurchaseInvoice,
} from "@/services/purchase-invoice.service";
import { useGetActiveWarehousesQuery } from "@/services/warehouse.service";
import { getApiErrorMessage } from "@/services/error-handler";
import { formatDate, formatMoney } from "@/lib/procurement-format";
import { SupplierBalanceBadge } from "@/components/procurement/supplier-balance-badge";

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
import { Badge } from "@/components/ui/badge";

interface FastMatchPoDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onCreated?: (invoice: PurchaseInvoice) => void;
}

export function FastMatchPoDialog({
  open,
  onOpenChange,
  onCreated,
}: FastMatchPoDialogProps) {
  const { data: poPage, isLoading: isLoadingPos } =
    useGetOutstandingPurchaseOrdersQuery(undefined, { skip: !open });
  const outstandingPos = useMemo(() => poPage?.content ?? [], [poPage?.content]);

  const { data: activeWarehouses = [] } = useGetActiveWarehousesQuery(undefined, {
    skip: !open,
  });

  const [fastMatchPo, { isLoading: isSubmitting }] = useFastMatchPoMutation();

  const [selectedPoId, setSelectedPoId] = useState<string>("");
  const [invoiceNumber, setInvoiceNumber] = useState("");
  const [invoiceDate, setInvoiceDate] = useState(() =>
    new Date().toISOString().slice(0, 10)
  );
  const [dueDate, setDueDate] = useState("");
  const [warehouseId, setWarehouseId] = useState<string>("");
  const [deliveryChallanNo, setDeliveryChallanNo] = useState("");
  const [notes, setNotes] = useState("");

  const selectedPo = useMemo(
    () => outstandingPos.find((po) => String(po.id) === selectedPoId),
    [outstandingPos, selectedPoId]
  );

  const handleSelectPo = (idStr: string) => {
    setSelectedPoId(idStr);
    const po = outstandingPos.find((p) => String(p.id) === idStr);
    if (po?.warehouseId) {
      setWarehouseId(String(po.warehouseId));
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!selectedPoId) {
      toast.error("Please select an approved purchase order.");
      return;
    }

    if (!invoiceNumber.trim()) {
      toast.error("Supplier's invoice number is required.");
      return;
    }

    try {
      const invoice = await fastMatchPo({
        purchaseOrderId: Number(selectedPoId),
        invoiceNumber: invoiceNumber.trim(),
        invoiceDate: invoiceDate || undefined,
        dueDate: dueDate || undefined,
        warehouseId: warehouseId ? Number(warehouseId) : undefined,
        deliveryChallanNo: deliveryChallanNo.trim() || undefined,
        notes: notes.trim() || undefined,
      }).unwrap();

      toast.success(`PO Fast-Match Complete: Invoice ${invoice.invoiceNumber} recorded`, {
        description: `Goods receipt note generated and stock posted into inventory. Total payable: ${formatMoney(
          invoice.totalAmount,
          invoice.currency
        )}.`,
      });

      onCreated?.(invoice);
      onOpenChange(false);
      // Reset form
      setSelectedPoId("");
      setInvoiceNumber("");
      setDeliveryChallanNo("");
      setNotes("");
    } catch (err) {
      toast.error(getApiErrorMessage(err) || "Failed to fast-match purchase order");
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[94vh] w-[96vw] max-w-4xl overflow-y-auto p-4 sm:p-6 lg:p-7">
        <DialogHeader className="border-b border-border pb-4">
          <div className="flex items-center gap-3">
            <div className="rounded-xl bg-primary/10 p-2.5 text-primary ring-1 ring-primary/20">
              <PackageCheck className="size-6" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <DialogTitle className="text-xl font-bold tracking-tight">
                  Direct PO Fast-Match & Invoice
                </DialogTitle>
                <Badge variant="secondary" className="text-[11px] font-medium bg-primary/10 text-primary">
                  <Sparkles className="mr-1 size-3" /> 2-in-1 Match
                </Badge>
              </div>
              <DialogDescription className="text-xs text-muted-foreground sm:text-sm">
                When goods and supplier invoice arrive together, record the Goods Receipt
                and Purchase Invoice simultaneously with full 3-way match integrity.
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-5 pt-2">
          {/* Section 1: Choose Purchase Order */}
          <div className="space-y-2 rounded-xl border border-border/80 bg-card p-4">
            <Label className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
              Select Approved Purchase Order <span className="text-destructive">*</span>
            </Label>
            {isLoadingPos ? (
              <div className="flex items-center gap-2 py-3 text-xs text-muted-foreground">
                <Loader2 className="size-4 animate-spin" />
                <span>Loading outstanding purchase orders...</span>
              </div>
            ) : outstandingPos.length === 0 ? (
              <p className="py-2 text-xs text-muted-foreground">
                No outstanding approved purchase orders found awaiting delivery.
              </p>
            ) : (
              <Select value={selectedPoId} onValueChange={handleSelectPo}>
                <SelectTrigger className="h-10 text-sm">
                  <SelectValue placeholder="Choose a purchase order to receive & bill…" />
                </SelectTrigger>
                <SelectContent>
                  {outstandingPos.map((po) => (
                    <SelectItem key={po.id} value={String(po.id)}>
                      <span className="font-semibold">{po.poNumber}</span> —{" "}
                      {po.supplierName} ({formatDate(po.orderDate)}) •{" "}
                      {formatMoney(po.totalAmount, po.currency)}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )}

            {selectedPo && (
              <div className="mt-3 space-y-2 border-t border-border/60 pt-3">
                <div className="flex flex-wrap items-center justify-between gap-2 text-xs">
                  <div>
                    Supplier: <strong className="font-medium">{selectedPo.supplierName}</strong>{" "}
                    <span className="font-mono text-muted-foreground">({selectedPo.supplierCode})</span>
                  </div>
                  <Badge variant="outline" className="font-mono">
                    Total: {formatMoney(selectedPo.totalAmount, selectedPo.currency)}
                  </Badge>
                </div>

                <SupplierBalanceBadge supplierId={selectedPo.supplierId} />

                <div className="rounded-md border border-border/60 bg-muted/20 p-2.5">
                  <span className="text-[11px] font-semibold text-muted-foreground uppercase">
                    Lines to be received & billed:
                  </span>
                  <ul className="mt-1 divide-y divide-border/40 text-xs">
                    {selectedPo.lines.map((l) => (
                      <li key={l.id} className="flex items-center justify-between py-1">
                        <span>
                          {l.itemName} <span className="text-muted-foreground font-mono text-[11px]">({l.itemCode})</span>
                        </span>
                        <span className="font-mono text-muted-foreground">
                          {l.quantity} {l.uomCode} @ {formatMoney(l.unitPrice, selectedPo.currency)}
                        </span>
                      </li>
                    ))}
                  </ul>
                </div>
              </div>
            )}
          </div>

          {/* Section 2: Invoice & Receipt Parameters */}
          <div className="grid gap-4 rounded-xl border border-border/80 bg-card p-4 sm:grid-cols-2">
            <div className="space-y-1.5">
              <Label className="text-xs font-semibold uppercase text-muted-foreground">
                Supplier Invoice Number <span className="text-destructive">*</span>
              </Label>
              <Input
                placeholder="e.g. INV-987654"
                required
                value={invoiceNumber}
                onChange={(e) => setInvoiceNumber(e.target.value)}
                disabled={isSubmitting}
                className="h-10 font-mono"
              />
            </div>

            <div className="space-y-1.5">
              <Label className="text-xs font-semibold uppercase text-muted-foreground">
                Delivery Challan / Gate Pass No.
              </Label>
              <Input
                placeholder="e.g. DC-10293"
                value={deliveryChallanNo}
                onChange={(e) => setDeliveryChallanNo(e.target.value)}
                disabled={isSubmitting}
                className="h-10 font-mono"
              />
            </div>

            <div className="space-y-1.5">
              <Label className="text-xs font-semibold uppercase text-muted-foreground">
                Invoice Date <span className="text-destructive">*</span>
              </Label>
              <Input
                type="date"
                required
                value={invoiceDate}
                onChange={(e) => setInvoiceDate(e.target.value)}
                disabled={isSubmitting}
                className="h-10"
              />
            </div>

            <div className="space-y-1.5">
              <Label className="text-xs font-semibold uppercase text-muted-foreground">
                Due Date
              </Label>
              <Input
                type="date"
                value={dueDate}
                onChange={(e) => setDueDate(e.target.value)}
                disabled={isSubmitting}
                className="h-10"
              />
            </div>

            <div className="space-y-1.5 sm:col-span-2">
              <Label className="text-xs font-semibold uppercase text-muted-foreground">
                Receiving Warehouse
              </Label>
              <Select
                value={warehouseId}
                onValueChange={setWarehouseId}
                disabled={isSubmitting}
              >
                <SelectTrigger className="h-10">
                  <SelectValue placeholder="Use order's nominated warehouse" />
                </SelectTrigger>
                <SelectContent>
                  {activeWarehouses.map((wh) => (
                    <SelectItem key={wh.id} value={String(wh.id)}>
                      {wh.name} ({wh.code})
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-1.5 sm:col-span-2">
              <Label className="text-xs font-semibold uppercase text-muted-foreground">
                Notes / Remarks
              </Label>
              <Textarea
                placeholder="Optional notes regarding the fast-matched arrival..."
                rows={2}
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                disabled={isSubmitting}
              />
            </div>
          </div>

          <DialogFooter className="gap-2 sm:gap-0 pt-2">
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
              disabled={isSubmitting}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              disabled={isSubmitting || !selectedPoId || !invoiceNumber.trim()}
              className="gap-2"
            >
              {isSubmitting ? (
                <>
                  <Loader2 className="size-4 animate-spin" />
                  Recording Receipt & Invoice...
                </>
              ) : (
                <>
                  <CheckCircle2 className="size-4" />
                  Fast-Match & Record Invoice
                </>
              )}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
