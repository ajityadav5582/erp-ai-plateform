package com.erp.platform.identity.application;

import com.erp.platform.identity.application.dto.AssignRoleRequest;
import com.erp.platform.identity.application.dto.RemoveRoleRequest;
import com.erp.platform.identity.application.dto.UserRoleListResponse;
import com.erp.platform.identity.application.dto.UserRoleResponse;
import com.erp.platform.identity.application.mapper.UserRoleMapper;
import com.erp.platform.identity.domain.Role;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

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

    public UserRoleServiceImpl(
            UserRoleRepository userRoleRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleMapper userRoleMapper) {
        this.userRoleRepository = userRoleRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleMapper = userRoleMapper;
    }

    @Override
    @Transactional
    public UserRoleResponse assignRole(AssignRoleRequest request) {
        // Validate user exists
        userRepository.findById(request.userId())
                .orElseThrow(() -> UserNotFoundException.byUserId(request.userId()));

        // Validate role exists
        Role role = roleRepository.findById(request.roleId())
                .orElseThrow(() -> RoleNotFoundException.byRoleId(request.roleId()));

        // Check if assignment already exists
        if (userRoleRepository.existsByUserIdAndRoleId(request.userId(), request.roleId())) {
            throw new IllegalStateException(
                    "Role " + request.roleId() + " is already assigned to user " + request.userId());
        }

        // Convert expiresAt from LocalDateTime to Instant
        Instant expiresAt = request.expiresAt() != null
                ? request.expiresAt().atZone(java.time.ZoneId.systemDefault()).toInstant()
                : null;

        // Create user role assignment using factory method
        UserRole userRole = UserRole.assign(
                UUID.randomUUID(),
                request.userId(),
                request.roleId(),
                role.getTenantId(),
                request.assignedBy() != null ? request.assignedBy() : "system",
                Instant.now(),
                expiresAt,
                request.isPrimaryRole() != null ? request.isPrimaryRole() : false
        );

        // If this is set as primary, remove primary designation from other assignments
        if (Boolean.TRUE.equals(request.isPrimaryRole())) {
            removePrimaryDesignation(request.userId(), request.roleId());
        }

        UserRole savedUserRole = userRoleRepository.save(userRole);
        return userRoleMapper.toResponse(savedUserRole, role);
    }

    @Override
    @Transactional
    public void removeRole(RemoveRoleRequest request) {
        // Find the active assignment
        UserRole userRole = userRoleRepository.findByUserIdAndRoleId(request.userId(), request.roleId())
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
    public UserRoleResponse getUserRoleById(UUID userRoleId) {
        UserRole userRole = userRoleRepository.findByUserRoleId(userRoleId)
                .orElseThrow(() -> new IllegalStateException("User role assignment not found: " + userRoleId));

        // Fetch role for response
        Role role = roleRepository.findById(userRole.getRoleId()).orElse(null);
        return userRoleMapper.toResponse(userRole, role);
    }

    @Override
    public UserRoleListResponse listUserRoles(Long userId, Pageable pageable) {
        // Validate user exists
        userRepository.findById(userId)
                .orElseThrow(() -> UserNotFoundException.byUserId(userId));

        Page<UserRole> userRolePage = userRoleRepository.findByUserId(userId, pageable);
        List<UserRoleResponse> responses = userRolePage.getContent().stream()
                .map(userRole -> {
                    Role role = roleRepository.findById(userRole.getRoleId()).orElse(null);
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
        // Validate role exists
        roleRepository.findById(roleId)
                .orElseThrow(() -> RoleNotFoundException.byRoleId(roleId));

        Page<UserRole> userRolePage = userRoleRepository.findByRoleId(roleId, pageable);
        List<UserRoleResponse> responses = userRolePage.getContent().stream()
                .map(userRole -> {
                    Role role = roleRepository.findById(userRole.getRoleId()).orElse(null);
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
        // Validate user exists
        userRepository.findById(userId)
                .orElseThrow(() -> UserNotFoundException.byUserId(userId));

        return userRoleRepository.findPrimaryByUserId(userId, Instant.now())
                .map(userRole -> {
                    Role role = roleRepository.findById(userRole.getRoleId()).orElse(null);
                    return userRoleMapper.toResponse(userRole, role);
                })
                .orElse(null);
    }

    @Override
    public boolean hasRole(Long userId, Long roleId) {
        return userRoleRepository.existsActiveByUserIdAndRoleId(userId, roleId, Instant.now());
    }

    @Override
    public List<UserRoleResponse> listActiveRoles(Long userId) {
        // Validate user exists
        userRepository.findById(userId)
                .orElseThrow(() -> UserNotFoundException.byUserId(userId));

        List<UserRole> activeUserRoles = userRoleRepository.findActiveByUserId(userId, Instant.now());
        return activeUserRoles.stream()
                .map(userRole -> {
                    Role role = roleRepository.findById(userRole.getRoleId()).orElse(null);
                    return userRoleMapper.toResponse(userRole, role);
                })
                .toList();
    }

    /**
     * Remove primary designation from all other assignments for the user.
     *
     * @param userId the user ID
     * @param excludeRoleId the role ID to exclude (the one being set as primary)
     */
    private void removePrimaryDesignation(Long userId, Long excludeRoleId) {
        List<UserRole> primaryAssignments = userRoleRepository.findByUserId(userId);
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
