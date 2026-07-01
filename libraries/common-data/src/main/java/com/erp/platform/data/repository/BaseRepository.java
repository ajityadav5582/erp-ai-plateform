package com.erp.platform.data.repository;

import com.erp.platform.common.kernel.TenantAwareEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.io.Serializable;
import java.util.Optional;

/**
 * Base repository interface providing common operations for all entities.
 *
 * <p>This interface combines JpaRepository with JpaSpecificationExecutor
 * to provide both standard CRUD operations and specification-based querying.
 *
 * <p>All business repositories should extend this interface.
 *
 * @param <T> the entity type
 * @param <ID> the entity identifier type
 * @since 1.0.0
 */
@NoRepositoryBean
public interface BaseRepository<T extends TenantAwareEntity<ID>, ID extends Serializable>
        extends JpaRepository<T, ID>, JpaSpecificationExecutor<T> {

    /**
     * Finds an entity by its ID and tenant ID.
     *
     * <p>This method ensures tenant isolation by requiring both the entity ID
     * and the tenant ID. The tenant ID is automatically resolved from the
     * current tenant context.
     *
     * @param id the entity ID
     * @param tenantId the tenant ID
     * @return the entity if found, empty otherwise
     */
    Optional<T> findByIdAndTenantId(ID id, ID tenantId);

    /**
     * Checks if an entity exists by its ID and tenant ID.
     *
     * @param id the entity ID
     * @param tenantId the tenant ID
     * @return true if the entity exists, false otherwise
     */
    boolean existsByIdAndTenantId(ID id, ID tenantId);

    /**
     * Deletes an entity by its ID and tenant ID.
     *
     * <p>This method ensures tenant isolation by requiring both the entity ID
     * and the tenant ID.
     *
     * @param id the entity ID
     * @param tenantId the tenant ID
     */
    void deleteByIdAndTenantId(ID id, ID tenantId);

    /**
     * Counts entities by tenant ID.
     *
     * @param tenantId the tenant ID
     * @return the count of entities for the tenant
     */
    long countByTenantId(ID tenantId);

    /**
     * Finds all entities by tenant ID.
     *
     * @param tenantId the tenant ID
     * @return list of entities for the tenant
     */
    java.util.List<T> findByTenantId(ID tenantId);

    /**
     * Finds all entities by tenant ID with pagination.
     *
     * @param tenantId the tenant ID
     * @param pageable the pagination information
     * @return page of entities for the tenant
     */
    Page<T> findByTenantId(ID tenantId, Pageable pageable);

    /**
     * Deletes all entities by tenant ID.
     *
     * <p>Use with caution - this deletes all data for a tenant.
     *
     * @param tenantId the tenant ID
     */
    void deleteByTenantId(ID tenantId);
}
