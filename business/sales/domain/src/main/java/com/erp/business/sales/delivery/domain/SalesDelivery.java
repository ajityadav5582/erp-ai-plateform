package com.erp.business.sales.delivery.domain;

import com.erp.platform.data.entity.TenantScopedEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Sales delivery aggregate root: the goods issue that fulfils a sales order.
 *
 * <p><strong>Aggregate boundary.</strong> A delivery owns its lines and nothing
 * else. The sales order, customer and warehouse are referenced by id with a frozen
 * snapshot of their display fields, so a later rename never rewrites an issued
 * delivery.
 *
 * <p><strong>Posting is the state change that matters.</strong> {@link #post}
 * moves the delivery to {@link DeliveryStatus#POSTED}; the application service is
 * what then issues the outbound stock and advances the order, inside the same
 * transaction. {@link #cancel} reverses a posted delivery. The aggregate owns the
 * status machine and the line set; it does not reach across to Inventory or the
 * order, which keeps the boundary clean and the aggregate independently testable.
 */
@Entity
@Table(
        name = "sales_deliveries",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_sales_deliveries_tenant_company_number",
                        columnNames = {"tenant_id", "company_id", "delivery_number"}
                )
        },
        indexes = {
                @Index(name = "idx_sales_deliveries_tenant_company",
                        columnList = "tenant_id, company_id"),
                @Index(name = "idx_sales_deliveries_order",
                        columnList = "tenant_id, company_id, sales_order_id, delivery_date"),
                @Index(name = "idx_sales_deliveries_status",
                        columnList = "tenant_id, company_id, status, delivery_date")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class SalesDelivery extends TenantScopedEntity<Long> {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Delivery number is required")
    @Column(name = "delivery_number", nullable = false, length = 40)
    private String deliveryNumber;

    @Column(name = "fiscal_year_id")
    private Long fiscalYearId;

    @Column(name = "branch_id")
    private Long branchId;

    @NotNull(message = "Sales order is required")
    @Column(name = "sales_order_id", nullable = false)
    private Long salesOrderId;

    @NotBlank(message = "Sales order number is required")
    @Column(name = "so_number", nullable = false, length = 40)
    private String soNumber;

    @NotNull(message = "Customer is required")
    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "customer_code", nullable = false, length = 64)
    private String customerCode;

    @Column(name = "customer_name", nullable = false, length = 255)
    private String customerName;

    @NotNull(message = "Warehouse is required")
    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "warehouse_code", length = 64)
    private String warehouseCode;

    @Column(name = "warehouse_name", length = 255)
    private String warehouseName;

    @NotNull(message = "Delivery date is required")
    @Column(name = "delivery_date", nullable = false)
    private LocalDate deliveryDate;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private DeliveryStatus status = DeliveryStatus.DRAFT;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // ---- Workflow stamps: NULL until the transition happens. ----
    @Column(name = "posted_at")
    private Instant postedAt;

    @Column(name = "posted_by", length = 100)
    private String postedBy;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancelled_by", length = 100)
    private String cancelledBy;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @OneToMany(mappedBy = "salesDelivery", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    @OrderBy("lineNumber ASC")
    @ToString.Exclude
    @Builder.Default
    private List<SalesDeliveryLine> lines = new ArrayList<>();

    /**
     * Creates a draft delivery against a sales order. The customer and warehouse
     * snapshots are supplied by the caller, which has already resolved them.
     */
    public static SalesDelivery draft(String deliveryNumber, Long salesOrderId, String soNumber,
                                      Long customerId, String customerCode, String customerName,
                                      Long warehouseId, String warehouseCode, String warehouseName,
                                      LocalDate deliveryDate, Long fiscalYearId, Long branchId,
                                      String notes) {
        Objects.requireNonNull(salesOrderId, "Sales order is required");
        Objects.requireNonNull(warehouseId, "Warehouse is required");
        Objects.requireNonNull(deliveryDate, "Delivery date is required");

        return SalesDelivery.builder()
                .deliveryNumber(deliveryNumber)
                .salesOrderId(salesOrderId)
                .soNumber(soNumber)
                .customerId(customerId)
                .customerCode(customerCode)
                .customerName(customerName)
                .warehouseId(warehouseId)
                .warehouseCode(warehouseCode)
                .warehouseName(warehouseName)
                .deliveryDate(deliveryDate)
                .fiscalYearId(fiscalYearId)
                .branchId(branchId)
                .notes(notes)
                .status(DeliveryStatus.DRAFT)
                .lines(new ArrayList<>())
                .build();
    }

    /** Adds a line to a draft delivery. */
    public void addLine(SalesDeliveryLine line) {
        requireDraft("add a line to");
        if (lines.stream().anyMatch(existing -> existing.getLineNumber().equals(line.getLineNumber()))) {
            throw new IllegalStateException(
                    "Line number " + line.getLineNumber() + " is already used on this delivery");
        }
        line.assignScope(getTenantId(), getCompanyId());
        line.attachTo(this);
        lines.add(line);
    }

    /** Removes a line from a draft delivery. */
    public void removeLine(int lineNumber) {
        requireDraft("remove a line from");
        boolean removed = lines.removeIf(existing -> existing.getLineNumber() == lineNumber);
        if (!removed) {
            throw new IllegalStateException("No line " + lineNumber + " on this delivery");
        }
    }

    /** Posts the delivery: goods are issued. Requires at least one line. */
    public void post(String actor) {
        requireDraft("post");
        if (lines.isEmpty()) {
            throw new IllegalStateException("Cannot post a delivery with no lines");
        }
        this.status = DeliveryStatus.POSTED;
        this.postedAt = Instant.now();
        this.postedBy = actor;
    }

    /** Cancels a draft or posted delivery. A reason is required. */
    public void cancel(String actor, String reason) {
        if (status != DeliveryStatus.POSTED && status != DeliveryStatus.DRAFT) {
            throw new IllegalStateException("Cannot cancel a delivery in status " + status);
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A reason is required to cancel a delivery");
        }
        this.status = DeliveryStatus.CANCELLED;
        this.cancelledAt = Instant.now();
        this.cancelledBy = actor;
        this.cancellationReason = reason;
    }

    private void requireDraft(String action) {
        if (status != DeliveryStatus.DRAFT) {
            throw new IllegalStateException("Cannot " + action + " a delivery in status " + status);
        }
    }
}
