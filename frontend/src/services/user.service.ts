import { api } from "./api";

export type UserStatus =
  | "ACTIVE"
  | "INACTIVE"
  | "LOCKED"
  | "PENDING_ACTIVATION"
  | "ARCHIVED";

export interface UserListResponse {
  id: number;
  userId: number;
  username: string;
  email: string;
  fullName: string;
  status: UserStatus;
  roleId?: number | null;
  roleName?: string | null;
  roleCode?: string | null;
  branchId?: number | null;
  departmentId?: number | null;
  lastLoginAt?: string | null;
  createdAt?: string | null;
}

export interface User {
  id: number;
  userId: number;
  tenantId: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  fullName: string;
  phoneNumber?: string | null;
  roleId?: number | null;
  roleName?: string | null;
  roleCode?: string | null;
  profileImageUrl?: string | null;
  status: UserStatus;
  branchId?: number | null;
  departmentId?: number | null;
  lastLoginAt?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
  createdBy?: string | null;
  updatedBy?: string | null;
  version?: number;
}

export interface CreateUserRequest {
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  password?: string;
  phoneNumber?: string;
  roleId?: number;
  profileImageUrl?: string;
  branchId?: number;
  departmentId?: number;
}

export interface UpdateUserRequest {
  email?: string;
  firstName?: string;
  lastName?: string;
  phoneNumber?: string;
  roleId?: number;
  profileImageUrl?: string;
  branchId?: number;
  departmentId?: number;
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

export interface GetUsersParams {
  page?: number;
  size?: number;
  sort?: string;
  search?: string;
  status?: UserStatus;
  branchId?: number;
  departmentId?: number;
}

/**
 * User API endpoints with RTK Query caching and invalidation.
 */
export const userApi = api.injectEndpoints({
  endpoints: (build) => ({
    getUsers: build.query<PageResponse<UserListResponse>, GetUsersParams | void>({
      query: (params) => ({
        url: "/users",
        method: "GET",
        params: params ?? {},
      }),
      providesTags: ["User"],
    }),

    getUser: build.query<User, number>({
      query: (userId) => ({
        url: `/users/${userId}`,
        method: "GET",
      }),
      providesTags: (_result, _error, userId) => [{ type: "User", id: userId }],
    }),

    createUser: build.mutation<User, CreateUserRequest>({
      query: (body) => ({
        url: "/users",
        method: "POST",
        data: body,
      }),
      invalidatesTags: ["User"],
    }),

    updateUser: build.mutation<User, { userId: number; data: UpdateUserRequest }>({
      query: ({ userId, data }) => ({
        url: `/users/${userId}`,
        method: "PUT",
        data,
      }),
      invalidatesTags: (_result, _error, { userId }) => [
        { type: "User", id: userId },
        "User",
      ],
    }),

    activateUser: build.mutation<User, number>({
      query: (userId) => ({
        url: `/users/${userId}/activate`,
        method: "POST",
      }),
      invalidatesTags: (_result, _error, userId) => [
        { type: "User", id: userId },
        "User",
      ],
    }),

    deactivateUser: build.mutation<User, number>({
      query: (userId) => ({
        url: `/users/${userId}/deactivate`,
        method: "POST",
      }),
      invalidatesTags: (_result, _error, userId) => [
        { type: "User", id: userId },
        "User",
      ],
    }),

    deleteUser: build.mutation<void, number>({
      query: (userId) => ({
        url: `/users/${userId}`,
        method: "DELETE",
      }),
      invalidatesTags: ["User"],
    }),
  }),
});

export const {
  useGetUsersQuery,
  useGetUserQuery,
  useCreateUserMutation,
  useUpdateUserMutation,
  useActivateUserMutation,
  useDeactivateUserMutation,
  useDeleteUserMutation,
} = userApi;
