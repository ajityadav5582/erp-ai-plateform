package com.erp.platform.identity.interfaces.rest;

import com.erp.platform.identity.application.ProvinceService;
import com.erp.platform.identity.application.dto.CreateProvinceRequest;
import com.erp.platform.identity.application.dto.ProvinceListResponse;
import com.erp.platform.identity.application.dto.ProvinceResponse;
import com.erp.platform.identity.application.dto.UpdateProvinceRequest;
import com.erp.platform.identity.application.security.RequirePermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * REST controller for province management.
 *
 * <p>Exposes a standard REST API for province CRUD operations, following the
 * platform API standards. Provinces are master data shared across tenants,
 * so no tenant context is required for any operation.
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/identity/provinces")
@RequiredArgsConstructor
public class ProvinceController {

    private final ProvinceService provinceService;

    /**
     * Creates a new province.
     *
     * @param request the province creation request
     * @return 201 Created with the created province and a {@code Location} header
     */
    @PostMapping
    @RequirePermission("PROVINCE_CREATE")
    public ResponseEntity<ProvinceResponse> createProvince(
            @Valid @RequestBody CreateProvinceRequest request,
            UriComponentsBuilder uriBuilder) {
        ProvinceResponse response = provinceService.createProvince(request);
        return ResponseEntity
                .created(uriBuilder.path("/api/v1/identity/provinces/{id}").buildAndExpand(response.id()).toUri())
                .body(response);
    }

    /**
     * Lists all provinces with pagination.
     *
     * @param pageable pagination and sorting parameters
     * @return 200 OK with a page of provinces
     */
    @GetMapping
    @RequirePermission("PROVINCE_READ")
    public ResponseEntity<Page<ProvinceListResponse>> listProvinces(Pageable pageable) {
        Page<ProvinceListResponse> page = provinceService.listProvinces(pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * Lists all active provinces.
     *
     * @return 200 OK with a list of active provinces
     */
    @GetMapping("/active")
    @RequirePermission("PROVINCE_READ")
    public ResponseEntity<java.util.List<ProvinceListResponse>> listActiveProvinces() {
        return ResponseEntity.ok(provinceService.listActiveProvinces());
    }

    /**
     * Gets a province by its id.
     *
     * @param id the province id
     * @return 200 OK with the province
     */
    @GetMapping("/{id}")
    @RequirePermission("PROVINCE_READ")
    public ResponseEntity<ProvinceResponse> getProvinceById(@PathVariable Long id) {
        ProvinceResponse response = provinceService.getProvinceById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Gets a province by its unique code.
     *
     * @param code the province code
     * @return 200 OK with the province
     */
    @GetMapping("/by-code")
    @RequirePermission("PROVINCE_READ")
    public ResponseEntity<ProvinceResponse> getProvinceByCode(@RequestParam String code) {
        ProvinceResponse response = provinceService.getProvinceByCode(code);
        return ResponseEntity.ok(response);
    }

    /**
     * Fully updates a province.
     *
     * @param id the province id
     * @param request the update request
     * @return 200 OK with the updated province
     */
    @PutMapping("/{id}")
    @RequirePermission("PROVINCE_UPDATE")
    public ResponseEntity<ProvinceResponse> updateProvince(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProvinceRequest request) {
        ProvinceResponse response = provinceService.updateProvince(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a province.
     *
     * @param id the province id
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    @RequirePermission("PROVINCE_DELETE")
    public ResponseEntity<Void> deleteProvince(@PathVariable Long id) {
        provinceService.deleteProvince(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Activates a province (INACTIVE → ACTIVE).
     *
     * @param id the province id
     * @return 200 OK with the activated province
     */
    @PostMapping("/{id}/activate")
    @RequirePermission("PROVINCE_UPDATE")
    public ResponseEntity<ProvinceResponse> activateProvince(@PathVariable Long id) {
        ProvinceResponse response = provinceService.activateProvince(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Deactivates a province (ACTIVE → INACTIVE).
     *
     * @param id the province id
     * @return 200 OK with the deactivated province
     */
    @PostMapping("/{id}/deactivate")
    @RequirePermission("PROVINCE_UPDATE")
    public ResponseEntity<ProvinceResponse> deactivateProvince(@PathVariable Long id) {
        ProvinceResponse response = provinceService.deactivateProvince(id);
        return ResponseEntity.ok(response);
    }
}
