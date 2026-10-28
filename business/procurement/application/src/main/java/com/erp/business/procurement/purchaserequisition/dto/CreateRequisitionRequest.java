package com.erp.business.procurement.purchaserequisition.dto;

import com.erp.business.procurement.purchaserequisition.domain.RequisitionPriority;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record CreateRequisitionRequest(

        /**
         * Branch raising the demand. Validated by the service against
         * (tenantId, companyId); never trusted as-is from the body.
         */
        @NotNull(message = "Branch is required")
        Long branchId,

        Long departmentId,

        @NotNull(message = "Request date is required")
        LocalDate requestDate,

        /**
         * The request date in Bikram Sambat, {@code yyyy-MM-dd}, as the picker
         * presents it.
         *
         * <p>Optional and advisory. The server always recomputes it from
         * {@code requestDate} and persists the derived value, so a client cannot
         * store a BS date that disagrees with the AD date beside it. When present it
         * is cross-checked and a mismatch is rejected rather than silently
         * overwritten: silently ignoring it would leave the user staring at a date
         * they did not pick with no indication anything was wrong.
         */
        @Size(max = 10)
        String requestDateNp,

        LocalDate requiredByDate,

        RequisitionPriority priority,

        @Size(max = 3)
        String currency,

        /**
         * The destination warehouse's id, from the inventory service.
         *
         * <p>Optional. A requester frequently cannot say which site will serve the
         * request, and forcing a guess here adds friction to the most frequent
         * entry path in the system for no gain. When omitted, the order and then
         * the receipt fall through to the company default.
         */
        @Positive(message = "Warehouse id must be a positive identifier")
        Long warehouseId,

        /** Frozen alongside the id so the document keeps printing what it asked for. */
        @Size(max = 64)
        String warehouseCode,

        @Size(max = 255)
        String warehouseName,

        /**
         * Mandatory for HIGH and URGENT, which ask to jump the queue and therefore
         * owe the approver an explanation.
         */
        @Size(max = 2000)
        String justification,

        @Size(max = 2000)
        String notes,

        @NotEmpty(message = "A requisition needs at least one line")
        @Valid
        List<CreateRequisitionLineRequest> lines
) {
}
