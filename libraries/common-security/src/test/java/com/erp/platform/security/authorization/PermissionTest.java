package com.erp.platform.security.authorization;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link Permission}.
 */
class PermissionTest {

    @Test
    @DisplayName("Should create permission with of()")
    void shouldCreatePermissionWithOf() {
        // When
        Permission permission = Permission.of("invoice", "read", "invoice");

        // Then
        assertThat(permission.domain()).isEqualTo("invoice");
        assertThat(permission.action()).isEqualTo("read");
        assertThat(permission.resource()).isEqualTo("invoice");
    }

    @Test
    @DisplayName("Should parse valid permission string")
    void shouldParseValidPermissionString() {
        // When
        Permission permission = Permission.parse("invoice:read:invoice");

        // Then
        assertThat(permission.domain()).isEqualTo("invoice");
        assertThat(permission.action()).isEqualTo("read");
        assertThat(permission.resource()).isEqualTo("invoice");
    }

    @Test
    @DisplayName("Should throw exception for null permission string")
    void shouldThrowExceptionForNullPermissionString() {
        // When/Then
        assertThatThrownBy(() -> Permission.parse(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Permission cannot be null or blank");
    }

    @Test
    @DisplayName("Should throw exception for blank permission string")
    void shouldThrowExceptionForBlankPermissionString() {
        // When/Then
        assertThatThrownBy(() -> Permission.parse("   "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Permission cannot be null or blank");
    }

    @Test
    @DisplayName("Should throw exception for invalid permission format")
    void shouldThrowExceptionForInvalidPermissionFormat() {
        // When/Then
        assertThatThrownBy(() -> Permission.parse("invoice:read"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid permission format");
    }

    @Test
    @DisplayName("Should format permission as string")
    void shouldFormatPermissionAsString() {
        // Given
        Permission permission = Permission.of("invoice", "read", "invoice");

        // When
        String formatted = permission.toString();

        // Then
        assertThat(formatted).isEqualTo("invoice:read:invoice");
    }

    @Test
    @DisplayName("Should be equal for same permission")
    void shouldBeEqualForSamePermission() {
        // Given
        Permission permission1 = Permission.of("invoice", "read", "invoice");
        Permission permission2 = Permission.parse("invoice:read:invoice");

        // Then
        assertThat(permission1).isEqualTo(permission2);
        assertThat(permission1.hashCode()).isEqualTo(permission2.hashCode());
    }
}
