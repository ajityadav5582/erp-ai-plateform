package com.erp.platform.common.exception;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ErpException} and subclasses.
 *
 * @since 1.0.0
 */
class ErpExceptionTest {

    @Test
    void constructor_shouldSetErrorCodeAndMessage() {
        ErpException exception = new BusinessException("TEST_CODE", "Test message");

        assertThat(exception.getErrorCode()).isEqualTo("TEST_CODE");
        assertThat(exception.getMessage()).isEqualTo("Test message");
        assertThat(exception.getTimestamp()).isNotNull();
    }

    @Test
    void constructor_shouldSetDetails() {
        Map<String, Object> details = Map.of("key", "value");
        ErpException exception = new BusinessException("TEST_CODE", "Test message", details);

        assertThat(exception.getDetails()).isEqualTo(details);
        assertThat(exception.getDetail("key")).isEqualTo("value");
    }

    @Test
    void constructor_shouldSetCause() {
        RuntimeException cause = new RuntimeException("root cause");
        ErpException exception = new BusinessException("TEST_CODE", "Test message", cause);

        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void getDetail_shouldReturnNullForMissingKey() {
        ErpException exception = new BusinessException("TEST_CODE", "Test message");

        assertThat(exception.getDetail("missing")).isNull();
    }

    @Test
    void getDetail_shouldReturnNullWhenDetailsIsNull() {
        ErpException exception = new BusinessException("TEST_CODE", "Test message");

        assertThat(exception.getDetail("key")).isNull();
    }

    @Test
    void businessException_shouldExtendErpException() {
        assertThat(new BusinessException("CODE", "msg")).isInstanceOf(ErpException.class);
    }

    @Test
    void resourceNotFoundException_shouldExtendErpException() {
        assertThat(new ResourceNotFoundException("CODE", "msg")).isInstanceOf(ErpException.class);
    }

    @Test
    void validationException_shouldExtendErpException() {
        assertThat(new ValidationException("CODE", "msg")).isInstanceOf(ErpException.class);
    }

    @Test
    void conflictException_shouldExtendErpException() {
        assertThat(new ConflictException("CODE", "msg")).isInstanceOf(ErpException.class);
    }

    @Test
    void unauthorizedException_shouldExtendErpException() {
        assertThat(new UnauthorizedException("CODE", "msg")).isInstanceOf(ErpException.class);
    }

    @Test
    void forbiddenException_shouldExtendErpException() {
        assertThat(new ForbiddenException("CODE", "msg")).isInstanceOf(ErpException.class);
    }

    @Test
    void erpSystemException_shouldExtendErpException() {
        assertThat(new ErpSystemException("CODE", "msg")).isInstanceOf(ErpException.class);
    }

    @Test
    void timestamp_shouldBeSetToNow() {
        Instant before = Instant.now();
        ErpException exception = new BusinessException("CODE", "msg");
        Instant after = Instant.now();

        assertThat(exception.getTimestamp()).isBetween(before, after);
    }
}
