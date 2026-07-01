package com.erp.platform.security.annotation;

import java.lang.annotation.*;

/**
 * Annotation to mark a field as containing the tenant ID.
 *
 * <p>Used by security interceptors to automatically populate
 * the tenant ID from the security context.
 *
 * @since 1.0.0
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TenantId {
}
