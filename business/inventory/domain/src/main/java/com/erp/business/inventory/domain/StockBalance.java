package com.erp.business.inventory.domain;

import com.erp.business.inventory.shared.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Current stock balance per warehouse, item, and unit.
 * Denormalized for query performance - updated by StockMovementService.
 */
@Entity
@Table(
        name = "stock_balances",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_stock_balances_warehouse_item_unit",
                columnNames = {"tenant_id", "company_id", "warehouse_id", "item_id", "unit_id"}
        ),
        indexes = {
                @Index(name = "idx_stock_balances_tenant_company", columnList = "tenant_id, company_id"),
                @Index(name = "idx_stock_balances_warehouse", columnList = "tenant_id, company_id, warehouse_id"),
                @Index(name = "idx_stock_balances_item", columnList = "tenant_id, company_id, item_id"),
                @Index(name = "idx_stock_balances_low_stock", columnList = "tenant_id, company_id, on_hand_qty")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(callSuper = true)
public class StockBalance extends TenantScopedEntity<Long> {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "Warehouse is required")
    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @NotNull(message = "Item is required")
    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @NotNull(message = "Unit is required")
    @Column(name = "unit_id", nullable = false)
    private Long unitId;

    @NotNull(message = "On-hand quantity is required")
    @Column(name = "on_hand_qty", nullable = false, precision = 19, scale = 6)
    @Builder.Default
    private BigDecimal onHandQty = BigDecimal.ZERO.setScale(6);

    @NotNull(message = "Reserved quantity is required")
    @Column(name = "reserved_qty", nullable = false, precision = 19, scale = 6)
    @Builder.Default
    private BigDecimal reservedQty = BigDecimal.ZERO.setScale(6);

    @Column(name = "last_movement_at")
    private Instant lastMovementAt;

    public BigDecimal getAvailableQty() {
        return onHandQty.subtract(reservedQty);
    }

    public void applyMovement(StockMovement movement) {
        BigDecimal signedQty = movement.getSignedQuantity();
        switch (movement.getMovementType()) {
            case IN, OUT, ADJUST -> this.onHandQty = this.onHandQty.add(signedQty).setScale(6, RoundingMode.HALF_UP);
            case RESERVE -> this.reservedQty = this.reservedQty.add(signedQty.abs()).setScale(6, RoundingMode.HALF_UP);
            case RELEASE -> this.reservedQty = this.reservedQty.subtract(signedQty.abs()).setScale(6, RoundingMode.HALF_UP);
        }
        if (this.reservedQty.signum() < 0) {
            this.reservedQty = BigDecimal.ZERO.setScale(6);
        }
        this.lastMovementAt = movement.getMovementDate();
    }

    public static StockBalance create(Long tenantId, Long companyId,
                                      Long warehouseId, Long itemId, Long unitId) {
        StockBalance balance = StockBalance.builder()
                .warehouseId(warehouseId)
                .itemId(itemId)
                .unitId(unitId)
                .build();
        balance.assignScope(tenantId, companyId);
        return balance;
    }
}
