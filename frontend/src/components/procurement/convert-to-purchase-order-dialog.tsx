"use client";

import { useEffect, useState } from "react";
import {
  Calendar,
  Building2,
  FileCheck2,
  Loader2,
  Receipt,
  ShoppingCart,
  Store,
} from "lucide-react";
import { toast } from "sonner";
import {
  useConvertToPurchaseOrderMutation,
  type PurchaseOrder,
} from "@/services/purchase-order.service";
import type { RequisitionListResponse } from "@/services/purchase-requisition.service";
import {
  useGetSuppliersQuery,
  type SupplierListResponse,
} from "@/services/supplier.service";
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
import { SupplierCombobox } from "@/components/procurement/supplier-combobox";

interface ConvertToPurchaseOrderDialogProps {
  /** The approved requisition being turned into an order. */
  requisition: RequisitionListResponse | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  /** Called with the created draft so the caller can open it immediately. */
  onConverted?: (order: PurchaseOrder) => void;
}

export function ConvertToPurchaseOrderDialog({
  requisition,
  open,
  onOpenChange,
  onConverted,
}: ConvertToPurchaseOrderDialogProps) {
  const [selectedSupplier, setSelectedSupplier] =
    useState<SupplierListResponse | null>(null);
  const [expectedDeliveryDate, setExpectedDeliveryDate] = useState("");
  const [notes, setNotes] = useState("");
  const [supplierError, setSupplierError] = useState(false);

  // Fetch active suppliers when dialog opens
  const { data: suppliersPage, isFetching: isLoadingSuppliers } =
    useGetSuppliersQuery(
      { page: 0, size: 250, sort: "name,asc" },
      { skip: !open }
    );

  const suppliers = suppliersPage?.content ?? [];

  const [convert, { isLoading }] = useConvertToPurchaseOrderMutation();

  // Reset state on open
  useEffect(() => {
    if (open) {
      setSelectedSupplier(null);
      setExpectedDeliveryDate(requisition?.requiredByDate ?? "");
      setNotes("");
      setSupplierError(false);
    }
  }, [open, requisition]);

  const handleConvert = async () => {
    if (!requisition) return;

    if (!selectedSupplier) {
      setSupplierError(true);
      toast.error("Please select a supplier to raise the purchase order");
      return;
    }

    try {
      const order = await convert({
        requisitionId: requisition.id,
        supplierId: selectedSupplier.id,
        supplierCode: selectedSupplier.code,
        supplierName: selectedSupplier.name,
        ...(expectedDeliveryDate ? { expectedDeliveryDate } : {}),
        ...(notes.trim() ? { notes: notes.trim() } : {}),
      }).unwrap();

      toast.success(`Purchase order ${order.poNumber} created successfully`, {
        description: `Draft order generated from requisition ${requisition.prNumber}.`,
      });
      onConverted?.(order);
      onOpenChange(false);
    } catch (err) {
      toast.error("Failed to create purchase order", {
        description: getApiErrorMessage(err),
      });
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[92vh] w-full overflow-y-auto sm:max-w-xl md:max-w-2xl">
        <DialogHeader className="gap-1 border-b border-border/60 pb-3">
          <div className="flex items-center gap-2.5">
            <div className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-primary">
              <ShoppingCart className="size-5" />
            </div>
            <div>
              <DialogTitle className="text-lg font-semibold tracking-tight">
                Raise Purchase Order
              </DialogTitle>
              <DialogDescription className="text-xs text-muted-foreground">
                Convert approved requisition{" "}
                <span className="font-mono font-medium text-foreground">
                  {requisition?.prNumber}
                </span>{" "}
                into a draft purchase order.
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        <div className="space-y-4 py-2">
          {requisition && (
            <div className="rounded-xl border border-border/80 bg-muted/30 p-3.5 sm:p-4">
              <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
                <div className="space-y-1">
                  <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
                    <FileCheck2 className="size-3.5 text-muted-foreground" />
                    <span>Requisition</span>
                  </div>
                  <p className="font-mono text-xs font-semibold text-foreground sm:text-sm">
                    {requisition.prNumber}
                  </p>
                </div>

                <div className="space-y-1">
                  <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
                    <Receipt className="size-3.5 text-muted-foreground" />
                    <span>Approved Value</span>
                  </div>
                  <p className="font-mono text-xs font-semibold text-emerald-600 dark:text-emerald-400 sm:text-sm">
                    {formatMoney(
                      requisition.approvedTotalAmount,
                      requisition.currency
                    )}
                  </p>
                </div>

                <div className="space-y-1">
                  <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
                    <Calendar className="size-3.5 text-muted-foreground" />
                    <span>Required By</span>
                  </div>
                  <p className="text-xs font-medium text-foreground sm:text-sm">
                    {requisition.requiredByDate ?? "—"}
                  </p>
                </div>

                <div className="space-y-1">
                  <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
                    <Store className="size-3.5 text-muted-foreground" />
                    <span>Branch ID</span>
                  </div>
                  <p className="text-xs font-medium text-foreground sm:text-sm">
                    {requisition.branchId ? `Branch #${requisition.branchId}` : "—"}
                  </p>
                </div>
              </div>
            </div>
          )}

          <div className="space-y-4">
            {/* Supplier Selector */}
            <div className="space-y-1.5">
              <div className="flex items-center justify-between">
                <Label htmlFor="supplier" className="text-xs font-medium sm:text-sm">
                  Supplier <span className="text-destructive">*</span>
                </Label>
                {selectedSupplier?.type ? (
                  <span className="text-[11px] text-muted-foreground">
                    Type: {selectedSupplier.type}
                  </span>
                ) : null}
              </div>

              <SupplierCombobox
                value={selectedSupplier?.id}
                suppliers={suppliers}
                isLoading={isLoadingSuppliers}
                disabled={isLoading}
                placeholder="Search supplier by name, code, PAN, or phone…"
                onSelect={(supplier) => {
                  setSelectedSupplier(supplier);
                  setSupplierError(false);
                }}
                error={supplierError}
              />

              {supplierError ? (
                <p className="text-xs text-destructive">
                  Please select an active supplier for this purchase order.
                </p>
              ) : selectedSupplier ? (
                <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-muted-foreground">
                  {selectedSupplier.taxId && (
                    <span className="flex items-center gap-1">
                      <Building2 className="size-3 text-muted-foreground" />
                      PAN/VAT: {selectedSupplier.taxId}
                    </span>
                  )}
                  {selectedSupplier.phone && (
                    <span>Tel: {selectedSupplier.phone}</span>
                  )}
                  {selectedSupplier.email && (
                    <span>Email: {selectedSupplier.email}</span>
                  )}
                </div>
              ) : null}
            </div>

            {/* Expected Delivery Date */}
            <div className="space-y-1.5">
              <Label
                htmlFor="po-expected-date"
                className="text-xs font-medium sm:text-sm"
              >
                Expected Delivery Date{" "}
                <span className="font-normal text-muted-foreground">
                  (optional)
                </span>
              </Label>
              <Input
                id="po-expected-date"
                type="date"
                value={expectedDeliveryDate}
                onChange={(e) => setExpectedDeliveryDate(e.target.value)}
                className="h-9 text-xs sm:text-sm"
              />
            </div>

            {/* Notes */}
            <div className="space-y-1.5">
              <Label
                htmlFor="po-notes"
                className="text-xs font-medium sm:text-sm"
              >
                Delivery & Order Notes{" "}
                <span className="font-normal text-muted-foreground">
                  (optional)
                </span>
              </Label>
              <Textarea
                id="po-notes"
                rows={3}
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                placeholder="Special delivery instructions, terms, contact person, or notes…"
                className="resize-none text-xs sm:text-sm"
              />
            </div>
          </div>
        </div>

        <DialogFooter className="flex flex-col-reverse gap-2 border-t border-border/60 pt-3 sm:flex-row sm:justify-end">
          <Button
            type="button"
            variant="outline"
            onClick={() => onOpenChange(false)}
            disabled={isLoading}
            className="w-full sm:w-auto"
          >
            Cancel
          </Button>
          <Button
            type="button"
            onClick={handleConvert}
            disabled={isLoading || !selectedSupplier}
            className="w-full gap-2 sm:w-auto"
          >
            {isLoading ? (
              <Loader2 className="size-4 animate-spin" aria-hidden="true" />
            ) : (
              <ShoppingCart className="size-4" aria-hidden="true" />
            )}
            Create Draft Order
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
