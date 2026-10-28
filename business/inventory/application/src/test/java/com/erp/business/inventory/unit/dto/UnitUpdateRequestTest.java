package com.erp.business.inventory.unit.dto;

import com.erp.business.inventory.domain.UnitDimension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins down the absent-versus-null contract of {@link UnitUpdateRequest}.
 *
 * <p>Editing a unit sends the whole form, so the backend must be able to tell
 * "the user did not touch this field" from "the user cleared it". A record
 * DTO collapses both into the same {@code null}, which is exactly why this class
 * uses setters rather than record components.
 */
class UnitUpdateRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("omitted fields arrive as null and leave the unit untouched")
    void omittedFieldsAreNull() throws Exception {
        UnitUpdateRequest request = objectMapper.readValue("{}", UnitUpdateRequest.class);

        assertThat(request.name()).isNull();
        assertThat(request.code()).isNull();
        assertThat(request.dimension()).isNull();
        assertThat(request.symbol()).isNull();
        assertThat(request.decimalScale()).isNull();
    }

    @Test
    @DisplayName("an explicit null symbol is distinguishable from an omitted one")
    void explicitNullSymbol() throws Exception {
        UnitUpdateRequest request = objectMapper.readValue(
                "{\"name\":\"Kilogram\",\"symbol\":null}",
                UnitUpdateRequest.class);

        assertThat(request.name()).isEqualTo("Kilogram");
        assertThat(request.symbol()).isNull();
    }

    @Test
    @DisplayName("a dimension arrives as its enum constant, not a raw string")
    void dimensionDeserialisesToEnum() throws Exception {
        UnitUpdateRequest request = objectMapper.readValue(
                "{\"dimension\":\"VOLUME\"}",
                UnitUpdateRequest.class);

        assertThat(request.dimension()).isEqualTo(UnitDimension.VOLUME);
    }

    @Test
    @DisplayName("an unknown dimension is rejected rather than silently nulled")
    void unknownDimensionIsRejected() throws Exception {
        assertThat(
                org.junit.jupiter.api.Assertions.assertThrows(
                        com.fasterxml.jackson.databind.exc.InvalidFormatException.class,
                        () -> objectMapper.readValue(
                                "{\"dimension\":\"WEIGHT\"}",
                                UnitUpdateRequest.class)))
                .hasMessageContaining("WEIGHT");
    }
}
