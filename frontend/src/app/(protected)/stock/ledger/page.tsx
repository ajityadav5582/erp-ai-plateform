"use client";

import { useState } from "react";
import { RefreshCw } from "lucide-react";
import { useGetStockCardQuery } from "@/services/stock.service";
import { useGetItemsQuery } from "@/services/item.service";
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

export default function StockLedgerPage() {
  const [itemId, setItemId] = useState<number | "">("");
  const today = new Date();
  const firstOfYear = new Date(today.getFullYear(), 0, 1);
  const [from, setFrom] = useState(firstOfYear.toISOString().slice(0, 10));
  const [to, setTo] = useState(today.toISOString().slice(0, 10));
  const [warehouseId, setWarehouseId] = useState<number | "">("");

  const { data: itemsPage } = useGetItemsQuery({ size: 200, sort: "nameEn,asc" });
  const items = itemsPage?.content ?? [];

  const { data, isLoading, isFetching, isError, refetch } = useGetStockCardQuery(
    {
      itemId: Number(itemId),
      from: new Date(`${from}T00:00:00Z`).toISOString(),
      to: new Date(`${to}T23:59:59Z`).toISOString(),
      warehouseId: warehouseId || undefined,
    },
    { skip: !itemId }
  );

  const fmt = (n: number) => (n ?? 0).toLocaleString();
  const rows = data?.rows ?? [];

  return (
    <div className="flex flex-col gap-6 p-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-foreground">Stock Ledger</h1>
          <p className="text-sm text-muted-foreground">
            Movement history and running balance for an item.
          </p>
        </div>
        <Button
          variant="outline"
          size="sm"
          onClick={() => refetch()}
          disabled={isFetching || !itemId}
        >
          <RefreshCw className={`mr-2 size-4 ${isFetching ? "animate-spin" : ""}`} />
          Refresh
        </Button>
      </div>

      <div className="flex flex-wrap items-end gap-3">
        <div className="grid gap-1">
          <label className="text-sm text-muted-foreground">Item</label>
          <select
            className="h-9 min-w-64 rounded border border-input bg-background px-2 text-sm"
            value={itemId}
            onChange={(e) => setItemId(e.target.value ? Number(e.target.value) : "")}
          >
            <option value="">Select item…</option>
            {items.map((it) => (
              <option key={it.id} value={it.id}>
                {it.sku} — {it.nameEn}
              </option>
            ))}
          </select>
        </div>
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

      {itemId && data && (
        <div className="flex gap-6 text-sm">
          <div className="text-muted-foreground">
            Opening: <span className="font-medium text-foreground">{fmt(data.openingBalance)}</span>
          </div>
          <div className="text-muted-foreground">
            Closing: <span className="font-medium text-foreground">{fmt(data.closingBalance)}</span>
          </div>
        </div>
      )}

      <div className="rounded-lg border border-border bg-card">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Date</TableHead>
              <TableHead>Type</TableHead>
              <TableHead>Reference</TableHead>
              <TableHead>Warehouse</TableHead>
              <TableHead className="text-right">Qty</TableHead>
              <TableHead className="text-right">Δ</TableHead>
              <TableHead className="text-right">Balance</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading ? (
              <TableSkeletonRows columns={7} />
            ) : isError ? (
              <TableErrorRow
                columns={7}
                title="Could not load the ledger"
                description="The stock ledger did not complete. Check your connection and try again."
                onRetry={refetch}
              />
            ) : rows.length === 0 ? (
              <TableRow>
                <TableCell colSpan={7} className="h-40 text-center text-muted-foreground">
                  {itemId ? "No movements in this period." : "Select an item to view its ledger."}
                </TableCell>
              </TableRow>
            ) : (
              rows.map((r, i) => (
                <TableRow key={`${r.movementDate}-${i}`} className="transition-colors hover:bg-muted/50">
                  <TableCell className="text-sm text-muted-foreground">
                    {new Date(r.movementDate).toLocaleString()}
                  </TableCell>
                  <TableCell className="text-sm font-medium text-foreground">{r.movementType}</TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {r.referenceNumber ?? r.referenceType ?? "—"}
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {r.warehouseName} ({r.warehouseCode})
                  </TableCell>
                  <TableCell className="text-right text-sm text-foreground">{fmt(r.quantity)}</TableCell>
                  <TableCell className="text-right text-sm text-muted-foreground">{fmt(r.onHandDelta)}</TableCell>
                  <TableCell className="text-right text-sm font-medium text-foreground">
                    {fmt(r.runningBalance)}
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
