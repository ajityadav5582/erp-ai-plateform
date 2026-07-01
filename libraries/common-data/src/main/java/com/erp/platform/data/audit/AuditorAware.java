package com.erp.platform.data.audit;

/**
 * Auditor aware interface for JPA auditing.
 *
 * <p>Provides the current auditor (user) for audit fields.
 * Implementations should return the current user ID from the security context.
 *
 * @since 1.0.0
 */
public interface AuditorAware extends org.springframework.data.domain.AuditorAware<String> {
}
