"use client";

import { useMemo, useState } from "react";
import { Eye, MoreVertical, Receipt, Search, Wallet } from "lucide-react";
import {
  useGetPurchaseInvoicesQuery,
  useGetOutstandingInvoicesQuery,
  type PurchaseInvoice,
} from "@/services/purchase-invoice.service";
import { PurchaseInvoiceStatusBadge } from "@/components/procurement/procurement-status-badge";
import { ListPageHeader } from "@/components/common/list-page-header";
import { ListToolbar } from "@/components/common/list-toolbar";
import { ListPagination } from "@/components/common/list-pagination";
import { ScopeTabs } from "@/components/common/scope-tabs";
import {
  TableEmptyRow,
  TableErrorRow,
  TableSkeletonRows,
} from "@/components/common/table-state";
import {
  formatAmount,
  formatCurrencyTotals,
  formatDate,
  formatMoney,
  isOverdue,
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
const PurchaseInvoiceDetailsSheet = dynamic(
  () =>
    import("@/components/procurement/purchase-invoice-details-sheet").then((mod) => ({
      default: mod.PurchaseInvoiceDetailsSheet,
    })),
  { ssr: false, loading: () => null }
);

type InvoiceScope = "all" | "outstanding";

const COLUMNS = 9;

/**
 * Supplier invoices: what has been billed, and what is still owed.
 *
 * Two lists rather than one with a status filter, because they answer different
 * questions and are backed by different queries. "All invoices" is the accountant's
 * record of what suppliers have charged. "Outstanding" is the treasury worklist - the
 * invoices still owing money, which is a server-side query the backend already serves
 * and which the client cannot reproduce by filtering a page of results locally.
 *
 * Invoices are recorded from a goods receipt, not from here: an invoice is a bill for a
 * delivery, so the entry point lives on the receipt screen where the lines being billed
 * are already known.
 *
 * The scope switch now uses the same pill strip as Requisitions and Purchase Orders.
 * It was the only screen using shadcn's underline `Tabs`, which made it look like a
 * different kind of page from the two screens either side of it in the nav.
 */
export default function PurchaseInvoicesPage() {
  const [scope, setScope] = useState<InvoiceScope>("all");
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [searchTerm, setSearchTerm] = useState("");
  const [detailsId, setDetailsId] = useState<number | null>(null);

  const listParams = useMemo(
    () => ({ page, size: pageSize }),
    [page, pageSize]
  );

  const allQuery = useGetPurchaseInvoicesQuery(listParams, {
    skip: scope !== "all",
  });
  const outstandingQuery = useGetOutstandingInvoicesQuery(listParams, {
    skip: scope !== "outstanding",
  });

  const activeQuery = scope === "outstanding" ? outstandingQuery : allQuery;
  const { data, isLoading, isFetching, isError, refetch } = activeQuery;

  // Memoised because the currency-grouped totals below depend on this array's identity:
  // `?? []` builds a fresh array on every render, which would recompute the totals each
  // time even when the page contents have not changed.
  const invoices = useMemo(() => data?.content ?? [], [data]);

  const normalizedTerm = searchTerm.trim().toLowerCase();

  /**
   * Narrows the loaded page by the identifiers an accountant would actually type.
   *
   * Client-side because the backend `Pageable` binder reads only `page`, `size` and
   * `sort`; a `search` parameter would be accepted and silently dropped. The toolbar
   * states how many of the page's rows are hidden so the result is not mistaken for the
   * whole set.
   */
  const visibleInvoices = useMemo(() => {
    if (!normalizedTerm) return invoices;
    return invoices.filter(
      (invoice) =>
        invoice.invoiceNumber.toLowerCase().includes(normalizedTerm) ||
        invoice.grnNumber.toLowerCase().includes(normalizedTerm) ||
        invoice.supplierName.toLowerCase().includes(normalizedTerm) ||
        invoice.supplierCode.toLowerCase().includes(normalizedTerm)
    );
  }, [invoices, normalizedTerm]);

  /**
   * Sum of the current page's balances, kept per currency.
   *
   * Deliberately labelled as the page total rather than "total outstanding": it is the
   * balances on screen, and showing it as a company-wide figure would be wrong whenever
   * the page is not the whole set. Grouped by currency because adding a rupee balance to
   * a dollar balance produces a figure that settles nothing.
   */
  const pageBalanceTotals = useMemo(
    () =>
      sumByCurrency(
        visibleInvoices.map((invoice) => ({
          currency: invoice.currency,
          amount: invoice.balanceAmount,
        }))
      ),
    [visibleInvoices]
  );

  const isOutstanding = scope === "outstanding";

  const handleScopeChange = (next: string) => {
    setScope(next as InvoiceScope);
    // Each scope is a separate query with its own paging, so a page index valid for one
    // is meaningless for the other.
    setPage(0);
  };

  const handlePageSizeChange = (size: number) => {
    setPageSize(size);
    setPage(0);
  };

  return (
    <div className="flex flex-1 flex-col space-y-6 p-4 sm:p-6 lg:p-8">
      <ListPageHeader
        title="Purchase Invoices"
        description="Supplier invoices recorded against received goods, and the balances still to be paid."
        count={allQuery.data?.totalElements}
        countLabel="invoices"
      />

      <ScopeTabs
        ariaLabel="Invoice scope"
        value={scope}
        onValueChange={handleScopeChange}
        tabs={[
          {
            value: "all",
            label: "All invoices",
            icon: Receipt,
            hint: "Every supplier invoice recorded in this company, newest first.",
          },
          {
            value: "outstanding",
            label: "Outstanding",
            icon: Wallet,
            hint: "Invoices with a balance still to settle, oldest due date first.",
            badge: outstandingQuery.data?.totalElements,
          },
        ]}
      />

      <ListToolbar
        searchValue={searchTerm}
        onSearchChange={setSearchTerm}
        searchPlaceholder="Filter invoices by number, GRN or supplier on this page..."
        isFetching={isFetching}
        onRefresh={() => {
          setPage(0);
          refetch();
        }}
      />

      {normalizedTerm && (
        <p className="-mt-3 text-sm text-muted-foreground">
          Showing {visibleInvoices.length} of {invoices.length} invoice
          {invoices.length === 1 ? "" : "s"} on this page.
          {visibleInvoices.length === 0 &&
            " No match on this page - try another page or clear the filter."}
        </p>
      )}

      <div className="overflow-hidden rounded-xl border border-border bg-card shadow-sm">
        <Table>
          <TableHeader className="bg-muted/40">
            <TableRow>
              <TableHead>Invoice</TableHead>
              <TableHead>Supplier</TableHead>
              <TableHead>Receipt</TableHead>
              <TableHead>Invoice date</TableHead>
              <TableHead>Due</TableHead>
              <TableHead>Status</TableHead>
              <TableHead className="text-right">Total</TableHead>
              <TableHead className="text-right">Balance</TableHead>
              <TableHead className="text-right">Actions</TableHead>
            </TableRow>
          </TableHeader>

          <TableBody>
            {isLoading ? (
              <TableSkeletonRows columns={COLUMNS} />
            ) : isError ? (
              <TableErrorRow
                columns={COLUMNS}
                title="Could not load invoices"
                description="The purchase invoice list could not be loaded."
                onRetry={refetch}
              />
            ) : invoices.length === 0 ? (
              isOutstanding ? (
                <TableEmptyRow
                  icon={Wallet}
                  title="Nothing is outstanding"
                  description="Every recorded invoice has been settled in full."
                  columns={COLUMNS}
                />
              ) : (
                <TableEmptyRow
                  icon={Receipt}
                  title="No invoices recorded yet"
                  description="Record an invoice from a goods receipt once a supplier has billed for a delivery."
                  columns={COLUMNS}
                />
              )
            ) : visibleInvoices.length === 0 ? (
              <TableEmptyRow
                icon={Search}
                title="No invoice matches that search"
                description="Nothing on this page matches. Try a different term, clear the filter, or look at another page."
                columns={COLUMNS}
                action={
                  <Button variant="link" size="sm" onClick={() => setSearchTerm("")}>
                    Clear search
                  </Button>
                }
              />
            ) : (
              visibleInvoices.map((invoice: PurchaseInvoice) => {
                const late = isOverdue(invoice.dueDate, invoice.status);
                return (
                  <TableRow
                    key={invoice.id}
                    className="group transition-colors hover:bg-muted/50"
                  >
                    <TableCell className="font-mono text-sm font-medium text-foreground">
                      {invoice.invoiceNumber}
                    </TableCell>
                    <TableCell>
                      <span className="block text-sm text-foreground">
                        {invoice.supplierName}
                      </span>
                      <span className="block text-xs text-muted-foreground">
                        {invoice.supplierCode}
                      </span>
                    </TableCell>
                    <TableCell className="font-mono text-sm text-muted-foreground">
                      {invoice.grnNumber}
                    </TableCell>
                    <TableCell className="text-sm text-muted-foreground">
                      {formatDate(invoice.invoiceDate)}
                    </TableCell>
                    <TableCell>
                      <span
                        className={
                          late ? "font-medium text-red-600 dark:text-red-400" : undefined
                        }
                      >
                        {formatDate(invoice.dueDate)}
                      </span>
                      {late && (
                        <span className="block text-xs text-red-600 dark:text-red-400">
                          overdue
                        </span>
                      )}
                    </TableCell>
                    <TableCell>
                      <PurchaseInvoiceStatusBadge status={invoice.status} />
                    </TableCell>
                    <TableCell className="text-right text-sm text-muted-foreground">
                      {formatAmount(invoice.totalAmount)}
                    </TableCell>
                    <TableCell className="text-right text-sm font-medium">
                      {Number(invoice.balanceAmount) > 0 ? (
                        <span className="text-amber-600 dark:text-amber-400">
                          {formatMoney(invoice.balanceAmount, invoice.currency)}
                        </span>
                      ) : (
                        formatAmount(invoice.balanceAmount)
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
                          <DropdownMenuItem onClick={() => setDetailsId(invoice.id)}>
                            <Eye className="mr-2 size-4 text-muted-foreground" />
                            View details
                          </DropdownMenuItem>
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </TableCell>
                  </TableRow>
                );
              })
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
          itemLabel="invoices"
          isFetching={isFetching}
          trailingNote={`balance on this page ${formatCurrencyTotals(pageBalanceTotals)}`}
        />
      </div>

      <PurchaseInvoiceDetailsSheet
        invoiceId={detailsId}
        open={detailsId !== null}
        onOpenChange={(open) => {
          if (!open) setDetailsId(null);
        }}
      />
    </div>
  );
}
