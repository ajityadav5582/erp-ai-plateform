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
import { Plus, Trash2, Loader2, FileText } from "lucide-react";
import {
  useCreateQuotationMutation,
  type CreateSalesQuotationRequest,
  type SalesQuotationLineRequest,
} from "@/services/sales-quotation.service";
import { useGetActiveCustomersQuery } from "@/services/customer.service";
import { useGetActiveItemsQuery } from "@/services/item.service";
import { useGetActiveWarehousesQuery } from "@/services/warehouse.service";
import { formatMoney } from "@/lib/sales-format";
import { getApiErrorMessage } from "@/services/error-handler";

interface DraftLine extends SalesQuotationLineRequest {
  key: number;
}

const emptyLine = (key: number): DraftLine => ({
  key,
  itemId: 0,
  uomCode: "PCS",
  quantity: 1,
  unitPrice: 0,
  discountPercentage: 0,
  taxRate: 13,
});

export function CreateQuotationDialog({
  open,
  onOpenChange,
  onSuccess,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSuccess?: () => void;
}) {
  const [createQuotation, { isLoading }] = useCreateQuotationMutation();
  const { data: customers, isLoading: customersLoading } =
    useGetActiveCustomersQuery(undefined, { skip: !open });
  const { data: itemsPage, isLoading: itemsLoading } =
    useGetActiveItemsQuery({ size: 100 }, { skip: !open });
  const { data: warehouses, isLoading: warehousesLoading } =
    useGetActiveWarehousesQuery(undefined, { skip: !open });
  const items = itemsPage?.content ?? [];

  const [customerId, setCustomerId] = React.useState("");
  const [warehouseId, setWarehouseId] = React.useState("");
  const [quotationDate, setQuotationDate] = React.useState(() => new Date().toISOString().slice(0, 10));
  const [validUntilDate, setValidUntilDate] = React.useState(() => {
    const d = new Date();
    d.setDate(d.getDate() + 30);
    return d.toISOString().slice(0, 10);
  });
  const [reference, setReference] = React.useState("");
  const [termsAndConditions, setTermsAndConditions] = React.useState(
    "1. Prices are inclusive of 13% VAT.\n2. Delivery within 7 business days from PO confirmation.\n3. Payment term: 30 days from invoice date."
  );
  const [notes, setNotes] = React.useState("");
  const [lines, setLines] = React.useState<DraftLine[]>([emptyLine(1)]);
  const [error, setError] = React.useState<string | null>(null);
  const nextKey = React.useRef(2);

  React.useEffect(() => {
    if (open) {
      setCustomerId("");
      setWarehouseId("");
      setQuotationDate(new Date().toISOString().slice(0, 10));
      const d = new Date();
      d.setDate(d.getDate() + 30);
      setValidUntilDate(d.toISOString().slice(0, 10));
      setReference("");
      setNotes("");
      setLines([emptyLine(1)]);
      nextKey.current = 2;
      setError(null);
    }
  }, [open]);

  const updateLine = (key: number, patch: Partial<DraftLine>) =>
    setLines((ls) => ls.map((l) => (l.key === key ? { ...l, ...patch } : l)));

  const selectItem = (key: number, itemId: number) => {
    const item = items.find((it) => it.id === itemId);
    updateLine(key, {
      itemId,
      uomCode: item?.uomCode ?? "PCS",
      unitPrice: item?.sellingPrice ?? 0,
      taxRate: 13,
      discountPercentage: 0,
    });
  };

  const validLines = lines.filter((l) => l.itemId > 0 && l.quantity > 0);

  const { subtotal, totalDiscount, taxableSubtotal, totalTax, grandTotal } = React.useMemo(() => {
    let sub = 0;
    let disc = 0;
    let tax = 0;

    for (const l of validLines) {
      const gross = l.quantity * l.unitPrice;
      const lineDisc = (gross * (l.discountPercentage ?? 0)) / 100;
      const net = gross - lineDisc;
      const lineTax = (net * (l.taxRate ?? 0)) / 100;

      sub += gross;
      disc += lineDisc;
      tax += lineTax;
    }

    return {
      subtotal: sub,
      totalDiscount: disc,
      taxableSubtotal: sub - disc,
      totalTax: tax,
      grandTotal: sub - disc + tax,
    };
  }, [validLines]);

  const canSubmit = !!customerId && validLines.length > 0 && !isLoading;

  const submit = async () => {
    setError(null);
    if (!customerId) return setError("Please select a customer.");
    if (validLines.length === 0) return setError("Add at least one item line.");

    const body: CreateSalesQuotationRequest = {
      customerId: Number(customerId),
      quotationDate,
      validUntilDate: validUntilDate || undefined,
      warehouseId: warehouseId ? Number(warehouseId) : undefined,
      reference: reference.trim() || undefined,
      termsAndConditions: termsAndConditions.trim() || undefined,
      notes: notes.trim() || undefined,
      lines: validLines.map(({ key, ...l }) => ({
        ...l,
        discountPercentage: l.discountPercentage ?? 0,
        taxRate: l.taxRate ?? 13,
      })),
    };

    try {
      await createQuotation(body).unwrap();
      onOpenChange(false);
      onSuccess?.();
    } catch (e: unknown) {
      setError(getApiErrorMessage(e));
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-4xl max-h-[92vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <FileText className="h-5 w-5 text-primary" />
            New Sales Quotation / Price Estimate
          </DialogTitle>
          <DialogDescription>
            Issue a formal quotation or estimate to prospective or existing clients with pricing, discounts, and validity terms.
          </DialogDescription>
        </DialogHeader>

        <div className="grid gap-4 text-sm py-1">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
            <div className="grid gap-1">
              <Label>Customer *</Label>
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
                      <SelectItem key={c.id} value={String(c.id)}>
                        {c.name} {c.panNumber ? `(PAN: ${c.panNumber})` : ""}
                      </SelectItem>
                    ))
                  )}
                </SelectContent>
              </Select>
            </div>

            <div className="grid gap-1">
              <Label>Quotation Date *</Label>
              <Input
                type="date"
                value={quotationDate}
                onChange={(e) => setQuotationDate(e.target.value)}
              />
            </div>

            <div className="grid gap-1">
              <Label>Valid Until</Label>
              <Input
                type="date"
                value={validUntilDate}
                onChange={(e) => setValidUntilDate(e.target.value)}
              />
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            <div className="grid gap-1">
              <Label>Warehouse / Branch</Label>
              <Select value={warehouseId} onValueChange={setWarehouseId}>
                <SelectTrigger>
                  <SelectValue placeholder={warehousesLoading ? "Loading…" : "Select fulfillment warehouse"} />
                </SelectTrigger>
                <SelectContent>
                  {warehousesLoading ? (
                    <div className="flex items-center gap-2 px-2 py-1.5 text-muted-foreground">
                      <Loader2 className="h-3 w-3 animate-spin" /> Loading…
                    </div>
                  ) : (warehouses ?? []).length === 0 ? (
                    <div className="px-2 py-1.5 text-muted-foreground">No warehouses configured</div>
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

            <div className="grid gap-1">
              <Label>Customer Inquiry / RFQ Ref</Label>
              <Input
                placeholder="e.g. RFQ-OCT-2081 or Email Ref"
                value={reference}
                onChange={(e) => setReference(e.target.value)}
              />
            </div>
          </div>

          <div className="space-y-2 pt-1 border-t">
            <div className="flex items-center justify-between">
              <Label className="text-base font-semibold">Quoted Products & Services</Label>
              <Button
                size="sm"
                variant="outline"
                onClick={() => setLines((ls) => [...ls, emptyLine(nextKey.current++)])}
              >
                <Plus className="mr-1 h-3.5 w-3.5" />Add Item
              </Button>
            </div>

            <div className="grid grid-cols-12 gap-2 text-xs font-medium text-muted-foreground px-1">
              <div className="col-span-4">Item</div>
              <div className="col-span-1">UOM</div>
              <div className="col-span-2 text-right">Qty</div>
              <div className="col-span-2 text-right">Unit Price</div>
              <div className="col-span-1 text-right">Disc %</div>
              <div className="col-span-1 text-right">VAT %</div>
              <div className="col-span-1 text-center" />
            </div>

            <div className="space-y-2">
              {lines.map((l) => (
                <div key={l.key} className="grid grid-cols-12 items-center gap-2 p-1.5 rounded-md border bg-card">
                  <div className="col-span-4">
                    <Select
                      value={l.itemId ? String(l.itemId) : ""}
                      onValueChange={(v) => selectItem(l.key, Number(v))}
                    >
                      <SelectTrigger>
                        <SelectValue placeholder={itemsLoading ? "Loading…" : "Select Item"} />
                      </SelectTrigger>
                      <SelectContent>
                        {itemsLoading ? (
                          <div className="flex items-center gap-2 px-2 py-1.5 text-muted-foreground">
                            <Loader2 className="h-3 w-3 animate-spin" /> Loading…
                          </div>
                        ) : items.length === 0 ? (
                          <div className="px-2 py-1.5 text-muted-foreground">No items found</div>
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
                    className="col-span-1 text-xs"
                    placeholder="UOM"
                    value={l.uomCode}
                    onChange={(e) => updateLine(l.key, { uomCode: e.target.value })}
                  />
                  <Input
                    className="col-span-2 text-right"
                    type="number"
                    min="0"
                    step="any"
                    value={l.quantity}
                    onChange={(e) => updateLine(l.key, { quantity: Number(e.target.value) })}
                  />
                  <Input
                    className="col-span-2 text-right"
                    type="number"
                    min="0"
                    step="any"
                    value={l.unitPrice}
                    onChange={(e) => updateLine(l.key, { unitPrice: Number(e.target.value) })}
                  />
                  <Input
                    className="col-span-1 text-right text-xs"
                    type="number"
                    min="0"
                    max="100"
                    value={l.discountPercentage ?? 0}
                    onChange={(e) => updateLine(l.key, { discountPercentage: Number(e.target.value) })}
                  />
                  <Input
                    className="col-span-1 text-right text-xs"
                    type="number"
                    min="0"
                    max="100"
                    value={l.taxRate ?? 13}
                    onChange={(e) => updateLine(l.key, { taxRate: Number(e.target.value) })}
                  />
                  <div className="col-span-1 flex justify-center">
                    <Button
                      size="icon"
                      variant="ghost"
                      className="h-8 w-8 text-muted-foreground hover:text-red-600"
                      onClick={() => setLines((ls) => ls.filter((x) => x.key !== l.key))}
                      disabled={lines.length === 1}
                      aria-label="Remove line"
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>
                </div>
              ))}
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-3 border-t">
              <div className="space-y-2">
                <div className="space-y-1">
                  <Label>Terms & Conditions</Label>
                  <textarea
                    rows={3}
                    className="w-full rounded-md border border-input bg-transparent px-3 py-1.5 text-xs shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                    value={termsAndConditions}
                    onChange={(e) => setTermsAndConditions(e.target.value)}
                  />
                </div>
                <div className="space-y-1">
                  <Label>Internal Remarks</Label>
                  <Input
                    placeholder="e.g. Special price approved by Sales Director"
                    value={notes}
                    onChange={(e) => setNotes(e.target.value)}
                  />
                </div>
              </div>

              <div className="rounded-lg border bg-muted/30 p-3 space-y-1.5 text-xs">
                <div className="flex justify-between text-muted-foreground">
                  <span>Gross Quoted:</span>
                  <span className="font-mono tabular-nums">{formatMoney(subtotal)}</span>
                </div>
                {totalDiscount > 0 && (
                  <div className="flex justify-between text-muted-foreground">
                    <span>Discount:</span>
                    <span className="font-mono tabular-nums text-emerald-600">-{formatMoney(totalDiscount)}</span>
                  </div>
                )}
                <div className="flex justify-between text-muted-foreground">
                  <span>Taxable Subtotal:</span>
                  <span className="font-mono tabular-nums">{formatMoney(taxableSubtotal)}</span>
                </div>
                <div className="flex justify-between text-muted-foreground">
                  <span>VAT (13%):</span>
                  <span className="font-mono tabular-nums">{formatMoney(totalTax)}</span>
                </div>
                <div className="flex justify-between font-bold text-sm text-foreground pt-1.5 border-t">
                  <span>Grand Total (NPR):</span>
                  <span className="font-mono text-base tabular-nums">{formatMoney(grandTotal)}</span>
                </div>
              </div>
            </div>
          </div>

          {error ? <p className="text-sm text-red-600 font-medium">{error}</p> : null}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>Cancel</Button>
          <Button onClick={submit} disabled={!canSubmit}>
            {isLoading ? "Saving…" : "Generate Quotation"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
