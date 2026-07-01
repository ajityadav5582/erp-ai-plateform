package com.erp.platform.common.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RequestContext}.
 *
 * @since 1.0.0
 */
class RequestContextTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void setAndGetRequestId_shouldWork() {
        RequestContext.setRequestId("req-123");

        assertThat(RequestContext.getRequestId()).isEqualTo("req-123");
    }

    @Test
    void setAndGetCorrelationId_shouldWork() {
        RequestContext.setCorrelationId("corr-456");

        assertThat(RequestContext.getCorrelationId()).isEqualTo("corr-456");
    }

    @Test
    void setAndGetUserId_shouldWork() {
        RequestContext.setUserId("user-789");

        assertThat(RequestContext.getUserId()).isEqualTo("user-789");
    }

    @Test
    void setAndGetClientIp_shouldWork() {
        RequestContext.setClientIp("192.168.1.1");

        assertThat(RequestContext.getClientIp()).isEqualTo("192.168.1.1");
    }

    @Test
    void setAndGetUserAgent_shouldWork() {
        RequestContext.setUserAgent("Mozilla/5.0");

        assertThat(RequestContext.getUserAgent()).isEqualTo("Mozilla/5.0");
    }

    @Test
    void setAndGetAttribute_shouldWork() {
        RequestContext.setAttribute("customKey", "customValue");

        assertThat(RequestContext.getAttribute("customKey")).isEqualTo("customValue");
    }

    @Test
    void getAttribute_shouldReturnNullForMissingKey() {
        assertThat(RequestContext.getAttribute("missing")).isNull();
    }

    @Test
    void getAttributes_shouldReturnUnmodifiableMap() {
        RequestContext.setAttribute("key1", "value1");

        var attributes = RequestContext.getHolder().getAttributes();

        assertThat(attributes).containsEntry("key1", "value1");
    }

    @Test
    void clear_shouldRemoveAllContext() {
        RequestContext.setRequestId("req-123");
        RequestContext.setCorrelationId("corr-456");
        RequestContext.setUserId("user-789");
        RequestContext.setAttribute("key", "value");

        RequestContext.clear();

        assertThat(RequestContext.getRequestId()).isNull();
        assertThat(RequestContext.getCorrelationId()).isNull();
        assertThat(RequestContext.getUserId()).isNull();
        assertThat(RequestContext.getAttribute("key")).isNull();
    }

    @Test
    void setAttribute_shouldThrowForNullKey() {
        assertThatThrownBy(() -> RequestContext.setAttribute(null, "value"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getAttribute_shouldThrowForNullKey() {
        assertThatThrownBy(() -> RequestContext.getAttribute(null))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
