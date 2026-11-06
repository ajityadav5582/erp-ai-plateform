package com.erp.platform.identity.domain;

/**
 * POS operating profile of a company for SaaS POS.
 *
 * @since 1.0.0
 */
/**
 * @deprecated Use {@link BusinessType} + capability flags instead. Retained for
 * backward compatibility; will be removed in Phase 6.1.
 */
@Deprecated(since = "1.0", forRemoval = true)
public enum PosOperatingProfile {
    RETAIL,
    RESTAURANT,
    HYBRID,
    NONE
}
