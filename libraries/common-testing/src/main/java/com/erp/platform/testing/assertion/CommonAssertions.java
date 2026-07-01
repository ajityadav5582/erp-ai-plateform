package com.erp.platform.testing.assertion;

import com.erp.platform.common.dto.Result;
import org.assertj.core.api.AbstractAssert;

/**
 * Common assertions for the ERP platform.
 *
 * <p>Provides custom assertions for platform-specific types.
 *
 * @since 1.0.0
 */
public class CommonAssertions {

    private CommonAssertions() {
        // Utility class
    }

    /**
     * Creates an assertion for Result objects.
     *
     * @param result the result to assert
     * @param <T> the result type
     * @return the assertion
     */
    public static <T> ResultAssert<T> assertThat(Result<T> result) {
        return new ResultAssert<>(result);
    }

    /**
     * Assertion for Result objects.
     *
     * @param <T> the result type
     */
    public static class ResultAssert<T> extends AbstractAssert<ResultAssert<T>, Result<T>> {

        protected ResultAssert(Result<T> result) {
            super(result, ResultAssert.class);
        }

        public ResultAssert<T> isSuccess() {
            isNotNull();
            if (!actual.isSuccess()) {
                failWithMessage("Expected result to be success but was failure: %s", actual.getError());
            }
            return this;
        }

        public ResultAssert<T> isFailure() {
            isNotNull();
            if (!actual.isFailure()) {
                failWithMessage("Expected result to be failure but was success");
            }
            return this;
        }

        public ResultAssert<T> hasErrorCode(String errorCode) {
            isNotNull();
            isFailure();
            if (!errorCode.equals(actual.getError().errorCode())) {
                failWithMessage("Expected error code <%s> but was <%s>",
                    errorCode, actual.getError().errorCode());
            }
            return this;
        }

        public ResultAssert<T> hasValue(T value) {
            isNotNull();
            isSuccess();
            if (!actual.getValue().equals(value)) {
                failWithMessage("Expected value <%s> but was <%s>", value, actual.getValue());
            }
            return this;
        }
    }
}
