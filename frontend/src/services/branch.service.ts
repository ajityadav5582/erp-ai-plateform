import { api } from "./api";

export type BranchStatus = "ACTIVE" | "INACTIVE";

export interface BranchListResponse {
  id: number;
  branchCode: string;
  branchName: string;
  localLevelId: string | null;
  status: BranchStatus;
  createdAt: string;
}

export interface Branch {
  id: number;
  tenantId: number;
  branchCode: string;
  branchName: string;
  email: string;
  phone: string;
  address: string;
  localLevelId: string | null;
  wardNo: string | null;
  status: BranchStatus;
  createdAt: string;
  updatedAt: string;
  createdBy: string;
  updatedBy: string;
  version: number;
}

export interface CreateBranchRequest {
  branchCode: string;
  branchName: string;
  email?: string;
  phone?: string;
  address?: string;
  localLevelId?: string;
  wardNo?: string;
}

export interface UpdateBranchRequest {
  branchCode?: string;
  branchName?: string;
  email?: string;
  phone?: string;
  address?: string;
  localLevelId?: string;
  wardNo?: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface GetBranchesParams {
  page?: number;
  size?: number;
  sort?: string;
  search?: string;
  status?: BranchStatus;
}

/**
 * Branch API endpoints with RTK Query caching and invalidation.
 */
export const branchApi = api.injectEndpoints({
  endpoints: (build) => ({
    getBranches: build.query<PageResponse<BranchListResponse>, GetBranchesParams | void>({
      query: (params) => ({
        url: "/branches",
        method: "GET",
        params: params ?? {},
      }),
      providesTags: ["Branch"],
    }),

    getBranch: build.query<Branch, number>({
      query: (branchId) => ({
        url: `/branches/${branchId}`,
        method: "GET",
      }),
      providesTags: (_result, _error, branchId) => [{ type: "Branch", id: branchId }],
    }),

    createBranch: build.mutation<Branch, CreateBranchRequest>({
      query: (body) => ({
        url: "/branches",
        method: "POST",
        data: body,
      }),
      invalidatesTags: ["Branch"],
    }),

    updateBranch: build.mutation<Branch, { branchId: number; data: UpdateBranchRequest }>({
      query: ({ branchId, data }) => ({
        url: `/branches/${branchId}`,
        method: "PUT",
        data,
      }),
      invalidatesTags: (_result, _error, { branchId }) => [
        { type: "Branch", id: branchId },
        "Branch",
      ],
    }),

    activateBranch: build.mutation<Branch, number>({
      query: (branchId) => ({
        url: `/branches/${branchId}/activate`,
        method: "POST",
      }),
      invalidatesTags: (_result, _error, branchId) => [
        { type: "Branch", id: branchId },
        "Branch",
      ],
    }),

    deactivateBranch: build.mutation<Branch, number>({
      query: (branchId) => ({
        url: `/branches/${branchId}/deactivate`,
        method: "POST",
      }),
      invalidatesTags: (_result, _error, branchId) => [
        { type: "Branch", id: branchId },
        "Branch",
      ],
    }),

    deleteBranch: build.mutation<void, number>({
      query: (branchId) => ({
        url: `/branches/${branchId}`,
        method: "DELETE",
      }),
      invalidatesTags: ["Branch"],
    }),
  }),
});

export const {
  useGetBranchesQuery,
  useGetBranchQuery,
  useCreateBranchMutation,
  useUpdateBranchMutation,
  useActivateBranchMutation,
  useDeactivateBranchMutation,
  useDeleteBranchMutation,
} = branchApi;
