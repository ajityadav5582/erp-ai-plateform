package com.erp.platform.identity.application;

import com.erp.platform.identity.application.dto.CreateDepartmentRequest;
import com.erp.platform.identity.application.dto.DepartmentListResponse;
import com.erp.platform.identity.application.dto.DepartmentResponse;
import com.erp.platform.identity.application.dto.UpdateDepartmentRequest;
import com.erp.platform.identity.application.mapper.DepartmentMapper;
import com.erp.platform.identity.domain.Department;
import com.erp.platform.identity.domain.exception.DepartmentNotFoundException;
import com.erp.platform.identity.domain.exception.DuplicateDepartmentCodeException;
import com.erp.platform.identity.domain.exception.DuplicateDepartmentNameException;
import com.erp.platform.identity.infrastructure.persistence.DepartmentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Implementation of DepartmentService.
 *
 * @since 1.0.0
 */
@Service
@Transactional
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;

    public DepartmentServiceImpl(DepartmentRepository departmentRepository, DepartmentMapper departmentMapper) {
        this.departmentRepository = departmentRepository;
        this.departmentMapper = departmentMapper;
    }

    @Override
    public DepartmentResponse createDepartment(Long tenantId, CreateDepartmentRequest request) {
        if (departmentRepository.existsByTenantIdAndBranchIdAndDepartmentCode(
                tenantId, request.branchId(), request.departmentCode())) {
            throw new DuplicateDepartmentCodeException(
                "Department with code '" + request.departmentCode()
                    + "' already exists in this branch"
            );
        }
        if (departmentRepository.existsByTenantIdAndBranchIdAndDepartmentName(
                tenantId, request.branchId(), request.departmentName())) {
            throw new DuplicateDepartmentNameException(
                "Department with name '" + request.departmentName()
                    + "' already exists in this branch"
            );
        }

        Department department = departmentMapper.toEntity(request, tenantId);
        Department savedDepartment = departmentRepository.save(department);
        return departmentMapper.toResponse(savedDepartment);
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentResponse getDepartmentById(Long tenantId, UUID departmentId) {
        Department department = departmentRepository.findByTenantIdAndDepartmentId(tenantId, departmentId)
            .orElseThrow(() -> new DepartmentNotFoundException("Department not found with ID: " + departmentId));
        return departmentMapper.toResponse(department);
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentResponse getDepartmentByCode(Long tenantId, Long branchId, String departmentCode) {
        Department department = departmentRepository
            .findByTenantIdAndBranchIdAndDepartmentCode(tenantId, branchId, departmentCode)
            .orElseThrow(() -> new DepartmentNotFoundException(
                "Department not found with code: " + departmentCode
            ));
        return departmentMapper.toResponse(department);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DepartmentListResponse> listDepartments(Long tenantId, Pageable pageable) {
        return departmentRepository.findByTenantId(tenantId, pageable)
            .map(departmentMapper::toListResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentListResponse> listRootDepartments(Long tenantId, Long branchId) {
        return departmentRepository.findByBranchIdAndParentDepartmentIdIsNull(branchId).stream()
            .filter(d -> tenantId.equals(d.getTenantId()))
            .map(departmentMapper::toListResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentListResponse> listChildDepartments(Long tenantId, Long parentDepartmentId) {
        return departmentRepository.findByParentDepartmentId(parentDepartmentId).stream()
            .filter(d -> tenantId.equals(d.getTenantId()))
            .map(departmentMapper::toListResponse)
            .toList();
    }

    @Override
    public DepartmentResponse updateDepartment(Long tenantId, UUID departmentId, UpdateDepartmentRequest request) {
        Department department = departmentRepository.findByTenantIdAndDepartmentId(tenantId, departmentId)
            .orElseThrow(() -> new DepartmentNotFoundException("Department not found with ID: " + departmentId));

        Long branchId = request.branchId() != null ? request.branchId() : department.getBranchId();

        if (request.departmentCode() != null
                && !request.departmentCode().equalsIgnoreCase(department.getDepartmentCode())
                && departmentRepository.existsByTenantIdAndBranchIdAndDepartmentCode(
                    tenantId, branchId, request.departmentCode())) {
            throw new DuplicateDepartmentCodeException(
                "Department with code '" + request.departmentCode()
                    + "' already exists in this branch"
            );
        }
        if (request.departmentName() != null
                && !request.departmentName().equalsIgnoreCase(department.getDepartmentName())
                && departmentRepository.existsByTenantIdAndBranchIdAndDepartmentName(
                    tenantId, branchId, request.departmentName())) {
            throw new DuplicateDepartmentNameException(
                "Department with name '" + request.departmentName()
                    + "' already exists in this branch"
            );
        }

        Department updated = departmentMapper.applyUpdate(department, request);
        Department savedDepartment = departmentRepository.save(updated);
        return departmentMapper.toResponse(savedDepartment);
    }

    @Override
    public DepartmentResponse moveDepartment(Long tenantId, UUID departmentId, Long newParentDepartmentId) {
        Department department = departmentRepository.findByTenantIdAndDepartmentId(tenantId, departmentId)
            .orElseThrow(() -> new DepartmentNotFoundException("Department not found with ID: " + departmentId));

        List<Department> allDepartments = departmentRepository.findByTenantId(tenantId);
        department.moveToParent(newParentDepartmentId, allDepartments);

        Department savedDepartment = departmentRepository.save(department);
        return departmentMapper.toResponse(savedDepartment);
    }

    @Override
    public DepartmentResponse activateDepartment(Long tenantId, UUID departmentId) {
        Department department = departmentRepository.findByTenantIdAndDepartmentId(tenantId, departmentId)
            .orElseThrow(() -> new DepartmentNotFoundException("Department not found with ID: " + departmentId));
        department.activate();
        Department savedDepartment = departmentRepository.save(department);
        return departmentMapper.toResponse(savedDepartment);
    }

    @Override
    public DepartmentResponse deactivateDepartment(Long tenantId, UUID departmentId) {
        Department department = departmentRepository.findByTenantIdAndDepartmentId(tenantId, departmentId)
            .orElseThrow(() -> new DepartmentNotFoundException("Department not found with ID: " + departmentId));
        department.deactivate();
        Department savedDepartment = departmentRepository.save(department);
        return departmentMapper.toResponse(savedDepartment);
    }

    @Override
    public void deleteDepartment(Long tenantId, UUID departmentId) {
        Department department = departmentRepository.findByTenantIdAndDepartmentId(tenantId, departmentId)
            .orElseThrow(() -> new DepartmentNotFoundException("Department not found with ID: " + departmentId));
        departmentRepository.delete(department);
    }
}
