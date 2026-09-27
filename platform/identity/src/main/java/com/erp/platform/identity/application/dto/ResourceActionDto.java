package com.erp.platform.identity.application.dto;

import com.erp.platform.identity.domain.Action;
import com.erp.platform.identity.domain.Resource;

/**
 * Structured DTO representing a Resource + Action permission pair.
 *
 * @param resource the resource enum
 * @param action the action enum
 * @param permissionCode the permission code (RESOURCE_ACTION)
 *
 * @since 1.0.0
 */
public record ResourceActionDto(
        Resource resource,
        Action action,
        String permissionCode
) {
}
