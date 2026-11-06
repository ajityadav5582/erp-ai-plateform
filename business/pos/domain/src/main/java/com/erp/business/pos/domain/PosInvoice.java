package com.erp.business.pos.domain;

import com.erp.platform.data.entity.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pos_invoices")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(callSuper = true)
public class PosInvoice extends TenantScopedEntity<Long> {

    private static final long serialVersionUID = 1L;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private UUID idempotencyKey;

    @Column(name = "invoice_number", nullable = false, unique = true, length = 64)
    private String invoiceNumber;

    @Column(name = "register_id", nullable = false)
    private Long registerId;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "customer_pan", length = 16)
    private String customerPan;

    @Column(name = "payment_method", nullable = false, length = 32)
    private String paymentMethod;

    @Column(name = "tendered_amount", precision = 19, scale = 4)
    private BigDecimal tenderedAmount;

    @Column(name = "change_amount", precision = 19, scale = 4)
    private BigDecimal changeAmount;

    @Column(name = "gross_total", precision = 19, scale = 4, nullable = false)
    private BigDecimal grossTotal;

    @Column(name = "discount_amount", precision = 19, scale = 4)
    private BigDecimal discountAmount;

    @Column(name = "taxable_amount", precision = 19, scale = 4)
    private BigDecimal taxableAmount;

    @Column(name = "exempt_amount", precision = 19, scale = 4)
    private BigDecimal exemptAmount;

    @Column(name = "zero_rated_amount", precision = 19, scale = 4)
    private BigDecimal zeroRatedAmount;

    @Column(name = "vat_amount", precision = 19, scale = 4)
    private BigDecimal vatAmount;

    @Column(name = "grand_total", precision = 19, scale = 4, nullable = false)
    private BigDecimal grandTotal;

    @Column(name = "bs_date", length = 32)
    private String bsDate;

    @Column(name = "lines_json", columnDefinition = "TEXT", nullable = false)
    private String linesJson;

    @Column(name = "cashier_name", length = 128)
    private String cashierName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
