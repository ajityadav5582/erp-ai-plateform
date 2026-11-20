"use client";

import { useEffect, useMemo, useState } from "react";
import {
  CheckCircle2,
  Loader2,
  Package,
  Plus,
  ShoppingCart,
  Trash2,
} from "lucide-react";
import { toast } from "sonner";
import {
  useCreateDirectPurchaseOrderMutation,
  useSubmitPurchaseOrderMutation,
  type PurchaseOrder,
} from "@/services/purchase-order.service";
import {
  useGetSuppliersQuery,
  type SupplierListResponse,
} from "@/services/supplier.service";
import {
  useGetActiveItemsQuery,
  type ItemListResponse,
} from "@/services/item.service";
import { useGetBranchesQuery } from "@/services/branch.service";
import {
  useGetWarehousesQuery,
  NON_RECEIVING_WAREHOUSE_TYPES,
} from "@/services/warehouse.service";
import { getApiErrorMessage } from "@/services/error-handler";
import { formatMoney } from "@/lib/procurement-format";
import { SupplierCombobox } from "@/components/procurement/supplier-combobox";
import { ItemCombobox } from "@/components/requisitions/item-combobox";
import { todayISO, safeAdToBsISO } from "@/lib/nepali-date";

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
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Badge } from "@/components/ui/badge";

interface OrderLineState {
  id: string; // client-side key
  itemId: number | null;
  item: ItemListResponse | null;
  quantity: number | "";
  unitPrice: number | "";
  discountPercentage: number | "";
  taxRate: number | "";
  notes: string;
}

const createInitialLine = (): OrderLineState => ({
  id: `line-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
  itemId: null,
  item: null,
  quantity: 1,
  unitPrice: "",
  discountPercentage: 0,
  taxRate: 13,
  notes: "",
});

interface CreateDirectPurchaseOrderDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onCreated?: (order: PurchaseOrder) => void;
}

export function CreateDirectPurchaseOrderDialog({
  open,
  onOpenChange,
  onCreated,
}: CreateDirectPurchaseOrderDialogProps) {
  // Master data queries
  const { data: suppliersPage, isLoading: suppliersLoading } = useGetSuppliersQuery(
    { page: 0, size: 250, sort: "name,asc" },
    { skip: !open }
  );
  const suppliers = suppliersPage?.content ?? [];

  const { data: itemsPage } = useGetActiveItemsQuery(
    { page: 0, size: 250, sort: "nameEn,asc" },
    { skip: !open }
  );
  const items = useMemo(() => itemsPage?.content ?? [], [itemsPage]);

  const { data: branchesPage } = useGetBranchesQuery(
    { page: 0, size: 100 },
    { skip: !open }
  );
  const branches = useMemo(() => branchesPage?.content ?? [], [branchesPage]);

  const { data: warehousesPage } = useGetWarehousesQuery(
    { page: 0, size: 100 },
    { skip: !open }
  );
  const warehouses = useMemo(
    () =>
      (warehousesPage?.content ?? []).filter(
        (w) =>
          w.isActive &&
          (!w.type ||
            !NON_RECEIVING_WAREHOUSE_TYPES.includes(
              w.type as (typeof NON_RECEIVING_WAREHOUSE_TYPES)[number]
            ))
      ),
    [warehousesPage]
  );

  // Form State
  const [selectedSupplier, setSelectedSupplier] = useState<SupplierListResponse | null>(null);
  const [branchId, setBranchId] = useState<string>("");
  const [warehouseId, setWarehouseId] = useState<string>("");
  const [orderDate, setOrderDate] = useState<string>(todayISO());
  const [expectedDeliveryDate, setExpectedDeliveryDate] = useState<string>("");
  const [reference, setReference] = useState<string>("");
  const [notes, setNotes] = useState<string>("");
  const [currency] = useState<string>("NPR");
  const [lines, setLines] = useState<OrderLineState[]>([createInitialLine()]);
  const [errors, setErrors] = useState<Record<string, string>>({});

  // Mutations
  const [createDirectOrder, { isLoading: isCreating }] = useCreateDirectPurchaseOrderMutation();
  const [submitOrder, { isLoading: isSubmitting }] = useSubmitPurchaseOrderMutation();

  const isSaving = isCreating || isSubmitting;

  // Reset form when opened
  useEffect(() => {
    if (open) {
      setSelectedSupplier(null);
      setBranchId(branches.length > 0 && branches[0] ? String(branches[0].id) : "");
      setWarehouseId(warehouses.length > 0 && warehouses[0] ? String(warehouses[0].id) : "");
      setOrderDate(todayISO());
      setExpectedDeliveryDate("");
      setReference("");
      setNotes("");
      setLines([createInitialLine()]);
      setErrors({});
    }
  }, [open, branches, warehouses]);

  // Derived financial computations
  const calculations = useMemo(() => {
    let subtotal = 0;
    let totalDiscount = 0;
    let totalTax = 0;

    const computedLines = lines.map((l) => {
      const q = typeof l.quantity === "number" ? l.quantity : 0;
      const p = typeof l.unitPrice === "number" ? l.unitPrice : 0;
      const d = typeof l.discountPercentage === "number" ? l.discountPercentage : 0;
      const t = typeof l.taxRate === "number" ? l.taxRate : 0;

      const gross = q * p;
      const disc = gross * (d / 100);
      const net = Math.max(0, gross - disc);
      const tax = net * (t / 100);
      const total = net + tax;

      subtotal += gross;
      totalDiscount += disc;
      totalTax += tax;

      return {
        gross,
        discountAmount: disc,
        netAmount: net,
        taxAmount: tax,
        lineTotal: total,
      };
    });

    const grandTotal = subtotal - totalDiscount + totalTax;

    return {
      subtotal,
      totalDiscount,
      totalTax,
      grandTotal,
      computedLines,
    };
  }, [lines]);

  // Line item handlers
  const handleAddLine = () => {
    setLines((prev) => [...prev, createInitialLine()]);
  };

  const handleRemoveLine = (index: number) => {
    if (lines.length <= 1) {
      // Clear line instead of removing last
      setLines([createInitialLine()]);
      return;
    }
    setLines((prev) => prev.filter((_, i) => i !== index));
  };

  const handleSelectItem = (index: number, item: ItemListResponse) => {
    setLines((prev) =>
      prev.map((l, i) => {
        if (i !== index) return l;
        const defaultTax = item.taxabilityType === "EXEMPT" || item.taxabilityType === "ZERO_RATED" ? 0 : 13;
        const rawPrice = item.purchasePrice ?? item.sellingPrice;
        const defaultPrice: number | "" = typeof rawPrice === "number" ? rawPrice : "";
        return {
          ...l,
          itemId: item.id,
          item,
          unitPrice: defaultPrice,
          taxRate: defaultTax,
        };
      })
    );
  };

  const handleUpdateLine = (
    index: number,
    field: keyof OrderLineState,
    value: string | number
  ) => {
    setLines((prev) =>
      prev.map((l, i) => {
        if (i !== index) return l;
        return { ...l, [field]: value };
      })
    );
  };

  // Validation
  const validateForm = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!selectedSupplier) {
      newErrors.supplier = "Supplier is required";
    }

    if (!orderDate) {
      newErrors.orderDate = "Order date is required";
    }

    if (lines.length === 0) {
      newErrors.lines = "At least one order line is required";
    } else {
      lines.forEach((l, idx) => {
        if (!l.itemId) {
          newErrors[`line_${idx}_item`] = "Item must be selected";
        }
        if (typeof l.quantity !== "number" || l.quantity <= 0) {
          newErrors[`line_${idx}_qty`] = "Qty > 0";
        }
        if (typeof l.unitPrice !== "number" || l.unitPrice < 0) {
          newErrors[`line_${idx}_price`] = "Price >= 0";
        }
      });
    }

    setErrors(newErrors);
    if (Object.keys(newErrors).length > 0) {
      toast.error("Please correct the highlighted errors before saving");
      return false;
    }
    return true;
  };

  const handleSave = async (autoSubmit: boolean) => {
    if (!validateForm()) return;
    if (!selectedSupplier) return;

    const payload = {
      supplierId: selectedSupplier.id,
      supplierCode: selectedSupplier.code,
      supplierName: selectedSupplier.name,
      orderDate,
      expectedDeliveryDate: expectedDeliveryDate || undefined,
      branchId: branchId ? Number(branchId) : undefined,
      warehouseId: warehouseId ? Number(warehouseId) : undefined,
      currency,
      reference: reference.trim() || undefined,
      notes: notes.trim() || undefined,
      lines: lines.map((l) => ({
        itemId: l.itemId!,
        itemCode: l.item?.sku,
        itemName: l.item?.nameEn,
        uomCode: l.item?.uomCode ?? undefined,
        unitId: l.item?.uomId ?? undefined,
        quantity: Number(l.quantity),
        unitPrice: Number(l.unitPrice),
        discountPercentage: Number(l.discountPercentage || 0),
        taxRate: Number(l.taxRate || 0),
        notes: l.notes.trim() || undefined,
      })),
    };

    try {
      const createdOrder = await createDirectOrder(payload).unwrap();

      if (autoSubmit) {
        try {
          const submitted = await submitOrder(createdOrder.id).unwrap();
          toast.success(`Purchase order ${submitted.poNumber} created and submitted for approval!`);
          onCreated?.(submitted);
        } catch (subErr) {
          toast.warning(
            `Purchase order ${createdOrder.poNumber} created as draft, but auto-submission failed: ${getApiErrorMessage(subErr)}`
          );
          onCreated?.(createdOrder);
        }
      } else {
        toast.success(`Direct purchase order ${createdOrder.poNumber} created as Draft`);
        onCreated?.(createdOrder);
      }

      onOpenChange(false);
    } catch (err) {
      toast.error(getApiErrorMessage(err));
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[94vh] w-[96vw] max-w-[96vw] sm:max-w-4xl md:max-w-5xl lg:max-w-6xl xl:max-w-7xl 2xl:max-w-[1400px] overflow-y-auto p-4 sm:p-6 lg:p-7 xl:p-8">
        <DialogHeader className="border-b pb-3">
          <div className="flex items-center gap-2.5">
            <div className="flex size-9 items-center justify-center rounded-lg bg-primary/10 text-primary">
              <ShoppingCart className="size-5" />
            </div>
            <div>
              <DialogTitle className="text-lg font-bold sm:text-xl">
                Create Direct Purchase Order
              </DialogTitle>
              <DialogDescription className="text-xs text-muted-foreground">
                Issue a direct commercial order to a vendor without requiring an upstream requisition.
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        <div className="space-y-6 pt-2">
          {/* Section 1: Header Details */}
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {/* Supplier Selector */}
            <div className="space-y-1.5 sm:col-span-2 lg:col-span-1">
              <Label className="text-xs font-semibold">
                Supplier <span className="text-destructive">*</span>
              </Label>
              <SupplierCombobox
                value={selectedSupplier?.id ?? null}
                suppliers={suppliers}
                isLoading={suppliersLoading}
                onSelect={(s) => {
                  setSelectedSupplier(s);
                  if (errors.supplier) {
                    setErrors((prev) => {
                      const next = { ...prev };
                      delete next.supplier;
                      return next;
                    });
                  }
                }}
                error={!!errors.supplier}
                placeholder="Search vendor / supplier…"
              />
              {errors.supplier && (
                <p className="text-[11px] text-destructive">{errors.supplier}</p>
              )}
            </div>

            {/* Warehouse Destination */}
            <div className="space-y-1.5 sm:col-span-1 lg:col-span-1">
              <Label className="text-xs font-semibold">Destination Warehouse</Label>
              <Select value={warehouseId} onValueChange={setWarehouseId}>
                <SelectTrigger className="text-xs">
                  <SelectValue placeholder="Select warehouse…" />
                </SelectTrigger>
                <SelectContent>
                  {warehouses.map((w) => (
                    <SelectItem key={w.id} value={String(w.id)} className="text-xs">
                      {w.name} ({w.code})
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {/* Branch */}
            <div className="space-y-1.5 sm:col-span-1 lg:col-span-1">
              <Label className="text-xs font-semibold">Issuing Branch</Label>
              <Select value={branchId} onValueChange={setBranchId}>
                <SelectTrigger className="text-xs">
                  <SelectValue placeholder="Select branch…" />
                </SelectTrigger>
                <SelectContent>
                  {branches.map((b) => (
                    <SelectItem key={b.id} value={String(b.id)} className="text-xs">
                      {b.branchName} ({b.branchCode})
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {/* Order Date (AD & BS) */}
            <div className="space-y-1.5 sm:col-span-1 lg:col-span-1">
              <Label className="text-xs font-semibold">
                Order Date <span className="text-destructive">*</span>
              </Label>
              <div className="flex flex-col gap-1">
                <Input
                  type="date"
                  value={orderDate}
                  onChange={(e) => setOrderDate(e.target.value)}
                  className="text-xs"
                />
                <span className="text-[10px] text-muted-foreground font-mono">
                  BS: {safeAdToBsISO(orderDate) || "—"}
                </span>
              </div>
            </div>

            {/* Expected Delivery Date */}
            <div className="space-y-1.5 sm:col-span-1 lg:col-span-1">
              <Label className="text-xs font-semibold">Expected Delivery</Label>
              <Input
                type="date"
                value={expectedDeliveryDate}
                onChange={(e) => setExpectedDeliveryDate(e.target.value)}
                className="text-xs"
              />
            </div>

            {/* External Reference */}
            <div className="space-y-1.5 sm:col-span-2 lg:col-span-1">
              <Label className="text-xs font-semibold">Quotation / Ref No.</Label>
              <Input
                placeholder="e.g. QUOTE-2026-99"
                value={reference}
                onChange={(e) => setReference(e.target.value)}
                className="text-xs"
              />
            </div>
          </div>

          {/* Section 2: Line Items */}
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Package className="size-4 text-primary" />
                <h3 className="text-sm font-bold text-foreground">Order Line Items</h3>
                <Badge variant="secondary" className="text-[10px]">
                  {lines.length} {lines.length === 1 ? "Item" : "Items"}
                </Badge>
              </div>
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={handleAddLine}
                className="h-8 gap-1.5 text-xs text-primary hover:text-primary"
              >
                <Plus className="size-3.5" />
                <span>Add Item</span>
              </Button>
            </div>

            {/* Responsive Table / Card View */}
            <div className="overflow-hidden rounded-xl border border-border bg-card">
              <div className="overflow-x-auto">
                <table className="min-w-[760px] lg:min-w-full w-full text-left text-xs">
                  <thead className="border-b bg-muted/60 text-[11px] font-semibold uppercase text-muted-foreground">
                    <tr>
                      <th className="py-2.5 pl-3 pr-2 w-[35%] min-w-[260px]">Item *</th>
                      <th className="py-2.5 px-2 w-[13%] min-w-[90px]">Quantity *</th>
                      <th className="py-2.5 px-2 w-[15%] min-w-[110px]">Unit Price ({currency}) *</th>
                      <th className="py-2.5 px-2 w-[11%] min-w-[80px]">Disc %</th>
                      <th className="py-2.5 px-2 w-[10%] min-w-[80px]">VAT %</th>
                      <th className="py-2.5 px-2 w-[12%] min-w-[110px] text-right">Total</th>
                      <th className="py-2.5 pl-1 pr-3 w-[4%] min-w-[40px] text-center"></th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-border/60">
                    {lines.map((line, idx) => {
                      const comp = calculations.computedLines[idx];
                      const itemErr = errors[`line_${idx}_item`];
                      const qtyErr = errors[`line_${idx}_qty`];
                      const priceErr = errors[`line_${idx}_price`];

                      return (
                        <tr key={line.id} className="hover:bg-muted/20 transition-colors">
                          {/* Item Combobox */}
                          <td className="py-2 pl-3 pr-2 align-top">
                            <ItemCombobox
                              value={line.itemId ? String(line.itemId) : ""}
                              items={items}
                              onSelect={(item) => handleSelectItem(idx, item)}
                              error={!!itemErr}
                              placeholder="Search item SKU / name…"
                              className="text-xs"
                            />
                            {itemErr && (
                              <p className="mt-0.5 text-[10px] text-destructive">{itemErr}</p>
                            )}
                            {line.item && (
                              <div className="mt-1 flex items-center gap-1.5 text-[10px] text-muted-foreground">
                                <span>UOM: {line.item.uomCode || "PCS"}</span>
                                {line.item.taxabilityType && (
                                  <span>• Tax: {line.item.taxabilityType}</span>
                                )}
                              </div>
                            )}
                          </td>

                          {/* Quantity */}
                          <td className="py-2 px-2 align-top">
                            <Input
                              type="number"
                              min="0.001"
                              step="any"
                              value={line.quantity}
                              onChange={(e) =>
                                handleUpdateLine(
                                  idx,
                                  "quantity",
                                  e.target.value === "" ? "" : Number(e.target.value)
                                )
                              }
                              className="h-8 text-xs font-mono"
                            />
                            {qtyErr && (
                              <p className="mt-0.5 text-[10px] text-destructive">{qtyErr}</p>
                            )}
                          </td>

                          {/* Unit Price */}
                          <td className="py-2 px-2 align-top">
                            <Input
                              type="number"
                              min="0"
                              step="any"
                              value={line.unitPrice}
                              onChange={(e) =>
                                handleUpdateLine(
                                  idx,
                                  "unitPrice",
                                  e.target.value === "" ? "" : Number(e.target.value)
                                )
                              }
                              className="h-8 text-xs font-mono"
                              placeholder="0.00"
                            />
                            {priceErr && (
                              <p className="mt-0.5 text-[10px] text-destructive">{priceErr}</p>
                            )}
                          </td>

                          {/* Discount % */}
                          <td className="py-2 px-2 align-top">
                            <Input
                              type="number"
                              min="0"
                              max="100"
                              value={line.discountPercentage}
                              onChange={(e) =>
                                handleUpdateLine(
                                  idx,
                                  "discountPercentage",
                                  e.target.value === "" ? "" : Number(e.target.value)
                                )
                              }
                              className="h-8 text-xs font-mono"
                              placeholder="0"
                            />
                          </td>

                          {/* Tax Rate % */}
                          <td className="py-2 px-2 align-top">
                            <Input
                              type="number"
                              min="0"
                              max="100"
                              value={line.taxRate}
                              onChange={(e) =>
                                handleUpdateLine(
                                  idx,
                                  "taxRate",
                                  e.target.value === "" ? "" : Number(e.target.value)
                                )
                              }
                              className="h-8 text-xs font-mono"
                              placeholder="13"
                            />
                          </td>

                          {/* Computed Line Total */}
                          <td className="py-2 px-2 text-right align-middle font-mono text-xs font-semibold text-foreground">
                            <div>{formatMoney(comp ? comp.lineTotal : 0, currency)}</div>
                            {comp && comp.discountAmount > 0 ? (
                              <div className="text-[10px] font-normal text-muted-foreground">
                                Net: {formatMoney(comp.netAmount, currency)}
                              </div>
                            ) : null}
                          </td>

                          {/* Delete Action */}
                          <td className="py-2 pl-1 pr-3 text-center align-middle">
                            <Button
                              type="button"
                              variant="ghost"
                              size="icon"
                              onClick={() => handleRemoveLine(idx)}
                              className="size-7 text-muted-foreground hover:text-destructive hover:bg-destructive/10"
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
          </div>

          {/* Section 3: Notes & Financial Totals Summary */}
          <div className="grid grid-cols-1 gap-5 lg:grid-cols-12">
            <div className="space-y-1.5 lg:col-span-7 xl:col-span-8">
              <Label className="text-xs font-semibold">Notes / Terms & Conditions</Label>
              <Textarea
                placeholder="Commercial terms, payment agreements, or delivery instructions…"
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                rows={4}
                className="text-xs"
              />
            </div>

            <div className="rounded-xl border border-border/70 bg-muted/30 p-4 space-y-2.5 lg:col-span-5 xl:col-span-4">
              <div className="flex justify-between text-xs text-muted-foreground">
                <span>Subtotal (Gross):</span>
                <span className="font-mono text-foreground font-medium">
                  {formatMoney(calculations.subtotal, currency)}
                </span>
              </div>
              {calculations.totalDiscount > 0 && (
                <div className="flex justify-between text-xs text-emerald-600 dark:text-emerald-400">
                  <span>Discount:</span>
                  <span className="font-mono font-medium">
                    -{formatMoney(calculations.totalDiscount, currency)}
                  </span>
                </div>
              )}
              <div className="flex justify-between text-xs text-muted-foreground">
                <span>Tax / VAT:</span>
                <span className="font-mono text-foreground font-medium">
                  +{formatMoney(calculations.totalTax, currency)}
                </span>
              </div>
              <div className="border-t border-border/80 pt-2.5 flex justify-between text-sm font-bold text-foreground">
                <span>Grand Total:</span>
                <span className="font-mono text-primary text-base font-bold">
                  {formatMoney(calculations.grandTotal, currency)}
                </span>
              </div>
            </div>
          </div>
        </div>

        <DialogFooter className="mt-6 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end border-t pt-4">
          <Button
            type="button"
            variant="outline"
            onClick={() => onOpenChange(false)}
            disabled={isSaving}
            className="text-xs"
          >
            Cancel
          </Button>

          <Button
            type="button"
            variant="secondary"
            onClick={() => handleSave(false)}
            disabled={isSaving}
            className="text-xs gap-1.5"
          >
            {isCreating && !isSubmitting ? (
              <Loader2 className="size-3.5 animate-spin" />
            ) : (
              <Package className="size-3.5" />
            )}
            <span>Save as Draft</span>
          </Button>

          <Button
            type="button"
            onClick={() => handleSave(true)}
            disabled={isSaving}
            className="text-xs gap-1.5 bg-primary text-primary-foreground"
          >
            {isSubmitting ? (
              <Loader2 className="size-3.5 animate-spin" />
            ) : (
              <CheckCircle2 className="size-3.5" />
            )}
            <span>Save & Submit Order</span>
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
