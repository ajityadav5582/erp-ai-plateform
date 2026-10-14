package com.erp.business.pos.transaction.dto;

import com.erp.business.pos.domain.PosVatType;

import java.math.BigDecimal;

/**
 * Read model for one line of a POS transaction. Every money field was computed
 * by the domain at the time the line was priced — this record only carries the
 * frozen snapshot out to the client.
 */
public record PosTransactionLineResponse(

        Long id,

        int lineNumber,

        Long itemId,

        String itemCode,

        String itemName,

        String barcode,

        String uomCode,

        BigDecimal quantity,

        BigDecimal unitPrice,

        BigDecimal discountPercentage,

        /** Net line amount after discount, before VAT. */
        BigDecimal lineAmount,

        PosVatType vatType,

        BigDecimal vatRate,

        /** VAT on this line: {@code lineAmount × vatRate / 100}. */
        BigDecimal vatAmount,

        String description
) {
}
