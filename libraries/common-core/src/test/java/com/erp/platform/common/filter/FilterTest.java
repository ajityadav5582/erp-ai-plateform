package com.erp.platform.common.filter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Filter}.
 *
 * @since 1.0.0
 */
class FilterTest {

    @Test
    void empty_shouldCreateEmptyFilter() {
        Filter<TestEntity> filter = Filter.empty();

        assertThat(filter.isEmpty()).isTrue();
        assertThat(filter.getCriteria()).isEmpty();
    }

    @Test
    void add_shouldAddCriterion() {
        Filter<TestEntity> filter = Filter.empty();
        filter.add(new Filter.Criterion("status", Filter.Operator.EQUALS, "active"));

        assertThat(filter.isEmpty()).isFalse();
        assertThat(filter.getCriteria()).hasSize(1);
        assertThat(filter.getCriteria().get(0).field()).isEqualTo("status");
        assertThat(filter.getCriteria().get(0).operator()).isEqualTo(Filter.Operator.EQUALS);
        assertThat(filter.getCriteria().get(0).value()).isEqualTo("active");
    }

    @Test
    void add_shouldIgnoreNullCriterion() {
        Filter<TestEntity> filter = Filter.empty();
        filter.add(null);

        assertThat(filter.isEmpty()).isTrue();
    }

    @Test
    void add_shouldSupportChaining() {
        Filter<TestEntity> filter = Filter.empty()
            .add(new Filter.Criterion("status", Filter.Operator.EQUALS, "active"))
            .add(new Filter.Criterion("tenantId", Filter.Operator.EQUALS, 1L));

        assertThat(filter.getCriteria()).hasSize(2);
    }

    @Test
    void getCriteria_shouldReturnCopy() {
        Filter<TestEntity> filter = Filter.empty();
        filter.add(new Filter.Criterion("status", Filter.Operator.EQUALS, "active"));

        var criteria1 = filter.getCriteria();
        var criteria2 = filter.getCriteria();

        assertThat(criteria1).isNotSameAs(criteria2);
    }

    @Test
    void isEmpty_shouldReturnTrueForEmptyFilter() {
        assertThat(Filter.empty().isEmpty()).isTrue();
    }

    @Test
    void isEmpty_shouldReturnFalseForNonEmptyFilter() {
        Filter<TestEntity> filter = Filter.empty();
        filter.add(new Filter.Criterion("status", Filter.Operator.EQUALS, "active"));

        assertThat(filter.isEmpty()).isFalse();
    }

    @Test
    void operator_shouldHaveAllValues() {
        assertThat(Filter.Operator.values()).contains(
            Filter.Operator.EQUALS,
            Filter.Operator.NOT_EQUALS,
            Filter.Operator.GREATER_THAN,
            Filter.Operator.GREATER_THAN_OR_EQUALS,
            Filter.Operator.LESS_THAN,
            Filter.Operator.LESS_THAN_OR_EQUALS,
            Filter.Operator.LIKE,
            Filter.Operator.IN,
            Filter.Operator.NOT_IN,
            Filter.Operator.IS_NULL,
            Filter.Operator.IS_NOT_NULL,
            Filter.Operator.BETWEEN
        );
    }

    private record TestEntity(String status, Long tenantId) {
    }
}
