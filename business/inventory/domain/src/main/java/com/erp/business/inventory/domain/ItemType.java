package com.erp.business.inventory.domain;

/**
 * Classification of an item by its role in the business.
 *
 * <p>GOODS and SERVICE align with Nepal IRD billing rules and remain for
 * backward-compatibility with existing records. The four additional values
 * support production-oriented workflows (restaurant, manufacturing, pharmacy,
 * printing, etc.) without any database schema break — they share the same
 * {@code item_type} VARCHAR column.
 *
 * <p>Usage flags ({@code isPurchasable}, {@code isSellable}, {@code isComponent},
 * {@code isManufactured}) on {@link Item} are the runtime gates. The type is a
 * classification label; the flags are what procurement, POS, and manufacturing
 * actually enforce.
 */
public enum ItemType {

    /** Generic tradeable good — purchased and sold as-is. (Nepal IRD: सामान) */
    GOODS,

    /** Service / labour — not physically stocked. (Nepal IRD: सेवा) */
    SERVICE,

    /**
     * Raw material consumed in production.
     * Examples: Sugar, Tea Leaf, Flour, Fabric.
     * Typical flags: isPurchasable=true, isComponent=true.
     */
    RAW_MATERIAL,

    /**
     * Finished product produced from raw materials and sold.
     * Examples: Cup of Tea, Plate of Chowmin, Wooden Chair.
     * Typical flags: isSellable=true, isManufactured=true.
     */
    FINISHED_GOODS,

    /**
     * Item consumed internally without being sold or tracked as a component.
     * Examples: Tissue Paper, Cleaning Supplies, Staff Uniforms.
     * Typical flags: isPurchasable=true.
     */
    CONSUMABLE,

    /**
     * Work-in-progress item used as input to further production.
     * Examples: Pre-brewed Tea Base, Semi-stitched garment.
     * Typical flags: isComponent=true, isManufactured=true.
     */
    SEMI_FINISHED
}
