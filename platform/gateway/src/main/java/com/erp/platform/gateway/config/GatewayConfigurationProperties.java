package com.erp.platform.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for the API Gateway service.
 *
 * <p>Holds the list of downstream service routes that the gateway proxies to.
 * Each route defines a path prefix, the target service identifier, and the
 * base URI of the downstream service.
 */
@Configuration
@ConfigurationProperties(prefix = "erp.gateway")
public class GatewayConfigurationProperties {

    private final List<RouteDefinition> routes = new ArrayList<>();
    private final List<String> allowedOrigins = new ArrayList<>();
    private boolean rateLimitEnabled = false;
    private int rateLimitPerMinute = 1000;

    public List<RouteDefinition> getRoutes() {
        return routes;
    }

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public boolean isRateLimitEnabled() {
        return rateLimitEnabled;
    }

    public void setRateLimitEnabled(boolean rateLimitEnabled) {
        this.rateLimitEnabled = rateLimitEnabled;
    }

    public int getRateLimitPerMinute() {
        return rateLimitPerMinute;
    }

    public void setRateLimitPerMinute(int rateLimitPerMinute) {
        this.rateLimitPerMinute = rateLimitPerMinute;
    }

    /**
     * Definition of a downstream service route.
     */
    public static class RouteDefinition {
        private String id;
        private String path;
        private String uri;
        private String stripPrefix = "true";
        private int retries = 3;
        private long timeoutMs = 30000;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public String getStripPrefix() {
            return stripPrefix;
        }

        public void setStripPrefix(String stripPrefix) {
            this.stripPrefix = stripPrefix;
        }

        public int getRetries() {
            return retries;
        }

        public void setRetries(int retries) {
            this.retries = retries;
        }

        public long getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(long timeoutMs) {
            this.timeoutMs = timeoutMs;
        }
    }
}
