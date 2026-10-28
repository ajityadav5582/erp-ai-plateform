"use client";

import * as React from "react";
import { useSearchParams, useRouter } from "next/navigation";
import { MoreVertical, Eye, Send, X, Plus, Receipt, Coins, Printer, Undo } from "lucide-react";
import {
  useGetInvoicesQuery,
  useGetInvoicedOrderIdsQuery,
  useIssueInvoiceMutation,
  useCancelInvoiceMutation,
  type SalesInvoice,
} from "@/services/sales-invoice.service";
import { toast } from "sonner";
import { getApiErrorMessage } from "@/services/error-handler";
import {
  useGetOpenSalesOrdersQuery,
  type SalesOrder,
} from "@/services/sales-order.service";
import { ListPageHeader } from "@/components/common/list-page-header";
import { ListPagination } from "@/components/common/list-pagination";
import { SalesInvoiceStatusBadge, SalesOrderStatusBadge } from "@/components/sales/sales-status-badge";
import { SalesDetailsSheet, type DetailField } from "@/components/sales/sales-details-sheet";
import { CreateInvoiceDialog } from "@/components/sales/create-invoice-dialog";
import { CreateReceiptDialog } from "@/components/sales/create-receipt-dialog";
import { TaxInvoiceViewDialog } from "@/components/sales/tax-invoice-view-dialog";
import { CreateReturnDialog } from "@/components/sales/create-return-dialog";
import { formatMoney, formatDate } from "@/lib/sales-format";
import { Button } from "@/components/ui/button";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  Table, TableBody, TableCell, TableHead, TableHeader, TableRow,
} from "@/components/ui/table";
import {
  DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger, DropdownMenuSeparator,
} from "@/components/ui/dropdown-menu";
import { TableSkeletonRows, TableEmptyRow } from "@/components/common/table-state";

export default function SalesInvoicesPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const page = Number(searchParams.get("page") ?? "0");
  const [activeTab, setActiveTab] = React.useState<"invoices" | "pending">("invoices");
  const [selected, setSelected] = React.useState<SalesInvoice | null>(null);
  const [createOpen, setCreateOpen] = React.useState(false);
  const [selectedOrder, setSelectedOrder] = React.useState<SalesOrder | null>(null);
  const [receiptOpen, setReceiptOpen] = React.useState(false);
  const [receiptInvoice, setReceiptInvoice] = React.useState<SalesInvoice | null>(null);
  const [viewInvoiceId, setViewInvoiceId] = React.useState<number | null>(null);
  const [returnOpen, setReturnOpen] = React.useState(false);
  const [returnInvoice, setReturnInvoice] = React.useState<SalesInvoice | null>(null);

  const { data, isLoading, isFetching } = useGetInvoicesQuery({ page, size: 20, sort: "createdAt,desc" });
  const { data: openOrders, isLoading: ordersLoading } = useGetOpenSalesOrdersQuery();
  const { data: invoicedOrderIds } = useGetInvoicedOrderIdsQuery();
  const [issue] = useIssueInvoiceMutation();
  const [cancel] = useCancelInvoiceMutation();

  const handleIssueInvoice = async (id: number) => {
    try {
      await issue(id).unwrap();
      toast.success("Tax invoice issued successfully and queued for IRD CBMS sync");
      if (selected?.id === id) setSelected(null);
    } catch (err) {
      toast.error("Cannot issue invoice", { description: getApiErrorMessage(err) });
    }
  };

  const handleCancelInvoice = async (id: number) => {
    try {
      await cancel({ id, data: { reason: "Cancelled by user" } }).unwrap();
      toast.success("Tax invoice cancelled");
      if (selected?.id === id) setSelected(null);
    } catch (err) {
      toast.error("Cannot cancel invoice", { description: getApiErrorMessage(err) });
    }
  };

  const invoices = data?.content ?? [];
  const invoicedSet = React.useMemo(() => new Set(invoicedOrderIds ?? []), [invoicedOrderIds]);
  const pendingOrders = React.useMemo(() => {
    return openOrders ?? [];
  }, [openOrders]);

  const fields: DetailField[] = selected
    ? [
        { label: "Document", value: selected.isTaxInvoice ? "Tax Invoice" : "Invoice" },
        { label: "Customer", value: `${selected.customerName} (${selected.customerCode})` },
        { label: "Buyer PAN", value: selected.customerPanNumber ?? "—" },
        { label: "Invoice date (AD)", value: formatDate(selected.invoiceDate) },
        { label: "Invoice date (BS)", value: selected.invoiceDateNp },
        { label: "Sales order", value: selected.soNumber },
        { label: "Taxable", value: formatMoney(selected.taxableAmount, selected.currency) },
        { label: "Exempt", value: formatMoney(selected.exemptAmount, selected.currency) },
        { label: "Zero-rated", value: formatMoney(selected.zeroRatedAmount, selected.currency) },
        { label: "VAT", value: formatMoney(selected.vatAmount, selected.currency) },
        { label: "Total", value: formatMoney(selected.totalAmount, selected.currency) },
        { label: "Balance", value: formatMoney(selected.balanceAmount, selected.currency) },
      ]
    : [];

  return (
    <div className="space-y-4 p-4">
      <ListPageHeader
        title="Sales Invoices"
        description="IRD tax invoices issued to customers against confirmed sales orders."
        count={activeTab === "invoices" ? data?.totalElements : pendingOrders.length}
        countLabel={activeTab === "invoices" ? "Invoices" : "Pending Orders"}
        actions={
          <Button onClick={() => { setSelectedOrder(null); setCreateOpen(true); }}>
            <Plus className="mr-1 h-4 w-4" />New Invoice
          </Button>
        }
      />

      <div className="flex items-center justify-between">
        <Tabs value={activeTab} onValueChange={(v) => setActiveTab(v as "invoices" | "pending")}>
          <TabsList>
            <TabsTrigger value="invoices">
              All Invoices ({data?.totalElements ?? 0})
            </TabsTrigger>
            <TabsTrigger value="pending" className="flex items-center gap-1.5">
              <Receipt className="h-3.5 w-3.5" />
              Orders Ready for Invoicing ({pendingOrders.length})
            </TabsTrigger>
          </TabsList>
        </Tabs>
      </div>

      {activeTab === "invoices" ? (
        <>
          <div className="rounded-md border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Invoice #</TableHead>
                  <TableHead>Customer</TableHead>
                  <TableHead>Date (BS)</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead className="text-right">Total</TableHead>
                  <TableHead className="text-right">Balance</TableHead>
                  <TableHead className="w-10" />
                </TableRow>
              </TableHeader>
              <TableBody>
                {isLoading || isFetching ? (
                  <TableSkeletonRows columns={7} />
                ) : invoices.length === 0 ? (
                  <TableEmptyRow
                    columns={7}
                    title="No invoices yet"
                    description={
                      pendingOrders.length > 0
                        ? `You have ${pendingOrders.length} confirmed order(s) waiting to be invoiced. Click 'Orders Ready for Invoicing' tab or 'New Invoice' to generate a tax invoice.`
                        : "Confirmed sales orders appear here once tax invoices are generated."
                    }
                  />
                ) : (
                  invoices.map((i) => (
                    <TableRow key={i.id}>
                      <TableCell className="font-medium">{i.invoiceNumber}</TableCell>
                      <TableCell>{i.customerName}</TableCell>
                      <TableCell>{i.invoiceDateNp}</TableCell>
                      <TableCell><SalesInvoiceStatusBadge status={i.status} /></TableCell>
                      <TableCell className="text-right">{formatMoney(i.totalAmount, i.currency)}</TableCell>
                      <TableCell className="text-right">{formatMoney(i.balanceAmount, i.currency)}</TableCell>
                      <TableCell>
                        <DropdownMenu>
                          <DropdownMenuTrigger asChild>
                            <Button variant="ghost" size="icon"><MoreVertical className="h-4 w-4" /></Button>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent align="end">
                            <DropdownMenuItem onClick={() => setViewInvoiceId(i.id)}>
                              <Printer className="mr-2 h-4 w-4" />Print Tax Invoice (अनुसूची ५)
                            </DropdownMenuItem>
                            <DropdownMenuItem onClick={() => setSelected(i)}>
                              <Eye className="mr-2 h-4 w-4" />View Details
                            </DropdownMenuItem>
                            {i.status === "DRAFT" && (
                              <DropdownMenuItem onClick={() => handleIssueInvoice(i.id)}>
                                <Send className="mr-2 h-4 w-4 text-emerald-600" />Issue
                              </DropdownMenuItem>
                            )}
                            {(i.status === "ISSUED" || i.status === "PARTIALLY_PAID") && i.balanceAmount > 0 && (
                              <DropdownMenuItem onClick={() => { setReceiptInvoice(i); setReceiptOpen(true); }}>
                                <Coins className="mr-2 h-4 w-4 text-primary" />Record Payment
                              </DropdownMenuItem>
                            )}
                            {(i.status === "ISSUED" || i.status === "PARTIALLY_PAID" || i.status === "PAID") && (
                              <DropdownMenuItem onClick={() => { setReturnInvoice(i); setReturnOpen(true); }}>
                                <Undo className="mr-2 h-4 w-4 text-amber-600" />Sales Return (क्रेडिट नोट)
                              </DropdownMenuItem>
                            )}
                            {i.status !== "CANCELLED" && i.status !== "PAID" && (
                              <>
                                <DropdownMenuSeparator />
                                <DropdownMenuItem className="text-red-600" onClick={() => handleCancelInvoice(i.id)}>
                                  <X className="mr-2 h-4 w-4" />Cancel
                                </DropdownMenuItem>
                              </>
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
            itemLabel="invoices"
            onPageChange={(p) => router.push(`/sales-invoices?page=${p}`)}
          />
        </>
      ) : (
        <div className="rounded-md border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>SO #</TableHead>
                <TableHead>Customer</TableHead>
                <TableHead>Order Date</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Billing State</TableHead>
                <TableHead>Items</TableHead>
                <TableHead className="text-right">Total Order</TableHead>
                <TableHead className="text-right">Action</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {ordersLoading ? (
                <TableSkeletonRows columns={8} />
              ) : pendingOrders.length === 0 ? (
                <TableEmptyRow
                  columns={8}
                  title="No orders awaiting invoicing"
                  description="When sales orders are confirmed, they will show up here ready for legal tax invoicing."
                />
              ) : (
                pendingOrders.map((o) => (
                  <TableRow key={o.id}>
                    <TableCell className="font-medium">{o.soNumber}</TableCell>
                    <TableCell>{o.customerName} ({o.customerCode})</TableCell>
                    <TableCell>{formatDate(o.orderDate)}</TableCell>
                    <TableCell><SalesOrderStatusBadge status={o.status} /></TableCell>
                    <TableCell>
                      {invoicedSet.has(o.id) ? (
                        <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-blue-50 text-blue-700 border border-blue-200">
                          Partially Invoiced
                        </span>
                      ) : (
                        <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-amber-50 text-amber-700 border border-amber-200">
                          Ready to Bill
                        </span>
                      )}
                    </TableCell>
                    <TableCell>{o.lines.length} line(s)</TableCell>
                    <TableCell className="text-right font-medium">{formatMoney(o.totalAmount, o.currency)}</TableCell>
                    <TableCell className="text-right">
                      <Button
                        size="sm"
                        onClick={() => {
                          setSelectedOrder(o);
                          setCreateOpen(true);
                        }}
                      >
                        <Receipt className="mr-1.5 h-3.5 w-3.5" />Create Invoice
                      </Button>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </div>
      )}

      <CreateInvoiceDialog
        order={selectedOrder}
        open={createOpen}
        onOpenChange={(open) => {
          setCreateOpen(open);
          if (!open) setSelectedOrder(null);
        }}
        onSuccess={() => setActiveTab("invoices")}
      />

      <CreateReceiptDialog
        invoice={receiptInvoice}
        open={receiptOpen}
        onOpenChange={(open) => {
          setReceiptOpen(open);
          if (!open) setReceiptInvoice(null);
        }}
        onSuccess={() => setActiveTab("invoices")}
      />

      <TaxInvoiceViewDialog
        invoiceId={viewInvoiceId}
        open={!!viewInvoiceId}
        onOpenChange={(open) => !open && setViewInvoiceId(null)}
      />

      <CreateReturnDialog
        invoice={returnInvoice}
        open={returnOpen}
        onOpenChange={(open) => {
          setReturnOpen(open);
          if (!open) setReturnInvoice(null);
        }}
        onSuccess={() => setActiveTab("invoices")}
      />

      <SalesDetailsSheet
        open={!!selected}
        onOpenChange={(o) => !o && setSelected(null)}
        title={selected ? `Invoice ${selected.invoiceNumber}` : ""}
        subtitle={selected ? `${selected.customerName} (${selected.customerCode})` : "IRD tax invoice"}
        badge={selected ? <SalesInvoiceStatusBadge status={selected.status} /> : null}
        fields={fields}
        lines={selected ? {
          headers: ["Item", "VAT", "Net", "VAT amt"],
          rows: selected.lines.map((l) => ({
            key: String(l.id),
            cells: [`${l.itemCode} — ${l.itemName}`, l.vatType, formatMoney(l.lineAmount, selected.currency), formatMoney(l.vatAmount, selected.currency)],
          })),
        } : undefined}
        actions={
          selected ? (
            <div className="flex w-full items-center justify-between gap-2">
              {selected.status !== "CANCELLED" && selected.status !== "PAID" && (
                <Button
                  variant="outline"
                  size="sm"
                  className="text-red-600 hover:text-red-700 hover:bg-red-50"
                  onClick={() => handleCancelInvoice(selected.id)}
                >
                  <X className="mr-1.5 h-3.5 w-3.5" />Cancel Invoice
                </Button>
              )}
              <div className="flex items-center gap-2 ml-auto">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setViewInvoiceId(selected.id)}
                >
                  <Printer className="mr-1.5 h-3.5 w-3.5" />Print Invoice
                </Button>
                {selected.status === "DRAFT" && (
                  <Button size="sm" onClick={() => handleIssueInvoice(selected.id)}>
                    <Send className="mr-1.5 h-3.5 w-3.5" />Issue Invoice
                  </Button>
                )}
                {(selected.status === "ISSUED" || selected.status === "PARTIALLY_PAID") && selected.balanceAmount > 0 && (
                  <Button
                    size="sm"
                    onClick={() => {
                      setReceiptInvoice(selected);
                      setReceiptOpen(true);
                      setSelected(null);
                    }}
                  >
                    <Coins className="mr-1.5 h-3.5 w-3.5" />Record Payment
                  </Button>
                )}
              </div>
            </div>
          ) : undefined
        }
      />
    </div>
  );
}
