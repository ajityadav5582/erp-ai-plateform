package com.erp.business.procurement.goodsreceipt.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Records a delivery against a purchase order.
 *
 * <p><strong>Two ways to say which goods arrived.</strong> {@link #lines} is the
 * expressive form and carries an optional {@link LineRequest#receivedQuantity()}
 * for a short delivery. {@link #lineNumbers()} is the compact form kept for the
 * common case of "these lines arrived, in full". They are mutually exclusive and
 * {@code lines} wins, so a client can migrate without a flag day and an existing
 * client keeps working untouched.
 *
 * <p><strong>Why a quantity may now be sent.</strong> Partial receipt used to be
 * refused outright, on the reasoning that a quantity supplied by the caller could
 * let the order and the receipt note disagree about what is still outstanding.
 * That disagreement is prevented by construction rather than by prohibition: the
 * quantity is bounded by the order line's outstanding quantity in the aggregate,
 * and the order is the only writer of {@code receivedQuantity}. Without it, the
 * only way to record a short delivery was to book the ordered quantity and
 * correct it afterwards - which is how inventory ends up overstated, and it left
 * {@code PARTIALLY_RECEIVED} unreachable on both the line and the header.
 *
 * <p>Omitting both fields receives every outstanding line in full, which is the
 * common case of a supplier delivering the order complete.
 *
 * @param purchaseOrderId    the order being received against
 * @param receiptDate        when the goods arrived, defaults to today
 * @param warehouseId        where the goods landed, optional, see {@link #warehouseId()}
 * @param lines              the lines that arrived, each optionally short
 * @param lineNumbers        the line numbers that arrived in full
 * @param deliveryChallanNo  the supplier's own delivery reference
 * @param notes              free text notes
 */
public record CreateGoodsReceiptRequest(

        @NotNull(message = "Purchase order is required")
        @Positive(message = "Purchase order id must be positive")
        Long purchaseOrderId,

        /** When the goods arrived. Defaults to today. */
        LocalDate receiptDate,

        /**
         * The warehouse the goods were physically received into.
         *
         * <p>Optional in the payload but resolved before the note is posted: when
         * omitted the order's nominated warehouse is used, and failing that the
         * company default. That fallback chain exists because the gate clerk knows
         * where the lorry actually stopped, and the order only records where it was
         * meant to go - so this field is stated whenever reality differs from the
         * nomination, and may be omitted when it agrees.
         */
        @Positive(message = "Warehouse id must be a positive identifier")
        Long warehouseId,

        /** Frozen alongside the id so the posted note keeps printing what it received. */
        @Size(max = 64)
        String warehouseCode,

        @Size(max = 255)
        String warehouseName,

        /**
         * The order lines that arrived, each with an optional quantity for a short
         * delivery. Takes precedence over {@code lineNumbers}.
         */
        @Valid
        List<LineRequest> lines,

        /**
         * The order lines that arrived in full. Omit to receive every outstanding
         * line, which is the common case of a supplier delivering the order
         * complete.
         */
        List<@Positive(message = "Line numbers must be positive") Integer> lineNumbers,

        /** The supplier's delivery challan or gate pass number. */
        @Size(max = 100)
        String deliveryChallanNo,

        @Size(max = 2000)
        String notes
) {

    /**
     * One arriving line.
     *
     * <p>A null {@code receivedQuantity} means the whole outstanding quantity,
     * so a client that has no notion of a short delivery can send only the line
     * number and get complete-delivery behaviour.
     *
     * @param lineNumber       the 1-based order line that arrived
     * @param receivedQuantity how much arrived, null for everything outstanding
     * @param remarks          per line remarks, may be null
     */
    public record LineRequest(

            @NotNull(message = "Line number is required")
            @Positive(message = "Line number must be positive")
            Integer lineNumber,

            /**
             * How much of this line physically arrived. Null receives the full
             * outstanding quantity.
             */
            @Positive(message = "Received quantity must be positive")
            BigDecimal receivedQuantity,

            /**
             * Overrides the note's warehouse for this line only.
             *
             * <p>For a consignment split across two sites - some of the goods offloaded
             * at one depot, the rest continuing to another. Null, the normal case,
             * takes the note's warehouse.
             */
            @Positive(message = "Warehouse id must be a positive identifier")
            Long warehouseId,

            @Size(max = 500)
            String remarks
    ) {
    }
}
