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
import { getApiErrorMessage } from "@/services/error-handler";
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
    email: "",
    phone: "",
    address: "",
    creditLimit: undefined,
    paymentTermsDays: undefined,
  });
  const [error, setError] = React.useState<string | null>(null);

  React.useEffect(() => {
    if (open) {
      setForm({
        name: "",
        code: "",
        type: "RETAIL",
        vatRegistered: false,
        currency: "NPR",
        email: "",
        phone: "",
        address: "",
        creditLimit: undefined,
        paymentTermsDays: undefined,
      });
      setError(null);
    }
  }, [open]);

  const submit = async () => {
    setError(null);
    if (!form.name.trim() || !form.code.trim()) {
      setError("Name and code are required.");
      return;
    }
    if (form.vatRegistered && (!form.panNumber || form.panNumber.trim().length !== 9)) {
      setError("VAT-registered customers require a valid 9-digit PAN.");
      return;
    }
    try {
      await createCustomer({
        ...form,
        name: form.name.trim(),
        code: form.code.trim(),
        email: form.email?.trim() || undefined,
        phone: form.phone?.trim() || undefined,
        address: form.address?.trim() || undefined,
        panNumber: form.panNumber?.trim() ? form.panNumber.trim() : undefined,
        creditLimit: form.creditLimit ? Number(form.creditLimit) : undefined,
        paymentTermsDays: form.paymentTermsDays != null ? Number(form.paymentTermsDays) : undefined,
      }).unwrap();
      onOpenChange(false);
    } catch (e: unknown) {
      setError(getApiErrorMessage(e));
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>New Customer</DialogTitle>
          <DialogDescription>
            Add a customer master record for retail or wholesale billing.
          </DialogDescription>
        </DialogHeader>

        <div className="grid gap-3.5 text-sm py-1">
          <div className="grid grid-cols-2 gap-3">
            <div className="grid gap-1">
              <Label>Name *</Label>
              <Input
                placeholder="e.g. Himalayan Traders Pvt. Ltd."
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
              />
            </div>
            <div className="grid gap-1">
              <Label>Customer Code *</Label>
              <Input
                placeholder="e.g. CUST-001"
                value={form.code}
                onChange={(e) => setForm({ ...form, code: e.target.value.toUpperCase() })}
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="grid gap-1">
              <Label>Customer Type</Label>
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
              <Label>PAN (9 digits)</Label>
              <Input
                value={form.panNumber ?? ""}
                onChange={(e) => setForm({ ...form, panNumber: e.target.value })}
                placeholder="e.g. 601234567"
                maxLength={9}
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="grid gap-1">
              <Label>Email</Label>
              <Input
                type="email"
                placeholder="billing@example.com"
                value={form.email ?? ""}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
              />
            </div>
            <div className="grid gap-1">
              <Label>Phone</Label>
              <Input
                placeholder="+977-9800000000"
                value={form.phone ?? ""}
                onChange={(e) => setForm({ ...form, phone: e.target.value })}
              />
            </div>
          </div>

          <div className="grid gap-1">
            <Label>Billing / Legal Address</Label>
            <Input
              placeholder="e.g. Tripureshwor, Kathmandu, Bagmati, Nepal"
              value={form.address ?? ""}
              onChange={(e) => setForm({ ...form, address: e.target.value })}
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="grid gap-1">
              <Label>Credit Limit (NPR)</Label>
              <Input
                type="number"
                min="0"
                placeholder="0.00"
                value={form.creditLimit ?? ""}
                onChange={(e) => setForm({ ...form, creditLimit: e.target.value ? Number(e.target.value) : undefined })}
              />
            </div>
            <div className="grid gap-1">
              <Label>Payment Terms (Days)</Label>
              <Input
                type="number"
                min="0"
                placeholder="e.g. 30"
                value={form.paymentTermsDays ?? ""}
                onChange={(e) => setForm({ ...form, paymentTermsDays: e.target.value ? Number(e.target.value) : undefined })}
              />
            </div>
          </div>

          <div className="flex items-center space-x-2 pt-2 border-t">
            <input
              type="checkbox"
              id="cust-vat-registered"
              checked={form.vatRegistered}
              onChange={(e) => setForm({ ...form, vatRegistered: e.target.checked })}
              className="h-4 w-4 rounded border-gray-300 text-primary focus:ring-primary"
            />
            <label htmlFor="cust-vat-registered" className="text-sm font-medium cursor-pointer">
              VAT Registered (Buyer will receive legal VAT Tax Invoices)
            </label>
          </div>

          {error ? <p className="text-sm text-red-600 font-medium">{error}</p> : null}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>Cancel</Button>
          <Button onClick={submit} disabled={isLoading}>{isLoading ? "Saving…" : "Create Customer"}</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
