export interface PredefinedRoleDefinition {
  code: string;
  name: string;
  category: "PLATFORM" | "EXECUTIVE" | "DEPARTMENT" | "OPERATIONAL";
  description: string;
}

export const PREDEFINED_ROLES: PredefinedRoleDefinition[] = [
  // Platform Level Roles
  { code: "SUPER_ADMIN", name: "Super Administrator", category: "PLATFORM", description: "Platform super administrator with cross-tenant access." },
  { code: "PLATFORM_SUPPORT", name: "Platform Support", category: "PLATFORM", description: "Platform diagnostic support specialist." },
  { code: "PLATFORM_AUDITOR", name: "Platform Auditor", category: "PLATFORM", description: "Platform compliance and audit specialist." },

  // Tenant Executive Roles
  { code: "OWNER", name: "Tenant Owner", category: "EXECUTIVE", description: "Tenant owner with full executive, billing, and system control." },
  { code: "ADMIN", name: "Tenant Administrator", category: "EXECUTIVE", description: "Tenant administrator for user, role, and organization setup." },
  { code: "EXECUTIVE", name: "Executive (C-Level)", category: "EXECUTIVE", description: "C-Level executive (CEO, CFO, COO) with cross-module reporting & approvals." },

  // Department & Finance Managers
  { code: "MANAGER", name: "Department Manager", category: "DEPARTMENT", description: "Manages business operations within assigned scope or department." },
  { code: "FINANCE_MANAGER", name: "Finance Manager", category: "DEPARTMENT", description: "Manages financial planning, budgets, tax, and high-value approvals." },
  { code: "ACCOUNTANT", name: "Senior Accountant", category: "DEPARTMENT", description: "Manages General Ledger, chart of accounts, vouchers, AP/AR, and tax filings." },
  { code: "SALES_MANAGER", name: "Sales Manager", category: "DEPARTMENT", description: "Manages sales pipeline, price lists, customer contracts, and sales teams." },
  { code: "PURCHASE_MANAGER", name: "Purchase Manager", category: "DEPARTMENT", description: "Manages procurement budgets, supplier contracts, and purchase orders." },
  { code: "INVENTORY_MANAGER", name: "Inventory Manager", category: "DEPARTMENT", description: "Manages inventory balances, product catalog, warehouses, and stock counts." },
  { code: "HR_MANAGER", name: "HR Manager", category: "DEPARTMENT", description: "Manages employee records, organization hierarchy, payroll, and HR policies." },
  { code: "MANUFACTURING_MANAGER", name: "Manufacturing Manager", category: "DEPARTMENT", description: "Manages Bills of Materials (BOM), work orders, work centers, and MRP." },

  // Operational & Staff Roles
  { code: "SALES_REPRESENTATIVE", name: "Sales Representative", category: "OPERATIONAL", description: "Handles customer quotations, sales orders, CRM leads, and communications." },
  { code: "PURCHASE_AGENT", name: "Purchasing Agent", category: "OPERATIONAL", description: "Creates purchase requisitions, issues purchase orders, and tracks deliveries." },
  { code: "WAREHOUSE_OPERATOR", name: "Warehouse Operator", category: "OPERATIONAL", description: "Executes stock transfers, goods receipts (GRN), picking, and packing." },
  { code: "HR_OFFICER", name: "HR Officer", category: "OPERATIONAL", description: "Handles employee attendance, time-off/leave approvals, and recruitment." },
  { code: "QUALITY_INSPECTOR", name: "Quality Inspector", category: "OPERATIONAL", description: "Performs quality checks on incoming goods and production outputs." },
  { code: "CASHIER", name: "POS Cashier", category: "OPERATIONAL", description: "Handles point-of-sale transactions and customer receipts." },
  { code: "AUDITOR", name: "Compliance Auditor", category: "OPERATIONAL", description: "Read-only access across financial ledger, transactions, and audit logs." },
  { code: "EMPLOYEE", name: "Standard Employee", category: "OPERATIONAL", description: "Employee self-service access for personal records and attendance." },
];
