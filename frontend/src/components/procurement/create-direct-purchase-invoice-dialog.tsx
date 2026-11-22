"use client";

import { useEffect, useMemo, useState } from "react";
import {
  AlertCircle,
  Building2,
  CheckCircle2,
  FileCheck2,
  Info,
  Loader2,
  Plus,
  Receipt,
  Trash2,
} from "lucide-react";
import { toast } from "sonner";
import {
  useCreatePurchaseInvoiceMutation,
  type PurchaseInvoice,
} from "@/services/purchase-invoice.service";
import {
  useGetSuppliersQuery,
  type SupplierListResponse,
} from "@/services/supplier.service";
import {
  useGetActiveItemsQuery,
  type ItemListResponse,
} from "@/services/item.service";
import { useGetBranchesQuery } from "@/services/branch.service";
import { getApiErrorMessage } from "@/services/error-handler";
import { formatMoney } from "@/lib/procurement-format";
import { SupplierCombobox } from "@/components/procurement/supplier-combobox";
import { SupplierBalanceBadge } from "@/components/procurement/supplier-balance-badge";
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

type TaxCategory = "TAXABLE" | "EXEMPT" | "ZERO_RATED" | "NON_TAXABLE";

interface InvoiceLineState {
  id: string; // client-side key
  itemId: number | null;
  item: ItemListResponse | null;
  description: string;
  quantity: number | "";
  unitPrice: number | "";
  discountPercentage: number | "";
  taxCategory: TaxCategory;
  taxRate: number | "";
}

const createInitialLine = (): InvoiceLineState => ({
  id: `line-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
  itemId: null,
  item: null,
  description: "",
  quantity: 1,
  unitPrice: "",
  discountPercentage: 0,
  taxCategory: "TAXABLE",
  taxRate: 13,
});

interface CreateDirectPurchaseInvoiceDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onCreated?: (invoice: PurchaseInvoice) => void;
}

export function CreateDirectPurchaseInvoiceDialog({
  open,
  onOpenChange,
  onCreated,
}: CreateDirectPurchaseInvoiceDialogProps) {
  // Master data queries
  const { data: suppliersPage, isLoading: suppliersLoading } = useGetSuppliersQuery(
    { page: 0, size: 250, sort: "name,asc" },
    { skip: !open }
  );
  const suppliers = suppliersPage?.content ?? [];

  const { data: itemsPage } = useGetActiveItemsQuery(
    { page: 0, size: 300, sort: "nameEn,asc" },
    { skip: !open }
  );
  const items = itemsPage?.content ?? [];

  const { data: branchesPage } = useGetBranchesQuery(undefined, {
    skip: !open,
  });
  const branches = useMemo(() => branchesPage?.content ?? [], [branchesPage]);

  // Form State
  const [supplierId, setSupplierId] = useState<number | null>(null);
  const [selectedSupplier, setSelectedSupplier] = useState<SupplierListResponse | null>(null);
  const [invoiceNumber, setInvoiceNumber] = useState("");
  const [invoiceDate, setInvoiceDate] = useState(() => todayISO());
  const [dueDate, setDueDate] = useState("");
  const [branchId, setBranchId] = useState<string>("");
  const [currency, setCurrency] = useState("NPR");
  const [notes, setNotes] = useState("");
  const [lines, setLines] = useState<InvoiceLineState[]>([createInitialLine()]);
  const [isTdsApplicable, setIsTdsApplicable] = useState(false);
  const [tdsRate, setTdsRate] = useState("10.0");

  const [createInvoice, { isLoading: isSubmitting }] = useCreatePurchaseInvoiceMutation();

  // Reset form when dialog opens
  useEffect(() => {
    if (open) {
      setSupplierId(null);
      setSelectedSupplier(null);
      setInvoiceNumber("");
      setInvoiceDate(todayISO());
      setDueDate("");
      setBranchId(branches[0]?.id ? String(branches[0].id) : "");
      setCurrency("NPR");
      setNotes("");
      setIsTdsApplicable(false);
      setTdsRate("10.0");
      setLines([createInitialLine()]);
    }
  }, [open, branches]);

  // Handle supplier selection
  const handleSelectSupplier = (s: SupplierListResponse | null) => {
    setSelectedSupplier(s);
    setSupplierId(s ? s.id : null);

    if (s?.vatRegistrationStatus === "PAN_ONLY") {
      setLines((prev) =>
        prev.map((l) => ({
          ...l,
          taxCategory: "EXEMPT",
          taxRate: 0,
        }))
      );
      toast.info("Supplier is PAN-only. Tax category defaulted to Exempt (0% VAT).");
    }
  };

  // Line item manipulation
  const handleAddLine = () => {
    setLines((prev) => [...prev, createInitialLine()]);
  };

  const handleRemoveLine = (id: string) => {
    if (lines.length <= 1) {
      toast.error("Invoice must contain at least one line item");
      return;
    }
    setLines((prev) => prev.filter((l) => l.id !== id));
  };

  const handleSelectItem = (lineId: string, item: ItemListResponse) => {
    setLines((prev) =>
      prev.map((l) => {
        if (l.id !== lineId) return l;

        const isPanOnly = selectedSupplier?.vatRegistrationStatus === "PAN_ONLY";
        const taxCategory: TaxCategory = isPanOnly
          ? "EXEMPT"
          : item.taxabilityType?.toUpperCase() === "EXEMPT"
          ? "EXEMPT"
          : item.taxabilityType?.toUpperCase() === "ZERO_RATED"
          ? "ZERO_RATED"
          : "TAXABLE";

        return {
          ...l,
          itemId: item.id,
          item,
          unitPrice: item.purchasePrice != null ? Number(item.purchasePrice) : l.unitPrice,
          taxCategory,
          taxRate: taxCategory === "TAXABLE" ? Number(item.vatRate ?? 13) : 0,
        };
      })
    );
  };

  const handleUpdateLine = (
    id: string,
    field: keyof InvoiceLineState,
    value: unknown
  ) => {
    setLines((prev) =>
      prev.map((l) => {
        if (l.id !== id) return l;
        const updated = { ...l, [field]: value };

        // When changing taxCategory to EXEMPT/ZERO_RATED, reset tax rate to 0
        if (field === "taxCategory") {
          if (selectedSupplier?.vatRegistrationStatus === "PAN_ONLY" && value === "TAXABLE") {
            toast.warning("Supplier is PAN-only. Under Nepal VAT Act, VAT cannot be charged on this invoice.");
            return l;
          }
          if (value === "EXEMPT" || value === "ZERO_RATED" || value === "NON_TAXABLE") {
            updated.taxRate = 0;
          } else if (value === "TAXABLE" && (!updated.taxRate || updated.taxRate === 0)) {
            updated.taxRate = 13;
          }
        }

        return updated;
      })
    );
  };

  // Real-time calculations
  const calculatedTotals = useMemo(() => {
    let subtotal = 0;
    let taxableAmount = 0;
    let exemptAmount = 0;
    let taxAmount = 0;

    for (const line of lines) {
      const qty = typeof line.quantity === "number" ? line.quantity : 0;
      const price = typeof line.unitPrice === "number" ? line.unitPrice : 0;
      const discount = typeof line.discountPercentage === "number" ? line.discountPercentage : 0;
      const taxRate = typeof line.taxRate === "number" ? line.taxRate : 0;

      const gross = qty * price;
      const net = gross * (1 - discount / 100);
      subtotal += net;

      if (line.taxCategory === "TAXABLE") {
        taxableAmount += net;
        taxAmount += net * (taxRate / 100);
      } else {
        exemptAmount += net;
      }
    }

    const total = subtotal + taxAmount;
    const rate = Number(tdsRate) || 0;
    const tdsAmount = isTdsApplicable ? subtotal * (rate / 100) : 0;
    const netPayable = total - tdsAmount;
    return { subtotal, taxableAmount, exemptAmount, taxAmount, total, tdsAmount, netPayable };
  }, [lines, isTdsApplicable, tdsRate]);

  // Form submission
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!supplierId) {
      toast.error("Please select a supplier");
      return;
    }
    if (!invoiceNumber.trim()) {
      toast.error("Please enter the supplier's invoice number");
      return;
    }
    if (!invoiceDate) {
      toast.error("Please enter the invoice date");
      return;
    }

    // Validate lines
    for (let i = 0; i < lines.length; i++) {
      const line = lines[i];
      if (!line || !line.itemId) {
        toast.error(`Line ${i + 1}: Please select a service or item`);
        return;
      }
      if (!line.quantity || Number(line.quantity) <= 0) {
        toast.error(`Line ${i + 1}: Quantity must be greater than zero`);
        return;
      }
      if (line.unitPrice === "" || Number(line.unitPrice) < 0) {
        toast.error(`Line ${i + 1}: Unit price cannot be negative`);
        return;
      }
    }

    const payload = {
      goodsReceiptId: null,
      invoiceType: "DIRECT" as const,
      supplierId,
      branchId: branchId ? Number(branchId) : null,
      currency,
      invoiceNumber: invoiceNumber.trim(),
      invoiceDate: invoiceDate || undefined,
      dueDate: dueDate || undefined,
      isTdsApplicable,
      tdsRate: isTdsApplicable ? Number(tdsRate) : 0,
      notes: notes.trim() || undefined,
      lines: lines.map((l) => ({
        itemId: l.itemId!,
        itemCode: l.item?.sku,
        itemName: l.item?.nameEn,
        uomCode: l.item?.uomCode || "UNIT",
        quantity: Number(l.quantity),
        unitPrice: Number(l.unitPrice),
        discountPercentage: Number(l.discountPercentage) || 0,
        taxRate: l.taxCategory === "TAXABLE" ? Number(l.taxRate) || 13 : 0,
        taxCategory: l.taxCategory,
        description: l.description.trim() || undefined,
      })),
    };

    try {
      const created = await createInvoice(payload).unwrap();
      toast.success(`Direct Invoice ${created.invoiceNumber} recorded`, {
        description: isTdsApplicable
          ? `Net Payable: ${formatMoney(created.netPayableAmount ?? created.totalAmount, created.currency)} (TDS Withheld: ${formatMoney(created.tdsAmount ?? 0, created.currency)})`
          : `Total: ${formatMoney(created.totalAmount, created.currency)} (Input VAT: ${formatMoney(created.taxAmount, created.currency)})`,
      });
      onOpenChange(false);
      onCreated?.(created);
    } catch (err) {
      toast.error(getApiErrorMessage(err) || "Failed to record direct invoice");
    }
  };

  const bsDate = safeAdToBsISO(invoiceDate);

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[94vh] w-[96vw] max-w-[96vw] sm:max-w-4xl md:max-w-5xl lg:max-w-6xl xl:max-w-7xl 2xl:max-w-[1400px] overflow-y-auto p-4 sm:p-6 lg:p-7 xl:p-8">
        <DialogHeader className="border-b border-border pb-4">
          <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex items-center gap-3">
              <div className="rounded-xl bg-primary/10 p-2.5 text-primary ring-1 ring-primary/20">
                <Receipt className="size-6" />
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <DialogTitle className="text-xl font-bold tracking-tight text-foreground sm:text-2xl">
                    Record Direct Service / Expense Invoice
                  </DialogTitle>
                  <Badge variant="secondary" className="hidden sm:inline-flex text-[11px] font-medium">
                    Non-GRN Direct Expense
                  </Badge>
                </div>
                <DialogDescription className="text-xs text-muted-foreground sm:text-sm">
                  Record supplier invoices for professional services, SaaS software, utilities, rent, and direct business expenses.
                </DialogDescription>
              </div>
            </div>
            {currency && (
              <Badge variant="outline" className="self-start sm:self-center font-mono text-xs">
                Currency: {currency}
              </Badge>
            )}
          </div>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-6 pt-2">
          {/* Section 1: Supplier & Invoice Header Details */}
          <div className="rounded-xl border border-border/80 bg-card p-4 sm:p-5 shadow-xs space-y-4">
            <div className="flex items-center gap-2 border-b border-border/60 pb-2.5">
              <Building2 className="size-4 text-primary" />
              <h3 className="text-sm font-semibold text-foreground">
                1. Supplier & Invoice Information
              </h3>
            </div>

            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
              {/* Supplier Selection */}
              <div className="space-y-2 sm:col-span-2">
                <div className="flex items-center justify-between">
                  <Label className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                    Supplier <span className="text-destructive">*</span>
                  </Label>
                  {selectedSupplier && (
                    <span className="text-[11px] text-muted-foreground">
                      Code: <strong className="font-mono">{selectedSupplier.code}</strong>
                    </span>
                  )}
                </div>
                <SupplierCombobox
                  value={supplierId ? String(supplierId) : undefined}
                  suppliers={suppliers}
                  disabled={suppliersLoading || isSubmitting}
                  onSelect={handleSelectSupplier}
                  placeholder="Search and select supplier…"
                />
                {selectedSupplier && (
                  <div className="flex flex-wrap items-center gap-2 pt-1 text-xs text-muted-foreground">
                    <span className="inline-flex items-center gap-1 rounded-md bg-muted px-2 py-0.5 font-mono text-[11px]">
                      PAN: {selectedSupplier.taxId ?? "—"}
                    </span>
                    {selectedSupplier.vatRegistrationStatus === "VAT_REGISTERED" ? (
                      <Badge variant="secondary" className="bg-emerald-50 text-emerald-700 dark:bg-emerald-950 dark:text-emerald-300 text-[10px] font-medium">
                        VAT Registered (13%)
                      </Badge>
                    ) : (
                      <Badge variant="outline" className="text-[10px]">
                        PAN Only
                      </Badge>
                    )}
                    {selectedSupplier.paymentTermsDays && (
                      <span className="text-[11px] text-muted-foreground">
                        Terms: {selectedSupplier.paymentTermsDays} days
                      </span>
                    )}
                  </div>
                )}
                <SupplierBalanceBadge supplierId={supplierId} className="mt-2" />
                {selectedSupplier?.vatRegistrationStatus === "PAN_ONLY" && (
                  <div className="mt-2 flex items-center gap-2 rounded-lg border border-amber-200 bg-amber-50/70 p-2 text-xs text-amber-800 dark:border-amber-800 dark:bg-amber-950/30 dark:text-amber-200">
                    <AlertCircle className="size-4 shrink-0 text-amber-600" />
                    <span>
                      <strong>PAN-Only Supplier:</strong> Under Nepal VAT Act 2052, VAT (13%) cannot be charged or claimed. Lines are restricted to Non-VAT (0%).
                    </span>
                  </div>
                )}
              </div>

              {/* Supplier Invoice Number */}
              <div className="space-y-2">
                <Label className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                  Supplier Invoice No. <span className="text-destructive">*</span>
                </Label>
                <Input
                  required
                  placeholder="e.g. INV-2026-9041"
                  value={invoiceNumber}
                  onChange={(e) => setInvoiceNumber(e.target.value)}
                  disabled={isSubmitting}
                  className="font-mono text-sm"
                />
                <p className="text-[11px] text-muted-foreground">
                  Original invoice # issued by vendor
                </p>
              </div>

              {/* Branch */}
              <div className="space-y-2">
                <Label className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                  Branch
                </Label>
                <Select
                  value={branchId}
                  onValueChange={setBranchId}
                  disabled={isSubmitting || branches.length === 0}
                >
                  <SelectTrigger className="text-xs">
                    <SelectValue placeholder="Select branch" />
                  </SelectTrigger>
                  <SelectContent>
                    {branches.map((b) => (
                      <SelectItem key={b.id} value={String(b.id)}>
                        {b.branchName} ({b.branchCode})
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <p className="text-[11px] text-muted-foreground">
                  Attributed operating location
                </p>
              </div>

              {/* Invoice Date */}
              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <Label className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                    Invoice Date <span className="text-destructive">*</span>
                  </Label>
                  {bsDate && (
                    <span className="rounded-xs bg-muted px-1.5 py-0.5 text-[10px] font-medium text-muted-foreground">
                      BS: {bsDate}
                    </span>
                  )}
                </div>
                <Input
                  type="date"
                  required
                  value={invoiceDate}
                  onChange={(e) => setInvoiceDate(e.target.value)}
                  disabled={isSubmitting}
                  className="text-xs"
                />
              </div>

              {/* Due Date */}
              <div className="space-y-2">
                <Label className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                  Payment Due Date
                </Label>
                <Input
                  type="date"
                  value={dueDate}
                  onChange={(e) => setDueDate(e.target.value)}
                  disabled={isSubmitting}
                  className="text-xs"
                />
              </div>

              {/* Currency */}
              <div className="space-y-2">
                <Label className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                  Currency
                </Label>
                <Input
                  value={currency}
                  onChange={(e) => setCurrency(e.target.value.toUpperCase())}
                  disabled={isSubmitting}
                  maxLength={3}
                  className="font-mono text-xs uppercase"
                />
              </div>
            </div>
          </div>

          {/* Section 2: Service & Expense Line Items */}
          <div className="rounded-xl border border-border/80 bg-card p-4 sm:p-5 shadow-xs space-y-4">
            <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between border-b border-border/60 pb-3">
              <div>
                <div className="flex items-center gap-2">
                  <FileCheck2 className="size-4 text-primary" />
                  <h3 className="text-sm font-semibold text-foreground">
                    2. Service & Expense Line Items
                  </h3>
                  <Badge variant="outline" className="text-[10px]">
                    {lines.length} {lines.length === 1 ? "Line" : "Lines"}
                  </Badge>
                </div>
                <p className="text-xs text-muted-foreground mt-0.5">
                  Select non-physical or service items from your catalog, or specify expense purpose and tax categories.
                </p>
              </div>

              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={handleAddLine}
                disabled={isSubmitting}
                className="h-8 gap-1.5 self-start sm:self-auto font-medium"
              >
                <Plus className="size-3.5" />
                Add Service Line
              </Button>
            </div>

            <div className="overflow-x-auto rounded-lg border border-border/70">
              <table className="w-full text-left text-xs">
                <thead>
                  <tr className="border-b border-border bg-muted/40 text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">
                    <th className="py-2.5 pl-3 pr-2 w-10 text-center">#</th>
                    <th className="py-2.5 px-2 min-w-[240px]">Service / Expense Item</th>
                    <th className="py-2.5 px-2 min-w-[180px]">Line Description</th>
                    <th className="py-2.5 px-2 w-24">Qty</th>
                    <th className="py-2.5 px-2 w-20">UOM</th>
                    <th className="py-2.5 px-2 w-32">Unit Price</th>
                    <th className="py-2.5 px-2 w-20">Disc %</th>
                    <th className="py-2.5 px-2 w-36">Tax Category</th>
                    <th className="py-2.5 px-2 w-20">VAT %</th>
                    <th className="py-2.5 px-2 w-32 text-right">Net Amount</th>
                    <th className="py-2.5 pr-3 pl-2 w-10 text-center"></th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border">
                  {lines.map((line, idx) => {
                    const qty = typeof line.quantity === "number" ? line.quantity : 0;
                    const price = typeof line.unitPrice === "number" ? line.unitPrice : 0;
                    const disc = typeof line.discountPercentage === "number" ? line.discountPercentage : 0;
                    const lineNet = qty * price * (1 - disc / 100);

                    return (
                      <tr key={line.id} className="align-middle transition-colors hover:bg-muted/30">
                        <td className="py-2.5 pl-3 pr-2 text-center font-medium text-muted-foreground">
                          {idx + 1}
                        </td>
                        <td className="py-2.5 px-2">
                          <ItemCombobox
                            value={line.itemId ? String(line.itemId) : undefined}
                            items={items}
                            disabled={isSubmitting}
                            onSelect={(item) => handleSelectItem(line.id, item)}
                            placeholder="Select service/item…"
                          />
                        </td>
                        <td className="py-2.5 px-2">
                          <Input
                            placeholder="e.g. October Retainer…"
                            value={line.description}
                            onChange={(e) => handleUpdateLine(line.id, "description", e.target.value)}
                            disabled={isSubmitting}
                            className="h-8 text-xs"
                          />
                        </td>
                        <td className="py-2.5 px-2">
                          <Input
                            type="number"
                            min="0.0001"
                            step="any"
                            value={line.quantity}
                            onChange={(e) =>
                              handleUpdateLine(
                                line.id,
                                "quantity",
                                e.target.value === "" ? "" : Number(e.target.value)
                              )
                            }
                            disabled={isSubmitting}
                            className="h-8 text-xs font-mono"
                          />
                        </td>
                        <td className="py-2.5 px-2 text-muted-foreground font-mono text-[11px]">
                          <span className="rounded-xs bg-muted px-1.5 py-0.5">
                            {line.item?.uomCode ?? "UNIT"}
                          </span>
                        </td>
                        <td className="py-2.5 px-2">
                          <Input
                            type="number"
                            min="0"
                            step="any"
                            value={line.unitPrice}
                            onChange={(e) =>
                              handleUpdateLine(
                                line.id,
                                "unitPrice",
                                e.target.value === "" ? "" : Number(e.target.value)
                              )
                            }
                            disabled={isSubmitting}
                            className="h-8 text-xs font-mono"
                          />
                        </td>
                        <td className="py-2.5 px-2">
                          <Input
                            type="number"
                            min="0"
                            max="100"
                            step="0.01"
                            value={line.discountPercentage}
                            onChange={(e) =>
                              handleUpdateLine(
                                line.id,
                                "discountPercentage",
                                e.target.value === "" ? 0 : Number(e.target.value)
                              )
                            }
                            disabled={isSubmitting}
                            className="h-8 text-xs font-mono"
                          />
                        </td>
                        <td className="py-2.5 px-2">
                          <Select
                            value={line.taxCategory}
                            onValueChange={(val: TaxCategory) => handleUpdateLine(line.id, "taxCategory", val)}
                            disabled={isSubmitting || selectedSupplier?.vatRegistrationStatus === "PAN_ONLY"}
                          >
                            <SelectTrigger className="h-8 text-xs">
                              <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                              <SelectItem value="TAXABLE">Taxable (13%)</SelectItem>
                              <SelectItem value="EXEMPT">Exempt (0%)</SelectItem>
                              <SelectItem value="ZERO_RATED">Zero-Rated (0%)</SelectItem>
                            </SelectContent>
                          </Select>
                        </td>
                        <td className="py-2.5 px-2">
                          <Input
                            type="number"
                            disabled={line.taxCategory !== "TAXABLE" || isSubmitting}
                            value={line.taxRate}
                            onChange={(e) =>
                              handleUpdateLine(
                                line.id,
                                "taxRate",
                                e.target.value === "" ? 0 : Number(e.target.value)
                              )
                            }
                            className="h-8 text-xs font-mono"
                          />
                        </td>
                        <td className="py-2.5 px-2 text-right font-semibold font-mono text-foreground">
                          {formatMoney(lineNet, currency)}
                        </td>
                        <td className="py-2.5 pr-3 pl-2 text-center">
                          <Button
                            type="button"
                            variant="ghost"
                            size="icon"
                            className="size-7 text-muted-foreground hover:text-destructive hover:bg-destructive/10"
                            onClick={() => handleRemoveLine(line.id)}
                            disabled={lines.length <= 1 || isSubmitting}
                            title="Remove line"
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

            <div className="flex justify-start">
              <Button
                type="button"
                variant="ghost"
                size="sm"
                onClick={handleAddLine}
                disabled={isSubmitting}
                className="h-8 gap-1.5 text-xs text-primary hover:text-primary/90"
              >
                <Plus className="size-3.5" />
                Add Another Line Item
              </Button>
            </div>
          </div>

          {/* Section 3: Summary Breakdown & Compliance Note */}
          <div className="grid gap-4 lg:grid-cols-12">
            <div className="lg:col-span-7 space-y-3">
              <div className="space-y-2">
                <Label className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                  Notes & Remarks
                </Label>
                <Textarea
                  placeholder="Record internal remarks, project reference, cost center, or approval tracking notes…"
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  disabled={isSubmitting}
                  className="h-24 text-xs"
                />
              </div>

              {/* Compliance Info Banner */}
              <div className="flex items-start gap-2.5 rounded-xl border border-primary/20 bg-primary/5 p-3.5 text-xs">
                <Info className="mt-0.5 size-4 shrink-0 text-primary" />
                <div className="space-y-0.5 text-muted-foreground">
                  <span className="font-semibold text-foreground">
                    Nepal IRD Compliance & VAT Purchase Book (Annex-10)
                  </span>
                  <p>
                    This direct bill will automatically be logged into your statutory <strong>VAT Purchase Book (Annex-10)</strong> and Accounts Payable ledger, enabling your company to claim 100% of eligible Input VAT credits without requiring a warehouse Goods Receipt.
                  </p>
                </div>
              </div>
            </div>

            <div className="lg:col-span-5 space-y-4">
              {/* Nepal TDS Configuration */}
              <div className="rounded-xl border border-border/80 bg-card p-4 text-xs space-y-3">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <input
                      type="checkbox"
                      id="direct-tds-checkbox"
                      checked={isTdsApplicable}
                      onChange={(e) => setIsTdsApplicable(e.target.checked)}
                      className="size-4 rounded border-gray-300 text-primary focus:ring-primary"
                    />
                    <Label htmlFor="direct-tds-checkbox" className="font-semibold text-foreground cursor-pointer">
                      Deduct Nepal TDS (Withholding Tax)
                    </Label>
                  </div>
                </div>

                {isTdsApplicable && (
                  <div className="space-y-1.5 pt-1">
                    <Label className="text-[11px] text-muted-foreground uppercase font-semibold">
                      TDS Withholding Category
                    </Label>
                    <Select value={tdsRate} onValueChange={setTdsRate}>
                      <SelectTrigger className="h-8 text-xs">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="1.5">1.5% — Goods & Works Contracts (Sec 89)</SelectItem>
                        <SelectItem value="10.0">10.0% — Professional & Consultancy (Sec 88)</SelectItem>
                        <SelectItem value="15.0">15.0% — House Rent / Facility Lease (Sec 88)</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                )}
              </div>

              {/* Financial Summary */}
              <div className="rounded-xl border border-border/80 bg-muted/40 p-4 sm:p-5 text-xs space-y-2.5">
                <h4 className="text-xs font-semibold uppercase tracking-wider text-muted-foreground border-b border-border/60 pb-2">
                  Invoice Financial Summary
                </h4>

                <div className="flex justify-between text-muted-foreground pt-1">
                  <span>Subtotal (Net Amount)</span>
                  <span className="font-mono font-medium text-foreground">
                    {formatMoney(calculatedTotals.subtotal, currency)}
                  </span>
                </div>

                <div className="flex justify-between text-muted-foreground">
                  <span>Taxable Base (VAT Applicable)</span>
                  <span className="font-mono font-medium text-foreground">
                    {formatMoney(calculatedTotals.taxableAmount, currency)}
                  </span>
                </div>

                <div className="flex justify-between text-muted-foreground">
                  <span className="flex items-center gap-1">
                    Input VAT (13% Creditable Tax)
                  </span>
                  <span className="font-mono font-semibold text-emerald-600 dark:text-emerald-400">
                    +{formatMoney(calculatedTotals.taxAmount, currency)}
                  </span>
                </div>

                {calculatedTotals.exemptAmount > 0 && (
                  <div className="flex justify-between text-muted-foreground">
                    <span>Exempt Purchases</span>
                    <span className="font-mono font-medium text-foreground">
                      {formatMoney(calculatedTotals.exemptAmount, currency)}
                    </span>
                  </div>
                )}

                <div className="flex justify-between text-muted-foreground font-semibold">
                  <span>Gross Invoice Total</span>
                  <span className="font-mono text-foreground">
                    {formatMoney(calculatedTotals.total, currency)}
                  </span>
                </div>

                {isTdsApplicable && (
                  <div className="flex justify-between text-amber-700 dark:text-amber-400 font-medium">
                    <span>Less TDS Withheld ({tdsRate}% on Subtotal)</span>
                    <span className="font-mono">
                      -{formatMoney(calculatedTotals.tdsAmount, currency)}
                    </span>
                  </div>
                )}

                <div className="border-t border-border/80 pt-3 mt-2 flex justify-between items-baseline">
                  <span className="text-sm font-bold text-foreground">Net Payable to Supplier</span>
                  <span className="text-lg font-bold font-mono text-primary">
                    {formatMoney(calculatedTotals.netPayable, currency)}
                  </span>
                </div>
              </div>
            </div>
          </div>

          <DialogFooter className="gap-2 border-t border-border pt-4">
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
              disabled={isSubmitting}
            >
              Cancel
            </Button>
            <Button type="submit" disabled={isSubmitting} className="gap-2 min-w-[180px]">
              {isSubmitting ? (
                <>
                  <Loader2 className="size-4 animate-spin" />
                  Recording Invoice…
                </>
              ) : (
                <>
                  <CheckCircle2 className="size-4" />
                  Record Direct Invoice
                </>
              )}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
