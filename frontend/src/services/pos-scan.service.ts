import { api } from "./api";

/** Mirrors the backend `PosVatType` enum. */
export const POS_VAT_TYPES = ["TAXABLE", "EXEMPT", "ZERO_RATED", "EXPORT"] as const;
export type PosVatType = (typeof POS_VAT_TYPES)[number];

/** Mirrors the backend `ScannedItemResponse` record. */
export interface ScannedItem {
  id: number;
  sku: string;
  barcode: string | null;
  name: string;
  uomCode: string;
  unitPrice: number;
  vatType: PosVatType;
  vatRate: number;
  availableStock: number | null;
  isStockable: boolean;
}

/** One requested cart line for a price preview. */
export interface PriceLineRequest {
  itemId: number;
  quantity: number;
  discountPercentage?: number;
}

/** Mirrors the backend `PricedLine` record. */
export interface PricedLine {
  itemId: number;
  itemCode: string;
  itemName: string;
  uomCode: string;
  quantity: number;
  unitPrice: number;
  discountPercentage: number;
  vatType: PosVatType;
  vatRate: number;
  lineAmount: number;
  vatAmount: number;
}

/** Mirrors the backend `PricePreview` record. */
export interface PricePreview {
  lines: PricedLine[];
  subtotal: number;
  vatTotal: number;
  total: number;
}

const SCAN_URL = "/pos/scan";
const PRICING_URL = "/pos/pricing";

export const posScanApi = api.injectEndpoints({
  endpoints: (build) => ({
    /**
     * Resolves a scanned barcode / SKU / product code to a priced item in one
     * call. Not cached: every scan must see the current catalogue price.
     */
    scanPosItem: build.query<ScannedItem, string>({
      query: (code) => ({ url: SCAN_URL, method: "GET", params: { code } }),
      keepUnusedDataFor: 0,
    }),

    /**
     * Prices a whole cart server-side and returns the authoritative totals.
     * Not cached: the terminal calls it on every cart change.
     */
    previewPosPrices: build.mutation<PricePreview, PriceLineRequest[]>({
      query: (lines) => ({ url: `${PRICING_URL}/preview`, method: "POST", data: lines }),
    }),
  }),
});

export const { useLazyScanPosItemQuery, usePreviewPosPricesMutation } = posScanApi;
