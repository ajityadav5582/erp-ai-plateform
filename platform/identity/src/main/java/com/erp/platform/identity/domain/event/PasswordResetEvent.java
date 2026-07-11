package com.erp.platform.identity.domain.event;

import com.erp.platform.events.domain.DomainEvent;
import com.erp.platform.events.EventNamingConvention;
import com.erp.platform.events.EventVersion;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Domain event published when a user's password is reset.
 *
 * @since 1.0.0
 */
public record PasswordResetEvent(
        UUID eventId,
        String tenantId,
        Instant occurredAt,
        String version,
        Map<String, Object> metadata,
        Long userId,
        String username,
        String resetType
) implements DomainEvent<PasswordResetEvent> {

    public static final String EVENT_NAME = EventNamingConvention.domainEvent("password", "reset");
    public static final EventVersion VERSION = EventVersion.DEFAULT;

    /**
     * Creates a new PasswordResetEvent.
     *
     * @param tenantId the tenant ID
     * @param userId the user ID
     * @param username the username
     * @param resetType the reset type (e.g., "forgot", "admin")
     * @return the created event
     */
    public static PasswordResetEvent of(String tenantId, Long userId, String username, String resetType) {
        return new PasswordResetEvent(
                UUID.randomUUID(),
                tenantId,
                Instant.now(),
                VERSION.toString(),
                Map.of("resetType", resetType),
                userId,
                username,
                resetType
        );
    }

    @Override
    public UUID getEventId() {
        return eventId;
    }

    @Override
    public String getTenantId() {
        return tenantId;
    }

    @Override
    public Instant getOccurredAt() {
        return occurredAt;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getEventName() {
        return EVENT_NAME;
    }

    @Override
    public Map<String, Object> getMetadata() {
        return metadata;
    }
}
