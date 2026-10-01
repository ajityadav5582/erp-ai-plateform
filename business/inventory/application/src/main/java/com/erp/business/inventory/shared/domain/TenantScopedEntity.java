package com.erp.business.inventory.shared.domain;

import com.erp.platform.data.lock.OptimisticLock;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Base class for every entity that belongs to exactly one tenant and one
 * company.
 *
 * <p>Every row in the inventory schema is multi-tenant AND multi-company: a
 * category, product, unit, vendor or stock movement always belongs to a single
 * company inside a single tenant. Declaring the two columns once here means a
 * new feature cannot forget them, and every derived query can use the Spring
 * Data names {@code findByTenantIdAndCompanyId...}.
 *
 * <p><strong>Why the columns are filled by the service, not the client.</strong>
 * {@link #assignScope(Long, Long)} is called from the application service using
 * values read off the request context (see
 * {@link com.erp.business.inventory.shared.context.BusinessContextAccessor}).
 * Because a caller cannot pass these ids in from a JSON body, there is no way
 * for a client to write a row into another company by tampering with the
 * request payload.
 *
 * <p><strong>Usage.</strong>
 * <pre>{@code
 * @Entity
 * @Table(name = "vendors")
 * public class Vendor extends TenantScopedEntity<Long> {
 *     // no tenantId / companyId fields here
 * }
 *
 * // in the service
 * Vendor vendor = vendorMapper.toEntity(request);
 * vendor.assignScope(context.getTenantId(), context.getCompanyId());
 * }</pre>
 *
 * @param <T> the type of the entity identifier
 * @since 1.0.0
 */
@MappedSuperclass
@Getter
@Setter
public abstract class TenantScopedEntity<T extends java.io.Serializable> extends OptimisticLock<T> {

    private static final long serialVersionUID = 1L;

    /**
     * The tenant this row belongs to. Taken from the signed token claim, never
     * from the request body.
     */
    @NotNull(message = "Tenant ID is required")
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    /**
     * The company this row belongs to. Taken from the {@code X-Company-Id}
     * header on the validated request context, never from the request body.
     */
    @NotNull(message = "Company ID is required")
    @Column(name = "company_id", nullable = false)
    private Long companyId;

    /**
     * Assigns the tenant and company this row belongs to.
     *
     * <p>Call this before persisting a new row. The tenant is mapped
     * {@code updatable = false}, so JPA never rewrites it on an update.
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
     * <p>A row that reached the database without a tenant or company would be
     * invisible to every scoped query and effectively orphaned, so fail loudly
     * at flush time instead.
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
