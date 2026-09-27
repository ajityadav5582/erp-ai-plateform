package com.erp.platform.identity.application;

import com.erp.platform.identity.application.dto.CreateUserRequest;
import com.erp.platform.identity.application.dto.UpdateUserRequest;
import com.erp.platform.identity.application.dto.UserListResponse;
import com.erp.platform.identity.application.dto.UserResponse;
import com.erp.platform.identity.application.mapper.UserMapper;
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
import java.util.List;

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

    public UserServiceImpl(
            UserRepository userRepository,
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            UserRoleRepository userRoleRepository,
            RoleRepository roleRepository,
            AuthorizationService authorizationService) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.authorizationService = authorizationService;
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

        // Assign role (from request or default)
        Role roleToAssign = null;
        if (request.roleId() != null) {
            roleToAssign = roleRepository.findByIdAndTenantIdOrGlobalSystem(request.roleId(), tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with ID: " + request.roleId()));
        } else {
            List<Role> tenantRoles = roleRepository.findByTenantId(tenantId, Pageable.unpaged()).getContent();
            if (!tenantRoles.isEmpty()) {
                roleToAssign = tenantRoles.stream()
                        .filter(r -> "EMPLOYEE".equalsIgnoreCase(r.getRoleCode()))
                        .findFirst()
                        .orElse(tenantRoles.get(0));
            }
        }

        if (roleToAssign != null) {
            UserRole userRole = UserRole.assign(
                    savedUser.getId(),
                    roleToAssign.getId(),
                    savedUser.getTenantId(),
                    "system",
                    Instant.now(),
                    null,
                    true
            );
            userRoleRepository.save(userRole);
        }

        return userMapper.toResponse(savedUser, roleToAssign);
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
                .map(u -> userMapper.toListResponse(u, findRoleForUser(u.getId(), tenantId)));
        }
        if (cleanSearch == null) {
            return userRepository.findByTenantIdAndStatus(tenantId, status, pageable)
                .map(u -> userMapper.toListResponse(u, findRoleForUser(u.getId(), tenantId)));
        }
        return userRepository.findByTenantIdAndSearchAndStatus(tenantId, cleanSearch, status, pageable)
            .map(u -> userMapper.toListResponse(u, findRoleForUser(u.getId(), tenantId)));
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
            .map(u -> userMapper.toListResponse(u, findRoleForUser(u.getId(), tenantId)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserListResponse> listUsersByDepartment(Long tenantId, Long departmentId, Pageable pageable) {
        return userRepository.findByTenantIdAndDepartmentId(tenantId, departmentId, pageable)
            .map(u -> userMapper.toListResponse(u, findRoleForUser(u.getId(), tenantId)));
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

        // Update role if roleId is provided
        if (request.roleId() != null) {
            Role newRole = roleRepository.findByIdAndTenantIdOrGlobalSystem(request.roleId(), tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with ID: " + request.roleId()));

            userRoleRepository.deleteByUserIdAndTenantId(userId, tenantId);

            UserRole userRole = UserRole.assign(
                updatedUser.getId(),
                newRole.getId(),
                updatedUser.getTenantId(),
                "system",
                Instant.now(),
                null,
                true
            );
            userRoleRepository.save(userRole);
        }

        Role currentRole = findRoleForUser(userId, tenantId);
        return userMapper.toResponse(updatedUser, currentRole);
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
        return userMapper.toResponse(activatedUser, role);
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
        return userMapper.toResponse(deactivatedUser, role);
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
        List<UserRole> userRoles = userRoleRepository.findActiveByUserIdAndTenantId(
                userId, tenantId, java.time.LocalDateTime.now());
        if (userRoles.isEmpty()) {
            userRoles = userRoleRepository.findByUserIdAndTenantId(userId, tenantId);
        }
        if (!userRoles.isEmpty()) {
            Long roleId = userRoles.get(0).getRoleId();
            return roleRepository.findByIdAndTenantIdOrGlobalSystem(roleId, tenantId).orElse(null);
        }
        return null;
    }
}
