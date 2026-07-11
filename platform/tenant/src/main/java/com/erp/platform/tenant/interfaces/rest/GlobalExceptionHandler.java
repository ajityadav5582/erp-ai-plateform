package com.erp.platform.tenant.interfaces.rest;

import com.erp.platform.tenant.domain.TenantNotFoundException;
import com.erp.platform.tenant.domain.exception.TenantOperationException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

/**
 * Global exception handler for the tenant REST API.
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
     * Tenant not found → 404 Not Found.
     */
    @ExceptionHandler(TenantNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(TenantNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), request);
    }

    /**
     * Tenant lifecycle / business-rule violation → 409 Conflict.
     */
    @ExceptionHandler(TenantOperationException.class)
    public ResponseEntity<ApiError> handleTenantOperation(TenantOperationException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "TENANT_OPERATION_CONFLICT", ex.getMessage(), request);
    }

    /**
     * Illegal argument (e.g. duplicate tenant code) → 409 Conflict.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage(), request);
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
