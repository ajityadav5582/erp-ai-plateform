"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { Loader2, Truck } from "lucide-react";
import { toast } from "sonner";
import {
  useRecordGoodsReceiptMutation,
  type GoodsReceipt,
  type GoodsReceiptLineRequest,
} from "@/services/goods-receipt.service";
import type {
  PurchaseOrder,
  PurchaseOrderLine,
} from "@/services/purchase-order.service";
import {
  useGetWarehousesQuery,
  NON_RECEIVING_WAREHOUSE_TYPES,
  formatWarehouseType,
} from "@/services/warehouse.service";
import { getApiErrorMessage } from "@/services/error-handler";
import { formatMoney, formatQuantity } from "@/lib/procurement-format";

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

interface RecordGoodsReceiptDialogProps {
  /** The order the goods arrived against. */
  order: PurchaseOrder | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onRecorded?: (receipt: GoodsReceipt) => void;
}

/**
 * Records a delivery against a purchase order.
 *
 * **Quantities are asked for, and default to the full outstanding amount.** A
 * supplier honouring an order delivers what it can: a truckload now, the balance
 * next week. Booking the ordered quantity and correcting it afterwards is how stock
 * ends up overstated, so a short delivery is recorded as the short quantity it is
 * and the order stays open for the rest.
 *
 * Every line defaults to its full outstanding quantity and the whole outstanding set
 * is ticked by default, because a complete delivery is the common case and making a
 * storekeeper tick eleven boxes for it is a good way to get one wrong. Reducing a
 * line is the deliberate act.
 *
 * Lines already fully received are shown but cannot be ticked, so the operator can
 * see the whole order and understand why a line is missing from the selection.
 *
 * **The warehouse is asked for here, not before.** This is the only screen in the
 * procurement chain that knows where the goods physically ended up, so it is the only
 * place a fact like "the nominated site was full and the driver was rerouted" can be
 * captured. The picker opens on the order's nomination and is only changed when
 * reality differs, and a per-line override exists for the consignment that was split
 * across two sites on one challan.
 */
export function RecordGoodsReceiptDialog({
  order,
  open,
  onOpenChange,
  onRecorded,
}: RecordGoodsReceiptDialogProps) {
  const [selectedLines, setSelectedLines] = useState<number[]>([]);
  // Quantity arriving, keyed by order line number and held as a string so a
  // half-typed "1" is not immediately reformatted to "1.000000" under the cursor.
  const [quantities, setQuantities] = useState<Record<number, string>>({});
  const [receiptDate, setReceiptDate] = useState("");
  const [deliveryChallanNo, setDeliveryChallanNo] = useState("");
  const [notes, setNotes] = useState("");
  // Empty string means "fall back to whatever the order nominated", which is the
  // common case. It is held as a string rather than a number so the select has a
  // valid empty value and so an untouched field is distinguishable from a choice.
  const [warehouseId, setWarehouseId] = useState("");
  // Per-line destination, keyed by order line number. Almost always empty: the
  // split consignment is rare enough that showing a control on every row would be
  // noise. Only rendered for lines that diverge.
  const [lineWarehouseIds, setLineWarehouseIds] = useState<Record<number, string>>(
    {}
  );

  const [record, { isLoading }] = useRecordGoodsReceiptMutation();

  // Every active warehouse is offered as a destination. The backend's only gate
  // on a receiving site is that it exists and is active in the company (see
  // WarehouseServiceAdapter#validateWarehouse) - it never rejects a transit,
  // quarantine or scrap site. Excluding those kinds here therefore enforced no
  // real rule; it just left the operator holding a delivery with an empty picker
  // whenever the company's only warehouse happened to be one of them, which is
  // exactly the "warehouse cannot be selected" dead end. The kind is still shown
  // against each option so the choice stays informed.
  const { data: warehouseData } = useGetWarehousesQuery(
    { page: 0, size: 100, sort: "name,asc" },
    { skip: !open }
  );
  const warehouses = useMemo(() => {
    const active = (warehouseData?.content ?? []).filter((w) => w.isActive);
    // Ensure the order's nominated warehouse is always selectable, even if it
    // happens to be inactive, so the default selection is never dropped.
    const nominatedId = order?.warehouseId;
    if (nominatedId != null && !active.some((w) => w.id === nominatedId)) {
      const nominated = (warehouseData?.content ?? []).find(
        (w) => w.id === nominatedId
      );
      if (nominated) active.unshift(nominated);
    }
    return active;
  }, [warehouseData, order?.warehouseId]);

  // Outstanding lines are the only ones that can be received. Deriving from the line
  // status rather than comparing quantity to `receivedQuantity` avoids a float
  // comparison at the UI boundary, and the status is already authoritative - the
  // backend derives it from the same numbers.
  const outstandingLines = useMemo(
    () =>
      (order?.lines ?? []).filter(
        (line) => line.status === "OPEN" || line.status === "PARTIALLY_RECEIVED"
      ),
    [order]
  );

  const receivedLines = useMemo(
    () =>
      (order?.lines ?? []).filter(
        (line) => line.status === "RECEIVED" || line.status === "CANCELLED"
      ),
    [order]
  );

  // What is still to be delivered on a line. Read from the quantities rather than
  // re-derived from the status, so it is correct for a line that is part way through
  // being delivered as well as one that has not been touched.
  const outstandingQuantity = useCallback(
    (line: PurchaseOrderLine) =>
      Math.max(0, Number(line.quantity) - Number(line.receivedQuantity ?? 0)),
    []
  );

  // Default to everything outstanding: a supplier delivering an order complete is the
  // common case, and making the storekeeper tick eleven boxes for a full delivery is a
  // good way to get one of them wrong.
  useEffect(() => {
    if (open) {
      setSelectedLines(outstandingLines.map((line) => line.lineNumber));
      setQuantities(
        Object.fromEntries(
          outstandingLines.map((line) => [
            line.lineNumber,
            String(outstandingQuantity(line)),
          ])
        )
      );
      setReceiptDate(new Date().toISOString().slice(0, 10));
      setDeliveryChallanNo("");
      setNotes("");
      // Seeded from the order, so a lorry that follows the order to its nominated
      // site needs no input at all and only a genuine diversion requires a change.
      setWarehouseId(order?.warehouseId ? String(order.warehouseId) : "");
      setLineWarehouseIds({});
    }
    // Re-running on every `outstandingLines` change would reset the operator's ticks
    // whenever the underlying query refetches underneath them.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, order?.id]);

  /**
   * Reads back what the operator keyed for one line, or null when it is not a
   * usable number. Returning null rather than NaN keeps the validation message
   * honest instead of rendering "NaN is not a quantity".
   */
  const quantityFor = (line: PurchaseOrderLine): number | null => {
    const raw = quantities[line.lineNumber];
    if (raw === undefined || raw.trim() === "") return null;
    const parsed = Number(raw);
    return Number.isFinite(parsed) ? parsed : null;
  };

  const toggleLine = (lineNumber: number) => {
    setSelectedLines((prev) =>
      prev.includes(lineNumber)
        ? prev.filter((n) => n !== lineNumber)
        : [...prev, lineNumber].sort((a, b) => a - b)
    );
  };

  const setQuantity = (lineNumber: number, value: string) => {
    setQuantities((prev) => ({ ...prev, [lineNumber]: value }));
  };

  const setLineWarehouse = (lineNumber: number, value: string) => {
    setLineWarehouseIds((prev) => ({ ...prev, [lineNumber]: value }));
  };

  /**
   * Whether any line has been sent somewhere other than the note's warehouse. When
   * none has, the header picker is left unshown on every row, because a control the
   * operator is never expected to touch is worse than no control at all.
   */
  const anyLineOverride = useMemo(
    () => Object.values(lineWarehouseIds).some((v) => v !== ""),
    [lineWarehouseIds]
  );

  /**
   * The value of what is actually about to be booked, not the value of the ticked
   * lines. A line ticked at 300 out of an ordered 500 is worth 300, and showing the
   * full 500 would have the storekeeper confirm a figure that is not the one being
   * posted.
   */
  const selectedValue = useMemo(() => {
    const sum = outstandingLines
      .filter((line) => selectedLines.includes(line.lineNumber))
      .reduce((total, line) => {
        const ordered = Number(line.quantity);
        const arriving = quantityFor(line) ?? outstandingQuantity(line);
        const ratio = ordered > 0 ? arriving / ordered : 0;
        return total + Number(line.lineAmount) * ratio;
      }, 0);
    return order ? formatMoney(sum, order.currency) : "—";
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [outstandingLines, selectedLines, quantities, order]);

  const handleRecord = async () => {
    if (!order) return;

    if (selectedLines.length === 0) {
      toast.error("Select at least one line to receive");
      return;
    }

    /**
     * A receipt with neither an explicit warehouse nor an order nomination has
     * nowhere to put the stock, and the server refuses it. Saying so here, naming
     * the field, beats a 422 that quotes an order number the operator has never
     * heard of.
     */
    const nominated =
      order.warehouseId != null || warehouseId !== "" || anyLineOverride;
    if (!nominated) {
      toast.error("Choose the warehouse the goods arrived at", {
        description:
          "This order did not nominate one, so the receipt has to say where the stock went.",
      });
      return;
    }

    // Checked here so the storekeeper is told which line is wrong, rather than
    // getting a 422 from the server that names a quantity and an order number but
    // not the box on screen that produced it.
    const arriving: GoodsReceiptLineRequest[] = [];
    for (const line of outstandingLines) {
      if (!selectedLines.includes(line.lineNumber)) continue;

      const outstanding = outstandingQuantity(line);
      const quantity = quantityFor(line);

      if (quantity === null || quantity <= 0) {
        toast.error(`Enter a quantity for line ${line.lineNumber}`);
        return;
      }
      if (quantity > outstanding) {
        toast.error(
          `Line ${line.lineNumber} has only ${outstanding} ${line.uomCode} outstanding`
        );
        return;
      }
      const lineWarehouseId = lineWarehouseIds[line.lineNumber];
      arriving.push({
        lineNumber: line.lineNumber,
        receivedQuantity: quantity,
        ...(lineWarehouseId ? { warehouseId: Number(lineWarehouseId) } : {}),
      });
    }

    /**
     * The code and name travel with the id because the note freezes them: a warehouse
     * renamed after this receipt must not rewrite what the printed document says it
     * received. Resolved from the loaded list so the user never types it.
     */
    const chosen = warehouseId
      ? warehouses.find((w) => String(w.id) === warehouseId)
      : null;

    try {
      const receipt = await record({
        purchaseOrderId: order.id,
        lines: arriving,
        ...(chosen
          ? {
              warehouseId: chosen.id,
              warehouseCode: chosen.code,
              warehouseName: chosen.name,
            }
          : {}),
        ...(receiptDate ? { receiptDate } : {}),
        ...(deliveryChallanNo.trim()
          ? { deliveryChallanNo: deliveryChallanNo.trim() }
          : {}),
        ...(notes.trim() ? { notes: notes.trim() } : {}),
      }).unwrap();

      toast.success(`Goods receipt ${receipt.grnNumber} recorded`, {
        description: `Posted against order ${order.poNumber}.`,
      });
      onRecorded?.(receipt);
      onOpenChange(false);
    } catch (err) {
      toast.error("Failed to record the goods receipt", {
        description: getApiErrorMessage(err),
      });
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>Record goods receipt</DialogTitle>
          <DialogDescription>
            {order
              ? `Tick the lines that arrived against order ${order.poNumber} and reduce the quantity where the supplier delivered short. Each ticked line is received in full for whatever is outstanding unless you lower it.`
              : "Select the purchase order this delivery belongs to."}
          </DialogDescription>
        </DialogHeader>

        {order && outstandingLines.length === 0 ? (
          <div className="rounded-md border border-border bg-muted/40 p-4 text-sm text-muted-foreground">
            Every line on {order.poNumber} has already been received. There is nothing
            left to post against this order.
          </div>
        ) : (
          <div className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="grn-warehouse">
                Receiving warehouse{" "}
                <span className="font-normal text-muted-foreground">
                  {order?.warehouseName
                    ? `— nominated ${order.warehouseName} (${order.warehouseCode})`
                    : "— this order nominated none"}
                </span>
              </Label>
              <Select value={warehouseId} onValueChange={setWarehouseId}>
                <SelectTrigger id="grn-warehouse">
                  <SelectValue
                    placeholder={
                      order?.warehouseName
                        ? "Follow the order's nominated warehouse"
                        : "Where did the goods land?"
                    }
                  />
                </SelectTrigger>
                <SelectContent>
                  {warehouses.map((warehouse) => (
                    <SelectItem key={warehouse.id} value={String(warehouse.id)}>
                      {warehouse.name} ({warehouse.code})
                      {warehouse.type &&
                        NON_RECEIVING_WAREHOUSE_TYPES.includes(warehouse.type) && (
                          <span className="text-muted-foreground">
                            {" · "}
                            {formatWarehouseType(warehouse.type)}
                          </span>
                        )}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <p className="text-xs text-muted-foreground">
                This is where the stock is recorded as arriving. Change it only if the
                goods went somewhere other than the order said they would.
              </p>
            </div>

            <div className="rounded-md border border-border">
              <div className="border-b border-border bg-muted/40 px-3 py-2 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                Outstanding lines
              </div>
              <ul className="divide-y divide-border">
                {outstandingLines.map((line) => {
                  const checked = selectedLines.includes(line.lineNumber);
                  const lineWarehouse = lineWarehouseIds[line.lineNumber] ?? "";
                  return (
                    <li
                      key={line.id}
                      className="flex items-start gap-3 px-3 py-2.5"
                    >
                      <Checkbox
                        checked={checked}
                        onCheckedChange={() => toggleLine(line.lineNumber)}
                        aria-label={`Receive line ${line.lineNumber}, ${line.itemName}`}
                        className="mt-0.5"
                      />
                      <div className="min-w-0 flex-1">
                        <p className="text-sm font-medium text-foreground">
                          {line.lineNumber}. {line.itemName}
                        </p>
                        <p className="text-xs text-muted-foreground">
                          {line.itemCode} ·{" "}
                          {formatQuantity(outstandingQuantity(line))}{" "}
                          {line.uomCode} outstanding
                          {Number(line.receivedQuantity) > 0 && (
                            <> · {formatQuantity(line.receivedQuantity)} already in</>
                          )}
                        </p>
                        {(anyLineOverride || lineWarehouse !== "") && checked && (
                          <div className="mt-1.5">
                            <Label
                              htmlFor={`grn-line-warehouse-${line.lineNumber}`}
                              className="text-xs text-muted-foreground"
                            >
                              Different warehouse for this line
                            </Label>
                            <Select
                              value={lineWarehouse}
                              onValueChange={(v) =>
                                setLineWarehouse(line.lineNumber, v)
                              }
                            >
                              <SelectTrigger
                                id={`grn-line-warehouse-${line.lineNumber}`}
                                className="mt-0.5 h-8"
                              >
                                <SelectValue placeholder="Same as the note" />
                              </SelectTrigger>
                              <SelectContent>
                                {warehouses.map((warehouse) => (
                                  <SelectItem
                                    key={warehouse.id}
                                    value={String(warehouse.id)}
                                  >
                                    {warehouse.name} ({warehouse.code})
                                    {warehouse.type &&
                                      NON_RECEIVING_WAREHOUSE_TYPES.includes(
                                        warehouse.type
                                      ) && (
                                        <span className="text-muted-foreground">
                                          {" · "}
                                          {formatWarehouseType(warehouse.type)}
                                        </span>
                                      )}
                                  </SelectItem>
                                ))}
                              </SelectContent>
                            </Select>
                          </div>
                        )}
                      </div>
                      <div className="flex shrink-0 items-center gap-2">
                        <Label
                          htmlFor={`grn-qty-${line.lineNumber}`}
                          className="sr-only"
                        >
                          Quantity arriving on line {line.lineNumber}
                        </Label>
                        <Input
                          id={`grn-qty-${line.lineNumber}`}
                          type="number"
                          min={0}
                          max={outstandingQuantity(line)}
                          step="any"
                          inputMode="decimal"
                          disabled={!checked}
                          value={quantities[line.lineNumber] ?? ""}
                          onChange={(e) => setQuantity(line.lineNumber, e.target.value)}
                          className="h-8 w-24 text-right"
                          aria-label={`Quantity arriving on line ${line.lineNumber}, ${line.itemName}`}
                        />
                        <div className="w-24 text-right text-xs">
                          <p className="text-muted-foreground">
                            of {formatQuantity(line.quantity)}
                          </p>
                          <p className="font-medium text-foreground">
                            {formatMoney(line.lineAmount, order?.currency)}
                          </p>
                        </div>
                      </div>
                    </li>
                  );
                })}
              </ul>
            </div>

            {receivedLines.length > 0 && (
              <details className="rounded-md border border-border">
                <summary className="cursor-pointer px-3 py-2 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                  {receivedLines.length} line
                  {receivedLines.length === 1 ? "" : "s"} already complete
                </summary>
                <ul className="divide-y divide-border border-t border-border">
                  {receivedLines.map((line) => (
                    <li
                      key={line.id}
                      className="flex items-center justify-between gap-3 px-3 py-2 text-sm"
                    >
                      <span className="text-muted-foreground">
                        {line.lineNumber}. {line.itemName}
                      </span>
                      <span className="text-xs text-muted-foreground">
                        {formatQuantity(line.receivedQuantity)} {line.uomCode}{" "}
                        received
                      </span>
                    </li>
                  ))}
                </ul>
              </details>
            )}

            <div className="space-y-2">
              <Label htmlFor="grn-date">
                Receipt date{" "}
                <span className="font-normal text-muted-foreground">
                  (defaults to today)
                </span>
              </Label>
              <Input
                id="grn-date"
                type="date"
                value={receiptDate}
                onChange={(e) => setReceiptDate(e.target.value)}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="grn-challan">
                Delivery challan / gate pass{" "}
                <span className="font-normal text-muted-foreground">(optional)</span>
              </Label>
              <Input
                id="grn-challan"
                value={deliveryChallanNo}
                onChange={(e) => setDeliveryChallanNo(e.target.value)}
                placeholder="The supplier's own delivery reference"
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="grn-notes">
                Notes{" "}
                <span className="font-normal text-muted-foreground">(optional)</span>
              </Label>
              <Textarea
                id="grn-notes"
                rows={2}
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                placeholder="Condition on arrival, shortages, damage…"
              />
            </div>

            <div className="rounded-md border border-border bg-muted/40 px-3 py-2 text-sm">
              <span className="text-muted-foreground">
                Receiving {selectedLines.length} line
                {selectedLines.length === 1 ? "" : "s"} worth{" "}
              </span>
              <span className="font-medium text-foreground">{selectedValue}</span>
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
            onClick={handleRecord}
            disabled={isLoading || selectedLines.length === 0}
          >
            {isLoading ? (
              <Loader2 className="mr-2 size-4 animate-spin" aria-hidden="true" />
            ) : (
              <Truck className="mr-2 size-4" aria-hidden="true" />
            )}
            Post receipt
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
