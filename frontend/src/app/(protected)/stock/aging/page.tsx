"use client";

import { useState } from "react";
import { Clock, RefreshCw, ChevronLeft, ChevronRight } from "lucide-react";
import {
  useGetStockAgingQuery,
  type StockAgingRow,
} from "@/services/stock.service";
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

const BUCKET_LABEL: Record<StockAgingRow["ageBucket"], string> = {
  NEVER_MOVED: "Never moved",
  FRESH: "0–30 days",
  AGING: "31–90 days",
  SLOW: "91–180 days",
  STAGNANT: "181–365 days",
  DEAD: "Over a year",
};

const BUCKET_CLASS: Record<StockAgingRow["ageBucket"], string> = {
  NEVER_MOVED: "bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300",
  FRESH: "bg-emerald-100 text-emerald-700 dark:bg-emerald-950 dark:text-emerald-400",
  AGING: "bg-blue-100 text-blue-700 dark:bg-blue-950 dark:text-blue-400",
  SLOW: "bg-amber-100 text-amber-700 dark:bg-amber-950 dark:text-amber-400",
  STAGNANT: "bg-orange-100 text-orange-700 dark:bg-orange-950 dark:text-orange-400",
  DEAD: "bg-red-100 text-red-700 dark:bg-red-950 dark:text-red-400",
};

const money = (n: number | null | undefined) =>
  (n ?? 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });

const qty = (n: number | null | undefined) => (n ?? 0).toLocaleString();

export default function StockAgingPage() {
  const [page, setPage] = useState(0);
  const [pageSize] = useState(20);
  const [minDays, setMinDays] = useState(0);
  const [warehouseId, setWarehouseId] = useState<number | "">("");

  const { data, isLoading, isFetching, isError, refetch } = useGetStockAgingQuery({
    page,
    size: pageSize,
    minDays,
    warehouseId: warehouseId || undefined,
  });

  const rows = data?.content ?? [];
  const totalElements = data?.totalElements ?? 0;
  const totalPages = data?.totalPages ?? 0;

  return (
    <div className="flex flex-col gap-6 p-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-foreground">Stock Aging</h1>
          <p className="text-sm text-muted-foreground">
            How long stock has sat since its last movement — capital doing no work.
          </p>
        </div>
        <Button variant="outline" size="sm" onClick={() => refetch()} disabled={isFetching}>
          <RefreshCw className={`mr-2 size-4 ${isFetching ? "animate-spin" : ""}`} />
          Refresh
        </Button>
      </div>

      <div className="flex flex-wrap items-end gap-3">
        <div className="grid gap-1">
          <label className="text-sm text-muted-foreground">Idle at least</label>
          <select
            className="h-9 rounded border border-input bg-background px-2 text-sm"
            value={minDays}
            onChange={(e) => {
              setMinDays(Number(e.target.value));
              setPage(0);
            }}
          >
            <option value={0}>Any age</option>
            <option value={30}>30 days</option>
            <option value={90}>90 days</option>
            <option value={180}>180 days</option>
            <option value={365}>1 year</option>
          </select>
        </div>
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
              <TableHead className="text-right">Value</TableHead>
              <TableHead>Last Movement</TableHead>
              <TableHead className="text-right">Days Idle</TableHead>
              <TableHead>Bucket</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading ? (
              <TableSkeletonRows columns={8} />
            ) : isError ? (
              <TableErrorRow
                columns={8}
                title="Could not load the aging report"
                description="The stock aging report did not complete. Check your connection and try again."
                onRetry={refetch}
              />
            ) : rows.length === 0 ? (
              <TableRow>
                <TableCell colSpan={8} className="h-64 text-center">
                  <div className="flex flex-col items-center justify-center gap-2">
                    <div className="flex size-12 items-center justify-center rounded-full bg-muted">
                      <Clock className="size-6 text-muted-foreground" />
                    </div>
                    <p className="text-base font-semibold text-foreground">No stock to age</p>
                    <p className="max-w-sm text-sm text-muted-foreground">
                      No balance holds stock at this age. Stock appears once goods are received.
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
                  <TableCell className="text-right text-sm text-muted-foreground">
                    {qty(row.onHand)}
                  </TableCell>
                  <TableCell className="text-right text-sm text-muted-foreground">
                    {money(row.onHandValue)}
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {row.lastMovementAt
                      ? new Date(row.lastMovementAt).toLocaleDateString()
                      : "—"}
                  </TableCell>
                  <TableCell className="text-right text-sm font-medium text-foreground">
                    {row.daysSinceMovement ?? "—"}
                  </TableCell>
                  <TableCell>
                    <span
                      className={`inline-flex rounded-full px-2 py-0.5 text-xs font-medium ${BUCKET_CLASS[row.ageBucket]}`}
                    >
                      {BUCKET_LABEL[row.ageBucket]}
                    </span>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>

        <div className="flex flex-col gap-3 border-t border-border bg-muted/20 px-4 py-3 text-sm sm:flex-row sm:items-center sm:justify-between">
          <div className="text-muted-foreground">
            Showing <span className="font-medium text-foreground">{rows.length}</span> of{" "}
            <span className="font-medium text-foreground">{totalElements}</span> balances holding
            stock
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
