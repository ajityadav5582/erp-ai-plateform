package com.erp.platform.common.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link UuidUtils}.
 *
 * @since 1.0.0
 */
class UuidUtilsTest {

    @Test
    void generate_shouldReturnValidUuid() {
        UUID uuid = UuidUtils.generate();

        assertThat(uuid).isNotNull();
        assertThat(uuid.version()).isEqualTo(4);
    }

    @Test
    void generateString_shouldReturnValidUuidString() {
        String uuidString = UuidUtils.generateString();

        assertThat(uuidString).isNotNull();
        assertThat(UuidUtils.isValid(uuidString)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "550e8400-e29b-41d4-a716-446655440000",
        "00000000-0000-0000-0000-000000000000",
        "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF"
    })
    void isValid_shouldReturnTrueForValidUuids(String uuidString) {
        assertThat(UuidUtils.isValid(uuidString)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "not-a-uuid",
        "550e8400-e29b-41d4-a716-44665544000",
        "550e8400-e29b-41d4-a716-4466554400000",
        "",
        "null"
    })
    void isValid_shouldReturnFalseForInvalidUuids(String uuidString) {
        assertThat(UuidUtils.isValid(uuidString)).isFalse();
    }

    @Test
    void isValid_shouldReturnFalseForNull() {
        assertThat(UuidUtils.isValid(null)).isFalse();
    }

    @Test
    void isValid_shouldReturnFalseForBlank() {
        assertThat(UuidUtils.isValid("   ")).isFalse();
    }

    @Test
    void parse_shouldReturnUuidForValidString() {
        String uuidString = "550e8400-e29b-41d4-a716-446655440000";
        UUID uuid = UuidUtils.parse(uuidString);

        assertThat(uuid).isEqualTo(UUID.fromString(uuidString));
    }

    @Test
    void parse_shouldThrowForNull() {
        assertThatThrownBy(() -> UuidUtils.parse(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parse_shouldThrowForBlank() {
        assertThatThrownBy(() -> UuidUtils.parse("   "))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parse_shouldThrowForInvalid() {
        assertThatThrownBy(() -> UuidUtils.parse("not-a-uuid"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void toBytes_andFromBytes_shouldRoundTrip() {
        UUID original = UUID.randomUUID();
        byte[] bytes = UuidUtils.toBytes(original);
        UUID recovered = UuidUtils.fromBytes(bytes);

        assertThat(recovered).isEqualTo(original);
    }

    @Test
    void toBytes_shouldReturnEmptyArrayForNull() {
        byte[] bytes = UuidUtils.toBytes(null);

        assertThat(bytes).isEmpty();
    }

    @Test
    void fromBytes_shouldThrowForInvalidLength() {
        assertThatThrownBy(() -> UuidUtils.fromBytes(new byte[8]))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void safeParse_shouldReturnUuidForValidString() {
        String uuidString = "550e8400-e29b-41d4-a716-446655440000";
        UUID uuid = UuidUtils.safeParse(uuidString);

        assertThat(uuid).isEqualTo(UUID.fromString(uuidString));
    }

    @Test
    void safeParse_shouldReturnNullForInvalidString() {
        assertThat(UuidUtils.safeParse("not-a-uuid")).isNull();
    }

    @Test
    void safeParse_shouldReturnNullForNull() {
        assertThat(UuidUtils.safeParse(null)).isNull();
    }
}
