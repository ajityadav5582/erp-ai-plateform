"use client";

import * as React from "react";
import { useSearchParams, useRouter } from "next/navigation";
import {
  MoreVertical,
  Eye,
  Send,
  X,
  Plus,
  Truck,
  Printer,
  FileText,
  PackageCheck,
  ArrowRight,
} from "lucide-react";
import { toast } from "sonner";
import { getApiErrorMessage } from "@/services/error-handler";
import {
  useGetDeliveriesQuery,
  usePostDeliveryMutation,
  useCancelDeliveryMutation,
  type SalesDelivery,
} from "@/services/sales-delivery.service";
import {
  useGetOpenSalesOrdersQuery,
  useLazyGetSalesOrderQuery,
  type SalesOrder,
} from "@/services/sales-order.service";
import { ListPageHeader } from "@/components/common/list-page-header";
import { ListPagination } from "@/components/common/list-pagination";
import { DeliveryStatusBadge, SalesOrderStatusBadge } from "@/components/sales/sales-status-badge";
import { SalesDetailsSheet, type DetailField } from "@/components/sales/sales-details-sheet";
import { CreateDeliveryDialog } from "@/components/sales/create-delivery-dialog";
import { CreateInvoiceDialog } from "@/components/sales/create-invoice-dialog";
import { DeliveryChallanViewDialog } from "@/components/sales/delivery-challan-view-dialog";
import { formatDate, formatMoney, formatQty } from "@/lib/sales-format";
import { Button } from "@/components/ui/button";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  Table, TableBody, TableCell, TableHead, TableHeader, TableRow,
} from "@/components/ui/table";
import {
  DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger, DropdownMenuSeparator,
} from "@/components/ui/dropdown-menu";
import { TableSkeletonRows, TableEmptyRow } from "@/components/common/table-state";

export default function DeliveriesPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const page = Number(searchParams.get("page") ?? "0");
  const [activeTab, setActiveTab] = React.useState<"deliveries" | "pending">("deliveries");
  const [selected, setSelected] = React.useState<SalesDelivery | null>(null);
  const [createOpen, setCreateOpen] = React.useState(false);
  const [selectedOrder, setSelectedOrder] = React.useState<SalesOrder | null>(null);

  // Challan & Invoicing shortcuts
  const [challanDelivery, setChallanDelivery] = React.useState<SalesDelivery | null>(null);
  const [invoiceOrder, setInvoiceOrder] = React.useState<SalesOrder | null>(null);
  const [invoiceOpen, setInvoiceOpen] = React.useState(false);

  const { data, isLoading, isFetching } = useGetDeliveriesQuery({ page, size: 20, sort: "createdAt,desc" });
  const { data: openOrders, isLoading: ordersLoading } = useGetOpenSalesOrdersQuery();
  const [getOrderTrigger] = useLazyGetSalesOrderQuery();
  const [post] = usePostDeliveryMutation();
  const [cancel] = useCancelDeliveryMutation();

  const handlePostDelivery = async (id: number) => {
    try {
      await post(id).unwrap();
      toast.success("Delivery posted: Stock relieved from warehouse");
      if (selected?.id === id) setSelected(null);
    } catch (err) {
      toast.error("Cannot post delivery", { description: getApiErrorMessage(err) });
    }
  };

  const handleCancelDelivery = async (id: number) => {
    try {
      await cancel({ id, data: { reason: "Cancelled by user" } }).unwrap();
      toast.success("Delivery cancelled: Stock movement reversed");
      if (selected?.id === id) setSelected(null);
    } catch (err) {
      toast.error("Cannot cancel delivery", { description: getApiErrorMessage(err) });
    }
  };

  const rows = data?.content ?? [];
  const pendingOrders = React.useMemo(() => {
    return (openOrders ?? []).filter((o) => o.lines.some((l) => l.outstandingQuantity > 0));
  }, [openOrders]);

  const handleCreateInvoiceFromDelivery = async (delivery: SalesDelivery) => {
    try {
      const res = await getOrderTrigger(delivery.salesOrderId).unwrap();
      setInvoiceOrder(res);
      setInvoiceOpen(true);
    } catch (e) {
      console.error("Failed to fetch order for invoice", e);
    }
  };

  const fields: DetailField[] = selected
    ? [
        { label: "Sales order", value: selected.soNumber },
        { label: "Customer", value: `${selected.customerName} (${selected.customerCode})` },
        { label: "Warehouse", value: selected.warehouseName ?? selected.warehouseId },
        { label: "Delivery date", value: formatDate(selected.deliveryDate) },
        {
          label: "Inventory impact",
          value:
            selected.status === "POSTED"
              ? "Physical stock deducted from warehouse ledger"
              : selected.status === "CANCELLED"
              ? "Cancelled (Stock returned)"
              : "Draft (Stock not yet issued)",
        },
        {
          label: "Next action",
          value:
            selected.status === "POSTED"
              ? "Print Delivery Challan (ढुवानी चलानी) for driver & Create Tax Invoice (कर बीजक) for customer billing."
              : selected.status === "DRAFT"
              ? "Click 'Post (issue stock)' once items are dispatched from warehouse."
              : "No further action needed.",
          full: true,
        },
        { label: "Notes", value: selected.notes, full: true },
      ]
    : [];


  return (
    <div className="space-y-4 p-4">
      <ListPageHeader
        title="Deliveries"
        description="Goods issued against confirmed sales orders."
        count={activeTab === "deliveries" ? data?.totalElements : pendingOrders.length}
        countLabel={activeTab === "deliveries" ? "Deliveries" : "Pending Orders"}
        actions={
          <Button onClick={() => { setSelectedOrder(null); setCreateOpen(true); }}>
            <Plus className="mr-1 h-4 w-4" />New Delivery
          </Button>
        }
      />

      <div className="flex items-center justify-between">
        <Tabs value={activeTab} onValueChange={(v) => setActiveTab(v as "deliveries" | "pending")}>
          <TabsList>
            <TabsTrigger value="deliveries">
              All Deliveries ({data?.totalElements ?? 0})
            </TabsTrigger>
            <TabsTrigger value="pending" className="flex items-center gap-1.5">
              <Truck className="h-3.5 w-3.5" />
              Orders Ready for Delivery ({pendingOrders.length})
            </TabsTrigger>
          </TabsList>
        </Tabs>
      </div>

      {activeTab === "deliveries" ? (
        <>
          <div className="rounded-md border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Delivery #</TableHead>
                  <TableHead>Sales order</TableHead>
                  <TableHead>Customer</TableHead>
                  <TableHead>Date</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead className="w-10" />
                </TableRow>
              </TableHeader>
              <TableBody>
                {isLoading || isFetching ? (
                  <TableSkeletonRows columns={6} />
                ) : rows.length === 0 ? (
                  <TableEmptyRow
                    columns={6}
                    title="No deliveries yet"
                    description={
                      pendingOrders.length > 0
                        ? `You have ${pendingOrders.length} confirmed order(s) waiting for delivery. Click 'Orders Ready for Delivery' tab or 'New Delivery' to record a dispatch.`
                        : "Confirmed sales orders appear here once dispatches are recorded."
                    }
                  />
                ) : (
                  rows.map((d) => (
                    <TableRow key={d.id}>
                      <TableCell className="font-medium">{d.deliveryNumber}</TableCell>
                      <TableCell>{d.soNumber}</TableCell>
                      <TableCell>{d.customerName}</TableCell>
                      <TableCell>{formatDate(d.deliveryDate)}</TableCell>
                      <TableCell><DeliveryStatusBadge status={d.status} /></TableCell>
                      <TableCell>
                        <DropdownMenu>
                          <DropdownMenuTrigger asChild>
                            <Button variant="ghost" size="icon"><MoreVertical className="h-4 w-4" /></Button>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent align="end">
                            <DropdownMenuItem onClick={() => setSelected(d)}>
                              <Eye className="mr-2 h-4 w-4" />View Details
                            </DropdownMenuItem>
                            {d.status === "POSTED" && (
                              <>
                                <DropdownMenuItem onClick={() => setChallanDelivery(d)}>
                                  <Printer className="mr-2 h-4 w-4 text-primary" />Print Challan (चलानी)
                                </DropdownMenuItem>
                                <DropdownMenuItem onClick={() => handleCreateInvoiceFromDelivery(d)}>
                                  <FileText className="mr-2 h-4 w-4 text-emerald-600" />Create Tax Invoice (बीजक)
                                </DropdownMenuItem>
                                <DropdownMenuSeparator />
                                <DropdownMenuItem
                                  className="text-red-600"
                                  onClick={() => handleCancelDelivery(d.id)}
                                >
                                  <X className="mr-2 h-4 w-4" />Cancel Delivery
                                </DropdownMenuItem>
                              </>
                            )}
                            {d.status === "DRAFT" && (
                              <DropdownMenuItem onClick={() => handlePostDelivery(d.id)}>
                                <Send className="mr-2 h-4 w-4 text-emerald-600" />Post (issue stock)
                              </DropdownMenuItem>
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
            itemLabel="deliveries"
            onPageChange={(p) => router.push(`/deliveries?page=${p}`)}
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
                <TableHead className="text-right">Total Order</TableHead>
                <TableHead className="text-right">Delivered Value</TableHead>
                <TableHead className="text-right">Action</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {ordersLoading ? (
                <TableSkeletonRows columns={7} />
              ) : pendingOrders.length === 0 ? (
                <TableEmptyRow
                  columns={7}
                  title="No orders awaiting delivery"
                  description="When sales orders are confirmed, they will show up here ready for warehouse fulfillment."
                />
              ) : (
                pendingOrders.map((o) => (
                  <TableRow key={o.id}>
                    <TableCell className="font-medium">{o.soNumber}</TableCell>
                    <TableCell>{o.customerName} ({o.customerCode})</TableCell>
                    <TableCell>{formatDate(o.orderDate)}</TableCell>
                    <TableCell><SalesOrderStatusBadge status={o.status} /></TableCell>
                    <TableCell className="text-right font-medium">{formatMoney(o.totalAmount, o.currency)}</TableCell>
                    <TableCell className="text-right text-muted-foreground">{formatMoney(o.deliveredAmount, o.currency)}</TableCell>
                    <TableCell className="text-right">
                      <Button
                        size="sm"
                        onClick={() => {
                          setSelectedOrder(o);
                          setCreateOpen(true);
                        }}
                      >
                        <Truck className="mr-1.5 h-3.5 w-3.5" />Deliver Goods
                      </Button>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </div>
      )}

      <CreateDeliveryDialog
        order={selectedOrder}
        open={createOpen}
        onOpenChange={(open) => {
          setCreateOpen(open);
          if (!open) setSelectedOrder(null);
        }}
        onSuccess={() => setActiveTab("deliveries")}
      />

      <DeliveryChallanViewDialog
        delivery={challanDelivery}
        open={!!challanDelivery}
        onOpenChange={(o) => !o && setChallanDelivery(null)}
      />

      <CreateInvoiceDialog
        order={invoiceOrder}
        open={invoiceOpen}
        onOpenChange={(o) => {
          setInvoiceOpen(o);
          if (!o) setInvoiceOrder(null);
        }}
        onSuccess={() => router.push("/sales-invoices")}
      />

      <SalesDetailsSheet
        open={!!selected}
        onOpenChange={(o) => !o && setSelected(null)}
        title={selected ? `Delivery ${selected.deliveryNumber}` : ""}
        subtitle={selected ? `${selected.customerName} (${selected.customerCode})` : "Goods issue"}
        badge={selected ? <DeliveryStatusBadge status={selected.status} /> : null}
        fields={fields}
        lines={selected ? {
          headers: ["Item", "Qty"],
          rows: selected.lines.map((l) => ({ key: String(l.id), cells: [`${l.itemCode} — ${l.itemName}`, formatQty(l.quantity)] })),
        } : undefined}
        actions={
          selected ? (
            <div className="flex w-full items-center justify-between gap-2">
              {selected.status === "POSTED" ? (
                <>
                  <Button
                    variant="outline"
                    size="sm"
                    className="text-red-600 hover:text-red-700 hover:bg-red-50"
                    onClick={() => handleCancelDelivery(selected.id)}
                  >
                    <X className="mr-1.5 h-3.5 w-3.5" />Cancel Delivery
                  </Button>
                  <div className="flex items-center gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setChallanDelivery(selected)}
                    >
                      <Printer className="mr-1.5 h-3.5 w-3.5" />Print Challan
                    </Button>
                    <Button
                      size="sm"
                      onClick={() => handleCreateInvoiceFromDelivery(selected)}
                    >
                      <FileText className="mr-1.5 h-3.5 w-3.5" />Create Tax Invoice
                    </Button>
                  </div>
                </>
              ) : selected.status === "DRAFT" ? (
                <Button
                  className="w-full"
                  onClick={() => handlePostDelivery(selected.id)}
                >
                  <Send className="mr-1.5 h-3.5 w-3.5" />Post Delivery (Issue Stock)
                </Button>
              ) : null}
            </div>
          ) : undefined
        }
      />
    </div>
  );
}
