package com.erp.platform.validation.util;

import java.util.Collection;
import java.util.Map;

/**
 * Validation utility methods.
 *
 * <p>Provides common validation helpers that can be used
 * in custom validators and business logic.
 *
 * @since 1.0.0
 */
public final class ValidationUtils {

    private ValidationUtils() {
        // Utility class
    }

    /**
     * Checks if a string is null or blank.
     *
     * @param value the string to check
     * @return true if null or blank
     */
    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * Checks if a string is not null and not blank.
     *
     * @param value the string to check
     * @return true if not null and not blank
     */
    public static boolean isNotBlank(String value) {
        return !isBlank(value);
    }

    /**
     * Checks if a collection is null or empty.
     *
     * @param collection the collection to check
     * @return true if null or empty
     */
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * Checks if a collection is not null and not empty.
     *
     * @param collection the collection to check
     * @return true if not null and not empty
     */
    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }

    /**
     * Checks if a map is null or empty.
     *
     * @param map the map to check
     * @return true if null or empty
     */
    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    /**
     * Checks if a map is not null and not empty.
     *
     * @param map the map to check
     * @return true if not null and not empty
     */
    public static boolean isNotEmpty(Map<?, ?> map) {
        return !isEmpty(map);
    }

    /**
     * Checks if a number is positive.
     *
     * @param value the number to check
     * @return true if positive
     */
    public static boolean isPositive(Number value) {
        return value != null && value.doubleValue() > 0;
    }

    /**
     * Checks if a number is negative.
     *
     * @param value the number to check
     * @return true if negative
     */
    public static boolean isNegative(Number value) {
        return value != null && value.doubleValue() < 0;
    }

    /**
     * Checks if a number is zero.
     *
     * @param value the number to check
     * @return true if zero
     */
    public static boolean isZero(Number value) {
        return value != null && value.doubleValue() == 0;
    }
}
