package com.erp.platform.identity.domain;

import com.erp.platform.data.lock.OptimisticLock;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Company aggregate root for multi-company management within a tenant.
 *
 * <p>Represents a distinct legal/business entity owned by a Tenant. A single
 * Tenant can own multiple Companies (e.g. different brands, subsidiaries, or
 * trading names). A Company is the operational unit that users are assigned to
 * for day-to-day business operations (sales, finance, inventory, etc.).
 *
 * <p><strong>Tenant vs Company:</strong>
 * <ul>
 *   <li>A <b>Tenant</b> is the top-level SaaS account — the organization that
 *       subscribes to the platform and owns all data.</li>
 *   <li>A <b>Company</b> is a distinct legal/business entity within that tenant.
 *       One tenant can have many companies, but a company belongs to exactly
 *       one tenant.</li>
 * </ul>
 *
 * <p>The aggregate follows Clean Architecture and DDD principles:
 * <ul>
 *   <li>Extends {@link OptimisticLock} for optimistic locking support</li>
 *   <li>Encapsulates all company business logic and invariants</li>
 *   <li>Does not expose mutable state directly (no setters for business fields)</li>
 *   <li>Provides domain behavior methods for state transitions</li>
 * </ul>
 *
 * <p><strong>Business Rules:</strong>
 * <ul>
 *   <li>Company code must be unique within a tenant</li>
 *   <li>Company name must be unique within a tenant</li>
 *   <li>A company belongs to exactly one tenant</li>
 *   <li>Only active companies can have new assignments (users, branches, etc.)</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Entity
@Table(name = "companies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class Company extends OptimisticLock<Long> {

    private static final long serialVersionUID = 1L;

    /** Internal business identifier required by the existing companies table schema. */
    @Column(name = "company_id", nullable = false, unique = true, updatable = false)
    private UUID companyId;

    /**
     * The tenant this company belongs to.
     * A company cannot belong to multiple tenants.
     */
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    /**
     * Unique company code within the tenant.
     * Used for display, reporting, and integration.
     * Must be unique within the tenant scope.
     */
    @Column(name = "company_code", nullable = false, length = 50)
    private String companyCode;

    /**
     * Unique company name within the tenant.
     * Human-readable name for the company.
     * Must be unique within the tenant scope.
     */
    @Column(name = "company_name", nullable = false, length = 200)
    private String companyName;

    @Column(name = "pan_number", length = 30)
    private String panNumber;

    @Column(name = "city", length = 100)
    private String city;

    /**
     * Legal name of the company.
     * Used for legal documents, invoices, and official communications.
     */
    @Column(name = "legal_name", length = 200)
    private String legalName;

    /**
     * Primary email address for the company.
     * Used for notifications and communications.
     */
    @Column(name = "email", length = 255)
    private String email;

    /**
     * Primary phone number for the company.
     * Used for communications and emergency contacts.
     */
    @Column(name = "phone", length = 50)
    private String phone;

    /**
     * Website URL for the company.
     * Optional field for reference and branding.
     */
    @Column(name = "website", length = 200)
    private String website;

    /**
     * Street address of the company.
     */
    @Column(name = "address", length = 500)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "company_type", length = 40)
    private CompanyType companyType;

    @Enumerated(EnumType.STRING)
    @Column(name = "fiscal_year_type", length = 20)
    private FiscalYearType fiscalYearType;

    @Column(name = "vat_registered")
    private Boolean vatRegistered;

    /**
     * Current status of the company.
     * Determines whether the company is operational.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CompanyStatus status;

    // ==================== Domain Methods ====================

    /**
     * Activates the company, making it operational.
     *
     * @throws IllegalStateException if company is already active
     */
    public void activate() {
        if (this.status == CompanyStatus.ACTIVE) {
            throw new IllegalStateException("Company is already active");
        }
        this.status = CompanyStatus.ACTIVE;
    }

    /**
     * Deactivates the company, making it non-operational.
     *
     * @throws IllegalStateException if company is not active
     */
    public void deactivate() {
        if (this.status != CompanyStatus.ACTIVE) {
            throw new IllegalStateException("Only active companies can be deactivated");
        }
        this.status = CompanyStatus.INACTIVE;
    }

    /**
     * Updates the company's contact and branding information.
     *
     * @param legalName the new legal name
     * @param email the new email address
     * @param phone the new phone number
     * @param website the new website URL
     * @param address the new street address
     */
    public void updateDetails(String legalName, String email, String phone, String website, String address) {
        if (legalName != null && !legalName.isBlank()) {
            this.legalName = legalName.trim();
        }
        if (email != null && !email.isBlank()) {
            this.email = email.trim();
        }
        if (phone != null && !phone.isBlank()) {
            this.phone = phone.trim();
        }
        if (website != null && !website.isBlank()) {
            this.website = website.trim();
        }
        if (address != null && !address.isBlank()) {
            this.address = address.trim();
        }
    }

    // ==================== Factory Methods ====================

    /**
     * Creates a new Company instance.
     *
     * <p>Business Rules:
     * <ul>
     *   <li>tenantId must not be null</li>
     *   <li>companyCode must not be blank</li>
     *   <li>companyName must not be blank</li>
     *   <li>status defaults to ACTIVE</li>
     * </ul>
     *
     * @param tenantId the tenant this company belongs to
     * @param companyCode the unique company code within the tenant
     * @param companyName the unique company name within the tenant
     * @return a new Company instance
     * @throws IllegalArgumentException if any required field is invalid
     */
    public static Company create(Long tenantId, String companyCode, String companyName) {
        validateTenantId(tenantId);
        validateCompanyCode(companyCode);
        validateCompanyName(companyName);

        return Company.builder()
                .companyId(UUID.randomUUID())
                .tenantId(tenantId)
                .companyCode(companyCode.trim().toUpperCase())
                .companyName(companyName.trim())
                .status(CompanyStatus.ACTIVE)
                .build();
    }

    public static Company create(Long tenantId, String companyCode, String companyName,
                                 String panNumber, String city, String phone, String email,
                                 CompanyType companyType, FiscalYearType fiscalYearType,
                                 boolean vatRegistered) {
        Company company = create(tenantId, companyCode, companyName);
        company.panNumber = panNumber.trim();
        company.city = city.trim();
        company.phone = phone.trim();
        company.email = email.trim();
        company.companyType = companyType;
        company.fiscalYearType = fiscalYearType;
        company.vatRegistered = vatRegistered;
        return company;
    }

    // ==================== Validation Methods ====================

    private static void validateTenantId(Long tenantId) {
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalArgumentException("Tenant ID must be a positive number");
        }
    }

    private static void validateCompanyCode(String companyCode) {
        if (companyCode == null || companyCode.isBlank()) {
            throw new IllegalArgumentException("Company code must not be blank");
        }
        if (companyCode.length() > 50) {
            throw new IllegalArgumentException("Company code must not exceed 50 characters");
        }
    }

    private static void validateCompanyName(String companyName) {
        if (companyName == null || companyName.isBlank()) {
            throw new IllegalArgumentException("Company name must not be blank");
        }
        if (companyName.length() > 200) {
            throw new IllegalArgumentException("Company name must not exceed 200 characters");
        }
    }
}
