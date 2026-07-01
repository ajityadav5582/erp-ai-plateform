package com.erp.platform.validation.annotations;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Validates that a string is not null and not blank (after trimming).
 *
 * <p>This annotation is a custom implementation of the NotBlank constraint
 * that can be used across the platform.
 *
 * @since 1.0.0
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {})
public @interface NotBlank {

    /**
     * Error message.
     *
     * @return the error message
     */
    String message() default "{validation.notblank}";

    /**
     * Validation groups.
     *
     * @return the validation groups
     */
    Class<?>[] groups() default {};

    /**
     * Payload.
     *
     * @return the payload
     */
    Class<? extends Payload>[] payload() default {};

    /**
     * Whether to trim the value before validation.
     *
     * @return true to trim before validation
     */
    boolean trim() default true;
}
