package com.erp.platform.data.lock;

import org.junit.jupiter.api.Test;

import java.io.Serializable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OptimisticLock}.
 *
 * @since 1.0.0
 */
class OptimisticLockTest {

    @Test
    void getVersion_shouldReturnNullWhenNotSet() {
        TestEntity entity = new TestEntity();
        assertThat(entity.getVersion()).isNull();
    }

    @Test
    void setVersion_shouldSetVersion() {
        TestEntity entity = new TestEntity();
        entity.setVersion(1);

        assertThat(entity.getVersion()).isEqualTo(1);
    }

    @Test
    void incrementVersion_shouldIncrementFromNull() {
        TestEntity entity = new TestEntity();
        entity.incrementVersion();

        assertThat(entity.getVersion()).isEqualTo(1);
    }

    @Test
    void incrementVersion_shouldIncrementFromExisting() {
        TestEntity entity = new TestEntity();
        entity.setVersion(5);
        entity.incrementVersion();

        assertThat(entity.getVersion()).isEqualTo(6);
    }

    private static class TestEntity extends OptimisticLock<Long> implements Serializable {
        private static final long serialVersionUID = 1L;
    }
}
