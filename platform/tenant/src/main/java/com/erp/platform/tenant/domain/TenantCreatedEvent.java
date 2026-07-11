package com.erp.platform.tenant.domain;

import com.erp.platform.events.domain.DomainEvent;
import lombok.Builder;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * Domain event fired when a tenant is created.
 *
 * @since 1.0.0
 */
@Builder
public record TenantCreatedEvent(
        UUID eventId,
        UUID tenantId,
        String tenantCode,
        String legalName,
        String displayName,
        String isolationStrategy,
        Instant occurredAt,
        String version,
        String eventName,
        Map<String, Object> metadata
) implements DomainEvent<TenantCreatedEvent> {

    /**
     * Creates a new tenant created event.
     *
     * @param tenantId the tenant ID
     * @param tenantCode the tenant code
     * @param legalName the legal name
     * @param displayName the display name
     * @param isolationStrategy the isolation strategy
     * @param occurredAt the timestamp when the event occurred (provided by application layer)
     * @return a new TenantCreatedEvent instance
     */
    public static TenantCreatedEvent of(UUID tenantId, String tenantCode, String legalName,
                                         String displayName, String isolationStrategy, Instant occurredAt) {
        return TenantCreatedEvent.builder()
                .eventId(UUID.randomUUID())
                .tenantId(tenantId)
                .tenantCode(tenantCode)
                .legalName(legalName)
                .displayName(displayName)
                .isolationStrategy(isolationStrategy)
                .occurredAt(occurredAt)
                .version("1.0")
                .eventName("tenant.created")
                .metadata(Collections.emptyMap())
                .build();
    }

    @Override
    public UUID getEventId() {
        return this.eventId;
    }

    @Override
    public String getTenantId() {
        return this.tenantId.toString();
    }

    @Override
    public Instant getOccurredAt() {
        return this.occurredAt;
    }

    @Override
    public String getEventName() {
        return this.eventName;
    }

    @Override
    public String getVersion() {
        return this.version;
    }

    @Override
    public Map<String, Object> getMetadata() {
        return this.metadata;
    }
}
