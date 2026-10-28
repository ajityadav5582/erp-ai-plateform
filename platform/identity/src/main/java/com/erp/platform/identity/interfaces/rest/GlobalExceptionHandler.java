package com.erp.platform.identity.interfaces.rest;

import com.erp.platform.common.exception.AbstractGlobalExceptionHandler;
import com.erp.platform.common.exception.ApiError;
import com.erp.platform.tenant.domain.exception.TenantOperationException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for the identity REST API.
 *
 * <p>This class used to contain 22 near-identical {@code @ExceptionHandler}
 * methods whose only job was to name an HTTP status and an error code for a given
 * exception type. Those status/code pairs now live on the exceptions themselves
 * (see {@code com.erp.platform.common.exception.BaseApiException}), so a single
 * inherited method handles all of them.
 *
 * <p>Only {@link TenantOperationException} still needs an explicit method, and that
 * is deliberate. Its subclasses ({@code CannotActivateTenantException},
 * {@code CannotSuspendTenantException}, and friends) are not individually mapped and
 * currently fall through to the generic 500 handler. Giving the base class an HTTP
 * status would silently change those subclasses from 500 to 409. They are left
 * untouched so behaviour is preserved exactly; mapping them properly is a separate
 * behavioural decision, not a refactor.
 *
 * @since 1.0.0
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends AbstractGlobalExceptionHandler {

    /**
     * Tenant lifecycle / business-rule violation → 409 Conflict.
     *
     * @param ex      the tenant operation failure
     * @param request the current request
     * @return the standard error response
     */
    @ExceptionHandler(TenantOperationException.class)
    public ResponseEntity<ApiError> handleTenantOperation(TenantOperationException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "TENANT_OPERATION_CONFLICT", ex.getMessage(), request);
    }
}
