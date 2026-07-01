package com.erp.platform.common.result;

import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link Result}.
 *
 * @since 1.0.0
 */
class ResultTest {

    @Test
    void success_shouldCreateSuccessfulResult() {
        Result<String> result = Result.success("value");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.isFailure()).isFalse();
        assertThat(result.getValue()).isEqualTo("value");
        assertThat(result.getError()).isNull();
    }

    @Test
    void success_handle_shouldInvokeOnSuccess() {
        Result<String> result = Result.success("value");

        String handled = result.handle(
            v -> "success:" + v,
            e -> "error:" + e.errorCode()
        );

        assertThat(handled).isEqualTo("success:value");
    }

    @Test
    void success_map_shouldTransformValue() {
        Result<String> result = Result.success("value");

        Result<Integer> mapped = result.map(String::length);

        assertThat(mapped.isSuccess()).isTrue();
        assertThat(mapped.getValue()).isEqualTo(5);
    }

    @Test
    void success_flatMap_shouldTransformValue() {
        Result<String> result = Result.success("value");

        Result<Integer> flatMapped = result.flatMap(v -> Result.success(v.length()));

        assertThat(flatMapped.isSuccess()).isTrue();
        assertThat(flatMapped.getValue()).isEqualTo(5);
    }

    @Test
    void failure_shouldCreateFailedResult() {
        Result.ErrorDetail error = new Result.ErrorDetail("ERROR_CODE", "Error message");
        Result<String> result = Result.failure(error);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo(error);
    }

    @Test
    void failure_shouldCreateFailedResultFromCodeAndMessage() {
        Result<String> result = Result.failure("ERROR_CODE", "Error message");

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().errorCode()).isEqualTo("ERROR_CODE");
        assertThat(result.getError().message()).isEqualTo("Error message");
    }

    @Test
    void failure_getValue_shouldThrow() {
        Result<String> result = Result.failure("ERROR_CODE", "Error message");

        assertThatThrownBy(result::getValue)
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void failure_handle_shouldInvokeOnFailure() {
        Result<String> result = Result.failure("ERROR_CODE", "Error message");

        String handled = result.handle(
            v -> "success:" + v,
            e -> "error:" + e.errorCode()
        );

        assertThat(handled).isEqualTo("error:ERROR_CODE");
    }

    @Test
    void failure_map_shouldReturnFailure() {
        Result<String> result = Result.failure("ERROR_CODE", "Error message");

        Result<Integer> mapped = result.map(String::length);

        assertThat(mapped.isFailure()).isTrue();
        assertThat(mapped.getError().errorCode()).isEqualTo("ERROR_CODE");
    }

    @Test
    void failure_flatMap_shouldReturnFailure() {
        Result<String> result = Result.failure("ERROR_CODE", "Error message");

        Result<Integer> flatMapped = result.flatMap(v -> Result.success(v.length()));

        assertThat(flatMapped.isFailure()).isTrue();
        assertThat(flatMapped.getError().errorCode()).isEqualTo("ERROR_CODE");
    }

    @Test
    void errorDetail_shouldCreateWithCodeAndMessage() {
        Result.ErrorDetail error = new Result.ErrorDetail("CODE", "message");

        assertThat(error.errorCode()).isEqualTo("CODE");
        assertThat(error.message()).isEqualTo("message");
        assertThat(error.details()).isNull();
    }

    @Test
    void errorDetail_shouldCreateWithDetails() {
        java.util.Map<String, Object> details = java.util.Map.of("key", "value");
        Result.ErrorDetail error = new Result.ErrorDetail("CODE", "message", details);

        assertThat(error.errorCode()).isEqualTo("CODE");
        assertThat(error.message()).isEqualTo("message");
        assertThat(error.details()).isEqualTo(details);
    }

    @Test
    void errorDetail_of_shouldCreateFromException() {
        Exception exception = new RuntimeException("test error");
        Result.ErrorDetail error = Result.ErrorDetail.of(exception);

        assertThat(error.errorCode()).isEqualTo("RuntimeException");
        assertThat(error.message()).isEqualTo("test error");
    }

    @Test
    void errorDetail_of_shouldCreateFromNullMessage() {
        Exception exception = new RuntimeException();
        Result.ErrorDetail error = Result.ErrorDetail.of(exception);

        assertThat(error.errorCode()).isEqualTo("RuntimeException");
        assertThat(error.message()).isEqualTo("Unknown error");
    }

    @Test
    void errorDetail_of_shouldCreateWithAllFields() {
        java.util.Map<String, Object> details = java.util.Map.of("key", "value");
        Result.ErrorDetail error = Result.ErrorDetail.of("CODE", "message", details);

        assertThat(error.errorCode()).isEqualTo("CODE");
        assertThat(error.message()).isEqualTo("message");
        assertThat(error.details()).isEqualTo(details);
    }

    @Test
    void from_shouldWrapSuccessfulSupplier() {
        Result<String> result = Result.from(() -> "value");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo("value");
    }

    @Test
    void from_shouldWrapFailingSupplier() {
        Result<String> result = Result.from(() -> {
            throw new RuntimeException("test");
        });

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().errorCode()).isEqualTo("RuntimeException");
    }
}
