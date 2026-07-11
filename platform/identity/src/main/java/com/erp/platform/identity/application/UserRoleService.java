package com.erp.platform.identity.application;

import com.erp.platform.identity.application.dto.AssignRoleRequest;
import com.erp.platform.identity.application.dto.RemoveRoleRequest;
import com.erp.platform.identity.application.dto.UserRoleListResponse;
import com.erp.platform.identity.application.dto.UserRoleResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for UserRole assignment operations.
 *
 * <p>Manages the many-to-many relationship between User and Role
 * with additional assignment metadata such as expiration, primary role,
 * and revocation tracking.
 *
 * @since 1.0.0
 */
public interface UserRoleService {

    /**
     * Assign a role to a user.
     *
     * @param request the assign role request
     * @return the created user role response
     */
    UserRoleResponse assignRole(AssignRoleRequest request);

    /**
     * Remove a role from a user (revoke the assignment).
     *
     * @param request the remove role request
     */
    void removeRole(RemoveRoleRequest request);

    /**
     * Get a user role assignment by user role ID.
     *
     * @param userRoleId the user role ID
     * @return the user role response
     */
    UserRoleResponse getUserRoleById(UUID userRoleId);

    /**
     * List all role assignments for a user.
     *
     * @param userId the user ID
     * @param pageable the pagination information
     * @return the paginated user role list response
     */
    UserRoleListResponse listUserRoles(Long userId, Pageable pageable);

    /**
     * List all user assignments for a role.
     *
     * @param roleId the role ID
     * @param pageable the pagination information
     * @return the paginated user role list response
     */
    UserRoleListResponse listRoleUsers(Long roleId, Pageable pageable);

    /**
     * Get the primary role for a user.
     *
     * @param userId the user ID
     * @return the primary user role response, or null if no primary role is set
     */
    UserRoleResponse getPrimaryRole(Long userId);

    /**
     * Check if a user has a specific role assigned and active.
     *
     * @param userId the user ID
     * @param roleId the role ID
     * @return true if the user has the role assigned and active, false otherwise
     */
    boolean hasRole(Long userId, Long roleId);

    /**
     * List all active roles for a user.
     *
     * @param userId the user ID
     * @return list of active user role responses
     */
    List<UserRoleResponse> listActiveRoles(Long userId);
}
