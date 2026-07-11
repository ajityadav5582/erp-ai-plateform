package com.erp.platform.identity.domain.event;

import com.erp.platform.events.domain.DomainEvent;
import com.erp.platform.events.EventNamingConvention;
import com.erp.platform.events.EventVersion;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Domain event published when a user logs out.
 *
 * @since 1.0.0
 */
public record UserLoggedOutEvent(
        UUID eventId,
        String tenantId,
        Instant occurredAt,
        String version,
        Map<String, Object> metadata,
        Long userId,
        String username,
        String logoutType
) implements DomainEvent<UserLoggedOutEvent> {

    public static final String EVENT_NAME = EventNamingConvention.domainEvent("user", "logged_out");
    public static final EventVersion VERSION = EventVersion.DEFAULT;

    /**
     * Creates a new UserLoggedOutEvent.
     *
     * @param tenantId the tenant ID
     * @param userId the user ID
     * @param username the username
     * @param logoutType the logout type (e.g., "single", "all")
     * @return the created event
     */
    public static UserLoggedOutEvent of(String tenantId, Long userId, String username, String logoutType) {
        return new UserLoggedOutEvent(
                UUID.randomUUID(),
                tenantId,
                Instant.now(),
                VERSION.toString(),
                Map.of("logoutType", logoutType),
                userId,
                username,
                logoutType
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
