"use client";

import { useState } from "react";
import { Package, RefreshCw, ChevronLeft, ChevronRight } from "lucide-react";
import { useGetStockOnHandQuery } from "@/services/stock.service";
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

export default function StockPage() {
  const [page, setPage] = useState(0);
  const [pageSize] = useState(20);
  const [inStockOnly, setInStockOnly] = useState(false);
  const [warehouseId, setWarehouseId] = useState<number | "">("");

  const { data, isLoading, isFetching, isError, refetch } = useGetStockOnHandQuery({
    page,
    size: pageSize,
    sort: "itemName,asc",
    inStockOnly,
    warehouseId: warehouseId || undefined,
  });

  const rows = data?.content ?? [];
  const totalElements = data?.totalElements ?? 0;
  const totalPages = data?.totalPages ?? 0;
  const fmt = (n: number) => (n ?? 0).toLocaleString();

  return (
    <div className="flex flex-col gap-6 p-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-foreground">Stock on Hand</h1>
          <p className="text-sm text-muted-foreground">
            Current stock levels per warehouse, item and unit.
          </p>
        </div>
        <Button variant="outline" size="sm" onClick={() => refetch()} disabled={isFetching}>
          <RefreshCw className={`mr-2 size-4 ${isFetching ? "animate-spin" : ""}`} />
          Refresh
        </Button>
      </div>

      <div className="flex flex-wrap items-end gap-4">
        <label className="flex h-9 items-center gap-2 text-sm text-muted-foreground">
          <input
            type="checkbox"
            checked={inStockOnly}
            onChange={(e) => {
              setInStockOnly(e.target.checked);
              setPage(0);
            }}
          />
          In stock only
        </label>
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
              <TableHead>SKU</TableHead>
              <TableHead>Warehouse</TableHead>
              <TableHead>Unit</TableHead>
              <TableHead className="text-right">On Hand</TableHead>
              <TableHead className="text-right">Reserved</TableHead>
              <TableHead className="text-right">Available</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading ? (
              <TableSkeletonRows columns={7} />
            ) : isError ? (
              <TableErrorRow
                columns={7}
                title="Could not load stock levels"
                description="The stock on-hand report did not complete. Check your connection and try again."
                onRetry={refetch}
              />
            ) : rows.length === 0 ? (
              <TableRow>
                <TableCell colSpan={7} className="h-64 text-center">
                  <div className="flex flex-col items-center justify-center gap-2">
                    <div className="flex size-12 items-center justify-center rounded-full bg-muted">
                      <Package className="size-6 text-muted-foreground" />
                    </div>
                    <p className="text-base font-semibold text-foreground">No stock records</p>
                    <p className="max-w-sm text-sm text-muted-foreground">
                      {inStockOnly
                        ? "No items currently have stock on hand."
                        : "No stock balances yet. Stock appears once goods are received."}
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
                  <TableCell className="font-medium text-foreground">{row.itemName}</TableCell>
                  <TableCell className="font-mono text-sm text-muted-foreground">{row.sku}</TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {row.warehouseName}{" "}
                    <span className="text-xs">({row.warehouseCode})</span>
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">{row.unitCode}</TableCell>
                  <TableCell className="text-right text-sm font-medium text-foreground">
                    {fmt(row.onHand)}
                  </TableCell>
                  <TableCell className="text-right text-sm text-muted-foreground">
                    {fmt(row.reserved)}
                  </TableCell>
                  <TableCell className="text-right text-sm font-medium text-foreground">
                    {fmt(row.available)}
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>

        <div className="flex flex-col gap-3 border-t border-border bg-muted/20 px-4 py-3 text-sm sm:flex-row sm:items-center sm:justify-between">
          <div className="text-muted-foreground">
            Showing <span className="font-medium text-foreground">{rows.length}</span> of{" "}
            <span className="font-medium text-foreground">{totalElements}</span> total
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
