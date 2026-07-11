package com.erp.platform.identity.application.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Paginated list response for role permissions.
 *
 * @since 1.0.0
 */
public record RolePermissionListResponse(
        List<RolePermissionResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static RolePermissionListResponse of(Page<RolePermissionResponse> page) {
        return new RolePermissionListResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
