import { api } from "./api";

/** Mirrors the backend `SalesRegisterResponse` record. */
export interface SalesRegister {
  periodFrom: string;
  periodTo: string;
  invoiceCount: number;
  creditNoteCount: number;
  taxableSales: number;
  exemptSales: number;
  zeroRatedSales: number;
  outputVat: number;
  creditTaxable: number;
  creditVat: number;
  netTaxableSales: number;
  netOutputVat: number;
}

export interface GetRegisterParams {
  from: string;
  to: string;
}

const BASE_URL = "/sales/register";

/**
 * Sales register API: the VAT-201 figures for a period, net of credit notes.
 */
export const salesRegisterApi = api.injectEndpoints({
  endpoints: (build) => ({
    getSalesRegister: build.query<SalesRegister, GetRegisterParams>({
      query: (params) => ({ url: BASE_URL, method: "GET", params }),
      providesTags: ["SalesRegister"],
    }),
  }),
});

export const { useGetSalesRegisterQuery } = salesRegisterApi;
