package com.erp.business.sales.shared.web;

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
 * Global exception handler for the Sales REST API.
 *
 * <p>Translates business rule violations, aggregate guard failures, and domain
 * exceptions into consistent, structured {@link ApiError} payloads with descriptive,
 * user-friendly messages.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends AbstractGlobalExceptionHandler {

    /**
     * A requested entity was not found in the current tenant/company scope → 404 Not Found.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND",
                messageOr(ex, "The requested sales resource was not found"), request);
    }

    /**
     * Explicit platform business validation exception → 422 Unprocessable Entity.
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiError> handleBusinessValidation(
            ValidationException ex, HttpServletRequest request) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR",
                messageOr(ex, "The operation violates a business validation rule"), request);
    }

    /**
     * General ERP domain exception → 422 Unprocessable Entity.
     */
    @ExceptionHandler(ErpException.class)
    public ResponseEntity<ApiError> handleErpException(
            ErpException ex, HttpServletRequest request) {
        String code = ex.getErrorCode() != null ? ex.getErrorCode() : "BUSINESS_RULE_VIOLATION";
        return build(HttpStatus.UNPROCESSABLE_ENTITY, code,
                messageOr(ex, "A business validation rule failed"), request);
    }

    /**
     * Authorization refusal → 401 Unauthorized.
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiError> handleUnauthorized(
            UnauthorizedException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                messageOr(ex, "You are not authorized to perform this action"), request);
    }

    /**
     * Permission check failure → 403 Forbidden.
     */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiError> handleForbidden(
            ForbiddenException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "FORBIDDEN",
                messageOr(ex, "You do not have permission to perform this sales action"), request);
    }

    /**
     * An aggregate lifecycle guard was hit (e.g. attempting to cancel an order
     * with existing deliveries, issuing an invoice with no lines, recording
     * payment on a draft invoice, or delivering goods for an unconfirmed order)
     * → 409 Conflict.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleIllegalState(
            IllegalStateException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "INVALID_STATE",
                messageOr(ex, "This action is not allowed in the current state"), request);
    }

    /**
     * Extracts non-blank message or provides standard fallback.
     */
    private static String messageOr(Exception ex, String fallback) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? fallback : message;
    }
}
