package com.erp.business.procurement.interfaces.rest;

import com.erp.platform.common.exception.AbstractGlobalExceptionHandler;
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
 * Global exception handler for the Procurement REST API.
 *
 * <p><strong>Why this class exists at all.</strong> Procurement had no
 * {@code @RestControllerAdvice}, so every failure escaped to Spring's default
 * error handling and the service answered with a different shape than Identity
 * and Inventory. That is not cosmetic: the frontend has to parse two different
 * error formats depending on which service replied, and
 * {@code getApiErrorMessage(...)} has nothing to show the user.
 *
 * <p>Most mapping is inherited from {@link AbstractGlobalExceptionHandler}, the
 * same base Identity and Inventory use, so all three services emit an identical
 * {@code ApiError} envelope.
 *
 * <p><strong>Why the handlers below are not inherited.</strong> The procurement
 * module predates the {@code BaseApiException} convention and signals its
 * failures with the older platform types -
 * {@link ValidationException}, {@link ResourceNotFoundException},
 * {@link UnauthorizedException} - which extend {@code ErpException} directly and
 * therefore carry no HTTP status. The base class only has a handler for
 * {@code BaseApiException}, so without these methods every one of them would fall
 * through to the catch-all {@code Exception} handler and be reported as a 500
 * with the message "An unexpected error occurred". Telling a user that a
 * missing purchase order is an internal server error, or that they are not the
 * assigned approver is a 500, is exactly the kind of defect that is reported as
 * "the API is broken" rather than "the request was refused".
 *
 * <p>{@link IllegalStateException} is handled for the same reason. The
 * requisition aggregate throws it from every lifecycle guard - "is CANCELLED and
 * cannot be submitted", "already APPROVED", "would exceed the approved
 * quantity" - and each of those is a client-correctable conflict, not a server
 * fault. It is reported as 409 so the client can reload and act, rather than
 * 500 so it alerts someone.
 *
 * @since 1.0.0
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends AbstractGlobalExceptionHandler {

    /**
     * A referenced document does not exist in the caller's tenant and company →
     * 404.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<com.erp.platform.common.exception.ApiError> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND",
                messageOr(ex, "The requested resource was not found"), request);
    }

    /**
     * A business rule rejected the request → 422, matching the status the base
     * handler already uses for bean-validation failures.
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<com.erp.platform.common.exception.ApiError> handleBusinessValidation(
            ValidationException ex, HttpServletRequest request) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR",
                messageOr(ex, "The request violates a business rule"), request);
    }

    /**
     * General ERP domain exception → 422 Unprocessable Entity with error code and detail.
     */
    @ExceptionHandler(ErpException.class)
    public ResponseEntity<com.erp.platform.common.exception.ApiError> handleErpException(
            ErpException ex, HttpServletRequest request) {
        String code = ex.getErrorCode() != null ? ex.getErrorCode() : "VALIDATION_ERROR";
        return build(HttpStatus.UNPROCESSABLE_ENTITY, code,
                messageOr(ex, "A business validation rule failed"), request);
    }

    /**
     * The caller is not authenticated, or is authenticated but is not the party
     * entitled to act → 401.
     *
     * <p>Approval assignment failures land here: "only the assigned approver may
     * act" and "this step is assigned to a role the acting user does not hold"
     * are both authorisation refusals on an authenticated request.
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<com.erp.platform.common.exception.ApiError> handleUnauthorized(
            UnauthorizedException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                messageOr(ex, "You are not authorised to perform this action"), request);
    }

    /**
     * The caller is authenticated but forbidden → 403.
     */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<com.erp.platform.common.exception.ApiError> handleForbidden(
            ForbiddenException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "FORBIDDEN",
                messageOr(ex, "You do not have permission to perform this action"), request);
    }

    /**
     * An aggregate lifecycle guard was hit → 409 Conflict.
     *
     * <p>Examples: submitting a requisition that is already CANCELLED, approving
     * a step that already carries a decision, or awarding a quantity above what
     * was approved. Each names the offending document in its message, so the
     * detail is safe and useful to return.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<com.erp.platform.common.exception.ApiError> handleIllegalState(
            IllegalStateException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "INVALID_STATE",
                messageOr(ex, "The operation is not allowed in the current state"), request);
    }

    /**
     * @param ex       the failure, which may carry no message
     * @param fallback the text to use when it does not
     * @return a non-blank message safe to send to the client
     */
    private static String messageOr(Exception ex, String fallback) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? fallback : message;
    }
}
