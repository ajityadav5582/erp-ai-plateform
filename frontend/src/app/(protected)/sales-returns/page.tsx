"use client";

import * as React from "react";
import { useSearchParams, useRouter } from "next/navigation";
import { MoreVertical, Eye, Send, X, Plus } from "lucide-react";
import {
  useGetReturnsQuery,
  usePostReturnMutation,
  useCancelReturnMutation,
  type SalesReturn,
} from "@/services/sales-return.service";
import { toast } from "sonner";
import { getApiErrorMessage } from "@/services/error-handler";
import { ListPageHeader } from "@/components/common/list-page-header";
import { ListPagination } from "@/components/common/list-pagination";
import { ReturnStatusBadge } from "@/components/sales/sales-status-badge";
import { SalesDetailsSheet, type DetailField } from "@/components/sales/sales-details-sheet";
import { CreateReturnDialog } from "@/components/sales/create-return-dialog";
import { formatMoney } from "@/lib/sales-format";
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
  const [createOpen, setCreateOpen] = React.useState(false);

  const { data, isLoading, isFetching } = useGetReturnsQuery({ page, size: 20, sort: "createdAt,desc" });
  const [post] = usePostReturnMutation();
  const [cancel] = useCancelReturnMutation();

  const handlePostReturn = async (id: number) => {
    try {
      await post(id).unwrap();
      toast.success("Sales return posted and stock restored successfully");
      setSelected(null);
    } catch (err) {
      toast.error("Failed to post sales return", {
        description: getApiErrorMessage(err),
      });
    }
  };

  const handleCancelReturn = async (id: number) => {
    const reason = window.prompt("Reason for cancelling return note (mandatory):");
    if (reason === null) return;
    try {
      await cancel({ id, data: { reason } }).unwrap();
      toast.success("Sales return cancelled successfully");
      setSelected(null);
    } catch (err) {
      toast.error("Failed to cancel sales return", {
        description: getApiErrorMessage(err),
      });
    }
  };

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
      <ListPageHeader
        title="Sales Returns"
        description="Credit notes (Anusuchi 7) for returned goods."
        count={data?.totalElements}
        countLabel="Returns"
        actions={
          <Button onClick={() => setCreateOpen(true)}>
            <Plus className="mr-1 h-4 w-4" />New Return
          </Button>
        }
      />

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
                          <DropdownMenuItem onClick={() => handlePostReturn(r.id)}><Send className="mr-2 h-4 w-4" />Post (restock)</DropdownMenuItem>
                        )}
                        {r.status === "POSTED" && (
                          <DropdownMenuItem className="text-red-600" onClick={() => handleCancelReturn(r.id)}><X className="mr-2 h-4 w-4" />Cancel</DropdownMenuItem>
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

      <CreateReturnDialog open={createOpen} onOpenChange={setCreateOpen} />

      <SalesDetailsSheet
        open={!!selected}
        onOpenChange={(o) => !o && setSelected(null)}
        title={selected ? `Credit note ${selected.creditNoteNumber}` : ""}
        subtitle={selected ? `${selected.customerName} · Ref Invoice: ${selected.invoiceNumber}` : "Sales return"}
        badge={selected ? <ReturnStatusBadge status={selected.status} /> : null}
        fields={fields}
        lines={selected ? {
          headers: ["Item", "Qty", "Net", "VAT"],
          rows: selected.lines.map((l) => ({ key: String(l.id), cells: [`${l.itemCode} — ${l.itemName}`, l.quantity, formatMoney(l.lineAmount), formatMoney(l.vatAmount)] })),
        } : undefined}
        actions={
          selected ? (
            <div className="flex items-center gap-2 ml-auto">
              {selected.status === "DRAFT" && (
                <Button size="sm" onClick={() => handlePostReturn(selected.id)}>
                  <Send className="mr-1.5 h-3.5 w-3.5" />Post & Restock
                </Button>
              )}
              {selected.status === "POSTED" && (
                <Button
                  variant="outline"
                  size="sm"
                  className="text-red-600 hover:text-red-700 hover:bg-red-50"
                  onClick={() => handleCancelReturn(selected.id)}
                >
                  <X className="mr-1.5 h-3.5 w-3.5" />Cancel Return
                </Button>
              )}
            </div>
          ) : undefined
        }
      />
    </div>
  );
}
