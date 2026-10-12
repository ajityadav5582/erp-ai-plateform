package com.erp.business.inventory.domain;

/** Excise Duty calculation type under Nepal Excise Act 2058. */
public enum ExciseType {
    NONE,             // No excise duty applicable
    PERCENTAGE,       // Percentage of selling/assessable price (%)
    SPECIFIC_AMOUNT   // Fixed amount per unit (रु/एकाइ)
}
