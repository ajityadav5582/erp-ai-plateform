import { api } from "./api";

export interface PosTable {
  id: number;
  tableName: string;
  branchId: number | null;
  status: string;
}

export const posTableApi = api.injectEndpoints({
  endpoints: (build) => ({
    getPosTables: build.query<PosTable[], void>({
      query: () => ({ url: "/pos/tables", method: "GET", params: { size: 100 } }),
      transformResponse: (response: { content?: PosTable[] } | PosTable[]) =>
        Array.isArray(response) ? response : response.content ?? [],
      providesTags: ["PosTable"],
    }),
    occupyPosTable: build.mutation<PosTable, number>({
      query: (id) => ({ url: `/pos/tables/${id}/occupy`, method: "POST" }),
      invalidatesTags: ["PosTable"],
    }),
    releasePosTable: build.mutation<PosTable, number>({
      query: (id) => ({ url: `/pos/tables/${id}/release`, method: "POST" }),
      invalidatesTags: ["PosTable"],
    }),
  }),
});

export const { useGetPosTablesQuery, useOccupyPosTableMutation, useReleasePosTableMutation } = posTableApi;
