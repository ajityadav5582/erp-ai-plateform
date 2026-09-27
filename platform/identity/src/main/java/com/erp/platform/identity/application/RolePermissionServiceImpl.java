package com.erp.platform.identity.application;

import com.erp.platform.identity.application.dto.AssignPermissionRequest;
import com.erp.platform.identity.application.dto.RemovePermissionRequest;
import com.erp.platform.identity.application.dto.RolePermissionListResponse;
import com.erp.platform.identity.application.dto.RolePermissionResponse;
import com.erp.platform.identity.application.mapper.RolePermissionMapper;
import com.erp.platform.identity.domain.Permission;
import com.erp.platform.identity.domain.RolePermission;
import com.erp.platform.identity.domain.exception.PermissionNotFoundException;
import com.erp.platform.identity.domain.exception.PermissionOperationException;
import com.erp.platform.identity.domain.exception.RoleNotFoundException;
import com.erp.platform.identity.infrastructure.persistence.PermissionRepository;
import com.erp.platform.identity.infrastructure.persistence.RolePermissionRepository;
import com.erp.platform.identity.infrastructure.persistence.RoleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service implementation for managing role-permission assignments.
 *
 * <p>Handles the business logic for assigning and removing permissions from roles,
 * as well as querying role permissions.
 *
 * @since 1.0.0
 */
@Service
@Transactional(readOnly = true)
public class RolePermissionServiceImpl implements RolePermissionService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final String DEFAULT_SORT_BY = "assignedAt";
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, DEFAULT_SORT_BY);

    private final RolePermissionRepository rolePermissionRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final CurrentTenantProvider currentTenantProvider;

    public RolePermissionServiceImpl(
            RolePermissionRepository rolePermissionRepository,
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            CurrentTenantProvider currentTenantProvider) {
        this.rolePermissionRepository = rolePermissionRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.currentTenantProvider = currentTenantProvider;
    }

    @Override
    @Transactional
    public RolePermissionResponse assignPermission(AssignPermissionRequest request) {

        Long tenantId = currentTenantProvider.getCurrentTenantId();

        // Validate role exists and belongs to current tenant
        roleRepository.findByIdAndTenantId(request.roleId(), tenantId)
                .orElseThrow(() -> RoleNotFoundException.byRoleId(request.roleId()));

        // Validate permission exists
        Permission permission = permissionRepository.findById(request.permissionId())
                .orElseThrow(() -> PermissionNotFoundException.byId(request.permissionId()));

        // New assignments may only use active global permissions
        if (!permission.isActive()) {
            throw new PermissionOperationException(
                    "Only active permissions can be assigned to roles");
        }

        // Check if already assigned within the tenant
        if (rolePermissionRepository.existsByRoleIdAndTenantIdAndPermissionId(
                request.roleId(), tenantId, request.permissionId())) {
            throw new PermissionOperationException(
                    "Permission is already assigned to this role");
        }

        // Idempotent insert: uses database-level ON CONFLICT DO NOTHING
        int inserted = rolePermissionRepository.insertOnConflictDoNothing(
                request.roleId(),
                request.permissionId(),
                request.assignedBy(),
                LocalDateTime.now()
        );

        if (inserted == 0) {
            throw new PermissionOperationException(
                    "Permission is already assigned to this role");
        }

        RolePermission saved = rolePermissionRepository.findByTenantIdRoleIdAndPermissionId(
                tenantId, request.roleId(), request.permissionId())
                .orElseThrow(() -> new PermissionOperationException(
                        "Failed to assign permission to role"));

        return RolePermissionMapper.toResponse(saved, permission);
    }

    @Override
    @Transactional
    public void removePermission(RemovePermissionRequest request) {

        Long tenantId = currentTenantProvider.getCurrentTenantId();

        // Validate role exists and belongs to current tenant
        if (!roleRepository.findByIdAndTenantId(request.roleId(), tenantId).isPresent()) {
            throw RoleNotFoundException.byRoleId(request.roleId());
        }

        // Validate the assignment exists within the tenant
        if (!rolePermissionRepository.existsByRoleIdAndTenantIdAndPermissionId(
                request.roleId(), tenantId, request.permissionId())) {
            throw new PermissionOperationException(
                    "Permission is not assigned to this role");
        }

        rolePermissionRepository.deleteByRoleIdAndTenantIdAndPermissionId(
                request.roleId(), tenantId, request.permissionId());
    }

    @Override
    @Transactional(readOnly = true)
    public RolePermissionListResponse findPermissionsByRoleId(Long roleId, int page, int size) {

        Long tenantId = currentTenantProvider.getCurrentTenantId();

        // Validate role exists and belongs to current tenant
        if (!roleRepository.findByIdAndTenantId(roleId, tenantId).isPresent()) {
            throw RoleNotFoundException.byRoleId(roleId);
        }

        Pageable pageable = PageRequest.of(page, size, DEFAULT_SORT);
        Page<RolePermission> rolePermissions = rolePermissionRepository.findByRoleIdAndTenantId(roleId, tenantId, pageable);

        List<RolePermissionResponse> responses = rolePermissions.getContent().stream()
                .map(rp -> {
                    Permission permission = permissionRepository.findById(rp.getPermissionId()).orElse(null);
                    return RolePermissionMapper.toResponse(rp, permission);
                })
                .toList();

        return new RolePermissionListResponse(
                responses,
                rolePermissions.getNumber(),
                rolePermissions.getSize(),
                rolePermissions.getTotalElements(),
                rolePermissions.getTotalPages(),
                rolePermissions.isFirst(),
                rolePermissions.isLast()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasPermission(Long roleId, Long permissionId) {

        Long tenantId = currentTenantProvider.getCurrentTenantId();

        // Validate role exists and belongs to current tenant
        if (!roleRepository.findByIdAndTenantId(roleId, tenantId).isPresent()) {
            return false;
        }

        return rolePermissionRepository.existsByRoleIdAndTenantIdAndPermissionId(roleId, tenantId, permissionId);
    }

    @Override
    @Transactional(readOnly = true)
    public RolePermissionResponse findById(Long id) {

        Long tenantId = currentTenantProvider.getCurrentTenantId();
        return rolePermissionRepository.findByIdAndTenantId(id, tenantId)
                .map(rp -> {
                    Permission permission = permissionRepository.findById(rp.getPermissionId()).orElse(null);
                    return RolePermissionMapper.toResponse(rp, permission);
                })
                .orElse(null);
    }
}
