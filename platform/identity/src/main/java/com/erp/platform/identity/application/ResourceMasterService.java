package com.erp.platform.identity.application;

import com.erp.platform.identity.application.dto.CreateResourceMasterRequest;
import com.erp.platform.identity.application.dto.ResourceMasterListResponse;
import com.erp.platform.identity.application.dto.ResourceMasterResponse;
import com.erp.platform.identity.application.dto.UpdateResourceMasterRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for resource (module) master data management operations.
 *
 * <p>Resources are master data shared across tenants, so no tenant ID is
 * required for any operation.
 *
 * @since 1.0.0
 */
public interface ResourceMasterService {

    /**
     * Create a new resource.
     *
     * @param request the create resource request
     * @return the created resource response
     */
    ResourceMasterResponse createResource(CreateResourceMasterRequest request);

    /**
     * Get a resource by its id.
     *
     * @param id the resource id
     * @return the resource response
     */
    ResourceMasterResponse getResourceById(Long id);

    /**
     * Get a resource by its unique resource code.
     *
     * @param resourceCode the resource code
     * @return the resource response
     */
    ResourceMasterResponse getResourceByCode(String resourceCode);

    /**
     * List all resources with pagination.
     *
     * @param pageable the pagination parameters
     * @return page of resource list responses
     */
    Page<ResourceMasterListResponse> listResources(Pageable pageable);

    /**
     * List all active resources.
     *
     * @return list of active resource responses
     */
    List<ResourceMasterListResponse> listActiveResources();

    /**
     * List all active resources ordered by name.
     *
     * @return list of active resources ordered by name
     */
    List<ResourceMasterListResponse> listActiveResourcesByName();

    /**
     * List active resources by module.
     *
     * @param module the module name
     * @return list of active resources for the module
     */
    List<ResourceMasterListResponse> listActiveResourcesByModule(String module);

    /**
     * Update an existing resource.
     *
     * @param id      the resource id
     * @param request the update resource request
     * @return the updated resource response
     */
    ResourceMasterResponse updateResource(Long id, UpdateResourceMasterRequest request);

    /**
     * Activate a resource (INACTIVE → ACTIVE).
     *
     * @param id the resource id
     * @return the activated resource response
     */
    ResourceMasterResponse activateResource(Long id);

    /**
     * Deactivate a resource (ACTIVE → INACTIVE).
     *
     * @param id the resource id
     * @return the deactivated resource response
     */
    ResourceMasterResponse deactivateResource(Long id);

    /**
     * Delete a resource.
     *
     * @param id the resource id
     */
    void deleteResource(Long id);
}
