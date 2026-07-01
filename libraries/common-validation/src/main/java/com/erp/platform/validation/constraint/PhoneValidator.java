package com.erp.platform.validation.constraint;

import com.erp.platform.validation.annotation.ValidPhone;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for phone numbers.
 *
 * @since 1.0.0
 */
public class PhoneValidator implements ConstraintValidator<ValidPhone, String> {

    private static final String PHONE_PATTERN = "^[+]?[0-9]{10,15}$";
    private boolean allowNull;

    @Override
    public void initialize(ValidPhone constraintAnnotation) {
        this.allowNull = constraintAnnotation.allowNull();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return allowNull;
        }
        String digits = value.replaceAll("[\\s\\-()]", "");
        return digits.matches(PHONE_PATTERN);
    }
}
