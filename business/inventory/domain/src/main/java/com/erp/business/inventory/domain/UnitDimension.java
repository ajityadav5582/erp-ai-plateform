package com.erp.business.inventory.domain;

/**
 * The physical quantity a {@link Unit} measures.
 *
 * <p>The dimension is not decorative metadata: it is the rule that decides
 * whether two units may ever be compared or converted into one another. Mass
 * may be expressed in kilograms or grams, but a kilogram can never be added to
 * a litre, so the dimension is what lets the domain reject an invalid pairing
 * before it reaches the database rather than after it has corrupted a stock
 * balance.
 *
 * <p>Stored as {@code EnumType.STRING} so the persisted value stays readable in
 * SQL and adding a constant is an additive change rather than a renumbering of
 * existing rows.
 */
public enum UnitDimension {

    /** Weights: kg, g, lb, ton. */
    MASS,

    /** Liquid and gas volumes: L, mL, m3. */
    VOLUME,

    /** Linear measurements: m, cm, mm, ft. */
    LENGTH,

    /** Surface measurements: m2, ft2, acre. */
    AREA,

    /**
     * Discrete, countable things: pcs, box, pallet, dozen.
     *
     * <p>Kept distinct from {@link #MASS} and {@link #VOLUME} on purpose. A
     * "box" is a packaging container whose real contents vary by product, so
     * treating it as a mass or a volume would silently corrupt arithmetic.
     */
    COUNT,

    /** Durations and lead times: hours, days, weeks. */
    TIME
}
