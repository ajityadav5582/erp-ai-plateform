package com.erp.platform.identity.domain.event;

import com.erp.platform.events.domain.DomainEvent;
import com.erp.platform.events.EventNamingConvention;
import com.erp.platform.events.EventVersion;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Domain event published when a new user is created.
 *
 * @since 1.0.0
 */
public record UserCreatedEvent(
        UUID eventId,
        String tenantId,
        Instant occurredAt,
        String version,
        Map<String, Object> metadata,
        Long userId,
        String username,
        String email,
        String fullName
) implements DomainEvent<UserCreatedEvent> {

    public static final String EVENT_NAME = EventNamingConvention.domainEvent("user", "created");
    public static final EventVersion VERSION = EventVersion.DEFAULT;

    /**
     * Creates a new UserCreatedEvent.
     *
     * @param tenantId the tenant ID
     * @param userId the user ID
     * @param username the username
     * @param email the email
     * @param fullName the full name
     * @return the created event
     */
    public static UserCreatedEvent of(String tenantId, Long userId, String username, String email, String fullName) {
        return new UserCreatedEvent(
                UUID.randomUUID(),
                tenantId,
                Instant.now(),
                VERSION.toString(),
                Map.of(),
                userId,
                username,
                email,
                fullName
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
