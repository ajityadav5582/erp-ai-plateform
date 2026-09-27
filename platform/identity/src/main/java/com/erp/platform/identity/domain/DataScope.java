package com.erp.platform.identity.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enterprise Attribute-Based Access Control (ABAC) Data Scope enumeration.
 *
 * <p>Defines the row-level data access boundary for a permission granted to a role within a tenant.
 * Enables fine-grained branch, department, and user-level data isolation.
 *
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum DataScope {

    /**
     * Unrestricted tenant-wide data access.
     */
    ALL("All Tenant Data", "Grants access to all data records across the entire tenant"),

    /**
     * Access restricted to records belonging to the user's assigned branch.
     */
    OWN_BRANCH("Own Branch Data", "Grants access only to records associated with the user's primary or assigned branch"),

    /**
     * Access restricted to records belonging to the user's assigned department.
     */
    OWN_DEPARTMENT("Own Department Data", "Grants access only to records associated with the user's department"),

    /**
     * Access restricted exclusively to records created by or owned by the user.
     */
    SELF_ONLY("Self Only", "Grants access only to records owned or created by the authenticated user");

    private final String displayName;
    private final String description;
}
