package com.erp.platform.data.query;

import com.erp.platform.common.dto.PageRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link QueryUtils}.
 *
 * @since 1.0.0
 */
class QueryUtilsTest {

    @Test
    void toPageRequest_shouldConvertCorrectly() {
        com.erp.platform.common.dto.PageRequest platformRequest =
            new com.erp.platform.common.dto.PageRequest(1, 50, List.of("name,asc"));

        org.springframework.data.domain.PageRequest pageRequest = QueryUtils.toPageRequest(platformRequest);

        assertThat(pageRequest.getPageNumber()).isEqualTo(1);
        assertThat(pageRequest.getPageSize()).isEqualTo(50);
    }

    @Test
    void toPageRequest_shouldUseDefaultsWhenNull() {
        com.erp.platform.common.dto.PageRequest platformRequest =
            new com.erp.platform.common.dto.PageRequest(null, null, null);

        org.springframework.data.domain.PageRequest pageRequest = QueryUtils.toPageRequest(platformRequest);

        assertThat(pageRequest.getPageNumber()).isEqualTo(0);
        assertThat(pageRequest.getPageSize()).isEqualTo(20);
    }

    @Test
    void toSort_shouldConvertCorrectly() {
        List<PageRequest.Sort> sorts = List.of(
            PageRequest.Sort.by("name"),
            PageRequest.Sort.byDesc("createdAt")
        );

        org.springframework.data.domain.Sort sort = QueryUtils.toSort(sorts);

        assertThat(sort).isNotNull();
    }

    @Test
    void toSort_shouldReturnDefaultWhenEmpty() {
        org.springframework.data.domain.Sort sort = QueryUtils.toSort(List.of());

        assertThat(sort).isNotNull();
    }

    @Test
    void withTenantFilter_shouldCreateSpecification() {
        Specification<TestEntity> spec = QueryUtils.withTenantFilter(1L);

        assertThat(spec).isNotNull();
    }

    @Test
    void withTenantFilter_shouldCombineWithSpecification() {
        Specification<TestEntity> otherSpec = (root, query, cb) -> cb.conjunction();
        Specification<TestEntity> combined = QueryUtils.withTenantFilter(1L, otherSpec);

        assertThat(combined).isNotNull();
    }

    private record TestEntity(Long id, String name) {
    }
}
