package com.erp.platform.validation.annotation;

import com.erp.platform.validation.constraint.PhoneValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Validation annotation for phone numbers.
 *
 * <p>Validates that the annotated element is a valid phone number.
 *
 * @since 1.0.0
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = PhoneValidator.class)
public @interface ValidPhone {

    String message() default "Invalid phone format";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * Whether null values are considered valid.
     *
     * @return true if null is valid
     */
    boolean allowNull() default false;
}
