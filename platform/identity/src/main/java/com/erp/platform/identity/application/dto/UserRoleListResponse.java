package com.erp.platform.identity.application.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Response DTO for paginated list of user role assignments.
 *
 * @since 1.0.0
 */
public record UserRoleListResponse(
        List<UserRoleResponse> userRoles,
        long totalElements,
        int totalPages,
        int currentPage,
        int pageSize
) {

    /**
     * Create a UserRoleListResponse from a page of UserRoleResponse.
     *
     * @param userRoles the list of user role responses
     * @param page the page information
     * @return the user role list response
     */
    public static UserRoleListResponse of(List<UserRoleResponse> userRoles, Page<?> page) {
        return new UserRoleListResponse(
                userRoles,
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize()
        );
    }

    /**
     * Create a UserRoleListResponse from individual pagination parameters.
     *
     * @param userRoles the list of user role responses
     * @param totalElements the total number of elements
     * @param totalPages the total number of pages
     * @param currentPage the current page number
     * @param pageSize the page size
     * @return the user role list response
     */
    public static UserRoleListResponse of(
            List<UserRoleResponse> userRoles,
            long totalElements,
            int totalPages,
            int currentPage,
            int pageSize) {
        return new UserRoleListResponse(
                userRoles,
                totalElements,
                totalPages,
                currentPage,
                pageSize
        );
    }
}
