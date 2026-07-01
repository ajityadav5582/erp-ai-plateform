package com.erp.platform.security.tenant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TenantPrincipal}.
 */
class TenantPrincipalTest {

    @Test
    @DisplayName("Should create tenant principal with all fields")
    void shouldCreateTenantPrincipalWithAllFields() {
        // When
        TenantPrincipal principal = TenantPrincipal.of(
            "tenant-123", "user-456", "john.doe", "john@example.com"
        );

        // Then
        assertThat(principal.tenantId()).isEqualTo("tenant-123");
        assertThat(principal.userId()).isEqualTo("user-456");
        assertThat(principal.username()).isEqualTo("john.doe");
        assertThat(principal.email()).isEqualTo("john@example.com");
    }

    @Test
    @DisplayName("Should be equal for same tenant and user")
    void shouldBeEqualForSameTenantAndUser() {
        // Given
        TenantPrincipal principal1 = TenantPrincipal.of("tenant-123", "user-456", "john", "john@example.com");
        TenantPrincipal principal2 = TenantPrincipal.of("tenant-123", "user-456", "jane", "jane@example.com");

        // Then
        assertThat(principal1).isEqualTo(principal2);
        assertThat(principal1.hashCode()).isEqualTo(principal2.hashCode());
    }

    @Test
    @DisplayName("Should not be equal for different tenant")
    void shouldNotBeEqualForDifferentTenant() {
        // Given
        TenantPrincipal principal1 = TenantPrincipal.of("tenant-123", "user-456", "john", "john@example.com");
        TenantPrincipal principal2 = TenantPrincipal.of("tenant-789", "user-456", "john", "john@example.com");

        // Then
        assertThat(principal1).isNotEqualTo(principal2);
    }

    @Test
    @DisplayName("Should format as string")
    void shouldFormatAsString() {
        // Given
        TenantPrincipal principal = TenantPrincipal.of("tenant-123", "user-456", "john", "john@example.com");

        // When
        String formatted = principal.toString();

        // Then
        assertThat(formatted).contains("tenant-123");
        assertThat(formatted).contains("user-456");
        assertThat(formatted).contains("john");
    }
}
