package com.erp.platform.identity.infrastructure.persistence;

import com.erp.platform.identity.domain.ResourceMaster;
import com.erp.platform.identity.domain.ResourceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for ResourceMaster aggregate.
 *
 * <p>Provides data access methods for resource (module) master data.
 * Resources are master data shared across tenants.
 *
 * @since 1.0.0
 */
public interface ResourceMasterRepository extends JpaRepository<ResourceMaster, Long> {

    /**
     * Finds a resource by its unique resource code.
     *
     * @param resourceCode the resource code
     * @return the resource if found, empty otherwise
     */
    Optional<ResourceMaster> findByResourceCode(String resourceCode);

    /**
     * Finds all active resources.
     *
     * @return list of active resources
     */
    List<ResourceMaster> findByStatus(ResourceStatus status);

    /**
     * Finds all active resources, ordered by resource name.
     *
     * @return list of active resources ordered by name
     */
    List<ResourceMaster> findByStatusOrderByResourceName(ResourceStatus status);

    /**
     * Finds active resources paginated.
     *
     * @param status    the resource status
     * @param pageable  the pagination parameters
     * @return a page of active resources
     */
    Page<ResourceMaster> findByStatus(ResourceStatus status, Pageable pageable);

    /**
     * Finds resources by module.
     *
     * @param module the module name
     * @return list of resources for the module
     */
    List<ResourceMaster> findByModule(String module);

    /**
     * Finds active resources by module, ordered by name.
     *
     * @param module the module name
     * @param status the resource status
     * @return list of active resources for the module
     */
    List<ResourceMaster> findByModuleAndStatusOrderByResourceName(String module, ResourceStatus status);

    /**
     * Checks if a resource code exists.
     *
     * @param resourceCode the resource code to check
     * @return true if the resource code exists, false otherwise
     */
    boolean existsByResourceCode(String resourceCode);

    /**
     * Finds resources by system flag.
     *
     * @param isSystem the system flag
     * @return list of resources with the given system flag
     */
    List<ResourceMaster> findByIsSystem(Boolean isSystem);

    /**
     * Finds system resources ordered by resource name.
     *
     * @return list of system resources ordered by name
     */
    List<ResourceMaster> findByIsSystemTrueOrderByResourceName();

    /**
     * Finds non-system resources ordered by resource name.
     *
     * @return list of non-system resources ordered by name
     */
    List<ResourceMaster> findByIsSystemFalseOrderByResourceName();
}
