"use client";

import { useState } from "react";
import { RefreshCw } from "lucide-react";
import { useGetStockMovementSummaryQuery } from "@/services/stock.service";
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

export default function StockSummaryPage() {
  const today = new Date();
  const firstOfYear = new Date(today.getFullYear(), 0, 1);
  const [from, setFrom] = useState(firstOfYear.toISOString().slice(0, 10));
  const [to, setTo] = useState(today.toISOString().slice(0, 10));
  const [warehouseId, setWarehouseId] = useState<number | "">("");

  const { data, isLoading, isFetching, isError, refetch } = useGetStockMovementSummaryQuery({
    from: new Date(`${from}T00:00:00Z`).toISOString(),
    to: new Date(`${to}T23:59:59Z`).toISOString(),
    warehouseId: warehouseId || undefined,
  });

  const fmt = (n: number) => (n ?? 0).toLocaleString();
  const rows = data?.rows ?? [];

  return (
    <div className="flex flex-col gap-6 p-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-foreground">Stock Movement Summary</h1>
          <p className="text-sm text-muted-foreground">
            Opening, in, out, adjustments and closing per item over a period.
          </p>
        </div>
        <Button variant="outline" size="sm" onClick={() => refetch()} disabled={isFetching}>
          <RefreshCw className={`mr-2 size-4 ${isFetching ? "animate-spin" : ""}`} />
          Refresh
        </Button>
      </div>

      <div className="flex flex-wrap items-end gap-3">
        <div className="grid gap-1">
          <label className="text-sm text-muted-foreground">From</label>
          <input
            type="date"
            className="h-9 rounded border border-input bg-background px-2 text-sm"
            value={from}
            onChange={(e) => setFrom(e.target.value)}
          />
        </div>
        <div className="grid gap-1">
          <label className="text-sm text-muted-foreground">To</label>
          <input
            type="date"
            className="h-9 rounded border border-input bg-background px-2 text-sm"
            value={to}
            onChange={(e) => setTo(e.target.value)}
          />
        </div>
        <WarehouseFilter
          value={warehouseId}
          onChange={(v) => {
            setWarehouseId(v);
          }}
        />
      </div>

      {data && (
        <div className="flex flex-wrap gap-6 rounded-lg border border-border bg-card p-4 text-sm">
          <div className="text-muted-foreground">
            Opening: <span className="font-medium text-foreground">{fmt(data.totalOpening)}</span>
          </div>
          <div className="text-muted-foreground">
            In: <span className="font-medium text-emerald-600">{fmt(data.totalIn)}</span>
          </div>
          <div className="text-muted-foreground">
            Out: <span className="font-medium text-red-600">{fmt(data.totalOut)}</span>
          </div>
          <div className="text-muted-foreground">
            Adjust: <span className="font-medium text-foreground">{fmt(data.totalAdjust)}</span>
          </div>
          <div className="text-muted-foreground">
            Closing: <span className="font-medium text-foreground">{fmt(data.totalClosing)}</span>
          </div>
        </div>
      )}

      <div className="rounded-lg border border-border bg-card">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Item</TableHead>
              <TableHead>Warehouse</TableHead>
              <TableHead>Unit</TableHead>
              <TableHead className="text-right">Opening</TableHead>
              <TableHead className="text-right">In</TableHead>
              <TableHead className="text-right">Out</TableHead>
              <TableHead className="text-right">Adjust</TableHead>
              <TableHead className="text-right">Closing</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading ? (
              <TableSkeletonRows columns={8} />
            ) : isError ? (
              <TableErrorRow
                columns={8}
                title="Could not load the movement summary"
                description="The stock movement summary did not complete. Check your connection and try again."
                onRetry={refetch}
              />
            ) : rows.length === 0 ? (
              <TableRow>
                <TableCell colSpan={8} className="h-40 text-center text-muted-foreground">
                  No stock movements in this period.
                </TableCell>
              </TableRow>
            ) : (
              rows.map((r, i) => (
                <TableRow
                  key={`${r.itemId}-${r.warehouseId}-${r.unitId}-${i}`}
                  className="transition-colors hover:bg-muted/50"
                >
                  <TableCell className="font-medium text-foreground">
                    {r.itemName ?? r.sku ?? `#${r.itemId}`}
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {r.warehouseName} ({r.warehouseCode})
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">{r.unitCode}</TableCell>
                  <TableCell className="text-right text-sm text-muted-foreground">{fmt(r.opening)}</TableCell>
                  <TableCell className="text-right text-sm text-emerald-600">{fmt(r.totalIn)}</TableCell>
                  <TableCell className="text-right text-sm text-red-600">{fmt(r.totalOut)}</TableCell>
                  <TableCell className="text-right text-sm text-muted-foreground">{fmt(r.totalAdjust)}</TableCell>
                  <TableCell className="text-right text-sm font-medium text-foreground">
                    {fmt(r.closing)}
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>
    </div>
  );
}
