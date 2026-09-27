package com.erp.platform.identity.application;

import com.erp.platform.identity.application.dto.CreateUserRequest;
import com.erp.platform.identity.application.dto.UpdateUserRequest;
import com.erp.platform.identity.application.dto.UserListResponse;
import com.erp.platform.identity.application.dto.UserResponse;
import com.erp.platform.identity.domain.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


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
     * @param userId the user ID
     * @param request the update user request
     * @return the updated user response
     */
    UserResponse updateUser(Long tenantId, Long userId, UpdateUserRequest request);

    /**
     * Get user by ID.
     *
     * @param tenantId the tenant ID
     * @param userId the user ID
     * @return the user response
     */
    UserResponse getUserById(Long tenantId, Long userId);

    /**
     * Get user by email.
     *
     * @param tenantId the tenant ID
     * @param email the user email
     * @return the user response
     */
    UserResponse getUserByEmail(Long tenantId, String email);

    /**
     * List all users for a tenant with pagination, search, and status filtering.
     *
     * @param tenantId the tenant ID
     * @param search optional search string (matches username, email, firstName, lastName)
     * @param status optional user status filter
     * @param pageable the pagination parameters
     * @return page of user list responses
     */
    Page<UserListResponse> listUsers(Long tenantId, String search, UserStatus status, Pageable pageable);

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
     * @param userId the user ID
     * @return the activated user response
     */
    UserResponse activateUser(Long tenantId, Long userId);

    /**
     * Deactivate a user.
     *
     * @param tenantId the tenant ID
     * @param userId the user ID
     * @return the deactivated user response
     */
    UserResponse deactivateUser(Long tenantId, Long userId);

    /**
     * Delete a user (soft delete).
     *
     * @param tenantId the tenant ID
     * @param userId the user ID
     */
    void deleteUser(Long tenantId, Long userId);
}
