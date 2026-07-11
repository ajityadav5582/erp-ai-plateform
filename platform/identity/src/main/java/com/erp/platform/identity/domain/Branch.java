package com.erp.platform.identity.domain;

import com.erp.platform.data.lock.OptimisticLock;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Branch aggregate root for organizational unit management.
 *
 * <p>Represents a physical or logical branch/office location within a tenant organization.
 * A branch serves as a container for departments, teams, users, warehouses, and stores.
 *
 * <p>The aggregate follows Clean Architecture and DDD principles:
 * <ul>
 *   <li>Extends {@link OptimisticLock} for optimistic locking support</li>
 *   <li>Encapsulates all branch business logic and invariants</li>
 *   <li>Does not expose mutable state directly (no setters for business fields)</li>
 *   <li>Provides domain behavior methods for state transitions</li>
 * </ul>
 *
 * <p><strong>Business Rules:</strong>
 * <ul>
 *   <li>Branch code must be unique within a tenant</li>
 *   <li>Branch name must be unique within a tenant</li>
 *   <li>A branch belongs to exactly one tenant</li>
 *   <li>Only active branches can have new assignments (departments, users, etc.)</li>
 * </ul>
 *
 * <p><strong>Future Extensibility:</strong>
 * <p>The schema is designed to support future assignment of:
 * <ul>
 *   <li>Departments (via department.branch_id)</li>
 *   <li>Teams (via team.branch_id)</li>
 *   <li>Users (via user.branch_id)</li>
 *   <li>Warehouses (via warehouse.branch_id)</li>
 *   <li>Stores (via store.branch_id)</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Entity
@Table(name = "branches")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class Branch extends OptimisticLock<Long> {

    private static final long serialVersionUID = 1L;

    /**
     * The unique business identifier for the branch.
     * Used for API access, external references, and integration.
     * This is a UUID (not String) as per requirements.
     */
    @Column(name = "branch_id", nullable = false, unique = true, updatable = false)
    private UUID branchId;

    /**
     * The tenant this branch belongs to.
     * A branch cannot belong to multiple tenants.
     */
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    /**
     * Unique branch code within the tenant.
     * Used for display, reporting, and integration.
     * Must be unique within the tenant scope.
     */
    @Column(name = "branch_code", nullable = false, length = 50)
    private String branchCode;

    /**
     * Unique branch name within the tenant.
     * Human-readable name for the branch.
     * Must be unique within the tenant scope.
     */
    @Column(name = "branch_name", nullable = false, length = 200)
    private String branchName;

    /**
     * Contact email for the branch.
     * Used for notifications and communications.
     */
    @Column(name = "email", length = 255)
    private String email;

    /**
     * Contact phone number for the branch.
     * Used for communications and emergency contacts.
     */
    @Column(name = "phone", length = 50)
    private String phone;

    /**
     * Street address of the branch.
     */
    @Column(name = "address", length = 500)
    private String address;

    /**
     * City where the branch is located.
     */
    @Column(name = "city", length = 100)
    private String city;

    /**
     * State or province where the branch is located.
     */
    @Column(name = "state", length = 100)
    private String state;

    /**
     * Country where the branch is located.
     */
    @Column(name = "country", length = 100)
    private String country;

    /**
     * Postal or ZIP code of the branch location.
     */
    @Column(name = "postal_code", length = 20)
    private String postalCode;

    /**
     * Timezone identifier for the branch.
     * Used for scheduling, reporting, and time-based operations.
     * Example: "Asia/Kathmandu", "America/New_York"
     */
    @Column(name = "timezone", length = 50)
    private String timezone;

    /**
     * Currency code for the branch.
     * Used for financial transactions and reporting.
     * Example: "NPR", "USD", "EUR"
     */
    @Column(name = "currency", length = 3)
    private String currency;

    /**
     * The user ID of the branch manager.
     * References the user who manages this branch.
     * Nullable until a manager is assigned.
     */
    @Column(name = "manager_id")
    private Long managerId;

    /**
     * Current status of the branch.
     * Determines whether the branch is operational.
     */
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
     * Changes the manager of the branch.
     *
     * <p>Business Rules:
     * <ul>
     *   <li>New manager must be a valid user ID</li>
     *   <li>Manager can be set to null (unassigned)</li>
     * </ul>
     *
     * @param newManagerId the user ID of the new manager, or null to unassign
     * @throws IllegalArgumentException if newManagerId is negative
     */
    public void changeManager(Long newManagerId) {
        if (newManagerId != null && newManagerId <= 0) {
            throw new IllegalArgumentException("Manager ID must be a positive number");
        }
        this.managerId = newManagerId;
    }

    /**
     * Updates the address information for the branch.
     *
     * <p>All address fields are optional and can be updated independently.
     *
     * @param address the street address
     * @param city the city
     * @param state the state or province
     * @param country the country
     * @param postalCode the postal or ZIP code
     */
    public void updateAddress(String address, String city, String state, String country, String postalCode) {
        this.address = address;
        this.city = city;
        this.state = state;
        this.country = country;
        this.postalCode = postalCode;
    }

    // ==================== Factory Methods ====================

    /**
     * Creates a new Branch instance.
     *
     * <p>Business Rules:
     * <ul>
     *   <li>tenantId must not be null</li>
     *   <li>branchCode must not be blank</li>
     *   <li>branchName must not be blank</li>
     *   <li>branchId is auto-generated</li>
     *   <li>status defaults to ACTIVE</li>
     * </ul>
     *
     * @param tenantId the tenant this branch belongs to
     * @param branchCode the unique branch code within the tenant
     * @param branchName the unique branch name within the tenant
     * @return a new Branch instance
     * @throws IllegalArgumentException if any required field is invalid
     */
    public static Branch create(Long tenantId, String branchCode, String branchName) {
        validateTenantId(tenantId);
        validateBranchCode(branchCode);
        validateBranchName(branchName);

        return Branch.builder()
                .branchId(UUID.randomUUID())
                .tenantId(tenantId)
                .branchCode(branchCode.trim().toUpperCase())
                .branchName(branchName.trim())
                .status(BranchStatus.ACTIVE)
                .build();
    }

    // ==================== Validation Methods ====================

    private static void validateTenantId(Long tenantId) {
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalArgumentException("Tenant ID must be a positive number");
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
