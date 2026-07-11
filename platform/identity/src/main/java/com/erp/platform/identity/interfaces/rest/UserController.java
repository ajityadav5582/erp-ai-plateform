package com.erp.platform.identity.interfaces.rest;

import com.erp.platform.common.tenancy.TenantContext;
import com.erp.platform.identity.application.UserService;
import com.erp.platform.identity.application.dto.CreateUserRequest;
import com.erp.platform.identity.application.dto.UpdateUserRequest;
import com.erp.platform.identity.application.dto.UserListResponse;
import com.erp.platform.identity.application.dto.UserResponse;
import com.erp.platform.identity.domain.UserStatus;
import com.erp.platform.identity.domain.exception.CannotActivateUserException;
import com.erp.platform.identity.domain.exception.CannotDeactivateUserException;
import com.erp.platform.identity.domain.exception.CannotDeleteUserException;
import com.erp.platform.identity.domain.exception.DuplicateEmailException;
import com.erp.platform.identity.domain.exception.UserNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

/**
 * REST controller for user management.
 *
 * <p>Exposes a standard REST API for user CRUD operations and lifecycle state
 * transitions, following the platform API standards:
 * <ul>
 *   <li>Versioned base path {@code /api/v1/users}</li>
 *   <li>Proper HTTP methods (GET, POST, PUT, PATCH, DELETE)</li>
 *   <li>Appropriate status codes (201 + Location on create, 204 on delete)</li>
 *   <li>Pagination and filtering on collection endpoints</li>
 *   <li>Tenant isolation via {@link TenantContext}</li>
 * </ul>
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Creates a new user.
     *
     * @param request the user creation request
     * @return 201 Created with the created user and a {@code Location} header
     */
    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody CreateUserRequest request,
            UriComponentsBuilder uriBuilder) {
        Long tenantId = getTenantId();
        UserResponse response = userService.createUser(tenantId, request);
        return ResponseEntity
                .created(uriBuilder.path("/api/v1/users/{id}").buildAndExpand(response.userId()).toUri())
                .body(response);
    }

    /**
     * Lists all users for the current tenant with pagination.
     *
     * @param pageable pagination and sorting parameters (e.g. {@code ?page=0&size=20&sort=createdAt,desc})
     * @return 200 OK with a page of users
     */
    @GetMapping
    public ResponseEntity<Page<UserListResponse>> listUsers(Pageable pageable) {
        Long tenantId = getTenantId();
        Page<UserListResponse> page = userService.listUsers(tenantId, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * Lists users by branch with pagination.
     *
     * @param branchId the branch ID
     * @param pageable pagination and sorting parameters
     * @return 200 OK with a page of users
     */
    @GetMapping("/by-branch")
    public ResponseEntity<Page<UserListResponse>> listUsersByBranch(
            @RequestParam Long branchId,
            Pageable pageable) {
        Long tenantId = getTenantId();
        Page<UserListResponse> page = userService.listUsersByBranch(tenantId, branchId, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * Lists users by department with pagination.
     *
     * @param departmentId the department ID
     * @param pageable pagination and sorting parameters
     * @return 200 OK with a page of users
     */
    @GetMapping("/by-department")
    public ResponseEntity<Page<UserListResponse>> listUsersByDepartment(
            @RequestParam Long departmentId,
            Pageable pageable) {
        Long tenantId = getTenantId();
        Page<UserListResponse> page = userService.listUsersByDepartment(tenantId, departmentId, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * Gets a user by their UUID.
     *
     * @param userId the user UUID
     * @return 200 OK with the user
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID userId) {
        Long tenantId = getTenantId();
        UserResponse response = userService.getUserById(tenantId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Gets a user by their email address.
     *
     * @param email the user email
     * @return 200 OK with the user
     */
    @GetMapping("/by-email")
    public ResponseEntity<UserResponse> getUserByEmail(@RequestParam String email) {
        Long tenantId = getTenantId();
        UserResponse response = userService.getUserByEmail(tenantId, email);
        return ResponseEntity.ok(response);
    }

    /**
     * Fully updates a user.
     *
     * @param userId the user UUID
     * @param request the update request (only non-null fields are applied)
     * @return 200 OK with the updated user
     */
    @PutMapping("/{userId}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRequest request) {
        Long tenantId = getTenantId();
        UserResponse response = userService.updateUser(tenantId, userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Partially updates a user.
     *
     * @param userId the user UUID
     * @param request the partial update request
     * @return 200 OK with the updated user
     */
    @PatchMapping("/{userId}")
    public ResponseEntity<UserResponse> patchUser(
            @PathVariable UUID userId,
            @RequestBody UpdateUserRequest request) {
        Long tenantId = getTenantId();
        UserResponse response = userService.updateUser(tenantId, userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a user (soft delete).
     *
     * @param userId the user UUID
     * @return 204 No Content
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId) {
        Long tenantId = getTenantId();
        userService.deleteUser(tenantId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Activates a user (INACTIVE → ACTIVE).
     *
     * @param userId the user UUID
     * @return 200 OK with the activated user
     */
    @PostMapping("/{userId}/activate")
    public ResponseEntity<UserResponse> activateUser(@PathVariable UUID userId) {
        Long tenantId = getTenantId();
        UserResponse response = userService.activateUser(tenantId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Deactivates a user (ACTIVE → INACTIVE).
     *
     * @param userId the user UUID
     * @return 200 OK with the deactivated user
     */
    @PostMapping("/{userId}/deactivate")
    public ResponseEntity<UserResponse> deactivateUser(@PathVariable UUID userId) {
        Long tenantId = getTenantId();
        UserResponse response = userService.deactivateUser(tenantId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Extracts the tenant ID from the current request context.
     *
     * @return the tenant ID as a {@link Long}
     * @throws IllegalStateException if the tenant context is not set
     */
    private Long getTenantId() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalStateException("Tenant context is not set");
        }
        return Long.parseLong(tenantId);
    }
}
