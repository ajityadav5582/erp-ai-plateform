package com.erp.platform.identity.interfaces.rest;

import com.erp.platform.identity.application.UserRoleService;
import com.erp.platform.identity.application.dto.AssignRoleRequest;
import com.erp.platform.identity.application.dto.RemoveRoleRequest;
import com.erp.platform.identity.application.dto.UserRoleListResponse;
import com.erp.platform.identity.application.dto.UserRoleResponse;
import com.erp.platform.identity.domain.exception.RoleNotFoundException;
import com.erp.platform.identity.domain.exception.UserNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

/**
 * REST controller for user-role assignment management.
 *
 * <p>Exposes a standard REST API for managing the many-to-many relationship
 * between users and roles, following the platform API standards:
 * <ul>
 *   <li>Versioned base path {@code /api/v1/user-roles}</li>
 *   <li>Proper HTTP methods (GET, POST, DELETE)</li>
 *   <li>Appropriate status codes (201 + Location on create, 204 on delete)</li>
 *   <li>Pagination on collection endpoints</li>
 * </ul>
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/user-roles")
@RequiredArgsConstructor
public class UserRoleController {

    private final UserRoleService userRoleService;

    /**
     * Assigns a role to a user.
     *
     * @param request the role assignment request
     * @return 201 Created with the created assignment and a {@code Location} header
     */
    @PostMapping
    public ResponseEntity<UserRoleResponse> assignRole(
            @Valid @RequestBody AssignRoleRequest request,
            UriComponentsBuilder uriBuilder) {
        UserRoleResponse response = userRoleService.assignRole(request);
        return ResponseEntity
                .created(uriBuilder.path("/api/v1/user-roles/{id}").buildAndExpand(response.userRoleId()).toUri())
                .body(response);
    }

    /**
     * Removes a role from a user.
     *
     * @param request the role removal request
     * @return 204 No Content
     */
    @DeleteMapping
    public ResponseEntity<Void> removeRole(@Valid @RequestBody RemoveRoleRequest request) {
        userRoleService.removeRole(request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Gets a user role assignment by its business identifier.
     *
     * @param userRoleId the user role assignment UUID
     * @return 200 OK with the assignment
     */
    @GetMapping("/{userRoleId}")
    public ResponseEntity<UserRoleResponse> getUserRoleById(@PathVariable UUID userRoleId) {
        UserRoleResponse response = userRoleService.getUserRoleById(userRoleId);
        return ResponseEntity.ok(response);
    }

    /**
     * Lists all role assignments for a user.
     *
     * @param userId the user ID
     * @param pageable pagination parameters
     * @return 200 OK with a page of assignments
     */
    @GetMapping("/by-user")
    public ResponseEntity<UserRoleListResponse> listUserRoles(
            @RequestParam Long userId,
            Pageable pageable) {
        UserRoleListResponse response = userRoleService.listUserRoles(userId, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Lists all user assignments for a role.
     *
     * @param roleId the role ID
     * @param pageable pagination parameters
     * @return 200 OK with a page of assignments
     */
    @GetMapping("/by-role")
    public ResponseEntity<UserRoleListResponse> listRoleUsers(
            @RequestParam Long roleId,
            Pageable pageable) {
        UserRoleListResponse response = userRoleService.listRoleUsers(roleId, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Gets the primary role for a user.
     *
     * @param userId the user ID
     * @return 200 OK with the primary role assignment, or 404 if none
     */
    @GetMapping("/primary")
    public ResponseEntity<UserRoleResponse> getPrimaryRole(@RequestParam Long userId) {
        UserRoleResponse response = userRoleService.getPrimaryRole(userId);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Checks if a user has a specific role.
     *
     * @param userId the user ID
     * @param roleId the role ID
     * @return 200 OK with true/false
     */
    @GetMapping("/has-role")
    public ResponseEntity<Boolean> hasRole(
            @RequestParam Long userId,
            @RequestParam Long roleId) {
        boolean hasRole = userRoleService.hasRole(userId, roleId);
        return ResponseEntity.ok(hasRole);
    }

    /**
     * Lists all active roles for a user.
     *
     * @param userId the user ID
     * @return 200 OK with the list of active roles
     */
    @GetMapping("/active")
    public ResponseEntity<UserRoleListResponse> listActiveRoles(@RequestParam Long userId) {
        // This is a simplified endpoint - returns active roles as a list response
        UserRoleListResponse response = userRoleService.listUserRoles(userId, Pageable.unpaged());
        return ResponseEntity.ok(response);
    }
}
