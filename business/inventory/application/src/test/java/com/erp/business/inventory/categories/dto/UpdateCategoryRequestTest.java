package com.erp.business.inventory.categories.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins down the {@code parentId} presence contract of {@link UpdateCategoryRequest}.
 *
 * <p>The update dialog needs all three states to be distinguishable: leaving the
 * parent alone, re-parenting, and promoting a child back to root. A record-based
 * DTO collapses the first and third into the same {@code null}, which silently
 * made "move to root" impossible from the UI.
 */
class UpdateCategoryRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("omitted parentId means leave the hierarchy untouched")
    void omittedParentIdIsNotPresent() throws Exception {
        UpdateCategoryRequest request = objectMapper.readValue(
                "{\"name\":\"Mobiles\",\"slug\":\"mobiles\"}",
                UpdateCategoryRequest.class);

        assertThat(request.isParentIdPresent()).isFalse();
        assertThat(request.parentId()).isNull();
        assertThat(request.name()).isEqualTo("Mobiles");
        assertThat(request.slug()).isEqualTo("mobiles");
    }

    @Test
    @DisplayName("explicit null parentId means promote to a root category")
    void explicitNullParentIdIsPresent() throws Exception {
        UpdateCategoryRequest request = objectMapper.readValue(
                "{\"name\":\"Mobiles\",\"parentId\":null}",
                UpdateCategoryRequest.class);

        assertThat(request.isParentIdPresent()).isTrue();
        assertThat(request.parentId()).isNull();
    }

    @Test
    @DisplayName("a parent id means re-parent under that category")
    void numericParentIdIsPresent() throws Exception {
        UpdateCategoryRequest request = objectMapper.readValue(
                "{\"parentId\":42}",
                UpdateCategoryRequest.class);

        assertThat(request.isParentIdPresent()).isTrue();
        assertThat(request.parentId()).isEqualTo(42L);
    }

    @Test
    @DisplayName("description can be cleared with an explicit empty string")
    void descriptionRoundTrips() throws Exception {
        UpdateCategoryRequest request = objectMapper.readValue(
                "{\"description\":\"\"}",
                UpdateCategoryRequest.class);

        assertThat(request.description()).isEmpty();
    }
}
