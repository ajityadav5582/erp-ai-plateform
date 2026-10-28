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
import { useUnsavedChanges } from "@/hooks/use-unsaved-changes";
import {
  useCreateCustomerMutation,
  useGetActiveCustomersQuery,
  CUSTOMER_TYPES,
  CUSTOMER_TYPE_LABELS,
  type Customer,
  type CustomerType,
  type CreateCustomerRequest,
} from "@/services/customer.service";

export function CreateCustomerDialog({
  open,
  onOpenChange,
  onCreated,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onCreated?: (customer: Customer) => void;
}) {
  const [createCustomer, { isLoading }] = useCreateCustomerMutation();
  const { data: existingCustomers } = useGetActiveCustomersQuery(undefined, { skip: !open });
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
  const { markDirty, markClean, guardClose } = useUnsavedChanges();

  const initialFormRef = React.useRef<CreateCustomerRequest | null>(null);

  React.useEffect(() => {
    if (open) {
      const initial: CreateCustomerRequest = {
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
      };
      setForm(initial);
      initialFormRef.current = initial;
      setError(null);
      markClean();
    }
  }, [open, markClean]);

  // Mark the form dirty whenever it diverges from the pristine snapshot.
  React.useEffect(() => {
    if (!open || !initialFormRef.current) return;
    const dirty = JSON.stringify(form) !== JSON.stringify(initialFormRef.current);
    if (dirty) markDirty();
  }, [form, open, markDirty]);

  const submittingRef = React.useRef(false);

  const duplicateWarnings = React.useMemo(() => {
    const warnings: string[] = [];
    const pan = form.panNumber?.trim();
    const phone = form.phone?.trim();
    const email = form.email?.trim();
    for (const c of existingCustomers ?? []) {
      if (pan && c.panNumber === pan) warnings.push(`PAN already used by ${c.name} (${c.code})`);
      if (phone && c.phone === phone) warnings.push(`Phone already used by ${c.name} (${c.code})`);
      if (email && c.email === email) warnings.push(`Email already used by ${c.name} (${c.code})`);
    }
    return warnings;
  }, [form.panNumber, form.phone, form.email, existingCustomers]);

  const submit = async () => {
    if (submittingRef.current || isLoading) return;
    setError(null);
    if (!form.name.trim() || !form.code.trim()) {
      setError("Name and code are required.");
      return;
    }
    if (form.vatRegistered && (!form.panNumber || form.panNumber.trim().length !== 9)) {
      setError("VAT-registered customers require a valid 9-digit PAN.");
      return;
    }
    submittingRef.current = true;
    try {
      const created = await createCustomer({
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
      onCreated?.(created);
      markClean();
      onOpenChange(false);
    } catch (e: unknown) {
      setError(getApiErrorMessage(e));
    } finally {
      submittingRef.current = false;
    }
  };

  return (
    <Dialog open={open} onOpenChange={(next) => (next ? onOpenChange(true) : guardClose() && onOpenChange(false))}>
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

          {duplicateWarnings.length > 0 && (
            <div className="rounded-md border border-amber-300 bg-amber-50 p-2 text-xs text-amber-800 space-y-1">
              {duplicateWarnings.map((w, i) => (
                <p key={i}>{w}</p>
              ))}
              <p className="font-medium">You can still create the customer if this is intentional.</p>
            </div>
          )}
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
