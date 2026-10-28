"use client";

import { useGetActiveWarehousesQuery } from "@/services/warehouse.service";

/**
 * Labeled warehouse select shared by the stock reports.
 *
 * Bound to a `number | ""` value so a page can send `warehouseId` only when one
 * is chosen — the backend treats a missing filter as "all warehouses". The list
 * is the active-warehouses endpoint (unpaginated), the shape a filter wants.
 */
export function WarehouseFilter({
  value,
  onChange,
  id = "warehouse-filter",
}: {
  value: number | "";
  onChange: (value: number | "") => void;
  id?: string;
}) {
  const { data: warehouses } = useGetActiveWarehousesQuery();

  return (
    <div className="grid gap-1">
      <label htmlFor={id} className="text-sm text-muted-foreground">
        Warehouse
      </label>
      <select
        id={id}
        className="h-9 min-w-48 rounded border border-input bg-background px-2 text-sm"
        value={value}
        onChange={(e) => onChange(e.target.value ? Number(e.target.value) : "")}
      >
        <option value="">All warehouses</option>
        {(warehouses ?? []).map((w) => (
          <option key={w.id} value={w.id}>
            {w.name} ({w.code})
          </option>
        ))}
      </select>
    </div>
  );
}
