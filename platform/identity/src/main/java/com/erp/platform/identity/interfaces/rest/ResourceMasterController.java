package com.erp.platform.identity.interfaces.rest;

import com.erp.platform.identity.application.ResourceMasterService;
import com.erp.platform.identity.application.dto.CreateResourceMasterRequest;
import com.erp.platform.identity.application.dto.ResourceMasterListResponse;
import com.erp.platform.identity.application.dto.ResourceMasterResponse;
import com.erp.platform.identity.application.dto.UpdateResourceMasterRequest;
import com.erp.platform.identity.application.security.RequirePermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * REST controller for resource (module) master data management.
 *
 * <p>Exposes a standard REST API for resource CRUD operations, following the
 * platform API standards. Resources are master data shared across tenants,
 * so no tenant context is required for any operation.
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/resources")
@RequiredArgsConstructor
public class ResourceMasterController {

    private final ResourceMasterService resourceMasterService;

    /**
     * Creates a new resource.
     *
     * @param request the resource creation request
     * @return 201 Created with the created resource and a {@code Location} header
     */
    @PostMapping
    @RequirePermission("RESOURCE_CREATE")
    public ResponseEntity<ResourceMasterResponse> createResource(
            @Valid @RequestBody CreateResourceMasterRequest request,
            UriComponentsBuilder uriBuilder) {
        ResourceMasterResponse response = resourceMasterService.createResource(request);
        return ResponseEntity
                .created(uriBuilder.path("/api/v1/resources/{id}").buildAndExpand(response.id()).toUri())
                .body(response);
    }

    /**
     * Lists all resources with pagination.
     *
     * @param pageable pagination and sorting parameters
     * @return 200 OK with a page of resources
     */
    @GetMapping
    @RequirePermission("RESOURCE_READ")
    public ResponseEntity<Page<ResourceMasterListResponse>> listResources(Pageable pageable) {
        Page<ResourceMasterListResponse> page = resourceMasterService.listResources(pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * Lists all active resources.
     *
     * @return 200 OK with a list of active resources
     */
    @GetMapping("/active")
    @RequirePermission("RESOURCE_READ")
    public ResponseEntity<java.util.List<ResourceMasterListResponse>> listActiveResources() {
        return ResponseEntity.ok(resourceMasterService.listActiveResources());
    }

    /**
     * Lists all active resources ordered by name.
     *
     * @return 200 OK with a list of active resources ordered by name
     */
    @GetMapping("/active-by-name")
    @RequirePermission("RESOURCE_READ")
    public ResponseEntity<java.util.List<ResourceMasterListResponse>> listActiveResourcesByName() {
        return ResponseEntity.ok(resourceMasterService.listActiveResourcesByName());
    }

    /**
     * Lists active resources by module.
     *
     * @param module the module name
     * @return 200 OK with a list of active resources for the module
     */
    @GetMapping("/by-module")
    @RequirePermission("RESOURCE_READ")
    public ResponseEntity<java.util.List<ResourceMasterListResponse>> listActiveResourcesByModule(
            @RequestParam String module) {
        return ResponseEntity.ok(resourceMasterService.listActiveResourcesByModule(module));
    }

    /**
     * Gets a resource by its id.
     *
     * @param id the resource id
     * @return 200 OK with the resource
     */
    @GetMapping("/{id}")
    @RequirePermission("RESOURCE_READ")
    public ResponseEntity<ResourceMasterResponse> getResourceById(@PathVariable Long id) {
        ResourceMasterResponse response = resourceMasterService.getResourceById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Gets a resource by its unique code.
     *
     * @param code the resource code
     * @return 200 OK with the resource
     */
    @GetMapping("/by-code")
    @RequirePermission("RESOURCE_READ")
    public ResponseEntity<ResourceMasterResponse> getResourceByCode(@RequestParam String code) {
        ResourceMasterResponse response = resourceMasterService.getResourceByCode(code);
        return ResponseEntity.ok(response);
    }

    /**
     * Fully updates a resource.
     *
     * @param id      the resource id
     * @param request the update request
     * @return 200 OK with the updated resource
     */
    @PutMapping("/{id}")
    @RequirePermission("RESOURCE_UPDATE")
    public ResponseEntity<ResourceMasterResponse> updateResource(
            @PathVariable Long id,
            @Valid @RequestBody UpdateResourceMasterRequest request) {
        ResourceMasterResponse response = resourceMasterService.updateResource(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a resource.
     *
     * @param id the resource id
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    @RequirePermission("RESOURCE_DELETE")
    public ResponseEntity<Void> deleteResource(@PathVariable Long id) {
        resourceMasterService.deleteResource(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Activates a resource (INACTIVE → ACTIVE).
     *
     * @param id the resource id
     * @return 200 OK with the activated resource
     */
    @PostMapping("/{id}/activate")
    @RequirePermission("RESOURCE_UPDATE")
    public ResponseEntity<ResourceMasterResponse> activateResource(@PathVariable Long id) {
        ResourceMasterResponse response = resourceMasterService.activateResource(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Deactivates a resource (ACTIVE → INACTIVE).
     *
     * @param id the resource id
     * @return 200 OK with the deactivated resource
     */
    @PostMapping("/{id}/deactivate")
    @RequirePermission("RESOURCE_UPDATE")
    public ResponseEntity<ResourceMasterResponse> deactivateResource(@PathVariable Long id) {
        ResourceMasterResponse response = resourceMasterService.deactivateResource(id);
        return ResponseEntity.ok(response);
    }
}
