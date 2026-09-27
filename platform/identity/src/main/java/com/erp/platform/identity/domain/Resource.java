package com.erp.platform.identity.domain;

/**
 * Business resource enumeration for the ERP SaaS platform.
 *
 * <p>Represents business modules, sub-domains, and domain entities across the ERP platform.
 * Permissions are scoped to these resources.
 *
 * @since 1.0.0
 */
public enum Resource {

    // ==================== Identity, Organization & Security ====================

    /** Platform administration and cross-tenant configuration. */
    PLATFORM,

    /** Tenant management, subscription plans, and organizational profiles. */
    TENANT,

    /** Identity and access management domain. */
    IDENTITY,

    /** User account management and profile configurations. */
    USER,

    /** Role definitions and access rights. */
    ROLE,

    /** Fine-grained system permissions. */
    PERMISSION,

    /** Global role templates used for tenant initial provisioning. */
    ROLE_TEMPLATE,

    /** Physical or logical organizational branches. */
    BRANCH,

    /** Organizational departments and hierarchy. */
    DEPARTMENT,

    /** User to role assignments. */
    USER_ROLE,

    /** User to branch assignments. */
    USER_BRANCH,

    /** User to department assignments. */
    USER_DEPARTMENT,

    /** Geographic province/state definitions. */
    PROVINCE,

    /** Geographic district definitions. */
    DISTRICT,

    /** Municipal or local level geographic units. */
    LOCAL_LEVEL,

    /** Resource master catalog definition. */
    RESOURCE,

    // ==================== Financials & Accounting ====================

    /** Core accounting module. */
    ACCOUNTING,

    /** General Ledger Chart of Accounts. */
    CHART_OF_ACCOUNTS,

    /** General Ledger journal vouchers and manual entries. */
    JOURNAL_ENTRY,

    /** Accounts Payable (AP) vendor invoices and bills. */
    ACCOUNTS_PAYABLE,

    /** Accounts Receivable (AR) customer invoices and billing. */
    ACCOUNTS_RECEIVABLE,

    /** Tax rates, rules, and tax compliance filings. */
    TAX_MANAGEMENT,

    /** Multi-currency definitions and exchange rates. */
    CURRENCY,

    /** Company bank accounts and bank reconciliations. */
    BANK_ACCOUNT,

    /** Financial budgets, forecasts, and variance tracking. */
    BUDGET,

    /** Invoicing and customer billing records. */
    INVOICE,

    /** Financial payments and receipts processing. */
    PAYMENT,

    // ==================== Sales & CRM ====================

    /** Commercial sales domain. */
    SALES,

    /** Customer Relationship Management (CRM) leads, opportunities, and pipelines. */
    CRM,

    /** Customer master data and accounts. */
    CUSTOMER,

    /** Sales quotations, proposals, and estimates. */
    QUOTATION,

    /** Sales orders and order fulfillment. */
    SALES_ORDER,

    /** Commercial sales invoices. */
    SALES_INVOICE,

    /** Point-of-Sale (POS) register sessions and sales transactions. */
    POS_SESSION,

    // ==================== Procurement & Supply Chain ====================

    /** Procurement and purchasing domain. */
    PURCHASE,

    /** Vendor and supplier master data. */
    VENDOR,

    /** Internal purchase requisitions (PR). */
    PURCHASE_REQUISITION,

    /** Purchase orders (PO) issued to suppliers. */
    PURCHASE_ORDER,

    /** Goods Receipt Notes (GRN) for received stock. */
    GOODS_RECEIPT,

    // ==================== Inventory & Logistics ====================

    /** Stock management and inventory control. */
    INVENTORY,

    /** Product item catalog, SKUs, and items. */
    PRODUCT,

    /** Product categories and classifications. */
    PRODUCT_CATEGORY,

    /** Warehouse facilities and storage bin locations. */
    WAREHOUSE,

    /** Stock movement, internal transfers, and adjustments. */
    STOCK_MOVEMENT,

    /** Batch, lot numbers, and serial tracking. */
    SERIAL_LOT,

    // ==================== Human Resources & Payroll ====================

    /** Human resources domain. */
    HR,

    /** Employee profiles and service records. */
    EMPLOYEE,

    /** Employee time, attendance, and clocking logs. */
    ATTENDANCE,

    /** Leave requests, approvals, and entitlement balances. */
    LEAVE_REQUEST,

    /** Payroll processing, salary structures, and payslips. */
    PAYROLL,

    /** Recruitment, job openings, and candidate pipelines. */
    RECRUITMENT,

    // ==================== Manufacturing & Production ====================

    /** Manufacturing and production domain. */
    MANUFACTURING,

    /** Bill of Materials (BOM), components, and recipes. */
    BILL_OF_MATERIALS,

    /** Production work orders and shop floor routing. */
    WORK_ORDER,

    /** Shop floor work centers and machinery. */
    WORK_CENTER,

    /** Quality assurance and inspection control. */
    QUALITY_CONTROL,

    // ==================== AI & Intelligence ====================

    /** AI/ML module and orchestration engine. */
    AI,

    /** Autonomous AI agents and execution workflows. */
    AI_AGENT,

    /** Predictive analytics, machine learning insights, and forecasts. */
    AI_ANALYTICS,

    // ==================== System Analytics & Platform ====================

    /** Business intelligence dashboards and reports. */
    REPORTING,

    /** Security, compliance, and operational audit logs. */
    AUDIT_LOG,

    /** External integrations, webhooks, and API keys. */
    INTEGRATION,

    /** System notifications, email templates, and alerts. */
    NOTIFICATION
}

