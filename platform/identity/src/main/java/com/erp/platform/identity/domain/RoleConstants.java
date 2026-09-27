package com.erp.platform.identity.domain;

/**
 * Centralized constants for system-defined role codes in the ERP SaaS platform.
 *
 * <p>System roles are shared across all tenants and cannot be modified or deleted.
 * Predefined tenant default roles serve as standard role templates during tenant onboarding.
 *
 * <p><strong>Design note:</strong> These constants are used for seeding and referencing
 * predefined role codes. Authorization decisions (except SUPER_ADMIN bypass) must always
 * evaluate fine-grained permission codes (see {@link Permission}).
 *
 * <p><strong>Platform-level roles:</strong>
 * <ul>
 *   <li>{@link #SUPER_ADMIN} &mdash; platform-level administrator with full access across all tenants.</li>
 *   <li>{@link #PLATFORM_SUPPORT} &mdash; platform diagnostic support agent.</li>
 *   <li>{@link #PLATFORM_AUDITOR} &mdash; platform compliance & audit specialist.</li>
 * </ul>
 *
 * <p><strong>Tenant-level default roles:</strong>
 * <ul>
 *   <li>{@link #OWNER} &mdash; full executive access within tenant including billing and settings.</li>
 *   <li>{@link #ADMIN} &mdash; operational administrator for user & system settings within tenant.</li>
 *   <li>{@link #EXECUTIVE} &mdash; C-Level executive (CEO, CFO, COO) with cross-module reporting & high-level approvals.</li>
 *   <li>{@link #MANAGER} &mdash; general operational & departmental manager.</li>
 *   <li>{@link #FINANCE_MANAGER} &mdash; finance manager with budgeting, approvals, and financial statements access.</li>
 *   <li>{@link #ACCOUNTANT} &mdash; accountant for general ledger, vouchers, AP/AR, and tax filings.</li>
 *   <li>{@link #SALES_MANAGER} &mdash; commercial director / head of sales.</li>
 *   <li>{@link #SALES_REPRESENTATIVE} &mdash; sales agent for orders, quotes, and CRM interactions.</li>
 *   <li>{@link #PURCHASE_MANAGER} &mdash; procurement director / purchasing manager.</li>
 *   <li>{@link #PURCHASE_AGENT} &mdash; procurement officer for POs, requisitions, and vendor comms.</li>
 *   <li>{@link #INVENTORY_MANAGER} &mdash; inventory, stock control, and warehouse manager.</li>
 *   <li>{@link #WAREHOUSE_OPERATOR} &mdash; warehouse staff for picking, receiving, and stock transfers.</li>
 *   <li>{@link #HR_MANAGER} &mdash; human resources director / HR lead.</li>
 *   <li>{@link #HR_OFFICER} &mdash; HR specialist for attendance, leave, and recruitment.</li>
 *   <li>{@link #MANUFACTURING_MANAGER} &mdash; production plant manager for BOM and work orders.</li>
 *   <li>{@link #QUALITY_INSPECTOR} &mdash; quality control and compliance auditor.</li>
 *   <li>{@link #CASHIER} &mdash; POS terminal operator.</li>
 *   <li>{@link #AUDITOR} &mdash; internal / external audit consultant with read-only audit access.</li>
 *   <li>{@link #EMPLOYEE} &mdash; standard employee with self-service view permissions.</li>
 * </ul>
 *
 * @since 1.0.0
 */
public final class RoleConstants {

    // ==================== Platform-Level Roles ====================

    /**
     * Platform-level super administrator role.
     * <p>Has full access across all tenants and can manage all platform resources.
     */
    public static final String SUPER_ADMIN = "SUPER_ADMIN";

    /**
     * Platform support specialist.
     * <p>Has read-only diagnostic access for customer support and system troubleshooting.
     */
    public static final String PLATFORM_SUPPORT = "PLATFORM_SUPPORT";

    /**
     * Platform compliance auditor.
     * <p>Has read-only access to audit logs, security records, and system logs across tenants.
     */
    public static final String PLATFORM_AUDITOR = "PLATFORM_AUDITOR";

    // ==================== Tenant-Level Default Roles ====================

    /**
     * Tenant owner role.
     * <p>Has full executive control within their tenant including organization management, user assignment, billing, and system configuration.
     */
    public static final String OWNER = "OWNER";

    /**
     * Tenant administrator role.
     * <p>Assigned to the user created during initial registration. Can create and
     * manage multiple Companies within the tenant, switch between companies,
     * and manage users/roles according to the selected company.
     */
    public static final String TENANT_ADMIN = "TENANT_ADMIN";

    /**
     * Tenant administrator role.
     * <p>Can manage users, roles, organizational units, and tenant configurations.
     */
    public static final String ADMIN = "ADMIN";

    /**
     * Executive C-Level role (CEO, CFO, COO).
     * <p>Provides full visibility into executive dashboards, financial reports, and high-value approvals across all departments.
     */
    public static final String EXECUTIVE = "EXECUTIVE";

    /**
     * Departmental or Branch Manager.
     * <p>Manages team members, department operational records, and branch activities.
     */
    public static final String MANAGER = "MANAGER";

    /**
     * Finance Manager role.
     * <p>Manages financial planning, budgets, high-value invoice approvals, tax management, and financial statements.
     */
    public static final String FINANCE_MANAGER = "FINANCE_MANAGER";

    /**
     * Accountant role.
     * <p>Manages chart of accounts, journal entries, AP/AR vouchers, bank reconciliations, and financial reporting.
     */
    public static final String ACCOUNTANT = "ACCOUNTANT";

    /**
     * Sales Manager role.
     * <p>Manages commercial pipeline, sales targets, price lists, customer contracts, and sales reps.
     */
    public static final String SALES_MANAGER = "SALES_MANAGER";

    /**
     * Sales Representative role.
     * <p>Handles customer quotations, sales orders, CRM leads, and direct client communications.
     */
    public static final String SALES_REPRESENTATIVE = "SALES_REPRESENTATIVE";

    /**
     * Purchasing Manager role.
     * <p>Manages vendor contracts, procurement budgets, purchase requisitions, and purchase orders.
     */
    public static final String PURCHASE_MANAGER = "PURCHASE_MANAGER";

    /**
     * Purchasing Agent role.
     * <p>Creates purchase requisitions, issues purchase orders, and tracks vendor deliveries.
     */
    public static final String PURCHASE_AGENT = "PURCHASE_AGENT";

    /**
     * Inventory Manager role.
     * <p>Manages stock balances, product catalog, warehouses, stock counts, and inventory valuation.
     */
    public static final String INVENTORY_MANAGER = "INVENTORY_MANAGER";

    /**
     * Warehouse Operator role.
     * <p>Executes physical stock transfers, goods receipts (GRN), picking, packing, and dispatch.
     */
    public static final String WAREHOUSE_OPERATOR = "WAREHOUSE_OPERATOR";

    /**
     * Human Resources Manager role.
     * <p>Manages employee records, organizational structure, payroll execution, and HR policies.
     */
    public static final String HR_MANAGER = "HR_MANAGER";

    /**
     * HR Officer role.
     * <p>Handles daily employee attendance, time-off/leave approvals, and recruitment records.
     */
    public static final String HR_OFFICER = "HR_OFFICER";

    /**
     * Manufacturing / Production Manager role.
     * <p>Manages Bills of Materials (BOM), shop floor work orders, work centers, and MRP scheduling.
     */
    public static final String MANUFACTURING_MANAGER = "MANUFACTURING_MANAGER";

    /**
     * Quality Control Inspector role.
     * <p>Performs quality checks on incoming goods and production outputs.
     */
    public static final String QUALITY_INSPECTOR = "QUALITY_INSPECTOR";

    /**
     * Cashier role.
     * <p>Handles point-of-sale transactions, retail registers, payments, and receipt generation.
     */
    public static final String CASHIER = "CASHIER";

    /**
     * Auditor role.
     * <p>Read-only access across financial ledger, transactions, and audit logs for internal and external auditing.
     */
    public static final String AUDITOR = "AUDITOR";

    /**
     * Standard employee role.
     * <p>Self-service role assigned to internal employees for personal profile, attendance logging, and payslips.
     */
    public static final String EMPLOYEE = "EMPLOYEE";

    private RoleConstants() {
        // Utility class
    }
}

