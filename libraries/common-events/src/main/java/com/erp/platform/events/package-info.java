/**
 * Event infrastructure for the ERP AI Platform.
 *
 * <p>This package provides event abstractions for domain events
 * and integration events, including publishing, consuming,
 * serialization, and versioning.
 *
 * <p>Key components:
 * <ul>
 *   <li>{@link com.erp.platform.events.domain.DomainEvent} - Base domain event interface</li>
 *   <li>{@link com.erp.platform.events.integration.IntegrationEvent} - Integration event interface</li>
 *   <li>{@link com.erp.platform.events.domain.EventPublisher} - Event publisher interface</li>
 *   <li>{@link com.erp.platform.events.domain.EventConsumer} - Event consumer interface</li>
 *   <li>{@link com.erp.platform.events.serializer.EventSerializer} - Event serializer interface</li>
 *   <li>{@link com.erp.platform.events.versioning.EventVersion} - Event versioning utilities</li>
 *   <li>{@link com.erp.platform.events.versioning.EventNaming} - Event naming conventions</li>
 * </ul>
 *
 * @since 1.0.0
 */
package com.erp.platform.events;
