import { api } from "./api";
import type { PageResponse } from "@/types/api";

/** Mirrors the backend `PosQuickItemResponse` record. */
export interface PosQuickItem {
  id: number;
  tenantId: number;
  companyId: number;
  registerId: number | null;
  itemId: number;
  itemCode: string;
  itemName: string;
  uomCode: string;
  defaultPrice: number | null;
  gridRow: number;
  gridCol: number;
  displayColor: string | null;
  displayIcon: string | null;
  sortOrder: number;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface CreateQuickItemRequest {
  registerId?: number | null;
  itemId: number;
  defaultPrice?: number | null;
  gridRow: number;
  gridCol: number;
  displayColor?: string | null;
  displayIcon?: string | null;
  sortOrder: number;
}

export interface UpdateQuickItemRequest {
  itemId: number;
  defaultPrice?: number | null;
  gridRow: number;
  gridCol: number;
  displayColor?: string | null;
  displayIcon?: string | null;
  sortOrder: number;
}

export interface ReorderQuickItemsRequest {
  placements: {
    id: number;
    gridRow: number;
    gridCol: number;
    sortOrder: number;
  }[];
}

export interface GetPosQuickItemsParams {
  page?: number;
  size?: number;
  sort?: string;
  registerId?: number;
  term?: string;
}

const BASE_URL = "/pos/quick-items";

export const posQuickItemApi = api.injectEndpoints({
  endpoints: (build) => ({
    getPosQuickItems: build.query<PageResponse<PosQuickItem>, GetPosQuickItemsParams | void>({
      query: (params) => ({ url: BASE_URL, method: "GET", params: params ?? {} }),
      providesTags: (result) => [
        ...(result?.content.map((q) => ({ type: "PosQuickItem" as const, id: q.id })) ?? []),
        "PosQuickItem",
      ],
    }),

    getPosQuickItem: build.query<PosQuickItem, number>({
      query: (id) => ({ url: `${BASE_URL}/${id}`, method: "GET" }),
      providesTags: (_r, _e, id) => [{ type: "PosQuickItem", id }, "PosQuickItem"],
    }),

    /**
     * The terminal grid load: every company-shared tile plus one register's
     * tiles, active first, ordered by grid position. One call renders the grid.
     */
    getPosQuickItemGrid: build.query<PosQuickItem[], number>({
      query: (registerId) => ({ url: `${BASE_URL}/grid`, method: "GET", params: { registerId } }),
      providesTags: ["PosQuickItem"],
    }),

    createPosQuickItem: build.mutation<PosQuickItem, CreateQuickItemRequest>({
      query: (body) => ({ url: BASE_URL, method: "POST", data: body }),
      invalidatesTags: ["PosQuickItem"],
    }),

    updatePosQuickItem: build.mutation<PosQuickItem, { id: number; body: UpdateQuickItemRequest }>({
      query: ({ id, body }) => ({ url: `${BASE_URL}/${id}`, method: "PUT", data: body }),
      invalidatesTags: (_r, _e, { id }) => [{ type: "PosQuickItem", id }, "PosQuickItem"],
    }),

    activatePosQuickItem: build.mutation<PosQuickItem, number>({
      query: (id) => ({ url: `${BASE_URL}/${id}/activate`, method: "POST" }),
      invalidatesTags: (_r, _e, id) => [{ type: "PosQuickItem", id }, "PosQuickItem"],
    }),

    deactivatePosQuickItem: build.mutation<PosQuickItem, number>({
      query: (id) => ({ url: `${BASE_URL}/${id}/deactivate`, method: "POST" }),
      invalidatesTags: (_r, _e, id) => [{ type: "PosQuickItem", id }, "PosQuickItem"],
    }),

    deletePosQuickItem: build.mutation<void, number>({
      query: (id) => ({ url: `${BASE_URL}/${id}`, method: "DELETE" }),
      invalidatesTags: ["PosQuickItem"],
    }),

    reorderPosQuickItems: build.mutation<PosQuickItem[], ReorderQuickItemsRequest>({
      query: (body) => ({ url: `${BASE_URL}/reorder`, method: "PUT", data: body }),
      invalidatesTags: ["PosQuickItem"],
    }),
  }),
});

export const {
  useGetPosQuickItemsQuery,
  useGetPosQuickItemQuery,
  useGetPosQuickItemGridQuery,
  useCreatePosQuickItemMutation,
  useUpdatePosQuickItemMutation,
  useActivatePosQuickItemMutation,
  useDeactivatePosQuickItemMutation,
  useDeletePosQuickItemMutation,
  useReorderPosQuickItemsMutation,
} = posQuickItemApi;
