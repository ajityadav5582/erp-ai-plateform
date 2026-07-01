package com.erp.platform.common.constants;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link CommonConstants}.
 *
 * @since 1.0.0
 */
class CommonConstantsTest {

    @Test
    void defaultValues_shouldBeCorrect() {
        assertThat(CommonConstants.DEFAULT_PAGE_SIZE).isEqualTo(20);
        assertThat(CommonConstants.MAX_PAGE_SIZE).isEqualTo(100);
        assertThat(CommonConstants.DEFAULT_PAGE_NUMBER).isEqualTo(0);
        assertThat(CommonConstants.DEFAULT_SORT_PROPERTY).isEqualTo("createdAt");
        assertThat(CommonConstants.DEFAULT_SORT_DIRECTION).isEqualTo("DESC");
    }

    @Test
    void tenantConstants_shouldBeDefined() {
        assertThat(CommonConstants.TENANT_HEADER).isEqualTo("X-Tenant-ID");
        assertThat(CommonConstants.DEFAULT_TENANT_ID).isEqualTo("1");
    }

    @Test
    void requestContextConstants_shouldBeDefined() {
        assertThat(CommonConstants.REQUEST_ID_HEADER).isEqualTo("X-Request-ID");
        assertThat(CommonConstants.CORRELATION_ID_HEADER).isEqualTo("X-Correlation-ID");
    }

    @Test
    void securityConstants_shouldBeDefined() {
        assertThat(CommonConstants.AUTHORIZATION_HEADER).isEqualTo("Authorization");
        assertThat(CommonConstants.BEARER_PREFIX).isEqualTo("Bearer ");
        assertThat(CommonConstants.USER_ID_CLAIM).isEqualTo("sub");
        assertThat(CommonConstants.TENANT_ID_CLAIM).isEqualTo("tenant_id");
        assertThat(CommonConstants.ROLES_CLAIM).isEqualTo("roles");
    }

    @Test
    void errorCodes_shouldBeDefined() {
        assertThat(CommonConstants.ERROR_VALIDATION).isEqualTo("VALIDATION_ERROR");
        assertThat(CommonConstants.ERROR_NOT_FOUND).isEqualTo("NOT_FOUND");
        assertThat(CommonConstants.ERROR_CONFLICT).isEqualTo("CONFLICT");
        assertThat(CommonConstants.ERROR_UNAUTHORIZED).isEqualTo("UNAUTHORIZED");
        assertThat(CommonConstants.ERROR_FORBIDDEN).isEqualTo("FORBIDDEN");
        assertThat(CommonConstants.ERROR_INTERNAL).isEqualTo("INTERNAL_ERROR");
        assertThat(CommonConstants.ERROR_BUSINESS).isEqualTo("BUSINESS_ERROR");
    }

    @Test
    void regexPatterns_shouldBeDefined() {
        assertThat(CommonConstants.PATTERN_EMAIL).isEqualTo("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
        assertThat(CommonConstants.PATTERN_PHONE).isEqualTo("^[+]?[0-9]{10,15}$");
        assertThat(CommonConstants.PATTERN_UUID).isEqualTo("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    }
}
