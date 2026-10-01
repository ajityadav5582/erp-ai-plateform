package com.erp.business.inventory.categories.dto;

import java.time.LocalDateTime;

/**
 * Lightweight response DTO for category list views.
 *
 * @since 1.0.0
 */
public record CategoryListResponse(
        Long id,
        Long tenantId,
        Long parentId,
        String parentName,
        String name,
        String slug,
        Boolean isActive,
        LocalDateTime createdAt
) {
}
