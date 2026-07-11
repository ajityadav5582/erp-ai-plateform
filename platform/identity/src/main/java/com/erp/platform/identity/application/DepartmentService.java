package com.erp.platform.identity.application;

import com.erp.platform.identity.application.dto.CreateDepartmentRequest;
import com.erp.platform.identity.application.dto.DepartmentListResponse;
import com.erp.platform.identity.application.dto.DepartmentResponse;
import com.erp.platform.identity.application.dto.UpdateDepartmentRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for department management operations.
 *
 * @since 1.0.0
 */
public interface DepartmentService {

    /**
     * Create a new department.
     *
     * @param tenantId the tenant ID
     * @param request the create department request
     * @return the created department response
     */
    DepartmentResponse createDepartment(Long tenantId, CreateDepartmentRequest request);

    /**
     * Get a department by its business identifier.
     *
     * @param tenantId the tenant ID
     * @param departmentId the department UUID
     * @return the department response
     */
    DepartmentResponse getDepartmentById(Long tenantId, UUID departmentId);

    /**
     * Get a department by its unique code within a branch.
     *
     * @param tenantId the tenant ID
     * @param branchId the branch ID
     * @param departmentCode the department code
     * @return the department response
     */
    DepartmentResponse getDepartmentByCode(Long tenantId, Long branchId, String departmentCode);

    /**
     * List all departments for a tenant with pagination.
     *
     * @param tenantId the tenant ID
     * @param pageable the pagination parameters
     * @return page of department list responses
     */
    Page<DepartmentListResponse> listDepartments(Long tenantId, Pageable pageable);

    /**
     * List all root departments (departments without a parent) for a branch.
     *
     * @param tenantId the tenant ID
     * @param branchId the branch ID
     * @return list of root department responses
     */
    List<DepartmentListResponse> listRootDepartments(Long tenantId, Long branchId);

    /**
     * List all child departments of a given parent department.
     *
     * @param tenantId the tenant ID
     * @param parentDepartmentId the parent department ID
     * @return list of child department responses
     */
    List<DepartmentListResponse> listChildDepartments(Long tenantId, Long parentDepartmentId);

    /**
     * Update an existing department.
     *
     * @param tenantId the tenant ID
     * @param departmentId the department UUID
     * @param request the update department request
     * @return the updated department response
     */
    DepartmentResponse updateDepartment(Long tenantId, UUID departmentId, UpdateDepartmentRequest request);

    /**
     * Move a department to a new parent (or to root when parent is null).
     *
     * @param tenantId the tenant ID
     * @param departmentId the department UUID
     * @param newParentDepartmentId the new parent department ID, or null to make it a root department
     * @return the moved department response
     */
    DepartmentResponse moveDepartment(Long tenantId, UUID departmentId, Long newParentDepartmentId);

    /**
     * Activate a department (INACTIVE → ACTIVE).
     *
     * @param tenantId the tenant ID
     * @param departmentId the department UUID
     * @return the activated department response
     */
    DepartmentResponse activateDepartment(Long tenantId, UUID departmentId);

    /**
     * Deactivate a department (ACTIVE → INACTIVE).
     *
     * @param tenantId the tenant ID
     * @param departmentId the department UUID
     * @return the deactivated department response
     */
    DepartmentResponse deactivateDepartment(Long tenantId, UUID departmentId);

    /**
     * Delete a department.
     *
     * @param tenantId the tenant ID
     * @param departmentId the department UUID
     */
    void deleteDepartment(Long tenantId, UUID departmentId);
}
