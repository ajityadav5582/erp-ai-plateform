package com.erp.platform.identity.domain.event;

import com.erp.platform.events.domain.DomainEvent;
import com.erp.platform.events.EventNamingConvention;
import com.erp.platform.events.EventVersion;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Domain event published when a user logs in successfully.
 *
 * @since 1.0.0
 */
public record UserLoggedInEvent(
        UUID eventId,
        String tenantId,
        Instant occurredAt,
        String version,
        Map<String, Object> metadata,
        Long userId,
        String username,
        String deviceInfo,
        String ipAddress
) implements DomainEvent<UserLoggedInEvent> {

    public static final String EVENT_NAME = EventNamingConvention.domainEvent("user", "logged_in");
    public static final EventVersion VERSION = EventVersion.DEFAULT;

    /**
     * Creates a new UserLoggedInEvent.
     *
     * @param tenantId the tenant ID
     * @param userId the user ID
     * @param username the username
     * @param deviceInfo the device information
     * @param ipAddress the IP address
     * @return the created event
     */
    public static UserLoggedInEvent of(String tenantId, Long userId, String username, String deviceInfo, String ipAddress) {
        return new UserLoggedInEvent(
                UUID.randomUUID(),
                tenantId,
                Instant.now(),
                VERSION.toString(),
                Map.of("deviceInfo", deviceInfo != null ? deviceInfo : "unknown",
                       "ipAddress", ipAddress != null ? ipAddress : "unknown"),
                userId,
                username,
                deviceInfo,
                ipAddress
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
