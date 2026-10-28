package com.erp.business.inventory.domain;

/**
 * The kind of storage a warehouse provides.
 *
 * <p>Kept as an enum rather than free text because the value decides storage rules.
 * A receipt into {@link #COLD_STORAGE} and one into {@link #TRANSIT} cannot be treated
 * alike by picking or by valuation, and forcing the choice at the request boundary is
 * how a typo becomes a compile failure instead of a mis-stored pallet.
 *
 * <p>These describe the <em>facility</em>, not the state of any goods in it. Goods that
 * have arrived but cannot be accepted belong in {@link #QUARANTINE}, which is a
 * separate warehouse precisely because it must be excluded from available stock.
 */
public enum WarehouseType {

    /** Mixed general-purpose storage. The default for a company's first warehouse. */
    GENERAL,

    /** Temperature-controlled. Carries a different expiry and shrinkage profile. */
    COLD_STORAGE,

    /** In transit between locations. Not owned stock until received into a real warehouse. */
    TRANSIT,

    /** Received but not yet accepted for use. Excluded from available quantities. */
    QUARANTINE,

    /** Returned goods awaiting inspection or disposition. */
    RETURNS,

    /** Damaged or obsolete goods awaiting write-off. */
    SCRAP
}
