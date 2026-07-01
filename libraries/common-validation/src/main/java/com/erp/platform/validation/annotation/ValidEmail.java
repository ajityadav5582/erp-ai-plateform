package com.erp.platform.validation.annotation;

import com.erp.platform.validation.constraint.EmailValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Validation annotation for email addresses.
 *
 * <p>Validates that the annotated element is a valid email address.
 *
 * @since 1.0.0
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = EmailValidator.class)
public @interface ValidEmail {

    String message() default "Invalid email format";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * Whether null values are considered valid.
     *
     * @return true if null is valid
     */
    boolean allowNull() default false;
}
