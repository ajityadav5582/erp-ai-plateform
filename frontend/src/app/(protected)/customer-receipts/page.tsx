"use client";

import * as React from "react";
import { useSearchParams, useRouter } from "next/navigation";
import { MoreVertical, Eye, X, Plus, Receipt, Coins } from "lucide-react";
import {
  useGetReceiptsQuery,
  useCancelReceiptMutation,
  type CustomerReceipt,
} from "@/services/customer-receipt.service";
import { PAYMENT_METHOD_LABELS } from "@/services/customer-receipt.service";
import { toast } from "sonner";
import { getApiErrorMessage } from "@/services/error-handler";
import {
  useGetOutstandingInvoicesQuery,
  type SalesInvoice,
} from "@/services/sales-invoice.service";
import { ListPageHeader } from "@/components/common/list-page-header";
import { ListPagination } from "@/components/common/list-pagination";
import { ReceiptStatusBadge, SalesInvoiceStatusBadge } from "@/components/sales/sales-status-badge";
import { SalesDetailsSheet, type DetailField } from "@/components/sales/sales-details-sheet";
import { CreateReceiptDialog } from "@/components/sales/create-receipt-dialog";
import { formatMoney, formatDate } from "@/lib/sales-format";
import { Button } from "@/components/ui/button";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  Table, TableBody, TableCell, TableHead, TableHeader, TableRow,
} from "@/components/ui/table";
import {
  DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { TableSkeletonRows, TableEmptyRow } from "@/components/common/table-state";

export default function CustomerReceiptsPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const page = Number(searchParams.get("page") ?? "0");
  const [activeTab, setActiveTab] = React.useState<"receipts" | "outstanding">("receipts");
  const [selected, setSelected] = React.useState<CustomerReceipt | null>(null);
  const [createOpen, setCreateOpen] = React.useState(false);
  const [selectedInvoice, setSelectedInvoice] = React.useState<SalesInvoice | null>(null);

  const { data, isLoading, isFetching } = useGetReceiptsQuery({ page, size: 20, sort: "createdAt,desc" });
  const { data: outstandingInvoices, isLoading: outstandingLoading } =
    useGetOutstandingInvoicesQuery();
  const [cancel] = useCancelReceiptMutation();

  const handleCancelReceipt = async (id: number) => {
    const reason = window.prompt("Reason for voiding / cancelling this receipt (mandatory):");
    if (reason === null) return;
    try {
      await cancel({ id, data: { reason } }).unwrap();
      toast.success("Receipt cancelled successfully");
      setSelected(null);
    } catch (err) {
      toast.error("Failed to cancel receipt", {
        description: getApiErrorMessage(err),
      });
    }
  };

  const rows = data?.content ?? [];
  const unpaidInvoices = outstandingInvoices ?? [];

  const fields: DetailField[] = selected
    ? [
        { label: "Invoice", value: selected.invoiceNumber },
        { label: "Customer", value: `${selected.customerName} (${selected.customerCode})` },
        { label: "Amount", value: formatMoney(selected.amount) },
        { label: "Method", value: PAYMENT_METHOD_LABELS[selected.paymentMethod] },
        { label: "Receipt date", value: formatDate(selected.receiptDate) },
        { label: "Reference", value: selected.reference },
        { label: "Notes", value: selected.notes, full: true },
      ]
    : [];

  return (
    <div className="space-y-4 p-4">
      <ListPageHeader
        title="Customer Receipts"
        description="Money received against issued tax invoices."
        count={activeTab === "receipts" ? data?.totalElements : unpaidInvoices.length}
        countLabel={activeTab === "receipts" ? "Receipts" : "Outstanding Invoices"}
        actions={
          <Button onClick={() => { setSelectedInvoice(null); setCreateOpen(true); }}>
            <Plus className="mr-1 h-4 w-4" />New Receipt
          </Button>
        }
      />

      <div className="flex items-center justify-between">
        <Tabs value={activeTab} onValueChange={(v) => setActiveTab(v as "receipts" | "outstanding")}>
          <TabsList>
            <TabsTrigger value="receipts">
              All Receipts ({data?.totalElements ?? 0})
            </TabsTrigger>
            <TabsTrigger value="outstanding" className="flex items-center gap-1.5">
              <Coins className="h-3.5 w-3.5" />
              Invoices Awaiting Payment ({unpaidInvoices.length})
            </TabsTrigger>
          </TabsList>
        </Tabs>
      </div>

      {activeTab === "receipts" ? (
        <>
          <div className="rounded-md border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Receipt #</TableHead>
                  <TableHead>Invoice</TableHead>
                  <TableHead>Customer</TableHead>
                  <TableHead>Date</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead className="text-right">Amount</TableHead>
                  <TableHead className="w-10" />
                </TableRow>
              </TableHeader>
              <TableBody>
                {isLoading || isFetching ? (
                  <TableSkeletonRows columns={7} />
                ) : rows.length === 0 ? (
                  <TableEmptyRow
                    columns={7}
                    title="No receipts yet"
                    description={
                      unpaidInvoices.length > 0
                        ? `You have ${unpaidInvoices.length} issued invoice(s) awaiting payment. Click 'Invoices Awaiting Payment' tab or 'New Receipt' to record collections.`
                        : "Issued tax invoices will show here once customer payments are recorded."
                    }
                  />
                ) : (
                  rows.map((r) => (
                    <TableRow key={r.id}>
                      <TableCell className="font-medium">{r.receiptNumber}</TableCell>
                      <TableCell>{r.invoiceNumber}</TableCell>
                      <TableCell>{r.customerName}</TableCell>
                      <TableCell>{formatDate(r.receiptDate)}</TableCell>
                      <TableCell><ReceiptStatusBadge status={r.status} /></TableCell>
                      <TableCell className="text-right">{formatMoney(r.amount)}</TableCell>
                      <TableCell>
                        <DropdownMenu>
                          <DropdownMenuTrigger asChild>
                            <Button variant="ghost" size="icon"><MoreVertical className="h-4 w-4" /></Button>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent align="end">
                            <DropdownMenuItem onClick={() => setSelected(r)}><Eye className="mr-2 h-4 w-4" />View</DropdownMenuItem>
                            {r.status === "POSTED" && (
                              <DropdownMenuItem className="text-red-600" onClick={() => handleCancelReceipt(r.id)}><X className="mr-2 h-4 w-4" />Cancel</DropdownMenuItem>
                            )}
                          </DropdownMenuContent>
                        </DropdownMenu>
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </div>

          <ListPagination
            page={page}
            pageSize={20}
            totalPages={data?.totalPages ?? 0}
            totalElements={data?.totalElements ?? 0}
            itemLabel="receipts"
            onPageChange={(p) => router.push(`/customer-receipts?page=${p}`)}
          />
        </>
      ) : (
        <div className="rounded-md border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Invoice #</TableHead>
                <TableHead>Customer</TableHead>
                <TableHead>Date (BS)</TableHead>
                <TableHead>Status</TableHead>
                <TableHead className="text-right">Total Invoice</TableHead>
                <TableHead className="text-right">Balance Due</TableHead>
                <TableHead className="text-right">Action</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {outstandingLoading ? (
                <TableSkeletonRows columns={7} />
              ) : unpaidInvoices.length === 0 ? (
                <TableEmptyRow
                  columns={7}
                  title="No unpaid invoices"
                  description="All issued invoices have been fully settled or no issued invoices exist yet."
                />
              ) : (
                unpaidInvoices.map((inv) => (
                  <TableRow key={inv.id}>
                    <TableCell className="font-medium">{inv.invoiceNumber}</TableCell>
                    <TableCell>{inv.customerName} ({inv.customerCode})</TableCell>
                    <TableCell>{inv.invoiceDateNp}</TableCell>
                    <TableCell><SalesInvoiceStatusBadge status={inv.status} /></TableCell>
                    <TableCell className="text-right">{formatMoney(inv.totalAmount, inv.currency)}</TableCell>
                    <TableCell className="text-right font-medium text-emerald-700 dark:text-emerald-400">
                      {formatMoney(inv.balanceAmount, inv.currency)}
                    </TableCell>
                    <TableCell className="text-right">
                      <Button
                        size="sm"
                        onClick={() => {
                          setSelectedInvoice(inv);
                          setCreateOpen(true);
                        }}
                      >
                        <Receipt className="mr-1.5 h-3.5 w-3.5" />Receive Payment
                      </Button>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </div>
      )}

      <CreateReceiptDialog
        invoice={selectedInvoice}
        open={createOpen}
        onOpenChange={(open) => {
          setCreateOpen(open);
          if (!open) setSelectedInvoice(null);
        }}
        onSuccess={() => setActiveTab("receipts")}
      />

      <SalesDetailsSheet
        open={!!selected}
        onOpenChange={(o) => !o && setSelected(null)}
        title={selected ? `Receipt ${selected.receiptNumber}` : ""}
        subtitle={selected ? `${selected.customerName} (${selected.customerCode})` : "Customer receipt"}
        badge={selected ? <ReceiptStatusBadge status={selected.status} /> : null}
        fields={fields}
        actions={
          selected && selected.status === "POSTED" ? (
            <Button
              variant="outline"
              size="sm"
              className="text-red-600 hover:text-red-700 hover:bg-red-50 ml-auto"
              onClick={() => handleCancelReceipt(selected.id)}
            >
              <X className="mr-1.5 h-3.5 w-3.5" />Cancel Receipt
            </Button>
          ) : undefined
        }
      />
    </div>
  );
}
