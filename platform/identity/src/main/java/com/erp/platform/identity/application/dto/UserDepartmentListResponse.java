package com.erp.platform.identity.application.dto;

import com.erp.platform.identity.domain.UserDepartment;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Paginated list response for user department assignments.
 *
 * @param assignments list of user department assignments
 * @param page the current page number
 * @param size the page size
 * @param totalElements the total number of elements
 * @param totalPages the total number of pages
 * @since 1.0.0
 */
public record UserDepartmentListResponse(
        List<UserDepartmentResponse> assignments,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static UserDepartmentListResponse from(Page<UserDepartment> page, List<UserDepartmentResponse> assignments) {
        return new UserDepartmentListResponse(
                assignments,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
