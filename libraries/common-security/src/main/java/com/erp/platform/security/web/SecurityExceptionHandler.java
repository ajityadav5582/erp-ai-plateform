package com.erp.platform.security.web;

import com.erp.platform.common.exception.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

/**
 * Turns the Spring Security authorization failures raised by
 * {@link com.erp.platform.security.aspect.PermissionAspect} into the platform's
 * standard {@link ApiError} envelope.
 *
 * <p><strong>Why this exists.</strong> {@code PermissionAspect} throws Spring
 * Security's {@link AccessDeniedException}. Services map their own exceptions in
 * {@code AbstractGlobalExceptionHandler}, but that base class deliberately carries
 * no Spring Security type, because {@code libraries:common-core} is also consumed
 * by the reactive Gateway and must stay off the servlet security stack. Nothing
 * therefore caught {@code AccessDeniedException}, so it propagated past the
 * {@code DispatcherServlet} into {@code ExceptionTranslationFilter}, which writes
 * its own bare {@code 403 Forbidden} with an <em>empty body</em>.
 *
 * <p>The observable symptom was a user reporting "403 Forbidden" with nothing to
 * act on: no message, no error code, no indication of which permission was
 * missing. The information needed to fix it already existed - the aspect's message
 * named the exact permission code - and was simply discarded on the way out. This
 * handler keeps it.
 *
 * <p><strong>Why it lives here and not in {@code common-core}.</strong>
 * {@code common-security} is the library that owns the aspect, and it already
 * depends on both {@code spring-security-core} and {@code common-core}. It is also
 * deliberately absent from the Gateway's dependency list, so nothing defined here
 * can push the servlet stack onto a reactive application.
 *
 * <p><strong>Registration.</strong> Services do not component-scan
 * {@code com.erp.platform.security}, so this advice has to be imported explicitly
 * from each application class, exactly as {@code PermissionAspect} is:
 * <pre>{@code
 * @Import({AuditingConfig.class, PermissionAspect.class, SecurityExceptionHandler.class})
 * }</pre>
 *
 * <p><strong>Why {@link Ordered#HIGHEST_PRECEDENCE}.</strong> Every service's
 * {@code AbstractGlobalExceptionHandler} ends with a catch-all
 * {@code @ExceptionHandler(Exception.class)}, and Spring resolves an exception by
 * walking its advices <em>in order</em> and taking the <em>first</em> one that has
 * any matching method - it does not pick the most specific match across advices.
 * Since this class declares no {@code Exception} handler of its own, claiming the
 * highest precedence cannot make it swallow anything: it intercepts only the two
 * exception types it explicitly lists, and every other failure falls through to
 * the service's own advice exactly as before.
 *
 * <p>Ordering this advice last is the one mistake that makes it silently
 * inert. {@code LOWEST_PRECEDENCE} is also the default for an unannotated advice,
 * so it looks like the safe, deferential choice; in practice the service's catch-all
 * answers first and an authorization refusal is reported as a 500 "An unexpected
 * error occurred". The advice is registered, the exception is thrown, and the
 * message the user should have seen is discarded - which is the same symptom this
 * class was written to fix.
 *
 * @since 1.0.0
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class SecurityExceptionHandler {

    private static final String PERMISSION_PREFIX = "Missing required permission(s): ";
    private static final String ROLE_PREFIX = "Missing required role(s): ";

    /**
     * The caller is authenticated but holds neither the permission nor the role the
     * annotated method requires -> 403, with a message naming what is missing.
     *
     * <p>Rewording the aspect's text matters. "Missing required permission(s):
     * PURCHASE_CREATE" is precise but reads like a log line, and the person seeing
     * it is usually a requester who cannot fix their own access. The message below
     * keeps the permission code - which is what an administrator needs in order to
     * act - while telling the user in words that the request was refused rather
     * than broken.
     *
     * @param ex      the refusal raised by the aspect
     * @param request the current request, used to report the failing path
     * @return the standard error response
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "FORBIDDEN", describe(ex), request);
    }

    /**
     * The caller reached a permission-protected method without being authenticated
     * at all -> 401.
     *
     * <p>Deliberately does <em>not</em> echo the aspect's message. That message is
     * {@code "Authentication required for <java method signature>"}, which names
     * internal implementation and tells the caller nothing actionable.
     *
     * @param ex      the missing-credentials failure
     * @param request the current request
     * @return the standard error response
     */
    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ResponseEntity<ApiError> handleNotAuthenticated(
            AuthenticationCredentialsNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                "You must be signed in to perform this action.", request);
    }

    /**
     * Builds a user-facing sentence from the aspect's refusal.
     *
     * <p>The prefixes are matched rather than assumed. A future caller could raise
     * {@code AccessDeniedException} with no message, or with text that must not be
     * echoed to a client; this still degrades to the generic sentence rather than
     * rendering a null or leaking internals.
     *
     * @param ex the refusal raised by the aspect
     * @return the message to send to the client
     */
    private static String describe(AccessDeniedException ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return genericRefusal();
        }
        if (message.startsWith(PERMISSION_PREFIX)) {
            return "Your role does not grant the permission "
                    + quoted(message.substring(PERMISSION_PREFIX.length()))
                    + " required for this action. Ask an administrator to add it to your role.";
        }
        if (message.startsWith(ROLE_PREFIX)) {
            return "Your account does not hold the role "
                    + quoted(message.substring(ROLE_PREFIX.length()))
                    + " required for this action. Ask an administrator to assign it to you.";
        }
        return genericRefusal();
    }

    private static String genericRefusal() {
        return "You do not have permission to perform this action. "
                + "If you believe this is wrong, ask an administrator to review your role.";
    }

    private static String quoted(String codes) {
        String trimmed = codes.strip();
        return "'" + (trimmed.isEmpty() ? "required" : trimmed) + "'";
    }

    private static ResponseEntity<ApiError> build(HttpStatus status, String code, String message,
                                                  HttpServletRequest request) {
        ApiError error = new ApiError(code, message, request.getRequestURI(), Instant.now());
        return ResponseEntity.status(status).body(error);
    }
}
