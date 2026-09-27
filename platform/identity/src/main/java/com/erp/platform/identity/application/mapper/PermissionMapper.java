package com.erp.platform.identity.application.mapper;

import com.erp.platform.identity.application.dto.PermissionListResponse;
import com.erp.platform.identity.application.dto.PermissionResponse;
import com.erp.platform.identity.domain.Permission;

import java.util.List;

/**
 * Mapper for Permission entity and DTOs.
 *
 * @since 1.0.0
 */
public class PermissionMapper {

    /**
     * Convert Permission entity to PermissionResponse DTO.
     *
     * @param permission the permission entity
     * @return the permission response DTO
     */
    public static PermissionResponse toResponse(Permission permission) {
        if (permission == null) {
            return null;
        }

        return new PermissionResponse(
                permission.getId(),
                permission.getPermissionCode(),
                permission.getPermissionName(),
                permission.getResourceCode(),
                permission.getActionCode(),
                permission.getDescription(),
                permission.getStatus(),
                permission.getCreatedAt(),
                permission.getUpdatedAt()
        );
    }

    /**
     * Convert list of Permission entities to list of PermissionResponse DTOs.
     *
     * @param permissions the list of permission entities
     * @return the list of permission response DTOs
     */
    public static List<PermissionResponse> toResponseList(List<Permission> permissions) {
        if (permissions == null) {
            return List.of();
        }
        return permissions.stream()
                .map(PermissionMapper::toResponse)
                .toList();
    }

    /**
     * Create PermissionListResponse from page of permissions.
     *
     * @param permissions the list of permission responses
     * @param totalElements the total number of elements
     * @param totalPages the total number of pages
     * @param currentPage the current page number
     * @param pageSize the page size
     * @return the permission list response
     */
    public static PermissionListResponse toListResponse(
            List<PermissionResponse> permissions,
            long totalElements,
            int totalPages,
            int currentPage,
            int pageSize) {
        return PermissionListResponse.of(permissions, totalElements, totalPages, currentPage, pageSize);
    }

}
