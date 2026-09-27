package com.erp.platform.identity.application.dto;

import com.erp.platform.identity.domain.Action;
import com.erp.platform.identity.domain.Resource;

/**
 * Request DTO for single or batch permission validation.
 *
 * <p>Supports checking by {@code permissionCode} (e.g. "USER_CREATE" or "USER:CREATE")
 * or explicitly by {@code resource} + {@code action}.
 *
 * @param permissionCode optional permission code string
 * @param resource optional resource enum
 * @param action optional action enum
 *
 * @since 1.0.0
 */
public record PermissionCheckRequest(
        String permissionCode,
        Resource resource,
        Action action
) {
}
