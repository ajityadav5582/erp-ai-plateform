"use client";

import * as React from "react";
import { Banknote, CreditCard, Loader2, Wallet } from "lucide-react";
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  POS_PAYMENT_METHODS,
  POS_PAYMENT_METHOD_LABELS,
  type PosPaymentMethod,
} from "@/services/pos-transaction.service";
import { useGetActiveCustomersQuery } from "@/services/customer.service";
import { formatPosCurrency } from "@/lib/pos-format";
import { computeNepalTax } from "@/lib/pos-tax";

const WALK_IN = "walk-in";
const QUICK_CASH = [100, 500, 1000];

interface SettlementDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  total: number;
  isSubmitting: boolean;
  onConfirm: (payment: {
    paymentMethod: PosPaymentMethod;
    amountTendered?: number;
    paymentReference?: string;
    customerId?: number;
    customerName?: string;
    customerPanNumber?: string;
  }) => void;
}

export function SettlementDialog({ open, onOpenChange, total, isSubmitting, onConfirm }: SettlementDialogProps) {
  const { data: customers, isLoading: customersLoading } = useGetActiveCustomersQuery(undefined, { skip: !open });
  const [method, setMethod] = React.useState<PosPaymentMethod>("CASH");
  const [tendered, setTendered] = React.useState("");
  const [reference, setReference] = React.useState("");
  const [customerId, setCustomerId] = React.useState(WALK_IN);
  const [customerPan, setCustomerPan] = React.useState("");
  const [discount, setDiscount] = React.useState(0);
  const [nonTaxable, setNonTaxable] = React.useState(0);

  React.useEffect(() => {
    if (open) {
      setMethod("CASH");
      setTendered("");
      setReference("");
      setCustomerId(WALK_IN);
      setCustomerPan("");
      setDiscount(0);
      setNonTaxable(0);
    }
  }, [open]);

  const isWalkIn = customerId === WALK_IN;
  const selectedCustomer = (customers ?? []).find((c) => String(c.id) === customerId);

  const tenderedValue = Number(tendered);
  const hasTendered = tendered.trim() !== "" && Number.isFinite(tenderedValue);
  const change = hasTendered ? Math.max(tenderedValue - total, 0) : 0;
  const isShort = hasTendered && tenderedValue < total;

  const tax = computeNepalTax(total, discount, nonTaxable, 0.13);

  const submit = () => {
    onConfirm({
      paymentMethod: method,
      amountTendered: method === "CASH" && hasTendered ? tenderedValue : undefined,
      paymentReference: reference.trim() || undefined,
      customerId: isWalkIn ? undefined : selectedCustomer?.id,
      customerName: isWalkIn ? undefined : selectedCustomer?.name,
      customerPanNumber: customerPan.trim() || undefined,
    });
  };

  // Hotkeys
  React.useEffect(() => {
    if (!open) return;
    const handler = (e: KeyboardEvent) => {
      if (e.key === "F1") { e.preventDefault(); setMethod("CASH"); }
      else if (e.key === "F2") { e.preventDefault(); setMethod("DIGITAL_WALLET"); }
      else if (e.key === "F3") { e.preventDefault(); setMethod("CARD"); }
      else if (e.key === "F4") { e.preventDefault(); setMethod("OTHER"); }
      else if (e.key === "Enter" && !isSubmitting && !isShort) { e.preventDefault(); submit(); }
    };
    window.addEventListener("keydown", handler);
    return () => window.removeEventListener("keydown", handler);
  }, [open, isSubmitting, isShort]);

  const isTaxInvoice = customerPan.trim().length === 9;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>Settlement</DialogTitle>
          <DialogDescription>
            {isTaxInvoice ? "Tax Invoice (कर विजक)" : "Abbreviated / Cash Invoice"}
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4">
          {/* Customer */}
          <div className="space-y-2">
            <Label>Customer</Label>
            <Select
              value={customerId}
              onValueChange={(value) => {
                setCustomerId(value);
                const customer = (customers ?? []).find((c) => String(c.id) === value);
                setCustomerPan(customer?.panNumber ?? "");
              }}
            >
              <SelectTrigger>
                <SelectValue placeholder={customersLoading ? "Loading…" : "Walk-in customer"} />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={WALK_IN}>Walk-in customer</SelectItem>
                {(customers ?? []).map((customer) => (
                  <SelectItem key={customer.id} value={String(customer.id)}>
                    {customer.name}
                    {customer.panNumber ? ` · ${customer.panNumber}` : ""}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-2">
            <Label htmlFor="customer-pan">Buyer PAN (9 digits)</Label>
            <Input
              id="customer-pan"
              value={customerPan}
              onChange={(e) => setCustomerPan(e.target.value)}
              placeholder="123456789"
              maxLength={9}
            />
          </div>

          {/* Method selector */}
          <div className="grid grid-cols-2 gap-2">
            {POS_PAYMENT_METHODS.map((option) => (
              <Button
                key={option}
                type="button"
                variant={method === option ? "default" : "outline"}
                className="h-14 flex-col gap-1"
                onClick={() => setMethod(option)}
              >
                {option === "CASH" ? <Banknote className="h-5 w-5" /> : option === "CARD" ? <CreditCard className="h-5 w-5" /> : <Wallet className="h-5 w-5" />}
                <span className="text-xs">{POS_PAYMENT_METHOD_LABELS[option]}</span>
              </Button>
            ))}
          </div>

          {/* Tax breakdown */}
          <div className="rounded-lg border bg-muted/30 p-3 text-sm">
            <div className="flex justify-between"><span>Gross Total</span><span>{formatPosCurrency(tax.grossTotal)}</span></div>
            <div className="flex justify-between"><span>Discount</span><span>{formatPosCurrency(tax.discount)}</span></div>
            <div className="flex justify-between"><span>Non-Taxable</span><span>{formatPosCurrency(tax.nonTaxable)}</span></div>
            <div className="flex justify-between"><span>Taxable (13% base)</span><span>{formatPosCurrency(tax.taxable)}</span></div>
            <div className="flex justify-between"><span>VAT (13%)</span><span>{formatPosCurrency(tax.vat)}</span></div>
            <div className="flex justify-between"><span>Round-off</span><span>{formatPosCurrency(tax.roundOff)}</span></div>
            <div className="flex justify-between font-bold"><span>Grand Net Payable</span><span>{formatPosCurrency(tax.grandTotal)}</span></div>
          </div>

          {/* Discount / non-taxable inputs */}
          <div className="grid grid-cols-2 gap-2">
            <div className="space-y-1">
              <Label htmlFor="discount">Discount</Label>
              <Input id="discount" type="number" min={0} value={discount} onChange={(e) => setDiscount(Number(e.target.value) || 0)} />
            </div>
            <div className="space-y-1">
              <Label htmlFor="nontax">Non-Taxable</Label>
              <Input id="nontax" type="number" min={0} value={nonTaxable} onChange={(e) => setNonTaxable(Number(e.target.value) || 0)} />
            </div>
          </div>

          {method === "CASH" && (
            <div className="space-y-2">
              <Label htmlFor="tendered">Cash tendered</Label>
              <Input
                id="tendered"
                type="number"
                min={0}
                step="0.01"
                value={tendered}
                onChange={(e) => setTendered(e.target.value)}
                placeholder={tax.grandTotal.toFixed(2)}
                className="h-12 text-lg"
              />
              <div className="flex flex-wrap gap-2">
                {QUICK_CASH.map((amount) => (
                  <Button key={amount} type="button" variant="secondary" size="sm" onClick={() => setTendered(String(amount))}>
                    Rs {amount}
                  </Button>
                ))}
                <Button type="button" variant="secondary" size="sm" onClick={() => setTendered(tax.grandTotal.toFixed(2))}>
                  Exact
                </Button>
              </div>
              {hasTendered && (
                <p className={isShort ? "text-sm text-destructive" : "text-sm text-emerald-600"}>
                  {isShort ? `Short by ${formatPosCurrency(total - tenderedValue)}` : `Change due ${formatPosCurrency(change)}`}
                </p>
              )}
            </div>
          )}

          {method !== "CASH" && (
            <div className="space-y-2">
              <Label htmlFor="reference">Reference (approval code / txn id)</Label>
              <Input id="reference" value={reference} onChange={(e) => setReference(e.target.value)} placeholder="Optional" />
            </div>
          )}

          {method === "DIGITAL_WALLET" && (
            <div className="rounded-lg border p-3 text-center text-sm text-muted-foreground">
              Dynamic QR for Rs {tax.grandTotal.toFixed(2)} (counter + bill trace) would render here.
            </div>
          )}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)} disabled={isSubmitting}>
            Cancel
          </Button>
          <Button type="button" onClick={submit} disabled={isSubmitting || isShort}>
            {isSubmitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            Complete sale
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
