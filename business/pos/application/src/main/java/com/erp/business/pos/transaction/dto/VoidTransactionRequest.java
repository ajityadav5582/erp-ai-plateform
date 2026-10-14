package com.erp.business.pos.transaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Void payload. A reason is mandatory: the void is frozen onto the transaction
 * for the audit trail and IRD reconciliation, so a void without an explanation
 * is rejected by the domain.
 *
 * @param reason brief explanation of why the sale is being voided
 */
public record VoidTransactionRequest(

        @NotBlank(message = "A reason is required to void a transaction")
        @Size(max = 500, message = "Reason must not exceed 500 characters")
        String reason
) {
}
