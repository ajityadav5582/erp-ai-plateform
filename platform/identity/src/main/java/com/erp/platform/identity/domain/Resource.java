package com.erp.platform.identity.domain;

/**
 * Business resource enumeration.
 *
 * <p>Represents the business modules/domains in the ERP platform.
 * Permissions are scoped to these resources.
 *
 * <p>Designed for extensibility to support future modules.
 *
 * @since 1.0.0
 */
public enum Resource {

    /**
     * Product management module.
     * Covers product catalog, pricing, inventory tracking.
     */
    PRODUCT,

    /**
     * Sales module.
     * Covers orders, customers, quotations, invoices.
     */
    SALES,

    /**
     * Purchase module.
     * Covers procurement, vendors, purchase orders.
     */
    PURCHASE,

    /**
     * Inventory module.
     * Covers stock management, warehouses, movements.
     */
    INVENTORY,

    /**
     * Human Resources module.
     * Covers employees, attendance, payroll, leave management.
     */
    HR,

    /**
     * Accounting module.
     * Covers chart of accounts, journal entries, financial reports.
     */
    ACCOUNTING,

    /**
     * Customer Relationship Management module.
     * Covers leads, opportunities, campaigns, support tickets.
     */
    CRM,

    /**
     * AI/ML module.
     * Covers AI agents, pipelines, models, orchestration.
     */
    AI,

    /**
     * Manufacturing module.
     * Covers production, bills of materials, work orders.
     */
    MANUFACTURING,

    /**
     * Platform administration.
     * Covers tenant management, system configuration.
     */
    PLATFORM,

    /**
     * Reporting and analytics.
     * Covers dashboards, reports, data exports.
     */
    REPORTING,

    /**
     * Integration module.
     * Covers external APIs, webhooks, data synchronization.
     */
    INTEGRATION
}
