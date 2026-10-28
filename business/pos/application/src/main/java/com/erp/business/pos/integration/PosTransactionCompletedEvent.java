package com.erp.business.pos.integration;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PosTransactionCompletedEvent(
        UUID eventId,
        String tenantId,
        Long companyId,
        Long fiscalYearId,
        Long posTransactionId,
        String transactionNumber,
        Long customerId,
        String customerName,
        String customerPanNumber,
        LocalDate invoiceDate,
        String notes,
        String cashierUserId,
        List<Line> lines
) implements Serializable {

    public record Line(
            Long itemId,
            String uomCode,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal discountPercentage,
            String vatType,
            BigDecimal vatRate,
            String description
    ) implements Serializable {
    }
}
