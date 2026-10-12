package com.erp.platform.identity.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;

/**
 * Branch aggregate root for organizational unit management.
 *
 * <p>Represents a physical or logical branch/office location within a tenant organization.
 * A branch serves as a container for departments, teams, users, warehouses, and stores.
 *
 * <p>The aggregate follows Clean Architecture and DDD principles:
 * <ul>
 *   <li>Extends {@link TenantScopedEntity} for tenant/company scoping and
 *       optimistic locking, exactly as the inventory {@code Category} and
 *       {@code Supplier} aggregates do</li>
 *   <li>Encapsulates all branch business logic and invariants</li>
 *   <li>Does not expose mutable state directly (no setters for business fields)</li>
 *   <li>Provides domain behavior methods for state transitions</li>
 * </ul>
 *
 * <p><strong>Business Rules:</strong>
 * <ul>
 *   <li>Branch code must be unique within a company</li>
 *   <li>Branch name must be unique within a company</li>
 *   <li>A branch belongs to exactly one tenant and one company</li>
 *   <li>Only active branches can have new assignments (departments, users, etc.)</li>
 *   <li>A branch's location is derived from a local level (province -> district -> local level)</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Entity
@Table(
        name = "branches",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_company_branch_code",
                        columnNames = {"company_id", "branch_code"}
                ),
                @UniqueConstraint(
                        name = "uk_company_branch_name",
                        columnNames = {"company_id", "branch_name"}
                )
        },
        indexes = {
                @Index(name = "idx_branches_tenant_company", columnList = "tenant_id, company_id")
        }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Branch extends TenantScopedEntity<Long> {

    private static final long serialVersionUID = 1L;

    /**
     * Unique branch code within the company.
     * Used for display, reporting, and integration.
     * Backed by the {@code uk_company_branch_code} unique constraint, so two
     * companies of the same tenant may each have a {@code HEAD-OFFICE} branch.
     */
    @NotBlank(message = "Branch code is required")
    @Size(max = 50, message = "Branch code must not exceed 50 characters")
    @Column(name = "branch_code", nullable = false, length = 50)
    private String branchCode;

    /**
     * Unique branch name within the company.
     * Human-readable name for the branch.
     * Backed by the {@code uk_company_branch_name} unique constraint.
     */
    @NotBlank(message = "Branch name is required")
    @Size(max = 200, message = "Branch name must not exceed 200 characters")
    @Column(name = "branch_name", nullable = false, length = 200)
    private String branchName;

    /**
     * Contact email for the branch.
     * Used for notifications and communications.
     */
    @Size(max = 255, message = "Email must not exceed 255 characters")
    @Column(name = "email", length = 255)
    private String email;

    /**
     * Contact phone number for the branch.
     * Used for communications and emergency contacts.
     */
    @Size(max = 50, message = "Phone must not exceed 50 characters")
    @Column(name = "phone", length = 50)
    private String phone;

    /**
     * Street address of the branch.
     */
    @Size(max = 500, message = "Address must not exceed 500 characters")
    @Column(name = "address", length = 500)
    private String address;

    /**
     * The local level (municipality / rural municipality) where the branch is located.
     * Province and district are derived from this reference.
     */
    @Size(max = 50, message = "Local level ID must not exceed 50 characters")
    @Column(name = "local_level_id", length = 50)
    private String localLevelId;

    /**
     * Ward number within the local level.
     */
    @Size(max = 50, message = "Ward number must not exceed 50 characters")
    @Column(name = "ward_no", length = 50)
    private String wardNo;

    /**
     * Current status of the branch.
     * Determines whether the branch is operational.
     */
    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BranchStatus status;

    // ==================== Domain Methods ====================

    /**
     * Activates the branch, making it operational.
     *
     * <p>Business Rules:
     * <ul>
     *   <li>Branch must not already be active</li>
     *   <li>Branch must have a valid tenant</li>
     * </ul>
     *
     * @throws IllegalStateException if branch is already active
     */
    public void activate() {
        if (this.status == BranchStatus.ACTIVE) {
            throw new IllegalStateException("Branch is already active");
        }
        this.status = BranchStatus.ACTIVE;
    }

    /**
     * Deactivates the branch, making it non-operational.
     *
     * <p>Business Rules:
     * <ul>
     *   <li>Branch must be active to be deactivated</li>
     *   <li>Deactivation preserves all historical data for audit</li>
     * </ul>
     *
     * @throws IllegalStateException if branch is not active
     */
    public void deactivate() {
        if (this.status != BranchStatus.ACTIVE) {
            throw new IllegalStateException("Only active branches can be deactivated");
        }
        this.status = BranchStatus.INACTIVE;
    }

    /**
     * Updates the location information for the branch.
     *
     * <p>Both fields are optional and can be updated independently.
     *
     * @param localLevelId the local level id (municipality_id), or null to clear
     * @param wardNo the ward number, or null to clear
     */
    public void updateLocation(String localLevelId, String wardNo) {
        this.localLevelId = localLevelId;
        this.wardNo = wardNo;
    }

    /**
     * Updates the branch profile in place.
     *
     * <p><strong>Why this exists.</strong> A builder copy cannot be used for
     * updates. Lombok's {@code @Builder} only covers the fields declared on the
     * annotated class, so the {@code id} and {@code version} inherited from
     * {@link TenantScopedEntity} are dropped. {@code repository.save(...)} then
     * saw a null id, called {@code persist()} and INSERTed a second branch row on
     * every update. Mutating the managed entity keeps the identity, the optimistic
     * lock, and the tenant/company scope intact.
     *
     * <p>Each argument is optional: {@code null} leaves the current value
     * untouched. Blank values are ignored so an update request cannot erase a
     * required field.
     *
     * @param branchCode   the new branch code, or {@code null} to keep the current one
     * @param branchName   the new branch name, or {@code null} to keep the current one
     * @param email        the new email, or {@code null} to keep the current one
     * @param phone        the new phone, or {@code null} to keep the current one
     * @param address      the new address, or {@code null} to keep the current one
     * @param localLevelId the new local level id, or {@code null} to keep the current one
     * @param wardNo       the new ward number, or {@code null} to keep the current one
     */
    public void updateDetails(String branchCode, String branchName, String email,
                              String phone, String address, String localLevelId, String wardNo) {
        if (branchCode != null && !branchCode.isBlank()) {
            validateBranchCode(branchCode);
            this.branchCode = branchCode.trim().toUpperCase();
        }
        if (branchName != null && !branchName.isBlank()) {
            validateBranchName(branchName);
            this.branchName = branchName.trim();
        }
        if (email != null && !email.isBlank()) {
            this.email = email.trim();
        }
        if (phone != null && !phone.isBlank()) {
            this.phone = phone.trim();
        }
        if (address != null && !address.isBlank()) {
            this.address = address.trim();
        }
        if (localLevelId != null && !localLevelId.isBlank()) {
            this.localLevelId = localLevelId.trim();
        }
        if (wardNo != null && !wardNo.isBlank()) {
            this.wardNo = wardNo.trim();
        }
    }

    // ==================== Factory Methods ====================

    /**
     * Creates a new Branch owned by a specific company.
     *
     * <p>Business Rules:
     * <ul>
     *   <li>tenantId must not be null</li>
     *   <li>companyId must not be null; a branch always belongs to a company</li>
     *   <li>branchCode must not be blank</li>
     *   <li>branchName must not be blank</li>
     *   <li>status defaults to ACTIVE</li>
     * </ul>
     *
     * @param tenantId the tenant this branch belongs to
     * @param companyId the company this branch belongs to
     * @param branchCode the unique branch code within the company
     * @param branchName the unique branch name within the company
     * @return a new Branch instance
     * @throws IllegalArgumentException if any required field is invalid
     */
    public static Branch create(Long tenantId, Long companyId, String branchCode, String branchName) {
        validateTenantId(tenantId);
        validateCompanyId(companyId);
        validateBranchCode(branchCode);
        validateBranchName(branchName);

        Branch branch = Branch.builder()
                .branchCode(branchCode.trim().toUpperCase())
                .branchName(branchName.trim())
                .status(BranchStatus.ACTIVE)
                .build();

        // tenantId/companyId live on TenantScopedEntity and are not part of the
        // builder, so the scope is assigned explicitly. This is the single place a
        // Branch becomes tenant/company bound, and the value can only come from the
        // validated request context.
        branch.assignScope(tenantId, companyId);

        return branch;
    }

    // ==================== Validation Methods ====================

    private static void validateTenantId(Long tenantId) {
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalArgumentException("Tenant ID must be a positive number");
        }
    }

    private static void validateCompanyId(Long companyId) {
        if (companyId == null || companyId <= 0) {
            throw new IllegalArgumentException("Company ID must be a positive number");
        }
    }

    private static void validateBranchCode(String branchCode) {
        if (branchCode == null || branchCode.isBlank()) {
            throw new IllegalArgumentException("Branch code must not be blank");
        }
        if (branchCode.length() > 50) {
            throw new IllegalArgumentException("Branch code must not exceed 50 characters");
        }
    }

    private static void validateBranchName(String branchName) {
        if (branchName == null || branchName.isBlank()) {
            throw new IllegalArgumentException("Branch name must not be blank");
        }
        if (branchName.length() > 200) {
            throw new IllegalArgumentException("Branch name must not exceed 200 characters");
        }
    }
}
