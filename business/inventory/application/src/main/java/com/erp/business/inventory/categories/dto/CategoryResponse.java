package com.erp.business.inventory.categories.dto;

import java.time.LocalDateTime;

/**
 * Response DTO for category data, including parent category information.
 *
 * @since 1.0.0
 */
public record CategoryResponse(
        Long id,
        Long tenantId,
        Long companyId,
        Long parentId,
        String name,
        String slug,
        String description,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String createdBy,
        String updatedBy,
        Integer version
) {
}
