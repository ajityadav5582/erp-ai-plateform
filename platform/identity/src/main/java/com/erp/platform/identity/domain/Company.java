package com.erp.platform.identity.domain;

import com.erp.platform.data.lock.OptimisticLock;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
@Entity
@Table(name = "companies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class Company extends OptimisticLock<Long> {

    private static final long serialVersionUID = 1L;

    @Column(name = "company_id", nullable = false, unique = true, updatable = false)
    private UUID companyId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "company_code", nullable = false, length = 50)
    private String companyCode;

    @Column(name = "company_name", nullable = false, length = 200)
    private String companyName;

    @Column(name = "pan_number", length = 30)
    private String panNumber;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "legal_name", length = 200)
    private String legalName;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "website", length = 200)
    private String website;

    @Column(name = "address", length = 500)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "company_type", length = 40)
    private CompanyType companyType;

    @Enumerated(EnumType.STRING)
    @Column(name = "business_type", length = 40)
    private BusinessType businessType;

    @Enumerated(EnumType.STRING)
    @Column(name = "fiscal_year_type", length = 20)
    private FiscalYearType fiscalYearType;

    @Column(name = "vat_registered")
    private Boolean vatRegistered;

    /** VAT registration number issued by IRD (separate from PAN). */
    @Column(name = "vat_registration_number", length = 30)
    private String vatRegistrationNumber;

    /** Effective start date of VAT registration. */
    @Column(name = "vat_effective_from")
    private LocalDate vatEffectiveFrom;

    /** Effective end date of VAT registration (null if still active). */
    @Column(name = "vat_effective_to")
    private LocalDate vatEffectiveTo;

    /**
     * Authoritative IRD tax scheme for this company. Replaces the legacy
     * {@code vatRegistered} boolean; kept in sync with it during the
     * transition. Nullable for backward compatibility with rows created
     * before V46.
     */
    @Getter(AccessLevel.NONE)
    @Enumerated(EnumType.STRING)
    @Column(name = "tax_scheme", length = 20)
    private TaxScheme taxScheme;

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

    public void updateCompanyName(String companyName) {
        if (companyName != null && !companyName.isBlank()) {
            this.companyName = companyName.trim();
        }
    }

    public void updatePanNumber(String panNumber) {
        if (panNumber != null && !panNumber.isBlank()) {
            this.panNumber = panNumber.trim();
        }
    }

    public void updateCity(String city) {
        if (city != null && !city.isBlank()) {
            this.city = city.trim();
        }
    }

    public void updateCompanyType(CompanyType companyType) {
        if (companyType != null) {
            this.companyType = companyType;
        }
    }

    public void updateBusinessType(BusinessType businessType) {
        if (businessType != null) {
            this.businessType = businessType;
        }
    }

    public void updateFiscalYearType(FiscalYearType fiscalYearType) {
        if (fiscalYearType != null) {
            this.fiscalYearType = fiscalYearType;
        }
    }

    public void updateVatRegistered(Boolean vatRegistered) {
        if (vatRegistered != null) {
            this.vatRegistered = vatRegistered;
            // Keep the authoritative tax scheme in sync during transition.
            this.taxScheme = vatRegistered ? TaxScheme.VAT_REGISTERED : TaxScheme.PAN_ONLY;
        }
    }

    public void updateVatRegistration(String vatRegistrationNumber, LocalDate effectiveFrom, LocalDate effectiveTo) {
        if (vatRegistrationNumber != null) {
            this.vatRegistrationNumber = vatRegistrationNumber.trim();
        }
        if (effectiveFrom != null) {
            this.vatEffectiveFrom = effectiveFrom;
        }
        if (effectiveTo != null) {
            this.vatEffectiveTo = effectiveTo;
        }
    }

    public void updateTaxScheme(TaxScheme taxScheme) {
        if (taxScheme != null) {
            this.taxScheme = taxScheme;
            this.vatRegistered = taxScheme.isVatApplicable();
        }
    }

    /**
     * Authoritative IRD tax scheme. Falls back to the legacy {@code vatRegistered}
     * boolean for rows created before V46 (where {@code tax_scheme} is null).
     */
    public TaxScheme getTaxScheme() {
        if (this.taxScheme != null) {
            return this.taxScheme;
        }
        return Boolean.TRUE.equals(this.vatRegistered)
                ? TaxScheme.VAT_REGISTERED
                : TaxScheme.PAN_ONLY;
    }

    /**
     * Derived convenience accessor. Prefer {@link #getTaxScheme()} in new code.
     */
    public boolean isVatRegistered() {
        return getTaxScheme().isVatApplicable();
    }

    /**
     * Checks whether VAT registration is effective as of the given date.
     */
    public boolean isVatEffectiveAsOf(LocalDate asOf) {
        if (!getTaxScheme().isVatApplicable()) {
            return false;
        }
        LocalDate check = (asOf == null) ? LocalDate.now() : asOf;
        if (vatEffectiveFrom != null && check.isBefore(vatEffectiveFrom)) {
            return false;
        }
        if (vatEffectiveTo != null && check.isAfter(vatEffectiveTo)) {
            return false;
        }
        return true;
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
                                 CompanyType companyType, BusinessType businessType,
                                 FiscalYearType fiscalYearType, boolean vatRegistered) {
        Company company = create(tenantId, companyCode, companyName);
        company.panNumber = panNumber == null ? null : panNumber.trim();
        company.city = city == null ? null : city.trim();
        company.phone = phone == null ? null : phone.trim();
        company.email = email == null ? null : email.trim();
        company.companyType = companyType;
        company.businessType = businessType == null ? BusinessType.OTHER : businessType;
        company.fiscalYearType = fiscalYearType;
        company.vatRegistered = vatRegistered;
        return company;
    }

    /**
     * Backwards-compatible overload that defaults {@link BusinessType} to {@link BusinessType#OTHER}.
     */
    public static Company create(Long tenantId, String companyCode, String companyName,
                                 String panNumber, String city, String phone, String email,
                                 CompanyType companyType, FiscalYearType fiscalYearType,
                                 boolean vatRegistered) {
        return create(tenantId, companyCode, companyName, panNumber, city, phone, email,
                companyType, BusinessType.OTHER, fiscalYearType, vatRegistered);
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
