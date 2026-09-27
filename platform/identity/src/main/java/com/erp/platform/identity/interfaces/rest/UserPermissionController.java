package com.erp.platform.identity.interfaces.rest;

import com.erp.platform.identity.application.AuthorizationService;
import com.erp.platform.identity.application.dto.PermissionCheckRequest;
import com.erp.platform.identity.application.dto.PermissionCheckResponse;
import com.erp.platform.identity.application.dto.UserPermissionsResponse;
import com.erp.platform.identity.domain.Action;
import com.erp.platform.identity.domain.Resource;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for UI frontend permission checks and user security profile queries.
 *
 * <p>Provides reusable endpoints for:
 * <ul>
 *   <li>Fetching the authenticated user's active tenant permissions (`/api/v1/permissions/me`)</li>
 *   <li>Batch evaluating permissions for UI component visibility (`/api/v1/permissions/check-batch`)</li>
 *   <li>Evaluating single permission check requests (`/api/v1/permissions/check-permission`)</li>
 * </ul>
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/user-permissions")
@RequiredArgsConstructor
public class UserPermissionController {

    private final AuthorizationService authorizationService;

    /**
     * Gets all active tenant permissions for the currently authenticated user.
     *
     * <p>Returns active role codes, permission code strings, and structured
     * Resource + Action pairs for UI client route guards and menu rendering.
     *
     * @return 200 OK with UserPermissionsResponse
     */
    @GetMapping("/me")
    public ResponseEntity<UserPermissionsResponse> getMyPermissions() {
        UserPermissionsResponse response = authorizationService.getUserPermissionsResponse();
        return ResponseEntity.ok(response);
    }

    /**
     * Evaluates a single permission check request for UI components.
     *
     * @param request the permission check request
     * @return 200 OK with PermissionCheckResponse
     */
    @PostMapping("/check")
    public ResponseEntity<PermissionCheckResponse> checkPermission(@Valid @RequestBody PermissionCheckRequest request) {
        PermissionCheckResponse response = authorizationService.evaluatePermission(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Batch evaluates multiple permission check requests for UI components.
     *
     * @param requests list of permission check requests
     * @return 200 OK with list of PermissionCheckResponse objects
     */
    @PostMapping("/check-batch")
    public ResponseEntity<List<PermissionCheckResponse>> checkPermissionsBatch(
            @Valid @RequestBody List<PermissionCheckRequest> requests) {
        List<PermissionCheckResponse> response = authorizationService.evaluatePermissions(requests);
        return ResponseEntity.ok(response);
    }

    /**
     * Evaluates a permission check via query parameters.
     *
     * @param permissionCode optional permission code (e.g. USER_CREATE or USER:CREATE)
     * @param resource optional resource enum (e.g. USER)
     * @param action optional action enum (e.g. CREATE)
     * @return 200 OK with PermissionCheckResponse
     */
    @GetMapping("/check-permission")
    public ResponseEntity<PermissionCheckResponse> checkPermissionQuery(
            @RequestParam(required = false) String permissionCode,
            @RequestParam(required = false) Resource resource,
            @RequestParam(required = false) Action action) {
        PermissionCheckRequest request = new PermissionCheckRequest(permissionCode, resource, action);
        PermissionCheckResponse response = authorizationService.evaluatePermission(request);
        return ResponseEntity.ok(response);
    }
}
