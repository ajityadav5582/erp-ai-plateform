package com.erp.platform.data.optimistic;

import java.lang.annotation.*;

/**
 * Annotation to enable optimistic locking on an entity.
 *
 * <p>When placed on a version field, JPA will use this field
 * for optimistic locking to prevent lost updates.
 *
 * @since 1.0.0
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Versioned {
}
