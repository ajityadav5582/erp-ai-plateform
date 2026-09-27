package com.erp.platform.identity.interfaces.rest;

import com.erp.platform.identity.application.DistrictService;
import com.erp.platform.identity.application.dto.CreateDistrictRequest;
import com.erp.platform.identity.application.dto.DistrictListResponse;
import com.erp.platform.identity.application.dto.DistrictResponse;
import com.erp.platform.identity.application.dto.UpdateDistrictRequest;
import com.erp.platform.identity.application.security.RequirePermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * REST controller for district management.
 *
 * <p>Exposes a standard REST API for district CRUD operations, following the
 * platform API standards. Districts are master data shared across tenants,
 * so no tenant context is required for any operation.
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/districts")
@RequiredArgsConstructor
public class DistrictController {

    private final DistrictService districtService;

    /**
     * Creates a new district.
     *
     * @param request the district creation request
     * @return 201 Created with the created district and a {@code Location} header
     */
    @PostMapping
    @RequirePermission("DISTRICT_CREATE")
    public ResponseEntity<DistrictResponse> createDistrict(
            @Valid @RequestBody CreateDistrictRequest request,
            UriComponentsBuilder uriBuilder) {
        DistrictResponse response = districtService.createDistrict(request);
        return ResponseEntity
                .created(uriBuilder.path("/api/v1/districts/{id}").buildAndExpand(response.id()).toUri())
                .body(response);
    }

    /**
     * Lists all districts with pagination.
     *
     * @param pageable pagination and sorting parameters
     * @return 200 OK with a page of districts
     */
    @GetMapping
    @RequirePermission("DISTRICT_READ")
    public ResponseEntity<Page<DistrictListResponse>> listDistricts(Pageable pageable) {
        Page<DistrictListResponse> page = districtService.listDistricts(pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * Lists all active districts.
     *
     * @return 200 OK with a list of active districts
     */
    @GetMapping("/active")
    @RequirePermission("DISTRICT_READ")
    public ResponseEntity<java.util.List<DistrictListResponse>> listActiveDistricts() {
        return ResponseEntity.ok(districtService.listActiveDistricts());
    }

    /**
     * Lists all active districts for a province.
     *
     * @param provinceId the province ID
     * @return 200 OK with a list of active districts for the province
     */
    @GetMapping("/by-province")
    @RequirePermission("DISTRICT_READ")
    public ResponseEntity<java.util.List<DistrictListResponse>> listDistrictsByProvince(
            @RequestParam Long provinceId) {
        return ResponseEntity.ok(districtService.listDistrictsByProvince(provinceId));
    }

    /**
     * Gets a district by its id.
     *
     * @param id the district id
     * @return 200 OK with the district
     */
    @GetMapping("/{id}")
    @RequirePermission("DISTRICT_READ")
    public ResponseEntity<DistrictResponse> getDistrictById(@PathVariable Long id) {
        DistrictResponse response = districtService.getDistrictById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Gets a district by its unique code.
     *
     * @param code the district code
     * @return 200 OK with the district
     */
    @GetMapping("/by-code")
    @RequirePermission("DISTRICT_READ")
    public ResponseEntity<DistrictResponse> getDistrictByCode(@RequestParam String code) {
        DistrictResponse response = districtService.getDistrictByCode(code);
        return ResponseEntity.ok(response);
    }

    /**
     * Fully updates a district.
     *
     * @param id the district id
     * @param request the update request
     * @return 200 OK with the updated district
     */
    @PutMapping("/{id}")
    @RequirePermission("DISTRICT_UPDATE")
    public ResponseEntity<DistrictResponse> updateDistrict(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDistrictRequest request) {
        DistrictResponse response = districtService.updateDistrict(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a district.
     *
     * @param id the district id
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    @RequirePermission("DISTRICT_DELETE")
    public ResponseEntity<Void> deleteDistrict(@PathVariable Long id) {
        districtService.deleteDistrict(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Activates a district (INACTIVE → ACTIVE).
     *
     * @param id the district id
     * @return 200 OK with the activated district
     */
    @PostMapping("/{id}/activate")
    @RequirePermission("DISTRICT_UPDATE")
    public ResponseEntity<DistrictResponse> activateDistrict(@PathVariable Long id) {
        DistrictResponse response = districtService.activateDistrict(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Deactivates a district (ACTIVE → INACTIVE).
     *
     * @param id the district id
     * @return 200 OK with the deactivated district
     */
    @PostMapping("/{id}/deactivate")
    @RequirePermission("DISTRICT_UPDATE")
    public ResponseEntity<DistrictResponse> deactivateDistrict(@PathVariable Long id) {
        DistrictResponse response = districtService.deactivateDistrict(id);
        return ResponseEntity.ok(response);
    }
}
