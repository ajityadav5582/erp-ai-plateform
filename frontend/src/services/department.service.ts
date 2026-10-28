import { api } from "./api";
import type { PageResponse } from "@/types/api";

export type DepartmentStatus = "ACTIVE" | "INACTIVE";

export interface DepartmentListResponse {
  id: number;
  departmentId: string;
  departmentCode: string;
  departmentName: string;
  branchId: number;
  parentDepartmentId: number | null;
  managerId: number | null;
  status: DepartmentStatus;
  createdAt: string;
}

export interface Department {
  id: number;
  departmentId: string;
  tenantId: number;
  branchId: number;
  departmentCode: string;
  departmentName: string;
  description: string;
  managerId: number | null;
  parentDepartmentId: number | null;
  status: DepartmentStatus;
  createdAt: string;
  updatedAt: string;
  createdBy: string;
  updatedBy: string;
  version: number;
}

export interface CreateDepartmentRequest {
  departmentCode: string;
  departmentName: string;
  description?: string;
  branchId: number;
  managerId?: number;
  parentDepartmentId?: number;
}

export interface UpdateDepartmentRequest {
  departmentCode?: string;
  departmentName?: string;
  description?: string;
  branchId?: number;
  managerId?: number;
  parentDepartmentId?: number;
}

export interface GetDepartmentsParams {
  page?: number;
  size?: number;
  sort?: string;
  search?: string;
  status?: DepartmentStatus;
  branchId?: number;
}

/**
 * Department API endpoints with RTK Query caching and invalidation.
 */
export const departmentApi = api.injectEndpoints({
  endpoints: (build) => ({
    getDepartments: build.query<PageResponse<DepartmentListResponse>, GetDepartmentsParams | void>({
      query: (params) => ({
        url: "/identity/departments",
        method: "GET",
        params: params ?? {},
      }),
      providesTags: ["Department"],
    }),

    getDepartment: build.query<Department, string>({
      query: (departmentId) => ({
        url: `/identity/departments/${departmentId}`,
        method: "GET",
      }),
      providesTags: (_result, _error, departmentId) => [{ type: "Department", id: departmentId }],
    }),

    createDepartment: build.mutation<Department, CreateDepartmentRequest>({
      query: (body) => ({
        url: "/identity/departments",
        method: "POST",
        data: body,
      }),
      invalidatesTags: ["Department"],
    }),

    updateDepartment: build.mutation<Department, { departmentId: string; data: UpdateDepartmentRequest }>({
      query: ({ departmentId, data }) => ({
        url: `/identity/departments/${departmentId}`,
        method: "PUT",
        data,
      }),
      invalidatesTags: (_result, _error, { departmentId }) => [
        { type: "Department", id: departmentId },
        "Department",
      ],
    }),

    activateDepartment: build.mutation<Department, string>({
      query: (departmentId) => ({
        url: `/identity/departments/${departmentId}/activate`,
        method: "POST",
      }),
      invalidatesTags: (_result, _error, departmentId) => [
        { type: "Department", id: departmentId },
        "Department",
      ],
    }),

    deactivateDepartment: build.mutation<Department, string>({
      query: (departmentId) => ({
        url: `/identity/departments/${departmentId}/deactivate`,
        method: "POST",
      }),
      invalidatesTags: (_result, _error, departmentId) => [
        { type: "Department", id: departmentId },
        "Department",
      ],
    }),

    deleteDepartment: build.mutation<void, string>({
      query: (departmentId) => ({
        url: `/identity/departments/${departmentId}`,
        method: "DELETE",
      }),
      invalidatesTags: ["Department"],
    }),
  }),
});

export const {
  useGetDepartmentsQuery,
  useGetDepartmentQuery,
  useCreateDepartmentMutation,
  useUpdateDepartmentMutation,
  useActivateDepartmentMutation,
  useDeactivateDepartmentMutation,
  useDeleteDepartmentMutation,
} = departmentApi;
