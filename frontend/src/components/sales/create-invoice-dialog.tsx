"use client";

import * as React from "react";
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from "@/components/ui/select";
import { Checkbox } from "@/components/ui/checkbox";
import { FileText, Loader2 } from "lucide-react";
import {
  useCreateInvoiceMutation,
  useGetInvoicedOrderIdsQuery,
  type CreateInvoiceRequest,
  type InvoiceLineRequest,
  type VatType,
  VAT_TYPES,
  VAT_TYPE_LABELS,
} from "@/services/sales-invoice.service";
import {
  useGetOpenSalesOrdersQuery,
  type SalesOrder,
  type SalesOrderLine,
} from "@/services/sales-order.service";
import { formatMoney } from "@/lib/sales-format";
import { getApiErrorMessage } from "@/services/error-handler";

interface CreateInvoiceDialogProps {
  order?: SalesOrder | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSuccess?: () => void;
}

interface SelectedInvoiceLine {
  checked: boolean;
  salesOrderLineNumber: number;
  itemId: number;
  itemCode: string;
  itemName: string;
  uomCode: string;
  orderedQuantity: number;
  deliveredQuantity: number;
  quantity: string;
  unitPrice: number;
  discountPercentage: number;
  vatType: VatType;
  vatRate: number;
  description: string;
}

const addDays = (dateStr: string, days: number): string => {
  const d = new Date(dateStr);
  d.setDate(d.getDate() + days);
  return d.toISOString().slice(0, 10);
};

export function CreateInvoiceDialog({
  order,
  open,
  onOpenChange,
  onSuccess,
}: CreateInvoiceDialogProps) {
  const [createInvoice, { isLoading }] = useCreateInvoiceMutation();
  const { data: openOrders, isLoading: ordersLoading } = useGetOpenSalesOrdersQuery(undefined, { skip: !open });
  const { data: invoicedOrderIds } = useGetInvoicedOrderIdsQuery(undefined, { skip: !open });

  const invoicedSet = React.useMemo(() => new Set(invoicedOrderIds ?? []), [invoicedOrderIds]);
  const availableOrders = React.useMemo(() => {
    return (openOrders ?? []).filter((o) => !invoicedSet.has(o.id));
  }, [openOrders, invoicedSet]);

  const [selectedOrderId, setSelectedOrderId] = React.useState<string>("");
  const [invoiceDate, setInvoiceDate] = React.useState<string>(() => new Date().toISOString().slice(0, 10));
  const [dueDate, setDueDate] = React.useState<string>("");
  const [notes, setNotes] = React.useState<string>("");
  const [lines, setLines] = React.useState<SelectedInvoiceLine[]>([]);
  const [error, setError] = React.useState<string | null>(null);

  const submittingRef = React.useRef(false);
  const prevOrderIdRef = React.useRef<number | null>(null);

  const activeOrder: SalesOrder | undefined = React.useMemo(() => {
    if (order) return order;
    if (selectedOrderId) {
      return availableOrders?.find((o) => String(o.id) === selectedOrderId);
    }
    return undefined;
  }, [order, selectedOrderId, availableOrders]);

  const initLines = React.useCallback((orderLines: SalesOrderLine[]) => {
    const mapped: SelectedInvoiceLine[] = orderLines.map((l) => ({
      checked: true,
      salesOrderLineNumber: l.lineNumber,
      itemId: l.itemId,
      itemCode: l.itemCode,
      itemName: l.itemName,
      uomCode: l.uomCode,
      orderedQuantity: l.quantity,
      deliveredQuantity: l.deliveredQuantity,
      quantity: String(l.quantity),
      unitPrice: l.unitPrice,
      discountPercentage: l.discountPercentage ?? 0,
      vatType: (l.taxRate > 0 ? "TAXABLE" : "EXEMPT") as VatType,
      vatRate: l.taxRate ?? 0,
      description: l.description ?? "",
    }));
    setLines(mapped);
  }, []);

  React.useEffect(() => {
    if (open) {
      setError(null);
      const today = new Date().toISOString().slice(0, 10);
      setInvoiceDate(today);
      setNotes("");
      prevOrderIdRef.current = null;

      if (order) {
        setSelectedOrderId(String(order.id));
        const terms = order.paymentTermsDays ?? 30;
        setDueDate(addDays(today, terms));
      } else {
        setSelectedOrderId("");
        setDueDate(addDays(today, 30));
        setLines([]);
      }
    } else {
      prevOrderIdRef.current = null;
    }
  }, [open, order]);

  React.useEffect(() => {
    if (!open || !activeOrder) return;
    if (activeOrder.id === prevOrderIdRef.current) return;
    prevOrderIdRef.current = activeOrder.id;

    const terms = activeOrder.paymentTermsDays ?? 30;
    setDueDate(addDays(invoiceDate, terms));
    initLines(activeOrder.lines);
  }, [open, activeOrder, invoiceDate, initLines]);

  const handleToggleLine = (lineNumber: number) => {
    setLines((prev) =>
      prev.map((l) => (l.salesOrderLineNumber === lineNumber ? { ...l, checked: !l.checked } : l))
    );
  };

  const handleUpdateLine = (lineNumber: number, patch: Partial<SelectedInvoiceLine>) => {
    setLines((prev) =>
      prev.map((l) => (l.salesOrderLineNumber === lineNumber ? { ...l, ...patch } : l))
    );
  };

  const selectedLinesToInvoice = lines.filter((l) => l.checked && Number(l.quantity) > 0);
  const currency = activeOrder?.currency ?? "NPR";

  const { subtotal, vatTotal, grandTotal } = React.useMemo(() => {
    let sub = 0;
    let vat = 0;
    for (const l of selectedLinesToInvoice) {
      const gross = Number(l.quantity) * l.unitPrice;
      const disc = (gross * (l.discountPercentage || 0)) / 100;
      const net = gross - disc;
      sub += net;
      if (l.vatType === "TAXABLE") {
        vat += (net * (l.vatRate || 0)) / 100;
      }
    }
    return { subtotal: sub, vatTotal: vat, grandTotal: sub + vat };
  }, [selectedLinesToInvoice]);

  const canSubmit = !!activeOrder && selectedLinesToInvoice.length > 0 && !isLoading && !submittingRef.current;

  const handleSubmit = async () => {
    if (submittingRef.current || isLoading) return;
    setError(null);
    if (!activeOrder) return setError("Please select a sales order.");
    if (!invoiceDate) return setError("Invoice date is required.");
    if (selectedLinesToInvoice.length === 0) return setError("Select at least one line to invoice.");

    if (dueDate && dueDate < invoiceDate) {
      return setError("Due date cannot be earlier than invoice date.");
    }

    for (const l of selectedLinesToInvoice) {
      const qty = Number(l.quantity);
      if (!Number.isFinite(qty) || qty <= 0) {
        return setError(`Quantity for ${l.itemCode} must be greater than zero.`);
      }
      if (qty > l.orderedQuantity) {
        return setError(`Invoice quantity for ${l.itemCode} cannot exceed ordered quantity (${l.orderedQuantity}).`);
      }
    }

    const payloadLines: InvoiceLineRequest[] = selectedLinesToInvoice.map((l) => ({
      itemId: l.itemId,
      uomCode: l.uomCode,
      quantity: Number(l.quantity),
      unitPrice: l.unitPrice,
      discountPercentage: l.discountPercentage,
      vatType: l.vatType,
      vatRate: l.vatType === "TAXABLE" ? (l.vatRate || 0) : 0,
      description: l.description || undefined,
    }));

    const request: CreateInvoiceRequest = {
      salesOrderId: activeOrder.id,
      invoiceDate,
      dueDate: dueDate || undefined,
      notes: notes || undefined,
      lines: payloadLines,
    };

    submittingRef.current = true;
    try {
      await createInvoice(request).unwrap();
      onOpenChange(false);
      onSuccess?.();
    } catch (err: unknown) {
      setError(getApiErrorMessage(err));
    } finally {
      submittingRef.current = false;
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-4xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <FileText className="h-5 w-5 text-primary" />
            Create Sales Tax Invoice (Draft)
          </DialogTitle>
          <DialogDescription>
            Create a draft tax invoice against a confirmed sales order. Official issuance is a separate step.
          </DialogDescription>
        </DialogHeader>

        <div className="grid gap-4 text-sm py-2">
          {!order && (
            <div className="grid gap-1.5">
              <Label>Confirmed Sales Order</Label>
              <Select value={selectedOrderId} onValueChange={setSelectedOrderId}>
                <SelectTrigger>
                  <SelectValue placeholder={ordersLoading ? "Loading open orders…" : "Select a confirmed sales order"} />
                </SelectTrigger>
                <SelectContent>
                  {ordersLoading ? (
                    <div className="flex items-center gap-2 px-2 py-1.5 text-muted-foreground">
                      <Loader2 className="h-3 w-3 animate-spin" /> Loading…
                    </div>
                  ) : availableOrders.length === 0 ? (
                    <div className="px-2 py-1.5 text-muted-foreground">No confirmed orders awaiting invoicing</div>
                  ) : (
                    availableOrders.map((o) => (
                      <SelectItem key={o.id} value={String(o.id)}>
                        {o.soNumber} — {o.customerName} ({formatMoney(o.totalAmount, o.currency)})
                      </SelectItem>
                    ))
                  )}
                </SelectContent>
              </Select>
            </div>
          )}

          {activeOrder && (
            <div className="rounded-lg border bg-muted/40 p-3 space-y-1 text-xs">
              <div className="flex flex-wrap justify-between gap-2">
                <span className="font-medium text-foreground">{activeOrder.soNumber}</span>
                <span className="text-muted-foreground">{activeOrder.customerName} ({activeOrder.customerCode})</span>
              </div>
              <div className="flex flex-wrap justify-between gap-2 text-muted-foreground">
                <span>Buyer PAN: {activeOrder.customerPanNumber || "None (Consumer)"}</span>
                <span>Currency: {activeOrder.currency}</span>
              </div>
              <div className="flex flex-wrap justify-between gap-2 text-muted-foreground">
                <span>Order Date: {activeOrder.orderDate}</span>
                <span>Status: {activeOrder.status}</span>
              </div>
            </div>
          )}

          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            <div className="grid gap-1.5">
              <Label>Invoice Date (AD) *</Label>
              <Input type="date" value={invoiceDate} onChange={(e) => setInvoiceDate(e.target.value)} />
            </div>

            <div className="grid gap-1.5">
              <Label>Due Date</Label>
              <Input type="date" value={dueDate} onChange={(e) => setDueDate(e.target.value)} />
            </div>
          </div>

          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <Label>Invoice Lines</Label>
              <span className="text-xs text-muted-foreground">
                {selectedLinesToInvoice.length} of {lines.length} lines selected
              </span>
            </div>

            {lines.length === 0 ? (
              <div className="rounded-md border p-4 text-center text-sm text-muted-foreground">
                {activeOrder ? "No lines available for this sales order." : "Select an order to see available lines."}
              </div>
            ) : (
              <div className="rounded-md border divide-y">
                {lines.map((l) => {
                  const lineAmount = (Number(l.quantity) * l.unitPrice) * (1 - (l.discountPercentage || 0) / 100);
                  const vatAmount = l.vatType === "TAXABLE" ? (lineAmount * (l.vatRate || 0)) / 100 : 0;
                  return (
                    <div
                      key={l.salesOrderLineNumber}
                      className={`flex flex-col p-3 gap-2 ${l.checked ? "bg-primary/5" : "opacity-60"}`}
                    >
                      <div className="flex items-center justify-between gap-2">
                        <div className="flex items-center gap-3 min-w-0">
                          <Checkbox
                            checked={l.checked}
                            onCheckedChange={() => handleToggleLine(l.salesOrderLineNumber)}
                          />
                          <div className="min-w-0">
                            <p className="font-medium truncate">{l.itemCode} — {l.itemName}</p>
                            <p className="text-xs text-muted-foreground">{l.uomCode}</p>
                          </div>
                        </div>
                        <div className="text-right shrink-0">
                          <span className="text-sm font-medium tabular-nums block">
                            {formatMoney(lineAmount + vatAmount, currency)}
                          </span>
                          <span className="text-[10px] text-muted-foreground">incl. VAT</span>
                        </div>
                      </div>

                      <div className="grid grid-cols-2 md:grid-cols-4 gap-2 pl-7 pt-1 text-xs">
                        <div className="text-muted-foreground">
                          <span className="block">Ordered: {l.orderedQuantity}</span>
                          <span className="block">Delivered: {l.deliveredQuantity}</span>
                        </div>
                        <div>
                          <Label className="text-xs text-muted-foreground">Invoice now:</Label>
                          <Input
                            type="number"
                            min="0"
                            max={l.orderedQuantity}
                            step="any"
                            disabled={!l.checked}
                            inputMode="decimal"
                            value={l.quantity}
                            onChange={(e) => handleUpdateLine(l.salesOrderLineNumber, { quantity: e.target.value })}
                            className="h-8 text-right"
                          />
                        </div>
                        <div>
                          <Label className="text-xs text-muted-foreground">Price:</Label>
                          <Input
                            type="number"
                            min="0"
                            step="any"
                            disabled={!l.checked}
                            value={l.unitPrice}
                            onChange={(e) => handleUpdateLine(l.salesOrderLineNumber, { unitPrice: Number(e.target.value) })}
                            className="h-8 text-right"
                          />
                        </div>
                        <div>
                          <Label className="text-xs text-muted-foreground">VAT Treatment:</Label>
                          <Select
                            disabled={!l.checked}
                            value={l.vatType}
                            onValueChange={(val: VatType) => handleUpdateLine(l.salesOrderLineNumber, { vatType: val })}
                          >
                            <SelectTrigger className="h-8">
                              <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                              {VAT_TYPES.map((vt) => (
                                <SelectItem key={vt} value={vt}>
                                  {VAT_TYPE_LABELS[vt]}
                                </SelectItem>
                              ))}
                            </SelectContent>
                          </Select>
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>

          <div className="rounded-lg border bg-muted/30 p-3 space-y-1 text-sm">
            <div className="flex justify-between text-muted-foreground text-xs">
              <span>Taxable Subtotal:</span>
              <span className="tabular-nums">{formatMoney(subtotal, currency)}</span>
            </div>
            <div className="flex justify-between text-muted-foreground text-xs">
              <span>VAT:</span>
              <span className="tabular-nums">{formatMoney(vatTotal, currency)}</span>
            </div>
            <div className="flex justify-between font-semibold text-foreground pt-1 border-t">
              <span>Total Invoice Amount:</span>
              <span className="tabular-nums">{formatMoney(grandTotal, currency)}</span>
            </div>
          </div>

          <div className="grid gap-1.5">
            <Label>Notes (Optional)</Label>
            <Input
              placeholder="e.g. Terms of payment, bank details, delivery reference"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
            />
          </div>

          {error && <p className="text-sm text-red-600 font-medium">{error}</p>}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>Cancel</Button>
          <Button onClick={handleSubmit} disabled={!canSubmit}>
            {isLoading ? "Creating Draft…" : "Create Draft Invoice"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
