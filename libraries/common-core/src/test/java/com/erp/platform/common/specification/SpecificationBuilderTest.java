package com.erp.platform.common.specification;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SpecificationBuilder}.
 *
 * @since 1.0.0
 */
class SpecificationBuilderTest {

    @Test
    void build_shouldReturnAlwaysSpecificationWhenEmpty() {
        SpecificationBuilder<TestEntity> builder = new SpecificationBuilder<>();
        Specification<TestEntity> spec = builder.build();

        assertThat(spec).isEqualTo(Specification.always());
    }

    @Test
    void filter_shouldAddFilterCriterion() {
        SpecificationBuilder<TestEntity> builder = new SpecificationBuilder<>();
        builder.filter("status", Filter.Operator.EQUALS, "active");

        Specification<TestEntity> spec = builder.build();

        assertThat(spec.isSatisfiedBy(new TestEntity("active"))).isTrue();
        assertThat(spec.isSatisfiedBy(new TestEntity("inactive"))).isFalse();
    }

    @Test
    void filter_shouldSupportMultipleFilters() {
        SpecificationBuilder<TestEntity> builder = new SpecificationBuilder<>();
        builder.filter("status", Filter.Operator.EQUALS, "active");
        builder.filter("verified", Filter.Operator.EQUALS, true);

        Specification<TestEntity> spec = builder.build();

        assertThat(spec.isSatisfiedBy(new TestEntity("active", true))).isTrue();
        assertThat(spec.isSatisfiedBy(new TestEntity("active", false))).isFalse();
        assertThat(spec.isSatisfiedBy(new TestEntity("inactive", true))).isFalse();
    }

    @Test
    void filter_shouldSupportInOperator() {
        SpecificationBuilder<TestEntity> builder = new SpecificationBuilder<>();
        builder.filter("status", Filter.Operator.IN, List.of("active", "pending"));

        Specification<TestEntity> spec = builder.build();

        assertThat(spec.isSatisfiedBy(new TestEntity("active"))).isTrue();
        assertThat(spec.isSatisfiedBy(new TestEntity("pending"))).isTrue();
        assertThat(spec.isSatisfiedBy(new TestEntity("inactive"))).isFalse();
    }

    @Test
    void filter_shouldSupportLikeOperator() {
        SpecificationBuilder<TestEntity> builder = new SpecificationBuilder<>();
        builder.filter("name", Filter.Operator.LIKE, "John%");

        Specification<TestEntity> spec = builder.build();

        assertThat(spec.isSatisfiedBy(new TestEntity("John Doe"))).isTrue();
        assertThat(spec.isSatisfiedBy(new TestEntity("Jane Doe"))).isFalse();
    }

    @Test
    void filter_shouldSupportIsNullOperator() {
        SpecificationBuilder<TestEntity> builder = new SpecificationBuilder<>();
        builder.filter("deletedAt", Filter.Operator.IS_NULL, null);

        Specification<TestEntity> spec = builder.build();

        assertThat(spec.isSatisfiedBy(new TestEntity("active", true, null))).isTrue();
        assertThat(spec.isSatisfiedBy(new TestEntity("active", true, Instant.now()))).isFalse();
    }

    @Test
    void filter_shouldSupportBetweenOperator() {
        SpecificationBuilder<TestEntity> builder = new SpecificationBuilder<>();
        builder.filter("age", Filter.Operator.BETWEEN, List.of(18, 65));

        Specification<TestEntity> spec = builder.build();

        assertThat(spec.isSatisfiedBy(new TestEntity("active", true, null, 25))).isTrue();
        assertThat(spec.isSatisfiedBy(new TestEntity("active", true, null, 10))).isFalse();
        assertThat(spec.isSatisfiedBy(new TestEntity("active", true, null, 70))).isFalse();
    }

    private record TestEntity(String status, boolean verified, java.time.Instant deletedAt, Integer age) {
        TestEntity(String status) {
            this(status, false, null, null);
        }
        TestEntity(String status, boolean verified) {
            this(status, verified, null, null);
        }
        TestEntity(String status, boolean verified, java.time.Instant deletedAt) {
            this(status, verified, deletedAt, null);
        }
    }
}
