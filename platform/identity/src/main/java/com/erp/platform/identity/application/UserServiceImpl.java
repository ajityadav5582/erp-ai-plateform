package com.erp.platform.identity.application;

import com.erp.platform.identity.application.dto.CreateUserRequest;
import com.erp.platform.identity.application.dto.UpdateUserRequest;
import com.erp.platform.identity.application.dto.UserListResponse;
import com.erp.platform.identity.application.dto.UserResponse;
import com.erp.platform.identity.application.dto.UserRoleResponse;
import com.erp.platform.identity.application.mapper.UserMapper;
import com.erp.platform.identity.application.mapper.UserRoleMapper;
import com.erp.platform.identity.domain.Role;
import com.erp.platform.identity.domain.User;
import com.erp.platform.identity.domain.UserRole;
import com.erp.platform.identity.domain.UserStatus;
import com.erp.platform.identity.domain.exception.CannotActivateUserException;
import com.erp.platform.identity.domain.exception.CannotDeactivateUserException;
import com.erp.platform.identity.domain.exception.CannotDeleteUserException;
import com.erp.platform.identity.domain.exception.DuplicateEmailException;
import com.erp.platform.identity.domain.exception.UnauthorizedException;
import com.erp.platform.identity.domain.exception.UserNotFoundException;
import com.erp.platform.identity.infrastructure.persistence.RoleRepository;
import com.erp.platform.identity.infrastructure.persistence.UserRepository;
import com.erp.platform.identity.infrastructure.persistence.UserRoleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of UserService.
 *
 * @since 1.0.0
 */

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final AuthorizationService authorizationService;
    private final UserRoleMapper userRoleMapper;

    public UserServiceImpl(
            UserRepository userRepository,
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            UserRoleRepository userRoleRepository,
            RoleRepository roleRepository,
            AuthorizationService authorizationService,
            UserRoleMapper userRoleMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.authorizationService = authorizationService;
        this.userRoleMapper = userRoleMapper;
    }

    @Override
    public UserResponse createUser(Long tenantId, CreateUserRequest request) {
        // Only SUPER_ADMIN or TENANT_ADMIN can create users
        authorizationService.requireAnyAdmin();

        // Check for duplicate email within tenant
        if (userRepository.existsByTenantIdAndEmail(tenantId, request.email())) {
            throw new DuplicateEmailException(
                "User with email '" + request.email() + "' already exists in this tenant"
            );
        }

        // Check for duplicate username within tenant
        if (userRepository.existsByTenantIdAndUsername(tenantId, request.username())) {
            throw new DuplicateEmailException(
                "User with username '" + request.username() + "' already exists in this tenant"
            );
        }

        String rawPassword = (request.password() != null && !request.password().isBlank())
                ? request.password()
                : "UserPassword123!";
        String passwordHash = passwordEncoder.encode(rawPassword);

        // Create user entity with password hash
        User user = userMapper.toEntity(request, tenantId, passwordHash);

        // Auto activate user created by admin if needed
        user.activate(Instant.now());

        // Save user
        User savedUser = userRepository.save(user);

        // Assign roles (from request or default)
        List<UserRoleResponse> assignedRoles = new ArrayList<>();
        Role primaryRole = null;

        if (request.roleIds() != null && !request.roleIds().isEmpty()) {
            for (Long roleId : request.roleIds()) {
                Role role = roleRepository.findByIdAndTenantIdOrGlobalSystem(roleId, tenantId)
                    .orElseThrow(() -> new IllegalArgumentException("Role not found with ID: " + roleId));
                if (primaryRole == null) {
                    primaryRole = role;
                }
                UserRole userRole = UserRole.assign(
                        savedUser.getId(),
                        role.getId(),
                        savedUser.getTenantId(),
                        "system",
                        Instant.now(),
                        null,
                        primaryRole == role
                );
                userRoleRepository.save(userRole);
                assignedRoles.add(userRoleMapper.toResponse(userRole, role));
            }
        } else {
            // Default role assignment
            List<Role> tenantRoles = roleRepository.findByTenantId(tenantId, Pageable.unpaged()).getContent();
            if (!tenantRoles.isEmpty()) {
                Role defaultRole = tenantRoles.stream()
                        .filter(r -> "EMPLOYEE".equalsIgnoreCase(r.getRoleCode()))
                        .findFirst()
                        .orElse(tenantRoles.get(0));
                primaryRole = defaultRole;
                UserRole userRole = UserRole.assign(
                        savedUser.getId(),
                        defaultRole.getId(),
                        savedUser.getTenantId(),
                        "system",
                        Instant.now(),
                        null,
                        true
                );
                userRoleRepository.save(userRole);
                assignedRoles.add(userRoleMapper.toResponse(userRole, defaultRole));
            }
        }

        return userMapper.toResponse(savedUser, primaryRole, assignedRoles);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long tenantId, Long userId) {
        User user = userRepository.findByIdAndTenantId(userId, tenantId)
            .orElseThrow(() -> new UserNotFoundException(
                "User not found with ID: " + userId
            ));

        Role role = findRoleForUser(userId, tenantId);
        return userMapper.toResponse(user, role);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(Long tenantId, String email) {
        User user = userRepository.findByTenantIdAndEmail(tenantId, email)
            .orElseThrow(() -> new UserNotFoundException(
                "User not found with email: " + email
            ));

        Role role = findRoleForUser(user.getId(), tenantId);
        return userMapper.toResponse(user, role);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserListResponse> listUsers(Long tenantId, String search, UserStatus status, Pageable pageable) {
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        if (cleanSearch == null && status == null) {
            return userRepository.findByTenantId(tenantId, pageable)
                .map(u -> userMapper.toListResponse(u, findRoleForUser(u.getId(), tenantId), getUserRolesForResponse(u.getId(), tenantId)));
        }
        if (cleanSearch == null) {
            return userRepository.findByTenantIdAndStatus(tenantId, status, pageable)
                .map(u -> userMapper.toListResponse(u, findRoleForUser(u.getId(), tenantId), getUserRolesForResponse(u.getId(), tenantId)));
        }
        return userRepository.findByTenantIdAndSearchAndStatus(tenantId, cleanSearch, status, pageable)
            .map(u -> userMapper.toListResponse(u, findRoleForUser(u.getId(), tenantId), getUserRolesForResponse(u.getId(), tenantId)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserListResponse> listUsers(Long tenantId, Pageable pageable) {
        return listUsers(tenantId, null, null, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserListResponse> listUsersByBranch(Long tenantId, Long branchId, Pageable pageable) {
        return userRepository.findByTenantIdAndBranchId(tenantId, branchId, pageable)
            .map(u -> userMapper.toListResponse(u, findRoleForUser(u.getId(), tenantId), getUserRolesForResponse(u.getId(), tenantId)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserListResponse> listUsersByDepartment(Long tenantId, Long departmentId, Pageable pageable) {
        return userRepository.findByTenantIdAndDepartmentId(tenantId, departmentId, pageable)
            .map(u -> userMapper.toListResponse(u, findRoleForUser(u.getId(), tenantId), getUserRolesForResponse(u.getId(), tenantId)));
    }

    @Override
    public UserResponse updateUser(Long tenantId, Long userId, UpdateUserRequest request) {
        // Only SUPER_ADMIN or TENANT_ADMIN can update users
        authorizationService.requireAnyAdmin();

        User user = userRepository.findByIdAndTenantId(userId, tenantId)
            .orElseThrow(() -> new UserNotFoundException(
                "User not found with ID: " + userId
            ));

        // Check for duplicate email if email is being changed
        if (request.email() != null && !request.email().equals(user.getEmail())) {
            if (userRepository.existsByTenantIdAndEmail(tenantId, request.email())) {
                throw new DuplicateEmailException(
                    "User with email '" + request.email() + "' already exists in this tenant"
                );
            }
        }

        // Update user fields
        if (request.email() != null) {
            user.updateEmail(request.email());
        }
        if (request.firstName() != null) {
            user.updateFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            user.updateLastName(request.lastName());
        }
        if (request.phoneNumber() != null) {
            user.updatePhoneNumber(request.phoneNumber());
        }
        if (request.profileImageUrl() != null) {
            user.updateProfileImageUrl(request.profileImageUrl());
        }
        if (request.branchId() != null) {
            user.updateBranch(request.branchId());
        }
        if (request.departmentId() != null) {
            user.updateDepartment(request.departmentId());
        }

        User updatedUser = userRepository.save(user);

        // Update roles if roleIds is provided
        if (request.roleIds() != null && !request.roleIds().isEmpty()) {
            // Delete existing roles
            userRoleRepository.deleteByUserIdAndTenantId(userId, tenantId);

            // Assign new roles
            List<UserRoleResponse> assignedRoles = new ArrayList<>();
            Role primaryRole = null;

            for (Long roleId : request.roleIds()) {
                Role role = roleRepository.findByIdAndTenantIdOrGlobalSystem(roleId, tenantId)
                    .orElseThrow(() -> new IllegalArgumentException("Role not found with ID: " + roleId));
                if (primaryRole == null) {
                    primaryRole = role;
                }
                UserRole userRole = UserRole.assign(
                    updatedUser.getId(),
                    role.getId(),
                    updatedUser.getTenantId(),
                    "system",
                    Instant.now(),
                    null,
                    primaryRole == role
                );
                userRoleRepository.save(userRole);
                assignedRoles.add(userRoleMapper.toResponse(userRole, role));
            }

            return userMapper.toResponse(updatedUser, primaryRole, assignedRoles);
        }

        Role currentRole = findRoleForUser(userId, tenantId);
        List<UserRoleResponse> userRoles = getUserRolesForResponse(userId, tenantId);
        return userMapper.toResponse(updatedUser, currentRole, userRoles);
    }

    @Override
    public UserResponse activateUser(Long tenantId, Long userId) {
        // Only SUPER_ADMIN or TENANT_ADMIN can activate users
        authorizationService.requireAnyAdmin();

        User user = userRepository.findByIdAndTenantId(userId, tenantId)
            .orElseThrow(() -> new UserNotFoundException(
                "User not found with ID: " + userId
            ));

        try {
            user.activate(Instant.now());
        } catch (IllegalStateException e) {
            throw new CannotActivateUserException(e.getMessage());
        }

        User activatedUser = userRepository.save(user);

        Role role = findRoleForUser(activatedUser.getId(), tenantId);
        List<UserRoleResponse> userRoles = getUserRolesForResponse(activatedUser.getId(), tenantId);
        return userMapper.toResponse(activatedUser, role, userRoles);
    }

    @Override
    public UserResponse deactivateUser(Long tenantId, Long userId) {
        // Only SUPER_ADMIN or TENANT_ADMIN can deactivate users
        authorizationService.requireAnyAdmin();

        User user = userRepository.findByIdAndTenantId(userId, tenantId)
            .orElseThrow(() -> new UserNotFoundException(
                "User not found with ID: " + userId
            ));

        try {
            user.deactivate(Instant.now());
        } catch (IllegalStateException e) {
            throw new CannotDeactivateUserException(e.getMessage());
        }

        User deactivatedUser = userRepository.save(user);

        Role role = findRoleForUser(deactivatedUser.getId(), tenantId);
        List<UserRoleResponse> userRoles = getUserRolesForResponse(deactivatedUser.getId(), tenantId);
        return userMapper.toResponse(deactivatedUser, role, userRoles);
    }

    @Override
    public void deleteUser(Long tenantId, Long userId) {
        // Only SUPER_ADMIN or TENANT_ADMIN can delete users
        authorizationService.requireAnyAdmin();

        User user = userRepository.findByIdAndTenantId(userId, tenantId)
            .orElseThrow(() -> new UserNotFoundException(
                "User not found with ID: " + userId
            ));

        try {
            user.archive(Instant.now());
        } catch (IllegalStateException e) {
            throw new CannotDeleteUserException(e.getMessage());
        }

        userRepository.save(user);
    }

    private Role findRoleForUser(Long userId, Long tenantId) {
        if (userId == null) {
            return null;
        }
        List<UserRole> activeUserRoles = userRoleRepository.findActiveByUserIdAndTenantId(
                userId, tenantId, LocalDateTime.now());
        List<UserRole> userRoles = activeUserRoles.isEmpty()
                ? userRoleRepository.findByUserIdAndTenantId(userId, tenantId)
                : activeUserRoles;
        if (!userRoles.isEmpty()) {
            // Find primary role first
            Optional<UserRole> primaryRole = userRoles.stream()
                    .filter(UserRole::isPrimaryRole)
                    .findFirst();
            Long roleId = primaryRole.map(UserRole::getRoleId)
                    .orElseGet(() -> userRoles.get(0).getRoleId());
            return roleRepository.findByIdAndTenantIdOrGlobalSystem(roleId, tenantId).orElse(null);
        }
        return null;
    }

    private List<UserRoleResponse> getUserRolesForResponse(Long userId, Long tenantId) {
        if (userId == null) {
            return List.of();
        }
        List<UserRole> userRoles = userRoleRepository.findByUserIdAndTenantId(userId, tenantId);
        return userRoles.stream()
                .map(userRole -> {
                    Role role = roleRepository.findByIdAndTenantIdOrGlobalSystem(userRole.getRoleId(), tenantId).orElse(null);
                    return userRoleMapper.toResponse(userRole, role);
                })
                .toList();
    }
}
