package com.erpai.integration.ird.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "company_ird_config",
        uniqueConstraints = @UniqueConstraint(name = "uk_company_ird_config_tenant_company",
                columnNames = {"tenant_id", "company_id"}))
public class CompanyIrdConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "seller_pan", nullable = false, length = 9))
    private SellerPan sellerPan;

    @Column(name = "api_username", nullable = false, length = 128)
    private String apiUsername;

    @Column(name = "api_password_encrypted", nullable = false, length = 512)
    private String apiPasswordEncrypted;

    @Enumerated(EnumType.STRING)
    @Column(name = "environment", nullable = false, length = 16)
    private IrdEnvironment environment;

    @Column(name = "is_cbms_active", nullable = false)
    private boolean cbmsActive;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 32)
    private IrdVerificationStatus verificationStatus;

    @Column(name = "last_verified_at")
    private Instant lastVerifiedAt;

    @Column(name = "last_error_message", length = 1000)
    private String lastErrorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CompanyIrdConfig() {
    }

    public CompanyIrdConfig(Long tenantId, Long companyId, SellerPan sellerPan, String apiUsername,
                           String apiPasswordEncrypted, IrdEnvironment environment) {
        this.tenantId = tenantId;
        this.companyId = companyId;
        this.sellerPan = sellerPan;
        this.apiUsername = apiUsername;
        this.apiPasswordEncrypted = apiPasswordEncrypted;
        this.environment = environment;
        this.cbmsActive = false;
        this.verificationStatus = IrdVerificationStatus.PENDING_VERIFICATION;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void updateCredentials(String apiUsername, String apiPasswordEncrypted, SellerPan sellerPan,
                                  IrdEnvironment environment) {
        this.apiUsername = apiUsername;
        this.apiPasswordEncrypted = apiPasswordEncrypted;
        this.sellerPan = sellerPan;
        this.environment = environment;
        this.verificationStatus = IrdVerificationStatus.PENDING_VERIFICATION;
        this.lastVerifiedAt = null;
        this.lastErrorMessage = null;
        this.updatedAt = Instant.now();
    }

    public void activateCbms() {
        if (verificationStatus != IrdVerificationStatus.VERIFIED) {
            throw new IllegalStateException("Cannot activate CBMS unless verification status is VERIFIED");
        }
        this.cbmsActive = true;
        this.updatedAt = Instant.now();
    }

    public void deactivateCbms() {
        this.cbmsActive = false;
        this.updatedAt = Instant.now();
    }

    public void markVerified() {
        this.verificationStatus = IrdVerificationStatus.VERIFIED;
        this.lastVerifiedAt = Instant.now();
        this.lastErrorMessage = null;
        this.updatedAt = Instant.now();
    }

    public void markFailed(String errorMessage) {
        this.verificationStatus = IrdVerificationStatus.FAILED;
        this.lastErrorMessage = errorMessage;
        this.updatedAt = Instant.now();
    }

    // getters
    public Long getId() { return id; }
    public Long getTenantId() { return tenantId; }
    public Long getCompanyId() { return companyId; }
    public SellerPan getSellerPan() { return sellerPan; }
    public String getApiUsername() { return apiUsername; }
    public String getApiPasswordEncrypted() { return apiPasswordEncrypted; }
    public IrdEnvironment getEnvironment() { return environment; }
    public boolean isCbmsActive() { return cbmsActive; }
    public IrdVerificationStatus getVerificationStatus() { return verificationStatus; }
    public Instant getLastVerifiedAt() { return lastVerifiedAt; }
    public String getLastErrorMessage() { return lastErrorMessage; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // setters for JPA/mapping
    public void setId(Long id) { this.id = id; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }
    public void setSellerPan(SellerPan sellerPan) { this.sellerPan = sellerPan; }
    public void setApiUsername(String apiUsername) { this.apiUsername = apiUsername; }
    public void setApiPasswordEncrypted(String apiPasswordEncrypted) { this.apiPasswordEncrypted = apiPasswordEncrypted; }
    public void setEnvironment(IrdEnvironment environment) { this.environment = environment; }
    public void setCbmsActive(boolean cbmsActive) { this.cbmsActive = cbmsActive; }
    public void setVerificationStatus(IrdVerificationStatus verificationStatus) { this.verificationStatus = verificationStatus; }
    public void setLastVerifiedAt(Instant lastVerifiedAt) { this.lastVerifiedAt = lastVerifiedAt; }
    public void setLastErrorMessage(String lastErrorMessage) { this.lastErrorMessage = lastErrorMessage; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
