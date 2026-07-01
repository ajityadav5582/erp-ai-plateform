package com.erp.platform.common.dto;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ResponseWrapper}.
 *
 * @since 1.0.0
 */
class ResponseWrapperTest {

    @Test
    void success_shouldCreateWrapperWithSuccessMeta() {
        ResponseWrapper<String> wrapper = ResponseWrapper.success("data");

        assertThat(wrapper.data()).isEqualTo("data");
        assertThat(wrapper.meta()).isNotNull();
        assertThat(wrapper.meta().requestId()).isNull();
        assertThat(wrapper.meta().timestamp()).isNotNull();
        assertThat(wrapper.meta().traceId()).isNull();
        assertThat(wrapper.meta().spanId()).isNull();
    }

    @Test
    void of_shouldCreateWrapperWithCustomMeta() {
        ResponseWrapper.Meta meta = ResponseWrapper.Meta.of("req-123", Instant.now(), "trace-456", "span-789");
        ResponseWrapper<String> wrapper = ResponseWrapper.of("data", meta);

        assertThat(wrapper.data()).isEqualTo("data");
        assertThat(wrapper.meta()).isEqualTo(meta);
    }

    @Test
    void meta_success_shouldCreateSuccessMeta() {
        ResponseWrapper.Meta meta = ResponseWrapper.Meta.success();

        assertThat(meta.requestId()).isNull();
        assertThat(meta.timestamp()).isNotNull();
        assertThat(meta.traceId()).isNull();
        assertThat(meta.spanId()).isNull();
    }

    @Test
    void meta_of_shouldCreateMetaWithAllFields() {
        Instant now = Instant.now();
        ResponseWrapper.Meta meta = ResponseWrapper.Meta.of("req-123", now, "trace-456", "span-789");

        assertThat(meta.requestId()).isEqualTo("req-123");
        assertThat(meta.timestamp()).isEqualTo(now);
        assertThat(meta.traceId()).isEqualTo("trace-456");
        assertThat(meta.spanId()).isEqualTo("span-789");
    }
}
