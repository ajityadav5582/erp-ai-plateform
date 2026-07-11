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
import com.erp.platform.identity.infrastructure.persistence.RoleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

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

    public RoleServiceImpl(RoleRepository roleRepository, RoleMapper roleMapper) {
        this.roleRepository = roleRepository;
        this.roleMapper = roleMapper;
    }

    @Override
    public RoleResponse createRole(Long tenantId, CreateRoleRequest request) {
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

        return roleMapper.toResponse(savedRole);
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse getRoleById(UUID roleId) {
        Role role = roleRepository.findByRoleId(roleId)
            .orElseThrow(() -> new RoleNotFoundException(
                "Role not found with ID: " + roleId
            ));

        return roleMapper.toResponse(role);
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse getRoleByCode(String roleCode) {
        Role role = roleRepository.findByRoleCode(roleCode)
            .orElseThrow(() -> new RoleNotFoundException(
                "Role not found with code: " + roleCode
            ));

        return roleMapper.toResponse(role);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RoleListResponse> listRoles(Pageable pageable) {
        return roleRepository.findAll(pageable)
            .map(roleMapper::toListResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RoleListResponse> listRolesByTenant(Long tenantId, Pageable pageable) {
        return roleRepository.findByTenantId(tenantId, pageable)
            .map(roleMapper::toListResponse);
    }

    @Override
    public RoleResponse updateRole(UUID roleId, UpdateRoleRequest request) {
        Role role = roleRepository.findByRoleId(roleId)
            .orElseThrow(() -> new RoleNotFoundException(
                "Role not found with ID: " + roleId
            ));

        // Update role details
        role.updateDetails(request.roleName(), request.description());

        Role updatedRole = roleRepository.save(role);

        return roleMapper.toResponse(updatedRole);
    }

    @Override
    public void deleteRole(UUID roleId) {
        Role role = roleRepository.findByRoleId(roleId)
            .orElseThrow(() -> new RoleNotFoundException(
                "Role not found with ID: " + roleId
            ));

        try {
            role.deactivate(Instant.now());
        } catch (IllegalStateException e) {
            throw new CannotDeleteRoleException(e.getMessage());
        }

        roleRepository.save(role);
    }
}
