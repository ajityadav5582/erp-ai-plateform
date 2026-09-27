package com.erp.platform.identity.application;

import com.erp.platform.identity.application.dto.CreateRoleRequest;
import com.erp.platform.identity.application.dto.RoleListResponse;
import com.erp.platform.identity.application.dto.RoleResponse;
import com.erp.platform.identity.application.dto.UpdateRoleRequest;
import com.erp.platform.identity.application.mapper.RoleMapper;
import com.erp.platform.identity.domain.Role;
import com.erp.platform.identity.domain.exception.CannotDeleteRoleException;
import com.erp.platform.identity.domain.exception.DuplicateRoleCodeException;
import com.erp.platform.identity.domain.exception.RoleNotFoundException;
import com.erp.platform.identity.domain.exception.UnauthorizedException;
import com.erp.platform.identity.domain.Permission;
import com.erp.platform.identity.infrastructure.persistence.PermissionRepository;
import com.erp.platform.identity.infrastructure.persistence.RolePermissionRepository;
import com.erp.platform.identity.infrastructure.persistence.RoleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Implementation of RoleService.
 *
 * @since 1.0.0
 */
@Service
@Transactional
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;
    private final CurrentTenantProvider currentTenantProvider;
    private final AuthorizationService authorizationService;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public RoleServiceImpl(RoleRepository roleRepository,
                           RoleMapper roleMapper,
                           CurrentTenantProvider currentTenantProvider,
                           AuthorizationService authorizationService,
                           PermissionRepository permissionRepository,
                           RolePermissionRepository rolePermissionRepository) {
        this.roleRepository = roleRepository;
        this.roleMapper = roleMapper;
        this.currentTenantProvider = currentTenantProvider;
        this.authorizationService = authorizationService;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    @Override
    public RoleResponse createRole(Long tenantId, CreateRoleRequest request) {
        // Only SUPER_ADMIN, OWNER, TENANT_ADMIN, or ADMIN can create roles
        authorizationService.requireAnyAdmin();

        Long currentTenantId = currentTenantProvider.getCurrentTenantId();
        if (currentTenantId == null || !currentTenantId.equals(tenantId)) {
            throw new UnauthorizedException("Cannot create a role outside the current tenant");
        }

        // Check for duplicate role code within tenant
        if (roleRepository.existsByTenantIdAndRoleCode(tenantId, request.roleCode())) {
            throw new DuplicateRoleCodeException(
                "Role with code '" + request.roleCode() + "' already exists in this tenant"
            );
        }

        // Create role entity
        Role role = roleMapper.toEntity(request, tenantId);

        // Save role
        Role savedRole = roleRepository.save(role);

        // Assign initial permissions if specified
        if (request.permissionIds() != null && !request.permissionIds().isEmpty()) {
            List<Permission> permissions = permissionRepository.findAllById(request.permissionIds());
            for (Permission perm : permissions) {
                if (perm.isActive()) {
                    rolePermissionRepository.insertOnConflictDoNothing(
                        savedRole.getId(),
                        perm.getId(),
                        "tenant_admin",
                        LocalDateTime.now()
                    );
                }
            }
        }

        Set<Long> permIds = new HashSet<>(rolePermissionRepository.findPermissionIdsByRoleIdAndTenantId(savedRole.getId(), tenantId));
        return roleMapper.toResponse(savedRole, permIds);
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse getRoleById(Long roleId) {
        Long tenantId = currentTenantProvider.getCurrentTenantId();
        Role role = roleRepository.findByIdAndTenantId(roleId, tenantId)
            .orElseThrow(() -> new RoleNotFoundException(
                "Role not found with ID: " + roleId
            ));

        Set<Long> permIds = new HashSet<>(rolePermissionRepository.findPermissionIdsByRoleIdAndTenantId(roleId, tenantId));
        return roleMapper.toResponse(role, permIds);
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse getRoleByCode(String roleCode) {
        Long tenantId = currentTenantProvider.getCurrentTenantId();
        Role role = roleRepository.findByTenantIdAndRoleCode(tenantId, roleCode)
            .orElseThrow(() -> new RoleNotFoundException(
                "Role not found with code: " + roleCode
            ));

        Set<Long> permIds = new HashSet<>(rolePermissionRepository.findPermissionIdsByRoleIdAndTenantId(role.getId(), tenantId));
        return roleMapper.toResponse(role, permIds);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RoleListResponse> listRoles(Pageable pageable) {
        Long tenantId = currentTenantProvider.getCurrentTenantId();
        return roleRepository.findByTenantId(tenantId, pageable)
            .map(roleMapper::toListResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RoleListResponse> listRolesByTenant(Long tenantId, Pageable pageable) {
        return roleRepository.findByTenantId(tenantId, pageable)
            .map(roleMapper::toListResponse);
    }

    @Override
    public RoleResponse updateRole(Long roleId, UpdateRoleRequest request) {
        // Only SUPER_ADMIN, OWNER, TENANT_ADMIN, or ADMIN can update roles
        authorizationService.requireAnyAdmin();

        Long tenantId = currentTenantProvider.getCurrentTenantId();
        Role role = roleRepository.findByIdAndTenantId(roleId, tenantId)
            .orElseThrow(() -> new RoleNotFoundException(
                "Role not found with ID: " + roleId
            ));

        // Update role details
        role.updateDetails(request.roleName(), request.description());

        Role updatedRole = roleRepository.save(role);

        // Update permissions if permissionIds field is provided
        if (request.permissionIds() != null) {
            if (request.permissionIds().isEmpty()) {
                rolePermissionRepository.deleteByRoleIdAndTenantId(roleId, tenantId);
            } else {
                List<Long> targetIds = new ArrayList<>(request.permissionIds());
                rolePermissionRepository.deleteRolePermissionsExcept(roleId, targetIds);
                List<Permission> permissions = permissionRepository.findAllById(request.permissionIds());
                for (Permission perm : permissions) {
                    if (perm.isActive()) {
                        rolePermissionRepository.insertOnConflictDoNothing(
                            roleId,
                            perm.getId(),
                            "tenant_admin",
                            LocalDateTime.now()
                        );
                    }
                }
            }
        }

        Set<Long> permIds = new HashSet<>(rolePermissionRepository.findPermissionIdsByRoleIdAndTenantId(roleId, tenantId));
        return roleMapper.toResponse(updatedRole, permIds);
    }

    @Override
    public void deleteRole(Long roleId) {
        // Only SUPER_ADMIN, OWNER, TENANT_ADMIN, or ADMIN can delete roles
        authorizationService.requireAnyAdmin();

        Long tenantId = currentTenantProvider.getCurrentTenantId();
        Role role = roleRepository.findByIdAndTenantId(roleId, tenantId)
            .orElseThrow(() -> new RoleNotFoundException(
                "Role not found with ID: " + roleId
            ));

        try {
            role.deactivate();
        } catch (IllegalStateException e) {
            throw new CannotDeleteRoleException(e.getMessage());
        }

        roleRepository.save(role);
    }
}
