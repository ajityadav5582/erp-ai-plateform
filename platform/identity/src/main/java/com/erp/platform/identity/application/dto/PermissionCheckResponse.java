package com.erp.platform.identity.application.dto;

import com.erp.platform.identity.domain.Action;
import com.erp.platform.identity.domain.Resource;

/**
 * Response DTO representing permission authorization evaluation outcome.
 *
 * @param permissionCode the evaluated permission code
 * @param resource the evaluated resource, if applicable
 * @param action the evaluated action, if applicable
 * @param granted whether access is granted for the given permission
 *
 * @since 1.0.0
 */
public record PermissionCheckResponse(
        String permissionCode,
        Resource resource,
        Action action,
        boolean granted
) {
    public static PermissionCheckResponse granted(String permissionCode, Resource resource, Action action) {
        return new PermissionCheckResponse(permissionCode, resource, action, true);
    }

    public static PermissionCheckResponse denied(String permissionCode, Resource resource, Action action) {
        return new PermissionCheckResponse(permissionCode, resource, action, false);
    }
}
