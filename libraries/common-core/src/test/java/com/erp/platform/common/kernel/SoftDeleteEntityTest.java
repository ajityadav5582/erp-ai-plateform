package com.erp.platform.common.kernel;

import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SoftDeleteEntity}.
 *
 * @since 1.0.0
 */
class SoftDeleteEntityTest {

    @Test
    void isDeleted_shouldReturnFalseWhenNotDeleted() {
        TestEntity entity = new TestEntity();
        assertThat(entity.isDeleted()).isFalse();
    }

    @Test
    void isDeleted_shouldReturnTrueWhenDeleted() {
        TestEntity entity = new TestEntity();
        entity.markDeleted("user-1");
        assertThat(entity.isDeleted()).isTrue();
    }

    @Test
    void markDeleted_shouldSetDeletedAtAndDeletedBy() {
        TestEntity entity = new TestEntity();
        Instant before = Instant.now();
        entity.markDeleted("user-1");
        Instant after = Instant.now();

        assertThat(entity.getDeletedAt()).isBetween(before, after);
        assertThat(entity.getDeletedBy()).isEqualTo("user-1");
    }

    @Test
    void restore_shouldClearDeletedFields() {
        TestEntity entity = new TestEntity();
        entity.markDeleted("user-1");
        entity.restore();

        assertThat(entity.isDeleted()).isFalse();
        assertThat(entity.getDeletedAt()).isNull();
        assertThat(entity.getDeletedBy()).isNull();
    }

    @Test
    void getDeletedAt_shouldReturnNullWhenNotDeleted() {
        TestEntity entity = new TestEntity();
        assertThat(entity.getDeletedAt()).isNull();
    }

    @Test
    void getDeletedBy_shouldReturnNullWhenNotDeleted() {
        TestEntity entity = new TestEntity();
        assertThat(entity.getDeletedBy()).isNull();
    }

    private static class TestEntity extends SoftDeleteEntity<Long> implements Serializable {
        private static final long serialVersionUID = 1L;
    }
}
