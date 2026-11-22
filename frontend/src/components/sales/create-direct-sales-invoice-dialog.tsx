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
import {
  Loader2,
  Plus,
  Trash2,
  Warehouse as WarehouseIcon,
  Zap,
} from "lucide-react";
import {
  useCreateDirectInvoiceMutation,
  type SalesInvoice,
  type VatType,
  VAT_TYPES,
  VAT_TYPE_LABELS,
} from "@/services/sales-invoice.service";
import { useGetActiveCustomersQuery } from "@/services/customer.service";
import { useGetActiveItemsQuery, type ItemListResponse } from "@/services/item.service";
import { useGetActiveWarehousesQuery } from "@/services/warehouse.service";
import { formatMoney } from "@/lib/sales-format";
import { getApiErrorMessage } from "@/services/error-handler";
import { toast } from "sonner";
import { todayISO, safeAdToBsISO } from "@/lib/nepali-date";

interface CreateDirectSalesInvoiceDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSuccess?: (invoice: SalesInvoice) => void;
}

interface DirectInvoiceLine {
  id: string;
  itemId: number | null;
  item: ItemListResponse | null;
  uomCode: string;
  quantity: number | "";
  unitPrice: number | "";
  discountPercentage: number | "";
  vatType: VatType;
  vatRate: number | "";
  description: string;
}

const createEmptyLine = (): DirectInvoiceLine => ({
  id: `line-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
  itemId: null,
  item: null,
  uomCode: "PCS",
  quantity: 1,
  unitPrice: "",
  discountPercentage: 0,
  vatType: "TAXABLE",
  vatRate: 13,
  description: "",
});

export function CreateDirectSalesInvoiceDialog({
  open,
  onOpenChange,
  onSuccess,
}: CreateDirectSalesInvoiceDialogProps) {
  const [createDirectInvoice, { isLoading }] = useCreateDirectInvoiceMutation();

  const { data: customers = [] } = useGetActiveCustomersQuery(undefined, { skip: !open });
  const { data: itemsPage } = useGetActiveItemsQuery(
    { page: 0, size: 300, sort: "nameEn,asc" },
    { skip: !open }
  );
  const items = itemsPage?.content ?? [];
  const { data: warehouses = [] } = useGetActiveWarehousesQuery(undefined, { skip: !open });

  // Header State
  const [customerSelectionType, setCustomerSelectionType] = React.useState<"existing" | "walkin">("walkin");
  const [selectedCustomerId, setSelectedCustomerId] = React.useState<string>("");
  const [walkinName, setWalkinName] = React.useState("Walk-in Cash Customer");
  const [walkinPan, setWalkinPan] = React.useState("");
  const [warehouseId, setWarehouseId] = React.useState<string>("");
  const [invoiceDate, setInvoiceDate] = React.useState<string>(() => todayISO());
  const [dueDate, setDueDate] = React.useState<string>("");
  const [notes, setNotes] = React.useState<string>("");

  // Settlement State
  const [isSettled, setIsSettled] = React.useState(true);
  const [paymentMethod, setPaymentMethod] = React.useState("CASH");
  const [paymentReference, setPaymentReference] = React.useState("");

  // Lines State
  const [lines, setLines] = React.useState<DirectInvoiceLine[]>([createEmptyLine()]);
  const [error, setError] = React.useState<string | null>(null);

  // Auto-select first warehouse
  React.useEffect(() => {
    if (warehouses[0] && !warehouseId) {
      setWarehouseId(String(warehouses[0].id));
    }
  }, [warehouses, warehouseId]);

  const nepaliDate = React.useMemo(() => safeAdToBsISO(invoiceDate), [invoiceDate]);

  const handleItemSelect = (lineId: string, itemIdStr: string) => {
    const selectedItem = items.find((i) => String(i.id) === itemIdStr);
    if (!selectedItem) return;

    setLines((prev) =>
      prev.map((l) => {
        if (l.id !== lineId) return l;

        const effectiveVatType: VatType =
          selectedItem.taxabilityType === "EXEMPT"
            ? "EXEMPT"
            : selectedItem.taxabilityType === "ZERO_RATED"
            ? "ZERO_RATED"
            : "TAXABLE";

        const effectiveVatRate =
          effectiveVatType === "TAXABLE" ? (selectedItem.vatRate ?? 13) : 0;

        return {
          ...l,
          itemId: selectedItem.id,
          item: selectedItem,
          uomCode: selectedItem.uomCode ?? "PCS",
          unitPrice: selectedItem.sellingPrice ?? 0,
          vatType: effectiveVatType,
          vatRate: effectiveVatRate,
          description: selectedItem.nameEn,
        };
      })
    );
  };

  const handleUpdateLine = (lineId: string, patch: Partial<DirectInvoiceLine>) => {
    setLines((prev) => prev.map((l) => (l.id === lineId ? { ...l, ...patch } : l)));
  };

  const handleRemoveLine = (lineId: string) => {
    if (lines.length <= 1) {
      setLines([createEmptyLine()]);
      return;
    }
    setLines((prev) => prev.filter((l) => l.id !== lineId));
  };

  const handleAddLine = () => {
    setLines((prev) => [...prev, createEmptyLine()]);
  };

  // Calculations
  const totals = React.useMemo(() => {
    let subtotal = 0;
    let taxable = 0;
    let exempt = 0;
    let zeroRated = 0;
    let vat = 0;

    for (const line of lines) {
      const qty = typeof line.quantity === "number" ? line.quantity : 0;
      const price = typeof line.unitPrice === "number" ? line.unitPrice : 0;
      const discount = typeof line.discountPercentage === "number" ? line.discountPercentage : 0;
      const vRate = typeof line.vatRate === "number" ? line.vatRate : 0;

      const gross = qty * price;
      const discAmt = gross * (discount / 100);
      const lineAmt = gross - discAmt;

      subtotal += lineAmt;

      if (line.vatType === "TAXABLE") {
        taxable += lineAmt;
        vat += lineAmt * (vRate / 100);
      } else if (line.vatType === "EXEMPT") {
        exempt += lineAmt;
      } else if (line.vatType === "ZERO_RATED" || line.vatType === "EXPORT") {
        zeroRated += lineAmt;
      }
    }

    const total = subtotal + vat;
    return { subtotal, taxable, exempt, zeroRated, vat, total };
  }, [lines]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    const validLines = lines.filter((l) => l.itemId !== null && Number(l.quantity) > 0);
    if (validLines.length === 0) {
      setError("Please add at least one line item with a selected product and quantity.");
      return;
    }

    const customerId =
      customerSelectionType === "existing" && selectedCustomerId
        ? Number(selectedCustomerId)
        : null;

    const customerName =
      customerSelectionType === "existing"
        ? customers.find((c) => String(c.id) === selectedCustomerId)?.name ?? null
        : walkinName;

    const customerPanNumber =
      customerSelectionType === "existing"
        ? customers.find((c) => String(c.id) === selectedCustomerId)?.panNumber ?? null
        : walkinPan.trim() || null;

    try {
      const response = await createDirectInvoice({
        customerId,
        customerName,
        customerPanNumber,
        warehouseId: warehouseId ? Number(warehouseId) : null,
        invoiceDate,
        dueDate: dueDate || null,
        isSettled,
        paymentMethod: isSettled ? paymentMethod : null,
        paymentReference: isSettled ? paymentReference || null : null,
        notes: notes.trim() || null,
        lines: validLines.map((l) => ({
          itemId: l.itemId!,
          uomCode: l.uomCode,
          quantity: Number(l.quantity),
          unitPrice: Number(l.unitPrice) || 0,
          discountPercentage: Number(l.discountPercentage) || 0,
          vatType: l.vatType,
          vatRate: l.vatType === "TAXABLE" ? Number(l.vatRate) || 13 : 0,
          description: l.description.trim() || undefined,
        })),
      }).unwrap();

      toast.success(
        `Direct Tax Invoice ${response.invoiceNumber} created & issued successfully!`,
        {
          description: isSettled
            ? "Stock decremented from warehouse & payment marked as Settled."
            : "Stock decremented from warehouse. Balance due.",
        }
      );

      onOpenChange(false);
      onSuccess?.(response);
    } catch (err) {
      setError(getApiErrorMessage(err));
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <div className="flex items-center gap-2">
            <div className="rounded-md bg-amber-500/10 p-2 text-amber-600">
              <Zap className="size-5" />
            </div>
            <div>
              <DialogTitle className="text-lg font-semibold">
                Record Direct Sales Invoice (Spot / Walk-in Cash Sale)
              </DialogTitle>
              <DialogDescription className="text-xs text-muted-foreground">
                Issue a legal Nepal IRD tax invoice directly over the counter with automatic warehouse stock issue and immediate payment settlement.
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        {error && (
          <div className="rounded-lg border border-destructive/20 bg-destructive/10 p-3 text-xs text-destructive">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Header metadata */}
          <div className="rounded-lg border bg-muted/20 p-3.5 space-y-3">
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
              {/* Customer selection toggle */}
              <div className="space-y-1 sm:col-span-1">
                <Label className="text-xs font-medium">Customer Type</Label>
                <div className="flex items-center gap-2 pt-1">
                  <Button
                    type="button"
                    size="sm"
                    variant={customerSelectionType === "walkin" ? "default" : "outline"}
                    className="h-8 text-xs flex-1"
                    onClick={() => setCustomerSelectionType("walkin")}
                  >
                    Walk-in / Cash
                  </Button>
                  <Button
                    type="button"
                    size="sm"
                    variant={customerSelectionType === "existing" ? "default" : "outline"}
                    className="h-8 text-xs flex-1"
                    onClick={() => setCustomerSelectionType("existing")}
                  >
                    Registered B2B
                  </Button>
                </div>
              </div>

              {/* Customer details */}
              {customerSelectionType === "existing" ? (
                <div className="space-y-1 sm:col-span-2">
                  <Label className="text-xs font-medium">
                    Select Customer <span className="text-destructive">*</span>
                  </Label>
                  <Select
                    value={selectedCustomerId}
                    onValueChange={setSelectedCustomerId}
                  >
                    <SelectTrigger className="h-8 text-xs">
                      <SelectValue placeholder="Search or select customer..." />
                    </SelectTrigger>
                    <SelectContent className="max-h-56">
                      {customers.map((c) => (
                        <SelectItem key={c.id} value={String(c.id)}>
                          {c.name} ({c.code}) {c.panNumber ? `— PAN: ${c.panNumber}` : ""}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              ) : (
                <>
                  <div className="space-y-1">
                    <Label className="text-xs font-medium">Customer Name</Label>
                    <Input
                      className="h-8 text-xs"
                      value={walkinName}
                      onChange={(e) => setWalkinName(e.target.value)}
                      placeholder="e.g. Walk-in Cash Customer"
                    />
                  </div>
                  <div className="space-y-1">
                    <Label className="text-xs font-medium">Buyer PAN (Optional for B2B)</Label>
                    <Input
                      className="h-8 text-xs font-mono"
                      maxLength={9}
                      value={walkinPan}
                      onChange={(e) => setWalkinPan(e.target.value)}
                      placeholder="9-digit PAN"
                    />
                  </div>
                </>
              )}
            </div>

            <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
              {/* Warehouse for instant issue */}
              <div className="space-y-1">
                <Label className="text-xs font-medium flex items-center gap-1">
                  <WarehouseIcon className="size-3.5 text-muted-foreground" />
                  Dispatch Warehouse <span className="text-destructive">*</span>
                </Label>
                <Select value={warehouseId} onValueChange={setWarehouseId}>
                  <SelectTrigger className="h-8 text-xs">
                    <SelectValue placeholder="Select warehouse" />
                  </SelectTrigger>
                  <SelectContent>
                    {warehouses.map((w) => (
                      <SelectItem key={w.id} value={String(w.id)}>
                        {w.name} ({w.code})
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              {/* Invoice Date */}
              <div className="space-y-1">
                <Label className="text-xs font-medium">Invoice Date (AD)</Label>
                <Input
                  type="date"
                  className="h-8 text-xs"
                  value={invoiceDate}
                  onChange={(e) => setInvoiceDate(e.target.value)}
                />
                <p className="text-[10px] text-muted-foreground font-mono">
                  Bikram Sambat: {nepaliDate || "—"}
                </p>
              </div>

              {/* Due Date */}
              <div className="space-y-1">
                <Label className="text-xs font-medium">Due Date</Label>
                <Input
                  type="date"
                  className="h-8 text-xs"
                  value={dueDate}
                  onChange={(e) => setDueDate(e.target.value)}
                />
              </div>
            </div>
          </div>

          {/* Lines Table */}
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <Label className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                Line Items
              </Label>
              <Button
                type="button"
                variant="outline"
                size="sm"
                className="h-7 text-xs"
                onClick={handleAddLine}
              >
                <Plus className="mr-1 size-3.5" /> Add Item
              </Button>
            </div>

            <div className="rounded-lg border overflow-hidden">
              <table className="w-full text-xs">
                <thead className="bg-muted/40 border-b text-muted-foreground">
                  <tr>
                    <th className="p-2 text-left w-64">Product / Item</th>
                    <th className="p-2 text-left w-20">UOM</th>
                    <th className="p-2 text-right w-24">Qty</th>
                    <th className="p-2 text-right w-28">Rate (NPR)</th>
                    <th className="p-2 text-right w-20">Disc %</th>
                    <th className="p-2 text-left w-28">VAT</th>
                    <th className="p-2 text-right w-28">Amount</th>
                    <th className="p-2 w-10"></th>
                  </tr>
                </thead>
                <tbody className="divide-y">
                  {lines.map((line) => {
                    const qty = typeof line.quantity === "number" ? line.quantity : 0;
                    const price = typeof line.unitPrice === "number" ? line.unitPrice : 0;
                    const disc = typeof line.discountPercentage === "number" ? line.discountPercentage : 0;
                    const lineAmt = qty * price * (1 - disc / 100);

                    return (
                      <tr key={line.id} className="hover:bg-muted/10">
                        <td className="p-2">
                          <Select
                            value={line.itemId ? String(line.itemId) : ""}
                            onValueChange={(val) => handleItemSelect(line.id, val)}
                          >
                            <SelectTrigger className="h-8 text-xs">
                              <SelectValue placeholder="Select item..." />
                            </SelectTrigger>
                            <SelectContent className="max-h-56">
                              {items.map((itm) => (
                                <SelectItem key={itm.id} value={String(itm.id)}>
                                  {itm.nameEn} ({itm.sku})
                                </SelectItem>
                              ))}
                            </SelectContent>
                          </Select>
                        </td>
                        <td className="p-2">
                          <Input
                            className="h-8 text-xs font-mono uppercase"
                            value={line.uomCode}
                            onChange={(e) => handleUpdateLine(line.id, { uomCode: e.target.value })}
                          />
                        </td>
                        <td className="p-2">
                          <Input
                            type="number"
                            min={0.0001}
                            step="any"
                            className="h-8 text-xs text-right font-mono"
                            value={line.quantity}
                            onChange={(e) =>
                              handleUpdateLine(line.id, {
                                quantity: e.target.value === "" ? "" : Number(e.target.value),
                              })
                            }
                          />
                        </td>
                        <td className="p-2">
                          <Input
                            type="number"
                            min={0}
                            step="0.01"
                            className="h-8 text-xs text-right font-mono"
                            value={line.unitPrice}
                            onChange={(e) =>
                              handleUpdateLine(line.id, {
                                unitPrice: e.target.value === "" ? "" : Number(e.target.value),
                              })
                            }
                          />
                        </td>
                        <td className="p-2">
                          <Input
                            type="number"
                            min={0}
                            max={100}
                            step="0.1"
                            className="h-8 text-xs text-right font-mono"
                            value={line.discountPercentage}
                            onChange={(e) =>
                              handleUpdateLine(line.id, {
                                discountPercentage: e.target.value === "" ? "" : Number(e.target.value),
                              })
                            }
                          />
                        </td>
                        <td className="p-2">
                          <Select
                            value={line.vatType}
                            onValueChange={(val) =>
                              handleUpdateLine(line.id, {
                                vatType: val as VatType,
                                vatRate: val === "TAXABLE" ? 13 : 0,
                              })
                            }
                          >
                            <SelectTrigger className="h-8 text-xs">
                              <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                              {VAT_TYPES.map((t) => (
                                <SelectItem key={t} value={t}>
                                  {VAT_TYPE_LABELS[t]}
                                </SelectItem>
                              ))}
                            </SelectContent>
                          </Select>
                        </td>
                        <td className="p-2 text-right font-mono font-medium">
                          {formatMoney(lineAmt, "NPR")}
                        </td>
                        <td className="p-2 text-center">
                          <Button
                            type="button"
                            variant="ghost"
                            size="icon"
                            className="h-7 w-7 text-muted-foreground hover:text-destructive"
                            onClick={() => handleRemoveLine(line.id)}
                          >
                            <Trash2 className="size-3.5" />
                          </Button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>

          {/* Payment Settlement & Totals Grid */}
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 pt-2">
            {/* Payment Settlement */}
            <div className="rounded-lg border bg-muted/20 p-3 space-y-2.5">
              <div className="flex items-center gap-2">
                <Checkbox
                  id="directSettleCheckbox"
                  checked={isSettled}
                  onCheckedChange={(c) => setIsSettled(Boolean(c))}
                />
                <Label htmlFor="directSettleCheckbox" className="text-xs font-semibold cursor-pointer">
                  Instant Payment Settlement (Mark as Paid)
                </Label>
              </div>

              {isSettled && (
                <div className="grid grid-cols-2 gap-2 pt-1">
                  <div className="space-y-1">
                    <Label className="text-[11px] text-muted-foreground">Payment Method</Label>
                    <Select value={paymentMethod} onValueChange={setPaymentMethod}>
                      <SelectTrigger className="h-8 text-xs">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="CASH">Cash</SelectItem>
                        <SelectItem value="BANK_TRANSFER">Bank Transfer / Fonepay</SelectItem>
                        <SelectItem value="ESEWA">eSewa</SelectItem>
                        <SelectItem value="KHALTI">Khalti</SelectItem>
                        <SelectItem value="CHEQUE">Cheque</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                  <div className="space-y-1">
                    <Label className="text-[11px] text-muted-foreground">Reference / Trx ID</Label>
                    <Input
                      className="h-8 text-xs font-mono"
                      value={paymentReference}
                      onChange={(e) => setPaymentReference(e.target.value)}
                      placeholder="e.g. FP-90123"
                    />
                  </div>
                </div>
              )}

              <div className="space-y-1 pt-1">
                <Label className="text-[11px] text-muted-foreground">Notes / Remarks</Label>
                <Input
                  className="h-8 text-xs"
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  placeholder="Optional customer reference or remarks"
                />
              </div>
            </div>

            {/* Live Totals Card */}
            <div className="rounded-lg border bg-muted/30 p-3.5 space-y-2 text-xs">
              <div className="flex justify-between text-muted-foreground">
                <span>Subtotal (Net Excl. Tax):</span>
                <span className="font-mono font-medium">{formatMoney(totals.subtotal, "NPR")}</span>
              </div>
              <div className="flex justify-between text-muted-foreground">
                <span>Taxable Amount:</span>
                <span className="font-mono">{formatMoney(totals.taxable, "NPR")}</span>
              </div>
              {totals.exempt > 0 && (
                <div className="flex justify-between text-muted-foreground">
                  <span>Exempt Amount:</span>
                  <span className="font-mono">{formatMoney(totals.exempt, "NPR")}</span>
                </div>
              )}
              <div className="flex justify-between text-muted-foreground">
                <span>Output VAT (13%):</span>
                <span className="font-mono font-medium text-foreground">
                  {formatMoney(totals.vat, "NPR")}
                </span>
              </div>
              <div className="border-t pt-2 flex justify-between text-sm font-bold text-foreground">
                <span>Grand Total:</span>
                <span className="font-mono text-primary">{formatMoney(totals.total, "NPR")}</span>
              </div>
            </div>
          </div>

          <DialogFooter className="pt-2">
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
              disabled={isLoading}
            >
              Cancel
            </Button>
            <Button type="submit" disabled={isLoading} className="gap-1.5">
              {isLoading ? (
                <Loader2 className="size-4 animate-spin" />
              ) : (
                <Zap className="size-4" />
              )}
              Create & Issue Direct Invoice
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
