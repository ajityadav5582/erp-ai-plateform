package com.erp.platform.identity.interfaces.rest;

import com.erp.platform.identity.application.LocalLevelTypeService;
import com.erp.platform.identity.application.dto.LocalLevelTypeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for local level type master data.
 *
 * <p>Exposes read-only REST endpoints for local level type master data seeded by Flyway
 * migration {@code V026__create_nepal_administrative_divisions.sql}. Local level types
 * are reference data shared across tenants, so no tenant context is required.
 *
 * @since 2.0.0
 */
@RestController
@RequestMapping("/api/v1/identity/local-level-types")
@RequiredArgsConstructor
public class LocalLevelTypeController {

    private final LocalLevelTypeService localLevelTypeService;

    /**
     * Gets a local level type by its id.
     *
     * @param id the local level type id
     * @return 200 OK with the local level type
     */
    @GetMapping("/{id}")
    public ResponseEntity<LocalLevelTypeResponse> getLocalLevelTypeById(@PathVariable Short id) {
        return ResponseEntity.ok(localLevelTypeService.getLocalLevelTypeById(id));
    }

    /**
     * Gets a local level type by its name.
     *
     * @param name the local level type name
     * @return 200 OK with the local level type
     */
    @GetMapping("/by-name")
    public ResponseEntity<LocalLevelTypeResponse> getLocalLevelTypeByName(@RequestParam String name) {
        return ResponseEntity.ok(localLevelTypeService.getLocalLevelTypeByName(name));
    }

    /**
     * Lists all local level types.
     *
     * @return 200 OK with a list of all local level types
     */
    @GetMapping("/all")
    public ResponseEntity<List<LocalLevelTypeResponse>> listAllLocalLevelTypes() {
        return ResponseEntity.ok(localLevelTypeService.listAllLocalLevelTypes());
    }
}
