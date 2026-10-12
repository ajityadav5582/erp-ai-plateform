"use client";

import { useEffect, useMemo } from "react";
import { useForm, useFieldArray } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Loader2, Plus, Trash2, Copy, FileText } from "lucide-react";
import { toast } from "sonner";
import {
  useCreateRequisitionMutation,
  useUpdateRequisitionMutation,
  type Requisition,
} from "@/services/purchase-requisition.service";
import { useGetBranchesQuery } from "@/services/branch.service";
import { useGetDepartmentsQuery } from "@/services/department.service";
import {
  useGetWarehousesQuery,
  NON_RECEIVING_WAREHOUSE_TYPES,
} from "@/services/warehouse.service";
import { useGetActiveItemsQuery } from "@/services/item.service";
import { getApiErrorMessage } from "@/services/error-handler";
import { useSelectedCompany } from "@/config/company-context";
import {
  emptyLineValues,
  requisitionEditDefaults,
  requisitionFormDefaults,
  requisitionFormSchema,
  snapshotFromRequisition,
  toCreateRequisitionRequest,
  toUpdateRequisitionRequest,
  type RequisitionFormValues,
} from "@/components/requisitions/requisition-form";

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Form, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { NepaliDatePicker } from "@/components/ui/nepali-date-picker";
import { BsDateField } from "@/components/ui/bs-date-field";
import {
  formatBsLong,
  formatBsLongNp,
  formatAdLong,
  safeAdToBs,
  safeAdToBsISO,
  safeBsToAd,
  todayISO,
} from "@/lib/nepali-date";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";

interface CreateRequisitionDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  /** Pre-selected branch, used when opening from a branch-filtered list. */
  defaultBranchId?: number;
  /**
   * The requisition being edited, or `undefined` to raise a new one.
   *
   * Only DRAFT documents are editable; the page decides that from
   * the server's actionability report rather than re-deriving it here.
   */
  editing?: Requisition | null;
}

export function CreateRequisitionDialog({
  open,
  onOpenChange,
  defaultBranchId,
  editing,
}: CreateRequisitionDialogProps) {
  const [createRequisition, { isLoading: isCreating }] =
    useCreateRequisitionMutation();
  const [updateRequisition, { isLoading: isUpdating }] =
    useUpdateRequisitionMutation();
  const isLoading = isCreating || isUpdating;
  const isEditing = Boolean(editing);
  const selectedCompany = useSelectedCompany();

  // In edit mode the line descriptions come from the document's own frozen
  // snapshot, not the live catalogue: an item renamed after the requisition was
  // raised must not rewrite the history the document records.
  const documentSnapshot = useMemo(
    () => (editing ? snapshotFromRequisition(editing) : new Map()),
    [editing]
  );

  const form = useForm<RequisitionFormValues>({
    resolver: zodResolver(requisitionFormSchema),
    defaultValues: {
      ...requisitionFormDefaults,
      lines: [emptyLineValues()],
    },
  });

  // Active branches are the only ones that can raise demand, so the list is
  // filtered server-side rather than client-side.
  const { data: branchData } = useGetBranchesQuery(
    { page: 0, size: 100, status: "ACTIVE", sort: "branchName,asc" },
    { skip: !open }
  );
  const branches = useMemo(() => branchData?.content ?? [], [branchData]);

  // Departments are scoped to a branch, so the list is fetched with the selected
  // branch and re-fetched when it changes. A department from another branch is
  // meaningless on this document.
  const selectedBranchId = form.watch("branchId");
  const { data: departmentData } = useGetDepartmentsQuery(
    {
      page: 0,
      size: 100,
      status: "ACTIVE",
      sort: "departmentName,asc",
      branchId: selectedBranchId ? Number(selectedBranchId) : undefined,
    },
    { skip: !selectedBranchId }
  );
  const departments = useMemo(
    () => departmentData?.content ?? [],
    [departmentData]
  );

  // Receiving sites only. Quarantine and transit are somewhere goods pass
  // through, not somewhere a branch asks for stock to end up, so nominating one
  // here would be a demand for the wrong floor.
  const { data: warehouseData } = useGetWarehousesQuery(
    { page: 0, size: 100, sort: "name,asc" },
    { skip: !open }
  );
  const warehouses = useMemo(
    () =>
      (warehouseData?.content ?? []).filter(
        (w) =>
          w.isActive &&
          !(w.type && NON_RECEIVING_WAREHOUSE_TYPES.includes(w.type))
      ),
    [warehouseData]
  );

  const { data: itemData } = useGetActiveItemsQuery(
    { page: 0, size: 200, sort: "nameEn,asc" },
    { skip: !open }
  );
  const items = useMemo(() => itemData?.content ?? [], [itemData]);

  /**
   * The backend stores item code, name and UOM as a frozen snapshot rather than
   * resolving them live, so they must be sent with each line. Reading them from
   * the loaded catalogue here keeps that snapshot correct without the user
   * having to type it.
   */
  const itemSnapshot = useMemo(() => {
    const map = new Map<number, { code: string; name: string; uom: string }>();
    if (editing) {
      // Document snapshot first, so a line the catalogue no longer lists keeps
      // the description it was raised with instead of blanking out.
      documentSnapshot.forEach((value, key) => map.set(key, value));
    }
    items.forEach((item) => {
      if (!map.has(item.id)) {
        map.set(item.id, {
          code: item.sku,
          name: item.nameEn,
          uom: item.uomCode ?? "PCS",
        });
      }
    });
    return map;
  }, [items, editing, documentSnapshot]);

  /**
   * The backend freezes the warehouse code and name onto the document, so both
   * have to travel with the id. Watched rather than read through
   * `getValues` inside the submit handler so the memo recomputes when the picker
   * changes, and seeded from the document first so a warehouse that has since
   * been deactivated or paged off the list keeps the description it was raised
   * with - the same reasoning as `itemSnapshot` above.
   */
  const selectedWarehouseId = form.watch("warehouseId");
  const warehouseSnapshot = useMemo(() => {
    if (editing?.warehouseId && String(editing.warehouseId) === selectedWarehouseId) {
      return { code: editing.warehouseCode ?? "", name: editing.warehouseName ?? "" };
    }
    const match = warehouses.find((w) => String(w.id) === selectedWarehouseId);
    return match ? { code: match.code, name: match.name } : null;
  }, [warehouses, selectedWarehouseId, editing]);

  const { fields, append, remove } = useFieldArray({
    control: form.control,
    name: "lines",
  });

  // Seed on every open: the create form starts blank, the edit form starts from
  // the document. Keyed on `open` rather than on close, so a dialog reopened with
  // a different requisition cannot show the previous one's contents.
  useEffect(() => {
    if (!open) return;
    if (editing) {
      form.reset(requisitionEditDefaults(editing));
    } else {
      form.reset({
        ...requisitionFormDefaults,
        lines: [emptyLineValues()],
      });
    }
    // `form` is stable; re-running on `editing` identity would discard in-flight
    // typing every time a background refetch produces a new object.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, editing?.id]);

  // Pre-fill the branch when opened from a branch-filtered list. Not in edit
  // mode: an existing document keeps the branch it was raised against.
  useEffect(() => {
    if (open && defaultBranchId && !editing) {
      form.setValue("branchId", String(defaultBranchId));
    }
  }, [open, defaultBranchId, editing, form]);

  const watchedLines = form.watch("lines");

  const requestDate = form.watch("requestDate");
  const requestDateNp = form.watch("requestDateNp");

  // The two fields are two views of one date, so editing either has to update the
  // other. Doing it in the change handler rather than a watcher avoids a render loop:
  // each setter only fires on a genuine user edit.
  const setRequestDateAd = (adISO: string) => {
    form.setValue("requestDate", adISO, { shouldValidate: true, shouldDirty: true });
    form.setValue("requestDateNp", safeAdToBsISO(adISO) ?? "", {
      shouldValidate: true,
      shouldDirty: true,
    });
  };

  const setRequestDateNp = (bsISO: string) => {
    form.setValue("requestDateNp", bsISO, { shouldValidate: true, shouldDirty: true });
    // A cleared or out-of-range BS value leaves the AD field alone rather than
    // blanking a valid date the user already entered.
    const adISO = safeBsToAd(bsISO);
    if (adISO) {
      form.setValue("requestDate", adISO, { shouldValidate: true, shouldDirty: true });
    }
  };

  // Display projections for the two fields.
  const requestBsLong = useMemo(
    () => (requestDate ? formatBsLongNp(requestDate) : null),
    [requestDate],
  );
  const requestAdLong = useMemo(
    () => (requestDate ? formatAdLong(requestDate) : null),
    [requestDate],
  );
  const requiredByDate = form.watch("requiredByDate");
  const requiredByBs = useMemo(
    () => safeAdToBs(requiredByDate),
    [requiredByDate],
  );
  const requiredByBsLong = useMemo(
    () => (requiredByDate ? formatBsLongNp(requiredByDate) : null),
    [requiredByDate],
  );
  const requiredByAdLong = useMemo(
    () => (requiredByDate ? formatAdLong(requiredByDate) : null),
    [requiredByDate],
  );
  // Sanity reference: proves the calendar is tracking the real current date.
  const todayBsLabel = useMemo(() => {
    try {
      return formatBsLong(todayISO());
    } catch {
      return null;
    }
  }, []);

  // Estimated total, recomputed on every keystroke. The server recomputes this
  // authoritatively; showing it here lets the requester sanity-check the request
  // before submitting.
  // Estimated total breakdown, recomputed on every keystroke.
  const estimatedBreakdown = useMemo(() => {
    return (watchedLines ?? []).reduce(
      (acc, line) => {
        const quantity = Number(line.quantity);
        const price = Number(line.estimatedUnitPrice);
        const taxRate = Number(line.taxRate);
        if (Number.isNaN(quantity) || Number.isNaN(price)) return acc;
        const net = quantity * price;
        const tax = Number.isNaN(taxRate) ? 0 : (net * taxRate) / 100;
        acc.net += net;
        acc.tax += tax;
        acc.total += net + tax;
        return acc;
      },
      { net: 0, tax: 0, total: 0 }
    );
  }, [watchedLines]);

  const onSubmit = async (values: RequisitionFormValues) => {
    if (!selectedCompany) {
      toast.error("No company selected", {
        description:
          "Select a company from the header before saving a requisition.",
      });
      return;
    }

    try {
      if (editing) {
        // A PUT is a full replacement, so lines the user deleted are gone and
        // lines they kept are rewritten. Nothing is merged on the server.
        const result = await updateRequisition({
          requisitionId: editing.id,
          data: toUpdateRequisitionRequest(values, itemSnapshot, warehouseSnapshot),
        }).unwrap();

        toast.success(`Requisition ${result.prNumber} updated`, {
          description: "Submit it for approval when the details are ready.",
        });
        onOpenChange(false);
        return;
      }

      // No companyId in the body: the backend scopes the create to the company
      // carried by the X-Company-Id header.
      const result = await createRequisition(
        toCreateRequisitionRequest(values, itemSnapshot, warehouseSnapshot)
      ).unwrap();

      toast.success(`Requisition ${result.prNumber} created`, {
        description: "Submit it for approval when the details are ready.",
      });
      form.reset({
        ...requisitionFormDefaults,
        lines: [emptyLineValues()],
      });
      onOpenChange(false);
    } catch (err) {
      toast.error(
        editing ? "Failed to update requisition" : "Failed to create requisition",
        { description: getApiErrorMessage(err) }
      );
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-5xl">
        <DialogHeader>
          <div className="flex items-center gap-2">
            <div className="flex size-9 items-center justify-center rounded-lg bg-primary/10 text-primary">
              <FileText className="size-5" />
            </div>
            <div>
              <DialogTitle>
                {editing
                  ? `Edit ${editing.prNumber}`
                  : "New Purchase Requisition"}
              </DialogTitle>
              <DialogDescription>
                {editing ? (
                  <>
                    Saving replaces the document as it stands. Lines you remove
                    are deleted, so keep what you still need.
                  </>
                ) : (
                  <>
                    Raise what a branch needs. No supplier is selected here; that
                    happens after the request is approved.
                  </>
                )}
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4 py-2">
          {/* --- Request details --- */}
            <h4 className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
              Request details
            </h4>
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
              <FormField
                control={form.control}
                name="branchId"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Branch</FormLabel>
                    <Select
                      value={field.value || undefined}
                      onValueChange={(val) => {
                        field.onChange(val);
                        // A branch change invalidates the department: a cost
                        // centre from another branch has no meaning here, so
                        // clear it rather than leave a dangling reference.
                        form.setValue("departmentId", "", {
                          shouldValidate: false,
                          shouldDirty: true,
                        });
                      }}
                      disabled={isLoading || isEditing}
                    >
                      <SelectTrigger>
                        <SelectValue placeholder="Select a branch" />
                      </SelectTrigger>
                      <SelectContent>
                        {branches.length === 0 ? (
                          <SelectItem value="none" disabled>
                            No active branches
                          </SelectItem>
                        ) : (
                          branches.map((branch) => (
                            <SelectItem
                              key={branch.id}
                              value={String(branch.id)}
                            >
                              {branch.branchName}
                            </SelectItem>
                          ))
                        )}
                      </SelectContent>
                    </Select>
                    <FormMessage />
                    {isEditing && (
                      <p className="text-xs text-muted-foreground">
                        The branch selects the approval policy, so it cannot be
                        changed once a requisition has been raised.
                      </p>
                    )}
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="departmentId"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Department</FormLabel>
                    <Select
                      value={field.value || undefined}
                      onValueChange={field.onChange}
                      disabled={isLoading || isEditing || !selectedBranchId}
                    >
                      <SelectTrigger>
                        <SelectValue placeholder="Select a department" />
                      </SelectTrigger>
                      <SelectContent>
                        {departments.length === 0 ? (
                          <SelectItem value="none" disabled>
                            {selectedBranchId
                              ? "No active departments in this branch"
                              : "Pick a branch first"}
                          </SelectItem>
                        ) : (
                          departments.map((dept) => (
                            <SelectItem
                              key={dept.id}
                              value={String(dept.id)}
                            >
                              {dept.departmentName}
                            </SelectItem>
                          ))
                        )}
                      </SelectContent>
                    </Select>
                    <FormMessage />
                    <p className="text-xs text-muted-foreground">
                      Optional cost centre. Scoped to the branch above, so the
                      list changes when the branch changes.
                    </p>
                  </FormItem>
                )}
              />

              {/* Two separate fields for one date: the Gregorian value the backend
                  stores, and the Bikram Sambat value it projects. Editing either
                  updates the other, so the pair can never drift apart. */}
              <FormField
                control={form.control}
                name="requestDate"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Request Date (AD)</FormLabel>
                    <Input
                      type="date"
                      value={field.value ?? ""}
                      onChange={(e) => setRequestDateAd(e.target.value)}
                      disabled={isLoading}
                    />
                    {requestDate ? (
                      <p className="text-xs text-muted-foreground">
                        {requestAdLong}
                      </p>
                    ) : (
                      <p className="text-xs text-muted-foreground">
                        Bikram Sambat calendar
                        {todayBsLabel ? ` — today is ${todayBsLabel}` : ""}
                      </p>
                    )}
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="requestDateNp"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Request Date (NP)</FormLabel>
                    <BsDateField
                      value={field.value ?? ""}
                      onChange={setRequestDateNp}
                      disabled={isLoading}
                    />
                    {requestDateNp ? (
                      <p className="font-nepali text-xs text-muted-foreground">
                        {requestBsLong}
                      </p>
                    ) : null}
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="requiredByDate"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Required By</FormLabel>
                    <NepaliDatePicker
                      value={field.value ?? ""}
                      onChange={field.onChange}
                      disabled={isLoading}
                      placeholder="ऐच्छिक"
                    />
                    {requiredByBs ? (
                      <dl className="mt-1 grid grid-cols-[auto_1fr] gap-x-2 gap-y-0.5 rounded-md border border-border bg-muted/50 px-2.5 py-2 text-xs">
                        <dt className="text-muted-foreground">Nepali (BS)</dt>
                        <dd className="font-nepali font-medium text-foreground">
                          {requiredByBsLong}
                        </dd>
                        <dt className="text-muted-foreground">Gregorian (AD)</dt>
                        <dd className="font-medium text-foreground">
                          {requiredByAdLong}
                        </dd>
                      </dl>
                    ) : null}
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="priority"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Priority</FormLabel>
                    <Select
                      value={field.value}
                      onValueChange={field.onChange}
                      disabled={isLoading}
                    >
                      <SelectTrigger>
                        <SelectValue placeholder="Select a priority" />
                      </SelectTrigger>
                      <SelectContent>
                        {(["LOW", "NORMAL", "HIGH", "URGENT"] as const).map(
                          (priority) => (
                            <SelectItem key={priority} value={priority}>
                              {priority.charAt(0) + priority.slice(1).toLowerCase()}
                            </SelectItem>
                          )
                        )}
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            <h4 className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
              Receiving details
            </h4>
            <FormField
              control={form.control}
              name="warehouseId"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>
                    Receiving Warehouse
                    <span className="ml-1 text-muted-foreground text-xs">
                      optional
                    </span>
                  </FormLabel>
                  <Select
                    value={field.value ?? ""}
                    onValueChange={field.onChange}
                    disabled={isLoading}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="No preference - decide at receipt" />
                    </SelectTrigger>
                    <SelectContent>
                      {warehouses.map((warehouse) => (
                        <SelectItem key={warehouse.id} value={String(warehouse.id)}>
                          {warehouse.name} ({warehouse.code})
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <p className="text-xs text-muted-foreground">
                    A nomination, not a commitment. If empty, the storekeeper
                    picks the warehouse when the goods actually arrive.
                  </p>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="justification"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>
                    Justification
                    {form.watch("priority") === "HIGH" ||
                    form.watch("priority") === "URGENT" ? (
                      <span className="ml-1 text-destructive">*</span>
                    ) : null}
                  </FormLabel>
                  <Input
                    placeholder="Why is this purchase needed?"
                    value={field.value ?? ""}
                    onChange={field.onChange}
                    disabled={isLoading}
                  />
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="notes"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Notes</FormLabel>
                  <Input
                    placeholder="Internal notes for the approver or storekeeper"
                    value={field.value ?? ""}
                    onChange={field.onChange}
                    disabled={isLoading}
                  />
                  <FormMessage />
                </FormItem>
              )}
            />

            {/* --- Lines --- */}
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <h3 className="text-sm font-semibold text-foreground">
                  Items
                  <span className="ml-1.5 font-normal text-muted-foreground">
                    ({fields.length})
                  </span>
                </h3>
                <div className="flex items-center gap-1.5">
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    className="gap-1.5"
                    onClick={() => append(emptyLineValues())}
                    disabled={isLoading || fields.length >= 50}
                  >
                    <Plus className="size-3.5" />
                    Add Line
                  </Button>
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    className="gap-1.5 text-muted-foreground"
                    onClick={() => {
                      if (fields.length === 0) return;
                      const last = fields[fields.length - 1]!;
                      append({
                        itemId: last.itemId,
                        quantity: last.quantity,
                        estimatedUnitPrice: last.estimatedUnitPrice,
                        taxRate: last.taxRate,
                        remarks: "",
                      });
                    }}
                    disabled={isLoading || fields.length >= 50}
                    title="Duplicate the last line"
                  >
                    <Copy className="size-3.5" />
                    Copy
                  </Button>
                </div>
              </div>

              <div className="overflow-hidden rounded-lg border border-border">
                <Table>
                  <TableHeader className="bg-muted/40">
                    <TableRow>
                      <TableHead className="w-10">#</TableHead>
                      <TableHead>Item</TableHead>
                      <TableHead className="w-28">Quantity</TableHead>
                      <TableHead className="w-32">Est. Unit Price</TableHead>
                      <TableHead className="w-20">Tax %</TableHead>
                      <TableHead className="w-32 text-right">Line Total</TableHead>
                      <TableHead className="w-10" />
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {fields.map((field, index) => {
                      const lineQty = Number(form.watch(`lines.${index}.quantity`) || 0);
                      const linePrice = Number(form.watch(`lines.${index}.estimatedUnitPrice`) || 0);
                      const lineTax = Number(form.watch(`lines.${index}.taxRate`) || 0);
                      const net = lineQty * linePrice;
                      const tax = (net * lineTax) / 100;
                      const lineTotal = net + tax;

                      return (
                        <TableRow key={field.id}>
                          <TableCell className="text-sm text-muted-foreground">
                            {index + 1}
                          </TableCell>

                          <TableCell>
                            <FormField
                              control={form.control}
                              name={`lines.${index}.itemId`}
                              render={({ field: itemField }) => (
                                <FormItem>
                                  <Select
                                    value={itemField.value || undefined}
                                    onValueChange={(val) => {
                                      itemField.onChange(val);
                                      const matchedItem = items.find(
                                        (it) => String(it.id) === val
                                      );
                                      if (matchedItem) {
                                        // Auto-fill the unit price from the catalogue's
                                        // purchase price when one is recorded. The
                                        // requester can override it afterwards; the
                                        // point is to cut down on typing for the common
                                        // case where the estimate is the catalogue
                                        // price.
                                        if (matchedItem.purchasePrice) {
                                          form.setValue(
                                            `lines.${index}.estimatedUnitPrice`,
                                            String(matchedItem.purchasePrice)
                                          );
                                        }
                                        // Auto-fill the tax rate from the item's own
                                        // VAT setting. A taxable item without an
                                        // explicit rate falls through to the default
                                        // rather than silently zeroing the column.
                                        if (
                                          matchedItem.taxabilityType === "TAXABLE" &&
                                          matchedItem.vatRate != null
                                        ) {
                                          form.setValue(
                                            `lines.${index}.taxRate`,
                                            String(matchedItem.vatRate)
                                          );
                                        }
                                      }
                                    }}
                                    disabled={isLoading}
                                  >
                                    <SelectTrigger>
                                      <SelectValue placeholder="Select an item" />
                                    </SelectTrigger>
                                    <SelectContent>
                                      {items.length === 0 ? (
                                        <SelectItem value="none" disabled>
                                          No active items
                                        </SelectItem>
                                      ) : (
                                        items.map((item) => (
                                          <SelectItem
                                            key={item.id}
                                            value={String(item.id)}
                                          >
                                            <div className="flex w-full flex-col">
                                              <span className="font-medium">
                                                {item.nameEn}
                                              </span>
                                              <span className="text-xs text-muted-foreground">
                                                {item.sku}
                                                {item.uomCode
                                                  ? ` · ${item.uomCode}`
                                                  : ""}
                                                {item.purchasePrice != null
                                                  ? ` · ${item.purchasePrice.toLocaleString(
                                                      undefined,
                                                      {
                                                        minimumFractionDigits: 2,
                                                      }
                                                    )}`
                                                  : ""}
                                              </span>
                                            </div>
                                          </SelectItem>
                                        ))
                                      )}
                                    </SelectContent>
                                  </Select>
                                  <FormMessage />
                                </FormItem>
                              )}
                            />
                          </TableCell>

                          <TableCell>
                            <FormField
                              control={form.control}
                              name={`lines.${index}.quantity`}
                              render={({ field: qtyField }) => (
                                <FormItem>
                                  <Input
                                    type="number"
                                    step="0.000001"
                                    min="0"
                                    placeholder="0"
                                    value={qtyField.value}
                                    onChange={qtyField.onChange}
                                    disabled={isLoading}
                                  />
                                  <FormMessage />
                                </FormItem>
                              )}
                            />
                          </TableCell>

                          <TableCell>
                            <FormField
                              control={form.control}
                              name={`lines.${index}.estimatedUnitPrice`}
                              render={({ field: priceField }) => (
                                <FormItem>
                                  <Input
                                    type="number"
                                    step="0.0001"
                                    min="0"
                                    placeholder="0.00"
                                    value={priceField.value}
                                    onChange={priceField.onChange}
                                    disabled={isLoading}
                                  />
                                  <FormMessage />
                                </FormItem>
                              )}
                            />
                          </TableCell>

                          <TableCell>
                            <FormField
                              control={form.control}
                              name={`lines.${index}.taxRate`}
                              render={({ field: taxField }) => (
                                <FormItem>
                                  <Input
                                    type="number"
                                    step="0.01"
                                    min="0"
                                    max="100"
                                    placeholder="13"
                                    value={taxField.value}
                                    onChange={taxField.onChange}
                                    disabled={isLoading}
                                  />
                                  <FormMessage />
                                </FormItem>
                              )}
                            />
                          </TableCell>

                          <TableCell className="text-right text-sm font-medium text-foreground">
                            {Number.isNaN(lineTotal) || lineTotal <= 0
                              ? "—"
                              : lineTotal.toLocaleString(undefined, {
                                  minimumFractionDigits: 2,
                                  maximumFractionDigits: 2,
                                })}
                          </TableCell>

                          <TableCell>
                            <Button
                              type="button"
                              variant="ghost"
                              size="icon"
                              className="size-8 text-destructive hover:text-destructive"
                              onClick={() => remove(index)}
                              disabled={isLoading || fields.length === 1}
                            >
                              <Trash2 className="size-4" />
                              <span className="sr-only">Remove line</span>
                            </Button>
                          </TableCell>
                        </TableRow>
                      );
                    })}
                  </TableBody>
                </Table>
              </div>
            </div>

            <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center rounded-lg border border-border bg-muted/30 p-4 gap-4">
              <div className="text-xs text-muted-foreground space-y-1">
                <p>
                  Currency:{" "}
                  <span className="font-semibold text-foreground">
                    {form.watch("currency") || "NPR"}
                  </span>
                </p>
                <p>Line totals include the tax rate entered per line.</p>
              </div>
              <div className="w-full sm:w-auto text-right space-y-1">
                <div className="flex justify-between sm:justify-end gap-6 text-xs text-muted-foreground">
                  <span>Subtotal (Net):</span>
                  <span className="font-medium text-foreground">
                    {estimatedBreakdown.net.toLocaleString(undefined, {
                      minimumFractionDigits: 2,
                      maximumFractionDigits: 2,
                    })}
                  </span>
                </div>
                <div className="flex justify-between sm:justify-end gap-6 text-xs text-muted-foreground">
                  <span>Est. Tax:</span>
                  <span className="font-medium text-foreground">
                    {estimatedBreakdown.tax.toLocaleString(undefined, {
                      minimumFractionDigits: 2,
                      maximumFractionDigits: 2,
                    })}
                  </span>
                </div>
                <div className="flex justify-between sm:justify-end gap-6 text-sm font-bold text-foreground border-t border-border pt-1">
                  <span>Estimated Total:</span>
                  <span className="text-primary text-base">
                    {form.watch("currency") || "NPR"}{" "}
                    {estimatedBreakdown.total.toLocaleString(undefined, {
                      minimumFractionDigits: 2,
                      maximumFractionDigits: 2,
                    })}
                  </span>
                </div>
              </div>
            </div>

            <DialogFooter className="pt-4">
              <Button
                type="button"
                variant="outline"
                onClick={() => onOpenChange(false)}
                disabled={isLoading}
              >
                Cancel
              </Button>
              <Button type="submit" disabled={isLoading || !selectedCompany}>
                {isLoading && <Loader2 className="mr-2 size-4 animate-spin" />}
                {isLoading
                  ? "Saving..."
                  : editing
                    ? "Save Changes"
                    : "Create Requisition"}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}

