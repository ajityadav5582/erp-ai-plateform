package com.erp.business.inventory.interfaces.stock;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

/**
 * A batch of stock movements sharing one source document - the unit Inventory
 * receives from Procurement. Posting the whole receipt in one call is what makes
 * the operation idempotent: the reference (type + id) identifies the document, so
 * a retry after a timeout is recognised and returns the movements already posted
 * instead of duplicating them.
 *
 * <p>The tenant and company are deliberately absent. They are resolved from the
 * caller's authenticated context on the Inventory side, exactly as they are for
 * every other Inventory endpoint, so a payload cannot address another company.
 *
 * @param referenceType   the source document type, e.g. "GOODS_RECEIPT"
 * @param referenceId     the source document id; with the type this is the idempotency key
 * @param referenceNumber the human-readable source document number, frozen onto the ledger
 * @param movementDate    when the goods physically moved; defaults to now when null
 * @param postedBy        the actor, for the audit trail
 * @param notes           optional free text
 * @param lines           the movements to post; at least one
 */
public record StockReceiptRequest(
        @NotBlank(message = "Reference type is required") String referenceType,
        @NotNull(message = "Reference id is required") Long referenceId,
        String referenceNumber,
        Instant movementDate,
        String postedBy,
        String notes,
        @NotEmpty(message = "At least one movement line is required")
        @Valid List<StockMovementLineRequest> lines
) {
}
