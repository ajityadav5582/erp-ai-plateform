package com.erp.business.sales.delivery.domain;

import com.erp.platform.data.entity.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * One line of a sales delivery: what was shipped against a sales-order line.
 *
 * <p>The item is a frozen snapshot referenced by id, and {@code salesOrderLineNumber}
 * ties the delivery back to the order line it fulfils, so the order's delivery
 * progress can be advanced precisely rather than guessed.
 */
@Entity
@Table(
        name = "sales_delivery_lines",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_sales_delivery_lines_delivery_line_number",
                columnNames = {"sales_delivery_id", "line_number"}
        ),
        indexes = {
                @Index(name = "idx_sales_delivery_lines_delivery",
                        columnList = "sales_delivery_id, line_number"),
                @Index(name = "idx_sales_delivery_lines_order_line",
                        columnList = "tenant_id, company_id, sales_order_line_number")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class SalesDeliveryLine extends TenantScopedEntity<Long> {

    private static final long serialVersionUID = 1L;

    private static final int QUANTITY_SCALE = 6;

    @NotNull(message = "Line number is required")
    @Positive(message = "Line number must be positive")
    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    /** The sales-order line this delivery fulfils. */
    @NotNull(message = "Sales order line number is required")
    @Column(name = "sales_order_line_number", nullable = false)
    private Integer salesOrderLineNumber;

    @NotNull(message = "Item is required")
    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @NotBlank(message = "Item code is required")
    @Column(name = "item_code", nullable = false, length = 100)
    private String itemCode;

    @NotBlank(message = "Item name is required")
    @Column(name = "item_name", nullable = false, length = 255)
    private String itemName;

    @NotBlank(message = "Unit of measure is required")
    @Column(name = "uom_code", nullable = false, length = 32)
    private String uomCode;

    @NotNull(message = "Quantity is required")
    @Column(name = "quantity", nullable = false, precision = 19, scale = 6)
    private BigDecimal quantity;

    @Column(name = "description", length = 500)
    private String description;

    /** Owning delivery. Written by the aggregate root only. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sales_delivery_id", nullable = false)
    @ToString.Exclude
    private SalesDelivery salesDelivery;

    public static SalesDeliveryLine create(int lineNumber, int salesOrderLineNumber, Long itemId,
                                           String itemCode, String itemName, String uomCode,
                                           BigDecimal quantity, String description) {
        return SalesDeliveryLine.builder()
                .lineNumber(lineNumber)
                .salesOrderLineNumber(salesOrderLineNumber)
                .itemId(Objects.requireNonNull(itemId, "Item is required"))
                .itemCode(itemCode)
                .itemName(itemName)
                .uomCode(uomCode)
                .quantity(quantity.setScale(QUANTITY_SCALE, RoundingMode.HALF_UP))
                .description(description)
                .build();
    }

    void attachTo(SalesDelivery delivery) {
        this.salesDelivery = delivery;
    }
}
