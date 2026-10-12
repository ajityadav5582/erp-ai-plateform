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

  const handleSubmit = async () => {
    if (!canSubmit) return;

    const payload: CreateInvoiceLineRequest[] = lines.map((line) => {
      const quantity = Number(line.quantity);
      const unitPrice = Number(line.unitPrice);
      const entry: CreateInvoiceLineRequest = { goodsReceiptLineId: line.goodsReceiptLineId };

      // Only send a number when it is actually a number. An empty or unparseable field
      // is omitted so the server falls back to the received figure, rather than being
      // sent as NaN and failing validation on a value the user never meant to set.
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
        lines: payload,
      }).unwrap();

      toast.success(`Supplier invoice ${invoice.invoiceNumber} recorded`, {
        description: `${formatMoney(
          invoice.totalAmount,
          invoice.currency
        )} payable.`,
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
      <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-3xl">
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
