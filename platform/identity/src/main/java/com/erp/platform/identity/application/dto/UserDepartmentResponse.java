package com.erp.platform.identity.application.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for user department assignment.
 *
 * @param userDepartmentId the unique user department assignment identifier
 * @param userId the user ID
 * @param username the username
 * @param departmentId the department ID
 * @param departmentCode the department code
 * @param departmentName the department name
 * @param isPrimary whether this is the primary department for the user
 * @param assignedBy the user or system that assigned the department
 * @param assignedAt the timestamp when the assignment occurred
 * @since 1.0.0
 */
public record UserDepartmentResponse(
        UUID userDepartmentId,
        Long userId,
        String username,
        Long departmentId,
        String departmentCode,
        String departmentName,
        Boolean isPrimary,
        String assignedBy,
        Instant assignedAt
) {
}
