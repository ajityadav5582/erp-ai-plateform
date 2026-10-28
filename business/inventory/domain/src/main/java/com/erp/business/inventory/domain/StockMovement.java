package com.erp.business.inventory.domain;

import com.erp.business.inventory.shared.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * Immutable stock ledger entry. One row per physical movement.
 * Never updated or deleted - only inserted.
 */
@Entity
@Table(
        name = "stock_movements",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_stock_movements_id_scope",
                        columnNames = {"id", "tenant_id", "company_id"}
                ),
                // DB-enforced idempotency: at most one set of movements per source
                // document and direction, so a concurrent retry cannot double-post.
                // reference_type/reference_id are nullable and SQL treats NULLs as
                // distinct, so manual movements without a reference are unaffected.
                @UniqueConstraint(
                        name = "uk_stock_movements_reference_direction",
                        columnNames = {"tenant_id", "company_id", "reference_type", "reference_id", "movement_type", "item_id", "unit_id", "warehouse_id"}
                )
        },
        indexes = {
                @Index(name = "idx_stock_movements_tenant_company", columnList = "tenant_id, company_id"),
                @Index(name = "idx_stock_movements_warehouse_item_unit", columnList = "tenant_id, company_id, warehouse_id, item_id, unit_id"),
                @Index(name = "idx_stock_movements_reference", columnList = "reference_type, reference_id"),
                @Index(name = "idx_stock_movements_date", columnList = "movement_date")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(callSuper = true)
public class StockMovement extends TenantScopedEntity<Long> {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "Movement type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 20)
    private StockMovementType movementType;

    @NotNull(message = "Warehouse is required")
    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @NotNull(message = "Item is required")
    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @NotNull(message = "Unit is required")
    @Column(name = "unit_id", nullable = false)
    private Long unitId;

    @NotNull(message = "Quantity is required")
    @Column(name = "quantity", nullable = false, precision = 19, scale = 6)
    private BigDecimal quantity;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @NotNull(message = "Movement date is required")
    @Column(name = "movement_date", nullable = false)
    private Instant movementDate;

    @Column(name = "posted_by", length = 100)
    private String postedBy;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    public static StockMovement create(Long tenantId, Long companyId,
                                       StockMovementType movementType,
                                       Long warehouseId, Long itemId, Long unitId,
                                       BigDecimal quantity,
                                       String referenceType, Long referenceId, String referenceNumber,
                                       Instant movementDate, String postedBy, String notes) {
        StockMovement movement = StockMovement.builder()
                .movementType(movementType)
                .warehouseId(warehouseId)
                .itemId(itemId)
                .unitId(unitId)
                .quantity(quantity.setScale(6, RoundingMode.HALF_UP))
                .referenceType(referenceType)
                .referenceId(referenceId)
                .referenceNumber(referenceNumber)
                .movementDate(movementDate)
                .postedBy(postedBy)
                .notes(notes)
                .build();
        movement.assignScope(tenantId, companyId);
        return movement;
    }

    public BigDecimal getSignedQuantity() {
        return switch (movementType) {
            case IN, ADJUST -> quantity;
            case OUT, RESERVE -> quantity.negate();
            case RELEASE -> quantity;
        };
    }

    public boolean isInbound() {
        return movementType == StockMovementType.IN || movementType == StockMovementType.RELEASE;
    }

    public boolean isOutbound() {
        return movementType == StockMovementType.OUT || movementType == StockMovementType.RESERVE;
    }
}
