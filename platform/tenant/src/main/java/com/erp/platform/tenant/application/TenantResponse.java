package com.erp.platform.tenant.application;

import com.erp.platform.tenant.domain.Tenant;
import com.erp.platform.tenant.domain.TenantStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for tenant operations.
 *
 * @since 1.0.0
 */
@Builder
public record TenantResponse(
        Long id,
        UUID tenantId,
        String tenantCode,
        String legalName,
        String displayName,
        String email,
        String phone,
        String website,
        String status,
        String isolationStrategy,
        Instant createdAt,
        Instant updatedAt,
        Instant activatedAt,
        Instant suspendedAt,
        Instant deactivatedAt,
        String timezone,
        String currency,
        String language,
        String logoUrl,
        String faviconUrl,
        UUID subscriptionPlanId
) {

    /**
     * Creates a response from a tenant entity.
     */
    public static TenantResponse from(Tenant tenant) {
        return TenantResponse.builder()
                .id(tenant.getId())
                .tenantId(tenant.getTenantId())
                .tenantCode(tenant.getTenantCode())
                .legalName(tenant.getLegalName())
                .displayName(tenant.getDisplayName())
                .email(tenant.getEmail())
                .phone(tenant.getPhone())
                .website(tenant.getWebsite())
                .status(tenant.getStatus().name())
                .isolationStrategy(tenant.getIsolationStrategy().name())
                .createdAt(tenant.getCreatedAt())
                .updatedAt(tenant.getUpdatedAt())
                .activatedAt(tenant.getActivatedAt())
                .suspendedAt(tenant.getSuspendedAt())
                .deactivatedAt(tenant.getDeactivatedAt())
                .timezone(tenant.getTimezone())
                .currency(tenant.getCurrency())
                .language(tenant.getLanguage())
                .logoUrl(tenant.getLogoUrl())
                .faviconUrl(tenant.getFaviconUrl())
                .subscriptionPlanId(tenant.getSubscriptionPlanId())
                .build();
    }
}
