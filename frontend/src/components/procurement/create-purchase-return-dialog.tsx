"use client";

import { useEffect, useMemo, useState } from "react";
import {
  AlertCircle,
  AlertTriangle,
  CheckCircle2,
  CheckSquare,
  FileText,
  Loader2,
  Package,
  RefreshCw,
  RotateCcw,
  Square,
  Sparkles,
} from "lucide-react";
import { toast } from "sonner";
import {
  useCreatePurchaseReturnMutation,
  useGetReturnableLinesQuery,
  usePostPurchaseReturnMutation,
  type CreatePurchaseReturnLineRequest,
  type PurchaseReturn,
} from "@/services/purchase-return.service";
import {
  useGetPurchaseInvoicesQuery,
} from "@/services/purchase-invoice.service";
import { getApiErrorMessage } from "@/services/error-handler";
import { formatMoney, formatQuantity } from "@/lib/procurement-format";
import { todayISO, safeAdToBsISO } from "@/lib/nepali-date";

import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
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
import { PurchaseInvoiceCombobox } from "@/components/procurement/purchase-invoice-combobox";

const RETURN_REASONS = [
  "Defective / Damaged Goods",
  "Rejected at Quality Control (QC)",
  "Wrong Item Delivered",
  "Price or Quantity Overbilled",
  "Expired / Shelf-life Rejection",
  "Commercial Terms Return",
  "Other Reason",
] as const;

interface LineDraftState {
  included: boolean;
  purchaseInvoiceLineId: number;
  itemId: number;
  itemCode: string;
  itemName: string;
  uomCode: string;
  invoicedQuantity: number;
  alreadyReturnedQuantity: number;
  returnableQuantity: number;
  returnQuantity: number | "";
  unitPrice: number;
  taxCategory: string;
  taxRate: number;
  remarks: string;
}

interface CreatePurchaseReturnDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  initialInvoiceId?: number | null;
  onCreated?: (purchaseReturn: PurchaseReturn) => void;
}

export function CreatePurchaseReturnDialog({
  open,
  onOpenChange,
  initialInvoiceId,
  onCreated,
}: CreatePurchaseReturnDialogProps) {
  const [selectedInvoiceId, setSelectedInvoiceId] = useState<number | null>(
    initialInvoiceId ?? null
  );
  const [returnDate, setReturnDate] = useState<string>(todayISO());
  const [reasonCategory, setReasonCategory] = useState<string>(RETURN_REASONS[0]);
  const [customReason, setCustomReason] = useState<string>("");
  const [notes, setNotes] = useState<string>("");
  const [lines, setLines] = useState<LineDraftState[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Load available invoices
  const { data: invoicesData, isLoading: isLoadingInvoices } =
    useGetPurchaseInvoicesQuery(
      { page: 0, size: 200 },
      { skip: !open }
    );

  const rawInvoices = invoicesData?.content;
  const invoices = useMemo(
    () => rawInvoices?.filter((i) => i.status !== "CANCELLED") ?? [],
    [rawInvoices]
  );

  const selectedInvoice = useMemo(
    () => invoices.find((i) => i.id === selectedInvoiceId) ?? null,
    [invoices, selectedInvoiceId]
  );

  // Sync initial invoice ID
  useEffect(() => {
    if (initialInvoiceId) {
      setSelectedInvoiceId(initialInvoiceId);
    }
  }, [initialInvoiceId]);

  // Load returnable lines for selected invoice
  const {
    data: returnableLinesData,
    isLoading: isLoadingLines,
    isError: isLinesError,
    error: linesError,
    refetch: refetchLines,
  } = useGetReturnableLinesQuery(selectedInvoiceId ?? 0, {
    skip: !open || !selectedInvoiceId,
  });

  // Populate line draft states whenever new invoice lines are fetched
  useEffect(() => {
    if (returnableLinesData && returnableLinesData.length > 0) {
      setLines(
        returnableLinesData.map((line) => {
          const returnable = line.returnableQuantity ?? 0;
          return {
            included: returnable > 0,
            purchaseInvoiceLineId: line.purchaseInvoiceLineId,
            itemId: line.itemId,
            itemCode: line.itemCode,
            itemName: line.itemName,
            uomCode: line.uomCode,
            invoicedQuantity: line.invoicedQuantity,
            alreadyReturnedQuantity: line.alreadyReturnedQuantity,
            returnableQuantity: returnable,
            returnQuantity: returnable > 0 ? returnable : 0,
            unitPrice: line.unitPrice,
            taxCategory: line.taxCategory,
            taxRate: line.taxRate,
            remarks: "",
          };
        })
      );
    } else {
      setLines([]);
    }
  }, [returnableLinesData]);

  // API mutations
  const [createReturn, { isLoading: isCreating }] =
    useCreatePurchaseReturnMutation();
  const [postReturn, { isLoading: isPosting }] =
    usePostPurchaseReturnMutation();

  // Reset form on close
  useEffect(() => {
    if (!open) {
      if (!initialInvoiceId) setSelectedInvoiceId(null);
      setReturnDate(todayISO());
      setReasonCategory(RETURN_REASONS[0]);
      setCustomReason("");
      setNotes("");
      setLines([]);
      setIsSubmitting(false);
    }
  }, [open, initialInvoiceId]);

  // Financial calculations
  const totals = useMemo(() => {
    let subtotal = 0;
    let tax = 0;

    for (const l of lines) {
      if (!l.included) continue;
      const qty = typeof l.returnQuantity === "number" ? l.returnQuantity : 0;
      const lineSubtotal = qty * l.unitPrice;
      const lineTax = (lineSubtotal * l.taxRate) / 100;
      subtotal += lineSubtotal;
      tax += lineTax;
    }

    return {
      subtotal,
      tax,
      grandTotal: subtotal + tax,
    };
  }, [lines]);

  const eligibleLinesCount = useMemo(
    () => lines.filter((l) => l.returnableQuantity > 0).length,
    [lines]
  );

  const activeSelectedLines = useMemo(
    () =>
      lines.filter(
        (l) =>
          l.included &&
          typeof l.returnQuantity === "number" &&
          l.returnQuantity > 0
      ),
    [lines]
  );

  const handleLineQtyChange = (idx: number, val: string) => {
    setLines((prev) => {
      const targetLine = prev[idx];
      if (!targetLine) return prev;
      const parsed = Number(val);
      const returnable = targetLine.returnableQuantity ?? 0;
      const newQty: number | "" =
        val === "" ? "" : Math.max(0, Math.min(returnable, isNaN(parsed) ? 0 : parsed));
      return prev.map((l, i) => (i === idx ? { ...l, returnQuantity: newQty } : l));
    });
  };

  const handleSetMaxQty = (idx: number) => {
    setLines((prev) =>
      prev.map((l, i) =>
        i === idx ? { ...l, included: true, returnQuantity: l.returnableQuantity } : l
      )
    );
  };

  const handleToggleInclude = (idx: number, included: boolean) => {
    setLines((prev) =>
      prev.map((l, i) => {
        if (i !== idx) return l;
        const newQty =
          included && (l.returnQuantity === 0 || l.returnQuantity === "")
            ? l.returnableQuantity
            : l.returnQuantity;
        return { ...l, included, returnQuantity: newQty };
      })
    );
  };

  const handleSelectAll = (select: boolean) => {
    setLines((prev) =>
      prev.map((l) => ({
        ...l,
        included: select && l.returnableQuantity > 0,
        returnQuantity:
          select && l.returnableQuantity > 0
            ? l.returnQuantity || l.returnableQuantity
            : l.returnQuantity,
      }))
    );
  };

  const handleReturnAllFull = () => {
    setLines((prev) =>
      prev.map((l) => ({
        ...l,
        included: l.returnableQuantity > 0,
        returnQuantity: l.returnableQuantity > 0 ? l.returnableQuantity : 0,
      }))
    );
    toast.success("Applied full eligible quantities for all items.");
  };

  const handleLineRemarksChange = (idx: number, remarks: string) => {
    setLines((prev) =>
      prev.map((l, i) => (i === idx ? { ...l, remarks } : l))
    );
  };

  const handleSubmit = async (autoPost: boolean) => {
    if (!selectedInvoiceId) {
      toast.error("Please select a purchase invoice to return against.");
      return;
    }

    if (activeSelectedLines.length === 0) {
      toast.error(
        "Please select at least one line item and enter a return quantity greater than zero."
      );
      return;
    }

    const finalReason =
      reasonCategory === "Other Reason" && customReason.trim()
        ? customReason.trim()
        : reasonCategory;

    const requestPayload = {
      purchaseInvoiceId: selectedInvoiceId,
      returnDate,
      reason: finalReason,
      notes: notes.trim() || undefined,
      lines: activeSelectedLines.map((l) => ({
        purchaseInvoiceLineId: l.purchaseInvoiceLineId,
        itemId: l.itemId,
        returnQuantity: Number(l.returnQuantity),
        unitPrice: l.unitPrice,
        taxCategory: l.taxCategory,
        taxRate: l.taxRate,
        remarks: l.remarks.trim() || undefined,
      })) as CreatePurchaseReturnLineRequest[],
    };

    setIsSubmitting(true);
    try {
      const created = await createReturn(requestPayload).unwrap();
      toast.success(
        `Draft Debit Note ${created.debitNoteNumber} created successfully.`
      );

      if (autoPost) {
        try {
          const posted = await postReturn(created.id).unwrap();
          toast.success(
            `Debit Note ${posted.debitNoteNumber} posted. Stock dispatched & Accounts Payable adjusted.`
          );
          onCreated?.(posted);
        } catch (postErr) {
          toast.error(
            `Created draft ${created.debitNoteNumber}, but failed to post: ${getApiErrorMessage(postErr)}`
          );
          onCreated?.(created);
        }
      } else {
        onCreated?.(created);
      }

      onOpenChange(false);
    } catch (err) {
      toast.error(getApiErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  };

  const isSaving = isCreating || isPosting || isSubmitting;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[94vh] w-[96vw] max-w-[96vw] sm:max-w-4xl md:max-w-5xl lg:max-w-6xl xl:max-w-7xl 2xl:max-w-[1400px] overflow-y-auto p-4 sm:p-6 lg:p-7 xl:p-8">
        <DialogHeader className="border-b pb-3">
          <div className="flex items-center gap-2.5">
            <div className="flex size-9 items-center justify-center rounded-lg bg-destructive/10 text-destructive">
              <RotateCcw className="size-5" />
            </div>
            <div>
              <DialogTitle className="text-lg font-bold sm:text-xl">
                Create Purchase Return / Debit Note
              </DialogTitle>
              <DialogDescription className="text-xs text-muted-foreground">
                Issue a return to vendor (RTV) to reverse accounts payable balance and adjust statutory input VAT.
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        <div className="space-y-6 pt-2">
          {/* Section 1: Header / Invoice Selection */}
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {/* Searchable Invoice Selector */}
            <div className="space-y-1.5 sm:col-span-2 lg:col-span-1">
              <Label className="text-xs font-semibold">
                Original Purchase Invoice <span className="text-destructive">*</span>
              </Label>
              <PurchaseInvoiceCombobox
                value={selectedInvoiceId}
                invoices={invoices}
                isLoading={isLoadingInvoices}
                disabled={isSaving || !!initialInvoiceId}
                placeholder="Search invoice number, supplier or PAN…"
                onSelect={(inv) => setSelectedInvoiceId(inv ? inv.id : null)}
              />
            </div>

            {/* Return Date (AD & BS) */}
            <div className="space-y-1.5">
              <Label className="text-xs font-semibold">
                Return Date <span className="text-destructive">*</span>
              </Label>
              <div className="flex flex-col gap-1">
                <Input
                  type="date"
                  value={returnDate}
                  onChange={(e) => setReturnDate(e.target.value)}
                  className="text-xs"
                />
                <span className="text-[10px] text-muted-foreground font-mono">
                  BS: {safeAdToBsISO(returnDate) || "—"}
                </span>
              </div>
            </div>

            {/* Return Reason Dropdown */}
            <div className="space-y-1.5">
              <Label className="text-xs font-semibold">
                Statutory Reason <span className="text-destructive">*</span>
              </Label>
              <Select
                value={reasonCategory}
                onValueChange={setReasonCategory}
              >
                <SelectTrigger className="text-xs">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {RETURN_REASONS.map((reason) => (
                    <SelectItem key={reason} value={reason} className="text-xs">
                      {reason}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {/* Custom Reason input if "Other Reason" selected */}
            {reasonCategory === "Other Reason" && (
              <div className="space-y-1.5 sm:col-span-2 lg:col-span-3">
                <Label className="text-xs font-semibold">Specify Reason Details *</Label>
                <Input
                  placeholder="State the commercial or quality reason for this return…"
                  value={customReason}
                  onChange={(e) => setCustomReason(e.target.value)}
                  className="text-xs"
                />
              </div>
            )}
          </div>

          {/* Selected Invoice Details Snapshot Banner */}
          {selectedInvoice && (
            <div className="rounded-xl border border-border/80 bg-muted/20 p-3 sm:p-4">
              <div className="grid grid-cols-2 gap-3 sm:grid-cols-4 lg:grid-cols-6 text-xs">
                <div>
                  <span className="text-[10px] uppercase font-semibold text-muted-foreground">Supplier</span>
                  <p className="font-semibold text-foreground truncate">{selectedInvoice.supplierName}</p>
                  <p className="text-[10px] text-muted-foreground font-mono">PAN: {selectedInvoice.supplierTaxId || "—"}</p>
                </div>
                <div>
                  <span className="text-[10px] uppercase font-semibold text-muted-foreground">Invoice No & Date</span>
                  <p className="font-semibold font-mono text-foreground">{selectedInvoice.invoiceNumber}</p>
                  <p className="text-[10px] text-muted-foreground">
                    {selectedInvoice.invoiceDate} {safeAdToBsISO(selectedInvoice.invoiceDate) ? `(${safeAdToBsISO(selectedInvoice.invoiceDate)})` : ""}
                  </p>
                </div>
                <div>
                  <span className="text-[10px] uppercase font-semibold text-muted-foreground">Original GRN</span>
                  <p className="font-semibold font-mono text-foreground">{selectedInvoice.grnNumber || "—"}</p>
                  <p className="text-[10px] text-muted-foreground">PO: {selectedInvoice.poNumber || "—"}</p>
                </div>
                <div>
                  <span className="text-[10px] uppercase font-semibold text-muted-foreground">Invoiced Total</span>
                  <p className="font-semibold font-mono text-foreground">
                    {formatMoney(selectedInvoice.totalAmount, selectedInvoice.currency)}
                  </p>
                </div>
                <div>
                  <span className="text-[10px] uppercase font-semibold text-muted-foreground">Payable Balance</span>
                  <p className="font-semibold font-mono text-primary font-bold">
                    {formatMoney(selectedInvoice.balanceAmount, selectedInvoice.currency)}
                  </p>
                </div>
                <div>
                  <span className="text-[10px] uppercase font-semibold text-muted-foreground">Status</span>
                  <div className="mt-0.5">
                    <Badge variant="outline" className="text-[11px] font-mono">
                      {selectedInvoice.status}
                    </Badge>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* Section 2: Return Items Table */}
          <div className="space-y-3">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
              <div className="flex items-center gap-2 flex-wrap">
                <Package className="size-4 text-destructive" />
                <h3 className="text-sm font-bold text-foreground">Items to Return</h3>
                {lines.length > 0 && (
                  <Badge variant="secondary" className="text-[10px]">
                    {activeSelectedLines.length} of {eligibleLinesCount} Eligible Selected
                  </Badge>
                )}
              </div>

              {/* Quick Batch Actions */}
              {lines.length > 0 && (
                <div className="flex items-center gap-2 flex-wrap text-xs">
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={handleReturnAllFull}
                    disabled={eligibleLinesCount === 0 || isSaving}
                    className="h-7 text-[11px] gap-1 text-primary hover:text-primary"
                  >
                    <Sparkles className="size-3" />
                    <span>Return Full (All Items)</span>
                  </Button>
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    onClick={() => handleSelectAll(true)}
                    disabled={eligibleLinesCount === 0 || isSaving}
                    className="h-7 text-[11px] gap-1"
                  >
                    <CheckSquare className="size-3" />
                    <span>Select All</span>
                  </Button>
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    onClick={() => handleSelectAll(false)}
                    disabled={isSaving}
                    className="h-7 text-[11px] gap-1 text-muted-foreground"
                  >
                    <Square className="size-3" />
                    <span>Clear</span>
                  </Button>
                </div>
              )}
            </div>

            {/* Error State */}
            {isLinesError ? (
              <div className="flex flex-col items-center justify-center rounded-xl border border-destructive/30 bg-destructive/5 p-6 text-center space-y-2">
                <AlertTriangle className="size-8 text-destructive" />
                <p className="text-xs font-semibold text-destructive">
                  Failed to load returnable items for this invoice.
                </p>
                <p className="text-[11px] text-muted-foreground">
                  {getApiErrorMessage(linesError)}
                </p>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => refetchLines()}
                  className="mt-2 text-xs gap-1.5"
                >
                  <RefreshCw className="size-3" />
                  <span>Retry Fetching Items</span>
                </Button>
              </div>
            ) : isLoadingLines ? (
              <div className="flex h-40 flex-col items-center justify-center rounded-xl border border-border bg-card space-y-2">
                <Loader2 className="size-6 animate-spin text-primary" />
                <p className="text-xs text-muted-foreground">Fetching returnable items and verified quantities…</p>
              </div>
            ) : !selectedInvoiceId ? (
              <div className="flex h-36 flex-col items-center justify-center rounded-xl border border-dashed border-border bg-card p-4 text-center">
                <Package className="size-8 text-muted-foreground/40 mb-1" />
                <p className="text-xs font-medium text-foreground">
                  Select a purchase invoice above to view and return items.
                </p>
                <p className="text-[11px] text-muted-foreground mt-0.5">
                  Items, invoiced quantities, and remaining returnable balances will automatically populate.
                </p>
              </div>
            ) : lines.length === 0 ? (
              <div className="flex h-36 flex-col items-center justify-center rounded-xl border border-dashed border-border bg-card p-4 text-center">
                <AlertCircle className="size-8 text-muted-foreground/60 mb-1" />
                <p className="text-xs font-medium text-foreground">
                  All items on this invoice have already been returned.
                </p>
                <p className="text-[11px] text-muted-foreground mt-0.5">
                  There are no eligible remaining quantities on invoice #{selectedInvoice?.invoiceNumber}.
                </p>
              </div>
            ) : (
              <div className="overflow-hidden rounded-xl border border-border bg-card shadow-sm">
                <div className="overflow-x-auto">
                  <table className="min-w-[760px] lg:min-w-full w-full text-left text-xs">
                    <thead className="border-b bg-muted/60 text-[11px] font-semibold uppercase text-muted-foreground">
                      <tr>
                        <th className="py-2.5 pl-3 pr-2 w-[4%] text-center">
                          <Checkbox
                            checked={
                              lines.length > 0 &&
                              lines.filter((l) => l.returnableQuantity > 0).every((l) => l.included)
                            }
                            onCheckedChange={(checked) => handleSelectAll(!!checked)}
                            disabled={eligibleLinesCount === 0}
                          />
                        </th>
                        <th className="py-2.5 px-2 w-[34%] min-w-[220px]">Item Description</th>
                        <th className="py-2.5 px-2 w-[14%] text-right">Invoiced / Prev Ret</th>
                        <th className="py-2.5 px-2 w-[16%] text-center min-w-[130px]">
                          Return Qty *
                        </th>
                        <th className="py-2.5 px-2 w-[11%] text-right">Unit Price</th>
                        <th className="py-2.5 px-2 w-[9%] text-center">VAT %</th>
                        <th className="py-2.5 pl-2 pr-3 w-[12%] text-right">Return Total</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-border/60">
                      {lines.map((line, idx) => {
                        const qty =
                          typeof line.returnQuantity === "number"
                            ? line.returnQuantity
                            : 0;
                        const subtotal = qty * line.unitPrice;
                        const tax = (subtotal * line.taxRate) / 100;
                        const lineTotal = subtotal + tax;
                        const isFullyReturned = line.returnableQuantity <= 0;

                        return (
                          <tr
                            key={line.purchaseInvoiceLineId}
                            className={`transition-colors ${
                              isFullyReturned
                                ? "bg-muted/20 opacity-50"
                                : line.included
                                ? "bg-card hover:bg-muted/30"
                                : "bg-muted/5 opacity-60"
                            }`}
                          >
                            {/* Checkbox */}
                            <td className="py-2.5 pl-3 pr-2 text-center align-top">
                              <Checkbox
                                checked={line.included}
                                onCheckedChange={(checked) =>
                                  handleToggleInclude(idx, !!checked)
                                }
                                disabled={isFullyReturned}
                              />
                            </td>

                            {/* Item details */}
                            <td className="py-2.5 px-2 align-top">
                              <div className="flex items-center gap-2">
                                <span className="font-semibold text-foreground">
                                  {line.itemName}
                                </span>
                                {isFullyReturned && (
                                  <Badge variant="secondary" className="text-[9px] py-0 px-1">
                                    Fully Returned
                                  </Badge>
                                )}
                              </div>
                              <div className="flex items-center gap-1.5 text-[10px] text-muted-foreground font-mono mt-0.5">
                                <span>{line.itemCode}</span>
                                <span>•</span>
                                <span>{line.uomCode}</span>
                              </div>
                              {line.included && !isFullyReturned && (
                                <Input
                                  placeholder="Line return remarks (optional)…"
                                  value={line.remarks}
                                  onChange={(e) =>
                                    handleLineRemarksChange(idx, e.target.value)
                                  }
                                  className="h-6 text-[10px] mt-1.5"
                                />
                              )}
                            </td>

                            {/* Quantities info */}
                            <td className="py-2.5 px-2 text-right align-top font-mono">
                              <div>{formatQuantity(line.invoicedQuantity)}</div>
                              <div className="text-[10px] text-muted-foreground">
                                Ret: {formatQuantity(line.alreadyReturnedQuantity)}
                              </div>
                              <div className="text-[10px] text-emerald-600 dark:text-emerald-400 font-bold">
                                Max: {formatQuantity(line.returnableQuantity)}
                              </div>
                            </td>

                            {/* Return Quantity input with Max button */}
                            <td className="py-2.5 px-2 align-top text-center">
                              {isFullyReturned ? (
                                <span className="text-[11px] text-muted-foreground font-mono">0.00</span>
                              ) : (
                                <div className="flex items-center gap-1">
                                  <Input
                                    type="number"
                                    min="0.000001"
                                    max={line.returnableQuantity}
                                    step="any"
                                    value={line.returnQuantity}
                                    onChange={(e) =>
                                      handleLineQtyChange(idx, e.target.value)
                                    }
                                    disabled={!line.included}
                                    className="h-8 text-xs font-mono text-center"
                                  />
                                  <Button
                                    type="button"
                                    variant="outline"
                                    size="sm"
                                    onClick={() => handleSetMaxQty(idx)}
                                    disabled={!line.included}
                                    className="h-8 px-1.5 text-[10px] font-mono shrink-0"
                                    title="Set to max eligible quantity"
                                  >
                                    Max
                                  </Button>
                                </div>
                              )}
                            </td>

                            {/* Unit Price */}
                            <td className="py-2.5 px-2 text-right align-top font-mono">
                              {formatMoney(line.unitPrice, selectedInvoice?.currency ?? "NPR")}
                            </td>

                            {/* Tax Rate */}
                            <td className="py-2.5 px-2 text-center align-top font-mono text-[11px]">
                              {line.taxRate}%
                            </td>

                            {/* Line Total */}
                            <td className="py-2.5 pl-2 pr-3 text-right align-top font-mono font-semibold text-foreground">
                              {formatMoney(
                                line.included && !isFullyReturned ? lineTotal : 0,
                                selectedInvoice?.currency ?? "NPR"
                              )}
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              </div>
            )}
          </div>

          {/* Section 3: Notes & Financial Summary */}
          <div className="grid grid-cols-1 gap-5 lg:grid-cols-12">
            <div className="space-y-1.5 lg:col-span-7 xl:col-span-8">
              <Label className="text-xs font-semibold">Debit Note Remarks / Statutory Notes</Label>
              <Textarea
                placeholder="State any agreed vendor reference, carrier dispatch details, or quality report reference…"
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                rows={4}
                className="text-xs"
              />
            </div>

            <div className="rounded-xl border border-destructive/20 bg-destructive/5 p-4 space-y-2.5 lg:col-span-5 xl:col-span-4">
              <div className="flex justify-between text-xs text-muted-foreground">
                <span>Subtotal (Net Returned):</span>
                <span className="font-mono text-foreground font-medium">
                  {formatMoney(totals.subtotal, selectedInvoice?.currency ?? "NPR")}
                </span>
              </div>
              <div className="flex justify-between text-xs text-muted-foreground">
                <span>Reversed Input VAT:</span>
                <span className="font-mono text-foreground font-medium">
                  {formatMoney(totals.tax, selectedInvoice?.currency ?? "NPR")}
                </span>
              </div>
              <div className="border-t border-destructive/20 pt-2.5 flex justify-between text-sm font-bold text-foreground">
                <span>Debit Note Total:</span>
                <span className="font-mono text-destructive text-base font-bold">
                  {formatMoney(totals.grandTotal, selectedInvoice?.currency ?? "NPR")}
                </span>
              </div>
              <p className="text-[10px] text-muted-foreground pt-1">
                Posting this Debit Note automatically deducts stock from warehouse and credits Accounts Payable.
              </p>
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
            onClick={() => handleSubmit(false)}
            disabled={isSaving || activeSelectedLines.length === 0}
            className="text-xs gap-1.5"
          >
            {isCreating && !isSubmitting ? (
              <Loader2 className="size-3.5 animate-spin" />
            ) : (
              <FileText className="size-3.5" />
            )}
            <span>Save Draft Debit Note</span>
          </Button>

          <Button
            type="button"
            onClick={() => handleSubmit(true)}
            disabled={isSaving || activeSelectedLines.length === 0}
            className="text-xs gap-1.5 bg-destructive text-destructive-foreground hover:bg-destructive/90"
          >
            {isSubmitting ? (
              <Loader2 className="size-3.5 animate-spin" />
            ) : (
              <CheckCircle2 className="size-3.5" />
            )}
            <span>Save & Post Debit Note</span>
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
