"use client";

import * as React from "react";
import { useSearchParams, useRouter } from "next/navigation";
import { Plus, MoreVertical, Eye, Pencil, Check, X, Lock, Truck, Receipt } from "lucide-react";
import {
  useGetSalesOrdersQuery,
  useConfirmSalesOrderMutation,
  useCancelSalesOrderMutation,
  useCloseSalesOrderMutation,
  type SalesOrder,
} from "@/services/sales-order.service";
import { useGetInvoicedOrderIdsQuery } from "@/services/sales-invoice.service";
import { ListPageHeader } from "@/components/common/list-page-header";
import { ListPagination } from "@/components/common/list-pagination";
import { SalesOrderStatusBadge } from "@/components/sales/sales-status-badge";
import { CreateSalesOrderDialog } from "@/components/sales/create-sales-order-dialog";
import { EditSalesOrderDialog } from "@/components/sales/edit-sales-order-dialog";
import { CreateDeliveryDialog } from "@/components/sales/create-delivery-dialog";
import { CreateInvoiceDialog } from "@/components/sales/create-invoice-dialog";
import { SalesDetailsSheet, type DetailField } from "@/components/sales/sales-details-sheet";
import { formatMoney, formatDate, formatQty } from "@/lib/sales-format";
import { Button } from "@/components/ui/button";
import {
  Table, TableBody, TableCell, TableHead, TableHeader, TableRow,
} from "@/components/ui/table";
import {
  DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { TableSkeletonRows, TableEmptyRow } from "@/components/common/table-state";

export default function SalesOrdersPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const page = Number(searchParams.get("page") ?? "0");
  const [createOpen, setCreateOpen] = React.useState(false);
  const [editOrder, setEditOrder] = React.useState<SalesOrder | null>(null);
  const [deliveryOrder, setDeliveryOrder] = React.useState<SalesOrder | null>(null);
  const [invoiceOrder, setInvoiceOrder] = React.useState<SalesOrder | null>(null);
  const [selected, setSelected] = React.useState<SalesOrder | null>(null);

  const { data, isLoading, isFetching } = useGetSalesOrdersQuery({ page, size: 20 });
  const { data: invoicedOrderIds } = useGetInvoicedOrderIdsQuery();
  const [confirm] = useConfirmSalesOrderMutation();
  const [cancel] = useCancelSalesOrderMutation();
  const [close] = useCloseSalesOrderMutation();

  const invoicedSet = React.useMemo(() => new Set(invoicedOrderIds ?? []), [invoicedOrderIds]);

  const orders = data?.content ?? [];

  const fields: DetailField[] = selected
    ? [
        { label: "Customer", value: `${selected.customerName} (${selected.customerCode})` },
        { label: "Order date", value: formatDate(selected.orderDate) },
        { label: "Expected delivery", value: formatDate(selected.expectedDeliveryDate) },
        { label: "Currency", value: selected.currency },
        { label: "Subtotal", value: formatMoney(selected.subtotalAmount, selected.currency) },
        { label: "VAT", value: formatMoney(selected.taxAmount, selected.currency) },
        { label: "Total", value: formatMoney(selected.totalAmount, selected.currency) },
        { label: "Delivered", value: formatMoney(selected.deliveredAmount, selected.currency) },
        { label: "Reference", value: selected.reference },
        { label: "Notes", value: selected.notes, full: true },
      ]
    : [];

  return (
    <div className="space-y-4 p-4">
      <ListPageHeader
        title="Sales Orders"
        description="Commitments to customers."
        count={data?.totalElements}
        countLabel="Orders"
        actions={<Button onClick={() => setCreateOpen(true)}><Plus className="mr-1 h-4 w-4" />New Order</Button>}
      />

      <div className="rounded-md border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>SO #</TableHead>
              <TableHead>Customer</TableHead>
              <TableHead>Date</TableHead>
              <TableHead>Status</TableHead>
              <TableHead className="text-right">Total</TableHead>
              <TableHead className="w-10" />
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading || isFetching ? (
              <TableSkeletonRows columns={6} />
            ) : orders.length === 0 ? (
              <TableEmptyRow columns={6} title="No results" />
            ) : (
              orders.map((o) => (
                <TableRow key={o.id}>
                  <TableCell className="font-medium">{o.soNumber}</TableCell>
                  <TableCell>{o.customerName}</TableCell>
                  <TableCell>{formatDate(o.orderDate)}</TableCell>
                  <TableCell><SalesOrderStatusBadge status={o.status} /></TableCell>
                  <TableCell className="text-right">{formatMoney(o.totalAmount, o.currency)}</TableCell>
                  <TableCell>
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button variant="ghost" size="icon"><MoreVertical className="h-4 w-4" /></Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end">
                        <DropdownMenuItem onClick={() => setSelected(o)}><Eye className="mr-2 h-4 w-4" />View</DropdownMenuItem>
                        {o.status === "DRAFT" && (
                          <DropdownMenuItem onClick={() => setEditOrder(o)}><Pencil className="mr-2 h-4 w-4" />Edit</DropdownMenuItem>
                        )}
                        {o.status === "DRAFT" && (
                          <DropdownMenuItem onClick={() => confirm(o.id)}><Check className="mr-2 h-4 w-4" />Confirm</DropdownMenuItem>
                        )}
                        {(o.status === "CONFIRMED" || o.status === "PARTIALLY_DELIVERED") && (
                          <>
                            {o.lines.some((l) => l.outstandingQuantity > 0) && (
                              <DropdownMenuItem onClick={() => setDeliveryOrder(o)}>
                                <Truck className="mr-2 h-4 w-4" />Create Delivery
                              </DropdownMenuItem>
                            )}
                            {!invoicedSet.has(o.id) && (
                              <DropdownMenuItem onClick={() => setInvoiceOrder(o)}>
                                <Receipt className="mr-2 h-4 w-4" />Create Invoice
                              </DropdownMenuItem>
                            )}
                          </>
                        )}
                        {(o.status === "DRAFT" || o.status === "CONFIRMED") && (
                          <DropdownMenuItem onClick={() => cancel({ id: o.id, data: { reason: "Cancelled" } })}><X className="mr-2 h-4 w-4" />Cancel</DropdownMenuItem>
                        )}
                        {(o.status === "DELIVERED" || o.status === "CONFIRMED") && (
                          <DropdownMenuItem onClick={() => close(o.id)}><Lock className="mr-2 h-4 w-4" />Close</DropdownMenuItem>
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
        totalElements={data?.totalElements ?? 0} itemLabel="orders"
        onPageChange={(p) => router.push(`/sales-orders?page=${p}`)}
      />

      <CreateSalesOrderDialog open={createOpen} onOpenChange={setCreateOpen} />
      <EditSalesOrderDialog order={editOrder} open={!!editOrder} onOpenChange={(open) => !open && setEditOrder(null)} />
      <CreateDeliveryDialog
        order={deliveryOrder}
        open={!!deliveryOrder}
        onOpenChange={(open) => !open && setDeliveryOrder(null)}
        onSuccess={() => router.push("/deliveries")}
      />
      <CreateInvoiceDialog
        order={invoiceOrder}
        open={!!invoiceOrder}
        onOpenChange={(open) => !open && setInvoiceOrder(null)}
        onSuccess={() => router.push("/sales-invoices")}
      />
      <SalesDetailsSheet
        open={!!selected}
        onOpenChange={(o) => !o && setSelected(null)}
        title={selected ? `Order ${selected.soNumber}` : ""}
        subtitle="Sales order details"
        fields={fields}
        lines={selected ? {
          headers: ["#", "Item", "Qty", "Price", "Amount"],
          rows: selected.lines.map((l) => ({
            key: String(l.id),
            cells: [l.lineNumber, `${l.itemCode} — ${l.itemName}`, formatQty(l.quantity), formatMoney(l.unitPrice, selected.currency), formatMoney(l.lineAmount, selected.currency)],
          })),
        } : undefined}
      />
    </div>
  );
}
