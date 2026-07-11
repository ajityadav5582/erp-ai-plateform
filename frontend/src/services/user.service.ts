import { api } from "./api";

/**
 * User API endpoints.
 */

export const userApi = api.injectEndpoints({
  endpoints: (build) => ({
    getUsers: build.query<
      { content: unknown[]; totalElements: number; totalPages: number },
      { page?: number; size?: number; sort?: string; search?: string }
    >({
      query: (params) => ({
        url: "/users",
        method: "GET",
        params,
      }),
      providesTags: ["User"],
    }),

    getUser: build.query<unknown, string>({
      query: (id) => ({
        url: `/users/${id}`,
        method: "GET",
      }),
      providesTags: (_result, _error, id) => [{ type: "User", id }],
    }),

    createUser: build.mutation<unknown, unknown>({
      query: (body) => ({
        url: "/users",
        method: "POST",
        data: body,
      }),
      invalidatesTags: ["User"],
    }),

    updateUser: build.mutation<unknown, { id: string; data: unknown }>({
      query: ({ id, data }) => ({
        url: `/users/${id}`,
        method: "PUT",
        data,
      }),
      invalidatesTags: (_result, _error, { id }) => [{ type: "User", id }, "User"],
    }),

    deleteUser: build.mutation<void, string>({
      query: (id) => ({
        url: `/users/${id}`,
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
  useDeleteUserMutation,
} = userApi;
