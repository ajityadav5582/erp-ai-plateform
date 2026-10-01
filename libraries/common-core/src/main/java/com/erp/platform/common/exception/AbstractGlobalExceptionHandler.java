package com.erp.platform.common.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Reusable {@code @RestControllerAdvice} base that maps platform exceptions to the
 * shared {@link ApiError} envelope.
 *
 * <p>Why this exists: the Identity service carried a 253-line
 * {@code GlobalExceptionHandler} containing 20 near-identical methods whose only
 * job was to name an HTTP status and an error code for a given exception type.
 * Now that {@link BaseApiException} carries that information, a single method
 * handles all of them. Adding a new domain exception costs no new handler method.
 *
 * <p>The remaining handlers exist for the failures a client can actually cause and
 * that therefore deserve a specific status and a message naming the offending
 * field, rather than the catch-all {@code 500 INTERNAL_ERROR}. Previously
 * {@code IllegalArgumentException}, {@code ConstraintViolationException} and a
 * failed optimistic-lock flush all fell through to the generic handler and the
 * client received {@code "An unexpected error occurred"} with no indication of
 * what to fix.
 *
 * <p>Deliberately <em>not</em> annotated with {@code @RestControllerAdvice} here.
 * {@code common-core} is also consumed by the reactive Gateway, which has no
 * {@code spring-web} MVC stack and must not pick up servlet exception handling.
 * Concrete services opt in by annotating their own subclass, which keeps the
 * servlet dependency local to services that actually serve HTTP requests.
 *
 * <p>Subclasses are annotated like:
 * <pre>{@code
 * @RestControllerAdvice
 * public class GlobalExceptionHandler extends AbstractGlobalExceptionHandler {}
 * }</pre>
 *
 * @since 1.0.0
 */
@Slf4j
public abstract class AbstractGlobalExceptionHandler {

    /**
     * Handles every {@link BaseApiException} using the status and code it carries.
     *
     * @param ex      the domain exception
     * @param request the current request, used to report the failing path
     * @return the standard error response
     */
    @ExceptionHandler(BaseApiException.class)
    public ResponseEntity<ApiError> handleApiException(BaseApiException ex, jakarta.servlet.http.HttpServletRequest request) {
        if (ex.getStatus().is5xxServerError()) {
            log.error("Server-side failure on {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        } else {
            log.debug("Client error on {}: {}", request.getRequestURI(), ex.getMessage());
        }
        return build(ex.getStatus(), ex.getErrorCode(), ex.getMessage(), request);
    }

    /**
     * Bean validation failure on a {@code @Valid @RequestBody} → 422 Unprocessable
     * Entity, listing every offending field rather than only the first.
     *
     * @param ex      the validation failure
     * @param request the current request
     * @return the standard error response
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, jakarta.servlet.http.HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        if (message.isBlank()) {
            message = "Request validation failed";
        }
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR", message, request);
    }

    /**
     * Bean validation failure on a path or query parameter
     * ({@code @Validated} + {@code @RequestParam}) → 422.
     *
     * @param ex      the constraint violation
     * @param request the current request
     * @return the standard error response
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex, jakarta.servlet.http.HttpServletRequest request) {
        String message = ex.getConstraintViolations().stream()
                .map(AbstractGlobalExceptionHandler::describeViolation)
                .collect(Collectors.joining("; "));
        if (message.isBlank()) {
            message = "Request validation failed";
        }
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR", message, request);
    }

    /**
     * Malformed JSON request body, or a body that cannot be bound to the target
     * type → 400 Bad Request.
     *
     * @param ex      the unreadable body
     * @param request the current request
     * @return the standard error response
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableRequest(HttpMessageNotReadableException ex, jakarta.servlet.http.HttpServletRequest request) {
        // The parser message names the offending field or enum constant, which is
        // exactly what the caller needs; it never contains server internals.
        String detail = rootCauseMessage(ex);
        String message = detail == null || detail.isBlank()
                ? "Request body is malformed or contains invalid JSON"
                : "Request body is malformed or contains invalid JSON: " + detail;
        return build(HttpStatus.BAD_REQUEST, "INVALID_REQUEST_BODY", message, request);
    }

    /**
     * A path variable or query parameter that cannot be converted to its declared
     * type, for example {@code /categories/abc} → 400.
     *
     * @param ex      the conversion failure
     * @param request the current request
     * @return the standard error response
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex, jakarta.servlet.http.HttpServletRequest request) {
        String required = ex.getRequiredType() == null ? "the expected type" : ex.getRequiredType().getSimpleName();
        String message = "Parameter '" + ex.getName() + "' with value '" + ex.getValue()
                + "' could not be converted to " + required;
        return build(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER", message, request);
    }

    /**
     * A required query parameter was not supplied → 400.
     *
     * @param ex      the missing parameter
     * @param request the current request
     * @return the standard error response
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParameter(MissingServletRequestParameterException ex, jakarta.servlet.http.HttpServletRequest request) {
        String message = "Required parameter '" + ex.getParameterName() + "' is missing";
        return build(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER", message, request);
    }

    /**
     * The path exists but not for this HTTP method → 405.
     *
     * @param ex      the method mismatch
     * @param request the current request
     * @return the standard error response
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, jakarta.servlet.http.HttpServletRequest request) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", ex.getMessage(), request);
    }

    /**
     * Domain guard violation such as "a category cannot be its own parent" →
     * 400. Domain methods deliberately throw plain
     * {@code IllegalArgumentException} to stay free of HTTP concerns, so this
     * is where those messages are finally surfaced to the caller.
     *
     * @param ex      the rejected argument
     * @param request the current request
     * @return the standard error response
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex, jakarta.servlet.http.HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", messageOrDefault(ex, "Invalid argument"), request);
    }

    /**
     * Invalid entity state, such as an optimistic-lock version conflict on flush →
     * 409. Reported as a conflict rather than a 500 because it is a concurrency
     * problem the caller can resolve by retrying with fresh data.
     *
     * @param ex      the state conflict
     * @param request the current request
     * @return the standard error response
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleOptimisticLocking(OptimisticLockingFailureException ex, jakarta.servlet.http.HttpServletRequest request) {
        log.debug("Optimistic lock conflict on {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION",
                "This record was modified by another request. Reload it and retry.", request);
    }

    /**
     * A database constraint was violated — most often a unique index the
     * application-level pre-check did not catch, such as two concurrent requests
     * inserting the same slug → 409 with the constraint named.
     *
     * @param ex      the constraint violation
     * @param request the current request
     * @return the standard error response
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex, jakarta.servlet.http.HttpServletRequest request) {
        log.warn("Data integrity violation on {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, "CONSTRAINT_VIOLATION",
                "The request conflicts with an existing record or violates a data constraint", request);
    }

    /**
     * Any other unexpected error → 500 Internal Server Error.
     *
     * <p>Kept last and broad on purpose: it is the safety net that prevents an
     * unhandled exception from leaking a stack trace to the client. The stack
     * trace goes to the log, not to the response.
     *
     * @param ex      the unexpected error
     * @param request the current request
     * @return the standard error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, jakarta.servlet.http.HttpServletRequest request) {
        log.error("Unexpected error processing {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred while processing " + request.getMethod()
                        + " " + request.getRequestURI(), request);
    }

    /**
     * Builds the standard error response.
     *
     * @param status  the HTTP status
     * @param code    the machine-readable error code
     * @param message the human-readable message
     * @param request the current request
     * @return the standard error response
     */
    protected ResponseEntity<ApiError> build(HttpStatus status, String code, String message,
                                             jakarta.servlet.http.HttpServletRequest request) {
        ApiError error = new ApiError(code, message, request.getRequestURI(), Instant.now());
        return ResponseEntity.status(status).body(error);
    }

    /**
     * Renders a single constraint violation as {@code path: message}, trimming
     * the {@code method.argument} prefix Spring adds by default because it
     * identifies internals rather than the request field the caller controls.
     *
     * @param violation the violation to describe
     * @return the human-readable description
     */
    private static String describeViolation(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath() == null ? "" : violation.getPropertyPath().toString();
        int lastDot = path.lastIndexOf('.');
        if (lastDot >= 0 && lastDot < path.length() - 1) {
            path = path.substring(lastDot + 1);
        }
        String message = violation.getMessage();
        return path.isBlank() ? message : path + ": " + message;
    }

    /**
     * Returns the exception's message, or a fallback when it carries none.
     *
     * @param ex       the exception
     * @param fallback the message to use when {@code ex} has none
     * @return a non-null, non-blank message safe to return to the client
     */
    private static String messageOrDefault(Exception ex, String fallback) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? fallback : message;
    }

    /**
     * Walks to the innermost cause, since Jackson wraps parse failures several
     * levels deep and the useful text lives at the bottom.
     *
     * @param ex the exception to unwrap
     * @return the root cause message, or null if there is none
     */
    private static String rootCauseMessage(Throwable ex) {
        Throwable current = ex;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current == ex ? null : current.getMessage();
    }
}
