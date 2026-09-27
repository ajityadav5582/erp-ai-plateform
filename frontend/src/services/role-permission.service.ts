import { api } from "./api";

export interface RolePermissionResponse {
  id: number;
  roleId: number;
  permissionId: number;
  permissionCode: string;
  resource: string;
  action: string;
  description: string;
  status: string;
  assignedBy: string;
  assignedAt: string;
  active: boolean;
}

export interface RolePermissionListResponse {
  content: RolePermissionResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface AssignPermissionRequest {
  roleId: number;
  permissionId: number;
}

export interface RemovePermissionRequest {
  roleId: number;
  permissionId: number;
}

export interface GetRolePermissionsParams {
  page?: number;
  size?: number;
  sort?: string;
}

/**
 * Role-Permission API endpoints with RTK Query caching and invalidation.
 */
export const rolePermissionApi = api.injectEndpoints({
  endpoints: (build) => ({
    getRolePermissions: build.query<RolePermissionListResponse, { roleId: number; params?: GetRolePermissionsParams }>({
      query: ({ roleId, params }) => ({
        url: `/role-permissions/role/${roleId}`,
        method: "GET",
        params,
      }),
      providesTags: (_result, _error, { roleId }) => [{ type: "Permission" as const, id: roleId }],
    }),

    assignPermission: build.mutation<RolePermissionResponse, AssignPermissionRequest>({
      query: (body) => ({
        url: "/role-permissions",
        method: "POST",
        data: body,
      }),
      invalidatesTags: (_result, _error, { roleId }) => [
        { type: "Permission" as const, id: roleId },
        "Permission",
      ],
    }),

    removePermission: build.mutation<void, RemovePermissionRequest>({
      query: (body) => ({
        url: "/role-permissions",
        method: "DELETE",
        data: body,
      }),
      invalidatesTags: (_result, _error, { roleId }) => [
        { type: "Permission" as const, id: roleId },
        "Permission",
      ],
    }),
  }),
});

export const {
  useGetRolePermissionsQuery,
  useAssignPermissionMutation,
  useRemovePermissionMutation,
} = rolePermissionApi;
