package com.erp.platform.common.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link PageRequest}.
 *
 * @since 1.0.0
 */
class PageRequestTest {

    @Test
    void defaultConstructor_shouldSetDefaults() {
        PageRequest pageRequest = new PageRequest();

        assertThat(pageRequest.page()).isNull();
        assertThat(pageRequest.size()).isNull();
        assertThat(pageRequest.sort()).isNull();
    }

    @Test
    void getPageOrDefault_shouldReturnDefaultWhenNull() {
        PageRequest pageRequest = new PageRequest(null, null);

        assertThat(pageRequest.getPageOrDefault()).isEqualTo(0);
    }

    @Test
    void getPageOrDefault_shouldReturnPageWhenSet() {
        PageRequest pageRequest = new PageRequest(2, null);

        assertThat(pageRequest.getPageOrDefault()).isEqualTo(2);
    }

    @Test
    void getSizeOrDefault_shouldReturnDefaultWhenNull() {
        PageRequest pageRequest = new PageRequest(null, null);

        assertThat(pageRequest.getSizeOrDefault()).isEqualTo(20);
    }

    @Test
    void getSizeOrDefault_shouldCapAtMaxSize() {
        PageRequest pageRequest = new PageRequest(null, 200);

        assertThat(pageRequest.getSizeOrDefault()).isEqualTo(100);
    }

    @Test
    void getSizeOrDefault_shouldReturnSizeWhenWithinRange() {
        PageRequest pageRequest = new PageRequest(null, 50);

        assertThat(pageRequest.getSizeOrDefault()).isEqualTo(50);
    }

    @Test
    void getOffset_shouldCalculateCorrectly() {
        PageRequest pageRequest = new PageRequest(2, 20);

        assertThat(pageRequest.getOffset()).isEqualTo(40L);
    }

    @Test
    void getSorts_shouldReturnDefaultSortWhenEmpty() {
        PageRequest pageRequest = new PageRequest(0, 20, List.of());

        List<PageRequest.Sort> sorts = pageRequest.getSorts();

        assertThat(sorts).hasSize(1);
        assertThat(sorts.get(0).property()).isEqualTo("createdAt");
        assertThat(sorts.get(0).direction()).isEqualTo(PageRequest.Direction.DESC);
    }

    @Test
    void getSorts_shouldParseSortString() {
        PageRequest pageRequest = new PageRequest(0, 20, List.of("name,asc"));

        List<PageRequest.Sort> sorts = pageRequest.getSorts();

        assertThat(sorts).hasSize(1);
        assertThat(sorts.get(0).property()).isEqualTo("name");
        assertThat(sorts.get(0).direction()).isEqualTo(PageRequest.Direction.ASC);
    }

    @Test
    void sort_fromString_shouldParseCorrectly() {
        PageRequest.Sort sort = PageRequest.Sort.fromString("name,asc");

        assertThat(sort.property()).isEqualTo("name");
        assertThat(sort.direction()).isEqualTo(PageRequest.Direction.ASC);
    }

    @Test
    void sort_fromString_shouldUseDefaultDirection() {
        PageRequest.Sort sort = PageRequest.Sort.fromString("name");

        assertThat(sort.property()).isEqualTo("name");
        assertThat(sort.direction()).isEqualTo(PageRequest.Direction.DESC);
    }

    @Test
    void sort_by_shouldCreateAscendingSort() {
        PageRequest.Sort sort = PageRequest.Sort.by("name");

        assertThat(sort.property()).isEqualTo("name");
        assertThat(sort.direction()).isEqualTo(PageRequest.Direction.ASC);
    }

    @Test
    void sort_byDesc_shouldCreateDescendingSort() {
        PageRequest.Sort sort = PageRequest.Sort.byDesc("name");

        assertThat(sort.property()).isEqualTo("name");
        assertThat(sort.direction()).isEqualTo(PageRequest.Direction.DESC);
    }
}
