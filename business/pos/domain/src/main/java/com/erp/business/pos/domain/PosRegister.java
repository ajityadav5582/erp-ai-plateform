package com.erp.business.pos.domain;

import com.erp.platform.data.entity.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Objects;

/**
 * A POS cash register terminal assigned to a specific warehouse and branch.
 *
 * <p><strong>Aggregate boundary.</strong> A register owns its identity and
 * location snapshot. It does not own sessions — sessions reference back to
 * the register by id. This keeps the register lightweight and independently
 * testable.
 *
 * <p><strong>Why warehouse is mandatory.</strong> Every completed POS transaction
 * deducts stock from the register's assigned warehouse via the inventory
 * {@code StockMovementService}. A register without a warehouse would produce
 * stock movements with no source location, corrupting the inventory ledger.
 *
 * <p><strong>Deactivation, not deletion.</strong> Registers are never deleted.
 * Setting {@code status = INACTIVE} prevents new sessions while preserving the
 * full historical record of every session and transaction ever run on it.
 *
 * @since 1.0.0
 */
@Entity
@Table(
        name = "pos_registers",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_pos_registers_tenant_company_code",
                        columnNames = {"tenant_id", "company_id", "register_code"}
                )
        },
        indexes = {
                @Index(name = "idx_pos_registers_tenant_company",
                        columnList = "tenant_id, company_id"),
                @Index(name = "idx_pos_registers_branch",
                        columnList = "tenant_id, company_id, branch_id"),
                @Index(name = "idx_pos_registers_warehouse",
                        columnList = "tenant_id, company_id, warehouse_id"),
                @Index(name = "idx_pos_registers_status",
                        columnList = "tenant_id, company_id, status")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class PosRegister extends TenantScopedEntity<Long> {

    private static final long serialVersionUID = 1L;

    // -----------------------------------------------------------------------
    // Identity
    // -----------------------------------------------------------------------

    /**
     * Human-readable register code, unique per tenant and company.
     * Allocated by the caller (e.g. "REG-KTM-01"). Never changes after creation.
     */
    @NotBlank(message = "Register code is required")
    @Size(max = 40, message = "Register code must not exceed 40 characters")
    @Column(name = "register_code", nullable = false, length = 40, updatable = false)
    private String registerCode;

    /**
     * Descriptive name shown in the UI and printed on receipts.
     * Can be updated (e.g. "Counter 1 – Ground Floor").
     */
    @NotBlank(message = "Register name is required")
    @Size(max = 255, message = "Register name must not exceed 255 characters")
    @Column(name = "register_name", nullable = false, length = 255)
    private String registerName;

    // -----------------------------------------------------------------------
    // Location snapshot (frozen on creation; denormalized for receipt printing)
    // -----------------------------------------------------------------------

    /** Branch this register belongs to. Nullable if no branch structure. */
    @Column(name = "branch_id")
    private Long branchId;

    /** Branch name snapshot. */
    @Size(max = 255)
    @Column(name = "branch_name", length = 255)
    private String branchName;

    /**
     * Warehouse from which stock is deducted on every completed transaction.
     * Mandatory — a register without a warehouse cannot deduct stock.
     */
    @NotNull(message = "Warehouse is required for a POS register")
    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @NotBlank(message = "Warehouse code snapshot is required")
    @Column(name = "warehouse_code", nullable = false, length = 64)
    private String warehouseCode;

    @NotBlank(message = "Warehouse name snapshot is required")
    @Column(name = "warehouse_name", nullable = false, length = 255)
    private String warehouseName;

    // -----------------------------------------------------------------------
    // Status
    // -----------------------------------------------------------------------

    @NotNull(message = "Register status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PosRegisterStatus status;

    /**
     * Id of the currently OPEN session on this register, or {@code null} when
     * the register has no open session. Updated by the session service when a
     * session is opened or closed. Denormalized for fast "is open?" checks
     * without a join.
     */
    @Column(name = "current_session_id")
    private Long currentSessionId;

    /** Free-text notes for the operations team. */
    @Size(max = 2000, message = "Notes must not exceed 2000 characters")
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // -----------------------------------------------------------------------
    // Factory method
    // -----------------------------------------------------------------------

    /**
     * Creates a new active register. The tenant/company scope must be assigned
     * separately via {@link #assignScope(Long, Long)} before persisting.
     *
     * @param registerCode  unique code for this register within the company
     * @param registerName  human-readable display name
     * @param warehouseId   warehouse from which stock will be deducted
     * @param warehouseCode warehouse code snapshot (frozen for receipt printing)
     * @param warehouseName warehouse name snapshot
     * @param branchId      optional branch; {@code null} if no branch structure
     * @param branchName    optional branch name snapshot
     * @param notes         optional free-text notes
     * @return a new {@link PosRegister} in {@link PosRegisterStatus#ACTIVE} state
     */
    public static PosRegister create(
            String registerCode, String registerName,
            Long warehouseId, String warehouseCode, String warehouseName,
            Long branchId, String branchName, String notes) {

        Objects.requireNonNull(warehouseId, "Warehouse id is required");
        Objects.requireNonNull(registerCode, "Register code is required");

        return PosRegister.builder()
                .registerCode(registerCode.trim().toUpperCase())
                .registerName(registerName)
                .warehouseId(warehouseId)
                .warehouseCode(warehouseCode)
                .warehouseName(warehouseName)
                .branchId(branchId)
                .branchName(branchName)
                .status(PosRegisterStatus.ACTIVE)
                .notes(notes)
                .build();
    }

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    /**
     * Marks this register as inactive. Once inactive, no new sessions can
     * be opened on it. Throws if the register has an open session — the
     * session must be closed first.
     *
     * @throws IllegalStateException if the register currently has an open session
     */
    public void deactivate() {
        if (currentSessionId != null) {
            throw new IllegalStateException(
                    "Register '" + registerCode + "' has an open session (id=" + currentSessionId
                            + "). Close the session before deactivating the register.");
        }
        this.status = PosRegisterStatus.INACTIVE;
    }

    /** Re-activates a previously deactivated register. */
    public void activate() {
        this.status = PosRegisterStatus.ACTIVE;
    }

    /**
     * Updates the register's editable details. The warehouse assignment is fixed
     * after creation, so historical sessions keep pointing at the warehouse they
     * actually drew stock from.
     *
     * @param registerName new display name (required)
     * @param branchId     new branch id, or {@code null} for none
     * @param branchName   new branch name snapshot, or {@code null}
     * @param notes        new free-text notes, or {@code null}
     */
    public void updateDetails(String registerName, Long branchId, String branchName, String notes) {
        if (registerName == null || registerName.isBlank()) {
            throw new IllegalArgumentException("Register name is required");
        }
        this.registerName = registerName.trim();
        this.branchId = branchId;
        this.branchName = branchName;
        this.notes = notes;
    }

    /**
     * Records that a session was opened on this register. Called by the session
     * service inside the same transaction as the session insert.
     *
     * @param sessionId the id of the newly opened session
     * @throws IllegalStateException if the register is not ACTIVE or already has
     *                               an open session
     */
    public void recordSessionOpened(Long sessionId) {
        if (status != PosRegisterStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Register '" + registerCode + "' is " + status
                            + ". Only ACTIVE registers can have sessions opened.");
        }
        if (currentSessionId != null) {
            throw new IllegalStateException(
                    "Register '" + registerCode + "' already has session " + currentSessionId
                            + " open. Close it before opening a new one.");
        }
        this.currentSessionId = Objects.requireNonNull(sessionId, "Session id must not be null");
    }

    /**
     * Records that the current session was closed.
     *
     * @param sessionId the id of the session being closed
     * @throws IllegalStateException if the given session is not the current open session
     */
    public void recordSessionClosed(Long sessionId) {
        if (!Objects.equals(currentSessionId, sessionId)) {
            throw new IllegalStateException(
                    "Session " + sessionId + " is not the current open session on register '"
                            + registerCode + "'. Current session: " + currentSessionId);
        }
        this.currentSessionId = null;
    }

    /**
     * Returns whether this register currently has an open session.
     */
    public boolean hasOpenSession() {
        return currentSessionId != null;
    }
}
