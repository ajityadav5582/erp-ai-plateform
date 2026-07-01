package com.erp.platform.validation.annotations;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Validates that a numeric value is not negative.
 *
 * <p>This annotation can be used on numeric fields to ensure
 * they are greater than or equal to zero.
 *
 * @since 1.0.0
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {})
public @interface NotNegative {

    /**
     * Error message.
     *
     * @return the error message
     */
    String message() default "{validation.notnegative}";

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
}
