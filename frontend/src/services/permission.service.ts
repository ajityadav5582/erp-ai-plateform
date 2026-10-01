import { api } from "./api";

export type PermissionStatus = "ACTIVE" | "INACTIVE";

export type Resource =
  // Identity & Security
  | "PLATFORM"
  | "TENANT"
  | "IDENTITY"
  | "USER"
  | "ROLE"
  | "PERMISSION"
  | "ROLE_TEMPLATE"
  | "BRANCH"
  | "DEPARTMENT"
  | "USER_ROLE"
  | "USER_BRANCH"
  | "USER_DEPARTMENT"
  | "PROVINCE"
  | "DISTRICT"
  | "LOCAL_LEVEL"
  | "RESOURCE"
  // Financials & Accounting
  | "ACCOUNTING"
  | "CHART_OF_ACCOUNTS"
  | "JOURNAL_ENTRY"
  | "ACCOUNTS_PAYABLE"
  | "ACCOUNTS_RECEIVABLE"
  | "TAX_MANAGEMENT"
  | "CURRENCY"
  | "BANK_ACCOUNT"
  | "BUDGET"
  | "INVOICE"
  | "PAYMENT"
  // Sales & CRM
  | "SALES"
  | "CRM"
  | "CUSTOMER"
  | "QUOTATION"
  | "SALES_ORDER"
  | "SALES_INVOICE"
  | "POS_SESSION"
  // Procurement & SCM
  | "PURCHASE"
  | "VENDOR"
  | "PURCHASE_REQUISITION"
  | "PURCHASE_ORDER"
  | "GOODS_RECEIPT"
  // Inventory & Logistics
  | "INVENTORY"
  | "PRODUCT"
  | "PRODUCT_CATEGORY"
  | "WAREHOUSE"
  | "STOCK_MOVEMENT"
  | "SERIAL_LOT"
  // HR & Payroll
  | "HR"
  | "EMPLOYEE"
  | "ATTENDANCE"
  | "LEAVE_REQUEST"
  | "PAYROLL"
  | "RECRUITMENT"
  // Manufacturing & Quality
  | "MANUFACTURING"
  | "BILL_OF_MATERIALS"
  | "WORK_ORDER"
  | "WORK_CENTER"
  | "QUALITY_CONTROL"
  // AI Platform
  | "AI"
  | "AI_AGENT"
  | "AI_ANALYTICS"
  // System & Analytics
  | "REPORTING"
  | "AUDIT_LOG"
  | "INTEGRATION"
  | "NOTIFICATION";

export type Action =
  // CRUD
  | "CREATE"
  | "VIEW"
  | "UPDATE"
  | "DELETE"
  | "LIST"
  // Workflow
  | "SUBMIT"
  | "APPROVE"
  | "REJECT"
  | "CANCEL"
  | "CLOSE"
  | "REOPEN"
  | "VOID"
  // Financial
  | "POST"
  | "RECONCILE"
  // Data I/O
  | "EXPORT"
  | "IMPORT"
  | "PRINT"
  | "REPORT"
  // Admin
  | "MANAGE"
  | "ASSIGN"
  | "LOCK"
  | "UNLOCK"
  | "AUDIT"
  // Execution
  | "EXECUTE";

export type ActionCategory =
  | "CRUD"
  | "WORKFLOW"
  | "FINANCIAL"
  | "DATA_IO"
  | "ADMIN"
  | "EXECUTION";


export interface PermissionResponse {
  id: number;
  permissionCode: string;
  permissionName: string;
  resource: Resource;
  action: Action;
  description: string;
  status: PermissionStatus;
  createdAt: string;
  updatedAt: string;
}

export interface PermissionListResponse {
  permissions: PermissionResponse[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
}

export interface CreatePermissionRequest {
  resource: Resource;
  action: Action;
  description?: string;
}

export interface UpdatePermissionRequest {
  description?: string;
}

export interface GetPermissionsParams {
  page?: number;
  size?: number;
  sort?: string;
  resource?: Resource;
  action?: Action;
  status?: PermissionStatus;
  search?: string;
}

/**
 * Permission API endpoints with RTK Query caching and invalidation.
 */
export const permissionApi = api.injectEndpoints({
  endpoints: (build) => ({
    getPermissions: build.query<PermissionListResponse, GetPermissionsParams | void>({
      query: (params) => ({
        url: "/identity/permissions",
        method: "GET",
        params: params ?? {},
      }),
      providesTags: ["Permission"],
    }),

    getPermission: build.query<PermissionResponse, number>({
      query: (permissionId) => ({
        url: `/identity/permissions/${permissionId}`,
        method: "GET",
      }),
      providesTags: (_result, _error, permissionId) => [{ type: "Permission", id: permissionId }],
    }),

    createPermission: build.mutation<PermissionResponse, CreatePermissionRequest>({
      query: (body) => ({
        url: "/identity/permissions",
        method: "POST",
        data: body,
      }),
      invalidatesTags: ["Permission"],
    }),

    updatePermission: build.mutation<PermissionResponse, { permissionId: number; data: UpdatePermissionRequest }>({
      query: ({ permissionId, data }) => ({
        url: `/identity/permissions/${permissionId}`,
        method: "PUT",
        data,
      }),
      invalidatesTags: (_result, _error, { permissionId }) => [
        { type: "Permission", id: permissionId },
        "Permission",
      ],
    }),

    deletePermission: build.mutation<void, number>({
      query: (permissionId) => ({
        url: `/identity/permissions/${permissionId}`,
        method: "DELETE",
      }),
      invalidatesTags: ["Permission"],
    }),
  }),
});

export const {
  useGetPermissionsQuery,
  useGetPermissionQuery,
  useCreatePermissionMutation,
  useUpdatePermissionMutation,
  useDeletePermissionMutation,
} = permissionApi;
