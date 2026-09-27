package com.erp.platform.identity.application.dto;

import com.erp.platform.identity.domain.UserStatus;

import java.time.LocalDateTime;

/**
 * Response DTO for user list data (summary view).
 *
 * @since 1.0.0
 */
public record UserListResponse(
    Long id,
    Long userId,
    String username,
    String email,
    String fullName,
    UserStatus status,
    Long roleId,
    String roleName,
    String roleCode,
    Long branchId,
    Long departmentId,
    LocalDateTime lastLoginAt,
    LocalDateTime createdAt
) {
}
