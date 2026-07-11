package com.erp.platform.identity.application.dto;

import com.erp.platform.identity.domain.Action;
import com.erp.platform.identity.domain.PermissionStatus;
import com.erp.platform.identity.domain.Resource;

import java.util.List;

/**
 * Response DTO for paginated permission list.
 *
 * @since 1.0.0
 */
public record PermissionListResponse(
    List<PermissionResponse> permissions,
    long totalElements,
    int totalPages,
    int currentPage,
    int pageSize
) {
    public static PermissionListResponse of(List<PermissionResponse> permissions, long totalElements, int totalPages, int currentPage, int pageSize) {
        return new PermissionListResponse(permissions, totalElements, totalPages, currentPage, pageSize);
    }
}
