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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

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

    public RolePermissionServiceImpl(
            RolePermissionRepository rolePermissionRepository,
            RoleRepository roleRepository,
            PermissionRepository permissionRepository) {
        this.rolePermissionRepository = rolePermissionRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @Override
    @Transactional
    public RolePermissionResponse assignPermission(AssignPermissionRequest request) {
        // Validate role exists
        if (!roleRepository.existsById(request.roleId())) {
            throw RoleNotFoundException.byRoleId(request.roleId());
        }

        // Validate permission exists
        Permission permission = permissionRepository.findById(request.permissionId())
                .orElseThrow(() -> PermissionNotFoundException.byId(request.permissionId()));

        // Check if already assigned
        if (rolePermissionRepository.existsByRoleIdAndPermissionId(request.roleId(), request.permissionId())) {
            throw new PermissionOperationException(
                    "Permission is already assigned to this role");
        }

        // Create the assignment
        RolePermission rolePermission = RolePermission.assign(
                UUID.randomUUID(),
                request.roleId(),
                request.permissionId(),
                request.assignedBy(),
                Instant.now()
        );

        RolePermission saved = rolePermissionRepository.save(rolePermission);

        return RolePermissionMapper.toResponse(saved, permission);
    }

    @Override
    @Transactional
    public void removePermission(RemovePermissionRequest request) {
        // Validate the assignment exists
        if (!rolePermissionRepository.existsByRoleIdAndPermissionId(request.roleId(), request.permissionId())) {
            throw new PermissionOperationException(
                    "Permission is not assigned to this role");
        }

        rolePermissionRepository.deleteByRoleIdAndPermissionId(request.roleId(), request.permissionId());
    }

    @Override
    @Transactional(readOnly = true)
    public RolePermissionListResponse findPermissionsByRoleId(Long roleId, int page, int size) {
        // Validate role exists
        if (!roleRepository.existsById(roleId)) {
            throw RoleNotFoundException.byRoleId(roleId);
        }

        Pageable pageable = PageRequest.of(page, size, DEFAULT_SORT);
        Page<RolePermission> rolePermissions = rolePermissionRepository.findByRoleId(roleId, pageable);

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
        return rolePermissionRepository.existsByRoleIdAndPermissionId(roleId, permissionId);
    }

    @Override
    @Transactional(readOnly = true)
    public RolePermissionResponse findByRolePermissionId(UUID rolePermissionId) {
        return rolePermissionRepository.findByRolePermissionId(rolePermissionId)
                .map(rp -> {
                    Permission permission = permissionRepository.findById(rp.getPermissionId()).orElse(null);
                    return RolePermissionMapper.toResponse(rp, permission);
                })
                .orElse(null);
    }
}
