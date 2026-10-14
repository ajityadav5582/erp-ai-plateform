package com.erp.business.pos.transaction.mapper;

import com.erp.business.pos.domain.PosTransaction;
import com.erp.business.pos.domain.PosTransactionLine;
import com.erp.business.pos.transaction.dto.PosTransactionLineResponse;
import com.erp.business.pos.transaction.dto.PosTransactionResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Maps the {@link PosTransaction} aggregate (with its lines) to its read model.
 *
 * <p>Line VAT is read through the domain's {@link PosTransactionLine#vatAmount()}
 * so the response can never disagree with the totals the domain computed.
 */
@Component
public class PosTransactionMapper {

    public PosTransactionResponse toResponse(PosTransaction transaction) {
        return new PosTransactionResponse(
                transaction.getId(),
                transaction.getTenantId(),
                transaction.getCompanyId(),
                transaction.getTransactionNumber(),
                transaction.getSessionId(),
                transaction.getRegisterId(),
                transaction.getFiscalYearId(),
                transaction.getBranchId(),
                transaction.getTransactionDate(),
                transaction.getTransactionDateNp(),
                transaction.getCustomerId(),
                transaction.getCustomerCode(),
                transaction.getCustomerName(),
                transaction.getCustomerPanNumber(),
                transaction.getCashierId(),
                transaction.getCashierName(),
                transaction.getWarehouseId(),
                transaction.getWarehouseCode(),
                transaction.getWarehouseName(),
                transaction.getCurrency(),
                transaction.getSubtotalAmount(),
                transaction.getTaxableAmount(),
                transaction.getExemptAmount(),
                transaction.getZeroRatedAmount(),
                transaction.getVatAmount(),
                transaction.getTotalAmount(),
                transaction.getPaymentMethod(),
                transaction.getPaymentReference(),
                transaction.getAmountTendered(),
                transaction.getChangeAmount(),
                transaction.getStatus(),
                toLocalDateTime(transaction.getCompletedAt()),
                toLocalDateTime(transaction.getVoidedAt()),
                transaction.getVoidedBy(),
                transaction.getVoidReason(),
                transaction.isTaxInvoice(),
                transaction.isCbmsSynced(),
                transaction.getCbmsRetryCount(),
                transaction.getNotes(),
                toLineResponses(transaction.getLines()),
                transaction.getVersion()
        );
    }

    private List<PosTransactionLineResponse> toLineResponses(List<PosTransactionLine> lines) {
        if (lines == null || lines.isEmpty()) {
            return List.of();
        }
        return lines.stream()
                .map(this::toLineResponse)
                .toList();
    }

    private PosTransactionLineResponse toLineResponse(PosTransactionLine line) {
        return new PosTransactionLineResponse(
                line.getId(),
                line.getLineNumber(),
                line.getItemId(),
                line.getItemCode(),
                line.getItemName(),
                line.getBarcode(),
                line.getUomCode(),
                line.getQuantity(),
                line.getUnitPrice(),
                line.getDiscountPercentage(),
                line.getLineAmount(),
                line.getVatType(),
                line.getVatRate(),
                line.vatAmount(),
                line.getDescription()
        );
    }

    private LocalDateTime toLocalDateTime(java.time.Instant instant) {
        return instant != null ? LocalDateTime.ofInstant(instant, ZoneId.systemDefault()) : null;
    }
}
