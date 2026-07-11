package com.erp.platform.identity.application;

import com.erp.platform.identity.application.dto.CreateUserRequest;
import com.erp.platform.identity.application.dto.UpdateUserRequest;
import com.erp.platform.identity.application.dto.UserListResponse;
import com.erp.platform.identity.application.dto.UserResponse;
import com.erp.platform.identity.application.mapper.UserMapper;
import com.erp.platform.identity.domain.User;
import com.erp.platform.identity.domain.UserStatus;
import com.erp.platform.identity.domain.exception.CannotActivateUserException;
import com.erp.platform.identity.domain.exception.CannotDeactivateUserException;
import com.erp.platform.identity.domain.exception.CannotDeleteUserException;
import com.erp.platform.identity.domain.exception.DuplicateEmailException;
import com.erp.platform.identity.domain.exception.UserNotFoundException;
import com.erp.platform.identity.infrastructure.persistence.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

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

    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Override
    public UserResponse createUser(Long tenantId, CreateUserRequest request) {
        // Check for duplicate email
        if (userRepository.existsByTenantIdAndEmail(tenantId, request.email())) {
            throw new DuplicateEmailException(
                "User with email '" + request.email() + "' already exists in this tenant"
            );
        }

        // Create user entity
        User user = userMapper.toEntity(request, tenantId);

        // Save user
        User savedUser = userRepository.save(user);

        return userMapper.toResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long tenantId, UUID userId) {
        User user = userRepository.findByTenantIdAndUserId(tenantId, userId)
            .orElseThrow(() -> new UserNotFoundException(
                "User not found with ID: " + userId
            ));

        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(Long tenantId, String email) {
        User user = userRepository.findByTenantIdAndEmail(tenantId, email)
            .orElseThrow(() -> new UserNotFoundException(
                "User not found with email: " + email
            ));

        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserListResponse> listUsers(Long tenantId, Pageable pageable) {
        return userRepository.findByTenantId(tenantId, pageable)
            .map(userMapper::toListResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserListResponse> listUsersByBranch(Long tenantId, Long branchId, Pageable pageable) {
        return userRepository.findByTenantIdAndBranchId(tenantId, branchId, pageable)
            .map(userMapper::toListResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserListResponse> listUsersByDepartment(Long tenantId, Long departmentId, Pageable pageable) {
        return userRepository.findByTenantIdAndDepartmentId(tenantId, departmentId, pageable)
            .map(userMapper::toListResponse);
    }

    @Override
    public UserResponse updateUser(Long tenantId, UUID userId, UpdateUserRequest request) {
        User user = userRepository.findByTenantIdAndUserId(tenantId, userId)
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
        if (request.jobTitle() != null) {
            user.updateJobTitle(request.jobTitle());
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

        return userMapper.toResponse(updatedUser);
    }

    @Override
    public UserResponse activateUser(Long tenantId, UUID userId) {
        User user = userRepository.findByTenantIdAndUserId(tenantId, userId)
            .orElseThrow(() -> new UserNotFoundException(
                "User not found with ID: " + userId
            ));

        try {
            user.activate(Instant.now());
        } catch (IllegalStateException e) {
            throw new CannotActivateUserException(e.getMessage());
        }

        User activatedUser = userRepository.save(user);

        return userMapper.toResponse(activatedUser);
    }

    @Override
    public UserResponse deactivateUser(Long tenantId, UUID userId) {
        User user = userRepository.findByTenantIdAndUserId(tenantId, userId)
            .orElseThrow(() -> new UserNotFoundException(
                "User not found with ID: " + userId
            ));

        try {
            user.deactivate(Instant.now());
        } catch (IllegalStateException e) {
            throw new CannotDeactivateUserException(e.getMessage());
        }

        User deactivatedUser = userRepository.save(user);

        return userMapper.toResponse(deactivatedUser);
    }

    @Override
    public void deleteUser(Long tenantId, UUID userId) {
        User user = userRepository.findByTenantIdAndUserId(tenantId, userId)
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
}
