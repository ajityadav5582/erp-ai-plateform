"use client";

import * as React from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  useCreateCustomerMutation,
  CUSTOMER_TYPES,
  CUSTOMER_TYPE_LABELS,
  type CustomerType,
  type CreateCustomerRequest,
} from "@/services/customer.service";

const NONE = "__none__";

export function CreateCustomerDialog({
  open,
  onOpenChange,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const [createCustomer, { isLoading }] = useCreateCustomerMutation();
  const [form, setForm] = React.useState<CreateCustomerRequest>({
    name: "",
    code: "",
    type: "RETAIL",
    vatRegistered: false,
    currency: "NPR",
  });
  const [error, setError] = React.useState<string | null>(null);

  React.useEffect(() => {
    if (open) {
      setForm({ name: "", code: "", type: "RETAIL", vatRegistered: false, currency: "NPR" });
      setError(null);
    }
  }, [open]);

  const submit = async () => {
    setError(null);
    if (!form.name.trim() || !form.code.trim()) {
      setError("Name and code are required.");
      return;
    }
    try {
      await createCustomer({
        ...form,
        panNumber: form.panNumber?.trim() ? form.panNumber.trim() : undefined,
      }).unwrap();
      onOpenChange(false);
    } catch (e: unknown) {
      const msg =
        (e as { data?: { message?: string } })?.data?.message ?? "Failed to create customer";
      setError(msg);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>New Customer</DialogTitle>
          <DialogDescription>Add a customer to sell to.</DialogDescription>
        </DialogHeader>

        <div className="grid gap-3 text-sm">
          <div className="grid gap-1">
            <Label>Name</Label>
            <Input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
          </div>
          <div className="grid gap-1">
            <Label>Code</Label>
            <Input value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value })} />
          </div>
          <div className="grid gap-1">
            <Label>Type</Label>
            <Select value={form.type} onValueChange={(v) => setForm({ ...form, type: v as CustomerType })}>
              <SelectTrigger><SelectValue /></SelectTrigger>
              <SelectContent>
                {CUSTOMER_TYPES.map((t) => (
                  <SelectItem key={t} value={t}>{CUSTOMER_TYPE_LABELS[t]}</SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="grid gap-1">
            <Label>PAN (9 digits, required if VAT-registered)</Label>
            <Input
              value={form.panNumber ?? ""}
              onChange={(e) => setForm({ ...form, panNumber: e.target.value })}
              placeholder="123456789"
            />
          </div>
          <label className="flex items-center gap-2">
            <input
              type="checkbox"
              checked={form.vatRegistered}
              onChange={(e) => setForm({ ...form, vatRegistered: e.target.checked })}
            />
            VAT registered
          </label>
          {error ? <p className="text-sm text-red-600">{error}</p> : null}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>Cancel</Button>
          <Button onClick={submit} disabled={isLoading}>{isLoading ? "Saving…" : "Create"}</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
