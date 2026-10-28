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

const METHOD_ICONS: Record<PosPaymentMethod, React.ReactNode> = {
  CASH: <Banknote className="h-5 w-5" />,
  CARD: <CreditCard className="h-5 w-5" />,
  DIGITAL_WALLET: <Wallet className="h-5 w-5" />,
  BANK_TRANSFER: <Wallet className="h-5 w-5" />,
  CHEQUE: <Banknote className="h-5 w-5" />,
  OTHER: <Wallet className="h-5 w-5" />,
};

const WALK_IN = "walk-in";

interface PaymentDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  total: number;
  isSubmitting: boolean;
  /** Called with the chosen method, the tendered amount and an optional reference. */
  onConfirm: (payment: {
    paymentMethod: PosPaymentMethod;
    amountTendered?: number;
    paymentReference?: string;
    customerId?: number;
    customerName?: string;
    customerPanNumber?: string;
  }) => void;
}

const QUICK_CASH = [50, 100, 500, 1000];

/**
 * The payment pad.
 *
 * Mobile-first: the method picker is a two-column grid of large targets, and
 * for cash the tender keypad offers one-tap round amounts plus a numeric
 * field, with the change due shown live. The server re-prices at checkout, so
 * the total shown here is the authoritative figure.
 *
 * An optional customer can be attached for B2B tax invoices: picking one
 * pre-fills its PAN, which is frozen onto the sale for the IRD/CBMS report.
 */
export function PaymentDialog({
  open,
  onOpenChange,
  total,
  isSubmitting,
  onConfirm,
}: PaymentDialogProps) {
  const { data: customers, isLoading: customersLoading } = useGetActiveCustomersQuery(
    undefined,
    { skip: !open }
  );
  const [method, setMethod] = React.useState<PosPaymentMethod>("CASH");
  const [tendered, setTendered] = React.useState<string>("");
  const [reference, setReference] = React.useState("");
  const [customerId, setCustomerId] = React.useState<string>(WALK_IN);
  const [customerPan, setCustomerPan] = React.useState("");

  React.useEffect(() => {
    if (open) {
      setMethod("CASH");
      setTendered("");
      setReference("");
      setCustomerId(WALK_IN);
      setCustomerPan("");
    }
  }, [open]);

  const isWalkIn = customerId === WALK_IN;
  const selectedCustomer = (customers ?? []).find((c) => String(c.id) === customerId);

  const tenderedValue = Number(tendered);
  const hasTendered = tendered.trim() !== "" && Number.isFinite(tenderedValue);
  const change = hasTendered ? Math.max(tenderedValue - total, 0) : 0;
  const isShort = hasTendered && tenderedValue < total;

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

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Payment</DialogTitle>
          <DialogDescription>
            Total due <span className="font-bold text-foreground">{formatPosCurrency(total)}</span>
          </DialogDescription>
        </DialogHeader>

        <form
          className="space-y-4"
          onSubmit={(event) => {
            event.preventDefault();
            if (isSubmitting || isShort) return;
            submit();
          }}
        >
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

          {!isWalkIn && (
            <div className="space-y-2">
              <Label htmlFor="customer-pan">PAN (tax invoice)</Label>
              <Input
                id="customer-pan"
                value={customerPan}
                onChange={(event) => setCustomerPan(event.target.value)}
                placeholder="Buyer PAN for a VAT invoice"
              />
            </div>
          )}

          <div className="grid grid-cols-2 gap-2">
            {POS_PAYMENT_METHODS.map((option) => (
              <Button
                key={option}
                type="button"
                variant={method === option ? "default" : "outline"}
                className="h-16 flex-col gap-1"
                onClick={() => setMethod(option)}
              >
                {METHOD_ICONS[option]}
                <span className="text-xs">{POS_PAYMENT_METHOD_LABELS[option]}</span>
              </Button>
            ))}
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
                onChange={(event) => setTendered(event.target.value)}
                placeholder={total.toFixed(2)}
                className="h-12 text-lg"
              />
              <div className="flex flex-wrap gap-2">
                {QUICK_CASH.map((amount) => (
                  <Button
                    key={amount}
                    type="button"
                    variant="secondary"
                    size="sm"
                    onClick={() => setTendered(String(amount))}
                  >
                    Rs {amount}
                  </Button>
                ))}
                <Button
                  type="button"
                  variant="secondary"
                  size="sm"
                  onClick={() => setTendered(total.toFixed(2))}
                >
                  Exact
                </Button>
              </div>
              {hasTendered && (
                <p className={isShort ? "text-sm text-destructive" : "text-sm text-emerald-600"}>
                  {isShort
                    ? `Short by ${formatPosCurrency(total - tenderedValue)}`
                    : `Change due ${formatPosCurrency(change)}`}
                </p>
              )}
            </div>
          )}

          {method !== "CASH" && (
            <div className="space-y-2">
              <Label htmlFor="reference">Reference (approval code / txn id)</Label>
              <Input
                id="reference"
                value={reference}
                onChange={(event) => setReference(event.target.value)}
                placeholder="Optional"
              />
            </div>
          )}
        </form>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)} disabled={isSubmitting}>
            Cancel
          </Button>
          <Button type="submit" onClick={submit} disabled={isSubmitting || isShort}>
            {isSubmitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            Complete sale
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
