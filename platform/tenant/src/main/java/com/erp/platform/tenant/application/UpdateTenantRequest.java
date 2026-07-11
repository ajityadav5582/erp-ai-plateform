package com.erp.platform.tenant.application;

import java.util.UUID;

/**
 * Request DTO for updating a tenant.
 *
 * <p>All fields are optional. Only the provided (non-null) fields are applied to
 * the tenant entity, which makes this DTO suitable for both full ({@code PUT})
 * and partial ({@code PATCH}) updates. Domain update methods are null/blank safe,
 * so omitted fields leave the existing value untouched.
 *
 * <p>Note: {@code tenantCode}, {@code legalName} and {@code isolationStrategy} are
 * immutable by design and therefore cannot be changed through this request.
 *
 * @since 1.0.0
 */
public record UpdateTenantRequest(
        String displayName,
        String email,
        String phone,
        String website,
        String timezone,
        String currency,
        String language,
        String logoUrl,
        String faviconUrl,
        UUID subscriptionPlanId
) {
}
