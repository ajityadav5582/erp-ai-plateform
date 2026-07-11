package com.erp.platform.identity.application.mapper;

import com.erp.platform.identity.application.dto.CreateDepartmentRequest;
import com.erp.platform.identity.application.dto.DepartmentListResponse;
import com.erp.platform.identity.application.dto.DepartmentResponse;
import com.erp.platform.identity.application.dto.UpdateDepartmentRequest;
import com.erp.platform.identity.domain.Department;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Mapper for converting between Department entity and DTOs.
 *
 * @since 1.0.0
 */
@Component
public class DepartmentMapper {

    /**
     * Convert CreateDepartmentRequest to Department entity.
     *
     * @param request the create request
     * @param tenantId the tenant ID
     * @return the Department entity
     */
    public Department toEntity(CreateDepartmentRequest request, Long tenantId) {
        Department department = Department.create(
            tenantId, request.branchId(), request.departmentCode(), request.departmentName());
        return department.toBuilder()
            .description(request.description())
            .managerId(request.managerId())
            .parentDepartmentId(request.parentDepartmentId())
            .build();
    }

    /**
     * Convert Department entity to DepartmentResponse.
     *
     * @param department the department entity
     * @return the DepartmentResponse
     */
    public DepartmentResponse toResponse(Department department) {
        return new DepartmentResponse(
            department.getId(),
            department.getDepartmentId(),
            department.getTenantId(),
            department.getBranchId(),
            department.getDepartmentCode(),
            department.getDepartmentName(),
            department.getDescription(),
            department.getManagerId(),
            department.getParentDepartmentId(),
            department.getStatus(),
            toLocalDateTime(department.getCreatedAt()),
            toLocalDateTime(department.getUpdatedAt()),
            department.getCreatedBy(),
            department.getUpdatedBy(),
            department.getVersion()
        );
    }

    /**
     * Convert Department entity to DepartmentListResponse.
     *
     * @param department the department entity
     * @return the DepartmentListResponse
     */
    public DepartmentListResponse toListResponse(Department department) {
        return new DepartmentListResponse(
            department.getId(),
            department.getDepartmentId(),
            department.getDepartmentCode(),
            department.getDepartmentName(),
            department.getBranchId(),
            department.getParentDepartmentId(),
            department.getManagerId(),
            department.getStatus(),
            toLocalDateTime(department.getCreatedAt())
        );
    }

    /**
     * Apply non-null fields from UpdateDepartmentRequest to a Department entity copy.
     *
     * @param department the existing department entity
     * @param request the update request
     * @return a copy of the department with updated fields
     */
    public Department applyUpdate(Department department, UpdateDepartmentRequest request) {
        return department.toBuilder()
            .departmentCode(request.departmentCode() != null
                ? request.departmentCode() : department.getDepartmentCode())
            .departmentName(request.departmentName() != null
                ? request.departmentName() : department.getDepartmentName())
            .description(request.description() != null
                ? request.description() : department.getDescription())
            .branchId(request.branchId() != null
                ? request.branchId() : department.getBranchId())
            .managerId(request.managerId() != null
                ? request.managerId() : department.getManagerId())
            .parentDepartmentId(request.parentDepartmentId() != null
                ? request.parentDepartmentId() : department.getParentDepartmentId())
            .build();
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
