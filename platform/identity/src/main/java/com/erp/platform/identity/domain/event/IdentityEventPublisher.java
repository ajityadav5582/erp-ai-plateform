package com.erp.platform.identity.domain.event;

import com.erp.platform.events.EventPublisher;
import com.erp.platform.events.domain.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Publisher for identity domain events.
 *
 * <p>Wraps the common EventPublisher to provide identity-specific
 * event publishing capabilities.
 *
 * @since 1.0.0
 */
@Component
public class IdentityEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(IdentityEventPublisher.class);

    private final EventPublisher<DomainEvent<?>> eventPublisher;

    public IdentityEventPublisher(EventPublisher<DomainEvent<?>> eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * Publishes a domain event.
     *
     * @param event the domain event to publish
     */
    public void publish(DomainEvent<?> event) {
        LOGGER.debug("Publishing identity domain event: {}", event.getEventName());
        eventPublisher.publish(event);
    }

    /**
     * Publishes a domain event asynchronously.
     *
     * @param event the domain event to publish
     */
    public void publishAsync(DomainEvent<?> event) {
        LOGGER.debug("Publishing identity domain event asynchronously: {}", event.getEventName());
        eventPublisher.publishAsync(event);
    }

    /**
     * Publishes a user created event.
     *
     * @param tenantId the tenant ID
     * @param userId the user ID
     * @param username the username
     * @param email the email
     * @param fullName the full name
     */
    public void publishUserCreated(String tenantId, Long userId, String username, String email, String fullName) {
        UserCreatedEvent event = UserCreatedEvent.of(tenantId, userId, username, email, fullName);
        publish(event);
    }

    /**
     * Publishes a user logged in event.
     *
     * @param tenantId the tenant ID
     * @param userId the user ID
     * @param username the username
     * @param deviceInfo the device information
     * @param ipAddress the IP address
     */
    public void publishUserLoggedIn(String tenantId, Long userId, String username, String deviceInfo, String ipAddress) {
        UserLoggedInEvent event = UserLoggedInEvent.of(tenantId, userId, username, deviceInfo, ipAddress);
        publish(event);
    }

    /**
     * Publishes a user logged out event.
     *
     * @param tenantId the tenant ID
     * @param userId the user ID
     * @param username the username
     * @param logoutType the logout type
     */
    public void publishUserLoggedOut(String tenantId, Long userId, String username, String logoutType) {
        UserLoggedOutEvent event = UserLoggedOutEvent.of(tenantId, userId, username, logoutType);
        publish(event);
    }

    /**
     * Publishes a password reset event.
     *
     * @param tenantId the tenant ID
     * @param userId the user ID
     * @param username the username
     * @param resetType the reset type
     */
    public void publishPasswordReset(String tenantId, Long userId, String username, String resetType) {
        PasswordResetEvent event = PasswordResetEvent.of(tenantId, userId, username, resetType);
        publish(event);
    }

    /**
     * Publishes a role created event.
     *
     * @param tenantId the tenant ID
     * @param roleId the role ID
     * @param roleCode the role code
     * @param roleName the role name
     * @param roleType the role type
     */
    public void publishRoleCreated(String tenantId, Long roleId, String roleCode, String roleName, String roleType) {
        RoleCreatedEvent event = RoleCreatedEvent.of(tenantId, roleId, roleCode, roleName, roleType);
        publish(event);
    }

    /**
     * Publishes a role updated event.
     *
     * @param tenantId the tenant ID
     * @param roleId the role ID
     * @param roleCode the role code
     * @param roleName the role name
     * @param roleType the role type
     */
    public void publishRoleUpdated(String tenantId, Long roleId, String roleCode, String roleName, String roleType) {
        RoleUpdatedEvent event = RoleUpdatedEvent.of(tenantId, roleId, roleCode, roleName, roleType);
        publish(event);
    }
}
