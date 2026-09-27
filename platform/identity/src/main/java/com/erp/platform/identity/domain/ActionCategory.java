package com.erp.platform.identity.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Action category enumeration.
 *
 * <p>Categorizes security operations to support dynamic UI grid header grouping
 * and compile-time security expression classification in multi-tenant RBAC/ABAC matrices.
 *
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum ActionCategory {

    /**
     * Standard CRUD operations (Create, View, Update, Delete, List).
     */
    CRUD("CRUD Operations", "Standard data manipulation and view operations"),

    /**
     * Business workflow actions (Submit, Approve, Reject, Cancel, Close, Reopen, Void).
     */
    WORKFLOW("Workflow & Approvals", "Approval and lifecycle workflow state transitions"),

    /**
     * Financial operations (Post to GL, Reconcile).
     */
    FINANCIAL("Financial & Ledger", "General ledger posting, voucher approval, and bank reconciliations"),

    /**
     * Bulk Data Import/Export & Output (Import, Export, Print, Report).
     */
    DATA_IO("Data I/O & Reporting", "Bulk operations, printing, and analytical reporting"),

    /**
     * Administrative management operations (Manage, Assign, Lock, Unlock, Audit).
     */
    ADMIN("Administrative Control", "High-privilege system, locking, and security management operations"),

    /**
     * Autonomous Execution & Automation operations (Execute AI agents, background pipelines).
     */
    EXECUTION("Execution & Automation", "Triggering automated workflows, AI execution, and background processing");

    private final String displayName;
    private final String description;
}

