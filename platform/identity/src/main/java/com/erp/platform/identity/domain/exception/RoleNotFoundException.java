package com.erp.platform.identity.domain.exception;

import java.util.UUID;

/**
 * Exception thrown when a role is not found.
 *
 * @since 1.0.0
 */
public class RoleNotFoundException extends RoleOperationException {

    private static final long serialVersionUID = 1L;

    public RoleNotFoundException(String message) {
        super(message);
    }

    /**
     * Creates a RoleNotFoundException for a specific role ID.
     *
     * @param roleId the role ID that was not found
     * @return the exception
     * @since 1.0.0
     */
    public static RoleNotFoundException byRoleId(Long roleId) {
        return new RoleNotFoundException("Role not found with ID: " + roleId);
    }

    /**
     * Creates a RoleNotFoundException for a specific role UUID.
     *
     * @param roleUuid the role UUID that was not found
     * @return the exception
     * @since 1.0.0
     */
    public static RoleNotFoundException byRoleUuid(UUID roleUuid) {
        return new RoleNotFoundException("Role not found with UUID: " + roleUuid);
    }
}
