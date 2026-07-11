package com.erp.platform.identity.domain;

/**
 * Business action enumeration.
 *
 * <p>Represents the operations that can be performed on resources.
 * Combined with {@link Resource} to form permission codes.
 *
 * <p>Designed for extensibility to support future action types.
 *
 * @since 1.0.0
 */
public enum Action {

    /**
     * Create a new resource instance.
     */
    CREATE,

    /**
     * Read/view resource data.
     */
    READ,

    /**
     * Update existing resource data.
     */
    UPDATE,

    /**
     * Delete a resource instance.
     */
    DELETE,

    /**
     * Approve a resource or workflow step.
     */
    APPROVE,

    /**
     * Export resource data.
     */
    EXPORT,

    /**
     * Import resource data.
     */
    IMPORT,

    /**
     * Manage resource configurations.
     */
    MANAGE,

    /**
     * Assign resource to other users/roles.
     */
    ASSIGN,

    /**
     * View reports related to the resource.
     */
    REPORT
}
