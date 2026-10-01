package com.erp.business.inventory.interfaces.rest;

import com.erp.business.inventory.shared.context.BusinessContextAccessor;
import com.erp.business.inventory.unit.dto.UnitCreateRequest;
import com.erp.business.inventory.unit.dto.UnitResponse;
import com.erp.business.inventory.unit.dto.UnitUpdateRequest;
import com.erp.business.inventory.unit.service.UnitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * REST surface for units of measure.
 *
 * <p>No endpoint accepts a tenant or company id. Both are resolved from the
 * validated request context through {@link BusinessContextAccessor} on every
 * call, so a client cannot create or read a row belonging to another company by
 * putting an id in the body.
 *
 * <p>{@code search} is a parameter on the collection endpoint rather than a
 * separate path so the list and search responses share one pagination contract.
 */
@RestController
@RequestMapping("/api/v1/inventory/units")
@RequiredArgsConstructor
public class UnitController {

    private final UnitService unitService;
    private final BusinessContextAccessor context;

    @PostMapping
    public ResponseEntity<UnitResponse> createUnit(
            @Valid @RequestBody UnitCreateRequest request,
            UriComponentsBuilder uriBuilder) {
        Long tenantId = context.getTenantId();
        Long companyId = context.getCompanyId();
        UnitResponse response = unitService.createUnit(tenantId, companyId, request);
        return ResponseEntity
                .created(uriBuilder.path("/api/v1/inventory/units/{id}").buildAndExpand(response.id()).toUri())
                .body(response);
    }

    @GetMapping
    public ResponseEntity<Page<UnitResponse>> listUnits(
            @RequestParam(required = false) String search,
            Pageable pageable) {
        Long tenantId = context.getTenantId();
        Long companyId = context.getCompanyId();
        Page<UnitResponse> page = unitService.searchUnits(tenantId, companyId, search, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UnitResponse> getUnitById(@PathVariable Long id) {
        Long tenantId = context.getTenantId();
        return ResponseEntity.ok(unitService.getUnitById(tenantId, id));
    }

    @GetMapping("/by-code")
    public ResponseEntity<UnitResponse> getUnitByCode(@RequestParam String code) {
        Long tenantId = context.getTenantId();
        Long companyId = context.getCompanyId();
        return ResponseEntity.ok(unitService.getUnitByCode(tenantId, companyId, code));
    }

    /**
     * Unpaginated list of active units, for populating picker dropdowns.
     *
     * <p>Safe to leave unpaged: a company defines tens of units, not thousands,
     * and a dropdown needs every option anyway.
     */
    @GetMapping("/active")
    public ResponseEntity<List<UnitResponse>> listActiveUnits() {
        Long tenantId = context.getTenantId();
        Long companyId = context.getCompanyId();
        return ResponseEntity.ok(unitService.listActiveUnits(tenantId, companyId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UnitResponse> updateUnit(
            @PathVariable Long id,
            @Valid @RequestBody UnitUpdateRequest request) {
        Long tenantId = context.getTenantId();
        return ResponseEntity.ok(unitService.updateUnit(tenantId, id, request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UnitResponse> patchUnit(
            @PathVariable Long id,
            @Valid @RequestBody UnitUpdateRequest request) {
        Long tenantId = context.getTenantId();
        return ResponseEntity.ok(unitService.updateUnit(tenantId, id, request));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<UnitResponse> activateUnit(@PathVariable Long id) {
        Long tenantId = context.getTenantId();
        return ResponseEntity.ok(unitService.activateUnit(tenantId, id));
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<UnitResponse> deactivateUnit(@PathVariable Long id) {
        Long tenantId = context.getTenantId();
        return ResponseEntity.ok(unitService.deactivateUnit(tenantId, id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUnit(@PathVariable Long id) {
        Long tenantId = context.getTenantId();
        unitService.deleteUnit(tenantId, id);
        return ResponseEntity.noContent().build();
    }
}
