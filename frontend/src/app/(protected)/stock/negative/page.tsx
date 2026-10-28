"use client";

import { useState } from "react";
import { TrendingDown, RefreshCw, ChevronLeft, ChevronRight } from "lucide-react";
import { useGetNegativeStockQuery } from "@/services/stock.service";
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

const qty = (n: number | null | undefined) => (n ?? 0).toLocaleString();

export default function NegativeStockPage() {
  const [page, setPage] = useState(0);
  const [pageSize] = useState(20);
  const [warehouseId, setWarehouseId] = useState<number | "">("");

  const { data, isLoading, isFetching, isError, refetch } = useGetNegativeStockQuery({
    page,
    size: pageSize,
    warehouseId: warehouseId || undefined,
  });

  const rows = data?.content ?? [];
  const totalElements = data?.totalElements ?? 0;
  const totalPages = data?.totalPages ?? 0;

  return (
    <div className="flex flex-col gap-6 p-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-foreground">Negative Stock</h1>
          <p className="text-sm text-muted-foreground">
            Balances that have gone below zero — a data-quality check, not a stock level.
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
              <TableHead className="text-right">On Hand</TableHead>
              <TableHead className="text-right">Reserved</TableHead>
              <TableHead className="text-right">Available</TableHead>
              <TableHead>Assessment</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading ? (
              <TableSkeletonRows columns={7} />
            ) : isError ? (
              <TableErrorRow
                columns={7}
                title="Could not load the negative-stock check"
                description="The negative-stock report did not complete. Check your connection and try again."
                onRetry={refetch}
              />
            ) : rows.length === 0 ? (
              <TableRow>
                <TableCell colSpan={7} className="h-64 text-center">
                  <div className="flex flex-col items-center justify-center gap-2">
                    <div className="flex size-12 items-center justify-center rounded-full bg-muted">
                      <TrendingDown className="size-6 text-muted-foreground" />
                    </div>
                    <p className="text-base font-semibold text-foreground">
                      No negative balances
                    </p>
                    <p className="max-w-sm text-sm text-muted-foreground">
                      Every stock balance is at or above zero. Nothing was issued without first
                      being received.
                    </p>
                  </div>
                </TableCell>
              </TableRow>
            ) : (
              rows.map((row) => (
                <TableRow
                  key={`${row.warehouseId}-${row.itemId}-${row.unitId}`}
                  className="transition-colors hover:bg-muted/50"
                >
                  <TableCell className="font-medium text-foreground">
                    {row.itemName}
                    <span className="ml-2 font-mono text-xs text-muted-foreground">
                      {row.sku}
                    </span>
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {row.warehouseName}{" "}
                    <span className="text-xs">({row.warehouseCode})</span>
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">{row.unitCode}</TableCell>
                  <TableCell className="text-right text-sm font-medium text-red-600">
                    {qty(row.onHand)}
                  </TableCell>
                  <TableCell className="text-right text-sm text-muted-foreground">
                    {qty(row.reserved)}
                  </TableCell>
                  <TableCell className="text-right text-sm text-muted-foreground">
                    {qty(row.available)}
                  </TableCell>
                  <TableCell className="text-sm">
                    {row.negativeStockAllowed ? (
                      <span className="inline-flex rounded-full bg-blue-100 px-2 py-0.5 text-xs font-medium text-blue-700 dark:bg-blue-950 dark:text-blue-400">
                        Backorder — warehouse allows it
                      </span>
                    ) : (
                      <span className="inline-flex rounded-full bg-red-100 px-2 py-0.5 text-xs font-medium text-red-700 dark:bg-red-950 dark:text-red-400">
                        Data error — warehouse forbids it
                      </span>
                    )}
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>

        <div className="flex flex-col gap-3 border-t border-border bg-muted/20 px-4 py-3 text-sm sm:flex-row sm:items-center sm:justify-between">
          <div className="text-muted-foreground">
            Showing <span className="font-medium text-foreground">{rows.length}</span> of{" "}
            <span className="font-medium text-foreground">{totalElements}</span> negative balances
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
