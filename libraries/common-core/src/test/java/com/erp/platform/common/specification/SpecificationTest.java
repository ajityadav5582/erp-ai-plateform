package com.erp.platform.common.specification;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Specification}.
 *
 * @since 1.0.0
 */
class SpecificationTest {

    @Test
    void always_shouldReturnTrueForAllEntities() {
        Specification<TestEntity> spec = Specification.always();

        assertThat(spec.isSatisfiedBy(new TestEntity("active"))).isTrue();
        assertThat(spec.isSatisfiedBy(new TestEntity("inactive"))).isTrue();
    }

    @Test
    void never_shouldReturnFalseForAllEntities() {
        Specification<TestEntity> spec = Specification.never();

        assertThat(spec.isSatisfiedBy(new TestEntity("active"))).isFalse();
    }

    @Test
    void and_shouldCombineSpecifications() {
        Specification<TestEntity> activeSpec = e -> e.status.equals("active");
        Specification<TestEntity> verifiedSpec = e -> e.verified;

        Specification<TestEntity> combined = activeSpec.and(verifiedSpec);

        assertThat(combined.isSatisfiedBy(new TestEntity("active", true))).isTrue();
        assertThat(combined.isSatisfiedBy(new TestEntity("active", false))).isFalse();
        assertThat(combined.isSatisfiedBy(new TestEntity("inactive", true))).isFalse();
    }

    @Test
    void or_shouldCombineSpecifications() {
        Specification<TestEntity> activeSpec = e -> e.status.equals("active");
        Specification<TestEntity> verifiedSpec = e -> e.verified;

        Specification<TestEntity> combined = activeSpec.or(verifiedSpec);

        assertThat(combined.isSatisfiedBy(new TestEntity("active", true))).isTrue();
        assertThat(combined.isSatisfiedBy(new TestEntity("active", false))).isTrue();
        assertThat(combined.isSatisfiedBy(new TestEntity("inactive", true))).isTrue();
        assertThat(combined.isSatisfiedBy(new TestEntity("inactive", false))).isFalse();
    }

    @Test
    void not_shouldNegateSpecification() {
        Specification<TestEntity> activeSpec = e -> e.status.equals("active");

        Specification<TestEntity> negated = activeSpec.not();

        assertThat(negated.isSatisfiedBy(new TestEntity("active"))).isFalse();
        assertThat(negated.isSatisfiedBy(new TestEntity("inactive"))).isTrue();
    }

    @Test
    void and_shouldShortCircuitOnFalse() {
        Specification<TestEntity> alwaysFalse = Specification.never();
        Specification<TestEntity> alwaysTrue = Specification.always();

        Specification<TestEntity> combined = alwaysFalse.and(alwaysTrue);

        assertThat(combined.isSatisfiedBy(new TestEntity("active"))).isFalse();
    }

    @Test
    void or_shouldShortCircuitOnTrue() {
        Specification<TestEntity> alwaysTrue = Specification.always();
        Specification<TestEntity> alwaysFalse = Specification.never();

        Specification<TestEntity> combined = alwaysTrue.or(alwaysFalse);

        assertThat(combined.isSatisfiedBy(new TestEntity("active"))).isTrue();
    }

    private record TestEntity(String status, boolean verified) {
        TestEntity(String status) {
            this(status, false);
        }
    }
}
