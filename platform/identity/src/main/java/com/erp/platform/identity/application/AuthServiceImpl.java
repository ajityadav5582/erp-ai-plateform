package com.erp.platform.identity.application;

import com.erp.platform.identity.application.CurrentTenantProvider;
import com.erp.platform.identity.application.dto.CurrentUserResponse;
import com.erp.platform.identity.application.dto.LoginRequest;
import com.erp.platform.identity.application.dto.PasswordResetConfirmRequest;
import com.erp.platform.identity.application.dto.PasswordResetRequest;
import com.erp.platform.identity.application.dto.PasswordResetResponse;
import com.erp.platform.identity.application.dto.RefreshRequest;
import com.erp.platform.identity.application.dto.TokenResponse;
import com.erp.platform.identity.application.security.JwtProperties;
import com.erp.platform.identity.application.security.JwtService;
import com.erp.platform.identity.application.security.PasswordResetProperties;
import com.erp.platform.identity.application.security.TokenHasher;
import com.erp.platform.identity.domain.PasswordResetToken;
import com.erp.platform.identity.domain.RefreshToken;
import com.erp.platform.identity.domain.Role;
import com.erp.platform.identity.domain.RoleType;
import com.erp.platform.identity.domain.User;
import com.erp.platform.identity.domain.UserRole;
import com.erp.platform.identity.domain.UserStatus;
import com.erp.platform.identity.domain.exception.AccountLockedException;
import com.erp.platform.identity.domain.exception.AuthenticationException;
import com.erp.platform.identity.domain.exception.InvalidCredentialsException;
import com.erp.platform.identity.domain.exception.InvalidRefreshTokenException;
import com.erp.platform.identity.domain.exception.PasswordResetTokenException;
import com.erp.platform.identity.domain.exception.TenantNotFoundException;
import com.erp.platform.identity.domain.exception.UserNotFoundException;
import com.erp.platform.identity.infrastructure.persistence.PasswordResetTokenRepository;
import com.erp.platform.identity.infrastructure.persistence.RefreshTokenRepository;
import com.erp.platform.identity.infrastructure.persistence.RoleRepository;
import com.erp.platform.identity.infrastructure.persistence.UserRepository;
import com.erp.platform.identity.infrastructure.persistence.UserRoleRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.erp.platform.identity.application.dto.RegisterRequest;
import com.erp.platform.identity.domain.exception.DuplicateEmailException;

/**
 * Implementation of AuthService.
 *
 * <p>Handles authentication, token management, and password reset operations.
 * Uses refresh token rotation for enhanced security.
 *
 * @since 1.0.0
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final JwtProperties jwtProperties;
    private final PasswordResetProperties passwordResetProperties;

    @PersistenceContext
    private EntityManager entityManager;

    private final CurrentTenantProvider currentTenantProvider;
    private final AuthorizationService authorizationService;

    public AuthServiceImpl(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            UserRoleRepository userRoleRepository,
            RoleRepository roleRepository,
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            JwtProperties jwtProperties,
            PasswordResetProperties passwordResetProperties,
            CurrentTenantProvider currentTenantProvider,
            AuthorizationService authorizationService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.jwtProperties = jwtProperties;
        this.passwordResetProperties = passwordResetProperties;
        this.currentTenantProvider = currentTenantProvider;
        this.authorizationService = authorizationService;
    }

    @Override
    @Transactional
    public TokenResponse register(RegisterRequest request) {
        Long tenantId = request.tenantId();

        if (tenantId == null) {
            throw new TenantNotFoundException("Tenant ID is required for registration");
        }

        // Validate that the tenant exists
        validateTenantExists(tenantId);

        // Check for duplicate email
        if (userRepository.existsByTenantIdAndEmail(tenantId, request.email())) {
            throw new DuplicateEmailException("User with email '" + request.email() + "' already exists in this tenant");
        }

        // Check for duplicate username
        if (userRepository.existsByTenantIdAndUsername(tenantId, request.username())) {
            throw new DuplicateEmailException("User with username '" + request.username() + "' already exists in this tenant");
        }

        String passwordHash = passwordEncoder.encode(request.password());

        User user = User.create(
                tenantId,
                request.username(),
                request.email(),
                passwordHash,
                request.firstName(),
                request.lastName(),
                request.phoneNumber(),
                request.jobTitle(),
                request.profileImageUrl(),
                request.branchId(),
                request.departmentId()
        );

        user.activate(Instant.now());
        User savedUser = userRepository.save(user);

        // Assign default role if roles exist in the tenant
        // Only assign non-system roles to newly registered users
        // System roles (SUPER_ADMIN) must never be assigned during normal registration
        // Tenant-scoped CUSTOM roles are assignable
        List<Role> tenantRoles = roleRepository.findByTenantId(tenantId, Pageable.unpaged()).getContent();
        if (!tenantRoles.isEmpty()) {
            List<Role> assignableRoles = tenantRoles.stream()
                    .filter(r -> r.getRoleType() != RoleType.SYSTEM)
                    .toList();

            if (!assignableRoles.isEmpty()) {
                Role defaultRole = assignableRoles.stream()
                        .filter(r -> "EMPLOYEE".equalsIgnoreCase(r.getRoleCode()))
                        .findFirst()
                        .orElse(assignableRoles.get(0));

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
            }
        }

        List<String> roles = getUserRoles(savedUser.getId(), savedUser.getTenantId());
        List<String> permissions = getUserPermissions(savedUser.getId(), savedUser.getTenantId());

        String accessToken = jwtService.generateAccessToken(
                savedUser.getId().toString(),
                savedUser.getTenantId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getFullName(),
                roles,
                permissions
        );

        String rawRefreshToken = jwtService.generateRawRefreshToken();
        String tokenHash = TokenHasher.sha256Hex(rawRefreshToken);
        String tokenId = jwtService.generateRefreshTokenId();

        RefreshToken refreshToken = RefreshToken.issue(
                UUID.fromString(tokenId),
                savedUser.getId(),
                savedUser.getTenantId(),
                tokenHash,
                Instant.now().plusMillis(jwtProperties.getRefreshTokenExpiryMs()),
                request.deviceInfo(),
                request.ipAddress(),
                Instant.now()
        );
        refreshTokenRepository.save(refreshToken);

        long accessTokenExpiresInSeconds = jwtProperties.getAccessTokenExpiryMs() / 1000;
        long refreshTokenExpiresInSeconds = jwtProperties.getRefreshTokenExpiryMs() / 1000;

        return TokenResponse.bearer(
                accessToken,
                rawRefreshToken,
                accessTokenExpiresInSeconds,
                refreshTokenExpiresInSeconds,
                savedUser.getId(),
                savedUser.getTenantId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getFullName(),
                roles
        );
    }

    @Override
    @Transactional
    public TokenResponse login(LoginRequest request) {
        // Find user by username or email.
        // If tenantId is provided, scope the search to that tenant.
        // If tenantId is omitted, search across all tenants.
        User user;
        if (request.tenantId() != null) {
            user = userRepository.findByTenantIdAndUsername(request.tenantId(), request.username())
                    .or(() -> userRepository.findByTenantIdAndEmail(request.tenantId(), request.username()))
                    .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));
        } else {
            user = userRepository.findByUsername(request.username())
                    .or(() -> userRepository.findByEmail(request.username()))
                    .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));
        }

        // Check if user is active
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AuthenticationException("User account is not active. Current status: " + user.getStatus());
        }

        // Check if account is temporarily locked due to failed login attempts
        if (user.isTemporarilyLocked(Instant.now())) {
            throw new AccountLockedException(
                    "Account is temporarily locked due to multiple failed login attempts. " +
                    "Please try again later or reset your password."
            );
        }

        // Verify password
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            // Register failed login attempt
            boolean locked = user.registerFailedLogin(
                    passwordResetProperties.getMaxFailedLoginAttempts(),
                    Instant.now(),
                    Instant.now().plusMillis(passwordResetProperties.getLockoutDurationMs())
            );
            userRepository.save(user);

            if (locked) {
                throw new AccountLockedException(
                        "Account has been temporarily locked due to " +
                        passwordResetProperties.getMaxFailedLoginAttempts() +
                        " failed login attempts. Please try again later or reset your password."
                );
            }

            throw new InvalidCredentialsException("Invalid username or password");
        }

        // Reset failed login attempts on successful login
        user.resetFailedLoginAttempts();
        user.recordLogin(Instant.now());
        userRepository.save(user);

        // Get user roles
        List<String> roles = getUserRoles(user.getId(), user.getTenantId());

        // Resolve permissions so downstream business services can authorize from the token
        List<String> permissions = getUserPermissions(user.getId(), user.getTenantId());

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(
                user.getId().toString(),
                user.getTenantId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                roles,
                permissions
        );

        String rawRefreshToken = jwtService.generateRawRefreshToken();
        String tokenHash = TokenHasher.sha256Hex(rawRefreshToken);
        String tokenId = jwtService.generateRefreshTokenId();

        // Store refresh token
        RefreshToken refreshToken = RefreshToken.issue(
                UUID.fromString(tokenId),
                user.getId(),
                user.getTenantId(),
                tokenHash,
                Instant.now().plusMillis(jwtProperties.getRefreshTokenExpiryMs()),
                request.deviceInfo(),
                request.ipAddress(),
                Instant.now()
        );
        refreshTokenRepository.save(refreshToken);

        long accessTokenExpiresInSeconds = jwtProperties.getAccessTokenExpiryMs() / 1000;
        long refreshTokenExpiresInSeconds = jwtProperties.getRefreshTokenExpiryMs() / 1000;

        return TokenResponse.bearer(
                accessToken,
                rawRefreshToken,
                accessTokenExpiresInSeconds,
                refreshTokenExpiresInSeconds,
                user.getId(),
                user.getTenantId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                roles
        );
    }

    @Override
    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        String tokenHash = TokenHasher.sha256Hex(request.refreshToken());

        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));

        // Check if token is usable
        if (!refreshToken.isUsable(Instant.now())) {
            if (refreshToken.isRevoked()) {
                throw new InvalidRefreshTokenException("Refresh token has been revoked");
            }
            throw new InvalidRefreshTokenException("Refresh token has expired");
        }

        // Revoke the old token (refresh token rotation)
        refreshToken.revoke(Instant.now(), "system");
        refreshTokenRepository.save(refreshToken);

        // Get user - must verify tenant isolation
        User user = userRepository.findByIdAndTenantId(refreshToken.getUserId(), refreshToken.getTenantId())
                .orElseThrow(() -> new UserNotFoundException("User not found for refresh token"));

        // Check if user is still active
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AuthenticationException("User account is not active");
        }

        // Get user roles
        List<String> roles = getUserRoles(user.getId(), user.getTenantId());

        // Re-resolve permissions on refresh so changes take effect within one access-token TTL
        List<String> permissions = getUserPermissions(user.getId(), user.getTenantId());

        // Generate new tokens
        String accessToken = jwtService.generateAccessToken(
                user.getId().toString(),
                user.getTenantId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                roles,
                permissions
        );

        String newRawRefreshToken = jwtService.generateRawRefreshToken();
        String newTokenHash = TokenHasher.sha256Hex(newRawRefreshToken);
        String newTokenId = jwtService.generateRefreshTokenId();

        // Store new refresh token
        RefreshToken newRefreshToken = RefreshToken.issue(
                UUID.fromString(newTokenId),
                user.getId(),
                user.getTenantId(),
                newTokenHash,
                Instant.now().plusMillis(jwtProperties.getRefreshTokenExpiryMs()),
                refreshToken.getDeviceInfo(),
                refreshToken.getIpAddress(),
                Instant.now()
        );
        refreshTokenRepository.save(newRefreshToken);

        long accessTokenExpiresInSeconds = jwtProperties.getAccessTokenExpiryMs() / 1000;
        long refreshTokenExpiresInSeconds = jwtProperties.getRefreshTokenExpiryMs() / 1000;

        return TokenResponse.bearer(
                accessToken,
                newRawRefreshToken,
                accessTokenExpiresInSeconds,
                refreshTokenExpiresInSeconds,
                user.getId(),
                user.getTenantId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                roles
        );
    }

    @Override
    @Transactional
    public void logout(RefreshRequest request) {
        String tokenHash = TokenHasher.sha256Hex(request.refreshToken());

        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            token.revoke(Instant.now(), "system");
            refreshTokenRepository.save(token);
        });
    }

    @Override
    @Transactional
    public void logoutAll(Long userId, Long tenantId) {
        // Revoke all active refresh tokens for the user
        List<RefreshToken> activeTokens = refreshTokenRepository.findByUserIdAndTenantId(userId, tenantId);

        Instant now = Instant.now();
        for (RefreshToken token : activeTokens) {
            if (token.isUsable(now)) {
                token.revoke(now, "system");
                refreshTokenRepository.save(token);
            }
        }
    }

    @Override
    @Transactional
    public PasswordResetResponse forgotPassword(PasswordResetRequest request) {
        // Find user by email.
        // If tenantId is provided, scope the search to that tenant.
        // If tenantId is omitted, search across all tenants.
        User user;
        if (request.tenantId() != null) {
            user = userRepository.findByTenantIdAndEmail(request.tenantId(), request.email())
                    .orElseThrow(() -> new UserNotFoundException("User not found with email: " + request.email()));
        } else {
            user = userRepository.findByEmail(request.email())
                    .orElseThrow(() -> new UserNotFoundException("User not found with email: " + request.email()));
        }

        // Invalidate any existing active reset tokens for this user
        passwordResetTokenRepository.invalidateActiveByUserIdAndTenantId(
                user.getId(),
                user.getTenantId(),
                Instant.now(),
                Instant.now()
        );

        // Generate new reset token
        String rawToken = jwtService.generateRawRefreshToken();
        String tokenHash = TokenHasher.sha256Hex(rawToken);
        String tokenId = jwtService.generateRefreshTokenId();

        PasswordResetToken resetToken = PasswordResetToken.issue(
                UUID.fromString(tokenId),
                user.getId(),
                user.getTenantId(),
                tokenHash,
                Instant.now().plusMillis(passwordResetProperties.getTokenExpiryMs()),
                Instant.now()
        );

        passwordResetTokenRepository.save(resetToken);

        // In production, this token should be sent via email
        // For development, we return it in the response
        if (passwordResetProperties.isReturnTokenInResponse()) {
            return PasswordResetResponse.of(
                    "Password reset token generated. In production, this would be sent via email.",
                    rawToken
            );
        } else {
            return PasswordResetResponse.of(
                    "If an account with that email exists, a password reset link has been sent."
            );
        }
    }

    @Override
    @Transactional
    public void resetPassword(PasswordResetConfirmRequest request) {
        String tokenHash = TokenHasher.sha256Hex(request.token());

        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new PasswordResetTokenException("Invalid password reset token"));

        // Check if token is usable
        if (!resetToken.isUsable(Instant.now())) {
            if (resetToken.isUsed()) {
                throw new PasswordResetTokenException("Password reset token has already been used");
            }
            throw new PasswordResetTokenException("Password reset token has expired");
        }

        // Find user - must verify tenant isolation
        User user = userRepository.findByIdAndTenantId(resetToken.getUserId(), resetToken.getTenantId())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Hash the new password
        String newPasswordHash = passwordEncoder.encode(request.newPassword());

        // Update password
        user.changePassword(newPasswordHash);
        user.clearLock();
        userRepository.save(user);

        // Mark token as used
        resetToken.markUsed(Instant.now());
        passwordResetTokenRepository.save(resetToken);
    }

    /**
     * Validates that the given tenant ID exists in the tenants table.
     *
     * @param tenantId the tenant ID to validate
     * @throws TenantNotFoundException if the tenant does not exist
     */
    @SuppressWarnings("unchecked")
    private void validateTenantExists(Long tenantId) {
        if (entityManager != null) {
            java.util.List<Object> results = entityManager.createNativeQuery(
                            "SELECT id FROM tenants WHERE id = ?")
                    .setParameter(1, tenantId)
                    .getResultList();

            if (results.isEmpty()) {
                throw new TenantNotFoundException(tenantId);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CurrentUserResponse getCurrentUser(Long userId, Long tenantId) {
        User user = userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new UserNotFoundException("User not found for ID: " + userId));

        List<String> roles = getUserRoles(user.getId(), user.getTenantId());
        List<String> permissions = new java.util.ArrayList<>(authorizationService.getUserPermissions(user.getTenantId(), user.getId()));

        return new CurrentUserResponse(
                user.getId(),
                user.getTenantId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                roles,
                permissions
        );
    }

    /**
     * Gets the list of active role codes for a user within a specific tenant.
     *
     * @param userId the user ID
     * @param tenantId the tenant ID
     * @return list of active role codes
     */
    private List<String> getUserRoles(Long userId, Long tenantId) {
        if (userId == null) {
            return List.of();
        }
        Long effectiveTenantId = tenantId != null ? tenantId : currentTenantProvider.getCurrentTenantId();
        List<UserRole> activeUserRoles = userRoleRepository.findActiveByUserIdAndTenantId(
                userId, effectiveTenantId, LocalDateTime.now());
        if (activeUserRoles.isEmpty()) {
            return List.of();
        }
        List<Long> roleIds = activeUserRoles.stream().map(UserRole::getRoleId).distinct().toList();
        return roleRepository.findAllById(roleIds).stream().map(Role::getRoleCode).toList();
    }

    private List<String> getUserRoles(Long userId) {
        return getUserRoles(userId, currentTenantProvider.getCurrentTenantId());
    }

    /**
     * Resolves the permission codes granted to a user's active roles in a tenant.
     *
     * <p>These are embedded in the access token so that downstream business services
     * (inventory, sales, finance, ...) can authorize requests from the token alone,
     * without calling back into the identity service or hitting its database.
     *
     * @param userId   the user's primary key
     * @param tenantId the tenant scope
     * @return the permission codes, or an empty list when none apply
     */
    private List<String> getUserPermissions(Long userId, Long tenantId) {
        if (userId == null || tenantId == null) {
            return List.of();
        }
        return List.copyOf(authorizationService.getUserPermissions(tenantId, userId));
    }
}
