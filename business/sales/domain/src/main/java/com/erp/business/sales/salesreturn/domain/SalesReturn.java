package com.erp.business.sales.salesreturn.domain;

import com.erp.platform.common.nepali.date.NepaliDateConverter;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Sales return aggregate root: the credit note issued when goods come back.
 *
 * <p><strong>The issued invoice is never mutated.</strong> A return is a separate
 * IRD document that references the original invoice for traceability; the sales
 * register nets credit notes against sales for the VAT return. This keeps an
 * issued tax invoice immutable, which is what an audit expects.
 *
 * <p><strong>Posting restocks and records.</strong> {@link #post} moves the return
 * to {@link ReturnStatus#POSTED}; the application service then restocks the goods
 * (an inbound movement in Inventory) inside the same transaction. The aggregate
 * owns its status, lines and derived credit totals; it does not reach across to
 * Inventory.
 */
@Entity
@Table(
        name = "sales_returns",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_sales_returns_tenant_company_number",
                        columnNames = {"tenant_id", "company_id", "credit_note_number"}
                )
        },
        indexes = {
                @Index(name = "idx_sales_returns_tenant_company",
                        columnList = "tenant_id, company_id"),
                @Index(name = "idx_sales_returns_invoice",
                        columnList = "tenant_id, company_id, invoice_id, return_date"),
                @Index(name = "idx_sales_returns_status",
                        columnList = "tenant_id, company_id, status, return_date")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class SalesReturn extends TenantScopedEntity<Long> {

    private static final long serialVersionUID = 1L;

    private static final int MONEY_SCALE = 4;

    /** IRD sequential credit-note number, allocated by the document counter. */
    @NotBlank(message = "Credit note number is required")
    @Column(name = "credit_note_number", nullable = false, length = 40)
    private String creditNoteNumber;

    @Column(name = "fiscal_year_id")
    private Long fiscalYearId;

    @Column(name = "branch_id")
    private Long branchId;

    @NotNull(message = "Invoice is required")
    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;

    @NotBlank(message = "Invoice number is required")
    @Column(name = "invoice_number", nullable = false, length = 40)
    private String invoiceNumber;

    @Column(name = "sales_order_id")
    private Long salesOrderId;

    @Column(name = "so_number", length = 40)
    private String soNumber;

    @NotNull(message = "Customer is required")
    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "customer_code", nullable = false, length = 64)
    private String customerCode;

    @Column(name = "customer_name", nullable = false, length = 255)
    private String customerName;

    @Column(name = "customer_pan_number", length = 16)
    private String customerPanNumber;

    /** The warehouse the returned goods are restocked into. */
    @NotNull(message = "Warehouse is required")
    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @NotNull(message = "Return date is required")
    @Column(name = "return_date", nullable = false)
    private LocalDate returnDate;

    /** Bikram Sambat rendering of {@link #returnDate}, derived at creation. */
    @Column(name = "return_date_np", nullable = false, length = 20)
    private String returnDateNp;

    @Column(name = "reason", length = 500)
    private String reason;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ReturnStatus status = ReturnStatus.DRAFT;

    // ---- Derived credit totals. Written only by recalculateTotals(). ----
    @Column(name = "subtotal_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal subtotalAmount = BigDecimal.ZERO.setScale(MONEY_SCALE);

    @Column(name = "vat_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal vatAmount = BigDecimal.ZERO.setScale(MONEY_SCALE);

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO.setScale(MONEY_SCALE);

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

    // ---- IRD CBMS Sync Fields ----
    @Column(name = "cbms_synced", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private Boolean cbmsSynced = false;

    @Column(name = "cbms_sync_time")
    private Instant cbmsSyncTime;

    @Column(name = "cbms_response", length = 500)
    private String cbmsResponse;

    @Column(name = "cbms_retry_count", nullable = false, columnDefinition = "integer default 0")
    @Builder.Default
    private Integer cbmsRetryCount = 0;

    @OneToMany(mappedBy = "salesReturn", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    @OrderBy("lineNumber ASC")
    @ToString.Exclude
    @Builder.Default
    private List<SalesReturnLine> lines = new ArrayList<>();

    /**
     * Creates a draft credit note against an invoice. The Bikram Sambat date is
     * derived from the AD date so the two cannot drift apart.
     */
    public static SalesReturn draft(String creditNoteNumber, Long invoiceId, String invoiceNumber,
                                    Long salesOrderId, String soNumber,
                                    Long customerId, String customerCode, String customerName,
                                    String customerPanNumber, Long warehouseId, LocalDate returnDate,
                                    Long fiscalYearId, Long branchId, String reason) {
        Objects.requireNonNull(invoiceId, "Invoice is required");
        Objects.requireNonNull(warehouseId, "Warehouse is required");
        Objects.requireNonNull(returnDate, "Return date is required");

        SalesReturn salesReturn = SalesReturn.builder()
                .creditNoteNumber(creditNoteNumber)
                .invoiceId(invoiceId)
                .invoiceNumber(invoiceNumber)
                .salesOrderId(salesOrderId)
                .soNumber(soNumber)
                .customerId(customerId)
                .customerCode(customerCode)
                .customerName(customerName)
                .customerPanNumber(customerPanNumber)
                .warehouseId(warehouseId)
                .returnDate(returnDate)
                .returnDateNp(NepaliDateConverter.format(returnDate))
                .fiscalYearId(fiscalYearId)
                .branchId(branchId)
                .reason(reason)
                .status(ReturnStatus.DRAFT)
                .lines(new ArrayList<>())
                .build();
        salesReturn.recalculateTotals();
        return salesReturn;
    }

    public void addLine(SalesReturnLine line) {
        requireDraft("add a line to");
        if (lines.stream().anyMatch(existing -> existing.getLineNumber().equals(line.getLineNumber()))) {
            throw new IllegalStateException(
                    "Line number " + line.getLineNumber() + " is already used on this credit note");
        }
        line.assignScope(getTenantId(), getCompanyId());
        line.attachTo(this);
        lines.add(line);
        recalculateTotals();
    }

    public void removeLine(int lineNumber) {
        requireDraft("remove a line from");
        boolean removed = lines.removeIf(existing -> existing.getLineNumber() == lineNumber);
        if (!removed) {
            throw new IllegalStateException("No line " + lineNumber + " on this credit note");
        }
        recalculateTotals();
    }

    /** Recomputes the credit totals from the lines. The single writer. */
    public void recalculateTotals() {
        BigDecimal net = BigDecimal.ZERO;
        BigDecimal vat = BigDecimal.ZERO;
        for (SalesReturnLine line : lines) {
            net = net.add(line.getLineAmount());
            vat = vat.add(line.vatAmount());
        }
        this.subtotalAmount = net.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        this.vatAmount = vat.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        this.totalAmount = net.add(vat).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /** Posts the credit note: goods are restocked by the service. Requires lines. */
    public void post(String actor) {
        requireDraft("post");
        if (lines.isEmpty()) {
            throw new IllegalStateException("Cannot post a credit note with no lines");
        }
        this.status = ReturnStatus.POSTED;
        this.postedAt = Instant.now();
        this.postedBy = actor;
    }

    /** Cancels a draft or posted credit note. A reason is required. */
    public void cancel(String actor, String reason) {
        if (status != ReturnStatus.POSTED && status != ReturnStatus.DRAFT) {
            throw new IllegalStateException("Cannot cancel a credit note in status " + status);
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A reason is required to cancel a credit note");
        }
        this.status = ReturnStatus.CANCELLED;
        this.cancelledAt = Instant.now();
        this.cancelledBy = actor;
        this.cancellationReason = reason;
    }

    public void markCbmsSynced(String response) {
        this.cbmsSynced = true;
        this.cbmsSyncTime = Instant.now();
        this.cbmsResponse = response != null && response.length() > 500 ? response.substring(0, 500) : response;
    }

    public void recordCbmsSyncFailure(String error) {
        this.cbmsSynced = false;
        this.cbmsRetryCount = (this.cbmsRetryCount != null ? this.cbmsRetryCount : 0) + 1;
        this.cbmsResponse = error != null && error.length() > 500 ? error.substring(0, 500) : error;
    }

    private void requireDraft(String action) {
        if (status != ReturnStatus.DRAFT) {
            throw new IllegalStateException("Cannot " + action + " a credit note in status " + status);
        }
    }
}
