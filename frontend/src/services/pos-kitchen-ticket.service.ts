import { api } from "./api";

export interface PosKitchenTicket {
  id: number;
  posTransactionId: number;
  status: string;
}

export const posKitchenTicketApi = api.injectEndpoints({
  endpoints: (build) => ({
    createPosKitchenTicket: build.mutation<PosKitchenTicket, number>({
      query: (posTransactionId) => ({ url: "/pos/kitchen-tickets", method: "POST", params: { posTransactionId } }),
      invalidatesTags: ["PosKitchenTicket"],
    }),
    getPosKitchenTicketsByTransaction: build.query<PosKitchenTicket[], number>({
      query: (posTransactionId) => ({ url: `/pos/kitchen-tickets/by-transaction/${posTransactionId}`, method: "GET" }),
      providesTags: ["PosKitchenTicket"],
    }),
  }),
});

export const { useCreatePosKitchenTicketMutation, useGetPosKitchenTicketsByTransactionQuery } = posKitchenTicketApi;
