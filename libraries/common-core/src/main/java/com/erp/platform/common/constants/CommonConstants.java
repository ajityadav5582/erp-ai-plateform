package com.erp.platform.common.constants;

/**
 * Common constants used across the platform.
 *
 * @since 1.0.0
 */
public final class CommonConstants {

    private CommonConstants() {
        // Utility class
    }

    // ==================== Default Values ====================
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    public static final int DEFAULT_PAGE_NUMBER = 0;
    public static final String DEFAULT_SORT_PROPERTY = "createdAt";
    public static final String DEFAULT_SORT_DIRECTION = "DESC";

    // ==================== Date/Time Formats ====================
    public static final String FORMAT_ISO_DATE_TIME = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    public static final String FORMAT_ISO_DATE = "yyyy-MM-dd";
    public static final String FORMAT_ISO_TIME = "HH:mm:ss";
    public static final String FORMAT_DISPLAY_DATE_TIME = "dd MMM yyyy, hh:mm a";
    public static final String FORMAT_DISPLAY_DATE = "dd MMM yyyy";

    // ==================== Currency ====================
    public static final String DEFAULT_CURRENCY = "USD";
    public static final int CURRENCY_SCALE = 4;
    public static final int CURRENCY_PRECISION = 19;

    // ==================== Tenant ====================
    public static final String TENANT_HEADER = "X-Tenant-ID";
    public static final String DEFAULT_TENANT_ID = "1";

    // ==================== Request Context ====================
    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String REQUEST_ID_ATTRIBUTE = "requestId";
    public static final String CORRELATION_ID_ATTRIBUTE = "correlationId";

    // ==================== Security ====================
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String USER_ID_CLAIM = "sub";
    public static final String TENANT_ID_CLAIM = "tenant_id";
    public static final String ROLES_CLAIM = "roles";

    // ==================== Cache ====================
    public static final String CACHE_TENANT_PREFIX = "tenant:";
    public static final String CACHE_USER_PREFIX = "user:";
    public static final String CACHE_CONFIG_PREFIX = "config:";
    public static final long CACHE_TTL_HOURS = 1;
    public static final long CACHE_TTL_DAYS = 1;

    // ==================== Kafka ====================
    public static final String KAFKA_HEADER_TENANT_ID = "tenant_id";
    public static final String KAFKA_HEADER_CORRELATION_ID = "correlation_id";
    public static final String KAFKA_HEADER_USER_ID = "user_id";
    public static final String KAFKA_HEADER_SOURCE_SERVICE = "source_service";

    // ==================== Audit ====================
    public static final String AUDIT_ACTION_CREATE = "CREATE";
    public static final String AUDIT_ACTION_UPDATE = "UPDATE";
    public static final String AUDIT_ACTION_DELETE = "DELETE";
    public static final String AUDIT_ACTION_VIEW = "VIEW";

    // ==================== Soft Delete ====================
    public static final String COLUMN_DELETED_AT = "deleted_at";
    public static final String COLUMN_TENANT_ID = "tenant_id";
    public static final String COLUMN_CREATED_AT = "created_at";
    public static final String COLUMN_UPDATED_AT = "updated_at";
    public static final String COLUMN_CREATED_BY = "created_by";
    public static final String COLUMN_UPDATED_BY = "updated_by";

    // ==================== Pagination ====================
    public static final String PARAM_PAGE = "page";
    public static final String PARAM_SIZE = "size";
    public static final String PARAM_SORT = "sort";

    // ==================== Error Codes ====================
    public static final String ERROR_VALIDATION = "VALIDATION_ERROR";
    public static final String ERROR_NOT_FOUND = "NOT_FOUND";
    public static final String ERROR_CONFLICT = "CONFLICT";
    public static final String ERROR_UNAUTHORIZED = "UNAUTHORIZED";
    public static final String ERROR_FORBIDDEN = "FORBIDDEN";
    public static final String ERROR_INTERNAL = "INTERNAL_ERROR";
    public static final String ERROR_BUSINESS = "BUSINESS_ERROR";

    // ==================== HTTP Headers ====================
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_ACCEPT = "Accept";
    public static final String MEDIA_TYPE_JSON = "application/json";
    public static final String CHARSET_UTF_8 = "UTF-8";

    // ==================== Regex Patterns ====================
    public static final String PATTERN_EMAIL = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
    public static final String PATTERN_PHONE = "^[+]?[0-9]{10,15}$";
    public static final String PATTERN_UUID = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
    public static final String PATTERN_ALPHANUMERIC = "^[a-zA-Z0-9]+$";
}
