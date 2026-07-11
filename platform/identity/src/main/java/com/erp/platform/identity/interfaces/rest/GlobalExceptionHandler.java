package com.erp.platform.identity.interfaces.rest;

import com.erp.platform.identity.domain.exception.AccountLockedException;
import com.erp.platform.identity.domain.exception.AuthenticationException;
import com.erp.platform.identity.domain.exception.CannotActivateUserException;
import com.erp.platform.identity.domain.exception.CannotDeactivateUserException;
import com.erp.platform.identity.domain.exception.CannotDeleteRoleException;
import com.erp.platform.identity.domain.exception.CannotDeleteUserException;
import com.erp.platform.identity.domain.exception.DuplicateEmailException;
import com.erp.platform.identity.domain.exception.DuplicateRoleCodeException;
import com.erp.platform.identity.domain.exception.InvalidCredentialsException;
import com.erp.platform.identity.domain.exception.InvalidRefreshTokenException;
import com.erp.platform.identity.domain.exception.PasswordResetTokenException;
import com.erp.platform.identity.domain.exception.PermissionNotFoundException;
import com.erp.platform.identity.domain.exception.RoleNotFoundException;
import com.erp.platform.identity.domain.exception.UserNotFoundException;
import com.erp.platform.identity.domain.exception.UserOperationException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

/**
 * Global exception handler for the identity REST API.
 *
 * <p>Maps domain and framework exceptions to the standard error response
 * structure and appropriate HTTP status codes defined in the platform
 * API and exception-handling standards.
 *
 * @since 1.0.0
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * User not found → 404 Not Found.
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(UserNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), request);
    }

    /**
     * Role not found → 404 Not Found.
     */
    @ExceptionHandler(RoleNotFoundException.class)
    public ResponseEntity<ApiError> handleRoleNotFound(RoleNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "ROLE_NOT_FOUND", ex.getMessage(), request);
    }

    /**
     * Permission not found → 404 Not Found.
     */
    @ExceptionHandler(PermissionNotFoundException.class)
    public ResponseEntity<ApiError> handlePermissionNotFound(PermissionNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "PERMISSION_NOT_FOUND", ex.getMessage(), request);
    }

    /**
     * Duplicate email → 409 Conflict.
     */
    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ApiError> handleDuplicateEmail(DuplicateEmailException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", ex.getMessage(), request);
    }

    /**
     * Duplicate role code → 409 Conflict.
     */
    @ExceptionHandler(DuplicateRoleCodeException.class)
    public ResponseEntity<ApiError> handleDuplicateRoleCode(DuplicateRoleCodeException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "DUPLICATE_ROLE_CODE", ex.getMessage(), request);
    }

    /**
     * Invalid credentials → 401 Unauthorized.
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", ex.getMessage(), request);
    }

    /**
     * Account locked → 403 Forbidden.
     */
    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ApiError> handleAccountLocked(AccountLockedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "ACCOUNT_LOCKED", ex.getMessage(), request);
    }

    /**
     * Authentication error → 401 Unauthorized.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_ERROR", ex.getMessage(), request);
    }

    /**
     * Invalid refresh token → 401 Unauthorized.
     */
    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ApiError> handleInvalidRefreshToken(InvalidRefreshTokenException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", ex.getMessage(), request);
    }

    /**
     * Password reset token error → 400 Bad Request.
     */
    @ExceptionHandler(PasswordResetTokenException.class)
    public ResponseEntity<ApiError> handlePasswordResetToken(PasswordResetTokenException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "PASSWORD_RESET_TOKEN_ERROR", ex.getMessage(), request);
    }

    /**
     * User lifecycle / business-rule violation → 409 Conflict.
     */
    @ExceptionHandler(UserOperationException.class)
    public ResponseEntity<ApiError> handleUserOperation(UserOperationException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "USER_OPERATION_CONFLICT", ex.getMessage(), request);
    }

    /**
     * Cannot activate user → 409 Conflict.
     */
    @ExceptionHandler(CannotActivateUserException.class)
    public ResponseEntity<ApiError> handleCannotActivateUser(CannotActivateUserException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "CANNOT_ACTIVATE_USER", ex.getMessage(), request);
    }

    /**
     * Cannot deactivate user → 409 Conflict.
     */
    @ExceptionHandler(CannotDeactivateUserException.class)
    public ResponseEntity<ApiError> handleCannotDeactivateUser(CannotDeactivateUserException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "CANNOT_DEACTIVATE_USER", ex.getMessage(), request);
    }

    /**
     * Cannot delete user → 409 Conflict.
     */
    @ExceptionHandler(CannotDeleteUserException.class)
    public ResponseEntity<ApiError> handleCannotDeleteUser(CannotDeleteUserException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "CANNOT_DELETE_USER", ex.getMessage(), request);
    }

    /**
     * Cannot delete role → 409 Conflict.
     */
    @ExceptionHandler(CannotDeleteRoleException.class)
    public ResponseEntity<ApiError> handleCannotDeleteRole(CannotDeleteRoleException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "CANNOT_DELETE_ROLE", ex.getMessage(), request);
    }

    /**
     * Bean validation failure → 422 Unprocessable Entity.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .orElse("Request validation failed");
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR", message, request);
    }

    /**
     * Any other unexpected error → 500 Internal Server Error.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error processing request: {}", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred", request);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String code, String message, HttpServletRequest request) {
        ApiError error = new ApiError(code, message, request.getRequestURI(), Instant.now());
        return ResponseEntity.status(status).body(error);
    }

    /**
     * Standard error response body.
     *
     * @param code      machine-readable error code
     * @param message   human-readable message
     * @param path      the request path that produced the error
     * @param timestamp occurrence time
     */
    public record ApiError(String code, String message, String path, Instant timestamp) {
    }
}
