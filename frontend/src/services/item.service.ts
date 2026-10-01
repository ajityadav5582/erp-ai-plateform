import { api } from "./api";
import type { PageResponse } from "@/types/api";

/**
 * Mirrors the backend `ItemType` enum. Jackson rejects any value outside this
 * union with a 400, so the union is the contract, not a convenience.
 */
export const ITEM_TYPES = ["GOODS", "SERVICE"] as const;
export type ItemType = (typeof ITEM_TYPES)[number];

export const ITEM_TYPE_LABELS: Record<ItemType, string> = {
  GOODS: "Goods",
  SERVICE: "Service",
};

/** Mirrors the backend `TaxabilityType` enum (Nepal VAT Act 2052). */
export const TAXABILITY_TYPES = ["TAXABLE", "EXEMPT", "ZERO_RATED"] as const;
export type TaxabilityType = (typeof TAXABILITY_TYPES)[number];

export const TAXABILITY_LABELS: Record<TaxabilityType, string> = {
  TAXABLE: "Taxable",
  EXEMPT: "Exempt",
  ZERO_RATED: "Zero Rated",
};

/** Mirrors the backend `ExciseType` enum (Nepal Excise Act 2058). */
export const EXCISE_TYPES = ["NONE", "PERCENTAGE", "SPECIFIC_AMOUNT"] as const;
export type ExciseType = (typeof EXCISE_TYPES)[number];

export const EXCISE_TYPE_LABELS: Record<ExciseType, string> = {
  NONE: "No Excise",
  PERCENTAGE: "Excise %",
  SPECIFIC_AMOUNT: "Excise per Unit",
};

export function formatItemType(itemType: string | null | undefined): string {
  if (!itemType) return "—";
  return ITEM_TYPE_LABELS[itemType as ItemType] ?? itemType;
}

export function formatTaxabilityType(taxabilityType: string | null | undefined): string {
  if (!taxabilityType) return "—";
  return TAXABILITY_LABELS[taxabilityType as TaxabilityType] ?? taxabilityType;
}

export function formatExciseType(exciseType: string | null | undefined): string {
  if (!exciseType) return "—";
  return EXCISE_TYPE_LABELS[exciseType as ExciseType] ?? exciseType;
}

/**
 * Standard Nepal VAT rate. Kept in sync with `Item.DEFAULT_VAT_RATE` on the
 * backend, which applies the same default when a taxable item is saved
 * without an explicit rate.
 */
export const DEFAULT_VAT_RATE = 13;

/**
 * Mirrors the backend `ItemListResponse` — the projection returned by every
 * list endpoint. The detail view uses the wider `Item` shape instead.
 */
export interface ItemListResponse {
  id: number;
  sku: string;
  barcode: string | null;
  hsCode: string | null;
  nameEn: string;
  nameNp: string | null;
  categoryId: number | null;
  categoryName: string | null;
  uomId: number;
  uomCode: string | null;
  itemType: ItemType | null;
  taxabilityType: TaxabilityType | null;
  vatRate: number | null;
  sellingPrice: number;
  purchasePrice: number | null;
  mrp: number | null;
  isActive: boolean;
}

/** Mirrors the backend `ItemResponse` record. */
export interface Item extends ItemListResponse {
  tenantId: number;
  companyId: number;
  uomName: string | null;
  descriptionEn: string | null;
  descriptionNp: string | null;
  exciseType: ExciseType | null;
  exciseRate: number | null;
  exciseAmount: number | null;
  isVatInclusive: boolean;
  isInventoryTracked: boolean;
  isBatchTracked: boolean;
  isExpiryTracked: boolean;
  minStockLevel: number | null;
  reorderLevel: number | null;
  reorderQuantity: number | null;
  createdAt: string;
  updatedAt: string;
  createdBy: string;
  updatedBy: string;
  version: number;
}

/** Mirrors the backend `CreateItemRequest` record. */
export interface CreateItemRequest {
  sku: string;
  nameEn: string;
  uomId: number;
  sellingPrice: number;
  barcode?: string;
  hsCode?: string;
  nameNp?: string;
  descriptionEn?: string;
  descriptionNp?: string;
  categoryId?: number;
  itemType?: ItemType;
  taxabilityType?: TaxabilityType;
  vatRate?: number;
  exciseType?: ExciseType;
  exciseRate?: number;
  exciseAmount?: number;
  purchasePrice?: number;
  mrp?: number;
  isVatInclusive?: boolean;
  isInventoryTracked?: boolean;
  isBatchTracked?: boolean;
  isExpiryTracked?: boolean;
  minStockLevel?: number;
  reorderLevel?: number;
  reorderQuantity?: number;
}

/**
 * Mirrors the backend `UpdateItemRequest`.
 *
 * Every field is optional and an omitted field means "leave unchanged" on the
 * server. Clearing a text field is done with an empty string, which the entity
 * normalises to null; optional numbers and the category have no clearing path
 * because a null there is indistinguishable from "unchanged".
 */
export interface UpdateItemRequest {
  sku?: string;
  nameEn?: string;
  uomId?: number;
  sellingPrice?: number;
  barcode?: string;
  hsCode?: string;
  nameNp?: string;
  descriptionEn?: string;
  descriptionNp?: string;
  categoryId?: number;
  itemType?: ItemType;
  taxabilityType?: TaxabilityType;
  vatRate?: number;
  exciseType?: ExciseType;
  exciseRate?: number;
  exciseAmount?: number;
  purchasePrice?: number;
  mrp?: number;
  isVatInclusive?: boolean;
  isInventoryTracked?: boolean;
  isBatchTracked?: boolean;
  isExpiryTracked?: boolean;
  minStockLevel?: number;
  reorderLevel?: number;
  reorderQuantity?: number;
}

export interface GetItemsParams {
  page?: number;
  size?: number;
  sort?: string;
  /**
   * Free-text term. The backend rejects blank or overlong terms with a 400, so
   * callers must omit the parameter entirely when the search box is empty.
   */
  search?: string;
}

/**
 * Item API endpoints with RTK Query caching and invalidation.
 *
 * None of these endpoints take a `companyId`: the backend scopes every call to
 * the company in the `X-Company-Id` header, which the Axios request interceptor
 * attaches from the user's current company/fiscal-year selection.
 */
export const itemApi = api.injectEndpoints({
  endpoints: (build) => ({
    getItems: build.query<PageResponse<ItemListResponse>, GetItemsParams | void>({
      query: (params) => {
        const { search, ...rest } = params ?? {};
        return {
          url: search?.trim()
            ? "/inventory/items/search"
            : "/inventory/items",
          method: "GET",
          params: search?.trim() ? { ...rest, query: search.trim() } : rest,
        };
      },
      providesTags: ["Item"],
    }),

    getItem: build.query<Item, string>({
      query: (itemId) => ({
        url: `/inventory/items/${itemId}`,
        method: "GET",
      }),
      providesTags: (_result, _error, itemId) => [{ type: "Item", id: itemId }],
    }),

    getItemBySku: build.query<Item, string>({
      query: (sku) => ({
        url: `/inventory/items/by-sku/${encodeURIComponent(sku)}`,
        method: "GET",
      }),
      providesTags: (_result, _error, sku) => [{ type: "Item", id: `sku-${sku}` }],
    }),

    getItemByBarcode: build.query<Item, string>({
      query: (barcode) => ({
        url: `/inventory/items/by-barcode/${encodeURIComponent(barcode)}`,
        method: "GET",
      }),
      providesTags: (_result, _error, barcode) => [
        { type: "Item", id: `barcode-${barcode}` },
      ],
    }),

    getActiveItems: build.query<PageResponse<ItemListResponse>, GetItemsParams | void>({
      query: (params) => ({
        url: "/inventory/items/active",
        method: "GET",
        params: params ?? {},
      }),
      providesTags: ["Item"],
    }),

    getItemsByCategory: build.query<
      PageResponse<ItemListResponse>,
      { categoryId: number; page?: number; size?: number; sort?: string }
    >({
      query: ({ categoryId, ...params }) => ({
        url: `/inventory/items/by-category/${categoryId}`,
        method: "GET",
        params,
      }),
      providesTags: ["Item"],
    }),

    getItemsByTaxability: build.query<
      PageResponse<ItemListResponse>,
      { taxabilityType: TaxabilityType; page?: number; size?: number; sort?: string }
    >({
      query: ({ taxabilityType, ...params }) => ({
        url: `/inventory/items/by-taxability/${taxabilityType}`,
        method: "GET",
        params,
      }),
      providesTags: ["Item"],
    }),

    createItem: build.mutation<Item, CreateItemRequest>({
      query: (body) => ({
        url: "/inventory/items",
        method: "POST",
        data: body,
      }),
      invalidatesTags: ["Item"],
    }),

    updateItem: build.mutation<Item, { itemId: number; data: UpdateItemRequest }>({
      query: ({ itemId, data }) => ({
        url: `/inventory/items/${itemId}`,
        method: "PUT",
        data,
      }),
      invalidatesTags: (_result, _error, { itemId }) => [
        { type: "Item", id: itemId.toString() },
        "Item",
      ],
    }),

    patchItem: build.mutation<Item, { itemId: number; data: UpdateItemRequest }>({
      query: ({ itemId, data }) => ({
        url: `/inventory/items/${itemId}`,
        method: "PATCH",
        data,
      }),
      invalidatesTags: (_result, _error, { itemId }) => [
        { type: "Item", id: itemId.toString() },
        "Item",
      ],
    }),

    activateItem: build.mutation<Item, number>({
      query: (itemId) => ({
        url: `/inventory/items/${itemId}/activate`,
        method: "POST",
      }),
      invalidatesTags: (_result, _error, itemId) => [
        { type: "Item", id: itemId.toString() },
        "Item",
      ],
    }),

    deactivateItem: build.mutation<Item, number>({
      query: (itemId) => ({
        url: `/inventory/items/${itemId}/deactivate`,
        method: "POST",
      }),
      invalidatesTags: (_result, _error, itemId) => [
        { type: "Item", id: itemId.toString() },
        "Item",
      ],
    }),

    deleteItem: build.mutation<void, number>({
      query: (itemId) => ({
        url: `/inventory/items/${itemId}`,
        method: "DELETE",
      }),
      invalidatesTags: ["Item"],
    }),
  }),
});

export const {
  useGetItemsQuery,
  useGetItemQuery,
  useGetItemBySkuQuery,
  useGetItemByBarcodeQuery,
  useGetActiveItemsQuery,
  useGetItemsByCategoryQuery,
  useGetItemsByTaxabilityQuery,
  useCreateItemMutation,
  useUpdateItemMutation,
  usePatchItemMutation,
  useActivateItemMutation,
  useDeactivateItemMutation,
  useDeleteItemMutation,
} = itemApi;
