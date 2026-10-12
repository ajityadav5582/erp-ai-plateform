"use client";

import { useEffect, useMemo, useState } from "react";
import { Loader2, Search } from "lucide-react";
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

interface ConvertToPurchaseOrderDialogProps {
  /** The approved requisition being turned into an order. */
  requisition: RequisitionListResponse | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  /** Called with the created draft so the caller can open it immediately. */
  onConverted?: (order: PurchaseOrder) => void;
}

/**
 * Turns an approved requisition into a draft purchase order.
 *
 * **Only the supplier is asked for.** Item, UOM, quantity, price, tax rate, currency,
 * branch and delivery location are all copied from the requisition server-side, and
 * that is the point: the approval authorised a specific set of lines at specific prices,
 * so letting the buyer retype them here would let the order quietly disagree with the
 * approval behind it. The dialog therefore shows what will be copied and asks for the
 * one thing a requisition genuinely cannot know - who will supply the goods.
 *
 * The supplier's code and name are sent as a pair because the server freezes them onto
 * the order at this moment. Reading them back from the catalogue later would let the
 * record change under a historical document if the supplier were ever renamed.
 */
export function ConvertToPurchaseOrderDialog({
  requisition,
  open,
  onOpenChange,
  onConverted,
}: ConvertToPurchaseOrderDialogProps) {
  /**
   * The chosen supplier is stored as the record itself, not as an id that has to be
   * looked back up in the visible list. Searching after choosing would otherwise drop
   * the selection out of the filtered list, silently block the convert, and leave the
   * buyer staring at a full dropdown with no way to tell why the button stopped working.
   */
  const [selectedSupplier, setSelectedSupplier] =
    useState<SupplierListResponse | null>(null);
  const [expectedDeliveryDate, setExpectedDeliveryDate] = useState("");
  const [notes, setNotes] = useState("");
  const [supplierSearch, setSupplierSearch] = useState("");

  /**
   * How many suppliers to pull for the picker.
   *
   * Large on purpose: the endpoint takes a `Pageable` and nothing else, so there is no
   * server-side search to narrow this down with. A big first page is what keeps the
   * common case - a buyer typing a few letters - answerable from data already loaded.
   */
  const SUPPLIER_PICKER_SIZE = 200;

  // Suppliers are fetched only while the dialog is open, and with only the parameters the
  // endpoint actually accepts.
  //
  // `listSuppliers(Pageable)` binds just `page`, `size` and `sort`. An earlier version of
  // this dialog also sent `search` and `status`; Spring's pageable resolver drops unknown
  // parameters silently, so the request succeeded while the search box did nothing and
  // deactivated suppliers were offered as valid choices for a new order. Sorting by name
  // gives the picker a stable alphabetical order regardless of insertion order.
  const {
    data: suppliersPage,
    isFetching: isLoadingSuppliers,
  } = useGetSuppliersQuery(
    { page: 0, size: SUPPLIER_PICKER_SIZE, sort: "name,asc" },
    { skip: !open }
  );

  /**
   * Active suppliers matching the search box, filtered here rather than on the server.
   *
   * Both halves matter. Deactivated suppliers are excluded because an order raised
   * against one would commit spend to a supplier the business has already switched off.
   */
  const suppliers = useMemo(() => {
    const all = suppliersPage?.content ?? [];
    const term = supplierSearch.trim().toLowerCase();
    return all.filter((supplier) => {
      if (!supplier.isActive) return false;
      if (!term) return true;
      return (
        supplier.name.toLowerCase().includes(term) ||
        supplier.code.toLowerCase().includes(term)
      );
    });
  }, [suppliersPage, supplierSearch]);

  /**
   * Keeps a chosen supplier visible even when the current search excludes it, so the
   * trigger never renders blank for a selection that is still genuinely held.
   */
  const visibleSuppliers = useMemo(() => {
    if (!selectedSupplier) return suppliers;
    if (suppliers.some((s) => s.id === selectedSupplier.id)) return suppliers;
    return [selectedSupplier, ...suppliers];
  }, [suppliers, selectedSupplier]);

  /**
   * Whether the company has more suppliers than were loaded. Reported to the buyer
   * rather than hidden: a silently truncated picker looks exactly like a supplier that
   * does not exist, and "we couldn't show you everyone" is a very different answer from
   * "that supplier isn't on file".
   */
  const isTruncated =
    (suppliersPage?.totalElements ?? 0) > (suppliersPage?.content.length ?? 0);

  const [convert, { isLoading }] = useConvertToPurchaseOrderMutation();

  // Reset on open so a previous supplier is never silently carried onto a different
  // requisition. Two orders raised against the same wrong supplier is a real failure
  // mode, and "the dialog remembered" is not an acceptable cause.
  useEffect(() => {
    if (open) {
      setSelectedSupplier(null);
      setExpectedDeliveryDate("");
      setNotes("");
      setSupplierSearch("");
    }
  }, [open]);

  const handleConvert = async () => {
    if (!requisition) return;

    if (!selectedSupplier) {
      toast.error("Choose a supplier before creating the order");
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

      toast.success(`Purchase order ${order.poNumber} created`, {
        description: `Converted from requisition ${requisition.prNumber}. It is a draft until submitted for approval.`,
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
      <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>Raise purchase order</DialogTitle>
          <DialogDescription>
            Converting requisition {requisition?.prNumber} into a draft order. Item,
            quantity, price and tax come from the approved requisition - only the
            supplier is chosen here.
          </DialogDescription>
        </DialogHeader>

        {requisition && (
          <div className="rounded-md border border-border bg-muted/40 p-3 text-sm">
            <dl className="grid grid-cols-2 gap-x-4 gap-y-2 sm:grid-cols-4">
              <div>
                <dt className="text-xs text-muted-foreground">Requisition</dt>
                <dd className="font-medium text-foreground">
                  {requisition.prNumber}
                </dd>
              </div>
              <div>
                <dt className="text-xs text-muted-foreground">Approved value</dt>
                <dd className="font-medium text-foreground">
                  {formatMoney(
                    requisition.approvedTotalAmount,
                    requisition.currency
                  )}
                </dd>
              </div>
              <div>
                <dt className="text-xs text-muted-foreground">Needed by</dt>
                <dd className="font-medium text-foreground">
                  {requisition.requiredByDate ?? "—"}
                </dd>
              </div>
              <div>
                <dt className="text-xs text-muted-foreground">Branch</dt>
                <dd className="font-medium text-foreground">
                  {requisition.branchId ?? "—"}
                </dd>
              </div>
            </dl>
          </div>
        )}

        <div className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="po-supplier">Supplier</Label>
            {isLoadingSuppliers && !suppliers.length ? (
              <div className="flex items-center gap-2 text-sm text-muted-foreground">
                <Loader2 className="size-4 animate-spin" aria-hidden="true" />
                Loading suppliers…
              </div>
            ) : (
              <>
                <Input
                  id="po-supplier-search"
                  placeholder="Search suppliers…"
                  value={supplierSearch}
                  onChange={(e) => setSupplierSearch(e.target.value)}
                  className="mb-2"
                />
                <Select
                  value={selectedSupplier ? String(selectedSupplier.id) : ""}
                  onValueChange={(value) =>
                    setSelectedSupplier(
                      visibleSuppliers.find((s) => String(s.id) === value) ?? null
                    )
                  }
                >
                  <SelectTrigger id="po-supplier">
                    <SelectValue placeholder="Select a supplier" />
                  </SelectTrigger>
                  <SelectContent>
                    {visibleSuppliers.length === 0 ? (
                      <div className="flex items-center gap-2 px-2 py-3 text-sm text-muted-foreground">
                        <Search className="size-4" aria-hidden="true" />
                        No active suppliers match that search
                      </div>
                    ) : (
                      visibleSuppliers.map((supplier) => (
                        <SelectItem
                          key={supplier.id}
                          value={String(supplier.id)}
                        >
                          {supplier.code} — {supplier.name}
                        </SelectItem>
                      ))
                    )}
                  </SelectContent>
                </Select>
              </>
            )}
            <p className="text-xs text-muted-foreground">
              A requisition names no supplier, so this is the one field that cannot be
              carried over. The code and name are frozen onto the order as a snapshot.
            </p>
            {isTruncated && (
              <p className="text-xs text-amber-600 dark:text-amber-400">
                Showing the first {suppliersPage?.content.length ?? 0} of{" "}
                {suppliersPage?.totalElements ?? 0} suppliers. Narrow the search to
                reach the rest, or add the supplier on the Suppliers screen.
              </p>
            )}
          </div>

          <div className="space-y-2">
            <Label htmlFor="po-expected-date">
              Expected delivery date{" "}
              <span className="font-normal text-muted-foreground">(optional)</span>
            </Label>
            <Input
              id="po-expected-date"
              type="date"
              value={expectedDeliveryDate}
              onChange={(e) => setExpectedDeliveryDate(e.target.value)}
            />
            <p className="text-xs text-muted-foreground">
              Leave blank to fall back to the requisition&apos;s needed-by date.
            </p>
          </div>

          <div className="space-y-2">
            <Label htmlFor="po-notes">
              Notes{" "}
              <span className="font-normal text-muted-foreground">(optional)</span>
            </Label>
            <Textarea
              id="po-notes"
              rows={3}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder="Delivery instructions, contact details, terms…"
            />
          </div>
        </div>

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
            onClick={handleConvert}
            disabled={isLoading || !selectedSupplier}
          >
            {isLoading && (
              <Loader2 className="mr-2 size-4 animate-spin" aria-hidden="true" />
            )}
            Create draft order
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
