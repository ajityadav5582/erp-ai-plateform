package com.erp.platform.identity.application.dto;

import java.util.List;

/**
 * Response DTO for the currently authenticated user (/api/v1/auth/me).
 *
 * @param id          the user's numeric ID
 * @param tenantId    the tenant ID
 * @param username    the user's username
 * @param email       the user's email
 * @param fullName    the user's full name
 * @param roles       the user's active role codes
 * @param permissions the user's active tenant permission codes
 *
 * @since 1.0.0
 */
public record CurrentUserResponse(
        Long id,
        Long tenantId,
        String username,
        String email,
        String fullName,
        List<String> roles,
        List<String> permissions
) {
    public CurrentUserResponse(Long id, Long tenantId, String username, String email, String fullName, List<String> roles) {
        this(id, tenantId, username, email, fullName, roles, List.of());
    }
}
