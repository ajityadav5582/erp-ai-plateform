package com.erp.platform.identity.application.mapper;

import com.erp.platform.identity.application.dto.UserRoleListResponse;
import com.erp.platform.identity.application.dto.UserRoleResponse;
import com.erp.platform.identity.domain.Role;
import com.erp.platform.identity.domain.UserRole;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Mapper for converting between UserRole entity and DTOs.
 *
 * @since 1.0.0
 */
@Component
public class UserRoleMapper {

    /**
     * Convert UserRole entity to UserRoleResponse.
     *
     * @param userRole the user role entity
     * @param role the role entity (for role name and code)
     * @return the UserRoleResponse
     */
    public UserRoleResponse toResponse(UserRole userRole, Role role) {
        return new UserRoleResponse(
                userRole.getId(),
                userRole.getUserRoleId(),
                userRole.getUserId(),
                userRole.getRoleId(),
                role != null ? role.getRoleName() : null,
                role != null ? role.getRoleCode() : null,
                userRole.getTenantId(),
                userRole.getAssignedBy(),
                toLocalDateTime(userRole.getAssignedAt()),
                toLocalDateTime(userRole.getExpiresAt()),
                userRole.isPrimaryRole(),
                toLocalDateTime(userRole.getRevokedAt()),
                userRole.getRevokedBy(),
                userRole.getRevokeReason(),
                userRole.getVersion()
        );
    }

    /**
     * Convert list of UserRole entities to list of UserRoleResponse DTOs.
     *
     * @param userRoles the list of user role entities
     * @return the list of user role responses
     */
    public List<UserRoleResponse> toResponseList(List<UserRole> userRoles) {
        return userRoles.stream()
                .map(userRole -> toResponse(userRole, null))
                .toList();
    }

    /**
     * Create UserRoleListResponse from page of user roles.
     *
     * @param userRoles the list of user role responses
     * @param totalElements the total number of elements
     * @param totalPages the total number of pages
     * @param currentPage the current page number
     * @param pageSize the page size
     * @return the user role list response
     */
    public UserRoleListResponse toListResponse(
            List<UserRoleResponse> userRoles,
            long totalElements,
            int totalPages,
            int currentPage,
            int pageSize) {
        return UserRoleListResponse.of(userRoles, totalElements, totalPages, currentPage, pageSize);
    }

    /**
     * Convert Instant to LocalDateTime.
     *
     * @param instant the instant to convert
     * @return the local date time
     */
    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant != null ? LocalDateTime.ofInstant(instant, ZoneId.systemDefault()) : null;
    }
}
