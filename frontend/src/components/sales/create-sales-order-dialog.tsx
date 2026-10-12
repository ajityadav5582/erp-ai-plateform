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
import { Plus, Trash2, Loader2 } from "lucide-react";
import {
  useCreateSalesOrderMutation,
  type CreateSalesOrderRequest,
  type SalesOrderLineRequest,
} from "@/services/sales-order.service";
import { useGetActiveCustomersQuery } from "@/services/customer.service";
import { useGetActiveItemsQuery } from "@/services/item.service";

interface DraftLine extends SalesOrderLineRequest {
  key: number;
}

const emptyLine = (key: number): DraftLine => ({
  key,
  itemId: 0,
  uomCode: "PCS",
  quantity: 1,
  unitPrice: 0,
});

const money = (n: number) =>
  n.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });

export function CreateSalesOrderDialog({
  open,
  onOpenChange,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const [createOrder, { isLoading }] = useCreateSalesOrderMutation();
  const { data: customers, isLoading: customersLoading } =
    useGetActiveCustomersQuery(undefined, { skip: !open });
  const { data: itemsPage, isLoading: itemsLoading } =
    useGetActiveItemsQuery({ size: 100 }, { skip: !open });
  const items = itemsPage?.content ?? [];

  const [customerId, setCustomerId] = React.useState("");
  const [orderDate, setOrderDate] = React.useState(() => new Date().toISOString().slice(0, 10));
  const [lines, setLines] = React.useState<DraftLine[]>([emptyLine(1)]);
  const [error, setError] = React.useState<string | null>(null);
  const nextKey = React.useRef(2);

  React.useEffect(() => {
    if (open) {
      setCustomerId("");
      setOrderDate(new Date().toISOString().slice(0, 10));
      setLines([emptyLine(1)]);
      nextKey.current = 2;
      setError(null);
    }
  }, [open]);

  const updateLine = (key: number, patch: Partial<DraftLine>) =>
    setLines((ls) => ls.map((l) => (l.key === key ? { ...l, ...patch } : l)));

  // Selecting an item defaults the UOM and unit price from the item master so the
  // user rarely types them; both stay editable per line.
  const selectItem = (key: number, itemId: number) => {
    const item = items.find((it) => it.id === itemId);
    updateLine(key, {
      itemId,
      uomCode: item?.uomCode ?? "PCS",
      unitPrice: item?.sellingPrice ?? 0,
    });
  };

  const validLines = lines.filter((l) => l.itemId > 0 && l.quantity > 0);
  const subtotal = validLines.reduce((sum, l) => sum + l.quantity * l.unitPrice, 0);
  const canSubmit = !!customerId && validLines.length > 0 && !isLoading;

  const submit = async () => {
    setError(null);
    if (!customerId) return setError("Select a customer.");
    if (validLines.length === 0) return setError("Add at least one line with an item and quantity.");

    const body: CreateSalesOrderRequest = {
      customerId: Number(customerId),
      orderDate,
      lines: validLines.map(({ key, ...l }) => l),
    };
    try {
      await createOrder(body).unwrap();
      onOpenChange(false);
    } catch (e: unknown) {
      const msg = (e as { data?: { message?: string } })?.data?.message ?? "Failed to create order";
      setError(msg);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-3xl">
        <DialogHeader>
          <DialogTitle>New Sales Order</DialogTitle>
          <DialogDescription>
            Raise a draft order for a customer. Unit price and UOM default from the item master and stay editable per line.
          </DialogDescription>
        </DialogHeader>

        <div className="grid gap-3 text-sm">
          <div className="grid grid-cols-2 gap-3">
            <div className="grid gap-1">
              <Label>Customer</Label>
              <Select value={customerId} onValueChange={setCustomerId}>
                <SelectTrigger>
                  <SelectValue placeholder={customersLoading ? "Loading customers…" : "Select customer"} />
                </SelectTrigger>
                <SelectContent>
                  {customersLoading ? (
                    <div className="flex items-center gap-2 px-2 py-1.5 text-muted-foreground">
                      <Loader2 className="h-3 w-3 animate-spin" /> Loading…
                    </div>
                  ) : (customers ?? []).length === 0 ? (
                    <div className="px-2 py-1.5 text-muted-foreground">No customers yet</div>
                  ) : (
                    (customers ?? []).map((c) => (
                      <SelectItem key={c.id} value={String(c.id)}>{c.name}</SelectItem>
                    ))
                  )}
                </SelectContent>
              </Select>
            </div>
            <div className="grid gap-1">
              <Label>Order date</Label>
              <Input type="date" value={orderDate} onChange={(e) => setOrderDate(e.target.value)} />
            </div>
          </div>

          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <Label>Lines</Label>
              <Button
                size="sm"
                variant="outline"
                onClick={() => setLines((ls) => [...ls, emptyLine(nextKey.current++)])}
              >
                <Plus className="mr-1 h-3 w-3" />Add line
              </Button>
            </div>

            <div className="grid grid-cols-12 gap-2 text-xs font-medium text-muted-foreground">
              <div className="col-span-5">Item</div>
              <div className="col-span-2">UOM</div>
              <div className="col-span-2 text-right">Qty</div>
              <div className="col-span-2 text-right">Unit price</div>
              <div className="col-span-1" />
            </div>

            {lines.map((l) => (
              <div key={l.key} className="grid grid-cols-12 items-center gap-2">
                <div className="col-span-5">
                  <Select
                    value={l.itemId ? String(l.itemId) : ""}
                    onValueChange={(v) => selectItem(l.key, Number(v))}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder={itemsLoading ? "Loading items…" : "Item"} />
                    </SelectTrigger>
                    <SelectContent>
                      {itemsLoading ? (
                        <div className="flex items-center gap-2 px-2 py-1.5 text-muted-foreground">
                          <Loader2 className="h-3 w-3 animate-spin" /> Loading…
                        </div>
                      ) : items.length === 0 ? (
                        <div className="px-2 py-1.5 text-muted-foreground">No items yet</div>
                      ) : (
                        items.map((it) => (
                          <SelectItem key={it.id} value={String(it.id)}>
                            {it.sku} — {it.nameEn}
                          </SelectItem>
                        ))
                      )}
                    </SelectContent>
                  </Select>
                </div>
                <Input
                  className="col-span-2"
                  placeholder="UOM"
                  value={l.uomCode}
                  onChange={(e) => updateLine(l.key, { uomCode: e.target.value })}
                />
                <Input
                  className="col-span-2 text-right"
                  type="number"
                  min="0"
                  value={l.quantity}
                  onChange={(e) => updateLine(l.key, { quantity: Number(e.target.value) })}
                />
                <Input
                  className="col-span-2 text-right"
                  type="number"
                  min="0"
                  value={l.unitPrice}
                  onChange={(e) => updateLine(l.key, { unitPrice: Number(e.target.value) })}
                />
                <Button
                  size="icon"
                  variant="ghost"
                  className="col-span-1"
                  onClick={() => setLines((ls) => ls.filter((x) => x.key !== l.key))}
                  disabled={lines.length === 1}
                  aria-label="Remove line"
                >
                  <Trash2 className="h-4 w-4" />
                </Button>
              </div>
            ))}

            <div className="flex items-center justify-between border-t pt-2 text-sm">
              <span className="text-muted-foreground">Subtotal ({validLines.length} {validLines.length === 1 ? "line" : "lines"})</span>
              <span className="font-medium tabular-nums">{money(subtotal)}</span>
            </div>
          </div>

          {error ? <p className="text-sm text-red-600">{error}</p> : null}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>Cancel</Button>
          <Button onClick={submit} disabled={!canSubmit}>
            {isLoading ? "Saving…" : "Create"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
