import { api } from "./api";

export type ProvinceStatus = "ACTIVE" | "INACTIVE";

export interface ProvinceListResponse {
  id: number;
  provinceCode: string;
  provinceName: string;
  nepaliName: string | null;
  countryCode: string | null;
  status: ProvinceStatus;
}

export interface DistrictListResponse {
  id: number;
  tenantId: number;
  provinceId: number;
  districtCode: string;
  districtName: string;
  nepaliName: string | null;
  countryCode: string | null;
  status: ProvinceStatus;
}

export interface DistrictResponse {
  id: number;
  tenantId: number;
  provinceId: number;
  districtCode: string;
  districtName: string;
  nepaliName: string | null;
  countryCode: string | null;
  status: ProvinceStatus;
}

export interface LocalLevelListResponse {
  municipalityId: string;
  name: string;
  nepaliName: string | null;
  districtId: number;
  localLevelTypeId: string | null;
}

export interface LocalLevelResponse {
  municipalityId: string;
  name: string;
  nepaliName: string | null;
  districtId: number;
  localLevelTypeId: string | null;
}

/**
 * Geography API endpoints with RTK Query caching.
 *
 * <p>Provides province -> district -> local level lookups so that
 * branch location can be selected through cascading dropdowns.
 */
export const geographyApi = api.injectEndpoints({
  endpoints: (build) => ({
    getProvinces: build.query<ProvinceListResponse[], void>({
      query: () => ({
        url: "/provinces/active",
        method: "GET",
      }),
      providesTags: ["Province"],
    }),

    getDistrictsByProvince: build.query<DistrictListResponse[], number>({
      query: (provinceId) => ({
        url: "/districts/by-province",
        method: "GET",
        params: { provinceId },
      }),
      providesTags: (_result, _error, provinceId) => [
        { type: "District", id: provinceId },
      ],
    }),

    getDistrictById: build.query<DistrictResponse, number>({
      query: (districtId) => ({
        url: `/districts/${districtId}`,
        method: "GET",
      }),
      providesTags: (_result, _error, districtId) => [
        { type: "District", id: districtId },
      ],
    }),

    getLocalLevelsByDistrict: build.query<LocalLevelListResponse[], number>({
      query: (districtId) => ({
        url: "/local-levels/by-district",
        method: "GET",
        params: { districtId },
      }),
      providesTags: (_result, _error, districtId) => [
        { type: "LocalLevel", id: districtId },
      ],
    }),

    getLocalLevelByMunicipalityId: build.query<LocalLevelResponse, string>({
      query: (municipalityId) => ({
        url: "/local-levels/by-municipality-id",
        method: "GET",
        params: { municipalityId },
      }),
      providesTags: (_result, _error, municipalityId) => [
        { type: "LocalLevel", id: municipalityId },
      ],
    }),
  }),
});

export const {
  useGetProvincesQuery,
  useGetDistrictsByProvinceQuery,
  useGetDistrictByIdQuery,
  useGetLocalLevelsByDistrictQuery,
  useGetLocalLevelByMunicipalityIdQuery,
} = geographyApi;
