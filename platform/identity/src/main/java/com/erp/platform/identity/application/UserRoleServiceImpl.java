package com.erp.platform.identity.application;

import com.erp.platform.identity.application.dto.AssignRoleRequest;
import com.erp.platform.identity.application.dto.RemoveRoleRequest;
import com.erp.platform.identity.application.dto.UserRoleListResponse;
import com.erp.platform.identity.application.dto.UserRoleResponse;
import com.erp.platform.identity.application.mapper.UserRoleMapper;
import com.erp.platform.identity.domain.Role;
import com.erp.platform.identity.domain.User;
import com.erp.platform.identity.domain.UserRole;
import com.erp.platform.identity.domain.exception.RoleNotFoundException;
import com.erp.platform.identity.domain.exception.UserNotFoundException;
import com.erp.platform.identity.infrastructure.persistence.RoleRepository;
import com.erp.platform.identity.infrastructure.persistence.UserRepository;
import com.erp.platform.identity.infrastructure.persistence.UserRoleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Instant;
import java.util.List;

/**
 * Service implementation for UserRole assignment operations.
 *
 * <p>Manages the many-to-many relationship between User and Role
 * with additional assignment metadata such as expiration, primary role,
 * and revocation tracking.
 *
 * @since 1.0.0
 */
@Service
@Transactional(readOnly = true)
public class UserRoleServiceImpl implements UserRoleService {

    private final UserRoleRepository userRoleRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleMapper userRoleMapper;
    private final CurrentTenantProvider currentTenantProvider;
    private final AuthorizationService authorizationService;

    public UserRoleServiceImpl(
            UserRoleRepository userRoleRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleMapper userRoleMapper,
            CurrentTenantProvider currentTenantProvider,
            AuthorizationService authorizationService) {
        this.userRoleRepository = userRoleRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleMapper = userRoleMapper;
        this.currentTenantProvider = currentTenantProvider;
        this.authorizationService = authorizationService;
    }

    @Override
    @Transactional
    public UserRoleResponse assignRole(AssignRoleRequest request) {
        // Only SUPER_ADMIN or TENANT_ADMIN can assign roles
        authorizationService.requireAnyAdmin();

        Long tenantId = currentTenantProvider.getCurrentTenantId();
        // Validate user exists in current tenant
        User user = userRepository.findByIdAndTenantId(request.userId(), tenantId)
                .orElseThrow(() -> UserNotFoundException.byUserId(request.userId()));

        if (!tenantId.equals(user.getTenantId())) {
            throw new IllegalStateException("User " + request.userId() + " does not belong to tenant " + tenantId);
        }

        // Validate role exists and belongs to current tenant
        Role role = roleRepository.findByIdAndTenantId(request.roleId(), tenantId)
                .orElseThrow(() -> RoleNotFoundException.byRoleId(request.roleId()));

        if (!tenantId.equals(role.getTenantId())) {
            throw new IllegalStateException("Role " + request.roleId() + " does not belong to tenant " + tenantId);
        }

        UserRole existingAssignment = userRoleRepository
                .findByUserIdAndTenantIdAndRoleId(request.userId(), tenantId, request.roleId())
                .orElse(null);
        if (existingAssignment != null && existingAssignment.isActive()) {
            throw new IllegalStateException(
                    "Role " + request.roleId() + " is already assigned to user " + request.userId());
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = request.expiresAt();
        String assignedBy = request.assignedBy() != null ? request.assignedBy() : "system";
        boolean primary = Boolean.TRUE.equals(request.isPrimaryRole());

        if (primary) {
            removePrimaryDesignation(request.userId(), request.roleId(), tenantId);
        }

        UserRole userRole;
        if (existingAssignment != null) {
            existingAssignment.reactivate(assignedBy, now, expiresAt, primary);
            userRole = existingAssignment;
        } else {
            userRole = UserRole.assign(
                    request.userId(), request.roleId(), tenantId, assignedBy, now, expiresAt, primary);
        }

        UserRole savedUserRole = userRoleRepository.save(userRole);
        return userRoleMapper.toResponse(savedUserRole, role);
    }

    @Override
    @Transactional
    public void removeRole(RemoveRoleRequest request) {
        // Only SUPER_ADMIN or TENANT_ADMIN can remove roles
        authorizationService.requireAnyAdmin();

        Long tenantId = currentTenantProvider.getCurrentTenantId();

        // Find the active assignment in current tenant
        UserRole userRole = userRoleRepository.findByUserIdAndTenantIdAndRoleId(request.userId(), tenantId, request.roleId())
                .orElseThrow(() -> new IllegalStateException(
                        "Role " + request.roleId() + " is not assigned to user " + request.userId()));

        // Revoke the assignment
        userRole.revoke(
                Instant.now(),
                request.revokedBy() != null ? request.revokedBy() : "system",
                request.revokeReason() != null ? request.revokeReason() : "Role removed by administrator"
        );

        userRoleRepository.save(userRole);
    }

    @Override
    public UserRoleResponse getUserRoleById(Long id) {
        // Only SUPER_ADMIN or TENANT_ADMIN can view user role assignments
        authorizationService.requireAnyAdmin();

        Long tenantId = currentTenantProvider.getCurrentTenantId();
        UserRole userRole = userRoleRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalStateException("User role assignment not found: " + id));

        // Fetch role for response - must verify role belongs to current tenant
        Role role = roleRepository.findByIdAndTenantIdOrGlobalSystem(userRole.getRoleId(), tenantId).orElse(null);
        return userRoleMapper.toResponse(userRole, role);
    }

    @Override
    public UserRoleListResponse listUserRoles(Long userId, Pageable pageable) {
        // Only SUPER_ADMIN or TENANT_ADMIN can list user roles
        authorizationService.requireAnyAdmin();

        Long tenantId = currentTenantProvider.getCurrentTenantId();
        // Validate user exists in current tenant
        userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> UserNotFoundException.byUserId(userId));

        Page<UserRole> userRolePage = userRoleRepository.findByUserIdAndTenantId(userId, tenantId, pageable);
        List<UserRoleResponse> responses = userRolePage.getContent().stream()
                .map(userRole -> {
                    Role role = roleRepository.findByIdAndTenantIdOrGlobalSystem(userRole.getRoleId(), tenantId).orElse(null);
                    return userRoleMapper.toResponse(userRole, role);
                })
                .toList();

        return userRoleMapper.toListResponse(
                responses,
                userRolePage.getTotalElements(),
                userRolePage.getTotalPages(),
                userRolePage.getNumber(),
                userRolePage.getSize()
        );
    }

    @Override
    public UserRoleListResponse listRoleUsers(Long roleId, Pageable pageable) {
        // Only SUPER_ADMIN or TENANT_ADMIN can list role users
        authorizationService.requireAnyAdmin();

        Long tenantId = currentTenantProvider.getCurrentTenantId();
        // Validate role exists and belongs to current tenant
        roleRepository.findByIdAndTenantIdOrGlobalSystem(roleId, tenantId)
                .orElseThrow(() -> RoleNotFoundException.byRoleId(roleId));

        Page<UserRole> userRolePage = userRoleRepository.findByRoleIdAndTenantId(roleId, tenantId, pageable);
        List<UserRoleResponse> responses = userRolePage.getContent().stream()
                .map(userRole -> {
                    Role role = roleRepository.findByIdAndTenantIdOrGlobalSystem(userRole.getRoleId(), tenantId).orElse(null);
                    return userRoleMapper.toResponse(userRole, role);
                })
                .toList();

        return userRoleMapper.toListResponse(
                responses,
                userRolePage.getTotalElements(),
                userRolePage.getTotalPages(),
                userRolePage.getNumber(),
                userRolePage.getSize()
        );
    }

    @Override
    public UserRoleResponse getPrimaryRole(Long userId) {
        // Only SUPER_ADMIN or TENANT_ADMIN can view primary role
        authorizationService.requireAnyAdmin();

        Long tenantId = currentTenantProvider.getCurrentTenantId();
        // Validate user exists in current tenant
        userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> UserNotFoundException.byUserId(userId));

        return userRoleRepository.findPrimaryByUserIdAndTenantId(userId, tenantId, LocalDateTime.now())
                .map(userRole -> {
                    Role role = roleRepository.findByIdAndTenantIdOrGlobalSystem(userRole.getRoleId(), tenantId).orElse(null);
                    return userRoleMapper.toResponse(userRole, role);
                })
                .orElse(null);
    }

    @Override
    public boolean hasRole(Long userId, Long roleId) {
        // Only SUPER_ADMIN or TENANT_ADMIN can check role assignments
        authorizationService.requireAnyAdmin();

        Long tenantId = currentTenantProvider.getCurrentTenantId();
        return userRoleRepository.existsActiveByUserIdAndTenantIdAndRoleId(
                userId, tenantId, roleId, LocalDateTime.now());
    }

    @Override
    public List<UserRoleResponse> listActiveRoles(Long userId) {
        // Only SUPER_ADMIN or TENANT_ADMIN can list active roles
        authorizationService.requireAnyAdmin();

        Long tenantId = currentTenantProvider.getCurrentTenantId();
        // Validate user exists in current tenant
        userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> UserNotFoundException.byUserId(userId));

        List<UserRole> activeUserRoles = userRoleRepository.findActiveByUserIdAndTenantId(
                userId, tenantId, LocalDateTime.now());
        return activeUserRoles.stream()
                .map(userRole -> {
                    Role role = roleRepository.findByIdAndTenantIdOrGlobalSystem(userRole.getRoleId(), tenantId).orElse(null);
                    return userRoleMapper.toResponse(userRole, role);
                })
                .toList();
    }

    /**
     * Remove primary designation from all other assignments for the user.
     *
     * @param userId the user ID
     * @param excludeRoleId the role ID to exclude (the one being set as primary)
     * @param tenantId the tenant ID
     */
    private void removePrimaryDesignation(Long userId, Long excludeRoleId, Long tenantId) {
        List<UserRole> primaryAssignments = userRoleRepository.findByUserIdAndTenantId(userId, tenantId);
        for (UserRole assignment : primaryAssignments) {
            if (assignment.getRoleId().equals(excludeRoleId)) {
                continue;
            }
            if (assignment.isPrimaryRole() && !assignment.isRevoked()) {
                assignment.removePrimaryDesignation();
                userRoleRepository.save(assignment);
            }
        }
    }
}
