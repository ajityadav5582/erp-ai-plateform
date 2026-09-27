package com.erp.platform.identity.application.dto;

import java.util.List;
import java.util.Set;

/**
 * DTO containing all tenant-scoped active permissions of a user.
 *
 * <p>Used by UI permission managers to configure client-side route guards,
 * button visibility, and feature access control.
 *
 * @param userId the user's numeric ID
 * @param tenantId the tenant ID
 * @param roles list of active role codes
 * @param permissionCodes set of active permission code strings
 * @param resourceActions list of structured Resource + Action permission pairs
 *
 * @since 1.0.0
 */
public record UserPermissionsResponse(
        Long userId,
        Long tenantId,
        List<String> roles,
        Set<String> permissionCodes,
        List<ResourceActionDto> resourceActions
) {
}
