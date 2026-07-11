package com.erp.platform.identity.application;

import com.erp.platform.identity.application.dto.CreateUserRequest;
import com.erp.platform.identity.application.dto.UpdateUserRequest;
import com.erp.platform.identity.application.dto.UserListResponse;
import com.erp.platform.identity.application.dto.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service interface for user management operations.
 *
 * @since 1.0.0
 */
public interface UserService {

    /**
     * Create a new user.
     *
     * @param tenantId the tenant ID
     * @param request the create user request
     * @return the created user response
     */
    UserResponse createUser(Long tenantId, CreateUserRequest request);

    /**
     * Update an existing user.
     *
     * @param tenantId the tenant ID
     * @param userId the user UUID
     * @param request the update user request
     * @return the updated user response
     */
    UserResponse updateUser(Long tenantId, UUID userId, UpdateUserRequest request);

    /**
     * Get user by ID.
     *
     * @param tenantId the tenant ID
     * @param userId the user UUID
     * @return the user response
     */
    UserResponse getUserById(Long tenantId, UUID userId);

    /**
     * Get user by email.
     *
     * @param tenantId the tenant ID
     * @param email the user email
     * @return the user response
     */
    UserResponse getUserByEmail(Long tenantId, String email);

    /**
     * List all users for a tenant with pagination.
     *
     * @param tenantId the tenant ID
     * @param pageable the pagination parameters
     * @return page of user list responses
     */
    Page<UserListResponse> listUsers(Long tenantId, Pageable pageable);

    /**
     * List users by branch with pagination.
     *
     * @param tenantId the tenant ID
     * @param branchId the branch ID
     * @param pageable the pagination parameters
     * @return page of user list responses
     */
    Page<UserListResponse> listUsersByBranch(Long tenantId, Long branchId, Pageable pageable);

    /**
     * List users by department with pagination.
     *
     * @param tenantId the tenant ID
     * @param departmentId the department ID
     * @param pageable the pagination parameters
     * @return page of user list responses
     */
    Page<UserListResponse> listUsersByDepartment(Long tenantId, Long departmentId, Pageable pageable);

    /**
     * Activate a user.
     *
     * @param tenantId the tenant ID
     * @param userId the user UUID
     * @return the activated user response
     */
    UserResponse activateUser(Long tenantId, UUID userId);

    /**
     * Deactivate a user.
     *
     * @param tenantId the tenant ID
     * @param userId the user UUID
     * @return the deactivated user response
     */
    UserResponse deactivateUser(Long tenantId, UUID userId);

    /**
     * Delete a user (soft delete).
     *
     * @param tenantId the tenant ID
     * @param userId the user UUID
     */
    void deleteUser(Long tenantId, UUID userId);
}
