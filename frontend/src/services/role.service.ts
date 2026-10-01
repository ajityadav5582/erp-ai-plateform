import { api } from "./api";
import type { PageResponse } from "@/types/api";

export type RoleType = "SYSTEM" | "CUSTOM";
export type RoleStatus = "ACTIVE" | "INACTIVE";

export interface RoleListResponse {
  id: number;
  tenantId: number;
  roleCode: string;
  roleName: string;
  description: string;
  roleType: RoleType;
  isSystemRole: boolean;
  isActive: boolean;
}

export interface Role {
  id: number;
  tenantId: number;
  roleCode: string;
  roleName: string;
  description: string;
  roleType: RoleType;
  isSystemRole: boolean;
  isActive: boolean;
  permissionIds?: number[];
}

export interface CreateRoleRequest {
  roleCode: string;
  roleName: string;
  description?: string;
  permissionIds?: number[];
}

export interface UpdateRoleRequest {
  roleName?: string;
  description?: string;
  permissionIds?: number[];
}

export interface GetRolesParams {
  page?: number;
  size?: number;
  sort?: string;
  search?: string;
  roleType?: RoleType;
}

/**
 * Role API endpoints with RTK Query caching and invalidation.
 */
export const roleApi = api.injectEndpoints({
  endpoints: (build) => ({
    getRoles: build.query<PageResponse<RoleListResponse>, GetRolesParams | void>({
      query: (params) => ({
        url: "/identity/roles",
        method: "GET",
        params: params ?? {},
      }),
      providesTags: ["Role"],
    }),

    getRole: build.query<Role, number>({
      query: (roleId) => ({
        url: `/identity/roles/${roleId}`,
        method: "GET",
      }),
      providesTags: (_result, _error, roleId) => [{ type: "Role", id: roleId }],
    }),

    createRole: build.mutation<Role, CreateRoleRequest>({
      query: (body) => ({
        url: "/identity/roles",
        method: "POST",
        data: body,
      }),
      invalidatesTags: ["Role"],
    }),

    updateRole: build.mutation<Role, { roleId: number; data: UpdateRoleRequest }>({
      query: ({ roleId, data }) => ({
        url: `/identity/roles/${roleId}`,
        method: "PUT",
        data,
      }),
      invalidatesTags: (_result, _error, { roleId }) => [
        { type: "Role", id: roleId },
        "Role",
      ],
    }),

    deleteRole: build.mutation<void, number>({
      query: (roleId) => ({
        url: `/identity/roles/${roleId}`,
        method: "DELETE",
      }),
      invalidatesTags: ["Role"],
    }),
  }),
});

export const {
  useGetRolesQuery,
  useGetRoleQuery,
  useCreateRoleMutation,
  useUpdateRoleMutation,
  useDeleteRoleMutation,
} = roleApi;
