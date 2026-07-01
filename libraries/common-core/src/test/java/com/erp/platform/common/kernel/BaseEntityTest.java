package com.erp.platform.common.kernel;

import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link BaseEntity}.
 *
 * @since 1.0.0
 */
class BaseEntityTest {

    @Test
    void hasId_shouldReturnFalseWhenIdIsNull() {
        TestEntity entity = new TestEntity();
        assertThat(entity.hasId()).isFalse();
    }

    @Test
    void hasId_shouldReturnTrueWhenIdIsSet() {
        TestEntity entity = new TestEntity();
        entity.setId(1L);
        assertThat(entity.hasId()).isTrue();
    }

    @Test
    void equals_shouldBeTrueForSameId() {
        TestEntity entity1 = new TestEntity();
        entity1.setId(1L);

        TestEntity entity2 = new TestEntity();
        entity2.setId(1L);

        assertThat(entity1).isEqualTo(entity2);
        assertThat(entity1.hashCode()).isEqualTo(entity2.hashCode());
    }

    @Test
    void equals_shouldBeFalseForDifferentId() {
        TestEntity entity1 = new TestEntity();
        entity1.setId(1L);

        TestEntity entity2 = new TestEntity();
        entity2.setId(2L);

        assertThat(entity1).isNotEqualTo(entity2);
    }

    @Test
    void equals_shouldBeFalseForNull() {
        TestEntity entity = new TestEntity();
        assertThat(entity).isNotEqualTo(null);
    }

    @Test
    void equals_shouldBeTrueForSameInstance() {
        TestEntity entity = new TestEntity();
        assertThat(entity).isEqualTo(entity);
    }

    @Test
    void toString_shouldContainFields() {
        TestEntity entity = new TestEntity();
        entity.setId(1L);
        entity.setCreatedAt(Instant.parse("2024-01-15T10:00:00Z"));
        entity.setCreatedBy("user-1");

        String toString = entity.toString();

        assertThat(toString).contains("id=1");
        assertThat(toString).contains("createdAt=2024-01-15T10:00:00Z");
        assertThat(toString).contains("createdBy='user-1'");
    }

    @Test
    void settersAndGetters_shouldWork() {
        TestEntity entity = new TestEntity();
        Instant now = Instant.now();

        entity.setId(1L);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setCreatedBy("user-1");
        entity.setUpdatedBy("user-2");

        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getCreatedAt()).isEqualTo(now);
        assertThat(entity.getUpdatedAt()).isEqualTo(now);
        assertThat(entity.getCreatedBy()).isEqualTo("user-1");
        assertThat(entity.getUpdatedBy()).isEqualTo("user-2");
    }

    private static class TestEntity extends BaseEntity<Long> implements Serializable {
        private static final long serialVersionUID = 1L;
    }
}
