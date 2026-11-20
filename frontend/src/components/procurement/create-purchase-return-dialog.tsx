"use client";

import { useEffect, useMemo, useState } from "react";
import {
  AlertCircle,
  CheckCircle2,
  FileText,
  Loader2,
  Package,
  RotateCcw,
  Warehouse,
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
  type PurchaseInvoice,
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
      { page: 0, size: 100 },
      { skip: !open }
    );

  const invoices = useMemo(
    () => invoicesData?.content.filter((i) => i.status !== "CANCELLED") ?? [],
    [invoicesData]
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
  } = useGetReturnableLinesQuery(selectedInvoiceId ?? 0, {
    skip: !open || !selectedInvoiceId,
  });

  // Populate line draft states
  useEffect(() => {
    if (returnableLinesData && returnableLinesData.length > 0) {
      setLines(
        returnableLinesData.map((line) => ({
          included: line.returnableQuantity > 0,
          purchaseInvoiceLineId: line.purchaseInvoiceLineId,
          itemId: line.itemId,
          itemCode: line.itemCode,
          itemName: line.itemName,
          uomCode: line.uomCode,
          invoicedQuantity: line.invoicedQuantity,
          alreadyReturnedQuantity: line.alreadyReturnedQuantity,
          returnableQuantity: line.returnableQuantity,
          returnQuantity: line.returnableQuantity > 0 ? line.returnableQuantity : 0,
          unitPrice: line.unitPrice,
          taxCategory: line.taxCategory,
          taxRate: line.taxRate,
          remarks: "",
        }))
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

  const handleLineQtyChange = (idx: number, val: string) => {
    setLines((prev) => {
      const next = [...prev];
      const line = { ...next[idx] };
      if (val === "") {
        line.returnQuantity = "";
      } else {
        const parsed = Number(val);
        line.returnQuantity = Math.max(
          0,
          Math.min(line.returnableQuantity, isNaN(parsed) ? 0 : parsed)
        );
      }
      next[idx] = line;
      return next;
    });
  };

  const handleToggleInclude = (idx: number, included: boolean) => {
    setLines((prev) => {
      const next = [...prev];
      next[idx] = { ...next[idx], included };
      return next;
    });
  };

  const handleLineRemarksChange = (idx: number, remarks: string) => {
    setLines((prev) => {
      const next = [...prev];
      next[idx] = { ...next[idx], remarks };
      return next;
    });
  };

  const handleSubmit = async (autoPost: boolean) => {
    if (!selectedInvoiceId) {
      toast.error("Please select a purchase invoice to return against.");
      return;
    }

    const activeLines = lines.filter(
      (l) =>
        l.included &&
        typeof l.returnQuantity === "number" &&
        l.returnQuantity > 0
    );

    if (activeLines.length === 0) {
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
      lines: activeLines.map((l) => ({
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
            {/* Invoice Selector */}
            <div className="space-y-1.5 sm:col-span-2 lg:col-span-1">
              <Label className="text-xs font-semibold">
                Original Purchase Invoice <span className="text-destructive">*</span>
              </Label>
              <Select
                value={selectedInvoiceId ? String(selectedInvoiceId) : ""}
                onValueChange={(val) => setSelectedInvoiceId(Number(val))}
                disabled={isLoadingInvoices || !!initialInvoiceId}
              >
                <SelectTrigger className="text-xs">
                  <SelectValue placeholder="Select supplier invoice…" />
                </SelectTrigger>
                <SelectContent>
                  {invoices.map((inv) => (
                    <SelectItem key={inv.id} value={String(inv.id)} className="text-xs">
                      {inv.invoiceNumber} — {inv.supplierName} ({formatMoney(inv.totalAmount, inv.currency)})
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
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
                  <p className="text-[10px] text-muted-foreground">PAN: {selectedInvoice.supplierTaxId || "—"}</p>
                </div>
                <div>
                  <span className="text-[10px] uppercase font-semibold text-muted-foreground">Invoice No & Date</span>
                  <p className="font-semibold font-mono text-foreground">{selectedInvoice.invoiceNumber}</p>
                  <p className="text-[10px] text-muted-foreground">{selectedInvoice.invoiceDate}</p>
                </div>
                <div>
                  <span className="text-[10px] uppercase font-semibold text-muted-foreground">Original GRN</span>
                  <p className="font-semibold font-mono text-foreground">{selectedInvoice.grnNumber}</p>
                  <p className="text-[10px] text-muted-foreground">PO: {selectedInvoice.poNumber}</p>
                </div>
                <div>
                  <span className="text-[10px] uppercase font-semibold text-muted-foreground">Invoiced Total</span>
                  <p className="font-semibold font-mono text-foreground">
                    {formatMoney(selectedInvoice.totalAmount, selectedInvoice.currency)}
                  </p>
                </div>
                <div>
                  <span className="text-[10px] uppercase font-semibold text-muted-foreground">Payable Balance</span>
                  <p className="font-semibold font-mono text-primary">
                    {formatMoney(selectedInvoice.balanceAmount, selectedInvoice.currency)}
                  </p>
                </div>
                <div>
                  <span className="text-[10px] uppercase font-semibold text-muted-foreground">Paid Amount</span>
                  <p className="font-semibold font-mono text-muted-foreground">
                    {formatMoney(selectedInvoice.paidAmount, selectedInvoice.currency)}
                  </p>
                </div>
              </div>
            </div>
          )}

          {/* Section 2: Return Items Table */}
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Package className="size-4 text-destructive" />
                <h3 className="text-sm font-bold text-foreground">Items to Return</h3>
                <Badge variant="secondary" className="text-[10px]">
                  {lines.filter((l) => l.included).length} of {lines.length} Selected
                </Badge>
              </div>
              <p className="text-[11px] text-muted-foreground hidden sm:block">
                Check items to return and adjust quantities within eligible return limits.
              </p>
            </div>

            {isLoadingLines ? (
              <div className="flex h-32 items-center justify-center rounded-xl border border-border bg-card">
                <Loader2 className="size-5 animate-spin text-muted-foreground" />
              </div>
            ) : lines.length === 0 ? (
              <div className="flex h-32 flex-col items-center justify-center rounded-xl border border-dashed border-border bg-card p-4 text-center">
                <AlertCircle className="size-6 text-muted-foreground/60 mb-1" />
                <p className="text-xs font-medium text-foreground">
                  {selectedInvoiceId
                    ? "All items on this invoice have already been returned."
                    : "Select a purchase invoice to view returnable items."}
                </p>
              </div>
            ) : (
              <div className="overflow-hidden rounded-xl border border-border bg-card">
                <div className="overflow-x-auto">
                  <table className="min-w-[760px] lg:min-w-full w-full text-left text-xs">
                    <thead className="border-b bg-muted/60 text-[11px] font-semibold uppercase text-muted-foreground">
                      <tr>
                        <th className="py-2.5 pl-3 pr-2 w-[4%] text-center">
                          <Checkbox
                            checked={lines.length > 0 && lines.every((l) => l.included)}
                            onCheckedChange={(checked) =>
                              setLines((prev) =>
                                prev.map((l) => ({
                                  ...l,
                                  included: !!checked && l.returnableQuantity > 0,
                                }))
                              )
                            }
                          />
                        </th>
                        <th className="py-2.5 px-2 w-[34%] min-w-[220px]">Item Description</th>
                        <th className="py-2.5 px-2 w-[12%] text-right">Invoiced / Prev Ret</th>
                        <th className="py-2.5 px-2 w-[14%] text-center min-w-[110px]">
                          Return Qty *
                        </th>
                        <th className="py-2.5 px-2 w-[12%] text-right">Unit Price</th>
                        <th className="py-2.5 px-2 w-[10%] text-center">VAT %</th>
                        <th className="py-2.5 pl-2 pr-3 w-[14%] text-right">Return Total</th>
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

                        return (
                          <tr
                            key={line.purchaseInvoiceLineId}
                            className={`transition-colors ${
                              line.included
                                ? "bg-card hover:bg-muted/30"
                                : "bg-muted/10 opacity-60"
                            }`}
                          >
                            {/* Checkbox */}
                            <td className="py-2.5 pl-3 pr-2 text-center align-top">
                              <Checkbox
                                checked={line.included}
                                onCheckedChange={(checked) =>
                                  handleToggleInclude(idx, !!checked)
                                }
                                disabled={line.returnableQuantity <= 0}
                              />
                            </td>

                            {/* Item details */}
                            <td className="py-2.5 px-2 align-top">
                              <div className="font-medium text-foreground">
                                {line.itemName}
                              </div>
                              <div className="flex items-center gap-1.5 text-[10px] text-muted-foreground font-mono mt-0.5">
                                <span>{line.itemCode}</span>
                                <span>•</span>
                                <span>{line.uomCode}</span>
                              </div>
                              {line.included && (
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
                              <div className="text-[10px] text-emerald-600 dark:text-emerald-400 font-semibold">
                                Max: {formatQuantity(line.returnableQuantity)}
                              </div>
                            </td>

                            {/* Return Quantity input */}
                            <td className="py-2.5 px-2 align-top text-center">
                              <Input
                                type="number"
                                min="0.001"
                                max={line.returnableQuantity}
                                step="any"
                                value={line.returnQuantity}
                                onChange={(e) =>
                                  handleLineQtyChange(idx, e.target.value)
                                }
                                disabled={!line.included || line.returnableQuantity <= 0}
                                className="h-8 text-xs font-mono text-center"
                              />
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
                                line.included ? lineTotal : 0,
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
            disabled={isSaving || lines.filter((l) => l.included).length === 0}
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
            disabled={isSaving || lines.filter((l) => l.included).length === 0}
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

