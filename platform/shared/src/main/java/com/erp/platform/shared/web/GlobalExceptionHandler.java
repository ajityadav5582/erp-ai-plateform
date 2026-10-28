package com.erp.platform.shared.web;

import com.erp.platform.business.inventory.InventoryClientException;
import com.erp.platform.common.exception.AbstractGlobalExceptionHandler;
import com.erp.platform.common.exception.ApiError;
import com.erp.platform.common.exception.ErpException;
import com.erp.platform.common.exception.ForbiddenException;
import com.erp.platform.common.exception.ResourceNotFoundException;
import com.erp.platform.common.exception.UnauthorizedException;
import com.erp.platform.common.exception.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends AbstractGlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", messageOr(ex, "The requested resource was not found"), request);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiError> handleBusinessValidation(ValidationException ex, HttpServletRequest request) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR", messageOr(ex, "The request violates a business rule"), request);
    }

    @ExceptionHandler(ErpException.class)
    public ResponseEntity<ApiError> handleErpException(ErpException ex, HttpServletRequest request) {
        String code = ex.getErrorCode() != null ? ex.getErrorCode() : "VALIDATION_ERROR";
        return build(HttpStatus.UNPROCESSABLE_ENTITY, code, messageOr(ex, "A business validation rule failed"), request);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiError> handleUnauthorized(UnauthorizedException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", messageOr(ex, "You are not authorised to perform this action"), request);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiError> handleForbidden(ForbiddenException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "FORBIDDEN", messageOr(ex, "You do not have permission to perform this action"), request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "INVALID_STATE", messageOr(ex, "The operation is not allowed in the current state"), request);
    }

    @ExceptionHandler(InventoryClientException.class)
    public ResponseEntity<ApiError> handleInventoryClient(InventoryClientException ex, HttpServletRequest request) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, "INVENTORY_UNAVAILABLE", messageOr(ex, "A dependent service is unavailable"), request);
    }

    private static String messageOr(Exception ex, String fallback) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? fallback : message;
    }
}
