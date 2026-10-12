"use client";

import * as React from "react";
import { useSearchParams, useRouter } from "next/navigation";
import { MoreVertical, Eye, Send, X } from "lucide-react";
import {
  useGetReturnsQuery,
  usePostReturnMutation,
  useCancelReturnMutation,
  type SalesReturn,
} from "@/services/sales-return.service";
import { ListPageHeader } from "@/components/common/list-page-header";
import { ListPagination } from "@/components/common/list-pagination";
import { ReturnStatusBadge } from "@/components/sales/sales-status-badge";
import { SalesDetailsSheet, type DetailField } from "@/components/sales/sales-details-sheet";
import { formatMoney, formatDate } from "@/lib/sales-format";
import { Button } from "@/components/ui/button";
import {
  Table, TableBody, TableCell, TableHead, TableHeader, TableRow,
} from "@/components/ui/table";
import {
  DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { TableSkeletonRows, TableEmptyRow } from "@/components/common/table-state";

export default function SalesReturnsPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const page = Number(searchParams.get("page") ?? "0");
  const [selected, setSelected] = React.useState<SalesReturn | null>(null);

  const { data, isLoading, isFetching } = useGetReturnsQuery({ page, size: 20 });
  const [post] = usePostReturnMutation();
  const [cancel] = useCancelReturnMutation();

  const rows = data?.content ?? [];

  const fields: DetailField[] = selected
    ? [
        { label: "Invoice", value: selected.invoiceNumber },
        { label: "Customer", value: `${selected.customerName} (${selected.customerCode})` },
        { label: "Return date (BS)", value: selected.returnDateNp },
        { label: "Credit total", value: formatMoney(selected.totalAmount) },
        { label: "VAT reversed", value: formatMoney(selected.vatAmount) },
        { label: "Reason", value: selected.reason, full: true },
      ]
    : [];

  return (
    <div className="space-y-4 p-4">
      <ListPageHeader title="Sales Returns" description="Credit notes for returned goods." count={data?.totalElements} countLabel="Returns" />

      <div className="rounded-md border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Credit note #</TableHead>
              <TableHead>Invoice</TableHead>
              <TableHead>Customer</TableHead>
              <TableHead>Date (BS)</TableHead>
              <TableHead>Status</TableHead>
              <TableHead className="text-right">Credit</TableHead>
              <TableHead className="w-10" />
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading || isFetching ? (
              <TableSkeletonRows columns={7} />
            ) : rows.length === 0 ? (
              <TableEmptyRow columns={7} title="No results" />
            ) : (
              rows.map((r) => (
                <TableRow key={r.id}>
                  <TableCell className="font-medium">{r.creditNoteNumber}</TableCell>
                  <TableCell>{r.invoiceNumber}</TableCell>
                  <TableCell>{r.customerName}</TableCell>
                  <TableCell>{r.returnDateNp}</TableCell>
                  <TableCell><ReturnStatusBadge status={r.status} /></TableCell>
                  <TableCell className="text-right">{formatMoney(r.totalAmount)}</TableCell>
                  <TableCell>
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button variant="ghost" size="icon"><MoreVertical className="h-4 w-4" /></Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end">
                        <DropdownMenuItem onClick={() => setSelected(r)}><Eye className="mr-2 h-4 w-4" />View</DropdownMenuItem>
                        {r.status === "DRAFT" && (
                          <DropdownMenuItem onClick={() => post(r.id)}><Send className="mr-2 h-4 w-4" />Post (restock)</DropdownMenuItem>
                        )}
                        {r.status === "POSTED" && (
                          <DropdownMenuItem className="text-red-600" onClick={() => cancel({ id: r.id, data: { reason: "Cancelled" } })}><X className="mr-2 h-4 w-4" />Cancel</DropdownMenuItem>
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

      <ListPagination page={page} pageSize={20} totalPages={data?.totalPages ?? 0} totalElements={data?.totalElements ?? 0} itemLabel="returns" onPageChange={(p) => router.push(`/sales-returns?page=${p}`)} />

      <SalesDetailsSheet
        open={!!selected}
        onOpenChange={(o) => !o && setSelected(null)}
        title={selected ? `Credit note ${selected.creditNoteNumber}` : ""}
        subtitle="Sales return"
        fields={fields}
        lines={selected ? {
          headers: ["Item", "Qty", "Net", "VAT"],
          rows: selected.lines.map((l) => ({ key: String(l.id), cells: [`${l.itemCode} — ${l.itemName}`, l.quantity, formatMoney(l.lineAmount), formatMoney(l.vatAmount)] })),
        } : undefined}
      />
    </div>
  );
}
