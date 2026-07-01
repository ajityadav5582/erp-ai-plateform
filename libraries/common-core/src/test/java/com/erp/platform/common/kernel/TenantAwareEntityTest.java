package com.erp.platform.common.kernel;

import org.junit.jupiter.api.Test;

import java.io.Serializable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TenantAwareEntity}.
 *
 * @since 1.0.0
 */
class TenantAwareEntityTest {

    @Test
    void belongsToTenant_shouldReturnTrueForMatchingTenant() {
        TestEntity entity = new TestEntity();
        entity.setTenantId(1L);

        assertThat(entity.belongsToTenant(1L)).isTrue();
    }

    @Test
    void belongsToTenant_shouldReturnFalseForDifferentTenant() {
        TestEntity entity = new TestEntity();
        entity.setTenantId(1L);

        assertThat(entity.belongsToTenant(2L)).isFalse();
    }

    @Test
    void belongsToTenant_shouldReturnFalseForNullTenantId() {
        TestEntity entity = new TestEntity();

        assertThat(entity.belongsToTenant(1L)).isFalse();
    }

    @Test
    void getTenantId_shouldReturnSetValue() {
        TestEntity entity = new TestEntity();
        entity.setTenantId(1L);

        assertThat(entity.getTenantId()).isEqualTo(1L);
    }

    @Test
    void getTenantId_shouldReturnNullWhenNotSet() {
        TestEntity entity = new TestEntity();
        assertThat(entity.getTenantId()).isNull();
    }

    private static class TestEntity extends TenantAwareEntity<Long> implements Serializable {
        private static final long serialVersionUID = 1L;
    }
}
