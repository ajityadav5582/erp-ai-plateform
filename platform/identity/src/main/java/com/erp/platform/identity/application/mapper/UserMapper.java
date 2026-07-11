package com.erp.platform.identity.application.mapper;

import com.erp.platform.identity.application.dto.CreateUserRequest;
import com.erp.platform.identity.application.dto.UserListResponse;
import com.erp.platform.identity.application.dto.UserResponse;
import com.erp.platform.identity.domain.User;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Mapper for converting between User entity and DTOs.
 *
 * @since 1.0.0
 */
@Component
public class UserMapper {

    /**
     * Convert CreateUserRequest to User entity.
     *
     * @param request the create request
     * @param tenantId the tenant ID
     * @return the User entity
     */
    public User toEntity(CreateUserRequest request, Long tenantId) {
        return User.create(
            UUID.randomUUID(),
            tenantId,
            request.username(),
            request.email(),
            "", // passwordHash - will be set during registration
            request.firstName(),
            request.lastName(),
            request.phoneNumber(),
            request.jobTitle(),
            request.profileImageUrl(),
            request.branchId(),
            request.departmentId()
        );
    }

    /**
     * Convert User entity to UserResponse.
     *
     * @param user the user entity
     * @return the UserResponse
     */
    public UserResponse toResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getUserId(),
            user.getTenantId(),
            user.getUsername(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getFullName(),
            user.getPhoneNumber(),
            user.getJobTitle(),
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

    /**
     * Convert User entity to UserListResponse.
     *
     * @param user the user entity
     * @return the UserListResponse
     */
    public UserListResponse toListResponse(User user) {
        return new UserListResponse(
            user.getId(),
            user.getUserId(),
            user.getUsername(),
            user.getEmail(),
            user.getFullName(),
            user.getStatus(),
            user.getJobTitle(),
            user.getBranchId(),
            user.getDepartmentId(),
            toLocalDateTime(user.getLastLoginAt()),
            toLocalDateTime(user.getCreatedAt())
        );
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
