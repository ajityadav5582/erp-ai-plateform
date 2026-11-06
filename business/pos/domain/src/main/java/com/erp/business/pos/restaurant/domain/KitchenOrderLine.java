package com.erp.business.pos.restaurant.domain;

import com.erp.platform.data.entity.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;

/**
 * A single line item on a kitchen order ticket. Restaurant-only.
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "pos_kitchen_order_lines",
        indexes = {
                @Index(name = "idx_pos_kol_tenant_company", columnList = "tenant_id, company_id"),
                @Index(name = "idx_pos_kol_order", columnList = "tenant_id, company_id, kitchen_order_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class KitchenOrderLine extends TenantScopedEntity<Long> {

    private static final long serialVersionUID = 1L;

    @Column(name = "kitchen_order_id", nullable = false)
    private Long kitchenOrderId;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "item_code", length = 100)
    private String itemCode;

    @Column(name = "item_name", length = 255)
    private String itemName;

    @Column(name = "quantity", precision = 19, scale = 4, nullable = false)
    private BigDecimal quantity;

    @Column(name = "notes", length = 255)
    private String notes;

    @Column(name = "course")
    @Builder.Default
    private int course = 1;

    public void assignKitchenOrder(Long kitchenOrderId) {
        this.kitchenOrderId = kitchenOrderId;
    }
}
