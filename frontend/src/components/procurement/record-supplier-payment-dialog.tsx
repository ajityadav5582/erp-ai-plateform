"use client";

import { useEffect, useMemo, useState } from "react";
import {
  AlertCircle,
  Building2,
  CheckCircle2,
  Info,
  Loader2,
  Sparkles,
  Wallet,
} from "lucide-react";
import { toast } from "sonner";
import {
  PAYMENT_METHODS,
  PAYMENT_METHOD_LABELS,
  REFERENCE_METHODS,
  usePaySupplierMutation,
  type PaymentMethod,
  type SupplierPayment,
} from "@/services/supplier-payment.service";
import {
  useGetOutstandingInvoicesQuery,
  type PurchaseInvoice,
} from "@/services/purchase-invoice.service";
import {
  useGetSuppliersQuery,
  type SupplierListResponse,
} from "@/services/supplier.service";
import { SupplierCombobox } from "@/components/procurement/supplier-combobox";
import { SupplierBalanceBadge } from "@/components/procurement/supplier-balance-badge";
import { getApiErrorMessage } from "@/services/error-handler";
import { formatDate, formatMoney } from "@/lib/procurement-format";

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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { Checkbox } from "@/components/ui/checkbox";
import { Badge } from "@/components/ui/badge";

interface RecordSupplierPaymentDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onPaid?: (payment: SupplierPayment) => void;
}

interface InvoiceAllocationRow {
  invoice: PurchaseInvoice;
  selected: boolean;
  allocatedAmount: string;
  notes: string;
}

export function RecordSupplierPaymentDialog({
  open,
  onOpenChange,
  onPaid,
}: RecordSupplierPaymentDialogProps) {
  const [selectedSupplier, setSelectedSupplier] =
    useState<SupplierListResponse | null>(null);

  const [paymentDate, setPaymentDate] = useState(() =>
    new Date().toISOString().split("T")[0]
  );
  const [method, setMethod] = useState<PaymentMethod>("BANK_TRANSFER");
  const [referenceNo, setReferenceNo] = useState("");
  const [paidFrom, setPaidFrom] = useState("");
  const [totalPaymentAmount, setTotalPaymentAmount] = useState("");
  const [notes, setNotes] = useState("");

  const [allocationRows, setAllocationRows] = useState<InvoiceAllocationRow[]>(
    []
  );

  const [pay, { isLoading: isPosting }] = usePaySupplierMutation();

  const { data: suppliersPage, isLoading: suppliersLoading } =
    useGetSuppliersQuery({ page: 0, size: 200 });
  const suppliers = useMemo(
    () => suppliersPage?.content ?? [],
    [suppliersPage]
  );

  const {
    data: outstandingPage,
    isLoading: invoicesLoading,
    isFetching: invoicesFetching,
  } = useGetOutstandingInvoicesQuery(
    {
      supplierId: selectedSupplier ? Number(selectedSupplier.id) : undefined,
      size: 100,
    },
    { skip: !open || !selectedSupplier }
  );

  const outstandingInvoices = useMemo(
    () => outstandingPage?.content ?? [],
    [outstandingPage]
  );

  // Sync outstanding invoices into allocation table state when supplier or query changes
  useEffect(() => {
    if (!selectedSupplier) {
      setAllocationRows([]);
      return;
    }

    setAllocationRows(
      outstandingInvoices.map((inv) => ({
        invoice: inv,
        selected: false,
        allocatedAmount: "",
        notes: "",
      }))
    );
  }, [outstandingInvoices, selectedSupplier]);

  // Reset form when modal opens
  useEffect(() => {
    if (open) {
      setSelectedSupplier(null);
      setPaymentDate(new Date().toISOString().split("T")[0]);
      setMethod("BANK_TRANSFER");
      setReferenceNo("");
      setPaidFrom("");
      setTotalPaymentAmount("");
      setNotes("");
      setAllocationRows([]);
    }
  }, [open]);

  // Calculations
  const totalAllocated = useMemo(() => {
    return allocationRows.reduce((acc, row) => {
      const val = parseFloat(row.allocatedAmount);
      return acc + (Number.isFinite(val) && val > 0 ? val : 0);
    }, 0);
  }, [allocationRows]);

  const paymentAmountNum = parseFloat(totalPaymentAmount) || 0;
  const unallocatedAmount = Math.max(0, paymentAmountNum - totalAllocated);
  const isOverAllocated = totalAllocated > paymentAmountNum && paymentAmountNum > 0;

  const totalOutstandingBalance = useMemo(() => {
    return allocationRows.reduce((acc, row) => acc + (row.invoice.balanceAmount || 0), 0);
  }, [allocationRows]);

  // Auto-allocate (FIFO): allocates payment amount starting from oldest due date
  const handleAutoAllocateFIFO = () => {
    const available = parseFloat(totalPaymentAmount);
    if (!Number.isFinite(available) || available <= 0) {
      toast.error("Please enter a valid Total Payment Amount first to auto-allocate.");
      return;
    }

    let remainingToAllocate = available;
    const updated = allocationRows.map((row) => {
      if (remainingToAllocate <= 0) {
        return { ...row, selected: false, allocatedAmount: "" };
      }
      const due = row.invoice.balanceAmount;
      const allocateForThis = Math.min(remainingToAllocate, due);
      remainingToAllocate -= allocateForThis;

      return {
        ...row,
        selected: allocateForThis > 0,
        allocatedAmount: allocateForThis > 0 ? allocateForThis.toFixed(2) : "",
      };
    });

    setAllocationRows(updated);
    toast.success("Payment automatically distributed across invoices by due date (FIFO).");
  };

  // Select all & pay full balance
  const handlePayAllSelected = () => {
    let sum = 0;
    const updated = allocationRows.map((row) => {
      const due = row.invoice.balanceAmount;
      sum += due;
      return {
        ...row,
        selected: true,
        allocatedAmount: due.toFixed(2),
      };
    });
    setAllocationRows(updated);
    setTotalPaymentAmount(sum.toFixed(2));
    toast.success("All outstanding invoices selected for full settlement.");
  };

  // Clear allocations
  const handleClearAllocations = () => {
    const updated = allocationRows.map((row) => ({
      ...row,
      selected: false,
      allocatedAmount: "",
    }));
    setAllocationRows(updated);
  };

  // Row toggles
  const handleToggleRow = (index: number, checked: boolean) => {
    setAllocationRows((prev) => {
      const next = [...prev];
      const row = next[index];
      if (!row) return prev;
      const due = row.invoice.balanceAmount;
      next[index] = {
        ...row,
        selected: checked,
        allocatedAmount: checked ? due.toFixed(2) : "",
      };
      return next;
    });
  };

  const handleAllocationAmountChange = (index: number, val: string) => {
    setAllocationRows((prev) => {
      const next = [...prev];
      const row = next[index];
      if (!row) return prev;
      const numVal = parseFloat(val);
      next[index] = {
        ...row,
        selected: Number.isFinite(numVal) && numVal > 0,
        allocatedAmount: val,
      };
      return next;
    });
  };

  // Form submission
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!selectedSupplier) {
      toast.error("Please select a supplier.");
      return;
    }

    if (!Number.isFinite(paymentAmountNum) || paymentAmountNum <= 0) {
      toast.error("Payment amount must be greater than zero.");
      return;
    }

    if (totalAllocated > paymentAmountNum) {
      toast.error(
        `Total allocated (NPR ${totalAllocated.toLocaleString()}) cannot exceed total payment (NPR ${paymentAmountNum.toLocaleString()}).`
      );
      return;
    }

    const activeAllocations = allocationRows
      .filter((r) => {
        const val = parseFloat(r.allocatedAmount);
        return Number.isFinite(val) && val > 0;
      })
      .map((r) => ({
        purchaseInvoiceId: r.invoice.id,
        allocatedAmount: parseFloat(r.allocatedAmount),
        discountAmount: 0,
        notes: r.notes.trim() || undefined,
      }));

    try {
      const res = await pay({
        supplierId: Number(selectedSupplier.id),
        amount: paymentAmountNum,
        paymentMethod: method,
        paymentDate: paymentDate || undefined,
        referenceNo: referenceNo.trim() || undefined,
        paidFrom: paidFrom.trim() || undefined,
        notes: notes.trim() || undefined,
        allocations: activeAllocations.length > 0 ? activeAllocations : undefined,
      }).unwrap();

      toast.success(`Payment ${res.paymentNumber} posted successfully.`);
      onPaid?.(res);
      onOpenChange(false);
    } catch (err) {
      toast.error(getApiErrorMessage(err));
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[94vh] w-[96vw] max-w-[96vw] sm:max-w-4xl md:max-w-5xl lg:max-w-6xl xl:max-w-7xl 2xl:max-w-[1400px] overflow-y-auto p-4 sm:p-6 lg:p-7 xl:p-8">
        <form onSubmit={handleSubmit} className="space-y-6">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2 text-xl font-bold tracking-tight">
              <Wallet className="size-5 text-primary" />
              Record Supplier Payment
            </DialogTitle>
            <DialogDescription>
              Record a single cheque, bank transfer, or cash disbursement settling
              multiple supplier invoices or advance deposits.
            </DialogDescription>
          </DialogHeader>

          {/* Supplier & Payment Header Form */}
          <div className="grid gap-4 rounded-xl border border-border/80 bg-muted/20 p-4 sm:grid-cols-2 lg:grid-cols-4">
            {/* Supplier Selector */}
            <div className="space-y-1.5 sm:col-span-2">
              <Label className="text-xs font-semibold uppercase text-muted-foreground">
                Supplier <span className="text-destructive">*</span>
              </Label>
              <SupplierCombobox
                value={selectedSupplier?.id}
                suppliers={suppliers}
                isLoading={suppliersLoading}
                onSelect={(s) => setSelectedSupplier(s)}
                placeholder="Search supplier by name, code or PAN…"
              />
              <SupplierBalanceBadge supplierId={selectedSupplier?.id} className="mt-1" />
            </div>

            {/* Payment Date */}
            <div className="space-y-1.5">
              <Label className="text-xs font-semibold uppercase text-muted-foreground">
                Payment Date <span className="text-destructive">*</span>
              </Label>
              <Input
                type="date"
                required
                value={paymentDate}
                onChange={(e) => setPaymentDate(e.target.value)}
                className="h-10 bg-background"
              />
            </div>

            {/* Payment Method */}
            <div className="space-y-1.5">
              <Label className="text-xs font-semibold uppercase text-muted-foreground">
                Payment Method <span className="text-destructive">*</span>
              </Label>
              <Select
                value={method}
                onValueChange={(v) => setMethod(v as PaymentMethod)}
              >
                <SelectTrigger className="h-10 bg-background">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {PAYMENT_METHODS.map((m) => (
                    <SelectItem key={m} value={m}>
                      {PAYMENT_METHOD_LABELS[m]}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {/* Total Payment Amount */}
            <div className="space-y-1.5 sm:col-span-2">
              <div className="flex items-center justify-between">
                <Label className="text-xs font-semibold uppercase text-muted-foreground">
                  Total Payment Amount (NPR) <span className="text-destructive">*</span>
                </Label>
                {totalAllocated > 0 && (
                  <button
                    type="button"
                    onClick={() => setTotalPaymentAmount(totalAllocated.toFixed(2))}
                    className="text-xs text-primary hover:underline"
                  >
                    Match Total Allocated (NPR {totalAllocated.toLocaleString()})
                  </button>
                )}
              </div>
              <div className="relative">
                <span className="absolute left-3 top-2.5 text-xs font-semibold text-muted-foreground">
                  NPR
                </span>
                <Input
                  type="number"
                  step="0.01"
                  min="0.01"
                  required
                  placeholder="0.00"
                  value={totalPaymentAmount}
                  onChange={(e) => setTotalPaymentAmount(e.target.value)}
                  className="h-10 pl-12 text-base font-semibold bg-background"
                />
              </div>
            </div>

            {/* Reference / Cheque No */}
            <div className="space-y-1.5">
              <Label className="text-xs font-semibold uppercase text-muted-foreground">
                Cheque / UTR Reference
              </Label>
              <Input
                placeholder={
                  REFERENCE_METHODS.includes(method)
                    ? "e.g. CHQ-991203 or UTR-1928"
                    : "Optional reference"
                }
                value={referenceNo}
                onChange={(e) => setReferenceNo(e.target.value)}
                className="h-10 bg-background"
              />
            </div>

            {/* Paid From Account / Branch */}
            <div className="space-y-1.5">
              <Label className="text-xs font-semibold uppercase text-muted-foreground">
                Paid From Account / Branch
              </Label>
              <Input
                placeholder="e.g. Nabil Bank Treasury A/C"
                value={paidFrom}
                onChange={(e) => setPaidFrom(e.target.value)}
                className="h-10 bg-background"
              />
            </div>

            {/* Notes */}
            <div className="space-y-1.5 sm:col-span-2 lg:col-span-4">
              <Label className="text-xs font-semibold uppercase text-muted-foreground">
                Notes & Remittance Memo
              </Label>
              <Textarea
                rows={2}
                placeholder="Optional notes or remittance instructions…"
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                className="bg-background"
              />
            </div>
          </div>

          {/* Outstanding Invoices Section */}
          <div className="space-y-3">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div>
                <h3 className="text-sm font-semibold text-foreground">
                  Outstanding Invoices for Settlement
                </h3>
                <p className="text-xs text-muted-foreground">
                  {selectedSupplier
                    ? `Showing unpaid or partially paid invoices for ${selectedSupplier.name}. Oldest due date first.`
                    : "Select a supplier above to load their outstanding invoices."}
                </p>
              </div>

              {selectedSupplier && allocationRows.length > 0 && (
                <div className="flex flex-wrap items-center gap-2">
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={handleAutoAllocateFIFO}
                    className="h-8 gap-1.5 text-xs text-primary border-primary/30 hover:bg-primary/5"
                  >
                    <Sparkles className="size-3.5" />
                    Auto-Allocate (FIFO)
                  </Button>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={handlePayAllSelected}
                    className="h-8 text-xs"
                  >
                    Select & Pay All
                  </Button>
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    onClick={handleClearAllocations}
                    className="h-8 text-xs text-muted-foreground"
                  >
                    Clear
                  </Button>
                </div>
              )}
            </div>

            {/* Table Area */}
            {!selectedSupplier ? (
              <div className="flex flex-col items-center justify-center rounded-xl border border-dashed border-border py-12 text-center">
                <Building2 className="size-10 text-muted-foreground/40 mb-2" />
                <p className="text-sm font-medium text-foreground">No Supplier Selected</p>
                <p className="text-xs text-muted-foreground max-w-sm mt-1">
                  Choose a supplier from the dropdown above to view outstanding invoices and allocate funds.
                </p>
              </div>
            ) : invoicesLoading || invoicesFetching ? (
              <div className="flex flex-col items-center justify-center rounded-xl border border-border py-12 text-center">
                <Loader2 className="size-8 animate-spin text-primary mb-2" />
                <p className="text-xs text-muted-foreground">Loading supplier invoices…</p>
              </div>
            ) : allocationRows.length === 0 ? (
              <div className="rounded-xl border border-border bg-muted/10 p-6 text-center">
                <CheckCircle2 className="size-8 text-emerald-600 mx-auto mb-2" />
                <p className="text-sm font-semibold text-foreground">
                  No Outstanding Invoices Found
                </p>
                <p className="text-xs text-muted-foreground max-w-md mx-auto mt-1">
                  This supplier currently has zero outstanding balance. You may still record an
                  advance payment above, which will sit as an unallocated credit on their account.
                </p>
              </div>
            ) : (
              <div className="overflow-x-auto rounded-xl border border-border bg-card">
                <table className="w-full text-left text-xs">
                  <thead className="border-b border-border bg-muted/50 font-medium text-muted-foreground">
                    <tr>
                      <th className="py-2.5 px-3 w-10 text-center">
                        <span className="sr-only">Select</span>
                      </th>
                      <th className="py-2.5 px-3 font-semibold">Invoice #</th>
                      <th className="py-2.5 px-3 font-semibold">Invoice Date</th>
                      <th className="py-2.5 px-3 font-semibold">Due Date</th>
                      <th className="py-2.5 px-3 font-semibold text-right">Invoice Total</th>
                      <th className="py-2.5 px-3 font-semibold text-right">Balance Due</th>
                      <th className="py-2.5 px-3 font-semibold text-right w-44">Allocated (NPR)</th>
                      <th className="py-2.5 px-3 font-semibold w-40">Line Note</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-border">
                    {allocationRows.map((row, idx) => {
                      const isOverdue =
                        row.invoice.dueDate &&
                        new Date(row.invoice.dueDate) < new Date();

                      return (
                        <tr
                          key={row.invoice.id}
                          className={`transition-colors ${
                            row.selected ? "bg-primary/5" : "hover:bg-muted/30"
                          }`}
                        >
                          <td className="py-2.5 px-3 text-center">
                            <Checkbox
                              checked={row.selected}
                              onCheckedChange={(checked) =>
                                handleToggleRow(idx, Boolean(checked))
                              }
                            />
                          </td>
                          <td className="py-2.5 px-3 font-medium text-foreground">
                            <div className="flex items-center gap-1.5">
                              <span className="font-mono text-xs font-semibold">
                                {row.invoice.invoiceNumber}
                              </span>
                              <Badge
                                variant="outline"
                                className="text-[10px] px-1 py-0 font-normal"
                              >
                                {row.invoice.status}
                              </Badge>
                            </div>
                          </td>
                          <td className="py-2.5 px-3 text-muted-foreground">
                            {formatDate(row.invoice.invoiceDate)}
                          </td>
                          <td className="py-2.5 px-3">
                            <span
                              className={
                                isOverdue
                                  ? "font-medium text-destructive"
                                  : "text-muted-foreground"
                              }
                            >
                              {row.invoice.dueDate
                                ? formatDate(row.invoice.dueDate)
                                : "—"}
                            </span>
                          </td>
                          <td className="py-2.5 px-3 text-right font-medium text-foreground">
                            {formatMoney(row.invoice.totalAmount, row.invoice.currency)}
                            {row.invoice.isTdsApplicable && (
                              <div className="text-[10px] text-amber-600 font-normal">
                                Net: {formatMoney(row.invoice.netPayableAmount ?? row.invoice.totalAmount, row.invoice.currency)}
                              </div>
                            )}
                          </td>
                          <td className="py-2.5 px-3 text-right font-semibold text-foreground">
                            {formatMoney(row.invoice.balanceAmount, row.invoice.currency)}
                          </td>
                          <td className="py-2.5 px-3 text-right">
                            <div className="flex items-center justify-end gap-1.5">
                              <Input
                                type="number"
                                step="0.01"
                                min="0"
                                max={row.invoice.balanceAmount}
                                placeholder="0.00"
                                value={row.allocatedAmount}
                                onChange={(e) =>
                                  handleAllocationAmountChange(idx, e.target.value)
                                }
                                className="h-8 w-28 text-right font-mono text-xs bg-background"
                              />
                              <Button
                                type="button"
                                variant="ghost"
                                size="sm"
                                onClick={() =>
                                  handleAllocationAmountChange(
                                    idx,
                                    row.invoice.balanceAmount.toFixed(2)
                                  )
                                }
                                className="h-8 px-2 text-[11px] text-muted-foreground hover:text-foreground"
                                title="Pay Full Balance"
                              >
                                Full
                              </Button>
                            </div>
                          </td>
                          <td className="py-2.5 px-3">
                            <Input
                              type="text"
                              placeholder="Optional note"
                              value={row.notes}
                              onChange={(e) => {
                                const val = e.target.value;
                                setAllocationRows((prev) => {
                                  const next = [...prev];
                                  const current = next[idx];
                                  if (!current) return prev;
                                  next[idx] = { ...current, notes: val };
                                  return next;
                                });
                              }}
                              className="h-8 text-xs bg-background"
                            />
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          {/* Live Treasury Reconciliation Summary Card */}
          <div className="rounded-xl border border-border/80 bg-muted/30 p-4">
            <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4 text-xs">
              <div>
                <span className="text-muted-foreground">Total Invoices Balance:</span>
                <p className="text-sm font-semibold text-foreground">
                  NPR {totalOutstandingBalance.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                </p>
              </div>

              <div>
                <span className="text-muted-foreground">Total Invoices Allocated:</span>
                <p className="text-sm font-semibold text-primary">
                  NPR {totalAllocated.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                </p>
              </div>

              <div>
                <span className="text-muted-foreground">Payment Amount Leaving Bank:</span>
                <p className="text-sm font-semibold text-foreground">
                  NPR {paymentAmountNum.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                </p>
              </div>

              <div>
                <span className="text-muted-foreground">Unallocated / Advance Credit:</span>
                <p
                  className={`text-sm font-semibold ${
                    unallocatedAmount > 0
                      ? "text-emerald-600 dark:text-emerald-400"
                      : "text-muted-foreground"
                  }`}
                >
                  NPR {unallocatedAmount.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                </p>
              </div>
            </div>

            {/* Over-allocation warning alert */}
            {isOverAllocated && (
              <div className="mt-3 flex items-center gap-2 rounded-lg border border-destructive/30 bg-destructive/10 p-2.5 text-xs text-destructive">
                <AlertCircle className="size-4 shrink-0" />
                <span>
                  Total allocated exceeds the Total Payment Amount by{" "}
                  <strong>
                    NPR {(totalAllocated - paymentAmountNum).toLocaleString(undefined, { minimumFractionDigits: 2 })}
                  </strong>
                  . Please increase payment amount or adjust allocation lines.
                </span>
              </div>
            )}

            {/* Advance credit information notice */}
            {unallocatedAmount > 0 && !isOverAllocated && (
              <div className="mt-3 flex items-center gap-2 rounded-lg border border-emerald-500/20 bg-emerald-500/10 p-2.5 text-xs text-emerald-800 dark:text-emerald-300">
                <Info className="size-4 shrink-0" />
                <span>
                  NPR {unallocatedAmount.toLocaleString(undefined, { minimumFractionDigits: 2 })} will
                  remain unallocated and sit as an advance credit on this supplier&apos;s ledger for future bills.
                </span>
              </div>
            )}
          </div>

          <DialogFooter className="gap-2 sm:gap-0">
            <Button
              type="button"
              variant="outline"
              disabled={isPosting}
              onClick={() => onOpenChange(false)}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              disabled={isPosting || isOverAllocated || paymentAmountNum <= 0}
              className="gap-2"
            >
              {isPosting && <Loader2 className="size-4 animate-spin" />}
              {totalAllocated === 0 && paymentAmountNum > 0 ? "Post Advance Payment" : "Post Payment"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
