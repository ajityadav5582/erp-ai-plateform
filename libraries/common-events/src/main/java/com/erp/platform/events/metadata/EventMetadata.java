package com.erp.platform.events.metadata;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Event metadata container.
 *
 * <p>Provides standard metadata for all events including
 * tenant information, correlation IDs, and tracing information.
 *
 * @since 1.0.0
 */
public class EventMetadata implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String tenantId;
    private final String correlationId;
    private final String requestId;
    private final String userId;
    private final Instant timestamp;
    private final String sourceService;
    private final transient Map<String, Object> customProperties;

    private EventMetadata(Builder builder) {
        this.tenantId = builder.tenantId;
        this.correlationId = builder.correlationId;
        this.requestId = builder.requestId;
        this.userId = builder.userId;
        this.timestamp = builder.timestamp != null ? builder.timestamp : Instant.now();
        this.sourceService = builder.sourceService;
        this.customProperties = new HashMap<>(builder.customProperties);
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getUserId() {
        return userId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getSourceService() {
        return sourceService;
    }

    public Map<String, Object> getCustomProperties() {
        return customProperties;
    }

    /**
     * Adds a custom property.
     *
     * @param key the property key
     * @param value the property value
     * @return this metadata for chaining
     */
    public EventMetadata withProperty(String key, Object value) {
        customProperties.put(key, value);
        return this;
    }

    /**
     * Creates a new builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for EventMetadata.
     */
    public static class Builder {
        private String tenantId;
        private String correlationId;
        private String requestId;
        private String userId;
        private Instant timestamp;
        private String sourceService;
        private final Map<String, Object> customProperties = new HashMap<>();

        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder sourceService(String sourceService) {
            this.sourceService = sourceService;
            return this;
        }

        public Builder property(String key, Object value) {
            this.customProperties.put(key, value);
            return this;
        }

        public EventMetadata build() {
            return new EventMetadata(this);
        }
    }
}
