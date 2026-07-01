package com.erp.platform.testing.assertions;

import org.assertj.core.api.AbstractAssert;

import java.util.UUID;

/**
 * Common assertions for the ERP AI Platform.
 *
 * <p>This class provides custom assertions for common
 * platform types.
 *
 * @since 1.0.0
 */
public class CommonAssertions {

    private CommonAssertions() {
        // Utility class
    }

    /**
     * Creates an assertion for UUID.
     *
     * @param actual the actual UUID
     * @return the assertion
     */
    public static UuidAssert assertThat(UUID actual) {
        return new UuidAssert(actual);
    }

    /**
     * Assertion for UUID.
     */
    public static class UuidAssert extends AbstractAssert<UuidAssert, UUID> {

        protected UuidAssert(UUID actual) {
            super(actual, UuidAssert.class);
        }

        /**
         * Verifies that the UUID is not null.
         *
         * @return this assertion
         */
        public UuidAssert isNotNull() {
            super.isNotNull();
            return this;
        }

        /**
         * Verifies that the UUID is not the nil UUID.
         *
         * @return this assertion
         */
        public UuidAssert isNotNil() {
            if (actual.equals(UUID.fromString("00000000-0000-0000-0000-000000000000"))) {
                failWithMessage("Expected UUID not to be nil");
            }
            return this;
        }
    }
}
