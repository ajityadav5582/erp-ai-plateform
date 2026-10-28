package com.erp.business.inventory.interfaces.rest;

import com.erp.platform.business.context.BusinessContextAccessor;
import com.erp.business.inventory.warehouse.dto.CreateWarehouseRequest;
import com.erp.business.inventory.warehouse.dto.UpdateWarehouseRequest;
import com.erp.business.inventory.warehouse.dto.WarehouseResponse;
import com.erp.business.inventory.warehouse.service.WarehouseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

/**
 * Warehouse endpoints.
 *
 * <p>There is no delete endpoint, unlike suppliers. A warehouse that has ever received
 * stock cannot be removed without orphaning every movement posted against it; the
 * record has to survive as history. Retiring it is what {@code deactivate} is for.
 *
 * <p>The tenant and company come from the validated request context, never from the
 * request body, so a caller cannot address another company's warehouses by tampering
 * with a payload.
 */
@RestController
@RequestMapping("/api/v1/inventory/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private static final String BASE_PATH = "/api/v1/inventory/warehouses";

    private final WarehouseService warehouseService;
    private final BusinessContextAccessor context;

    @PostMapping
    public ResponseEntity<WarehouseResponse> createWarehouse(
            @Valid @RequestBody CreateWarehouseRequest request,
            UriComponentsBuilder uriBuilder) {

        WarehouseResponse response = warehouseService.createWarehouse(
                context.getTenantId(), context.getCompanyId(), request);

        return ResponseEntity
                .created(uriBuilder.path(BASE_PATH + "/{id}")
                        .buildAndExpand(response.id()).toUri())
                .body(response);
    }

    /**
     * Returns the company's default warehouse, creating one on first call.
     *
     * <p>A GET that can write is unusual and deliberate: it is what lets a brand-new
     * company receive goods without anyone having to configure a warehouse first, and
     * it is idempotent, so calling it repeatedly is harmless. The alternative -
     * refusing until one exists - turns ordinary setup order into a prerequisite.
     */
    @GetMapping("/default")
    public ResponseEntity<WarehouseResponse> getOrCreateDefault(
            @RequestParam(required = false) Long branchId) {

        return ResponseEntity.ok(warehouseService.getOrCreateDefault(
                context.getTenantId(), context.getCompanyId(), branchId));
    }

    /** Promotes a warehouse to be the company default, demoting the previous one. */
    @PostMapping("/{id}/default")
    public ResponseEntity<WarehouseResponse> setDefaultWarehouse(@PathVariable Long id) {
        return ResponseEntity.ok(warehouseService.setDefaultWarehouse(
                context.getTenantId(), context.getCompanyId(), id));
    }

    @GetMapping
    public ResponseEntity<Page<WarehouseResponse>> listWarehouses(Pageable pageable) {
        return ResponseEntity.ok(warehouseService.listWarehouses(
                context.getTenantId(), context.getCompanyId(), pageable));
    }

    @GetMapping("/active")
    public ResponseEntity<List<WarehouseResponse>> listActiveWarehouses() {
        return ResponseEntity.ok(warehouseService.listActiveWarehouses(
                context.getTenantId(), context.getCompanyId()));
    }

    @GetMapping("/by-branch/{branchId}")
    public ResponseEntity<List<WarehouseResponse>> listByBranch(@PathVariable Long branchId) {
        return ResponseEntity.ok(warehouseService.listWarehousesByBranch(
                context.getTenantId(), context.getCompanyId(), branchId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WarehouseResponse> getWarehouseById(@PathVariable Long id) {
        return ResponseEntity.ok(warehouseService.getWarehouseById(
                context.getTenantId(), context.getCompanyId(), id));
    }

    @GetMapping("/by-code")
    public ResponseEntity<WarehouseResponse> getWarehouseByCode(@RequestParam String code) {
        return ResponseEntity.ok(warehouseService.getWarehouseByCode(
                context.getTenantId(), context.getCompanyId(), code));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WarehouseResponse> updateWarehouse(
            @PathVariable Long id,
            @Valid @RequestBody UpdateWarehouseRequest request) {
        return ResponseEntity.ok(warehouseService.updateWarehouse(
                context.getTenantId(), context.getCompanyId(), id, request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<WarehouseResponse> patchWarehouse(
            @PathVariable Long id,
            @Valid @RequestBody UpdateWarehouseRequest request) {
        return ResponseEntity.ok(warehouseService.updateWarehouse(
                context.getTenantId(), context.getCompanyId(), id, request));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<WarehouseResponse> activateWarehouse(@PathVariable Long id) {
        return ResponseEntity.ok(warehouseService.activateWarehouse(
                context.getTenantId(), context.getCompanyId(), id));
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<WarehouseResponse> deactivateWarehouse(@PathVariable Long id) {
        return ResponseEntity.ok(warehouseService.deactivateWarehouse(
                context.getTenantId(), context.getCompanyId(), id));
    }
}
