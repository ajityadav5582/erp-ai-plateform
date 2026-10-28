import { api } from "./api";
import type { PageResponse } from "@/types/api";

/**
 * Mirrors the backend `StockOnHandView` read model — one row per
 * (warehouse, item, unit) with display names resolved server-side.
 */
export interface StockOnHandRow {
  itemId: number;
  sku: string;
  itemName: string;
  warehouseId: number;
  warehouseCode: string;
  warehouseName: string;
  unitId: number;
  unitCode: string;
  onHand: number;
  reserved: number;
  available: number;
  lastMovementAt: string | null;
}

export interface GetStockOnHandParams {
  page?: number;
  size?: number;
  sort?: string;
  warehouseId?: number;
  itemId?: number;
  inStockOnly?: boolean;
}

export interface StockCardRow {
  movementDate: string;
  movementType: string;
  referenceType: string | null;
  referenceNumber: string | null;
  quantity: number;
  onHandDelta: number;
  runningBalance: number;
  postedBy: string | null;
  notes: string | null;
  warehouseCode: string;
  warehouseName: string;
  unitCode: string;
}

export interface StockCard {
  itemId: number;
  sku: string | null;
  itemName: string | null;
  from: string;
  to: string;
  openingBalance: number;
  closingBalance: number;
  rows: StockCardRow[];
}

export interface GetStockCardParams {
  itemId: number;
  from: string;
  to: string;
  warehouseId?: number;
  type?: string;
}

export interface StockMovementSummaryRow {
  itemId: number;
  sku: string | null;
  itemName: string | null;
  warehouseId: number;
  warehouseCode: string;
  warehouseName: string;
  unitId: number;
  unitCode: string;
  opening: number;
  totalIn: number;
  totalOut: number;
  totalAdjust: number;
  closing: number;
}

export interface StockMovementSummary {
  from: string;
  to: string;
  totalOpening: number;
  totalIn: number;
  totalOut: number;
  totalAdjust: number;
  totalClosing: number;
  rows: StockMovementSummaryRow[];
}

export interface GetStockSummaryParams {
  from: string;
  to: string;
  warehouseId?: number;
}

/** Mirrors the backend `LowStockView` read model. */
export interface LowStockRow {
  itemId: number;
  sku: string;
  itemName: string;
  warehouseId: number | null;
  warehouseCode: string | null;
  warehouseName: string | null;
  unitId: number | null;
  unitCode: string | null;
  onHand: number;
  reserved: number;
  available: number;
  minStockLevel: number;
  reorderLevel: number;
  reorderQuantity: number;
  shortfall: number;
  suggestedOrderQty: number;
  status: "OUT_OF_STOCK" | "BELOW_MIN" | "BELOW_REORDER";
  lastMovementAt: string | null;
}

export interface GetLowStockParams {
  page?: number;
  size?: number;
  warehouseId?: number;
}

/** Mirrors the backend `StockValuationView` read model. */
export interface StockValuationRow {
  itemId: number;
  sku: string;
  itemName: string;
  warehouseId: number;
  warehouseCode: string;
  warehouseName: string;
  unitId: number;
  unitCode: string;
  onHand: number;
  reserved: number;
  available: number;
  unitCost: number;
  onHandValue: number;
  reservedValue: number;
  availableValue: number;
  costBasisMissing: boolean;
  lastMovementAt: string | null;
}

export interface StockValuation {
  totalOnHandValue: number;
  totalReservedValue: number;
  totalAvailableValue: number;
  itemsValued: number;
  itemsMissingCost: number;
  rows: PageResponse<StockValuationRow>;
}

export interface GetStockValuationParams {
  page?: number;
  size?: number;
  warehouseId?: number;
}

/** Mirrors the backend `NegativeStockView` read model. */
export interface NegativeStockRow {
  itemId: number;
  sku: string;
  itemName: string;
  warehouseId: number;
  warehouseCode: string;
  warehouseName: string;
  unitId: number;
  unitCode: string;
  onHand: number;
  reserved: number;
  available: number;
  negativeStockAllowed: boolean;
  lastMovementAt: string | null;
}

export interface GetNegativeStockParams {
  page?: number;
  size?: number;
  warehouseId?: number;
}

/** Mirrors the backend `StockAgingView` read model. */
export interface StockAgingRow {
  itemId: number;
  sku: string;
  itemName: string;
  warehouseId: number;
  warehouseCode: string;
  warehouseName: string;
  unitId: number;
  unitCode: string;
  onHand: number;
  reserved: number;
  available: number;
  unitCost: number;
  onHandValue: number;
  lastMovementAt: string | null;
  daysSinceMovement: number | null;
  ageBucket: "NEVER_MOVED" | "FRESH" | "AGING" | "SLOW" | "STAGNANT" | "DEAD";
}

export interface GetStockAgingParams {
  page?: number;
  size?: number;
  warehouseId?: number;
  minDays?: number;
}

/**
 * Stock on-hand report. Read-only, so no cache tags are needed. Tenant and
 * company are scoped by the backend from the X-Company-Id header.
 */
export const stockApi = api.injectEndpoints({
  endpoints: (build) => ({
    getStockOnHand: build.query<PageResponse<StockOnHandRow>, GetStockOnHandParams | void>({
      query: (params) => ({
        url: "/inventory/stock/on-hand",
        method: "GET",
        params: params ?? {},
      }),
    }),

    getStockCard: build.query<StockCard, GetStockCardParams>({
      query: (params) => ({
        url: "/inventory/stock/ledger",
        method: "GET",
        params,
      }),
    }),

    getStockMovementSummary: build.query<StockMovementSummary, GetStockSummaryParams>({
      query: (params) => ({
        url: "/inventory/stock/summary",
        method: "GET",
        params,
      }),
    }),

    getLowStock: build.query<PageResponse<LowStockRow>, GetLowStockParams | void>({
      query: (params) => ({
        url: "/inventory/stock/low-stock",
        method: "GET",
        params: params ?? {},
      }),
    }),

    getStockValuation: build.query<StockValuation, GetStockValuationParams | void>({
      query: (params) => ({
        url: "/inventory/stock/valuation",
        method: "GET",
        params: params ?? {},
      }),
    }),

    getNegativeStock: build.query<PageResponse<NegativeStockRow>, GetNegativeStockParams | void>({
      query: (params) => ({
        url: "/inventory/stock/negative",
        method: "GET",
        params: params ?? {},
      }),
    }),

    getStockAging: build.query<PageResponse<StockAgingRow>, GetStockAgingParams | void>({
      query: (params) => ({
        url: "/inventory/stock/aging",
        method: "GET",
        params: params ?? {},
      }),
    }),
  }),
});

export const {
  useGetStockOnHandQuery,
  useGetStockCardQuery,
  useGetStockMovementSummaryQuery,
  useGetLowStockQuery,
  useGetStockValuationQuery,
  useGetNegativeStockQuery,
  useGetStockAgingQuery,
} = stockApi;
