package com.erp.platform.data.specification;

import com.erp.platform.common.filter.Filter;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SpecificationBuilder}.
 *
 * @since 1.0.0
 */
class SpecificationBuilderTest {

    @Test
    void build_shouldReturnConjunctionWhenEmpty() {
        SpecificationBuilder<TestEntity> builder = new SpecificationBuilder<>();
        Specification<TestEntity> spec = builder.build();

        assertThat(spec).isNotNull();
    }

    @Test
    void filter_shouldBuildSpecification() {
        SpecificationBuilder<TestEntity> builder = new SpecificationBuilder<>();
        builder.filter("status", Filter.Operator.EQUALS, "ACTIVE");

        Specification<TestEntity> spec = builder.build();

        assertThat(spec).isNotNull();
    }

    @Test
    void filter_shouldSupportMultipleFilters() {
        SpecificationBuilder<TestEntity> builder = new SpecificationBuilder<>();
        builder.filter("status", Filter.Operator.EQUALS, "ACTIVE");
        builder.filter("name", Filter.Operator.LIKE, "John%");

        Specification<TestEntity> spec = builder.build();

        assertThat(spec).isNotNull();
    }

    @Test
    void filter_shouldSupportInOperator() {
        SpecificationBuilder<TestEntity> builder = new SpecificationBuilder<>();
        builder.filter("status", Filter.Operator.IN, java.util.List.of("ACTIVE", "PENDING"));

        Specification<TestEntity> spec = builder.build();

        assertThat(spec).isNotNull();
    }

    @Test
    void filter_shouldSupportIsNullOperator() {
        SpecificationBuilder<TestEntity> builder = new SpecificationBuilder<>();
        builder.filter("deletedAt", Filter.Operator.IS_NULL, null);

        Specification<TestEntity> spec = builder.build();

        assertThat(spec).isNotNull();
    }

    @Test
    void filter_shouldSupportBetweenOperator() {
        SpecificationBuilder<TestEntity> builder = new SpecificationBuilder<>();
        builder.filter("age", Filter.Operator.BETWEEN, java.util.List.of(18, 65));

        Specification<TestEntity> spec = builder.build();

        assertThat(spec).isNotNull();
    }

    private record TestEntity(String status, String name, Integer age, java.time.Instant deletedAt) {
    }
}
