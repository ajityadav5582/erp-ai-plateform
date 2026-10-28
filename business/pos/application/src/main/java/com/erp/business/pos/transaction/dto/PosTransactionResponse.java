package com.erp.business.pos.transaction.dto;

import com.erp.business.pos.domain.PosPaymentMethod;
import com.erp.business.pos.domain.PosTransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Read model for a POS transaction: the frozen header, the derived totals and
 * the line snapshots. Totals are never accepted from a client — they are the
 * values the domain computed when the lines were priced.
 */
public record PosTransactionResponse(

        Long id,

        Long tenantId,

        Long companyId,

        /** Allocated on completion; {@code null} while the cart is OPEN. */
        String transactionNumber,

        Long sessionId,

        Long registerId,

        Long fiscalYearId,

        Long branchId,

        LocalDate transactionDate,

        /** Bikram Sambat equivalent of {@link #transactionDate}. */
        String transactionDateNp,

        Long customerId,

        String customerCode,

        String customerName,

        String customerPanNumber,

        String cashierId,

        String cashierName,

        Long warehouseId,

        String warehouseCode,

        String warehouseName,

        String currency,

        BigDecimal subtotalAmount,

        BigDecimal taxableAmount,

        BigDecimal exemptAmount,

        BigDecimal zeroRatedAmount,

        BigDecimal vatAmount,

        BigDecimal totalAmount,

        PosPaymentMethod paymentMethod,

        String paymentReference,

        BigDecimal amountTendered,

        BigDecimal changeAmount,

        PosTransactionStatus status,

        LocalDateTime completedAt,

        LocalDateTime voidedAt,

        String voidedBy,

        String voidReason,

        boolean isTaxInvoice,

        boolean cbmsSynced,

        int cbmsRetryCount,

        String notes,

        List<PosTransactionLineResponse> lines,

        Integer version
) {
}
