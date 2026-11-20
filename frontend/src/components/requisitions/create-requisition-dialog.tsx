"use client";

import { useEffect, useMemo } from "react";
import { useForm, useFieldArray } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  Loader2,
  Plus,
  Trash2,
  Copy,
  FileText,
  Building2,
  Calendar,
  Warehouse,
  Package,
  AlertCircle,
} from "lucide-react";
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
import { useGetActiveItemsQuery, type ItemListResponse } from "@/services/item.service";
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
import { ItemCombobox } from "@/components/requisitions/item-combobox";

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { NepaliDatePicker } from "@/components/ui/nepali-date-picker";
import { BsDateField } from "@/components/ui/bs-date-field";
import {
  formatBsLongNp,
  formatAdLong,
  safeAdToBs,
  safeAdToBsISO,
  safeBsToAd,
  todayISO,
  formatBsLong,
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
import { Badge } from "@/components/ui/badge";

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
  // through, not somewhere a branch asks for stock to end up.
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

  // Fetch active items for inline combobox search (up to 500 items for instant client-side lookup)
  const { data: itemData } = useGetActiveItemsQuery(
    { page: 0, size: 500, sort: "nameEn,asc" },
    { skip: !open }
  );
  const items = useMemo(() => itemData?.content ?? [], [itemData]);

  /**
   * The backend stores item code, name and UOM as a frozen snapshot rather than
   * resolving them live, so they must be sent with each line.
   */
  const itemSnapshot = useMemo(() => {
    const map = new Map<number, { code: string; name: string; uom: string }>();
    if (editing) {
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
   * travel with the id.
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

  // Seed on every open
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
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, editing?.id]);

  // Pre-fill the branch when opened from a branch-filtered list
  useEffect(() => {
    if (open && defaultBranchId && !editing) {
      form.setValue("branchId", String(defaultBranchId));
    }
  }, [open, defaultBranchId, editing, form]);

  const watchedLines = form.watch("lines");
  const requestDate = form.watch("requestDate");
  const requestDateNp = form.watch("requestDateNp");
  const priority = form.watch("priority");

  // Sync AD and BS dates
  const setRequestDateAd = (adISO: string) => {
    form.setValue("requestDate", adISO, { shouldValidate: true, shouldDirty: true });
    form.setValue("requestDateNp", safeAdToBsISO(adISO) ?? "", {
      shouldValidate: true,
      shouldDirty: true,
    });
  };

  const setRequestDateNp = (bsISO: string) => {
    form.setValue("requestDateNp", bsISO, { shouldValidate: true, shouldDirty: true });
    const adISO = safeBsToAd(bsISO);
    if (adISO) {
      form.setValue("requestDate", adISO, { shouldValidate: true, shouldDirty: true });
    }
  };

  // Projections
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

  const todayBsLabel = useMemo(() => {
    try {
      return formatBsLong(todayISO());
    } catch {
      return null;
    }
  }, []);

  // Estimated breakdown
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

  // Handle inline item selection with intelligent defaults auto-fill
  const handleItemSelect = (
    index: number,
    field: { onChange: (val: string) => void },
    selectedItem: ItemListResponse
  ) => {
    field.onChange(String(selectedItem.id));

    // Auto-fill unit price from catalogue purchase price when available
    if (selectedItem.purchasePrice != null && Number(selectedItem.purchasePrice) > 0) {
      form.setValue(
        `lines.${index}.estimatedUnitPrice`,
        String(selectedItem.purchasePrice),
        { shouldValidate: true, shouldDirty: true }
      );
    }

    // Auto-fill tax rate from the item's VAT setting
    if (selectedItem.taxabilityType === "TAXABLE" && selectedItem.vatRate != null) {
      form.setValue(
        `lines.${index}.taxRate`,
        String(selectedItem.vatRate),
        { shouldValidate: true, shouldDirty: true }
      );
    } else if (
      selectedItem.taxabilityType === "EXEMPT" ||
      selectedItem.taxabilityType === "ZERO_RATED"
    ) {
      form.setValue(
        `lines.${index}.taxRate`,
        "0",
        { shouldValidate: true, shouldDirty: true }
      );
    }
  };

  const onSubmit = async (values: RequisitionFormValues) => {
    if (!selectedCompany) {
      toast.error("No company selected", {
        description: "Select a company from the header before saving a requisition.",
      });
      return;
    }

    try {
      if (editing) {
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
      <DialogContent className="flex max-h-[92vh] w-[calc(100vw-1.5rem)] flex-col gap-0 overflow-hidden rounded-2xl p-0 sm:max-w-4xl lg:max-w-5xl">
        {/* Header */}
        <DialogHeader className="shrink-0 border-b border-border/80 bg-muted/20 px-4 py-3.5 sm:px-6 sm:py-4">
          <div className="flex items-center gap-3">
            <div className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-primary/10 text-primary">
              <FileText className="size-5" />
            </div>
            <div className="min-w-0 flex-1">
              <div className="flex items-center gap-2">
                <DialogTitle className="text-base sm:text-lg">
                  {editing ? `Edit Requisition` : "New Purchase Requisition"}
                </DialogTitle>
                {editing ? (
                  <Badge variant="outline" className="font-mono text-xs font-semibold">
                    {editing.prNumber}
                  </Badge>
                ) : null}
              </div>
              <DialogDescription className="text-xs sm:text-sm">
                {editing
                  ? "Update lines, estimated prices, or delivery site before submitting."
                  : "Raise branch purchase demand. No supplier is needed until approval."}
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        {/* Scrollable Form Content */}
        <Form {...form}>
          <form
            id="purchase-requisition-form"
            onSubmit={form.handleSubmit(onSubmit)}
            className="flex-1 space-y-6 overflow-y-auto px-4 py-4 sm:px-6 sm:py-5"
          >
            {/* Section 1: Scope & Origin */}
            <div className="rounded-xl border border-border/70 bg-card/60 p-3.5 sm:p-4.5 space-y-3.5">
              <div className="flex items-center gap-2 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                <Building2 className="size-3.5 text-primary" />
                <span>Scope & Origin</span>
              </div>

              <div className="grid grid-cols-1 gap-3.5 sm:grid-cols-2">
                <FormField
                  control={form.control}
                  name="branchId"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel className="text-xs font-medium">
                        Branch <span className="text-destructive">*</span>
                      </FormLabel>
                      <Select
                        value={field.value || undefined}
                        onValueChange={(val) => {
                          field.onChange(val);
                          form.setValue("departmentId", "", {
                            shouldValidate: false,
                            shouldDirty: true,
                          });
                        }}
                        disabled={isLoading || isEditing}
                      >
                        <FormControl>
                          <SelectTrigger className="h-9">
                            <SelectValue placeholder="Select a branch" />
                          </SelectTrigger>
                        </FormControl>
                        <SelectContent>
                          {branches.length === 0 ? (
                            <SelectItem value="none" disabled>
                              No active branches
                            </SelectItem>
                          ) : (
                            branches.map((branch) => (
                              <SelectItem key={branch.id} value={String(branch.id)}>
                                {branch.branchName}
                              </SelectItem>
                            ))
                          )}
                        </SelectContent>
                      </Select>
                      <FormMessage />
                      {isEditing && (
                        <p className="text-[11px] text-muted-foreground">
                          Branch is locked after creation to preserve policy assignment.
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
                      <FormLabel className="text-xs font-medium">Department</FormLabel>
                      <Select
                        value={field.value || undefined}
                        onValueChange={field.onChange}
                        disabled={isLoading || isEditing || !selectedBranchId}
                      >
                        <FormControl>
                          <SelectTrigger className="h-9">
                            <SelectValue
                              placeholder={
                                selectedBranchId
                                  ? "Optional cost centre"
                                  : "Select branch first"
                              }
                            />
                          </SelectTrigger>
                        </FormControl>
                        <SelectContent>
                          {departments.length === 0 ? (
                            <SelectItem value="none" disabled>
                              {selectedBranchId
                                ? "No departments in this branch"
                                : "Pick a branch first"}
                            </SelectItem>
                          ) : (
                            departments.map((dept) => (
                              <SelectItem key={dept.id} value={String(dept.id)}>
                                {dept.departmentName}
                              </SelectItem>
                            ))
                          )}
                        </SelectContent>
                      </Select>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </div>
            </div>

            {/* Section 2: Schedule & Priority */}
            <div className="rounded-xl border border-border/70 bg-card/60 p-3.5 sm:p-4.5 space-y-3.5">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                  <Calendar className="size-3.5 text-primary" />
                  <span>Timeline & Urgency</span>
                </div>
                {todayBsLabel ? (
                  <span className="hidden text-[11px] text-muted-foreground sm:inline-block">
                    Today: {todayBsLabel}
                  </span>
                ) : null}
              </div>

              <div className="grid grid-cols-1 gap-3.5 sm:grid-cols-2 lg:grid-cols-4">
                <FormField
                  control={form.control}
                  name="requestDate"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel className="text-xs font-medium">Request Date (AD)</FormLabel>
                      <FormControl>
                        <Input
                          type="date"
                          value={field.value ?? ""}
                          onChange={(e) => setRequestDateAd(e.target.value)}
                          disabled={isLoading}
                          className="h-9 text-xs sm:text-sm"
                        />
                      </FormControl>
                      {requestDate ? (
                        <p className="truncate text-[11px] text-muted-foreground">
                          {requestAdLong}
                        </p>
                      ) : null}
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="requestDateNp"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel className="text-xs font-medium">Request Date (BS)</FormLabel>
                      <FormControl>
                        <BsDateField
                          value={field.value ?? ""}
                          onChange={setRequestDateNp}
                          disabled={isLoading}
                        />
                      </FormControl>
                      {requestDateNp ? (
                        <p className="truncate font-nepali text-[11px] text-muted-foreground">
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
                      <FormLabel className="text-xs font-medium">
                        Required By <span className="text-[10px] text-muted-foreground font-normal">(Optional)</span>
                      </FormLabel>
                      <FormControl>
                        <NepaliDatePicker
                          value={field.value ?? ""}
                          onChange={field.onChange}
                          disabled={isLoading}
                          placeholder="Select deadline"
                        />
                      </FormControl>
                      {requiredByBs ? (
                        <p className="truncate text-[11px] text-muted-foreground font-nepali">
                          {requiredByBsLong} ({requiredByAdLong})
                        </p>
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
                      <FormLabel className="text-xs font-medium">Priority Level</FormLabel>
                      <Select
                        value={field.value}
                        onValueChange={field.onChange}
                        disabled={isLoading}
                      >
                        <FormControl>
                          <SelectTrigger className="h-9">
                            <SelectValue placeholder="Select priority" />
                          </SelectTrigger>
                        </FormControl>
                        <SelectContent>
                          <SelectItem value="LOW">
                            <span className="flex items-center gap-2">
                              <span className="size-2 rounded-full bg-slate-400" />
                              Low
                            </span>
                          </SelectItem>
                          <SelectItem value="NORMAL">
                            <span className="flex items-center gap-2">
                              <span className="size-2 rounded-full bg-sky-500" />
                              Normal
                            </span>
                          </SelectItem>
                          <SelectItem value="HIGH">
                            <span className="flex items-center gap-2">
                              <span className="size-2 rounded-full bg-amber-500" />
                              High
                            </span>
                          </SelectItem>
                          <SelectItem value="URGENT">
                            <span className="flex items-center gap-2">
                              <span className="size-2 rounded-full bg-rose-500" />
                              Urgent
                            </span>
                          </SelectItem>
                        </SelectContent>
                      </Select>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </div>
            </div>

            {/* Section 3: Receiving & Notes */}
            <div className="rounded-xl border border-border/70 bg-card/60 p-3.5 sm:p-4.5 space-y-3.5">
              <div className="flex items-center gap-2 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                <Warehouse className="size-3.5 text-primary" />
                <span>Destination & Justification</span>
              </div>

              <div className="grid grid-cols-1 gap-3.5 sm:grid-cols-3">
                <FormField
                  control={form.control}
                  name="warehouseId"
                  render={({ field }) => (
                    <FormItem className="sm:col-span-1">
                      <FormLabel className="text-xs font-medium">
                        Nominated Warehouse{" "}
                        <span className="text-[10px] text-muted-foreground font-normal">(Optional)</span>
                      </FormLabel>
                      <Select
                        value={field.value ?? ""}
                        onValueChange={field.onChange}
                        disabled={isLoading}
                      >
                        <FormControl>
                          <SelectTrigger className="h-9">
                            <SelectValue placeholder="Decide upon delivery" />
                          </SelectTrigger>
                        </FormControl>
                        <SelectContent>
                          {warehouses.map((warehouse) => (
                            <SelectItem key={warehouse.id} value={String(warehouse.id)}>
                              {warehouse.name} ({warehouse.code})
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="justification"
                  render={({ field }) => (
                    <FormItem className="sm:col-span-2">
                      <FormLabel className="text-xs font-medium">
                        Justification
                        {priority === "HIGH" || priority === "URGENT" ? (
                          <span className="ml-1 text-destructive font-semibold">
                            * (Required for {priority})
                          </span>
                        ) : (
                          <span className="ml-1 text-[10px] text-muted-foreground font-normal">
                            (Optional)
                          </span>
                        )}
                      </FormLabel>
                      <FormControl>
                        <Input
                          placeholder="Reason for purchase (required for High/Urgent)"
                          value={field.value ?? ""}
                          onChange={field.onChange}
                          disabled={isLoading}
                          className="h-9 text-xs sm:text-sm"
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </div>

              <FormField
                control={form.control}
                name="notes"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel className="text-xs font-medium">
                      Internal Notes{" "}
                      <span className="text-[10px] text-muted-foreground font-normal">(Optional)</span>
                    </FormLabel>
                    <FormControl>
                      <Input
                        placeholder="Additional remarks for approvers or procurement officers"
                        value={field.value ?? ""}
                        onChange={field.onChange}
                        disabled={isLoading}
                        className="h-9 text-xs sm:text-sm"
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            {/* Section 4: Demand Items */}
            <div className="space-y-3">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <div className="flex items-center gap-2">
                  <div className="flex items-center gap-1.5 text-sm font-semibold text-foreground">
                    <Package className="size-4 text-primary" />
                    <span>Requested Items</span>
                  </div>
                  <Badge variant="secondary" className="font-mono text-xs font-semibold">
                    {fields.length}
                  </Badge>
                </div>

                <div className="flex items-center gap-2">
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    className="h-8 gap-1.5 text-xs"
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
                    disabled={isLoading || fields.length >= 50 || fields.length === 0}
                    title="Duplicate last item"
                  >
                    <Copy className="size-3.5" />
                    <span className="hidden sm:inline">Copy Last</span>
                  </Button>

                  <Button
                    type="button"
                    size="sm"
                    className="h-8 gap-1.5 text-xs"
                    onClick={() => append(emptyLineValues())}
                    disabled={isLoading || fields.length >= 50}
                  >
                    <Plus className="size-3.5" />
                    <span>Add Item</span>
                  </Button>
                </div>
              </div>

              {/* Tablet & Desktop View: Clean Table */}
              <div className="hidden rounded-xl border border-border/80 bg-card overflow-hidden md:block shadow-2xs">
                <Table>
                  <TableHeader className="bg-muted/40">
                    <TableRow className="hover:bg-transparent">
                      <TableHead className="w-10 text-center text-xs font-semibold">#</TableHead>
                      <TableHead className="min-w-[240px] text-xs font-semibold">
                        Item Description (Code & Name)
                      </TableHead>
                      <TableHead className="w-28 text-xs font-semibold">Quantity</TableHead>
                      <TableHead className="w-32 text-xs font-semibold">Est. Unit Price</TableHead>
                      <TableHead className="w-20 text-xs font-semibold">Tax %</TableHead>
                      <TableHead className="w-40 text-xs font-semibold">Remarks</TableHead>
                      <TableHead className="w-28 text-right text-xs font-semibold">Line Total</TableHead>
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
                        <TableRow key={field.id} className="hover:bg-muted/20">
                          <TableCell className="text-center font-mono text-xs text-muted-foreground">
                            {index + 1}
                          </TableCell>

                          <TableCell>
                            <FormField
                              control={form.control}
                              name={`lines.${index}.itemId`}
                              render={({ field: itemField }) => (
                                <FormItem>
                                  <FormControl>
                                    <ItemCombobox
                                      value={itemField.value}
                                      items={items}
                                      disabled={isLoading}
                                      fallbackSnapshot={itemSnapshot.get(Number(itemField.value))}
                                      onSelect={(selectedItem) =>
                                        handleItemSelect(index, itemField, selectedItem)
                                      }
                                      error={Boolean(
                                        form.formState.errors.lines?.[index]?.itemId
                                      )}
                                    />
                                  </FormControl>
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
                                  <FormControl>
                                    <Input
                                      type="number"
                                      step="0.000001"
                                      min="0"
                                      placeholder="0"
                                      value={qtyField.value}
                                      onChange={qtyField.onChange}
                                      disabled={isLoading}
                                      className="h-9 font-mono text-xs sm:text-sm"
                                    />
                                  </FormControl>
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
                                  <FormControl>
                                    <Input
                                      type="number"
                                      step="0.0001"
                                      min="0"
                                      placeholder="0.00"
                                      value={priceField.value}
                                      onChange={priceField.onChange}
                                      disabled={isLoading}
                                      className="h-9 font-mono text-xs sm:text-sm"
                                    />
                                  </FormControl>
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
                                  <FormControl>
                                    <Input
                                      type="number"
                                      step="0.01"
                                      min="0"
                                      max="100"
                                      placeholder="13"
                                      value={taxField.value}
                                      onChange={taxField.onChange}
                                      disabled={isLoading}
                                      className="h-9 font-mono text-xs sm:text-sm"
                                    />
                                  </FormControl>
                                  <FormMessage />
                                </FormItem>
                              )}
                            />
                          </TableCell>

                          <TableCell>
                            <FormField
                              control={form.control}
                              name={`lines.${index}.remarks`}
                              render={({ field: remarksField }) => (
                                <FormItem>
                                  <FormControl>
                                    <Input
                                      placeholder="Optional note…"
                                      value={remarksField.value ?? ""}
                                      onChange={remarksField.onChange}
                                      disabled={isLoading}
                                      className="h-9 text-xs"
                                    />
                                  </FormControl>
                                  <FormMessage />
                                </FormItem>
                              )}
                            />
                          </TableCell>

                          <TableCell className="text-right font-mono text-xs font-semibold text-foreground">
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
                              className="size-8 text-muted-foreground hover:bg-destructive/10 hover:text-destructive transition-colors"
                              onClick={() => remove(index)}
                              disabled={isLoading || fields.length === 1}
                              title="Delete row"
                            >
                              <Trash2 className="size-4" />
                            </Button>
                          </TableCell>
                        </TableRow>
                      );
                    })}
                  </TableBody>
                </Table>
              </div>

              {/* Mobile View: Touch-Friendly Responsive Cards */}
              <div className="space-y-3 md:hidden">
                {fields.map((field, index) => {
                  const lineQty = Number(form.watch(`lines.${index}.quantity`) || 0);
                  const linePrice = Number(form.watch(`lines.${index}.estimatedUnitPrice`) || 0);
                  const lineTax = Number(form.watch(`lines.${index}.taxRate`) || 0);
                  const net = lineQty * linePrice;
                  const tax = (net * lineTax) / 100;
                  const lineTotal = net + tax;
                  const selectedItemId = form.watch(`lines.${index}.itemId`);
                  const currentSnapshot = itemSnapshot.get(Number(selectedItemId));

                  return (
                    <div
                      key={field.id}
                      className="rounded-xl border border-border/80 bg-card p-3.5 space-y-3 shadow-2xs"
                    >
                      {/* Card Header */}
                      <div className="flex items-center justify-between border-b border-border/60 pb-2">
                        <div className="flex items-center gap-1.5">
                          <span className="flex size-5.5 items-center justify-center rounded-md bg-primary/10 font-mono text-xs font-bold text-primary">
                            {index + 1}
                          </span>
                          <span className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                            Item Line #{index + 1}
                          </span>
                        </div>

                        <div className="flex items-center gap-2">
                          <span className="font-mono text-xs font-bold text-foreground">
                            {Number.isNaN(lineTotal) || lineTotal <= 0
                              ? "—"
                              : `रू ${lineTotal.toLocaleString(undefined, {
                                  minimumFractionDigits: 2,
                                  maximumFractionDigits: 2,
                                })}`}
                          </span>

                          <Button
                            type="button"
                            variant="ghost"
                            size="icon"
                            className="size-7 text-muted-foreground hover:bg-destructive/10 hover:text-destructive"
                            onClick={() => remove(index)}
                            disabled={isLoading || fields.length === 1}
                          >
                            <Trash2 className="size-3.5" />
                          </Button>
                        </div>
                      </div>

                      {/* Item Selector */}
                      <FormField
                        control={form.control}
                        name={`lines.${index}.itemId`}
                        render={({ field: itemField }) => (
                          <FormItem className="space-y-1">
                            <FormLabel className="text-xs font-medium">
                              Select Item <span className="text-destructive">*</span>
                            </FormLabel>
                            <FormControl>
                              <ItemCombobox
                                value={itemField.value}
                                items={items}
                                disabled={isLoading}
                                fallbackSnapshot={itemSnapshot.get(Number(itemField.value))}
                                onSelect={(selectedItem) =>
                                  handleItemSelect(index, itemField, selectedItem)
                                }
                                error={Boolean(
                                  form.formState.errors.lines?.[index]?.itemId
                                )}
                              />
                            </FormControl>
                            <FormMessage />
                          </FormItem>
                        )}
                      />

                      {/* Quantity & Unit Price */}
                      <div className="grid grid-cols-2 gap-2.5">
                        <FormField
                          control={form.control}
                          name={`lines.${index}.quantity`}
                          render={({ field: qtyField }) => (
                            <FormItem className="space-y-1">
                              <FormLabel className="text-xs font-medium">
                                Quantity {currentSnapshot?.uom ? `(${currentSnapshot.uom})` : ""}
                              </FormLabel>
                              <FormControl>
                                <Input
                                  type="number"
                                  step="0.000001"
                                  min="0"
                                  placeholder="0"
                                  value={qtyField.value}
                                  onChange={qtyField.onChange}
                                  disabled={isLoading}
                                  className="h-9 font-mono text-xs"
                                />
                              </FormControl>
                              <FormMessage />
                            </FormItem>
                          )}
                        />

                        <FormField
                          control={form.control}
                          name={`lines.${index}.estimatedUnitPrice`}
                          render={({ field: priceField }) => (
                            <FormItem className="space-y-1">
                              <FormLabel className="text-xs font-medium">Est. Unit Price</FormLabel>
                              <FormControl>
                                <Input
                                  type="number"
                                  step="0.0001"
                                  min="0"
                                  placeholder="0.00"
                                  value={priceField.value}
                                  onChange={priceField.onChange}
                                  disabled={isLoading}
                                  className="h-9 font-mono text-xs"
                                />
                              </FormControl>
                              <FormMessage />
                            </FormItem>
                          )}
                        />
                      </div>

                      {/* Tax Rate & Remarks */}
                      <div className="grid grid-cols-3 gap-2.5">
                        <FormField
                          control={form.control}
                          name={`lines.${index}.taxRate`}
                          render={({ field: taxField }) => (
                            <FormItem className="col-span-1 space-y-1">
                              <FormLabel className="text-xs font-medium">Tax %</FormLabel>
                              <FormControl>
                                <Input
                                  type="number"
                                  step="0.01"
                                  min="0"
                                  max="100"
                                  placeholder="13"
                                  value={taxField.value}
                                  onChange={taxField.onChange}
                                  disabled={isLoading}
                                  className="h-9 font-mono text-xs"
                                />
                              </FormControl>
                              <FormMessage />
                            </FormItem>
                          )}
                        />

                        <FormField
                          control={form.control}
                          name={`lines.${index}.remarks`}
                          render={({ field: remarksField }) => (
                            <FormItem className="col-span-2 space-y-1">
                              <FormLabel className="text-xs font-medium">Remarks</FormLabel>
                              <FormControl>
                                <Input
                                  placeholder="Optional note…"
                                  value={remarksField.value ?? ""}
                                  onChange={remarksField.onChange}
                                  disabled={isLoading}
                                  className="h-9 text-xs"
                                />
                              </FormControl>
                              <FormMessage />
                            </FormItem>
                          )}
                        />
                      </div>
                    </div>
                  );
                })}
              </div>

              {form.formState.errors.lines?.root ? (
                <div className="flex items-center gap-2 rounded-lg border border-destructive/30 bg-destructive/10 px-3 py-2 text-xs text-destructive">
                  <AlertCircle className="size-4 shrink-0" />
                  <span>{form.formState.errors.lines.root.message}</span>
                </div>
              ) : null}
            </div>

            {/* Financial Summary Breakdown */}
            <div className="flex flex-col gap-4 rounded-xl border border-border/80 bg-muted/30 p-4 sm:flex-row sm:items-center sm:justify-between">
              <div className="space-y-1 text-xs text-muted-foreground">
                <div className="flex items-center gap-1.5">
                  <span className="font-semibold text-foreground">Currency:</span>
                  <Badge variant="outline" className="font-mono text-[11px] font-bold">
                    {form.watch("currency") || "NPR"}
                  </Badge>
                </div>
                <p>Line totals include item-specific VAT rates.</p>
              </div>

              <div className="space-y-1.5 text-right">
                <div className="flex justify-between gap-6 text-xs sm:justify-end">
                  <span className="text-muted-foreground">Subtotal (Net):</span>
                  <span className="font-mono font-medium text-foreground">
                    {estimatedBreakdown.net.toLocaleString(undefined, {
                      minimumFractionDigits: 2,
                      maximumFractionDigits: 2,
                    })}
                  </span>
                </div>

                <div className="flex justify-between gap-6 text-xs sm:justify-end">
                  <span className="text-muted-foreground">Est. Tax:</span>
                  <span className="font-mono font-medium text-foreground">
                    {estimatedBreakdown.tax.toLocaleString(undefined, {
                      minimumFractionDigits: 2,
                      maximumFractionDigits: 2,
                    })}
                  </span>
                </div>

                <div className="flex justify-between gap-6 border-t border-border/80 pt-1.5 text-sm font-bold sm:justify-end">
                  <span className="text-foreground">Estimated Total:</span>
                  <span className="font-mono text-base font-bold text-primary">
                    {form.watch("currency") || "NPR"}{" "}
                    {estimatedBreakdown.total.toLocaleString(undefined, {
                      minimumFractionDigits: 2,
                      maximumFractionDigits: 2,
                    })}
                  </span>
                </div>
              </div>
            </div>
          </form>
        </Form>

        {/* Pinned Responsive Dialog Footer */}
        <DialogFooter className="shrink-0 border-t border-border/80 bg-muted/20 px-4 py-3.5 sm:px-6 sm:py-4 flex-col-reverse sm:flex-row gap-2">
          <Button
            type="button"
            variant="outline"
            onClick={() => onOpenChange(false)}
            disabled={isLoading}
            className="w-full sm:w-auto h-9"
          >
            Cancel
          </Button>

          <Button
            type="submit"
            form="purchase-requisition-form"
            disabled={isLoading || !selectedCompany}
            className="w-full sm:w-auto h-9 font-semibold"
          >
            {isLoading && <Loader2 className="mr-2 size-4 animate-spin" />}
            {isLoading
              ? "Saving..."
              : editing
                ? "Save Changes"
                : "Create Requisition"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
