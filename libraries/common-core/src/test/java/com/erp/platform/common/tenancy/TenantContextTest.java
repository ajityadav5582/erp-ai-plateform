package com.erp.platform.common.tenancy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link TenantContext} and {@link TenantContextHolder}.
 *
 * @since 1.0.0
 */
class TenantContextTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        TenantContextHolder.clear();
    }

    @Test
    void setTenantId_shouldSetTenant() {
        TenantContext.setTenantId("tenant-1");
        assertThat(TenantContext.getTenantId()).isEqualTo("tenant-1");
    }

    @Test
    void getTenantId_shouldReturnNullWhenNotSet() {
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    void clear_shouldRemoveTenant() {
        TenantContext.setTenantId("tenant-1");
        TenantContext.clear();
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    void TenantContextHolder_setTenantId_shouldSetTenant() {
        TenantContextHolder.setTenantId("tenant-1");
        assertThat(TenantContextHolder.getTenantId()).isEqualTo("tenant-1");
    }

    @Test
    void TenantContextHolder_hasTenant_shouldReturnTrueWhenSet() {
        TenantContextHolder.setTenantId("tenant-1");
        assertThat(TenantContextHolder.hasTenant()).isTrue();
    }

    @Test
    void TenantContextHolder_hasTenant_shouldReturnFalseWhenNotSet() {
        assertThat(TenantContextHolder.hasTenant()).isFalse();
    }

    @Test
    void TenantContextHolder_clear_shouldRemoveTenant() {
        TenantContextHolder.setTenantId("tenant-1");
        TenantContextHolder.clear();
        assertThat(TenantContextHolder.getTenantId()).isNull();
    }

    @Test
    void TenantIsolation_isEnabled_shouldReturnFalseWhenNoTenant() {
        assertThat(TenantIsolation.isEnabled()).isFalse();
    }

    @Test
    void TenantIsolation_isEnabled_shouldReturnTrueWhenTenantSet() {
        TenantContext.setTenantId("tenant-1");
        assertThat(TenantIsolation.isEnabled()).isTrue();
    }

    @Test
    void TenantIsolation_requireTenantId_shouldReturnTenantWhenSet() {
        TenantContext.setTenantId("tenant-1");
        assertThat(TenantIsolation.requireTenantId()).isEqualTo("tenant-1");
    }

    @Test
    void TenantIsolation_requireTenantId_shouldThrowWhenNotSet() {
        assertThatThrownBy(TenantIsolation::requireTenantId)
            .isInstanceOf(IllegalStateException.class);
    }
}
