package com.erp.platform.identity.domain;

/**
 * Re-export of the centralized {@link com.erp.platform.data.entity.TenantScopedEntity} from
 * {@code libraries:common-data}, kept under the identity package so existing
 * imports across the identity module do not need to change.
 *
 * <p>Identity entities (Branch, Company, CompanyFiscalYear, Department,
 * UserBranch, UserDepartment, UserRole, etc.) extend this class, which means
 * they inherit the {@code tenant_id} / {@code company_id} columns, the
 * {@code assignScope()} guard, and the {@code @PrePersist} scope check from
 * the shared base class.
 *
 * @since 1.0.0
 */
public abstract class TenantScopedEntity<T extends java.io.Serializable>
        extends com.erp.platform.data.entity.TenantScopedEntity<T> {

    private static final long serialVersionUID = 1L;
}
