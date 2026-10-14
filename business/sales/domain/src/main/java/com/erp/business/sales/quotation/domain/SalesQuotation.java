package com.erp.business.sales.quotation.domain;

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

@Entity
@Table(
        name = "sales_quotations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_sales_quotations_tenant_company_number",
                        columnNames = {"tenant_id", "company_id", "quotation_number"}
                )
        },
        indexes = {
                @Index(name = "idx_sales_quotations_tenant_company",
                        columnList = "tenant_id, company_id"),
                @Index(name = "idx_sales_quotations_customer",
                        columnList = "tenant_id, company_id, customer_id, quotation_date"),
                @Index(name = "idx_sales_quotations_status",
                        columnList = "tenant_id, company_id, status, quotation_date")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class SalesQuotation extends TenantScopedEntity<Long> {

    private static final long serialVersionUID = 1L;
    private static final int MONEY_SCALE = 4;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    @NotBlank(message = "Quotation number is required")
    @Column(name = "quotation_number", nullable = false, length = 40)
    private String quotationNumber;

    @NotNull(message = "Quotation date is required")
    @Column(name = "quotation_date", nullable = false)
    private LocalDate quotationDate;

    @Column(name = "valid_until_date")
    private LocalDate validUntilDate;

    @NotNull(message = "Customer is required")
    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @NotBlank(message = "Customer code is required")
    @Column(name = "customer_code", nullable = false, length = 64)
    private String customerCode;

    @NotBlank(message = "Customer name is required")
    @Column(name = "customer_name", nullable = false, length = 255)
    private String customerName;

    @Column(name = "customer_pan_number", length = 32)
    private String customerPanNumber;

    @NotBlank(message = "Currency is required")
    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "NPR";

    @Column(name = "fiscal_year_id")
    private Long fiscalYearId;

    @Column(name = "branch_id")
    private Long branchId;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    @Builder.Default
    private SalesQuotationStatus status = SalesQuotationStatus.DRAFT;

    @Column(name = "subtotal_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal subtotalAmount = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "sales_order_id")
    private Long salesOrderId;

    @Column(name = "converted_at")
    private Instant convertedAt;

    @Column(name = "reference", length = 100)
    private String reference;

    @Column(name = "terms_and_conditions", length = 2000)
    private String termsAndConditions;

    @Column(name = "notes", length = 1000)
    private String notes;

    @OneToMany(
            mappedBy = "quotation",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @OrderBy("lineNumber ASC")
    @Builder.Default
    private List<SalesQuotationLine> lines = new ArrayList<>();

    public static SalesQuotation draft(String quotationNumber,
                                       LocalDate quotationDate,
                                       LocalDate validUntilDate,
                                       Long customerId,
                                       String customerCode,
                                       String customerName,
                                       String customerPanNumber,
                                       String currency,
                                       Long fiscalYearId,
                                       Long branchId,
                                       Long warehouseId,
                                       String reference,
                                       String termsAndConditions,
                                       String notes) {
        return SalesQuotation.builder()
                .quotationNumber(Objects.requireNonNull(quotationNumber, "Quotation number is required"))
                .quotationDate(Objects.requireNonNull(quotationDate, "Quotation date is required"))
                .validUntilDate(validUntilDate)
                .customerId(Objects.requireNonNull(customerId, "Customer ID is required"))
                .customerCode(Objects.requireNonNull(customerCode, "Customer code is required"))
                .customerName(Objects.requireNonNull(customerName, "Customer name is required"))
                .customerPanNumber(customerPanNumber)
                .currency(currency != null ? currency : "NPR")
                .fiscalYearId(fiscalYearId)
                .branchId(branchId)
                .warehouseId(warehouseId)
                .reference(reference)
                .termsAndConditions(termsAndConditions)
                .notes(notes)
                .status(SalesQuotationStatus.DRAFT)
                .subtotalAmount(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .lines(new ArrayList<>())
                .build();
    }

    public void addLine(SalesQuotationLine line) {
        Objects.requireNonNull(line, "Line must not be null");
        line.assignQuotation(this);
        if (getTenantId() != null && getCompanyId() != null) {
            line.assignScope(getTenantId(), getCompanyId());
        }
        this.lines.add(line);
        recalculateTotals();
    }

    public void clearLines() {
        this.lines.clear();
        recalculateTotals();
    }

    public void recalculateTotals() {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;

        for (SalesQuotationLine line : this.lines) {
            BigDecimal gross = line.getQuantity().multiply(line.getUnitPrice()).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            BigDecimal lineDisc = line.getDiscountPercentage() != null && line.getDiscountPercentage().compareTo(BigDecimal.ZERO) > 0
                    ? gross.multiply(line.getDiscountPercentage()).divide(ONE_HUNDRED, MONEY_SCALE, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            BigDecimal net = gross.subtract(lineDisc);
            BigDecimal lineTax = line.getTaxRate() != null && line.getTaxRate().compareTo(BigDecimal.ZERO) > 0
                    ? net.multiply(line.getTaxRate()).divide(ONE_HUNDRED, MONEY_SCALE, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            subtotal = subtotal.add(gross);
            discount = discount.add(lineDisc);
            tax = tax.add(lineTax);
        }

        this.subtotalAmount = subtotal.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        this.discountAmount = discount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        this.taxAmount = tax.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        this.totalAmount = subtotal.subtract(discount).add(tax).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public void markSent() {
        if (this.status != SalesQuotationStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT quotations can be marked as SENT");
        }
        this.status = SalesQuotationStatus.SENT;
    }

    public void markAccepted() {
        if (this.status != SalesQuotationStatus.SENT && this.status != SalesQuotationStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT or SENT quotations can be marked as ACCEPTED");
        }
        this.status = SalesQuotationStatus.ACCEPTED;
    }

    public void markRejected(String reason) {
        if (this.status == SalesQuotationStatus.CONVERTED) {
            throw new IllegalStateException("Converted quotations cannot be rejected");
        }
        this.status = SalesQuotationStatus.REJECTED;
        if (reason != null && !reason.isBlank()) {
            this.notes = (this.notes != null ? this.notes + "\n" : "") + "Rejection Reason: " + reason.trim();
        }
    }

    public void markConverted(Long salesOrderId) {
        if (this.status == SalesQuotationStatus.CONVERTED) {
            throw new IllegalStateException("Quotation is already converted");
        }
        this.salesOrderId = Objects.requireNonNull(salesOrderId, "Sales Order ID is required");
        this.convertedAt = Instant.now();
        this.status = SalesQuotationStatus.CONVERTED;
    }

    @Override
    public void assignScope(Long tenantId, Long companyId) {
        super.assignScope(tenantId, companyId);
        for (SalesQuotationLine line : this.lines) {
            line.assignScope(tenantId, companyId);
        }
    }
}
