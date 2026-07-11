package com.erp.platform.tenant.interfaces.rest;

import com.erp.platform.tenant.application.CreateTenantCommandHandler;
import com.erp.platform.tenant.application.CreateTenantRequest;
import com.erp.platform.tenant.application.DeleteTenantCommandHandler;
import com.erp.platform.tenant.application.GetTenantQueryHandler;
import com.erp.platform.tenant.application.ListTenantsQueryHandler;
import com.erp.platform.tenant.application.TenantResponse;
import com.erp.platform.tenant.application.UpdateTenantCommandHandler;
import com.erp.platform.tenant.application.UpdateTenantRequest;
import com.erp.platform.tenant.domain.Tenant;
import com.erp.platform.tenant.domain.TenantNotFoundException;
import com.erp.platform.tenant.domain.TenantStatus;
import com.erp.platform.tenant.infrastructure.persistence.TenantRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.function.Consumer;

/**
 * REST controller for tenant management.
 *
 * <p>Exposes a standard REST API for tenant CRUD operations and lifecycle state
 * transitions, following the platform API standards:
 * <ul>
 *   <li>Versioned base path {@code /api/v1/tenants}</li>
 *   <li>Proper HTTP methods (GET, POST, PUT, PATCH, DELETE)</li>
 *   <li>Appropriate status codes (201 + Location on create, 204 on delete)</li>
 *   <li>Pagination and filtering on collection endpoints</li>
 * </ul>
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final CreateTenantCommandHandler createTenantCommandHandler;
    private final GetTenantQueryHandler getTenantQueryHandler;
    private final ListTenantsQueryHandler listTenantsQueryHandler;
    private final UpdateTenantCommandHandler updateTenantCommandHandler;
    private final DeleteTenantCommandHandler deleteTenantCommandHandler;
    private final TenantRepository tenantRepository;

    /**
     * Creates a new tenant.
     *
     * @param request the tenant creation request
     * @return 201 Created with the created tenant and a {@code Location} header
     */
    @PostMapping
    public ResponseEntity<TenantResponse> createTenant(
            @Valid @RequestBody CreateTenantRequest request,
            UriComponentsBuilder uriBuilder) {
        TenantResponse response = createTenantCommandHandler.handle(request);
        return ResponseEntity
                .created(uriBuilder.path("/api/v1/tenants/{id}").buildAndExpand(response.id()).toUri())
                .body(response);
    }

    /**
     * Lists tenants with pagination and optional status filtering.
     *
     * @param status  optional lifecycle status filter (e.g. {@code ACTIVE})
     * @param pageable pagination and sorting parameters (e.g. {@code ?page=0&size=20&sort=createdAt,desc})
     * @return 200 OK with a page of tenants
     */
    @GetMapping
    public ResponseEntity<Page<TenantResponse>> listTenants(
            @RequestParam(required = false) TenantStatus status,
            Pageable pageable) {
        Page<TenantResponse> page = listTenantsQueryHandler.handle(status, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * Gets a tenant by its primary key.
     *
     * @param id the tenant primary key
     * @return 200 OK with the tenant
     */
    @GetMapping("/{id}")
    public ResponseEntity<TenantResponse> getTenant(@PathVariable Long id) {
        return ResponseEntity.ok(getTenantQueryHandler.handle(id));
    }

    /**
     * Fully updates a tenant.
     *
     * @param id      the tenant primary key
     * @param request the update request (only non-null fields are applied)
     * @return 200 OK with the updated tenant
     */
    @PutMapping("/{id}")
    public ResponseEntity<TenantResponse> updateTenant(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTenantRequest request) {
        return ResponseEntity.ok(updateTenantCommandHandler.handle(id, request));
    }

    /**
     * Partially updates a tenant.
     *
     * @param id      the tenant primary key
     * @param request the partial update request
     * @return 200 OK with the updated tenant
     */
    @PatchMapping("/{id}")
    public ResponseEntity<TenantResponse> patchTenant(
            @PathVariable Long id,
            @RequestBody UpdateTenantRequest request) {
        return ResponseEntity.ok(updateTenantCommandHandler.handle(id, request));
    }

    /**
     * Deletes a tenant.
     *
     * @param id the tenant primary key
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTenant(@PathVariable Long id) {
        deleteTenantCommandHandler.handle(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Activates a tenant (PENDING/TRIAL/EXPIRED → ACTIVE).
     */
    @PostMapping("/{id}/activate")
    public ResponseEntity<TenantResponse> activateTenant(@PathVariable Long id) {
        return ResponseEntity.ok(applyLifecycle(id, t -> t.activate(Instant.now())));
    }

    /**
     * Suspends an active tenant (ACTIVE → SUSPENDED).
     */
    @PostMapping("/{id}/suspend")
    public ResponseEntity<TenantResponse> suspendTenant(@PathVariable Long id) {
        return ResponseEntity.ok(applyLifecycle(id, t -> t.suspend(Instant.now())));
    }

    /**
     * Reactivates a suspended tenant (SUSPENDED → ACTIVE).
     */
    @PostMapping("/{id}/reactivate")
    public ResponseEntity<TenantResponse> reactivateTenant(@PathVariable Long id) {
        return ResponseEntity.ok(applyLifecycle(id, t -> t.reactivate(Instant.now())));
    }

    /**
     * Deactivates a tenant (ACTIVE/SUSPENDED → DEACTIVATED).
     */
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<TenantResponse> deactivateTenant(@PathVariable Long id) {
        return ResponseEntity.ok(applyLifecycle(id, t -> t.deactivate(Instant.now())));
    }

    /**
     * Archives a tenant (terminal state).
     */
    @PostMapping("/{id}/archive")
    public ResponseEntity<TenantResponse> archiveTenant(@PathVariable Long id) {
        return ResponseEntity.ok(applyLifecycle(id, t -> t.archive(Instant.now())));
    }

    /**
     * Marks a tenant subscription as expired.
     */
    @PostMapping("/{id}/expire")
    public ResponseEntity<TenantResponse> expireTenant(@PathVariable Long id) {
        return ResponseEntity.ok(applyLifecycle(id, t -> t.expire(Instant.now())));
    }

    /**
     * Loads a tenant, applies the given lifecycle transition, persists it and
     * maps it back to a response.
     */
    private TenantResponse applyLifecycle(Long id, Consumer<Tenant> transition) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new TenantNotFoundException(id));
        transition.accept(tenant);
        Tenant savedTenant = tenantRepository.save(tenant);
        return TenantResponse.from(savedTenant);
    }
}
