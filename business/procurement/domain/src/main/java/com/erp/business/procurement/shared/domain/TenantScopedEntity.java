package com.erp.business.procurement.shared.domain;

import com.erp.platform.data.lock.OptimisticLock;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Base class for every procurement entity that belongs to exactly one tenant
 * and one company.
 *
 * <p>This mirrors
 * {@code com.erp.business.inventory.shared.domain.TenantScopedEntity} rather
 * than depending on it. Procurement is a separate bounded context and a
 * separate deployable; reusing the inventory class would create a compile-time
 * dependency between two services that are meant to be independently
 * deployable and independently versioned. The duplication is small, deliberate
 * and documented here so that nobody "fixes" it by adding the import.
 *
 * <p><strong>Why the scope columns are filled by the service.</strong>
 * {@link #assignScope(Long, Long)} is called from the application service using
 * values read off the authenticated request context, never from a JSON body.
 * A caller therefore cannot write a row into another company by tampering with
 * the request payload.
 *
 * @param <T> the type of the entity identifier
 * @since 1.0.0
 */
@MappedSuperclass
@Getter
@Setter
public abstract class TenantScopedEntity<T extends Serializable> extends OptimisticLock<T> {

    private static final long serialVersionUID = 1L;

    /** Owning tenant, taken from the signed {@code tenantId} token claim. */
    @NotNull(message = "Tenant ID is required")
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    /** Owning company, taken from the {@code X-Company-Id} request header. */
    @NotNull(message = "Company ID is required")
    @Column(name = "company_id", nullable = false)
    private Long companyId;

    /**
     * Assigns the tenant and company this row belongs to.
     *
     * <p>The tenant column is mapped {@code updatable = false}, so JPA never
     * rewrites it on an update and an entity cannot be migrated between
     * tenants by accident.
     *
     * @param tenantId  the owning tenant
     * @param companyId the owning company
     * @throws IllegalStateException if either id is null, or if this row is
     *                               already persisted under a different tenant
     */
    public void assignScope(Long tenantId, Long companyId) {
        if (tenantId == null) {
            throw new IllegalStateException("Cannot assign a null tenant scope");
        }
        if (companyId == null) {
            throw new IllegalStateException("Cannot assign a null company scope");
        }
        if (this.tenantId != null && !this.tenantId.equals(tenantId)) {
            throw new IllegalStateException(
                    "Refusing to move an entity across tenants: persisted under tenant "
                            + this.tenantId + ", attempted under tenant " + tenantId);
        }
        this.tenantId = tenantId;
        this.companyId = companyId;
    }

    /**
     * Guards against persisting an unscoped row.
     *
     * <p>A row without a tenant or company would be invisible to every scoped
     * query and effectively orphaned, so fail loudly at flush time instead.
     */
    @PrePersist
    void requireScope() {
        if (tenantId == null || companyId == null) {
            throw new IllegalStateException(
                    getClass().getSimpleName() + " must be assigned a tenant and company "
                            + "scope via assignScope() before it can be persisted");
        }
    }

    /**
     * Returns whether this row belongs to the given tenant and company.
     *
     * @param tenantId  the tenant to test
     * @param companyId the company to test
     * @return {@code true} if both match
     */
    public boolean isOwnedBy(Long tenantId, Long companyId) {
        return this.tenantId.equals(tenantId) && this.companyId.equals(companyId);
    }
}
