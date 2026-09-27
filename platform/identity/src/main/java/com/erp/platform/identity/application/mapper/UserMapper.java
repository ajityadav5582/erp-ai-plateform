package com.erp.platform.identity.application.mapper;

import com.erp.platform.identity.application.dto.CreateUserRequest;
import com.erp.platform.identity.application.dto.UserListResponse;
import com.erp.platform.identity.application.dto.UserResponse;
import com.erp.platform.identity.domain.Role;
import com.erp.platform.identity.domain.User;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Mapper for converting between User entity and DTOs.
 *
 * @since 1.0.0
 */
@Component
public class UserMapper {

    public User toEntity(CreateUserRequest request, Long tenantId, String passwordHash) {
        return User.create(
            tenantId,
            request.username(),
            request.email(),
            passwordHash != null ? passwordHash : "",
            request.firstName(),
            request.lastName(),
            request.phoneNumber(),
            null,
            request.profileImageUrl(),
            request.branchId(),
            request.departmentId()
        );
    }

    public User toEntity(CreateUserRequest request, Long tenantId) {
        return toEntity(request, tenantId, "");
    }

    /**
     * Convert User entity and Role to UserResponse.
     *
     * @param user the user entity
     * @param role the user's role
     * @return the UserResponse
     */
    public UserResponse toResponse(User user, Role role) {
        return new UserResponse(
            user.getId(),
            user.getId(),
            user.getTenantId(),
            user.getUsername(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getFullName(),
            user.getPhoneNumber(),
            role != null ? role.getId() : null,
            role != null ? role.getRoleName() : null,
            role != null ? role.getRoleCode() : null,
            user.getProfileImageUrl(),
            user.getStatus(),
            user.getBranchId(),
            user.getDepartmentId(),
            toLocalDateTime(user.getLastLoginAt()),
            toLocalDateTime(user.getCreatedAt()),
            toLocalDateTime(user.getUpdatedAt()),
            user.getCreatedBy(),
            user.getUpdatedBy(),
            user.getVersion()
        );
    }

    public UserResponse toResponse(User user) {
        return toResponse(user, null);
    }

    /**
     * Convert User entity and Role to UserListResponse.
     *
     * @param user the user entity
     * @param role the user's role
     * @return the UserListResponse
     */
    public UserListResponse toListResponse(User user, Role role) {
        return new UserListResponse(
            user.getId(),
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getFullName(),
            user.getStatus(),
            role != null ? role.getId() : null,
            role != null ? role.getRoleName() : null,
            role != null ? role.getRoleCode() : null,
            user.getBranchId(),
            user.getDepartmentId(),
            toLocalDateTime(user.getLastLoginAt()),
            toLocalDateTime(user.getCreatedAt())
        );
    }

    public UserListResponse toListResponse(User user) {
        return toListResponse(user, null);
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
