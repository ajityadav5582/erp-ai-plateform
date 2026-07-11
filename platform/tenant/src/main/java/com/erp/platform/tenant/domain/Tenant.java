package com.erp.platform.tenant.domain;

import com.erp.platform.data.lock.OptimisticLock;
import com.erp.platform.tenant.domain.exception.CannotActivateTenantException;
import com.erp.platform.tenant.domain.exception.CannotDeactivateTenantException;
import com.erp.platform.tenant.domain.exception.CannotReactivateTenantException;
import com.erp.platform.tenant.domain.exception.CannotSuspendTenantException;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Tenant aggregate root for multi-tenant management.
 *
 * <p>Represents an Organization (Tenant) in a multi-tenant SaaS platform.
 * This is the central entity that owns all tenant-related data and enforces
 * business invariants for tenant lifecycle management.
 *
 * <p>The aggregate follows Clean Architecture and DDD principles:
 * <ul>
 *   <li>Extends {@link OptimisticLock} for optimistic locking support</li>
 *   <li>Encapsulates all tenant business logic and invariants</li>
 *   <li>Does not expose mutable state directly (no setters for business fields)</li>
 *   <li>Provides domain behavior methods for state transitions</li>
 * </ul>
 *
 * <p><strong>Lifecycle Rules:</strong>
 * <ul>
 *   <li>PENDING → ACTIVE (via activate)</li>
 *   <li>ACTIVE → SUSPENDED (via suspend)</li>
 *   <li>SUSPENDED → ACTIVE (via reactivate)</li>
 *   <li>ACTIVE/SUSPENDED → DEACTIVATED (via deactivate)</li>
 *   <li>Any state → ARCHIVED (terminal state)</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Entity
@Table(name = "tenants")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class Tenant extends OptimisticLock<Long> {

    private static final long serialVersionUID = 1L;

    /**
     * The unique business identifier for the tenant.
     * Used for API access, subdomain routing, and external references.
     * This is a UUID (not String) as per requirements.
     */
    @Column(name = "tenant_id", nullable = false, unique = true, updatable = false)
    private UUID tenantId;

    /**
     * Human-readable unique code for the tenant.
     * Used for display purposes and easier identification.
     * Must be unique across all tenants.
     */
    @Column(name = "tenant_code", nullable = false, unique = true, updatable = false)
    private String tenantCode;

    /**
     * The legal name of the organization.
     * Used for legal documents, invoices, and official communications.
     */
    @Column(name = "legal_name", nullable = false)
    private String legalName;

    /**
     * The display name of the tenant.
     * Used in UI and user-facing contexts.
     * Can be different from the legal name for branding purposes.
     */
    @Column(name = "display_name", nullable = false)
    private String displayName;

    // =====================
    // Business Information
    // =====================

    /**
     * Primary email address for the tenant.
     * Used for notifications, billing, and support communications.
     */
    @Column(nullable = false)
    private String email;

    /**
     * Primary phone number for the tenant.
     * Used for support and emergency contact.
     */
    @Column(nullable = false)
    private String phone;

    /**
     * Website URL for the tenant organization.
     * Optional field for reference and branding.
     */
    @Column(nullable = false)
    private String website;

    // ==============
    // Lifecycle
    // ==============

    /**
     * Current status of the tenant.
     * Controls access to the platform and available features.
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TenantStatus status;

    /**
     * Data isolation strategy for the tenant.
     * Determines how tenant data is physically separated.
     */
    @Column(name = "isolation_strategy", nullable = false)
    @Enumerated(EnumType.STRING)
    private IsolationStrategy isolationStrategy;

    /**
     * Timestamp when the tenant was activated.
     * Set when transitioning to ACTIVE status.
     */
    @Column(name = "activated_at")
    private Instant activatedAt;

    /**
     * Timestamp when the tenant was suspended.
     * Set when transitioning to SUSPENDED status.
     */
    @Column(name = "suspended_at")
    private Instant suspendedAt;

    /**
     * Timestamp when the tenant was deactivated.
     * Set when transitioning to DEACTIVATED status.
     */
    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    // ==============
    // Localization
    // ==============

    /**
     * Timezone for the tenant.
     * Used for scheduling, reporting, and user interface display.
     */
    @Column(nullable = false)
    private String timezone;

    /**
     * Default currency for the tenant.
     * Used for financial transactions and reporting.
     * ISO 4217 currency code (e.g., "USD", "EUR", "NPR").
     */
    @Column(nullable = false)
    private String currency;

    /**
     * Default language for the tenant.
     * Used for UI localization and document generation.
     * ISO 639-1 language code (e.g., "en", "es", "ne").
     */
    @Column(nullable = false)
    private String language;

    // ==============
    // Branding
    // ==============

    /**
     * URL to the tenant's logo image.
     * Used in UI branding and white-labeling.
     */
    @Column(name = "logo_url")
    private String logoUrl;

    /**
     * URL to the tenant's favicon.
     * Used in browser tabs and bookmarks.
     */
    @Column(name = "favicon_url")
    private String faviconUrl;

    // ==============
    // Subscription
    // ==============

    /**
     * Reference to the subscription plan.
     * Prepared for future implementation of SubscriptionPlan entity.
     * Currently stores the plan ID for relationship establishment.
     */
    @Column(name = "subscription_plan_id")
    private UUID subscriptionPlanId;

    /**
     * Factory method to create a new tenant in PENDING status.
     *
     * <p>The application layer is responsible for providing the tenantId and tenantCode.
     * This ensures proper UUID generation and code validation at the application layer.
     *
     * @param tenantId the unique tenant identifier (UUID)
     * @param tenantCode the human-readable unique code
     * @param legalName the legal name of the organization
     * @param displayName the display name for UI
     * @param email the primary email address
     * @param phone the primary phone number
     * @param website the website URL
     * @param timezone the timezone for the tenant
     * @param currency the default currency
     * @param language the default language
     * @param isolationStrategy the data isolation strategy
     * @return a new Tenant instance in PENDING status
     */
    public static Tenant create(
            UUID tenantId,
            String tenantCode,
            String legalName,
            String displayName,
            String email,
            String phone,
            String website,
            String timezone,
            String currency,
            String language,
            IsolationStrategy isolationStrategy) {
        return Tenant.builder()
                .tenantId(tenantId)
                .tenantCode(tenantCode)
                .legalName(legalName)
                .displayName(displayName)
                .email(email)
                .phone(phone)
                .website(website)
                .timezone(timezone)
                .currency(currency)
                .language(language)
                .status(TenantStatus.PENDING)
                .isolationStrategy(isolationStrategy)
                .build();
    }

    /**
     * Activates the tenant.
     *
     * <p>Transitions the tenant from PENDING, TRIAL, or EXPIRED status to ACTIVE.
     * Sets the activatedAt timestamp to track when activation occurred.
     *
     * <p><strong>Business Invariant:</strong> Cannot activate a tenant that is already active,
     * suspended, deactivated, or archived.
     *
     * @param activatedAt the timestamp when activation occurs (provided by application layer)
     * @throws CannotActivateTenantException if the tenant cannot be activated in its current state
     */
    public void activate(Instant activatedAt) {
        if (this.status == TenantStatus.ACTIVE) {
            throw new CannotActivateTenantException(this.tenantId, this.status);
        }
        if (this.status == TenantStatus.SUSPENDED) {
            throw new CannotActivateTenantException(this.tenantId, this.status);
        }
        if (this.status == TenantStatus.DEACTIVATED) {
            throw new CannotActivateTenantException(this.tenantId, this.status);
        }
        if (this.status == TenantStatus.ARCHIVED) {
            throw new CannotActivateTenantException(this.tenantId, this.status);
        }

        this.status = TenantStatus.ACTIVE;
        this.activatedAt = activatedAt;
        this.suspendedAt = null;
    }

    /**
     * Suspends the tenant.
     *
     * <p>Transitions the tenant from ACTIVE status to SUSPENDED.
     * Sets the suspendedAt timestamp to track when suspension occurred.
     *
     * <p><strong>Business Invariant:</strong> Cannot suspend a tenant that is not active.
     *
     * @param suspendedAt the timestamp when suspension occurs (provided by application layer)
     * @throws CannotSuspendTenantException if the tenant cannot be suspended in its current state
     */
    public void suspend(Instant suspendedAt) {
        if (this.status != TenantStatus.ACTIVE) {
            throw new CannotSuspendTenantException(this.tenantId, this.status);
        }

        this.status = TenantStatus.SUSPENDED;
        this.suspendedAt = suspendedAt;
    }

    /**
     * Reactivates a suspended tenant.
     *
     * <p>Transitions the tenant from SUSPENDED status back to ACTIVE.
     * Clears the suspendedAt timestamp.
     *
     * <p><strong>Business Invariant:</strong> Cannot reactivate a tenant that is not suspended.
     *
     * @param reactivatedAt the timestamp when reactivation occurs (provided by application layer)
     * @throws CannotReactivateTenantException if the tenant cannot be reactivated in its current state
     */
    public void reactivate(Instant reactivatedAt) {
        if (this.status != TenantStatus.SUSPENDED) {
            throw new CannotReactivateTenantException(this.tenantId, this.status);
        }

        this.status = TenantStatus.ACTIVE;
        this.activatedAt = reactivatedAt;
        this.suspendedAt = null;
    }

    /**
     * Deactivates the tenant.
     *
     * <p>Transitions the tenant from ACTIVE or SUSPENDED status to DEACTIVATED.
     * Sets the deactivatedAt timestamp to track when deactivation occurred.
     *
     * <p><strong>Business Invariant:</strong> Cannot deactivate a tenant that is not active or suspended.
     *
     * @param deactivatedAt the timestamp when deactivation occurs (provided by application layer)
     * @throws CannotDeactivateTenantException if the tenant cannot be deactivated in its current state
     */
    public void deactivate(Instant deactivatedAt) {
        if (this.status != TenantStatus.ACTIVE && this.status != TenantStatus.SUSPENDED) {
            throw new CannotDeactivateTenantException(this.tenantId, this.status);
        }

        this.status = TenantStatus.DEACTIVATED;
        this.deactivatedAt = deactivatedAt;
    }

    /**
     * Archives the tenant.
     *
     * <p>Transitions the tenant to ARCHIVED status.
     * This is a terminal state - archived tenants cannot be reactivated.
     * Used for compliance and long-term data retention.
     *
     * @param archivedAt the timestamp when archiving occurs (provided by application layer)
     */
    public void archive(Instant archivedAt) {
        this.status = TenantStatus.ARCHIVED;
        this.deactivatedAt = archivedAt;
    }

    /**
     * Marks the tenant subscription as expired.
     *
     * <p>Transitions the tenant to EXPIRED status.
     * Used when subscription payment fails or term ends.
     *
     * @param expiredAt the timestamp when expiration occurs (provided by application layer)
     */
    public void expire(Instant expiredAt) {
        if (this.status == TenantStatus.ARCHIVED || this.status == TenantStatus.DEACTIVATED) {
            return; // Already in terminal state
        }
        this.status = TenantStatus.EXPIRED;
    }

    /**
     * Checks if the tenant is active.
     *
     * @return true if the tenant status is ACTIVE, false otherwise
     */
    public boolean isActive() {
        return this.status == TenantStatus.ACTIVE;
    }

    /**
     * Checks if the tenant is in a suspended state.
     *
     * @return true if the tenant status is SUSPENDED, false otherwise
     */
    public boolean isSuspended() {
        return this.status == TenantStatus.SUSPENDED;
    }

    /**
     * Checks if the tenant is in a pending state.
     *
     * @return true if the tenant status is PENDING, false otherwise
     */
    public boolean isPending() {
        return this.status == TenantStatus.PENDING;
    }

    /**
     * Checks if the tenant is in a trial state.
     *
     * @return true if the tenant status is TRIAL, false otherwise
     */
    public boolean isTrial() {
        return this.status == TenantStatus.TRIAL;
    }

    /**
     * Checks if the tenant is in an expired state.
     *
     * @return true if the tenant status is EXPIRED, false otherwise
     */
    public boolean isExpired() {
        return this.status == TenantStatus.EXPIRED;
    }

    /**
     * Checks if the tenant is deactivated.
     *
     * @return true if the tenant status is DEACTIVATED, false otherwise
     */
    public boolean isDeactivated() {
        return this.status == TenantStatus.DEACTIVATED;
    }

    /**
     * Checks if the tenant is archived.
     *
     * @return true if the tenant status is ARCHIVED, false otherwise
     */
    public boolean isArchived() {
        return this.status == TenantStatus.ARCHIVED;
    }

    /**
     * Checks if the tenant can be activated.
     *
     * <p>A tenant can be activated if it is in PENDING, TRIAL, or EXPIRED status.
     *
     * @return true if the tenant can be activated, false otherwise
     */
    public boolean canBeActivated() {
        return this.status == TenantStatus.PENDING ||
               this.status == TenantStatus.TRIAL ||
               this.status == TenantStatus.EXPIRED;
    }

    /**
     * Checks if the tenant can be suspended.
     *
     * <p>A tenant can be suspended if it is in ACTIVE status.
     *
     * @return true if the tenant can be suspended, false otherwise
     */
    public boolean canBeSuspended() {
        return this.status == TenantStatus.ACTIVE;
    }

    /**
     * Checks if the tenant can be reactivated.
     *
     * <p>A tenant can be reactivated if it is in SUSPENDED status.
     *
     * @return true if the tenant can be reactivated, false otherwise
     */
    public boolean canBeReactivated() {
        return this.status == TenantStatus.SUSPENDED;
    }

    /**
     * Checks if the tenant can be deactivated.
     *
     * <p>A tenant can be deactivated if it is in ACTIVE or SUSPENDED status.
     *
     * @return true if the tenant can be deactivated, false otherwise
     */
    public boolean canBeDeactivated() {
        return this.status == TenantStatus.ACTIVE || this.status == TenantStatus.SUSPENDED;
    }

    /**
     * Updates the tenant's branding information.
     *
     * @param displayName the new display name
     * @param logoUrl the new logo URL
     * @param faviconUrl the new favicon URL
     */
    public void updateBranding(String displayName, String logoUrl, String faviconUrl) {
        if (displayName != null && !displayName.isBlank()) {
            this.displayName = displayName;
        }
        if (logoUrl != null && !logoUrl.isBlank()) {
            this.logoUrl = logoUrl;
        }
        if (faviconUrl != null && !faviconUrl.isBlank()) {
            this.faviconUrl = faviconUrl;
        }
    }

    /**
     * Updates the tenant's business contact information.
     *
     * @param email the new email address
     * @param phone the new phone number
     * @param website the new website URL
     */
    public void updateContactInfo(String email, String phone, String website) {
        if (email != null && !email.isBlank()) {
            this.email = email;
        }
        if (phone != null && !phone.isBlank()) {
            this.phone = phone;
        }
        if (website != null && !website.isBlank()) {
            this.website = website;
        }
    }

    /**
     * Updates the tenant's localization settings.
     *
     * @param timezone the new timezone
     * @param currency the new currency
     * @param language the new language
     */
    public void updateLocalization(String timezone, String currency, String language) {
        if (timezone != null && !timezone.isBlank()) {
            this.timezone = timezone;
        }
        if (currency != null && !currency.isBlank()) {
            this.currency = currency;
        }
        if (language != null && !language.isBlank()) {
            this.language = language;
        }
    }

    /**
     * Associates a subscription plan with this tenant.
     *
     * <p>Prepared for future implementation of SubscriptionPlan entity.
     *
     * @param subscriptionPlanId the subscription plan ID to associate
     */
    public void assignSubscriptionPlan(UUID subscriptionPlanId) {
        this.subscriptionPlanId = subscriptionPlanId;
    }

    /**
     * Removes the subscription plan association from this tenant.
     */
    public void removeSubscriptionPlan() {
        this.subscriptionPlanId = null;
    }

    /**
     * Checks if the tenant has an associated subscription plan.
     *
     * @return true if a subscription plan is assigned, false otherwise
     */
    public boolean hasSubscriptionPlan() {
        return this.subscriptionPlanId != null;
    }
}
