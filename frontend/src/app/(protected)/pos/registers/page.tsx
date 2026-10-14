"use client";

import * as React from "react";
import { Monitor, PauseCircle, Pencil, PlayCircle, Plus, RefreshCw } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
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
import { TableSkeletonRows } from "@/components/common/table-state";
import { toast } from "sonner";
import {
  useGetPosRegistersQuery,
  useCreatePosRegisterMutation,
  useUpdatePosRegisterMutation,
  useActivatePosRegisterMutation,
  useDeactivatePosRegisterMutation,
  type PosRegister,
} from "@/services/pos-register.service";
import { useGetActiveWarehousesQuery } from "@/services/warehouse.service";
import { useGetBranchesQuery, type BranchListResponse } from "@/services/branch.service";
import { getApiErrorMessage } from "@/services/error-handler";
import { PosRegisterStatusBadge } from "@/components/pos/pos-status-badge";

const NO_BRANCH = "none";

/**
 * POS register administration — mobile first.
 *
 * Cards on a phone, a table from tablet up. Creating a register requires the
 * warehouse it deducts stock from; activation is a separate control because
 * deactivating a register with an open session is refused by the domain. The
 * warehouse is fixed at creation, but the name, branch and notes stay editable.
 */
export default function PosRegistersPage() {
  const [page, setPage] = React.useState(0);
  const [isCreateOpen, setIsCreateOpen] = React.useState(false);
  const [editing, setEditing] = React.useState<PosRegister | null>(null);
  const { data, isLoading, isFetching, refetch } = useGetPosRegistersQuery({ page, size: 20 });
  const [createRegister, { isLoading: isCreating }] = useCreatePosRegisterMutation();
  const [updateRegister, { isLoading: isUpdating }] = useUpdatePosRegisterMutation();
  const [activateRegister] = useActivatePosRegisterMutation();
  const [deactivateRegister] = useDeactivatePosRegisterMutation();
  const { data: warehouses, isLoading: warehousesLoading } = useGetActiveWarehousesQuery();
  const { data: branches } = useGetBranchesQuery({ size: 100, sort: "branchName,asc" });

  const branchOptions: BranchListResponse[] = branches?.content ?? [];

  const [form, setForm] = React.useState({
    registerCode: "",
    registerName: "",
    warehouseId: "",
    warehouseCode: "",
    warehouseName: "",
    branchId: "",
    branchName: "",
    notes: "",
  });

  const [editForm, setEditForm] = React.useState({
    registerName: "",
    branchId: "",
    branchName: "",
    notes: "",
  });

  const resetForm = () =>
    setForm({
      registerCode: "",
      registerName: "",
      warehouseId: "",
      warehouseCode: "",
      warehouseName: "",
      branchId: "",
      branchName: "",
      notes: "",
    });

  const onWarehouseChange = (value: string) => {
    const warehouse = (warehouses ?? []).find((w) => String(w.id) === value);
    if (!warehouse) return;
    setForm((prev) => ({
      ...prev,
      warehouseId: String(warehouse.id),
      warehouseCode: warehouse.code,
      warehouseName: warehouse.name,
    }));
  };

  const onCreateBranchChange = (value: string) => {
    if (value === NO_BRANCH) {
      setForm((prev) => ({ ...prev, branchId: "", branchName: "" }));
      return;
    }
    const branch = branchOptions.find((b) => String(b.id) === value);
    setForm((prev) => ({ ...prev, branchId: value, branchName: branch?.branchName ?? "" }));
  };

  const onEditBranchChange = (value: string) => {
    if (value === NO_BRANCH) {
      setEditForm((prev) => ({ ...prev, branchId: "", branchName: "" }));
      return;
    }
    const branch = branchOptions.find((b) => String(b.id) === value);
    setEditForm((prev) => ({ ...prev, branchId: value, branchName: branch?.branchName ?? "" }));
  };

  const onSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    try {
      await createRegister({
        registerCode: form.registerCode.trim().toUpperCase(),
        registerName: form.registerName.trim(),
        warehouseId: Number(form.warehouseId),
        warehouseCode: form.warehouseCode.trim(),
        warehouseName: form.warehouseName.trim(),
        branchId: form.branchId && form.branchId !== NO_BRANCH ? Number(form.branchId) : undefined,
        branchName: form.branchName.trim() || undefined,
        notes: form.notes.trim() || undefined,
      }).unwrap();
      toast.success("Register created.");
      setIsCreateOpen(false);
      resetForm();
    } catch (error) {
      toast.error(getApiErrorMessage(error) || "Could not create the register.");
    }
  };

  const openEdit = (register: PosRegister) => {
    setEditing(register);
    setEditForm({
      registerName: register.registerName,
      branchId: register.branchId != null ? String(register.branchId) : NO_BRANCH,
      branchName: register.branchName ?? "",
      notes: register.notes ?? "",
    });
  };

  const onSubmitEdit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!editing) return;
    try {
      await updateRegister({
        id: editing.id,
        body: {
          registerName: editForm.registerName.trim(),
          branchId: editForm.branchId && editForm.branchId !== NO_BRANCH ? Number(editForm.branchId) : null,
          branchName: editForm.branchName.trim() || null,
          notes: editForm.notes.trim() || null,
        },
      }).unwrap();
      toast.success("Register updated.");
      setEditing(null);
    } catch (error) {
      toast.error(getApiErrorMessage(error) || "Could not update the register.");
    }
  };

  const toggleActive = async (register: { id: number; status: string; registerName: string }) => {
    try {
      if (register.status === "ACTIVE") {
        await deactivateRegister(register.id).unwrap();
        toast.success(`${register.registerName} deactivated.`);
      } else {
        await activateRegister(register.id).unwrap();
        toast.success(`${register.registerName} activated.`);
      }
    } catch (error) {
      toast.error(getApiErrorMessage(error) || "Could not change the register status.");
    }
  };

  const registers = data?.content ?? [];

  return (
    <div className="space-y-4 p-4">
      <header className="flex flex-wrap items-center justify-between gap-2">
        <div>
          <h1 className="text-xl font-semibold">POS Registers</h1>
          <p className="text-sm text-muted-foreground">
            Terminals and the warehouses they deduct stock from.
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" size="icon" onClick={() => void refetch()} disabled={isFetching}>
            <RefreshCw className={isFetching ? "h-4 w-4 animate-spin" : "h-4 w-4"} />
          </Button>
          <Button onClick={() => setIsCreateOpen(true)}>
            <Plus className="mr-1 h-4 w-4" />
            New register
          </Button>
        </div>
      </header>

      {isLoading ? (
        <TableSkeletonRows columns={5} rows={5} />
      ) : registers.length === 0 ? (
        <div className="flex flex-col items-center gap-2 rounded-lg border border-dashed py-10">
          <Monitor className="h-8 w-8 text-muted-foreground" />
          <p className="text-sm text-muted-foreground">No registers yet.</p>
        </div>
      ) : (
        <>
          {/* Mobile: cards */}
          <ul className="space-y-2 md:hidden">
            {registers.map((register) => (
              <li key={register.id} className="rounded-lg border p-3">
                <div className="flex items-start justify-between gap-2">
                  <div>
                    <p className="font-medium">{register.registerName}</p>
                    <p className="text-xs text-muted-foreground">{register.registerCode}</p>
                  </div>
                  <PosRegisterStatusBadge status={register.status} />
                </div>
                <p className="mt-1 text-xs text-muted-foreground">
                  Warehouse: {register.warehouseName} ({register.warehouseCode})
                </p>
                {register.branchName && (
                  <p className="text-xs text-muted-foreground">Branch: {register.branchName}</p>
                )}
                <div className="mt-2 flex gap-2">
                  <Button variant="outline" size="sm" onClick={() => openEdit(register)}>
                    <Pencil className="mr-1 h-4 w-4" /> Edit
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => void toggleActive(register)}
                  >
                    {register.status === "ACTIVE" ? (
                      <>
                        <PauseCircle className="mr-1 h-4 w-4" /> Deactivate
                      </>
                    ) : (
                      <>
                        <PlayCircle className="mr-1 h-4 w-4" /> Activate
                      </>
                    )}
                  </Button>
                </div>
              </li>
            ))}
          </ul>

          {/* Tablet and up: table */}
          <div className="hidden rounded-lg border md:block">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Code</TableHead>
                  <TableHead>Name</TableHead>
                  <TableHead>Warehouse</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {registers.map((register) => (
                  <TableRow key={register.id}>
                    <TableCell className="font-mono text-xs">{register.registerCode}</TableCell>
                    <TableCell>
                      {register.registerName}
                      {register.branchName && (
                        <span className="block text-xs text-muted-foreground">
                          {register.branchName}
                        </span>
                      )}
                    </TableCell>
                    <TableCell>
                      {register.warehouseName} ({register.warehouseCode})
                    </TableCell>
                    <TableCell>
                      <PosRegisterStatusBadge status={register.status} />
                    </TableCell>
                    <TableCell className="text-right">
                      <Button variant="ghost" size="sm" onClick={() => openEdit(register)}>
                        Edit
                      </Button>
                      <Button variant="ghost" size="sm" onClick={() => void toggleActive(register)}>
                        {register.status === "ACTIVE" ? "Deactivate" : "Activate"}
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        </>
      )}

      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between">
          <Button variant="outline" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
            Previous
          </Button>
          <span className="text-sm text-muted-foreground">
            Page {data.number + 1} of {data.totalPages}
          </span>
          <Button
            variant="outline"
            disabled={data.last}
            onClick={() => setPage((p) => p + 1)}
          >
            Next
          </Button>
        </div>
      )}

      <Dialog open={isCreateOpen} onOpenChange={setIsCreateOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>New register</DialogTitle>
          </DialogHeader>
          <form onSubmit={onSubmit} className="space-y-3">
            <div className="space-y-1">
              <Label htmlFor="code">Register code</Label>
              <Input
                id="code"
                value={form.registerCode}
                onChange={(e) => setForm({ ...form, registerCode: e.target.value })}
                placeholder="REG-KTM-01"
                required
              />
            </div>
            <div className="space-y-1">
              <Label htmlFor="name">Register name</Label>
              <Input
                id="name"
                value={form.registerName}
                onChange={(e) => setForm({ ...form, registerName: e.target.value })}
                placeholder="Counter 1 – Ground Floor"
                required
              />
            </div>
            <div className="space-y-1">
              <Label htmlFor="warehouse">Warehouse</Label>
              <Select value={form.warehouseId} onValueChange={onWarehouseChange}>
                <SelectTrigger id="warehouse">
                  <SelectValue placeholder={warehousesLoading ? "Loading…" : "Select warehouse"} />
                </SelectTrigger>
                <SelectContent>
                  {warehousesLoading ? (
                    <div className="flex items-center gap-2 px-2 py-1.5 text-muted-foreground">
                      Loading…
                    </div>
                  ) : (warehouses ?? []).length === 0 ? (
                    <div className="px-2 py-1.5 text-muted-foreground">No warehouses configured</div>
                  ) : (
                    (warehouses ?? []).map((w) => (
                      <SelectItem key={w.id} value={String(w.id)}>
                        {w.name} ({w.code})
                      </SelectItem>
                    ))
                  )}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-1">
              <Label htmlFor="branch">Branch (optional)</Label>
              <Select value={form.branchId} onValueChange={onCreateBranchChange}>
                <SelectTrigger id="branch">
                  <SelectValue placeholder="Company-wide (no branch)" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={NO_BRANCH}>Company-wide (no branch)</SelectItem>
                  {branchOptions.map((b) => (
                    <SelectItem key={b.id} value={String(b.id)}>
                      {b.branchName} ({b.branchCode})
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-1">
              <Label htmlFor="notes">Notes (optional)</Label>
              <Textarea
                id="notes"
                value={form.notes}
                onChange={(e) => setForm({ ...form, notes: e.target.value })}
                placeholder="Anything worth remembering about this terminal"
              />
            </div>
            <Button type="submit" className="w-full" disabled={isCreating || !form.warehouseId}>
              {isCreating ? "Creating…" : "Create register"}
            </Button>
          </form>
        </DialogContent>
      </Dialog>

      <Dialog open={editing !== null} onOpenChange={(open) => !open && setEditing(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Edit register</DialogTitle>
          </DialogHeader>
          <form onSubmit={onSubmitEdit} className="space-y-3">
            <div className="space-y-1">
              <Label>Register code</Label>
              <Input value={editing?.registerCode ?? ""} disabled readOnly />
              <p className="text-xs text-muted-foreground">The code cannot be changed.</p>
            </div>
            <div className="space-y-1">
              <Label htmlFor="edit-name">Register name</Label>
              <Input
                id="edit-name"
                value={editForm.registerName}
                onChange={(e) => setEditForm({ ...editForm, registerName: e.target.value })}
                required
              />
            </div>
            <div className="space-y-1">
              <Label htmlFor="edit-warehouse">Warehouse</Label>
              <Input
                id="edit-warehouse"
                value={
                  editing ? `${editing.warehouseName} (${editing.warehouseCode})` : ""
                }
                disabled
                readOnly
              />
              <p className="text-xs text-muted-foreground">
                The warehouse is fixed at creation.
              </p>
            </div>
            <div className="space-y-1">
              <Label htmlFor="edit-branch">Branch (optional)</Label>
              <Select value={editForm.branchId} onValueChange={onEditBranchChange}>
                <SelectTrigger id="edit-branch">
                  <SelectValue placeholder="Company-wide (no branch)" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={NO_BRANCH}>Company-wide (no branch)</SelectItem>
                  {branchOptions.map((b) => (
                    <SelectItem key={b.id} value={String(b.id)}>
                      {b.branchName} ({b.branchCode})
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-1">
              <Label htmlFor="edit-notes">Notes (optional)</Label>
              <Textarea
                id="edit-notes"
                value={editForm.notes}
                onChange={(e) => setEditForm({ ...editForm, notes: e.target.value })}
              />
            </div>
            <Button type="submit" className="w-full" disabled={isUpdating}>
              {isUpdating ? "Saving…" : "Save changes"}
            </Button>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}
