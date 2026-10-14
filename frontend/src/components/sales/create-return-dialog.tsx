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
import { Checkbox } from "@/components/ui/checkbox";
import { Undo, Loader2, AlertCircle } from "lucide-react";
import {
  useCreateReturnMutation,
  usePostReturnMutation,
  type CreateReturnRequest,
  type ReturnLineRequest,
} from "@/services/sales-return.service";
import {
  useGetInvoicesQuery,
  type SalesInvoice,
  type SalesInvoiceLine,
} from "@/services/sales-invoice.service";
import { useGetActiveWarehousesQuery } from "@/services/warehouse.service";
import { formatMoney, formatQty } from "@/lib/sales-format";
import { getApiErrorMessage } from "@/services/error-handler";

interface CreateReturnDialogProps {
  invoice?: SalesInvoice | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSuccess?: () => void;
}

interface SelectedReturnLine {
  checked: boolean;
  itemId: number;
  itemCode: string;
  itemName: string;
  uomCode: string;
  billedQuantity: number;
  returnQuantity: number;
  unitPrice: number;
  vatType: "TAXABLE" | "EXEMPT" | "ZERO_RATED" | "EXPORT";
  vatRate: number;
  description: string;
}

export function CreateReturnDialog({
  invoice,
  open,
  onOpenChange,
  onSuccess,
}: CreateReturnDialogProps) {
  const [createReturn, { isLoading: isCreating }] = useCreateReturnMutation();
  const [postReturn, { isLoading: isPosting }] = usePostReturnMutation();
  const { data: invoicesPage, isLoading: invoicesLoading } = useGetInvoicesQuery(
    { size: 100, sort: "createdAt,desc" },
    { skip: !open }
  );
  const { data: warehouses, isLoading: warehousesLoading } =
    useGetActiveWarehousesQuery(undefined, { skip: !open });

  const eligibleInvoices = React.useMemo(() => {
    return (invoicesPage?.content ?? []).filter(
      (inv) => inv.status === "ISSUED" || inv.status === "PARTIALLY_PAID" || inv.status === "PAID"
    );
  }, [invoicesPage]);

  const [selectedInvoiceId, setSelectedInvoiceId] = React.useState<string>("");
  const [warehouseId, setWarehouseId] = React.useState<string>("");
  const [returnDate, setReturnDate] = React.useState<string>(
    () => new Date().toISOString().slice(0, 10)
  );
  const [reason, setReason] = React.useState<string>("");
  const [autoPost, setAutoPost] = React.useState<boolean>(true);
  const [lines, setLines] = React.useState<SelectedReturnLine[]>([]);
  const [error, setError] = React.useState<string | null>(null);

  const activeInvoice: SalesInvoice | undefined = React.useMemo(() => {
    if (invoice) return invoice;
    if (selectedInvoiceId) {
      return eligibleInvoices.find((i) => String(i.id) === selectedInvoiceId);
    }
    return undefined;
  }, [invoice, selectedInvoiceId, eligibleInvoices]);

  React.useEffect(() => {
    if (open) {
      setError(null);
      setReturnDate(new Date().toISOString().slice(0, 10));
      setReason("");
      setAutoPost(true);

      if (invoice) {
        setSelectedInvoiceId(String(invoice.id));
        initLines(invoice.lines);
      } else {
        setSelectedInvoiceId("");
        setWarehouseId("");
        setLines([]);
      }
    }
  }, [open, invoice]);

  React.useEffect(() => {
    if (!invoice && activeInvoice) {
      initLines(activeInvoice.lines);
    }
  }, [activeInvoice, invoice]);

  const initLines = (invLines: SalesInvoiceLine[]) => {
    const returnLines: SelectedReturnLine[] = (invLines ?? []).map((l) => ({
      checked: true,
      itemId: l.itemId,
      itemCode: l.itemCode,
      itemName: l.itemName,
      uomCode: l.uomCode,
      billedQuantity: l.quantity,
      returnQuantity: l.quantity,
      unitPrice: l.unitPrice,
      vatType: l.vatType,
      vatRate: l.vatRate ?? 13,
      description: l.description ?? "",
    }));
    setLines(returnLines);
  };

  const handleToggleLine = (itemId: number) => {
    setLines((prev) =>
      prev.map((l) => (l.itemId === itemId ? { ...l, checked: !l.checked } : l))
    );
  };

  const handleQuantityChange = (itemId: number, val: number) => {
    setLines((prev) =>
      prev.map((l) => (l.itemId === itemId ? { ...l, returnQuantity: val } : l))
    );
  };

  const selectedLinesToReturn = lines.filter((l) => l.checked && l.returnQuantity > 0);
  const currency = activeInvoice?.currency ?? "NPR";
  const isLoading = isCreating || isPosting;

  const { creditSubtotal, creditVat, creditTotal } = React.useMemo(() => {
    let sub = 0;
    let vat = 0;
    for (const l of selectedLinesToReturn) {
      const lineSub = l.returnQuantity * l.unitPrice;
      sub += lineSub;
      if (l.vatType === "TAXABLE") {
        vat += (lineSub * (l.vatRate || 13)) / 100;
      }
    }
    return {
      creditSubtotal: sub,
      creditVat: vat,
      creditTotal: sub + vat,
    };
  }, [selectedLinesToReturn]);

  const canSubmit =
    !!activeInvoice &&
    !!warehouseId &&
    selectedLinesToReturn.length > 0 &&
    !isLoading;

  const handleSubmit = async () => {
    setError(null);
    if (!activeInvoice) return setError("Please select an invoice.");
    if (!warehouseId) return setError("Please select a restock warehouse.");
    if (selectedLinesToReturn.length === 0)
      return setError("Select at least one line with a positive return quantity.");
    if (!reason.trim()) return setError("Please state a reason for this credit note / return.");

    for (const l of selectedLinesToReturn) {
      if (l.returnQuantity > l.billedQuantity) {
        return setError(
          `Return quantity (${l.returnQuantity}) for item "${l.itemCode} — ${l.itemName}" exceeds original invoiced quantity (${l.billedQuantity}).`
        );
      }
    }

    const payloadLines: ReturnLineRequest[] = selectedLinesToReturn.map((l) => ({
      itemId: l.itemId,
      uomCode: l.uomCode,
      quantity: l.returnQuantity,
      unitPrice: l.unitPrice,
      vatType: l.vatType,
      vatRate: l.vatType === "TAXABLE" ? (l.vatRate || 13) : 0,
      description: l.description || undefined,
    }));

    const request: CreateReturnRequest = {
      invoiceId: activeInvoice.id,
      warehouseId: Number(warehouseId),
      returnDate,
      reason: reason.trim(),
      lines: payloadLines,
    };

    try {
      const created = await createReturn(request).unwrap();
      if (autoPost) {
        await postReturn(created.id).unwrap();
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
            <Undo className="h-5 w-5 text-primary" />
            Issue Sales Return / Credit Note (Anusuchi 7)
          </DialogTitle>
          <DialogDescription>
            Record goods returned by a customer against an issued tax invoice. Goods are restocked in warehouse.
          </DialogDescription>
        </DialogHeader>

        <div className="grid gap-4 text-sm py-2">
          {!invoice && (
            <div className="grid gap-1.5">
              <Label>Tax Invoice</Label>
              <Select value={selectedInvoiceId} onValueChange={setSelectedInvoiceId}>
                <SelectTrigger>
                  <SelectValue
                    placeholder={
                      invoicesLoading
                        ? "Loading invoices…"
                        : "Select an issued tax invoice to credit"
                    }
                  />
                </SelectTrigger>
                <SelectContent>
                  {invoicesLoading ? (
                    <div className="flex items-center gap-2 px-2 py-1.5 text-muted-foreground">
                      <Loader2 className="h-3 w-3 animate-spin" /> Loading…
                    </div>
                  ) : eligibleInvoices.length === 0 ? (
                    <div className="px-2 py-1.5 text-muted-foreground">
                      No issued tax invoices available
                    </div>
                  ) : (
                    eligibleInvoices.map((inv) => (
                      <SelectItem key={inv.id} value={String(inv.id)}>
                        {inv.invoiceNumber} — {inv.customerName} ({formatMoney(inv.totalAmount, inv.currency)})
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
                <span>Invoice Date (BS): {activeInvoice.invoiceDateNp}</span>
                <span>PAN: {activeInvoice.customerPanNumber || "None (Consumer)"}</span>
              </div>
              <div className="pt-1 border-t flex justify-between text-muted-foreground">
                <span>Invoice Total: {formatMoney(activeInvoice.totalAmount, currency)}</span>
                <span>Balance: {formatMoney(activeInvoice.balanceAmount, currency)}</span>
              </div>
            </div>
          )}

          <div className="grid grid-cols-2 gap-3">
            <div className="grid gap-1.5">
              <Label>Restock Warehouse</Label>
              <Select value={warehouseId} onValueChange={setWarehouseId}>
                <SelectTrigger>
                  <SelectValue
                    placeholder={warehousesLoading ? "Loading warehouses…" : "Select warehouse"}
                  />
                </SelectTrigger>
                <SelectContent>
                  {warehousesLoading ? (
                    <div className="flex items-center gap-2 px-2 py-1.5 text-muted-foreground">
                      <Loader2 className="h-3 w-3 animate-spin" /> Loading…
                    </div>
                  ) : (warehouses ?? []).length === 0 ? (
                    <div className="px-2 py-1.5 text-muted-foreground">
                      No active warehouses found
                    </div>
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
              <Label>Return Date</Label>
              <Input
                type="date"
                value={returnDate}
                onChange={(e) => setReturnDate(e.target.value)}
              />
            </div>
          </div>

          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <Label>Items to Return</Label>
              <span className="text-xs text-muted-foreground">
                {selectedLinesToReturn.length} of {lines.length} lines selected
              </span>
            </div>

            {lines.length === 0 ? (
              <div className="rounded-md border p-4 text-center text-sm text-muted-foreground">
                {activeInvoice
                  ? "No lines found on this invoice."
                  : "Select an invoice to see items."}
              </div>
            ) : (
              <div className="rounded-md border divide-y">
                {lines.map((l) => (
                  <div key={l.itemId} className="flex items-center justify-between p-3 gap-3">
                    <div className="flex items-center gap-3 flex-1 min-w-0">
                      <Checkbox
                        checked={l.checked}
                        onCheckedChange={() => handleToggleLine(l.itemId)}
                      />
                      <div className="min-w-0">
                        <p className="font-medium truncate">{l.itemCode} — {l.itemName}</p>
                        <p className="text-xs text-muted-foreground">
                          Billed: {formatQty(l.billedQuantity)} {l.uomCode} @ {formatMoney(l.unitPrice, currency)} ({l.vatType})
                        </p>
                      </div>
                    </div>

                    <div className="flex items-center gap-2">
                      <Label className="text-xs text-muted-foreground">Qty:</Label>
                      <Input
                        type="number"
                        min="0"
                        max={l.billedQuantity}
                        step="any"
                        disabled={!l.checked}
                        value={l.returnQuantity}
                        onChange={(e) => handleQuantityChange(l.itemId, Number(e.target.value))}
                        className="w-24 text-right h-8"
                      />
                      <span className="text-xs text-muted-foreground w-10">{l.uomCode}</span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="rounded-lg border bg-muted/30 p-3 space-y-1 text-sm">
            <div className="flex justify-between text-muted-foreground text-xs">
              <span>Credit Subtotal:</span>
              <span className="tabular-nums">{formatMoney(creditSubtotal, currency)}</span>
            </div>
            <div className="flex justify-between text-muted-foreground text-xs">
              <span>VAT Reversed:</span>
              <span className="tabular-nums">{formatMoney(creditVat, currency)}</span>
            </div>
            <div className="flex justify-between font-semibold text-foreground pt-1 border-t">
              <span>Total Credit Note Amount:</span>
              <span className="tabular-nums">{formatMoney(creditTotal, currency)}</span>
            </div>
          </div>

          <div className="grid gap-1.5">
            <Label>Reason for Return (Required by IRD)</Label>
            <Input
              placeholder="e.g. Damaged during transit, wrong item delivered, defect reported"
              value={reason}
              onChange={(e) => setReason(e.target.value)}
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
              Post immediately (restocks inventory in warehouse)
            </label>
          </div>

          {error && <p className="text-sm text-red-600 font-medium">{error}</p>}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button onClick={handleSubmit} disabled={!canSubmit}>
            {isLoading
              ? "Processing…"
              : autoPost
              ? "Issue & Restock"
              : "Save as Draft"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
