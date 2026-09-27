package com.erp.platform.identity.interfaces.rest;

import com.erp.platform.identity.application.LocalLevelService;
import com.erp.platform.identity.application.dto.LocalLevelListResponse;
import com.erp.platform.identity.application.dto.LocalLevelResponse;
import com.erp.platform.identity.application.security.RequirePermission;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for local level (municipality / rural municipality) management.
 *
 * <p>Exposes read-only endpoints for local levels organized under districts.
 * A local level is referenced by a branch via {@code local_level_id} so that
 * the branch's location can be derived from province -> district -> local level.
 *
 * <p>Local levels are master data shared across tenants and are therefore
 * not tenant-scoped.
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/local-levels")
@RequiredArgsConstructor
public class LocalLevelController {

    private final LocalLevelService localLevelService;

    /**
     * Lists all local levels for a district.
     *
     * @param districtId the district ID
     * @return 200 OK with a list of local levels for the district
     */
    @GetMapping("/by-district")
    @RequirePermission("LOCAL_LEVEL_READ")
    public ResponseEntity<List<LocalLevelListResponse>> listLocalLevelsByDistrict(
            @RequestParam Long districtId) {
        return ResponseEntity.ok(localLevelService.listLocalLevelsByDistrict(districtId));
    }

    /**
     * Gets a local level by its municipality id.
     *
     * @param municipalityId the municipality id
     * @return 200 OK with the local level
     */
    @GetMapping("/by-municipality-id")
    @RequirePermission("LOCAL_LEVEL_READ")
    public ResponseEntity<LocalLevelResponse> getLocalLevelByMunicipalityId(
            @RequestParam String municipalityId) {
        return ResponseEntity.ok(localLevelService.getLocalLevelByMunicipalityId(municipalityId));
    }
}
