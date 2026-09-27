package com.erp.platform.identity.interfaces.rest;

import com.erp.platform.identity.application.UserRoleService;
import com.erp.platform.identity.application.dto.AssignRoleRequest;
import com.erp.platform.identity.application.dto.RemoveRoleRequest;
import com.erp.platform.identity.application.dto.UserRoleListResponse;
import com.erp.platform.identity.application.dto.UserRoleResponse;
import com.erp.platform.identity.application.security.RequirePermission;
import com.erp.platform.identity.domain.exception.RoleNotFoundException;
import com.erp.platform.identity.domain.exception.UserNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

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
    @RequirePermission("USER_ROLE_CREATE")
    public ResponseEntity<UserRoleResponse> assignRole(
            @Valid @RequestBody AssignRoleRequest request,
            UriComponentsBuilder uriBuilder) {
        UserRoleResponse response = userRoleService.assignRole(request);
        return ResponseEntity
                .created(uriBuilder.path("/api/v1/user-roles/{id}").buildAndExpand(response.id()).toUri())
                .body(response);
    }

    /**
     * Removes a role from a user.
     *
     * @param request the role removal request
     * @return 204 No Content
     */
    @DeleteMapping
    @RequirePermission("USER_ROLE_DELETE")
    public ResponseEntity<Void> removeRole(@Valid @RequestBody RemoveRoleRequest request) {
        userRoleService.removeRole(request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Gets a user role assignment by its identifier.
     *
     * @param id the user role assignment identifier
     * @return 200 OK with the assignment
     */
    @GetMapping("/{id}")
    @RequirePermission("USER_ROLE_READ")
    public ResponseEntity<UserRoleResponse> getUserRoleById(@PathVariable Long id) {
        UserRoleResponse response = userRoleService.getUserRoleById(id);
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
    @RequirePermission("USER_ROLE_READ")
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
    @RequirePermission("USER_ROLE_READ")
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
    @RequirePermission("USER_ROLE_READ")
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
    @RequirePermission("USER_ROLE_READ")
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
    @RequirePermission("USER_ROLE_READ")
    public ResponseEntity<UserRoleListResponse> listActiveRoles(@RequestParam Long userId) {
        var activeRoles = userRoleService.listActiveRoles(userId);
        UserRoleListResponse response = UserRoleListResponse.of(
                activeRoles,
                activeRoles.size(),
                1,
                0,
                activeRoles.size()
        );
        return ResponseEntity.ok(response);
    }
}
