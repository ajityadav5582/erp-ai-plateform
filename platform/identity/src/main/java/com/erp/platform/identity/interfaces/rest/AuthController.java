package com.erp.platform.identity.interfaces.rest;

import com.erp.platform.identity.application.AuthService;
import com.erp.platform.identity.application.dto.LoginRequest;
import com.erp.platform.identity.application.dto.PasswordResetConfirmRequest;
import com.erp.platform.identity.application.dto.PasswordResetRequest;
import com.erp.platform.identity.application.dto.PasswordResetResponse;
import com.erp.platform.identity.application.dto.RefreshRequest;
import com.erp.platform.identity.application.dto.TokenResponse;
import com.erp.platform.identity.domain.exception.AccountLockedException;
import com.erp.platform.identity.domain.exception.AuthenticationException;
import com.erp.platform.identity.domain.exception.InvalidCredentialsException;
import com.erp.platform.identity.domain.exception.InvalidRefreshTokenException;
import com.erp.platform.identity.domain.exception.PasswordResetTokenException;
import com.erp.platform.identity.domain.exception.UserNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for authentication operations.
 *
 * <p>Exposes endpoints for login, token refresh, logout, and password reset.
 * These endpoints are public (no authentication required).
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Authenticates a user and returns a token pair.
     *
     * @param request the login request
     * @return 200 OK with the token pair
     */
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Refreshes an access token using a valid refresh token.
     *
     * <p>Implements refresh token rotation - the presented refresh token is revoked
     * and a new one is issued.
     *
     * @param request the refresh request containing the opaque refresh token
     * @return 200 OK with the new token pair
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        TokenResponse response = authService.refresh(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Logs out the current session by revoking the refresh token.
     *
     * @param request the refresh request containing the opaque refresh token
     * @return 204 No Content
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Initiates a password reset flow.
     *
     * <p>Generates a password reset token and returns it. In production,
     * this token should be sent via email instead of returned in the response.
     *
     * @param request the password reset request
     * @return 200 OK with the reset token
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<PasswordResetResponse> forgotPassword(@Valid @RequestBody PasswordResetRequest request) {
        PasswordResetResponse response = authService.forgotPassword(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Completes a password reset using a valid reset token.
     *
     * @param request the password reset confirmation request
     * @return 204 No Content
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody PasswordResetConfirmRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }
}
