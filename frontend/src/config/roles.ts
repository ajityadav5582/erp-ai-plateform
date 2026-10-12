export interface PredefinedRole {
  code: string;
  name: string;
  category: string;
  description: string;
  /**
   * Permission codes this preset grants. Codes follow the backend's
   * {@code RESOURCE_ACTION} format (e.g. {@code USER_CREATE}), and a
   * {@code RESOURCE_*} entry expands to every action on that resource.
   */
  permissionCodes: string[];
}

/**
 * Every read-only permission the platform seeds. Used by the auditor-style
 * presets so they grant visibility without any write access.
 */
const READ_ONLY_ALL: string[] = [
  "ACCOUNTING_VIEW",
  "BRANCH_VIEW",
  "COMPANY_VIEW",
  "DEPARTMENT_VIEW",
  "DISTRICT_VIEW",
  "GOODS_RECEIPT_VIEW",
  "HR_VIEW",
  "INVENTORY_VIEW",
  "LOCAL_LEVEL_VIEW",
  "MANUFACTURING_VIEW",
  "PERMISSION_VIEW",
  "PR_VIEW",
  "PRODUCT_CATEGORY_VIEW",
  "PRODUCT_VIEW",
  "PROVINCE_VIEW",
  "PURCHASE_INVOICE_VIEW",
  "PURCHASE_ORDER_VIEW",
  "REPORTING_VIEW",
  "ROLE_VIEW",
  "SALES_VIEW",
  "SUPPLIER_PAYMENT_VIEW",
  "USER_VIEW",
  "VENDOR_VIEW",
  "WAREHOUSE_VIEW",
];

export const PREDEFINED_ROLES: PredefinedRole[] = [
  // Platform Level Roles
  { code: "SUPER_ADMIN", name: "Super Administrator", category: "PLATFORM", description: "Platform super administrator with cross-tenant access.", permissionCodes: ["*"] },
  { code: "PLATFORM_SUPPORT", name: "Platform Support", category: "PLATFORM", description: "Platform diagnostic support specialist.", permissionCodes: ["REPORTING_VIEW", "INVENTORY_VIEW", "WAREHOUSE_VIEW"] },
  { code: "PLATFORM_AUDITOR", name: "Platform Auditor", category: "PLATFORM", description: "Platform compliance and audit specialist.", permissionCodes: READ_ONLY_ALL },

  // Tenant Executive Roles
  { code: "OWNER", name: "Tenant Owner", category: "EXECUTIVE", description: "Tenant owner with full executive, billing, and system control.", permissionCodes: ["*"] },
  { code: "ADMIN", name: "Tenant Administrator", category: "EXECUTIVE", description: "Tenant administrator for user, role, and organization setup.", permissionCodes: ["USER_*", "ROLE_*", "PERMISSION_VIEW", "DEPARTMENT_VIEW", "BRANCH_*", "COMPANY_*", "REPORTING_VIEW"] },
  { code: "EXECUTIVE", name: "Executive (C-Level)", category: "EXECUTIVE", description: "C-Level executive (CEO, CFO, COO) with cross-module reporting & approvals.", permissionCodes: ["REPORTING_VIEW", "PR_APPROVE", "PURCHASE_ORDER_APPROVE", "PURCHASE_INVOICE_VIEW", "SUPPLIER_PAYMENT_VIEW", "GOODS_RECEIPT_VIEW", "PURCHASE_ORDER_VIEW", "PR_VIEW", "SALES_VIEW", "ACCOUNTING_VIEW"] },

  // Department & Finance Managers
  { code: "MANAGER", name: "Department Manager", category: "DEPARTMENT", description: "Manages business operations within assigned scope or department.", permissionCodes: ["PR_*", "PURCHASE_ORDER_*", "GOODS_RECEIPT_*", "PURCHASE_INVOICE_*", "SUPPLIER_PAYMENT_*", "REPORTING_VIEW", "USER_VIEW", "DEPARTMENT_VIEW"] },
  { code: "FINANCE_MANAGER", name: "Finance Manager", category: "DEPARTMENT", description: "Manages financial planning, budgets, tax, and high-value approvals.", permissionCodes: ["PURCHASE_INVOICE_*", "SUPPLIER_PAYMENT_*", "REPORTING_VIEW", "ACCOUNTING_VIEW", "PURCHASE_ORDER_VIEW", "GOODS_RECEIPT_VIEW"] },
  { code: "ACCOUNTANT", name: "Senior Accountant", category: "DEPARTMENT", description: "Manages General Ledger, chart of accounts, vouchers, AP/AR, and tax filings.", permissionCodes: ["ACCOUNTING_VIEW", "PURCHASE_INVOICE_VIEW", "SUPPLIER_PAYMENT_VIEW", "REPORTING_VIEW"] },
  { code: "SALES_MANAGER", name: "Sales Manager", category: "DEPARTMENT", description: "Manages sales pipeline, price lists, customer contracts, and sales teams.", permissionCodes: ["SALES_*", "REPORTING_VIEW"] },
  { code: "PURCHASE_MANAGER", name: "Purchase Manager", category: "DEPARTMENT", description: "Manages procurement budgets, supplier contracts, and purchase orders.", permissionCodes: ["PR_*", "PURCHASE_ORDER_*", "VENDOR_VIEW", "REPORTING_VIEW", "GOODS_RECEIPT_VIEW", "PURCHASE_INVOICE_VIEW"] },
  { code: "INVENTORY_MANAGER", name: "Inventory Manager", category: "DEPARTMENT", description: "Manages inventory balances, product catalog, warehouses, and stock counts.", permissionCodes: ["INVENTORY_*", "WAREHOUSE_VIEW", "PRODUCT_*", "PRODUCT_CATEGORY_VIEW", "REPORTING_VIEW"] },
  { code: "HR_MANAGER", name: "HR Manager", category: "DEPARTMENT", description: "Manages employee records, organization hierarchy, payroll, and HR policies.", permissionCodes: ["HR_VIEW", "USER_*", "DEPARTMENT_VIEW", "BRANCH_VIEW", "REPORTING_VIEW"] },
  { code: "MANUFACTURING_MANAGER", name: "Manufacturing Manager", category: "DEPARTMENT", description: "Manages Bills of Materials (BOM), work orders, work centers, and MRP.", permissionCodes: ["MANUFACTURING_VIEW", "PRODUCT_*", "REPORTING_VIEW"] },

  // Operational & Staff Roles
  { code: "SALES_REPRESENTATIVE", name: "Sales Representative", category: "OPERATIONAL", description: "Handles customer quotations, sales orders, CRM leads, and communications.", permissionCodes: ["SALES_VIEW"] },
  { code: "PURCHASE_AGENT", name: "Purchasing Agent", category: "OPERATIONAL", description: "Creates purchase requisitions, issues purchase orders, and tracks deliveries.", permissionCodes: ["PR_CREATE", "PR_VIEW", "PURCHASE_ORDER_VIEW", "GOODS_RECEIPT_VIEW", "VENDOR_VIEW"] },
  { code: "WAREHOUSE_OPERATOR", name: "Warehouse Operator", category: "OPERATIONAL", description: "Executes stock transfers, goods receipts (GRN), picking, and packing.", permissionCodes: ["INVENTORY_*", "GOODS_RECEIPT_VIEW", "WAREHOUSE_VIEW", "PRODUCT_VIEW"] },
  { code: "HR_OFFICER", name: "HR Officer", category: "OPERATIONAL", description: "Handles employee attendance, time-off/leave approvals, and recruitment.", permissionCodes: ["HR_VIEW", "USER_VIEW"] },
  { code: "QUALITY_INSPECTOR", name: "Quality Inspector", category: "OPERATIONAL", description: "Performs quality checks on incoming goods and production outputs.", permissionCodes: ["GOODS_RECEIPT_VIEW", "PRODUCT_VIEW"] },
  { code: "CASHIER", name: "POS Cashier", category: "OPERATIONAL", description: "Handles point-of-sale transactions, customer receipts, and payments.", permissionCodes: ["SALES_VIEW"] },
  { code: "AUDITOR", name: "Compliance Auditor", category: "OPERATIONAL", description: "Read-only access across financial ledger, transactions, and audit logs.", permissionCodes: READ_ONLY_ALL },
  { code: "EMPLOYEE", name: "Standard Employee", category: "OPERATIONAL", description: "Employee self-service access for personal records and attendance.", permissionCodes: [] },
];
