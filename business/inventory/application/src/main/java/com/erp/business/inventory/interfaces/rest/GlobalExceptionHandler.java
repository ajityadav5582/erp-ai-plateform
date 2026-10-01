package com.erp.business.inventory.interfaces.rest;

import com.erp.platform.common.exception.AbstractGlobalExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for the Inventory REST API.
 *
 * <p>Inventory previously had no {@code @RestControllerAdvice} at all, so its domain
 * exceptions escaped to Spring's default error handling and the service returned a
 * different error shape than Identity. That was a real inconsistency, not just
 * duplication: the frontend has to parse two different error formats depending on
 * which service answered.
 *
 * <p>All mapping logic is inherited from
 * {@link AbstractGlobalExceptionHandler}, which is the same base Identity uses, so
 * both services now emit an identical {@code ApiError} envelope. Adding a new
 * Inventory exception requires no change here.
 *
 * @since 1.0.0
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends AbstractGlobalExceptionHandler {
}
