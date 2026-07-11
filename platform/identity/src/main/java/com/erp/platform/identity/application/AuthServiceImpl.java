package com.erp.platform.identity.application;

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
import com.erp.platform.identity.domain.User;
import com.erp.platform.identity.domain.UserStatus;
import com.erp.platform.identity.domain.exception.AccountLockedException;
import com.erp.platform.identity.domain.exception.AuthenticationException;
import com.erp.platform.identity.domain.exception.InvalidCredentialsException;
import com.erp.platform.identity.domain.exception.InvalidRefreshTokenException;
import com.erp.platform.identity.domain.exception.PasswordResetTokenException;
import com.erp.platform.identity.domain.exception.UserNotFoundException;
import com.erp.platform.identity.infrastructure.persistence.PasswordResetTokenRepository;
import com.erp.platform.identity.infrastructure.persistence.RefreshTokenRepository;
import com.erp.platform.identity.infrastructure.persistence.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final JwtProperties jwtProperties;
    private final PasswordResetProperties passwordResetProperties;

    public AuthServiceImpl(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            JwtProperties jwtProperties,
            PasswordResetProperties passwordResetProperties) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.jwtProperties = jwtProperties;
        this.passwordResetProperties = passwordResetProperties;
    }

    @Override
    public TokenResponse login(LoginRequest request) {
        // Find user by username or email
        User user = userRepository.findByTenantIdAndUsername(request.tenantId(), request.username())
                .or(() -> userRepository.findByTenantIdAndEmail(request.tenantId(), request.username()))
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

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
        List<String> roles = getUserRoles(user.getId());

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(
                user.getUserId().toString(),
                user.getTenantId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                roles
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
                user.getUserId(),
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

        // Get user
        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new UserNotFoundException("User not found for refresh token"));

        // Check if user is still active
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AuthenticationException("User account is not active");
        }

        // Get user roles
        List<String> roles = getUserRoles(user.getId());

        // Generate new tokens
        String accessToken = jwtService.generateAccessToken(
                user.getUserId().toString(),
                user.getTenantId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                roles
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
                user.getUserId(),
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
        // Find user by email
        User user = userRepository.findByTenantIdAndEmail(request.tenantId(), request.email())
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + request.email()));

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

        // Find user
        User user = userRepository.findById(resetToken.getUserId())
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
     * Gets the list of role codes for a user.
     *
     * @param userId the user ID
     * @return list of role codes
     */
    private List<String> getUserRoles(Long userId) {
        // This is a simplified version - in a real implementation,
        // you would query the UserRoleRepository to get active roles
        // For now, return empty list - roles will be loaded by the security filter
        return List.of();
    }
}
