"use client";

import { useGetSupplierPaymentQuery } from "@/services/supplier-payment.service";
import {
  formatDate,
  formatMoney,
  formatTimestamp,
} from "@/lib/procurement-format";

import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetFooter,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet";
import {
  PaymentMethodBadge,
  SupplierPaymentStatusBadge,
} from "@/components/procurement/procurement-status-badge";

interface SupplierPaymentDetailsSheetProps {
  paymentId: number | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

function DetailRow({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="flex items-start justify-between gap-3 border-b border-border py-2 last:border-0">
      <span className="text-xs text-muted-foreground">{label}</span>
      <span className="text-right text-sm font-medium text-foreground">{value}</span>
    </div>
  );
}

/**
 * Full record of a single payment.
 *
 * The table carries the amount, supplier and invoice, but not who posted it or when.
 * Those two fields are the audit trail for money leaving the business, so they are
 * surfaced here rather than left in the database.
 */
export function SupplierPaymentDetailsSheet({
  paymentId,
  open,
  onOpenChange,
}: SupplierPaymentDetailsSheetProps) {
  const {
    data: payment,
    isLoading,
    error,
    refetch,
  } = useGetSupplierPaymentQuery(paymentId ?? 0, {
    skip: !paymentId || !open,
  });

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
        <SheetContent className="w-full overflow-y-auto sm:max-w-3xl">
        <SheetHeader className="sticky top-0 z-10 border-b border-border bg-background">
          <SheetTitle>
            {payment ? `Payment ${payment.paymentNumber}` : "Supplier payment"}
          </SheetTitle>
          <SheetDescription>
            {payment
              ? `${payment.supplierCode} — ${payment.supplierName}`
              : "Loading the payment…"}
          </SheetDescription>
        </SheetHeader>

        {isLoading && (
          <div className="space-y-3 px-4 py-4">
            <Skeleton className="h-6 w-40" />
            <Skeleton className="h-28 w-full" />
          </div>
        )}

        {!isLoading && error && (
          <div className="py-8 text-center">
            <p className="text-sm text-muted-foreground">
              This payment could not be loaded. It may belong to another company or may
              have been deleted.
            </p>
            <Button variant="outline" size="sm" className="mt-3" onClick={refetch}>
              Try again
            </Button>
          </div>
        )}

        {!isLoading && !error && payment && (
          <div className="px-4 pb-6">
            <div className="mb-4 flex flex-wrap gap-2">
              <SupplierPaymentStatusBadge status={payment.status} />
              <PaymentMethodBadge method={payment.paymentMethod} />
            </div>

            <div className="py-2">
              <h4 className="mb-1 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                Payment
              </h4>
              <DetailRow label="Amount" value={formatMoney(payment.amount, payment.currency)} />
              <DetailRow label="Payment date" value={formatDate(payment.paymentDate)} />
              <DetailRow label="Reference" value={payment.referenceNo ?? "—"} />
              <DetailRow label="Paid from" value={payment.paidFrom ?? "—"} />
              <DetailRow label="Settles invoice" value={payment.invoiceNumber} />
            </div>

            <div className="py-2">
              <h4 className="mb-1 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                Audit
              </h4>
              <DetailRow label="Recorded" value={formatTimestamp(payment.createdAt)} />
              <DetailRow label="Posted" value={formatTimestamp(payment.postedAt)} />
              <DetailRow label="Posted by" value={payment.postedBy ?? "—"} />
            </div>

            {payment.notes && (
              <div className="py-3">
                <h4 className="mb-1 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                  Notes
                </h4>
                <p className="whitespace-pre-wrap text-sm text-foreground">
                  {payment.notes}
                </p>
              </div>
            )}

            {payment.cancellationReason && (
              <div className="py-3">
                <h4 className="mb-1 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                  Cancellation reason
                </h4>
                <p className="whitespace-pre-wrap text-sm text-foreground">
                  {payment.cancellationReason}
                </p>
              </div>
            )}
          </div>
        )}

        {!isLoading && !error && payment && (
          <SheetFooter className="border-t border-border pt-4">
            <p className="text-xs text-muted-foreground">
              {payment.status === "CANCELLED"
                ? "This payment was cancelled and no longer reduces the invoice balance."
                : "Posted payments are final. To reverse one, cancel it from the invoice."}
            </p>
          </SheetFooter>
        )}
      </SheetContent>
    </Sheet>
  );
}
