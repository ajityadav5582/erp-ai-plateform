package com.erp.platform.common.dto;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ProblemDetail}.
 *
 * @since 1.0.0
 */
class ProblemDetailTest {

    @Test
    void of_shouldCreateWithMinimalInfo() {
        ProblemDetail detail = ProblemDetail.of("Title", "Detail message", 422);

        assertThat(detail.title()).isEqualTo("Title");
        assertThat(detail.detail()).isEqualTo("Detail message");
        assertThat(detail.status()).isEqualTo(422);
        assertThat(detail.type()).isNull();
        assertThat(detail.instance()).isNull();
        assertThat(detail.requestId()).isNull();
        assertThat(detail.details()).isNull();
        assertThat(detail.fieldErrors()).isNull();
        assertThat(detail.timestamp()).isNotNull();
    }

    @Test
    void of_shouldCreateWithAllFields() {
        ProblemDetail detail = ProblemDetail.of(
            "https://api.example.com/errors/validation",
            "Validation Error",
            "Request validation failed",
            422,
            "/api/v1/invoices",
            "req-123"
        );

        assertThat(detail.type()).isEqualTo("https://api.example.com/errors/validation");
        assertThat(detail.title()).isEqualTo("Validation Error");
        assertThat(detail.detail()).isEqualTo("Request validation failed");
        assertThat(detail.status()).isEqualTo(422);
        assertThat(detail.instance()).isEqualTo("/api/v1/invoices");
        assertThat(detail.requestId()).isEqualTo("req-123");
        assertThat(detail.timestamp()).isNotNull();
    }

    @Test
    void fieldError_shouldBeCreated() {
        ProblemDetail.FieldError fieldError = new ProblemDetail.FieldError("email", "Invalid format", "bad-email");

        assertThat(fieldError.field()).isEqualTo("email");
        assertThat(fieldError.message()).isEqualTo("Invalid format");
        assertThat(fieldError.rejectedValue()).isEqualTo("bad-email");
    }
}
