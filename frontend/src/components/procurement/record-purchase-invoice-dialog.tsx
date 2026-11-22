"use client";

import { useEffect, useMemo, useState } from "react";
import { Loader2, Receipt } from "lucide-react";
import { toast } from "sonner";
import {
  useCreatePurchaseInvoiceMutation,
  type CreateInvoiceLineRequest,
  type PurchaseInvoice,
} from "@/services/purchase-invoice.service";
import { useGetGoodsReceiptQuery } from "@/services/goods-receipt.service";
import { getApiErrorMessage } from "@/services/error-handler";
import { formatMoney } from "@/lib/procurement-format";

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
import { SupplierBalanceBadge } from "@/components/procurement/supplier-balance-badge";

/**
 * A single editable invoice line.
 *
 * Kept as a local shape rather than the request type: these are strings because they
 * come straight out of inputs, and converting once on submit is clearer than
 * maintaining a parallel numeric state that drifts from what the user typed.
 */
interface EditableLine {
  goodsReceiptLineId: number;
  itemName: string;
  itemCode: string;
  uomCode: string;
  /** The quantity received, shown as the pre-filled default. */
  receivedQuantity: number;
  /** The received unit price, shown as the pre-filled default. */
  receivedUnitPrice: number;
  quantity: string;
  unitPrice: string;
}

interface RecordPurchaseInvoiceDialogProps {
  goodsReceiptId: number;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onCreated?: (invoice: PurchaseInvoice) => void;
}

/**
 * Records a supplier's invoice against a delivery that has been received.
 *
 * **Pre-filled, never locked.** The lines arrive seeded from the receipt's quantities
 * and prices because the common case is an invoice that matches the delivery exactly,
 * and retyping eleven identical rows is how transcription errors happen.
 *
 * But every field stays editable, and the screen says so. This is the three-way match:
 * the amount owed is what the *supplier* says they charged, and comparing that against
 * the order is the entire point. A form that locked the ordered price would make a
 * mismatch impossible to record - and an unrepresentable discrepancy is one nobody
 * notices until the year-end accounts disagree.
 */
export function RecordPurchaseInvoiceDialog({
  goodsReceiptId,
  open,
  onOpenChange,
  onCreated,
}: RecordPurchaseInvoiceDialogProps) {
  const [invoiceNumber, setInvoiceNumber] = useState("");
  const [invoiceDate, setInvoiceDate] = useState("");
  const [dueDate, setDueDate] = useState("");
  const [notes, setNotes] = useState("");
  const [lines, setLines] = useState<EditableLine[]>([]);
  const [isTdsApplicable, setIsTdsApplicable] = useState(false);
  const [tdsRate, setTdsRate] = useState("1.5");

  const {
    data: receipt,
    isLoading: isLoadingReceipt,
  } = useGetGoodsReceiptQuery(goodsReceiptId, { skip: !open });

  const [create, { isLoading }] = useCreatePurchaseInvoiceMutation();

  // Seed from the receipt every time the dialog opens, so a second invoice for a
  // different delivery never inherits the previous one's figures.
  useEffect(() => {
    if (open && receipt) {
      setInvoiceNumber("");
      setInvoiceDate("");
      setDueDate("");
      setNotes("");
      setIsTdsApplicable(false);
      setTdsRate("1.5");
      setLines(
        receipt.lines.map((line) => ({
          goodsReceiptLineId: line.id,
          itemName: line.itemName,
          itemCode: line.itemCode,
          uomCode: line.uomCode,
          receivedQuantity: Number(line.receivedQuantity),
          receivedUnitPrice: Number(line.unitPrice),
          quantity: String(line.receivedQuantity),
          unitPrice: String(line.unitPrice),
        }))
      );
    }
  }, [open, receipt]);

  const updateLine = (
    goodsReceiptLineId: number,
    field: "quantity" | "unitPrice",
    value: string
  ) => {
    setLines((prev) =>
      prev.map((line) =>
        line.goodsReceiptLineId === goodsReceiptLineId
          ? { ...line, [field]: value }
          : line
      )
    );
  };

  /**
   * Whether the typed figures still match what was received.
   *
   * Shown as a warning, never a block: a supplier billing a short delivery or a
   * different price is recording a real event that this system is meant to surface,
   * not an error the clerk should be stopped from entering.
   */
  const variances = useMemo(
    () =>
      lines.filter((line) => {
        const qty = Number(line.quantity);
        const price = Number(line.unitPrice);
        const qtyDiffers =
          !Number.isNaN(qty) && Math.abs(qty - line.receivedQuantity) > 1e-9;
        const priceDiffers =
          !Number.isNaN(price) &&
          Math.abs(price - line.receivedUnitPrice) > 1e-9;
        return qtyDiffers || priceDiffers;
      }),
    [lines]
  );

  const canSubmit = invoiceNumber.trim().length > 0 && lines.length > 0 && !isLoading;

  const totals = useMemo(() => {
    let subtotal = 0;
    for (const line of lines) {
      const q = Number(line.quantity) || 0;
      const p = Number(line.unitPrice) || 0;
      subtotal += q * p;
    }
    const tax = subtotal * 0.13;
    const gross = subtotal + tax;
    const rate = Number(tdsRate) || 0;
    const tds = isTdsApplicable ? subtotal * (rate / 100) : 0;
    const netPayable = gross - tds;
    return { subtotal, tax, gross, tds, netPayable };
  }, [lines, isTdsApplicable, tdsRate]);

  const handleSubmit = async () => {
    if (!canSubmit) return;

    const payload: CreateInvoiceLineRequest[] = lines.map((line) => {
      const quantity = Number(line.quantity);
      const unitPrice = Number(line.unitPrice);
      const entry: CreateInvoiceLineRequest = { goodsReceiptLineId: line.goodsReceiptLineId };

      if (Number.isFinite(quantity) && line.quantity.trim() !== "") {
        entry.quantity = quantity;
      }
      if (Number.isFinite(unitPrice) && line.unitPrice.trim() !== "") {
        entry.unitPrice = unitPrice;
      }
      return entry;
    });

    try {
      const invoice = await create({
        goodsReceiptId,
        invoiceNumber: invoiceNumber.trim(),
        ...(invoiceDate ? { invoiceDate } : {}),
        ...(dueDate ? { dueDate } : {}),
        ...(notes.trim() ? { notes: notes.trim() } : {}),
        isTdsApplicable,
        tdsRate: isTdsApplicable ? Number(tdsRate) : 0,
        lines: payload,
      }).unwrap();

      toast.success(`Supplier invoice ${invoice.invoiceNumber} recorded`, {
        description: isTdsApplicable
          ? `${formatMoney(invoice.netPayableAmount ?? invoice.totalAmount, invoice.currency)} net payable (TDS withheld: ${formatMoney(invoice.tdsAmount ?? 0, invoice.currency)}).`
          : `${formatMoney(invoice.totalAmount, invoice.currency)} payable.`,
      });
      onCreated?.(invoice);
      onOpenChange(false);
    } catch (err) {
      toast.error("Failed to record the invoice", {
        description: getApiErrorMessage(err),
      });
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[94vh] w-[96vw] max-w-[96vw] sm:max-w-4xl md:max-w-5xl lg:max-w-6xl xl:max-w-7xl 2xl:max-w-[1400px] overflow-y-auto p-4 sm:p-6 lg:p-7 xl:p-8">
        <DialogHeader>
          <DialogTitle>Record supplier invoice</DialogTitle>
          <DialogDescription>
            {receipt
              ? `Billing against receipt ${receipt.grnNumber} (order ${receipt.poNumber}). Quantities and prices are prefilled from what arrived — change them if the supplier billed something different.`
              : "Loading the delivery…"}
          </DialogDescription>
        </DialogHeader>

        {isLoadingReceipt ? (
          <div className="flex items-center gap-2 py-6 text-sm text-muted-foreground">
            <Loader2 className="size-4 animate-spin" aria-hidden="true" />
            Loading the delivery…
          </div>
        ) : (
          <div className="space-y-4">
            <SupplierBalanceBadge supplierId={receipt?.supplierId} />
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="invoice-number">
                  Supplier invoice number
                </Label>
                <Input
                  id="invoice-number"
                  value={invoiceNumber}
                  onChange={(e) => setInvoiceNumber(e.target.value)}
                  placeholder="The supplier's own number"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="invoice-date">
                  Invoice date{" "}
                  <span className="font-normal text-muted-foreground">
                    (defaults to today)
                  </span>
                </Label>
                <Input
                  id="invoice-date"
                  type="date"
                  value={invoiceDate}
                  onChange={(e) => setInvoiceDate(e.target.value)}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="invoice-due">Due date</Label>
                <Input
                  id="invoice-due"
                  type="date"
                  value={dueDate}
                  onChange={(e) => setDueDate(e.target.value)}
                />
              </div>
            </div>

            {variances.length > 0 && (
              <div className="rounded-md border border-amber-500/30 bg-amber-500/10 p-3 text-sm text-amber-700 dark:text-amber-400">
                <p className="font-medium">
                  {variances.length} line
                  {variances.length === 1 ? "" : "s"} differ from what was received.
                </p>
                <p className="mt-1 text-xs">
                  That is recorded as billed, not corrected — but it is worth checking
                  against the supplier&apos;s paperwork before approving payment.
                </p>
              </div>
            )}

            <div className="rounded-md border border-border">
              <div className="border-b border-border bg-muted/40 px-3 py-2 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                Billed lines
              </div>
              <ul className="divide-y divide-border">
                {lines.map((line) => {
                  const differs =
                    variances.some(
                      (v) => v.goodsReceiptLineId === line.goodsReceiptLineId
                    );
                  return (
                    <li
                      key={line.goodsReceiptLineId}
                      className="grid gap-3 px-3 py-3 sm:grid-cols-[1fr_7rem_7rem]"
                    >
                      <div className="min-w-0">
                        <p className="text-sm font-medium text-foreground">
                          {line.itemName}
                          {differs && (
                            <span className="ml-2 text-xs font-normal text-amber-600 dark:text-amber-400">
                              differs from receipt
                            </span>
                          )}
                        </p>
                        <p className="text-xs text-muted-foreground">
                          {line.itemCode} · {line.uomCode} · received{" "}
                          {line.receivedQuantity} at {line.receivedUnitPrice}
                        </p>
                      </div>
                      <div className="space-y-1">
                        <Label
                          htmlFor={`qty-${line.goodsReceiptLineId}`}
                          className="text-xs text-muted-foreground"
                        >
                          Quantity
                        </Label>
                        <Input
                          id={`qty-${line.goodsReceiptLineId}`}
                          type="number"
                          step="any"
                          min="0"
                          value={line.quantity}
                          onChange={(e) =>
                            updateLine(line.goodsReceiptLineId, "quantity", e.target.value)
                          }
                        />
                      </div>
                      <div className="space-y-1">
                        <Label
                          htmlFor={`price-${line.goodsReceiptLineId}`}
                          className="text-xs text-muted-foreground"
                        >
                          Unit price
                        </Label>
                        <Input
                          id={`price-${line.goodsReceiptLineId}`}
                          type="number"
                          step="any"
                          min="0"
                          value={line.unitPrice}
                          onChange={(e) =>
                            updateLine(line.goodsReceiptLineId, "unitPrice", e.target.value)
                          }
                        />
                      </div>
                    </li>
                  );
                })}
              </ul>
            </div>

            {/* Nepal TDS (Withholding Tax) Configuration & Financial Summary */}
            <div className="rounded-lg border border-border bg-card p-4 space-y-3">
              <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
                <div className="flex items-center gap-2">
                  <input
                    type="checkbox"
                    id="tds-applicable"
                    checked={isTdsApplicable}
                    onChange={(e) => setIsTdsApplicable(e.target.checked)}
                    className="size-4 rounded border-gray-300 text-primary focus:ring-primary"
                  />
                  <Label htmlFor="tds-applicable" className="text-sm font-semibold cursor-pointer">
                    Apply Nepal TDS (Withholding Tax) — Section 88/89
                  </Label>
                </div>
                {isTdsApplicable && (
                  <div className="flex items-center gap-2">
                    <Label htmlFor="tds-rate-select" className="text-xs text-muted-foreground whitespace-nowrap">
                      TDS Rate:
                    </Label>
                    <select
                      id="tds-rate-select"
                      value={tdsRate}
                      onChange={(e) => setTdsRate(e.target.value)}
                      className="h-8 rounded-md border border-input bg-background px-2 text-xs font-medium"
                    >
                      <option value="1.5">1.5% — Goods Supply / Contract (Sec 89)</option>
                      <option value="10.0">10.0% — Professional Services (Sec 88)</option>
                      <option value="15.0">15.0% — House Rent / Lease (Sec 88)</option>
                    </select>
                  </div>
                )}
              </div>

              {/* Financial Calculation Breakdown */}
              <div className="rounded-md border border-border/80 bg-muted/40 p-3 text-xs space-y-1.5 font-mono">
                <div className="flex justify-between text-muted-foreground">
                  <span>Subtotal:</span>
                  <span>{formatMoney(totals.subtotal, receipt?.currency ?? "NPR")}</span>
                </div>
                <div className="flex justify-between text-muted-foreground">
                  <span>VAT (13%):</span>
                  <span>+{formatMoney(totals.tax, receipt?.currency ?? "NPR")}</span>
                </div>
                <div className="flex justify-between text-muted-foreground font-semibold">
                  <span>Gross Invoice Total:</span>
                  <span>{formatMoney(totals.gross, receipt?.currency ?? "NPR")}</span>
                </div>
                {isTdsApplicable && (
                  <div className="flex justify-between text-amber-700 dark:text-amber-400 font-medium">
                    <span>Less TDS Withheld ({tdsRate}% on Subtotal):</span>
                    <span>-{formatMoney(totals.tds, receipt?.currency ?? "NPR")}</span>
                  </div>
                )}
                <div className="border-t border-border/80 pt-2 flex justify-between font-bold text-sm text-foreground font-mono">
                  <span>Net Payable to Supplier:</span>
                  <span className="text-primary">{formatMoney(totals.netPayable, receipt?.currency ?? "NPR")}</span>
                </div>
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="invoice-notes">
                Notes{" "}
                <span className="font-normal text-muted-foreground">(optional)</span>
              </Label>
              <Textarea
                id="invoice-notes"
                rows={2}
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                placeholder="Discrepancies, credit notes, payment terms…"
              />
            </div>
          </div>
        )}

        <DialogFooter>
          <Button
            type="button"
            variant="outline"
            onClick={() => onOpenChange(false)}
            disabled={isLoading}
          >
            Cancel
          </Button>
          <Button
            type="button"
            onClick={handleSubmit}
            disabled={!canSubmit}
          >
            {isLoading ? (
              <Loader2 className="mr-2 size-4 animate-spin" aria-hidden="true" />
            ) : (
              <Receipt className="mr-2 size-4" aria-hidden="true" />
            )}
            Record invoice
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
