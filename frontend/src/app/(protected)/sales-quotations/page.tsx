"use client";

import * as React from "react";
import { useSearchParams, useRouter } from "next/navigation";
import {
  Plus,
  MoreVertical,
  Eye,
  Send,
  CheckCircle,
  XCircle,
  ArrowRightCircle,
} from "lucide-react";
import {
  useGetQuotationsQuery,
  useSendQuotationMutation,
  useAcceptQuotationMutation,
  useRejectQuotationMutation,
  useConvertToOrderMutation,
  type SalesQuotation,
} from "@/services/sales-quotation.service";
import { toast } from "sonner";
import { getApiErrorMessage } from "@/services/error-handler";
import { ListPageHeader } from "@/components/common/list-page-header";
import { ListPagination } from "@/components/common/list-pagination";
import { SalesQuotationStatusBadge } from "@/components/sales/sales-status-badge";
import { SalesDetailsSheet, type DetailField } from "@/components/sales/sales-details-sheet";
import { CreateQuotationDialog } from "@/components/sales/create-quotation-dialog";
import { formatMoney, formatDate } from "@/lib/sales-format";
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
import { TableSkeletonRows, TableEmptyRow } from "@/components/common/table-state";

export default function SalesQuotationsPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const page = Number(searchParams.get("page") ?? "0");
  const [createOpen, setCreateOpen] = React.useState(false);
  const [selected, setSelected] = React.useState<SalesQuotation | null>(null);

  const { data, isLoading, isFetching } = useGetQuotationsQuery({ page, size: 20, sort: "createdAt,desc" });
  const [sendQuotation] = useSendQuotationMutation();
  const [acceptQuotation] = useAcceptQuotationMutation();
  const [rejectQuotation] = useRejectQuotationMutation();
  const [convertToOrder, { isLoading: isConverting }] = useConvertToOrderMutation();

  const quotations = data?.content ?? [];

  const handleSend = async (id: number) => {
    try {
      await sendQuotation(id).unwrap();
      toast.success("Quotation marked as sent to customer");
      setSelected(null);
    } catch (err) {
      toast.error("Failed to mark quotation as sent", {
        description: getApiErrorMessage(err),
      });
    }
  };

  const handleAccept = async (id: number) => {
    try {
      await acceptQuotation(id).unwrap();
      toast.success("Quotation accepted by customer");
      setSelected(null);
    } catch (err) {
      toast.error("Failed to accept quotation", {
        description: getApiErrorMessage(err),
      });
    }
  };

  const handleReject = async (id: number) => {
    const reason = window.prompt("Reason for rejecting quotation (optional):") ?? "Customer declined";
    try {
      await rejectQuotation({ id, reason }).unwrap();
      toast.success("Quotation marked as rejected");
      setSelected(null);
    } catch (err) {
      toast.error("Failed to reject quotation", {
        description: getApiErrorMessage(err),
      });
    }
  };

  const handleConvert = async (q: SalesQuotation) => {
    try {
      const createdOrder = await convertToOrder(q.id).unwrap();
      toast.success(`Converted to Sales Order ${createdOrder.soNumber}`);
      setSelected(null);
      router.push(`/sales-orders`);
    } catch (err) {
      toast.error("Failed to convert quotation to sales order", {
        description: getApiErrorMessage(err),
      });
    }
  };

  const fields: DetailField[] = selected
    ? [
        { label: "Quotation #", value: selected.quotationNumber },
        { label: "Customer", value: `${selected.customerName} (${selected.customerCode})` },
        { label: "Buyer PAN", value: selected.customerPanNumber ?? "—" },
        { label: "Date", value: formatDate(selected.quotationDate) },
        { label: "Valid Until", value: selected.validUntilDate ? formatDate(selected.validUntilDate) : "—" },
        { label: "Status", value: selected.status },
        { label: "Gross Subtotal", value: formatMoney(selected.subtotalAmount, selected.currency) },
        { label: "Discount", value: formatMoney(selected.discountAmount, selected.currency) },
        { label: "VAT (13%)", value: formatMoney(selected.taxAmount, selected.currency) },
        { label: "Total Amount", value: formatMoney(selected.totalAmount, selected.currency) },
        { label: "Converted Order ID", value: selected.salesOrderId ? `#${selected.salesOrderId}` : "—" },
        { label: "Terms & Conditions", value: selected.termsAndConditions ?? "—", full: true },
        { label: "Remarks", value: selected.notes ?? "—", full: true },
      ]
    : [];

  return (
    <div className="space-y-4 p-4">
      <ListPageHeader
        title="Sales Quotations & Estimates"
        description="Formal price quotes and estimates for enterprise, wholesale, and retail clients before order confirmation."
        count={data?.totalElements}
        countLabel="Quotations"
        actions={
          <Button onClick={() => setCreateOpen(true)}>
            <Plus className="mr-1 h-4 w-4" />New Quotation
          </Button>
        }
      />

      <div className="rounded-md border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Quotation #</TableHead>
              <TableHead>Customer</TableHead>
              <TableHead>Date</TableHead>
              <TableHead>Valid Until</TableHead>
              <TableHead>Status</TableHead>
              <TableHead className="text-right">Total (NPR)</TableHead>
              <TableHead className="w-10" />
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading || isFetching ? (
              <TableSkeletonRows columns={7} />
            ) : quotations.length === 0 ? (
              <TableEmptyRow
                columns={7}
                title="No quotations issued yet"
                description="Raise a quotation or price estimate to share with prospective buyers."
              />
            ) : (
              quotations.map((q) => (
                <TableRow key={q.id}>
                  <TableCell className="font-medium">{q.quotationNumber}</TableCell>
                  <TableCell>
                    {q.customerName}
                    <span className="block text-xs text-muted-foreground">{q.customerCode}</span>
                  </TableCell>
                  <TableCell>{formatDate(q.quotationDate)}</TableCell>
                  <TableCell>{q.validUntilDate ? formatDate(q.validUntilDate) : "—"}</TableCell>
                  <TableCell>
                    <SalesQuotationStatusBadge status={q.status} />
                  </TableCell>
                  <TableCell className="text-right font-medium">
                    {formatMoney(q.totalAmount, q.currency)}
                  </TableCell>
                  <TableCell>
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button variant="ghost" size="icon">
                          <MoreVertical className="h-4 w-4" />
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end">
                        <DropdownMenuItem onClick={() => setSelected(q)}>
                          <Eye className="mr-2 h-4 w-4" />View Details
                        </DropdownMenuItem>

                        {q.status === "DRAFT" && (
                          <DropdownMenuItem onClick={() => handleSend(q.id)}>
                            <Send className="mr-2 h-4 w-4" />Mark as Sent
                          </DropdownMenuItem>
                        )}

                        {(q.status === "DRAFT" || q.status === "SENT") && (
                          <DropdownMenuItem onClick={() => handleAccept(q.id)}>
                            <CheckCircle className="mr-2 h-4 w-4 text-emerald-600" />Mark Accepted
                          </DropdownMenuItem>
                        )}

                        {(q.status === "DRAFT" || q.status === "SENT" || q.status === "ACCEPTED") && (
                          <DropdownMenuItem
                            className="font-medium text-primary"
                            onClick={() => handleConvert(q)}
                            disabled={isConverting}
                          >
                            <ArrowRightCircle className="mr-2 h-4 w-4" />Convert to Sales Order
                          </DropdownMenuItem>
                        )}

                        {q.status !== "CONVERTED" && q.status !== "REJECTED" && (
                          <DropdownMenuItem
                            className="text-red-600"
                            onClick={() => handleReject(q.id)}
                          >
                            <XCircle className="mr-2 h-4 w-4" />Mark Rejected
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
        itemLabel="quotations"
        onPageChange={(p) => router.push(`/sales-quotations?page=${p}`)}
      />

      <CreateQuotationDialog
        open={createOpen}
        onOpenChange={setCreateOpen}
      />

      <SalesDetailsSheet
        open={!!selected}
        onOpenChange={(o) => !o && setSelected(null)}
        title={selected ? `Quotation ${selected.quotationNumber}` : ""}
        subtitle={selected ? `${selected.customerName} (${selected.customerCode})` : "Formal price quotation & estimate"}
        badge={selected ? <SalesQuotationStatusBadge status={selected.status} /> : null}
        actions={
          selected ? (
            <div className="flex items-center gap-2 ml-auto flex-wrap justify-end">
              {selected.status === "DRAFT" && (
                <Button size="sm" variant="outline" onClick={() => handleSend(selected.id)}>
                  <Send className="mr-1.5 h-3.5 w-3.5" />Mark Sent
                </Button>
              )}
              {(selected.status === "DRAFT" || selected.status === "SENT") && (
                <Button size="sm" variant="outline" className="text-emerald-600 hover:text-emerald-700 hover:bg-emerald-50" onClick={() => handleAccept(selected.id)}>
                  <CheckCircle className="mr-1.5 h-3.5 w-3.5" />Accept
                </Button>
              )}
              {(selected.status === "DRAFT" || selected.status === "SENT" || selected.status === "ACCEPTED") && (
                <Button
                  size="sm"
                  onClick={() => handleConvert(selected)}
                  disabled={isConverting}
                >
                  <ArrowRightCircle className="mr-1.5 h-4 w-4" />Convert to Order
                </Button>
              )}
              {selected.status !== "CONVERTED" && selected.status !== "REJECTED" && (
                <Button
                  variant="outline"
                  size="sm"
                  className="text-red-600 hover:text-red-700 hover:bg-red-50"
                  onClick={() => handleReject(selected.id)}
                >
                  <XCircle className="mr-1.5 h-3.5 w-3.5" />Reject
                </Button>
              )}
            </div>
          ) : null
        }
        fields={fields}
        lines={
          selected
            ? {
                headers: ["Item", "Qty", "Rate", "Disc %", "VAT %", "Net Total"],
                rows: selected.lines.map((l) => ({
                  key: String(l.id),
                  cells: [
                    `${l.itemCode} — ${l.itemName}`,
                    `${l.quantity} ${l.uomCode}`,
                    formatMoney(l.unitPrice, selected.currency),
                    l.discountPercentage ? `${l.discountPercentage}%` : "—",
                    `${l.taxRate}%`,
                    formatMoney(l.lineAmount, selected.currency),
                  ],
                })),
              }
            : undefined
        }
      />
    </div>
  );
}
