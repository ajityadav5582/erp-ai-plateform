"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { ArrowRight, Eye, MoreVertical, Plus, Search, Wallet } from "lucide-react";
import {
  useGetSupplierPaymentsQuery,
  type SupplierPayment,
} from "@/services/supplier-payment.service";
import {
  PaymentMethodBadge,
  SupplierPaymentStatusBadge,
} from "@/components/procurement/procurement-status-badge";
import { ListPageHeader } from "@/components/common/list-page-header";
import { ListToolbar } from "@/components/common/list-toolbar";
import { ListPagination } from "@/components/common/list-pagination";
import {
  TableEmptyRow,
  TableErrorRow,
  TableSkeletonRows,
} from "@/components/common/table-state";
import {
  formatCurrencyTotals,
  formatDate,
  formatMoney,
  sumByCurrency,
} from "@/lib/procurement-format";

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
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import dynamic from "next/dynamic";

/**
 * Code-split modals. These components stay closed for the whole time this
 * list is on screen, so keeping them out of the initial payload lets the page
 * become interactive without parsing them. `loading: () => null` matches the
 * closed state, so opening one causes no layout shift.
 */
const SupplierPaymentDetailsSheet = dynamic(
  () =>
    import("@/components/procurement/supplier-payment-details-sheet").then((mod) => ({
      default: mod.SupplierPaymentDetailsSheet,
    })),
  { ssr: false, loading: () => null }
);

const RecordSupplierPaymentDialog = dynamic(
  () =>
    import("@/components/procurement/record-supplier-payment-dialog").then((mod) => ({
      default: mod.RecordSupplierPaymentDialog,
    })),
  { ssr: false, loading: () => null }
);

const COLUMNS = 9;

/**
 * The record of money paid out to suppliers.
 *
 * Supports both single-invoice payments and multi-invoice batch settlements.
 */
export default function SupplierPaymentsPage() {
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [searchTerm, setSearchTerm] = useState("");
  const [detailsId, setDetailsId] = useState<number | null>(null);
  const [recordPaymentOpen, setRecordPaymentOpen] = useState(false);

  const listParams = useMemo(
    () => ({ page, size: pageSize, sort: "paymentDate,desc" }),
    [page, pageSize]
  );

  const {
    data,
    isLoading,
    isFetching,
    isError,
    refetch,
  } = useGetSupplierPaymentsQuery(listParams);

  // Memoised because the per-currency totals below depend on this array's identity:
  // `?? []` builds a fresh array on every render, which would recompute the totals each
  // time even when the page contents have not changed.
  const payments = useMemo(() => data?.content ?? [], [data]);

  const normalizedTerm = searchTerm.trim().toLowerCase();

  const visiblePayments = useMemo(() => {
    if (!normalizedTerm) return payments;
    return payments.filter(
      (payment) =>
        payment.paymentNumber.toLowerCase().includes(normalizedTerm) ||
        (payment.invoiceNumber ?? "").toLowerCase().includes(normalizedTerm) ||
        payment.supplierName.toLowerCase().includes(normalizedTerm) ||
        payment.supplierCode.toLowerCase().includes(normalizedTerm) ||
        (payment.referenceNo ?? "").toLowerCase().includes(normalizedTerm)
    );
  }, [payments, normalizedTerm]);

  /**
   * Total paid on this page, kept per currency.
   *
   * Cancelled payments are excluded: they never left the business, so including them
   * would overstate what was actually disbursed. Grouping by currency rather than
   * summing flat is deliberate - a single figure across mixed currencies would be a
   * number that settles nothing. Labelled as a page total because that is what it is.
   */
  const pageTotals = useMemo(
    () =>
      sumByCurrency(
        visiblePayments
          .filter((payment) => payment.status === "POSTED")
          .map((payment) => ({
            currency: payment.currency,
            amount: payment.amount,
          }))
      ),
    [visiblePayments]
  );

  const handlePageSizeChange = (size: number) => {
    setPageSize(size);
    setPage(0);
  };

  return (
    <div className="flex flex-1 flex-col space-y-6 p-4 sm:p-6 lg:p-8">
      <ListPageHeader
        title="Supplier Payments"
        description="Money paid out to suppliers, and which invoice each payment settled."
        count={data?.totalElements}
        countLabel="payments"
        actions={
          <div className="flex items-center gap-2">
            <Button
              size="sm"
              onClick={() => setRecordPaymentOpen(true)}
              className="h-9 gap-1.5 shadow-sm"
            >
              <Plus className="size-4" />
              Record Payment
            </Button>
            <Button asChild variant="outline" size="sm" className="h-9 gap-1.5">
              <Link href="/purchase-invoices">
                Settle from Invoice
                <ArrowRight className="size-3.5" />
              </Link>
            </Button>
          </div>
        }
      />

      <ListToolbar
        searchValue={searchTerm}
        onSearchChange={setSearchTerm}
        searchPlaceholder="Filter payments by number, invoice, reference or supplier..."
        isFetching={isFetching}
        onRefresh={() => {
          setPage(0);
          refetch();
        }}
      />

      {normalizedTerm && (
        <p className="-mt-3 text-sm text-muted-foreground">
          Showing {visiblePayments.length} of {payments.length} payment
          {payments.length === 1 ? "" : "s"} on this page.
          {visiblePayments.length === 0 &&
            " No match on this page - try another page or clear the filter."}
        </p>
      )}

      <div className="overflow-hidden rounded-xl border border-border bg-card shadow-sm">
        <Table>
          <TableHeader className="bg-muted/40">
            <TableRow>
              <TableHead>Payment</TableHead>
              <TableHead>Date</TableHead>
              <TableHead>Supplier</TableHead>
              <TableHead>Settles invoice</TableHead>
              <TableHead>Method</TableHead>
              <TableHead>Reference</TableHead>
              <TableHead>Status</TableHead>
              <TableHead className="text-right">Amount</TableHead>
              <TableHead className="text-right">Actions</TableHead>
            </TableRow>
          </TableHeader>

          <TableBody>
            {isLoading ? (
              <TableSkeletonRows columns={COLUMNS} />
            ) : isError ? (
              <TableErrorRow
                columns={COLUMNS}
                title="Could not load supplier payments"
                description="The supplier payment list could not be loaded."
                onRetry={() => void refetch()}
              />
            ) : payments.length === 0 ? (
              <TableEmptyRow
                icon={Wallet}
                title="No payments recorded yet"
                description="Once an invoice is settled, the payment appears here."
                columns={COLUMNS}
              />
            ) : visiblePayments.length === 0 ? (
              <TableEmptyRow
                icon={Search}
                title="No payment matches that search"
                description="Nothing on this page matches. Try a different term, clear the filter, or look at another page."
                columns={COLUMNS}
                action={
                  <Button variant="link" size="sm" onClick={() => setSearchTerm("")}>
                    Clear search
                  </Button>
                }
              />
            ) : (
              visiblePayments.map((payment: SupplierPayment) => (
                <TableRow
                  key={payment.id}
                  className="group transition-colors hover:bg-muted/50"
                >
                  <TableCell className="font-mono text-sm font-medium text-foreground">
                    {payment.paymentNumber}
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {formatDate(payment.paymentDate)}
                  </TableCell>
                  <TableCell>
                    <span className="block text-sm text-foreground">
                      {payment.supplierName}
                    </span>
                    <span className="block text-xs text-muted-foreground">
                      {payment.supplierCode}
                    </span>
                  </TableCell>
                  <TableCell className="font-mono text-sm text-muted-foreground">
                    {payment.invoiceNumber ? (
                      payment.invoiceNumber
                    ) : Number(payment.unallocatedAmount) > 0 && (!payment.allocations || payment.allocations.length === 0) ? (
                      <span className="inline-flex items-center rounded-md bg-amber-500/10 px-2 py-0.5 text-xs font-semibold text-amber-700 dark:text-amber-400">
                        Advance Deposit
                      </span>
                    ) : (
                      <div className="flex flex-col gap-0.5">
                        <span className="inline-flex items-center rounded-md bg-primary/10 px-2 py-0.5 text-xs font-semibold text-primary w-fit">
                          Multi-Invoice
                        </span>
                        {Number(payment.unallocatedAmount) > 0 && (
                          <span className="text-[10px] text-amber-600 dark:text-amber-400 font-medium">
                            + {formatMoney(payment.unallocatedAmount, payment.currency)} advance
                          </span>
                        )}
                      </div>
                    )}
                  </TableCell>
                  <TableCell>
                    <PaymentMethodBadge method={payment.paymentMethod} />
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {payment.referenceNo ?? "—"}
                  </TableCell>
                  <TableCell>
                    <SupplierPaymentStatusBadge status={payment.status} />
                  </TableCell>
                  <TableCell className="text-right text-sm font-medium text-foreground">
                    <div>{formatMoney(payment.amount, payment.currency)}</div>
                    {Number(payment.unallocatedAmount) > 0 && (
                      <div className="text-[11px] font-normal text-emerald-600 dark:text-emerald-400">
                        avail. {formatMoney(payment.unallocatedAmount, payment.currency)}
                      </div>
                    )}
                  </TableCell>
                  <TableCell className="text-right">
                    <DropdownMenu modal>
                      <DropdownMenuTrigger asChild>
                        <Button variant="ghost" size="icon" className="size-8">
                          <MoreVertical className="size-4" />
                          <span className="sr-only">Open menu</span>
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end" className="z-[9999] w-48">
                        <DropdownMenuItem onClick={() => setDetailsId(payment.id)}>
                          <Eye className="mr-2 size-4 text-muted-foreground" />
                          View details
                        </DropdownMenuItem>
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>

        <ListPagination
          page={page}
          pageSize={pageSize}
          onPageChange={setPage}
          onPageSizeChange={handlePageSizeChange}
          totalPages={data?.totalPages ?? 1}
          totalElements={data?.totalElements ?? 0}
          itemLabel="payments"
          isFetching={isFetching}
          trailingNote={`paid on this page ${formatCurrencyTotals(pageTotals)}`}
        />
      </div>

      <SupplierPaymentDetailsSheet
        paymentId={detailsId}
        open={detailsId !== null}
        onOpenChange={(open) => {
          if (!open) setDetailsId(null);
        }}
      />

      <RecordSupplierPaymentDialog
        open={recordPaymentOpen}
        onOpenChange={setRecordPaymentOpen}
        onPaid={() => {
          refetch();
        }}
      />
    </div>
  );
}
