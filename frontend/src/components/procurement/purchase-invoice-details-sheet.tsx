"use client";

import { useState } from "react";
import { Coins, Loader2, Wallet } from "lucide-react";
import { toast } from "sonner";
import { useGetPurchaseInvoiceQuery } from "@/services/purchase-invoice.service";
import {
  useGetSupplierPaymentsByInvoiceQuery,
  useGetAvailableAdvancesQuery,
} from "@/services/supplier-payment.service";
import {
  formatAmount,
  formatDate,
  formatMoney,
  formatPercentage,
  formatQuantity,
  formatTimestamp,
  isOverdue,
} from "@/lib/procurement-format";

import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
  SheetFooter,
} from "@/components/ui/sheet";
import { Skeleton } from "@/components/ui/skeleton";
import { Button } from "@/components/ui/button";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  PaymentMethodBadge,
  PurchaseInvoiceStatusBadge,
} from "@/components/procurement/procurement-status-badge";
import { PaySupplierDialog } from "@/components/procurement/pay-supplier-dialog";
import { ApplyAdvanceCreditDialog } from "@/components/procurement/apply-advance-credit-dialog";
import type { SupplierPayment } from "@/services/supplier-payment.service";

interface PurchaseInvoiceDetailsSheetProps {
  invoiceId: number | null;
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
 * Read-only view of a supplier invoice, its billed lines, and the payments made
 * against it.
 *
 * The payment history lives here rather than only on the payments screen because the
 * question being asked while looking at an invoice is "what has already been paid for
 * this?", and the answer is the sum of the rows shown below it.
 *
 * Paying is offered only while a balance remains. A PAID invoice has nothing left to
 * settle, and the server would refuse the payment anyway - offering it would just
 * produce a confusing error on an action that looks reasonable.
 */
export function PurchaseInvoiceDetailsSheet({
  invoiceId,
  open,
  onOpenChange,
}: PurchaseInvoiceDetailsSheetProps) {
  const [payTarget, setPayTarget] = useState<{ invoiceId: number } | null>(null);
  const [advanceDialogOpen, setAdvanceDialogOpen] = useState(false);

  const {
    data: invoice,
    isLoading,
    error,
    refetch,
  } = useGetPurchaseInvoiceQuery(invoiceId ?? 0, {
    skip: !invoiceId || !open,
  });

  // The payment history is only needed once the invoice itself has loaded, so it is
  // skipped until then rather than firing against a zero id.
  const {
    data: paymentsPage,
    isFetching: isFetchingPayments,
  } = useGetSupplierPaymentsByInvoiceQuery(
    { invoiceId: invoice?.id ?? 0, page: 0, size: 20 },
    { skip: !open || !invoice }
  );

  const payments = paymentsPage?.content ?? [];
  const canPay = invoice ? Number(invoice.balanceAmount) > 0 : false;

  // Check if this supplier has advance credits available
  const { data: availableAdvances = [] } = useGetAvailableAdvancesQuery(
    invoice?.supplierId ?? 0,
    { skip: !open || !invoice || !canPay }
  );

  const totalAdvanceCredit = availableAdvances.reduce(
    (sum, a) => sum + Number(a.unallocatedAmount ?? 0),
    0
  );

  const handlePaid = (payment: SupplierPayment) => {
    toast.success(`Payment ${payment.paymentNumber} recorded`, {
      description: `${formatMoney(payment.amount, payment.currency)} against invoice ${payment.invoiceNumber}.`,
    });
    refetch();
  };

  const handleAdvanceApplied = () => {
    refetch();
  };

  return (
    <>
      <Sheet open={open} onOpenChange={onOpenChange}>
        <SheetContent className="w-full overflow-y-auto sm:max-w-3xl">
          <SheetHeader className="sticky top-0 z-10 border-b border-border bg-background">
            <SheetTitle>
              {invoice ? `Invoice ${invoice.invoiceNumber}` : "Supplier invoice"}
            </SheetTitle>
            <SheetDescription>
              {invoice
                ? `${invoice.supplierCode} — ${invoice.supplierName}`
                : "Loading the invoice…"}
            </SheetDescription>
          </SheetHeader>

          {isLoading && (
            <div className="space-y-3 px-4 py-4">
              <Skeleton className="h-6 w-48" />
              <Skeleton className="h-24 w-full" />
              <Skeleton className="h-32 w-full" />
            </div>
          )}

          {!isLoading && error && (
            <div className="py-8 text-center">
              <p className="text-sm text-muted-foreground">
                This invoice could not be loaded. It may belong to another company or may
                have been deleted.
              </p>
              <Button variant="outline" size="sm" className="mt-3" onClick={refetch}>
                Try again
              </Button>
            </div>
          )}

          {!isLoading && !error && invoice && (
            <div className="px-4 pb-6">
              <div className="mb-4 flex flex-wrap gap-2">
                <PurchaseInvoiceStatusBadge status={invoice.status} />
                {isOverdue(invoice.dueDate, invoice.status) && (
                  <span className="text-xs font-medium text-red-600 dark:text-red-400">
                    Payment overdue since {formatDate(invoice.dueDate)}
                  </span>
                )}
              </div>

              <div className="grid gap-4 py-2 sm:grid-cols-2">
                <div>
                  <h4 className="mb-1 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                    Invoice
                  </h4>
                  <DetailRow
                    label="Type"
                    value={invoice.invoiceType === "DIRECT" ? "Direct Service / Expense" : "Goods Receipt (3-Way)"}
                  />
                  <DetailRow
                    label="Invoice date"
                    value={formatDate(invoice.invoiceDate)}
                  />
                  <DetailRow label="Due date" value={formatDate(invoice.dueDate)} />
                  <DetailRow
                    label="Purchase order"
                    value={invoice.poNumber ?? "—"}
                  />
                  <DetailRow label="Goods receipt" value={invoice.grnNumber ?? "— (Direct Bill)"} />
                  <DetailRow
                    label="Supplier VAT status"
                    value={invoice.supplierVatRegistrationStatus ?? "—"}
                  />
                  <DetailRow
                    label="Supplier VAT number"
                    value={invoice.supplierVatRegistrationNumber ?? "—"}
                  />
                  <DetailRow
                    label="Supplier Tax ID"
                    value={invoice.supplierTaxId ?? "—"}
                  />
                  <DetailRow
                    label="Recorded"
                    value={formatTimestamp(invoice.createdAt)}
                  />
                </div>

                <div>
                  <h4 className="mb-1 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                    Amounts
                  </h4>
                  <DetailRow
                    label="Subtotal"
                    value={formatMoney(invoice.subtotalAmount, invoice.currency)}
                  />
                  <DetailRow
                    label="Tax"
                    value={formatMoney(invoice.taxAmount, invoice.currency)}
                  />
                  <DetailRow
                    label="Gross Total"
                    value={
                      <span className={invoice.isTdsApplicable ? "text-sm text-muted-foreground" : "text-base font-semibold"}>
                        {formatMoney(invoice.totalAmount, invoice.currency)}
                      </span>
                    }
                  />
                  {invoice.isTdsApplicable && (
                    <>
                      <DetailRow
                        label={`TDS Withheld (${invoice.tdsRate ?? 0}%)`}
                        value={
                          <span className="text-amber-600 dark:text-amber-400 font-medium">
                            -{formatMoney(invoice.tdsAmount ?? 0, invoice.currency)}
                          </span>
                        }
                      />
                      <DetailRow
                        label="Net Payable"
                        value={
                          <span className="text-base font-bold text-primary">
                            {formatMoney(invoice.netPayableAmount ?? invoice.totalAmount, invoice.currency)}
                          </span>
                        }
                      />
                    </>
                  )}
                  <DetailRow
                    label="Paid"
                    value={formatMoney(invoice.paidAmount, invoice.currency)}
                  />
                  <DetailRow
                    label="Balance"
                    value={
                      <span
                        className={
                          canPay
                            ? "text-amber-600 dark:text-amber-400"
                            : "text-emerald-600 dark:text-emerald-400"
                        }
                      >
                        {formatMoney(invoice.balanceAmount, invoice.currency)}
                      </span>
                    }
                  />
                </div>
              </div>

              {canPay && totalAdvanceCredit > 0 && (
                <div className="my-3 flex items-center justify-between rounded-lg border border-amber-500/30 bg-amber-500/10 p-3">
                  <div className="flex items-center gap-2.5">
                    <Coins className="size-4 text-amber-600 dark:text-amber-400 shrink-0" />
                    <div className="text-xs">
                      <span className="font-semibold text-foreground">
                        {formatMoney(totalAdvanceCredit, invoice.currency)}
                      </span>{" "}
                      in advance credit available from this supplier
                    </div>
                  </div>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setAdvanceDialogOpen(true)}
                    className="h-7 text-xs border-amber-500/40 text-amber-800 dark:text-amber-200 hover:bg-amber-500/20"
                  >
                    Apply Credit
                  </Button>
                </div>
              )}

              <div className="py-3">
                <h4 className="mb-2 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                  Billed lines
                </h4>
                <div className="overflow-hidden rounded-lg border border-border">
                  <Table>
                    <TableHeader className="bg-muted/40">
                      <TableRow>
                        <TableHead>#</TableHead>
                        <TableHead>Item</TableHead>
                        <TableHead className="text-right">Qty</TableHead>
                        <TableHead className="text-right">Price</TableHead>
                        <TableHead className="text-right">Disc %</TableHead>
                        <TableHead className="text-right">Tax %</TableHead>
                        <TableHead className="text-right">Taxable</TableHead>
                        <TableHead className="text-right">Amount</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {invoice.lines.map((line) => (
                        <TableRow key={line.id}>
                          <TableCell className="text-muted-foreground">
                            {line.lineNumber}
                          </TableCell>
                          <TableCell>
                            <span className="block font-medium text-foreground">
                              {line.itemName}
                            </span>
                            <span className="block text-xs text-muted-foreground">
                              {line.itemCode} · {line.uomCode}
                            </span>
                          </TableCell>
                          <TableCell className="text-right">
                            {formatQuantity(line.quantity)}
                          </TableCell>
                          <TableCell className="text-right">
                            {formatAmount(line.unitPrice)}
                          </TableCell>
                          <TableCell className="text-right">
                            {formatPercentage(line.discountPercentage)}
                          </TableCell>
                          <TableCell className="text-right">
                            {formatPercentage(line.taxRate)}
                          </TableCell>
                          <TableCell className="text-right">
                            {line.taxableAmount != null
                              ? formatAmount(line.taxableAmount)
                              : "—"}
                          </TableCell>
                          <TableCell className="text-right font-medium">
                            {formatAmount(line.lineAmount)}
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </div>
              </div>

              <div className="py-3">
                <h4 className="mb-2 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                  Payments made
                  {isFetchingPayments && (
                    <Loader2
                      className="ml-2 inline size-3 animate-spin"
                      aria-hidden="true"
                    />
                  )}
                </h4>
                {payments.length === 0 ? (
                  <p className="text-sm text-muted-foreground">
                    Nothing has been paid against this invoice yet.
                  </p>
                ) : (
                  <div className="overflow-hidden rounded-lg border border-border">
                    <Table>
                      <TableHeader className="bg-muted/40">
                        <TableRow>
                          <TableHead>Payment</TableHead>
                          <TableHead>Date</TableHead>
                          <TableHead>Method</TableHead>
                          <TableHead>Reference</TableHead>
                          <TableHead className="text-right">Amount</TableHead>
                        </TableRow>
                      </TableHeader>
                      <TableBody>
                        {payments.map((payment) => (
                          <TableRow key={payment.id}>
                            <TableCell className="font-medium text-foreground">
                              {payment.paymentNumber}
                            </TableCell>
                            <TableCell>{formatDate(payment.paymentDate)}</TableCell>
                            <TableCell>
                              <PaymentMethodBadge method={payment.paymentMethod} />
                            </TableCell>
                            <TableCell className="text-muted-foreground">
                              {payment.referenceNo ?? "—"}
                            </TableCell>
                            <TableCell className="text-right font-medium">
                              {formatMoney(payment.amount, payment.currency)}
                            </TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </div>
                )}
              </div>

              {invoice.notes && (
                <div className="py-3">
                  <h4 className="mb-1 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                    Notes
                  </h4>
                  <p className="whitespace-pre-wrap text-sm text-foreground">
                    {invoice.notes}
                  </p>
                </div>
              )}
            </div>
          )}

          {!isLoading && !error && invoice && (
            <SheetFooter className="border-t border-border pt-4">
              {canPay ? (
                <div className="flex w-full flex-wrap items-center justify-between gap-2">
                  {totalAdvanceCredit > 0 && (
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      onClick={() => setAdvanceDialogOpen(true)}
                      className="border-amber-500/40 text-amber-700 dark:text-amber-300 hover:bg-amber-500/10"
                    >
                      <Coins className="mr-1.5 size-4 text-amber-500" />
                      Apply Advance ({formatMoney(totalAdvanceCredit, invoice.currency)})
                    </Button>
                  )}
                  <Button
                    size="sm"
                    onClick={() => setPayTarget({ invoiceId: invoice.id })}
                    className="ml-auto"
                  >
                    <Wallet className="mr-2 size-4" aria-hidden="true" />
                    Pay {formatMoney(invoice.balanceAmount, invoice.currency)}
                  </Button>
                </div>
              ) : (
                <p className="text-xs text-muted-foreground">
                  This invoice is settled in full. Nothing further is payable.
                </p>
              )}
            </SheetFooter>
          )}
        </SheetContent>
      </Sheet>

      {payTarget && (
        <PaySupplierDialog
          invoiceId={payTarget.invoiceId}
          open={payTarget !== null}
          onOpenChange={(next) => !next && setPayTarget(null)}
          onPaid={handlePaid}
        />
      )}

      {invoice && (
        <ApplyAdvanceCreditDialog
          open={advanceDialogOpen}
          onOpenChange={setAdvanceDialogOpen}
          invoice={invoice}
          onApplied={handleAdvanceApplied}
        />
      )}
    </>
  );
}
