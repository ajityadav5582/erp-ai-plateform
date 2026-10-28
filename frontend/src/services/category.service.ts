import { api } from "./api";
import type { PageResponse } from "@/types/api";

export type CategoryStatus = "ACTIVE" | "INACTIVE";

export interface CategoryListResponse {
  id: number;
  tenantId: number;
  parentId: number | null;
  parentName: string | null;
  name: string;
  slug: string;
  isActive: boolean;
  createdAt: string;
}

export interface Category {
  id: number;
  tenantId: number;
  companyId: number;
  parentId: number | null;
  name: string;
  slug: string;
  description: string | null;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
  createdBy: string;
  updatedBy: string;
  version: number;
}

export interface CreateCategoryRequest {
  name: string;
  slug: string;
  description?: string;
  parentId?: number;
}

export interface UpdateCategoryRequest {
  name?: string;
  slug?: string;
  description?: string;
  /**
   * Three distinct states, matching the backend:
   *   omitted -> leave the parent untouched
   *   number  -> re-parent under that category
   *   null    -> promote to a root category
   */
  parentId?: number | null;
}

export interface GetCategoriesParams {
  page?: number;
  size?: number;
  sort?: string;
  search?: string;
  status?: boolean;
  parentId?: number | null;
}

/**
 * Category API endpoints with RTK Query caching and invalidation.
 *
 * None of these endpoints take a `companyId`: the backend scopes every call to
 * the company in the `X-Company-Id` header, which the Axios request interceptor
 * attaches from the user's current company/fiscal-year selection. Because the
 * selection is not a parameter, RTK Query cache entries are naturally separated
 * per company only by the user switching context and re-fetching — callers
 * should include the company id in their component keys when rendering lists.
 */
export const categoryApi = api.injectEndpoints({
  endpoints: (build) => ({
    getCategories: build.query<PageResponse<CategoryListResponse>, GetCategoriesParams | void>({
      query: (params) => ({
        url: "/inventory/categories",
        method: "GET",
        params: params ?? {},
      }),
      providesTags: ["Category"],
    }),

    getCategory: build.query<Category, string>({
      query: (categoryId) => ({
        url: `/inventory/categories/${categoryId}`,
        method: "GET",
      }),
      providesTags: (_result, _error, categoryId) => [{ type: "Category", id: categoryId }],
    }),

    getCategoryBySlug: build.query<Category, string>({
      query: (slug) => ({
        url: `/inventory/categories/by-slug`,
        method: "GET",
        params: { slug },
      }),
      providesTags: (_result, _error, slug) => [{ type: "Category", id: `slug-${slug}` }],
    }),

    getRootCategories: build.query<CategoryListResponse[], void>({
      query: () => ({
        url: `/inventory/categories/roots`,
        method: "GET",
      }),
      providesTags: ["Category"],
    }),

    getChildCategories: build.query<CategoryListResponse[], number>({
      query: (parentId) => ({
        url: `/inventory/categories/children`,
        method: "GET",
        params: { parentId },
      }),
      providesTags: ["Category"],
    }),

    getActiveCategories: build.query<CategoryListResponse[], void>({
      query: () => ({
        url: `/inventory/categories/active`,
        method: "GET",
      }),
      providesTags: ["Category"],
    }),

    getCategoryHierarchyPath: build.query<Category[], number>({
      query: (categoryId) => ({
        url: `/inventory/categories/${categoryId}/path`,
        method: "GET",
      }),
      providesTags: ["Category"],
    }),

    createCategory: build.mutation<Category, CreateCategoryRequest>({
      query: (body) => ({
        url: "/inventory/categories",
        method: "POST",
        data: body,
      }),
      invalidatesTags: ["Category"],
    }),

    updateCategory: build.mutation<Category, { categoryId: number; data: UpdateCategoryRequest }>({
      query: ({ categoryId, data }) => ({
        url: `/inventory/categories/${categoryId}`,
        method: "PUT",
        data,
      }),
      invalidatesTags: (_result, _error, { categoryId }) => [
        { type: "Category", id: categoryId.toString() },
        "Category",
      ],
    }),

    patchCategory: build.mutation<Category, { categoryId: number; data: UpdateCategoryRequest }>({
      query: ({ categoryId, data }) => ({
        url: `/inventory/categories/${categoryId}`,
        method: "PATCH",
        data,
      }),
      invalidatesTags: (_result, _error, { categoryId }) => [
        { type: "Category", id: categoryId.toString() },
        "Category",
      ],
    }),

    moveCategory: build.mutation<Category, { categoryId: number; newParentId?: number }>({
      query: ({ categoryId, newParentId }) => ({
        url: `/inventory/categories/${categoryId}/move`,
        method: "POST",
        params: { newParentId },
      }),
      invalidatesTags: (_result, _error, { categoryId }) => [
        { type: "Category", id: categoryId.toString() },
        "Category",
      ],
    }),

    activateCategory: build.mutation<Category, number>({
      query: (categoryId) => ({
        url: `/inventory/categories/${categoryId}/activate`,
        method: "POST",
      }),
      invalidatesTags: (_result, _error, categoryId) => [
        { type: "Category", id: categoryId.toString() },
        "Category",
      ],
    }),

    deactivateCategory: build.mutation<Category, number>({
      query: (categoryId) => ({
        url: `/inventory/categories/${categoryId}/deactivate`,
        method: "POST",
      }),
      invalidatesTags: (_result, _error, categoryId) => [
        { type: "Category", id: categoryId.toString() },
        "Category",
      ],
    }),

    deleteCategory: build.mutation<void, number>({
      query: (categoryId) => ({
        url: `/inventory/categories/${categoryId}`,
        method: "DELETE",
      }),
      invalidatesTags: ["Category"],
    }),
  }),
});

export const {
  useGetCategoriesQuery,
  useGetCategoryQuery,
  useGetCategoryBySlugQuery,
  useGetRootCategoriesQuery,
  useGetChildCategoriesQuery,
  useGetActiveCategoriesQuery,
  useGetCategoryHierarchyPathQuery,
  useCreateCategoryMutation,
  useUpdateCategoryMutation,
  usePatchCategoryMutation,
  useMoveCategoryMutation,
  useActivateCategoryMutation,
  useDeactivateCategoryMutation,
  useDeleteCategoryMutation,
} = categoryApi;
