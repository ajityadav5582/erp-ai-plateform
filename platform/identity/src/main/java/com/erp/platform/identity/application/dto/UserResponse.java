package com.erp.platform.identity.application.dto;

import com.erp.platform.identity.domain.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for user data.
 *
 * @since 1.0.0
 */
public record UserResponse(
    Long id,
    UUID userId,
    Long tenantId,
    String username,
    String email,
    String firstName,
    String lastName,
    String fullName,
    String phoneNumber,
    String jobTitle,
    String profileImageUrl,
    UserStatus status,
    Long branchId,
    Long departmentId,
    LocalDateTime lastLoginAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    String createdBy,
    String updatedBy,
    Integer version
) {
}
