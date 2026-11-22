package com.erp.business.procurement.purchaseinvoice.dto;

import com.erp.business.procurement.purchaseinvoice.domain.PurchaseInvoiceType;
import com.erp.business.procurement.purchaseinvoice.domain.PurchaseTaxCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Records a supplier's invoice against a delivery that has been received or directly
 * for services / non-physical expenses.
 */
public record CreateInvoiceRequest(

        /** The delivery being billed, or null for direct service invoices. */
        @Positive(message = "Goods receipt id must be positive")
        Long goodsReceiptId,

        /** Classification of the invoice. Optional; defaults to GOODS_RECEIPT when goodsReceiptId is present, else DIRECT. */
        PurchaseInvoiceType invoiceType,

        /** Required for direct invoices; ignored when goodsReceiptId is present. */
        Long supplierId,

        /** Optional branch for direct invoices. */
        Long branchId,

        /** Optional fiscal year for direct invoices. */
        Long fiscalYearId,

        /** Currency code, e.g. NPR. Defaults to NPR. */
        @Size(max = 3)
        String currency,

        /** The supplier's own invoice number. */
        @NotBlank(message = "Supplier invoice number is required")
        @Size(max = 60)
        String invoiceNumber,

        /** When the supplier issued it. Defaults to today. */
        LocalDate invoiceDate,

        /** When payment falls due. */
        LocalDate dueDate,

        /** Whether Tax Deducted at Source (TDS) under Nepal Income Tax Act 2058 applies. */
        Boolean isTdsApplicable,

        /** TDS deduction rate (e.g. 1.50 for 1.5% goods supply, 10.00 or 15.00 for services). */
        @DecimalMin(value = "0", message = "TDS rate cannot be negative")
        @DecimalMax(value = "100", message = "TDS rate cannot exceed 100")
        BigDecimal tdsRate,

        @NotEmpty(message = "At least one invoice line is required")
        @Valid
        List<Line> lines,

        @Size(max = 2000)
        String notes
) {

    /**
     * Overload for direct or receipt billing without explicit TDS settings.
     */
    public CreateInvoiceRequest(Long goodsReceiptId, PurchaseInvoiceType invoiceType,
                                Long supplierId, Long branchId, Long fiscalYearId,
                                String currency, String invoiceNumber, LocalDate invoiceDate,
                                LocalDate dueDate, List<Line> lines, String notes) {
        this(goodsReceiptId, invoiceType, supplierId, branchId, fiscalYearId,
             currency, invoiceNumber, invoiceDate, dueDate, false, BigDecimal.ZERO, lines, notes);
    }

    /**
     * Backward-compatible constructor for 3-way match receipt billing.
     */
    public CreateInvoiceRequest(Long goodsReceiptId, String invoiceNumber, LocalDate invoiceDate,
                                LocalDate dueDate, List<Line> lines, String notes) {
        this(goodsReceiptId,
             goodsReceiptId != null ? PurchaseInvoiceType.GOODS_RECEIPT : PurchaseInvoiceType.DIRECT,
             null, null, null, "NPR", invoiceNumber, invoiceDate, dueDate, false, BigDecimal.ZERO, lines, notes);
    }

    /**
     * One billed line.
     */
    public record Line(

            /** Required when billing from a receipt; null for direct service lines. */
            @Positive(message = "Receipt line id must be positive")
            Long goodsReceiptLineId,

            /** Required for direct service lines; optional when goodsReceiptLineId is present. */
            Long itemId,

            String itemCode,
            String itemName,
            String uomCode,

            @DecimalMin(value = "0.000001", message = "Quantity must be greater than zero")
            BigDecimal quantity,

            @DecimalMin(value = "0", message = "Unit price cannot be negative")
            BigDecimal unitPrice,

            @DecimalMin(value = "0", message = "Discount cannot be below zero")
            @DecimalMax(value = "100", message = "Discount cannot exceed 100")
            BigDecimal discountPercentage,

            @DecimalMin(value = "0", message = "Tax rate cannot be below zero")
            @DecimalMax(value = "100", message = "Tax rate cannot exceed 100")
            BigDecimal taxRate,

            @Size(max = 500)
            String description,

            PurchaseTaxCategory taxCategory
     ) {

        /**
         * Backward-compatible constructor for 3-way match receipt line billing.
         */
        public Line(Long goodsReceiptLineId, BigDecimal quantity, BigDecimal unitPrice,
                    BigDecimal discountPercentage, BigDecimal taxRate, String description,
                    PurchaseTaxCategory taxCategory) {
            this(goodsReceiptLineId, null, null, null, null, quantity, unitPrice,
                 discountPercentage, taxRate, description, taxCategory);
        }
    }
}
