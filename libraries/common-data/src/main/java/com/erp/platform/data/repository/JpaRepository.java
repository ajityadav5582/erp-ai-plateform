package com.erp.platform.data.repository;

import com.erp.platform.common.kernel.TenantAwareEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.io.Serializable;
import java.util.Optional;

/**
 * Extended JPA repository interface with tenant-aware operations.
 *
 * <p>This interface provides the foundation for all JPA repositories
 * in the platform, ensuring consistent tenant isolation and query capabilities.
 *
 * @param <T> the entity type
 * @param <ID> the entity identifier type
 * @since 1.0.0
 */
@NoRepositoryBean
public interface JpaRepository<T extends TenantAwareEntity<ID>, ID extends Serializable>
        extends org.springframework.data.jpa.repository.JpaRepository<T, ID>,
                JpaSpecificationExecutor<T> {

    // Note: This interface intentionally shadows Spring Data's JpaRepository
    // to add tenant-aware methods. The Spring Data JpaRepository is referenced
    // via fully qualified name to avoid import conflicts.

    /**
     * Finds an entity by its ID and tenant ID.
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
     * @param tenantId the tenant ID
     */
    void deleteByTenantId(ID tenantId);
}
