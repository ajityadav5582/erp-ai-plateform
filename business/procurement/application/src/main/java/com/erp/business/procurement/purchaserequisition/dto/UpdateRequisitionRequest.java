package com.erp.business.procurement.purchaserequisition.dto;

import com.erp.business.procurement.purchaserequisition.domain.RequisitionPriority;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * A full replacement of an editable requisition's content.
 *
 * <p><strong>Why PUT and not PATCH, and why the whole grid.</strong> The client
 * edits a grid of lines and submits it. A partial update would have to define
 * what "absent" means for every field - unset, cleared, or unchanged - and get
 * that wrong in a way that silently deletes data. A full replacement has exactly
 * one meaning: this is now the document. The service enforces that by rebuilding
 * the line set from the payload and recalculating the totals once, so no
 * intermediate flush can persist a half-applied edit.
 *
 * <p><strong>What cannot be changed here.</strong> There is no branch and no
 * requester. Branch decides which approval policy applies, and re-pointing a
 * document at another branch would silently change the control it is subject to.
 * The requester is the person who raised it, and authorship is not transferable
 * by editing the document. A requester who genuinely raised this for the wrong
 * branch should cancel it and raise a new one, which leaves an honest trail of
 * the mistake rather than quietly re-pointing something other people may already
 * have acted on.
 *
 * <p><strong>The request date, however, is editable</strong>, and this is
 * deliberate rather than an oversight. A draft raised with last week's date
 * because someone was tidying up a backlog needs to be correctable without
 * cancelling and re-raising it. The guard is {@code amendRequestDate}: permitted
 * while DRAFT, refused the moment the document is in the workflow,
 * and never permitted to move past the needed-by date. Once submitted, the date
 * is part of the document's identity - it fixes the numbering period and the
 * fiscal year the document is reported against - and from that point it is
 * fixed.
 *
 * <p>This paragraph previously claimed there was "no request date" while the
 * record below carried a {@code @NotNull} one and the service called
 * {@code amendRequestDate} on it. A contract that describes itself as not
 * accepting a field it requires is worse than no contract: the next reader
 * either deletes the field or refuses the edit, and both look defensible from
 * the documentation.
 */
public record UpdateRequisitionRequest(

        @NotNull(message = "Request date is required")
        LocalDate requestDate,

        /**
         * The request date in Bikram Sambat, {@code yyyy-MM-dd}, as the picker
         * produced it.
         *
         * <p>Not a second source of truth: the value stored is derived from
         * {@link #requestDate} on every persist. This field exists only so a
         * client that sends both can be checked for self-consistency before
         * anything is written. Sending it and disagreeing with the AD date is a
         * client bug - a stale field left behind by a copied row, or a picker
         * that converted one date while the form held another - and it is
         * rejected rather than quietly corrected, because correcting it would
         * hide the bug until it showed up as a document filed under a period
         * nobody expected.
         *
         * <p>Optional. A client that does not deal in Bikram Sambat dates may omit
         * it; only a value that is present and wrong is refused.
         */
        @Size(max = 10)
        String requestDateNp,

        /**
         * The needed-by date, or null to clear it. Optional in both directions:
         * a requisition with no committed delivery date is legitimate, and the
         * aggregate already refuses one that precedes the request date.
         */
        LocalDate requiredByDate,

        /**
         * The revised urgency, or null to keep the current one. Note the
         * deliberate asymmetry with {@link #requiredByDate}: an absent priority
         * leaves the document alone, because defaulting it to NORMAL would
         * silently demote an URGENT draft whose caller simply did not resend the
         * field.
         */
        RequisitionPriority priority,

        /**
         * The revised ISO 4217 code, or null/blank to keep the current one.
         *
         * <p>This field was previously accepted and thrown away: the endpoint
         * returned 200, the caller saw their change echoed nowhere, and reloading
         * showed the old value with no indication that the save had not taken.
         * A field a client can send but cannot set turns a visible bug into an
         * invisible one.
         *
         * <p>Null and blank both mean unchanged rather than reset, which is why
         * this does not follow the full-replacement rule the other absent fields
         * obey. A form field the user cleared submits "", not null, so both
         * spellings of "not supplied" have to behave the same or the safer one is
         * not safe at all. Resetting falls through to the NPR default and would
         * re-denominate every amount on the document without a single number
         * changing - turning a 5,000 rupee estimate into a 5,000 dollar one on the
         * strength of a field the caller left out.
         */
        @Size(max = 3)
        String currency,

        /**
         * The revised destination warehouse. Null leaves the current one in place,
         * following the same "absent means unchanged" rule as currency: clearing a
         * destination would leave goods with nowhere to go, and that should be a
         * deliberate act somewhere that can see the consequences.
         */
        @Positive(message = "Warehouse id must be a positive identifier")
        Long warehouseId,

        @Size(max = 64)
        String warehouseCode,

        @Size(max = 255)
        String warehouseName,

        /**
         * Mandatory for HIGH and URGENT, which ask to jump the queue and therefore
         * owe the approver an explanation. Enforced by the aggregate, not here,
         * because the rule depends on the effective priority rather than on what
         * this one payload happens to contain.
         */
        @Size(max = 2000)
        String justification,

        @Size(max = 2000)
        String notes,

        /**
         * The complete replacement line set, renumbered from 1 in the order given.
         */
        @NotEmpty(message = "A requisition needs at least one line")
        @Valid
        List<CreateRequisitionLineRequest> lines
) {
}
