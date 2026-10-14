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
import { Truck, Loader2 } from "lucide-react";
import {
  useCreateDeliveryMutation,
  usePostDeliveryMutation,
  type CreateDeliveryRequest,
  type DeliveryLineRequest,
} from "@/services/sales-delivery.service";
import {
  useGetOpenSalesOrdersQuery,
  type SalesOrder,
  type SalesOrderLine,
} from "@/services/sales-order.service";
import { useGetActiveWarehousesQuery } from "@/services/warehouse.service";
import { formatQty } from "@/lib/sales-format";
import { getApiErrorMessage } from "@/services/error-handler";

interface CreateDeliveryDialogProps {
  order?: SalesOrder | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSuccess?: () => void;
}

interface SelectedDeliveryLine {
  checked: boolean;
  salesOrderLineNumber: number;
  itemId: number;
  itemCode: string;
  itemName: string;
  uomCode: string;
  outstandingQuantity: number;
  quantity: number;
  description: string;
}

export function CreateDeliveryDialog({
  order,
  open,
  onOpenChange,
  onSuccess,
}: CreateDeliveryDialogProps) {
  const [createDelivery, { isLoading: isCreating }] = useCreateDeliveryMutation();
  const [postDelivery, { isLoading: isPosting }] = usePostDeliveryMutation();
  const { data: openOrders, isLoading: ordersLoading } = useGetOpenSalesOrdersQuery(undefined, { skip: !open });
  const { data: warehouses, isLoading: warehousesLoading } = useGetActiveWarehousesQuery(undefined, { skip: !open });

  const [selectedOrderId, setSelectedOrderId] = React.useState<string>("");
  const [warehouseId, setWarehouseId] = React.useState<string>("");
  const [deliveryDate, setDeliveryDate] = React.useState<string>(() => new Date().toISOString().slice(0, 10));
  const [notes, setNotes] = React.useState<string>("");
  const [autoPost, setAutoPost] = React.useState<boolean>(true);
  const [lines, setLines] = React.useState<SelectedDeliveryLine[]>([]);
  const [error, setError] = React.useState<string | null>(null);

  const availableOrders = React.useMemo(() => {
    return (openOrders ?? []).filter((o) => o.lines.some((l) => l.outstandingQuantity > 0));
  }, [openOrders]);

  const activeOrder: SalesOrder | undefined = React.useMemo(() => {
    if (order) return order;
    if (selectedOrderId) {
      return availableOrders.find((o) => String(o.id) === selectedOrderId);
    }
    return undefined;
  }, [order, selectedOrderId, availableOrders]);

  React.useEffect(() => {
    if (open) {
      setError(null);
      setDeliveryDate(new Date().toISOString().slice(0, 10));
      setNotes("");
      setAutoPost(true);
      if (order) {
        setSelectedOrderId(String(order.id));
        setWarehouseId(order.warehouseId ? String(order.warehouseId) : "");
        initLines(order.lines);
      } else {
        setSelectedOrderId("");
        setWarehouseId("");
        setLines([]);
      }
    }
  }, [open, order]);

  React.useEffect(() => {
    if (!order && activeOrder) {
      if (activeOrder.warehouseId) {
        setWarehouseId(String(activeOrder.warehouseId));
      }
      initLines(activeOrder.lines);
    }
  }, [activeOrder, order]);

  const initLines = (orderLines: SalesOrderLine[]) => {
    const linesToDeliver = orderLines
      .filter((l) => l.outstandingQuantity > 0)
      .map((l) => ({
        checked: true,
        salesOrderLineNumber: l.lineNumber,
        itemId: l.itemId,
        itemCode: l.itemCode,
        itemName: l.itemName,
        uomCode: l.uomCode,
        outstandingQuantity: l.outstandingQuantity,
        quantity: l.outstandingQuantity,
        description: l.description ?? "",
      }));
    setLines(linesToDeliver);
  };

  const handleToggleLine = (lineNumber: number) => {
    setLines((prev) =>
      prev.map((l) => (l.salesOrderLineNumber === lineNumber ? { ...l, checked: !l.checked } : l))
    );
  };

  const handleQuantityChange = (lineNumber: number, val: number) => {
    setLines((prev) =>
      prev.map((l) => (l.salesOrderLineNumber === lineNumber ? { ...l, quantity: val } : l))
    );
  };

  const selectedLinesToShip = lines.filter((l) => l.checked && l.quantity > 0);
  const isLoading = isCreating || isPosting;
  const canSubmit = !!activeOrder && !!warehouseId && selectedLinesToShip.length > 0 && !isLoading;

  const handleSubmit = async () => {
    setError(null);
    if (!activeOrder) return setError("Please select a sales order.");
    if (!warehouseId) return setError("Please select a shipping warehouse.");
    if (selectedLinesToShip.length === 0) return setError("Select at least one line with a positive quantity.");

    for (const l of selectedLinesToShip) {
      if (l.quantity > l.outstandingQuantity) {
        return setError(
          `Delivery quantity (${l.quantity}) for item "${l.itemCode} — ${l.itemName}" exceeds remaining outstanding order quantity (${l.outstandingQuantity}).`
        );
      }
    }

    const payloadLines: DeliveryLineRequest[] = selectedLinesToShip.map((l) => ({
      salesOrderLineNumber: l.salesOrderLineNumber,
      itemId: l.itemId,
      uomCode: l.uomCode,
      quantity: l.quantity,
      description: l.description || undefined,
    }));

    const request: CreateDeliveryRequest = {
      salesOrderId: activeOrder.id,
      warehouseId: Number(warehouseId),
      deliveryDate,
      notes: notes || undefined,
      lines: payloadLines,
    };

    try {
      const created = await createDelivery(request).unwrap();
      if (autoPost) {
        await postDelivery(created.id).unwrap();
      }
      onOpenChange(false);
      onSuccess?.();
    } catch (err: unknown) {
      setError(getApiErrorMessage(err));
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-3xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Truck className="h-5 w-5 text-primary" />
            Create Sales Delivery
          </DialogTitle>
          <DialogDescription>
            Issue goods against a confirmed sales order. Choose the warehouse and quantities to deliver.
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
                  ) : (openOrders ?? []).length === 0 ? (
                    <div className="px-2 py-1.5 text-muted-foreground">No confirmed orders awaiting delivery</div>
                  ) : (
                    (openOrders ?? []).map((o) => (
                      <SelectItem key={o.id} value={String(o.id)}>
                        {o.soNumber} — {o.customerName} ({o.status})
                      </SelectItem>
                    ))
                  )}
                </SelectContent>
              </Select>
            </div>
          )}

          {activeOrder && (
            <div className="rounded-lg border bg-muted/40 p-3 space-y-1 text-xs">
              <div className="flex justify-between">
                <span className="font-medium text-foreground">{activeOrder.soNumber}</span>
                <span className="text-muted-foreground">{activeOrder.customerName} ({activeOrder.customerCode})</span>
              </div>
              <div className="text-muted-foreground">
                Date: {activeOrder.orderDate} · Currency: {activeOrder.currency}
              </div>
            </div>
          )}

          <div className="grid grid-cols-2 gap-3">
            <div className="grid gap-1.5">
              <Label>Shipping Warehouse</Label>
              <Select value={warehouseId} onValueChange={setWarehouseId}>
                <SelectTrigger>
                  <SelectValue placeholder={warehousesLoading ? "Loading warehouses…" : "Select warehouse"} />
                </SelectTrigger>
                <SelectContent>
                  {warehousesLoading ? (
                    <div className="flex items-center gap-2 px-2 py-1.5 text-muted-foreground">
                      <Loader2 className="h-3 w-3 animate-spin" /> Loading…
                    </div>
                  ) : (warehouses ?? []).length === 0 ? (
                    <div className="px-2 py-1.5 text-muted-foreground">No active warehouses found</div>
                  ) : (
                    (warehouses ?? []).map((w) => (
                      <SelectItem key={w.id} value={String(w.id)}>
                        {w.name} ({w.code})
                      </SelectItem>
                    ))
                  )}
                </SelectContent>
              </Select>
            </div>

            <div className="grid gap-1.5">
              <Label>Delivery Date</Label>
              <Input type="date" value={deliveryDate} onChange={(e) => setDeliveryDate(e.target.value)} />
            </div>
          </div>

          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <Label>Lines to Deliver</Label>
              <span className="text-xs text-muted-foreground">
                {selectedLinesToShip.length} of {lines.length} lines selected
              </span>
            </div>

            {lines.length === 0 ? (
              <div className="rounded-md border p-4 text-center text-sm text-muted-foreground">
                {activeOrder ? "All lines for this sales order are already fully delivered." : "Select an order to see available lines."}
              </div>
            ) : (
              <div className="rounded-md border divide-y">
                {lines.map((l) => (
                  <div key={l.salesOrderLineNumber} className="flex items-center justify-between p-3 gap-3">
                    <div className="flex items-center gap-3 flex-1 min-w-0">
                      <Checkbox
                        checked={l.checked}
                        onCheckedChange={() => handleToggleLine(l.salesOrderLineNumber)}
                      />
                      <div className="min-w-0">
                        <p className="font-medium truncate">{l.itemCode} — {l.itemName}</p>
                        <p className="text-xs text-muted-foreground">
                          Outstanding: {formatQty(l.outstandingQuantity)} {l.uomCode}
                        </p>
                      </div>
                    </div>

                    <div className="flex items-center gap-2">
                      <Label className="text-xs text-muted-foreground">Qty:</Label>
                      <Input
                        type="number"
                        min="0"
                        max={l.outstandingQuantity}
                        step="any"
                        disabled={!l.checked}
                        value={l.quantity}
                        onChange={(e) => handleQuantityChange(l.salesOrderLineNumber, Number(e.target.value))}
                        className="w-24 text-right h-8"
                      />
                      <span className="text-xs text-muted-foreground w-10">{l.uomCode}</span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="grid gap-1.5">
            <Label>Notes (Optional)</Label>
            <Input
              placeholder="e.g. Challan number, carrier, dispatch details"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
            />
          </div>

          <div className="flex items-center space-x-2 pt-1 rounded-md border p-2.5 bg-muted/20">
            <Checkbox
              id="auto-post"
              checked={autoPost}
              onCheckedChange={(checked) => setAutoPost(!!checked)}
            />
            <label
              htmlFor="auto-post"
              className="text-xs font-medium leading-none cursor-pointer text-foreground"
            >
              Dispatch immediately (issues stock and advances order progress)
            </label>
          </div>

          {error && <p className="text-sm text-red-600 font-medium">{error}</p>}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>Cancel</Button>
          <Button onClick={handleSubmit} disabled={!canSubmit}>
            {isLoading ? "Processing…" : autoPost ? "Dispatch & Deliver" : "Save as Draft"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
