import { api } from "./api";

export interface Company {
  id: number;
  companyCode: string;
  companyName: string;
  panNumber: string | null;
  city: string | null;
  phone: string;
  email: string;
  companyType: CompanyType | null;
  fiscalYearType: FiscalYearType | null;
  vatRegistered: boolean | null;
  status: "ACTIVE" | "INACTIVE";
}

export type CompanyType =
  | "PRIVATE_LIMITED"
  | "PUBLIC_LIMITED"
  | "SOLE_PROPRIETORSHIP"
  | "NGO_INGO"
  | "TRUST"
  | "COOPERATIVE"
  | "GOVERNMENT_ENTITY";

export type FiscalYearType = "NEPALI_BS" | "ENGLISH_AD";

export interface CreateCompanyRequest {
  companyName: string;
  panNumber: string;
  city: string;
  phone: string;
  email: string;
  companyType: CompanyType;
  fiscalYearType: FiscalYearType;
  vatRegistered: boolean;
}

export interface CompanyFiscalYear {
  id: number;
  companyId: number;
  masterFiscalYearId: number | null;
  name: string;
  code: string;
  startDateBs: string | null;
  endDateBs: string | null;
  startDateAd: string | null;
  endDateAd: string | null;
  calendarType: FiscalYearType;
  active: boolean;
}

export interface MasterFiscalYear {
  id: number;
  name: string;
  code: string;
  startDateBs: string;
  endDateBs: string;
  startDateAd: string;
  endDateAd: string;
  displayOrder: number;
}

export interface CreateCompanyFiscalYearRequest {
  masterFiscalYearId: number;
  name: string;
  startDateBs: string;
  endDateBs: string;
  startDateAd: string;
  endDateAd: string;
  calendarType: FiscalYearType;
}

export const companyApi = api.injectEndpoints({
  endpoints: (build) => ({
    getCompanies: build.query<Company[], void>({
      query: () => ({ url: "/identity/companies", method: "GET" }),
      providesTags: ["Company"],
    }),
    createCompany: build.mutation<Company, CreateCompanyRequest>({
      query: (data) => ({ url: "/identity/companies", method: "POST", data }),
      invalidatesTags: ["Company"],
    }),
    getMasterFiscalYears: build.query<MasterFiscalYear[], void>({
      query: () => ({ url: "/identity/master-fiscal-years", method: "GET" }),
    }),
    getCompanyFiscalYears: build.query<CompanyFiscalYear[], number>({
      query: (companyId) => ({ url: `/identity/companies/${companyId}/fiscal-years`, method: "GET" }),
      providesTags: (_result, _error, companyId) => [{ type: "CompanyFiscalYear", id: companyId }],
    }),
    createCompanyFiscalYear: build.mutation<CompanyFiscalYear, { companyId: number; data: CreateCompanyFiscalYearRequest }>({
      query: ({ companyId, data }) => ({ url: `/identity/companies/${companyId}/fiscal-years`, method: "POST", data }),
      invalidatesTags: (_result, _error, { companyId }) => [{ type: "CompanyFiscalYear", id: companyId }],
    }),
    activateCompanyFiscalYear: build.mutation<CompanyFiscalYear, { companyId: number; fiscalYearId: number }>({
      query: ({ companyId, fiscalYearId }) => ({ url: `/identity/companies/${companyId}/fiscal-years/${fiscalYearId}/activate`, method: "PUT" }),
      invalidatesTags: (_result, _error, { companyId }) => [{ type: "CompanyFiscalYear", id: companyId }],
    }),
  }),
});

export const {
  useGetCompaniesQuery,
  useCreateCompanyMutation,
  useGetMasterFiscalYearsQuery,
  useGetCompanyFiscalYearsQuery,
  useCreateCompanyFiscalYearMutation,
  useActivateCompanyFiscalYearMutation,
} = companyApi;
