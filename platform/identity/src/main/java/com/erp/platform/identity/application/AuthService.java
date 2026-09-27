package com.erp.platform.identity.application;

import com.erp.platform.identity.application.dto.CurrentUserResponse;
import com.erp.platform.identity.application.dto.LoginRequest;
import com.erp.platform.identity.application.dto.PasswordResetConfirmRequest;
import com.erp.platform.identity.application.dto.PasswordResetRequest;
import com.erp.platform.identity.application.dto.PasswordResetResponse;
import com.erp.platform.identity.application.dto.RefreshRequest;
import com.erp.platform.identity.application.dto.TokenResponse;


import com.erp.platform.identity.application.dto.RegisterRequest;

/**
 * Service interface for authentication operations.
 *
 * <p>Provides methods for user authentication, token management,
 * and password reset operations.
 *
 * @since 1.0.0
 */
public interface AuthService {

    /**
     * Registers a new user and returns authentication tokens.
     *
     * @param request the registration request
     * @return token response with access and refresh tokens
     */
    TokenResponse register(RegisterRequest request);

    /**
     * Authenticates a user with username/email and password.
     *
     * @param request the login request containing credentials
     * @return token response with access and refresh tokens
     * @throws com.erp.platform.identity.domain.exception.AuthenticationException if authentication fails
     */
    TokenResponse login(LoginRequest request);

    /**
     * Gets profile details of the currently authenticated user.
     *
     * @param userId the user's numeric ID
     * @param tenantId the tenant ID
     * @return CurrentUserResponse containing user details and roles
     */
    CurrentUserResponse getCurrentUser(Long userId, Long tenantId);

    /**
     * Refreshes an access token using a valid refresh token.
     *
     * <p>Implements refresh token rotation - the old refresh token is revoked
     * and a new one is issued.
     *
     * @param request the refresh request containing the refresh token
     * @return new token response with fresh access and refresh tokens
     * @throws com.erp.platform.identity.domain.exception.InvalidRefreshTokenException if token is invalid
     */
    TokenResponse refresh(RefreshRequest request);


    /**
     * Logs out a user by revoking their refresh token.
     *
     * @param request the refresh request containing the token to revoke
     */
    void logout(RefreshRequest request);

    /**
     * Logs out a user from all devices by revoking all their active refresh tokens.
     *
     * @param userId the user ID
     * @param tenantId the tenant ID
     */
    void logoutAll(Long userId, Long tenantId);

    /**
     * Initiates a password reset flow by generating a reset token.
     *
     * <p>In production, the reset token should be sent via email.
     * In development, it may be returned in the response.
     *
     * @param request the password reset request containing email
     * @return password reset response with token or confirmation message
     * @throws com.erp.platform.identity.domain.exception.UserNotFoundException if user not found
     */
    PasswordResetResponse forgotPassword(PasswordResetRequest request);

    /**
     * Resets a user's password using a valid reset token.
     *
     * @param request the password reset confirmation request
     * @throws com.erp.platform.identity.domain.exception.PasswordResetTokenException if token is invalid
     */
    void resetPassword(PasswordResetConfirmRequest request);
}
