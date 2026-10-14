package com.erp.business.pos.shared.web;

import com.erp.platform.common.exception.AbstractGlobalExceptionHandler;
import com.erp.platform.common.exception.ApiError;
import com.erp.platform.common.exception.ErpException;
import com.erp.platform.common.exception.ForbiddenException;
import com.erp.platform.common.exception.ResourceNotFoundException;
import com.erp.platform.common.exception.UnauthorizedException;
import com.erp.platform.common.exception.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for the POS REST API.
 *
 * <p>Translates business-rule violations, aggregate guard failures and domain
 * exceptions into consistent, structured {@link ApiError} payloads.
 *
 * <p>Only the exception types the shared
 * {@link AbstractGlobalExceptionHandler} does <em>not</em> already map are
 * declared here. Redeclaring one the parent owns (for example
 * {@code BaseApiException} or {@code IllegalArgumentException}) makes the
 * mapping ambiguous and Spring refuses to start the context, so the generic
 * framework cases are deliberately left to the parent.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends AbstractGlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND",
                messageOr(ex, "The requested POS resource was not found"), request);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiError> handleBusinessValidation(
            ValidationException ex, HttpServletRequest request) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR",
                messageOr(ex, "The operation violates a business validation rule"), request);
    }

    @ExceptionHandler(ErpException.class)
    public ResponseEntity<ApiError> handleErpException(
            ErpException ex, HttpServletRequest request) {
        String code = ex.getErrorCode() != null ? ex.getErrorCode() : "BUSINESS_RULE_VIOLATION";
        return build(HttpStatus.UNPROCESSABLE_ENTITY, code,
                messageOr(ex, "A business validation rule failed"), request);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiError> handleUnauthorized(
            UnauthorizedException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                messageOr(ex, "You are not authorized to perform this action"), request);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiError> handleForbidden(
            ForbiddenException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "FORBIDDEN",
                messageOr(ex, "You do not have permission to perform this POS action"), request);
    }

    /** An aggregate lifecycle guard was hit → 409 Conflict. */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleIllegalState(
            IllegalStateException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "INVALID_STATE",
                messageOr(ex, "This action is not allowed in the current state"), request);
    }

    private static String messageOr(Exception ex, String fallback) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? fallback : message;
    }
}
