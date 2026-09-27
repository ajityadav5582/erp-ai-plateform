package com.erp.platform.identity.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Business Action Enumeration for the ERP SaaS platform.
 *
 * <p>Serves as the compile-time single source of truth for security expressions,
 * Spring Security SpEL authorization checks, and dynamic UI grid headers.
 *
 * <p>Bridges compile-time metadata with runtime {@link ActionEntity} database master data.
 *
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum Action {

    // ==================== CRUD Operations ====================

    /** Create a new resource instance. */
    CREATE("CREATE", "Create", "Create a new resource instance", ActionCategory.CRUD, 10),

    /** View resource data and detailed record views. */
    VIEW("VIEW", "View", "View resource data and detailed records", ActionCategory.CRUD, 20),

    /** Update existing resource data. */
    UPDATE("UPDATE", "Update", "Update existing resource data", ActionCategory.CRUD, 30),

    /** Delete or soft-delete a resource instance. */
    DELETE("DELETE", "Delete", "Delete or soft-delete a resource instance", ActionCategory.CRUD, 40),

    /** List or browse resource record collections. */
    LIST("LIST", "List", "List or browse resource record collections", ActionCategory.CRUD, 45),

    // ==================== Business Workflow & Lifecycle ====================

    /** Submit a document or record for managerial approval. */
    SUBMIT("SUBMIT", "Submit", "Submit a document for approval or processing", ActionCategory.WORKFLOW, 48),

    /** Approve a resource, order, or workflow step. */
    APPROVE("APPROVE", "Approve", "Approve a resource, order, or workflow step", ActionCategory.WORKFLOW, 50),

    /** Reject a resource, request, or workflow step. */
    REJECT("REJECT", "Reject", "Reject a resource or workflow step", ActionCategory.WORKFLOW, 60),

    /** Cancel an active or pending order, transaction, or document. */
    CANCEL("CANCEL", "Cancel", "Cancel an active order or transaction", ActionCategory.WORKFLOW, 65),

    /** Formally close a completed order, project, or period. */
    CLOSE("CLOSE", "Close", "Formally close a completed record or period", ActionCategory.WORKFLOW, 67),

    /** Reopen a closed document or period with audit logging. */
    REOPEN("REOPEN", "Reopen", "Reopen a closed document or period with audit trail", ActionCategory.WORKFLOW, 68),

    /** Void a financial transaction or invoice. */
    VOID("VOID", "Void", "Void a processed financial transaction or invoice", ActionCategory.WORKFLOW, 69),

    // ==================== Financial & Ledger Operations ====================

    /** Post financial voucher or journal entry to General Ledger. */
    POST("POST", "Post", "Post financial entries to General Ledger", ActionCategory.FINANCIAL, 70),

    /** Reconcile bank accounts or balance ledger accounts. */
    RECONCILE("RECONCILE", "Reconcile", "Reconcile bank accounts or balances", ActionCategory.FINANCIAL, 75),

    // ==================== Data I/O & Output ====================

    /** Export resource data to external files (Excel, CSV, PDF). */
    EXPORT("EXPORT", "Export", "Export resource data to external files", ActionCategory.DATA_IO, 80),

    /** Import resource data from external files. */
    IMPORT("IMPORT", "Import", "Import resource data from external files", ActionCategory.DATA_IO, 85),

    /** Print official documents, invoices, or receipts. */
    PRINT("PRINT", "Print", "Print official documents, invoices, or receipts", ActionCategory.DATA_IO, 88),

    /** View analytical reports related to the resource. */
    REPORT("REPORT", "Report", "View analytical reports for the resource", ActionCategory.DATA_IO, 90),

    // ==================== Administrative Control & Security ====================

    /** Administrative management of resource settings. */
    MANAGE("MANAGE", "Manage", "Administrative management of resource settings", ActionCategory.ADMIN, 95),

    /** Assign resource ownership, user roles, or permissions. */
    ASSIGN("ASSIGN", "Assign", "Assign resource ownership, roles, or permissions", ActionCategory.ADMIN, 100),

    /** Lock financial period or record against further modification. */
    LOCK("LOCK", "Lock", "Lock financial period or record against modification", ActionCategory.ADMIN, 105),

    /** Unlock a locked record or period for administrative adjustment. */
    UNLOCK("UNLOCK", "Unlock", "Unlock a locked record for administrative adjustment", ActionCategory.ADMIN, 110),

    /** View detailed audit logs and compliance history for the resource. */
    AUDIT("AUDIT", "Audit", "View compliance and change audit trails", ActionCategory.ADMIN, 115),

    // ==================== Automation & AI Execution ====================

    /** Execute automated workflows, AI agent pipelines, or background tasks. */
    EXECUTE("EXECUTE", "Execute", "Trigger AI agents, pipelines, or background tasks", ActionCategory.EXECUTION, 120);

    private final String code;
    private final String defaultName;
    private final String description;
    private final ActionCategory category;
    private final int displayOrder;

    private static final Map<String, Action> ACTION_CODE_MAP = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(Action::getCode, Function.identity()));

    /**
     * Lookup an Action by its action code (case-insensitive).
     *
     * @param code the action code (e.g., "CREATE", "VIEW")
     * @return an Optional containing the matching Action, or empty if not found
     */
    public static Optional<Action> fromCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(ACTION_CODE_MAP.get(code.trim().toUpperCase()));
    }

    /**
     * Generates a Spring Security SpEL authority expression for a given resource.
     *
     * <p>Example: {@code toSecurityExpression("PRODUCT")} returns {@code "hasAuthority('PRODUCT_CREATE')"}
     *
     * @param resourceCode the resource code
     * @return formatted SpEL security expression
     */
    public String toSecurityExpression(String resourceCode) {
        if (resourceCode == null || resourceCode.isBlank()) {
            throw new IllegalArgumentException("Resource code must not be null or blank");
        }
        return String.format("hasAuthority('%s_%s')", resourceCode.trim().toUpperCase(), this.code);
    }
}

