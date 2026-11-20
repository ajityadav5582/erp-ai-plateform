-- Migration: V39
-- Description: Updates user roles, business resources, actions, and default role templates
-- for the enterprise multi-tenant ERP SaaS platform.

-- ============================================================
-- 1. Seed New Business Resources
-- ============================================================
INSERT INTO resources (resource_code, resource_name, module, description, status, is_system, created_at, version) VALUES
    -- Identity, Organization & Security
    ('TENANT', 'Tenant', 'IDENTITY', 'Tenant organization and subscription management', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('ROLE_TEMPLATE', 'Role Template', 'IDENTITY', 'Global initial role templates', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('USER_ROLE', 'User Role', 'IDENTITY', 'User role assignments', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('USER_BRANCH', 'User Branch', 'IDENTITY', 'User branch assignments', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('USER_DEPARTMENT', 'User Department', 'IDENTITY', 'User department assignments', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('PROVINCE', 'Province', 'IDENTITY', 'Geographic province/state master data', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('DISTRICT', 'District', 'IDENTITY', 'Geographic district master data', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('LOCAL_LEVEL', 'Local Level', 'IDENTITY', 'Municipal and local level units', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),

    -- Financials & Accounting
    ('ACCOUNTING', 'Accounting', 'ACCOUNTING', 'General Ledger accounting', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('CHART_OF_ACCOUNTS', 'Chart of Accounts', 'ACCOUNTING', 'General Ledger chart of accounts', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('JOURNAL_ENTRY', 'Journal Entry', 'ACCOUNTING', 'General Ledger vouchers and journal entries', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('ACCOUNTS_PAYABLE', 'Accounts Payable', 'ACCOUNTING', 'AP vendor bills and payments', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('ACCOUNTS_RECEIVABLE', 'Accounts Receivable', 'ACCOUNTING', 'AR customer invoices and payments', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('TAX_MANAGEMENT', 'Tax Management', 'ACCOUNTING', 'Tax rules, rates, and compliance filings', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('CURRENCY', 'Currency', 'ACCOUNTING', 'Multi-currency and exchange rates', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('BANK_ACCOUNT', 'Bank Account', 'ACCOUNTING', 'Bank accounts and bank reconciliations', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('BUDGET', 'Budget', 'ACCOUNTING', 'Financial budgeting and forecasts', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('INVOICE', 'Invoice', 'ACCOUNTING', 'Customer invoicing and billing', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('PAYMENT', 'Payment', 'ACCOUNTING', 'Financial payments and collections', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),

    -- Sales & CRM
    ('CRM', 'CRM', 'SALES', 'CRM leads, opportunities, and pipelines', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('QUOTATION', 'Quotation', 'SALES', 'Sales quotes and proposals', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('SALES_ORDER', 'Sales Order', 'SALES', 'Sales order management', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('SALES_INVOICE', 'Sales Invoice', 'SALES', 'Commercial sales invoicing', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('POS_SESSION', 'POS Session', 'SALES', 'Point-of-Sale terminal register sessions', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),

    -- Procurement & Supply Chain
    ('VENDOR', 'Vendor', 'PURCHASE', 'Vendor and supplier master records', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('PURCHASE_REQUISITION', 'Purchase Requisition', 'PURCHASE', 'Internal purchase requisitions (PR)', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('PURCHASE_ORDER', 'Purchase Order', 'PURCHASE', 'Purchase orders (PO) issued to suppliers', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('GOODS_RECEIPT', 'Goods Receipt', 'PURCHASE', 'Goods Receipt Notes (GRN)', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),

    -- Inventory & Warehouse
    ('PRODUCT_CATEGORY', 'Product Category', 'INVENTORY', 'Product categories and classifications', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('WAREHOUSE', 'Warehouse', 'INVENTORY', 'Warehouse facilities and storage locations', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('STOCK_MOVEMENT', 'Stock Movement', 'INVENTORY', 'Stock transfers and adjustments', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('SERIAL_LOT', 'Serial & Lot', 'INVENTORY', 'Batch, lot numbers, and serial tracking', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),

    -- HR & Payroll
    ('HR', 'Human Resources', 'HR', 'HR master domain', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('EMPLOYEE', 'Employee', 'HR', 'Employee records and profiles', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('ATTENDANCE', 'Attendance', 'HR', 'Time and attendance clocking logs', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('LEAVE_REQUEST', 'Leave Request', 'HR', 'Time-off and leave management', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('PAYROLL', 'Payroll', 'HR', 'Payroll execution and salary slips', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('RECRUITMENT', 'Recruitment', 'HR', 'Job postings and applicant tracking', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),

    -- Manufacturing & Operations
    ('MANUFACTURING', 'Manufacturing', 'MANUFACTURING', 'Production and MRP domain', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('BILL_OF_MATERIALS', 'Bill of Materials', 'MANUFACTURING', 'Bills of Materials (BOM) and components', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('WORK_ORDER', 'Work Order', 'MANUFACTURING', 'Production work orders and shop floor tasks', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('WORK_CENTER', 'Work Center', 'MANUFACTURING', 'Shop floor machinery and work centers', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('QUALITY_CONTROL', 'Quality Control', 'MANUFACTURING', 'Quality assurance and inspection logs', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),

    -- AI & Intelligence Platform
    ('AI', 'AI Engine', 'AI', 'AI orchestration and ML engine', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('AI_AGENT', 'AI Agent', 'AI', 'Autonomous ERP AI assistants', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('AI_ANALYTICS', 'AI Analytics', 'AI', 'Predictive analytics and ML insights', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),

    -- System & Analytics
    ('NOTIFICATION', 'Notification', 'PLATFORM', 'System notifications and email templates', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('INTEGRATION', 'Integration', 'PLATFORM', 'External APIs, webhooks, and integrations', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0)
ON CONFLICT (resource_code) DO NOTHING;

-- ============================================================
-- 2. Seed New Business Actions
-- ============================================================
INSERT INTO actions (action_code, action_name, description, status, is_system, created_at, version) VALUES
    ('LIST', 'List', 'Browse or list resource record collections', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('SUBMIT', 'Submit', 'Submit a document for approval or processing', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('CLOSE', 'Close', 'Formally close a completed record or period', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('REOPEN', 'Reopen', 'Reopen a closed document or period with audit trail', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('VOID', 'Void', 'Void a processed financial transaction or invoice', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('POST', 'Post', 'Post financial entries to General Ledger', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('RECONCILE', 'Reconcile', 'Reconcile bank accounts or balances', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('LOCK', 'Lock', 'Lock financial period or record against modification', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('UNLOCK', 'Unlock', 'Unlock a locked record for administrative adjustment', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('AUDIT', 'Audit', 'View compliance and change audit trails', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('EXECUTE', 'Execute', 'Trigger AI agents, pipelines, or background tasks', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0)
ON CONFLICT (action_code) DO NOTHING;

-- ============================================================
-- 3. Seed Global Role Templates
-- ============================================================
INSERT INTO role_templates (template_code, template_name, description, status, is_system, created_at, version) VALUES
    ('PLATFORM_SUPPORT', 'Platform Support', 'Platform diagnostic customer support specialist', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('PLATFORM_AUDITOR', 'Platform Auditor', 'Platform compliance and audit specialist', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('EXECUTIVE', 'Executive', 'C-Level executive (CEO, CFO, COO) with cross-module reporting and high-level approvals', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('FINANCE_MANAGER', 'Finance Manager', 'Finance manager managing budgets, tax, and high-value approvals', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('SALES_MANAGER', 'Sales Manager', 'Sales manager managing pipelines, pricing, and sales reps', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('SALES_REPRESENTATIVE', 'Sales Representative', 'Sales representative handling quotes, orders, and CRM leads', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('PURCHASE_MANAGER', 'Purchase Manager', 'Purchasing manager managing procurement, vendor contracts, and POs', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('PURCHASE_AGENT', 'Purchasing Agent', 'Purchasing agent issuing purchase requisitions and purchase orders', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('WAREHOUSE_OPERATOR', 'Warehouse Operator', 'Warehouse operator executing stock transfers, GRNs, and dispatch', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('HR_MANAGER', 'HR Manager', 'Human resources director managing employee records, hierarchy, and payroll', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('HR_OFFICER', 'HR Officer', 'HR officer managing attendance, leave approvals, and recruitment', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('MANUFACTURING_MANAGER', 'Manufacturing Manager', 'Production plant manager managing BOM, work orders, and MRP', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('QUALITY_INSPECTOR', 'Quality Inspector', 'Quality assurance inspector performing inspections and compliance checks', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('AUDITOR', 'Auditor', 'Read-only financial and system compliance auditor', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0),
    ('EMPLOYEE', 'Employee', 'Standard employee with self-service view and leave request capabilities', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, 0)
ON CONFLICT (template_code) DO NOTHING;
