package com.erp.platform.common.context;

import org.springframework.util.Assert;

/**
 * Thread-local request context holder.
 *
 * <p>Provides access to request-scoped information throughout the request lifecycle.
 * This context is automatically populated by filters and cleared after request completion.
 *
 * @since 1.0.0
 */
public final class RequestContext {

    private static final ThreadLocal<RequestContextHolder> CURRENT_CONTEXT = ThreadLocal.withInitial(RequestContextHolder::new);

    private RequestContext() {
        // Utility class
    }

    /**
     * Sets the request ID.
     *
     * @param requestId the request ID
     */
    public static void setRequestId(String requestId) {
        getHolder().setRequestId(requestId);
    }

    /**
     * Returns the request ID.
     *
     * @return the request ID, or null if not set
     */
    public static String getRequestId() {
        return getHolder().getRequestId();
    }

    /**
     * Sets the correlation ID.
     *
     * @param correlationId the correlation ID
     */
    public static void setCorrelationId(String correlationId) {
        getHolder().setCorrelationId(correlationId);
    }

    /**
     * Returns the correlation ID.
     *
     * @return the correlation ID, or null if not set
     */
    public static String getCorrelationId() {
        return getHolder().getCorrelationId();
    }

    /**
     * Sets the user ID.
     *
     * @param userId the user ID
     */
    public static void setUserId(String userId) {
        getHolder().setUserId(userId);
    }

    /**
     * Returns the user ID.
     *
     * @return the user ID, or null if not set
     */
    public static String getUserId() {
        return getHolder().getUserId();
    }

    /**
     * Sets the tenant ID.
     *
     * @param tenantId the tenant ID
     */
    public static void setTenantId(String tenantId) {
        getHolder().setTenantId(tenantId);
    }

    /**
     * Returns the tenant ID.
     *
     * @return the tenant ID, or null if not set
     */
    public static String getTenantId() {
        return getHolder().getTenantId();
    }

    /**
     * Sets the active company ID.
     *
     * <p>The company (legal entity) is the second level of scoping, selected by
     * the user after login and carried on every subsequent request via the
     * {@code X-Company-Id} header. It is deliberately <em>not</em> part of the
     * JWT so the user can switch companies without re-authenticating.
     *
     * @param companyId the company ID
     */
    public static void setCompanyId(String companyId) {
        getHolder().setCompanyId(companyId);
    }

    /**
     * Returns the active company ID.
     *
     * @return the company ID, or null if not set
     */
    public static String getCompanyId() {
        return getHolder().getCompanyId();
    }

    /**
     * Sets the active fiscal year ID.
     *
     * <p>Carried on every request via the {@code X-Fiscal-Year-Id} header and
     * scoped per company, so switching fiscal years does not require a new token.
     *
     * @param fiscalYearId the fiscal year ID
     */
    public static void setFiscalYearId(String fiscalYearId) {
        getHolder().setFiscalYearId(fiscalYearId);
    }

    /**
     * Returns the active fiscal year ID.
     *
     * @return the fiscal year ID, or null if not set
     */
    public static String getFiscalYearId() {
        return getHolder().getFiscalYearId();
    }

    /**
     * Sets the client IP address.
     *
     * @param clientIp the client IP
     */
    public static void setClientIp(String clientIp) {
        getHolder().setClientIp(clientIp);
    }

    /**
     * Returns the client IP address.
     *
     * @return the client IP, or null if not set
     */
    public static String getClientIp() {
        return getHolder().getClientIp();
    }

    /**
     * Sets the user agent.
     *
     * @param userAgent the user agent
     */
    public static void setUserAgent(String userAgent) {
        getHolder().setUserAgent(userAgent);
    }

    /**
     * Returns the user agent.
     *
     * @return the user agent, or null if not set
     */
    public static String getUserAgent() {
        return getHolder().getUserAgent();
    }

    /**
     * Adds a custom attribute to the context.
     *
     * @param key the attribute key
     * @param value the attribute value
     */
    public static void setAttribute(String key, Object value) {
        Assert.notNull(key, "Key cannot be null");
        getHolder().setAttribute(key, value);
    }

    /**
     * Gets a custom attribute from the context.
     *
     * @param key the attribute key
     * @param <T> the attribute type
     * @return the attribute value, or null if not found
     */
    @SuppressWarnings("unchecked")
    public static <T> T getAttribute(String key) {
        Assert.notNull(key, "Key cannot be null");
        return (T) getHolder().getAttribute(key);
    }

    /**
     * Clears all context data.
     *
     * <p>This should be called in a finally block to prevent memory leaks.
     */
    public static void clear() {
        CURRENT_CONTEXT.remove();
    }

    private static RequestContextHolder getHolder() {
        return CURRENT_CONTEXT.get();
    }

    /**
     * Holder for request context data.
     */
    public static class RequestContextHolder {
        private String requestId;
        private String correlationId;
        private String userId;
        private String tenantId;
        private String companyId;
        private String fiscalYearId;
        private String clientIp;
        private String userAgent;
        private final java.util.Map<String, Object> attributes = new java.util.concurrent.ConcurrentHashMap<>();

        public String getCompanyId() {
            return companyId;
        }

        public void setCompanyId(String companyId) {
            this.companyId = companyId;
        }

        public String getFiscalYearId() {
            return fiscalYearId;
        }

        public void setFiscalYearId(String fiscalYearId) {
            this.fiscalYearId = fiscalYearId;
        }

        public String getRequestId() {
            return requestId;
        }

        public void setRequestId(String requestId) {
            this.requestId = requestId;
        }

        public String getCorrelationId() {
            return correlationId;
        }

        public void setCorrelationId(String correlationId) {
            this.correlationId = correlationId;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getTenantId() {
            return tenantId;
        }

        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }

        public String getClientIp() {
            return clientIp;
        }

        public void setClientIp(String clientIp) {
            this.clientIp = clientIp;
        }

        public String getUserAgent() {
            return userAgent;
        }

        public void setUserAgent(String userAgent) {
            this.userAgent = userAgent;
        }

        public void setAttribute(String key, Object value) {
            attributes.put(key, value);
        }

        public Object getAttribute(String key) {
            return attributes.get(key);
        }

        public java.util.Map<String, Object> getAttributes() {
            return java.util.Collections.unmodifiableMap(attributes);
        }

        public void clear() {
            requestId = null;
            correlationId = null;
            userId = null;
            tenantId = null;
            clientIp = null;
            userAgent = null;
            attributes.clear();
        }
    }
}
