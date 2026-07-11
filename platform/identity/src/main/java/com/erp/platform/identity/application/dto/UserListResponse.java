package com.erp.platform.identity.application.dto;

import com.erp.platform.identity.domain.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for user list data (summary view).
 *
 * @since 1.0.0
 */
public record UserListResponse(
    Long id,
    UUID userId,
    String username,
    String email,
    String fullName,
    UserStatus status,
    String jobTitle,
    Long branchId,
    Long departmentId,
    LocalDateTime lastLoginAt,
    LocalDateTime createdAt
) {
}
