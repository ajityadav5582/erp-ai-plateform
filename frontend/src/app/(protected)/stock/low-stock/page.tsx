"use client";

import { useState } from "react";
import { AlertTriangle, RefreshCw, ChevronLeft, ChevronRight } from "lucide-react";
import { useGetLowStockQuery, type LowStockRow } from "@/services/stock.service";
import { Button } from "@/components/ui/button";
import { TableSkeletonRows, TableErrorRow } from "@/components/common/table-state";
import { WarehouseFilter } from "@/components/stock/warehouse-filter";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";

const STATUS_LABEL: Record<LowStockRow["status"], string> = {
  OUT_OF_STOCK: "Out of stock",
  BELOW_MIN: "Below minimum",
  BELOW_REORDER: "Reorder",
};

const STATUS_CLASS: Record<LowStockRow["status"], string> = {
  OUT_OF_STOCK: "bg-red-100 text-red-700 dark:bg-red-950 dark:text-red-400",
  BELOW_MIN: "bg-amber-100 text-amber-700 dark:bg-amber-950 dark:text-amber-400",
  BELOW_REORDER: "bg-blue-100 text-blue-700 dark:bg-blue-950 dark:text-blue-400",
};

export default function LowStockPage() {
  const [page, setPage] = useState(0);
  const [pageSize] = useState(20);
  const [warehouseId, setWarehouseId] = useState<number | "">("");

  const { data, isLoading, isFetching, isError, refetch } = useGetLowStockQuery({
    page,
    size: pageSize,
    warehouseId: warehouseId || undefined,
  });

  const rows = data?.content ?? [];
  const totalElements = data?.totalElements ?? 0;
  const totalPages = data?.totalPages ?? 0;
  const fmt = (n: number | null | undefined) => (n ?? 0).toLocaleString();

  return (
    <div className="flex flex-col gap-6 p-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-foreground">Low Stock & Reorder</h1>
          <p className="text-sm text-muted-foreground">
            Items at or below their reorder level, most urgent first, with a suggested order
            quantity.
          </p>
        </div>
        <Button variant="outline" size="sm" onClick={() => refetch()} disabled={isFetching}>
          <RefreshCw className={`mr-2 size-4 ${isFetching ? "animate-spin" : ""}`} />
          Refresh
        </Button>
      </div>

      <div className="flex flex-wrap items-end gap-4">
        <WarehouseFilter
          value={warehouseId}
          onChange={(v) => {
            setWarehouseId(v);
            setPage(0);
          }}
        />
      </div>

      <div className="rounded-lg border border-border bg-card">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Item</TableHead>
              <TableHead>Warehouse</TableHead>
              <TableHead>Unit</TableHead>
              <TableHead>Status</TableHead>
              <TableHead className="text-right">On Hand</TableHead>
              <TableHead className="text-right">Reserved</TableHead>
              <TableHead className="text-right">Available</TableHead>
              <TableHead className="text-right">Reorder At</TableHead>
              <TableHead className="text-right">Shortfall</TableHead>
              <TableHead className="text-right">Suggested Qty</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading ? (
              <TableSkeletonRows columns={10} />
            ) : isError ? (
              <TableErrorRow
                columns={10}
                title="Could not load the reorder list"
                description="The low-stock report did not complete. Check your connection and try again."
                onRetry={refetch}
              />
            ) : rows.length === 0 ? (
              <TableRow>
                <TableCell colSpan={10} className="h-64 text-center">
                  <div className="flex flex-col items-center justify-center gap-2">
                    <div className="flex size-12 items-center justify-center rounded-full bg-muted">
                      <AlertTriangle className="size-6 text-muted-foreground" />
                    </div>
                    <p className="text-base font-semibold text-foreground">
                      Nothing needs reordering
                    </p>
                    <p className="max-w-sm text-sm text-muted-foreground">
                      No item is at or below its reorder level. Set a reorder level on an item to
                      have it tracked here.
                    </p>
                  </div>
                </TableCell>
              </TableRow>
            ) : (
              rows.map((row) => (
                <TableRow
                  key={`${row.warehouseId ?? "none"}-${row.itemId}-${row.unitId ?? "none"}`}
                  className="transition-colors hover:bg-muted/50"
                >
                  <TableCell className="font-medium text-foreground">
                    {row.itemName}
                    <span className="ml-2 font-mono text-xs text-muted-foreground">
                      {row.sku}
                    </span>
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {row.warehouseName ? (
                      <>
                        {row.warehouseName}{" "}
                        <span className="text-xs">({row.warehouseCode})</span>
                      </>
                    ) : (
                      <span className="italic">No stock record</span>
                    )}
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {row.unitCode ?? "—"}
                  </TableCell>
                  <TableCell>
                    <span
                      className={`inline-flex rounded-full px-2 py-0.5 text-xs font-medium ${STATUS_CLASS[row.status]}`}
                    >
                      {STATUS_LABEL[row.status]}
                    </span>
                  </TableCell>
                  <TableCell className="text-right text-sm text-muted-foreground">
                    {fmt(row.onHand)}
                  </TableCell>
                  <TableCell className="text-right text-sm text-muted-foreground">
                    {fmt(row.reserved)}
                  </TableCell>
                  <TableCell className="text-right text-sm font-medium text-foreground">
                    {fmt(row.available)}
                  </TableCell>
                  <TableCell className="text-right text-sm text-muted-foreground">
                    {fmt(Math.max(row.reorderLevel, row.minStockLevel))}
                  </TableCell>
                  <TableCell className="text-right text-sm font-medium text-red-600">
                    {fmt(row.shortfall)}
                  </TableCell>
                  <TableCell className="text-right text-sm font-semibold text-foreground">
                    {fmt(row.suggestedOrderQty)}
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>

        <div className="flex flex-col gap-3 border-t border-border bg-muted/20 px-4 py-3 text-sm sm:flex-row sm:items-center sm:justify-between">
          <div className="text-muted-foreground">
            Showing <span className="font-medium text-foreground">{rows.length}</span> of{" "}
            <span className="font-medium text-foreground">{totalElements}</span> items needing
            reorder
          </div>
          <div className="flex items-center gap-1">
            <span className="mr-2 text-xs text-muted-foreground">
              Page {page + 1} of {totalPages || 1}
            </span>
            <Button
              variant="outline"
              size="icon"
              className="size-8"
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0 || isLoading}
            >
              <ChevronLeft className="size-4" />
            </Button>
            <Button
              variant="outline"
              size="icon"
              className="size-8"
              onClick={() => setPage((p) => p + 1)}
              disabled={page >= totalPages - 1 || isLoading}
            >
              <ChevronRight className="size-4" />
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
