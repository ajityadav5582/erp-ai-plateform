package com.erp.platform.identity.domain.event;

import com.erp.platform.events.domain.DomainEvent;
import com.erp.platform.events.EventNamingConvention;
import com.erp.platform.events.EventVersion;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Domain event published when a new role is created.
 *
 * @since 1.0.0
 */
public record RoleCreatedEvent(
        UUID eventId,
        String tenantId,
        Instant occurredAt,
        String version,
        Map<String, Object> metadata,
        Long roleId,
        String roleCode,
        String roleName,
        String roleType
) implements DomainEvent<RoleCreatedEvent> {

    public static final String EVENT_NAME = EventNamingConvention.domainEvent("role", "created");
    public static final EventVersion VERSION = EventVersion.DEFAULT;

    /**
     * Creates a new RoleCreatedEvent.
     *
     * @param tenantId the tenant ID
     * @param roleId the role ID
     * @param roleCode the role code
     * @param roleName the role name
     * @param roleType the role type
     * @return the created event
     */
    public static RoleCreatedEvent of(String tenantId, Long roleId, String roleCode, String roleName, String roleType) {
        return new RoleCreatedEvent(
                UUID.randomUUID(),
                tenantId,
                Instant.now(),
                VERSION.toString(),
                Map.of("roleType", roleType),
                roleId,
                roleCode,
                roleName,
                roleType
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
