"use client";

import { useState } from "react";
import { Coins, RefreshCw, ChevronLeft, ChevronRight, AlertCircle } from "lucide-react";
import { useGetStockValuationQuery } from "@/services/stock.service";
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

const money = (n: number | null | undefined) =>
  (n ?? 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });

const qty = (n: number | null | undefined) => (n ?? 0).toLocaleString();

export default function StockValuationPage() {
  const [page, setPage] = useState(0);
  const [pageSize] = useState(20);
  const [warehouseId, setWarehouseId] = useState<number | "">("");

  const { data, isLoading, isFetching, isError, refetch } = useGetStockValuationQuery({
    page,
    size: pageSize,
    warehouseId: warehouseId || undefined,
  });

  const rows = data?.rows.content ?? [];
  const totalElements = data?.rows.totalElements ?? 0;
  const totalPages = data?.rows.totalPages ?? 0;

  return (
    <div className="flex flex-col gap-6 p-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-foreground">Stock Valuation</h1>
          <p className="text-sm text-muted-foreground">
            Stock on hand valued at each item&apos;s purchase price, most valuable first.
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

      {data && (
        <div className="flex flex-wrap gap-6 rounded-lg border border-border bg-card p-4 text-sm">
          <div className="text-muted-foreground">
            On-hand value:{" "}
            <span className="font-medium text-foreground">{money(data.totalOnHandValue)}</span>
          </div>
          <div className="text-muted-foreground">
            Reserved:{" "}
            <span className="font-medium text-foreground">{money(data.totalReservedValue)}</span>
          </div>
          <div className="text-muted-foreground">
            Available:{" "}
            <span className="font-medium text-emerald-600">
              {money(data.totalAvailableValue)}
            </span>
          </div>
          <div className="text-muted-foreground">
            Items valued:{" "}
            <span className="font-medium text-foreground">{data.itemsValued}</span>
          </div>
        </div>
      )}

      {data && data.itemsMissingCost > 0 && (
        <div className="flex items-start gap-2 rounded-lg border border-amber-300 bg-amber-50 p-3 text-sm text-amber-800 dark:border-amber-800 dark:bg-amber-950 dark:text-amber-300">
          <AlertCircle className="mt-0.5 size-4 shrink-0" />
          <span>
            {data.itemsMissingCost} item{data.itemsMissingCost === 1 ? "" : "s"} hold stock but
            have no purchase price on file, so they are valued at zero. Set a purchase price on
            those items for the total to be complete.
          </span>
        </div>
      )}

      <p className="text-xs text-muted-foreground">
        Indicative valuation: the unit cost is the item master&apos;s purchase price, not the cost
        actually paid on the movement that brought the stock in.
      </p>

      <div className="rounded-lg border border-border bg-card">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Item</TableHead>
              <TableHead>Warehouse</TableHead>
              <TableHead>Unit</TableHead>
              <TableHead className="text-right">On Hand</TableHead>
              <TableHead className="text-right">Reserved</TableHead>
              <TableHead className="text-right">Unit Cost</TableHead>
              <TableHead className="text-right">On-hand Value</TableHead>
              <TableHead className="text-right">Available Value</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading ? (
              <TableSkeletonRows columns={8} />
            ) : isError ? (
              <TableErrorRow
                columns={8}
                title="Could not load the valuation"
                description="The stock valuation report did not complete. Check your connection and try again."
                onRetry={refetch}
              />
            ) : rows.length === 0 ? (
              <TableRow>
                <TableCell colSpan={8} className="h-64 text-center">
                  <div className="flex flex-col items-center justify-center gap-2">
                    <div className="flex size-12 items-center justify-center rounded-full bg-muted">
                      <Coins className="size-6 text-muted-foreground" />
                    </div>
                    <p className="text-base font-semibold text-foreground">Nothing to value</p>
                    <p className="max-w-sm text-sm text-muted-foreground">
                      No stock balances carry a quantity. Stock appears once goods are received.
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
                    {qty(row.reserved)}
                  </TableCell>
                  <TableCell className="text-right text-sm text-muted-foreground">
                    {row.costBasisMissing ? (
                      <span className="italic text-amber-600">not set</span>
                    ) : (
                      money(row.unitCost)
                    )}
                  </TableCell>
                  <TableCell className="text-right text-sm font-medium text-foreground">
                    {money(row.onHandValue)}
                  </TableCell>
                  <TableCell className="text-right text-sm text-muted-foreground">
                    {money(row.availableValue)}
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>

        <div className="flex flex-col gap-3 border-t border-border bg-muted/20 px-4 py-3 text-sm sm:flex-row sm:items-center sm:justify-between">
          <div className="text-muted-foreground">
            Showing <span className="font-medium text-foreground">{rows.length}</span> of{" "}
            <span className="font-medium text-foreground">{totalElements}</span> valued rows
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
