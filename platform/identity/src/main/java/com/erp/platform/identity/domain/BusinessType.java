package com.erp.platform.identity.domain;

/**
 * Operational business category of a company.
 *
 * <p>Distinct from {@link CompanyType}, which describes the legal structure
 * (Private Limited, Sole Proprietorship, ...). {@code BusinessType} describes
 * what the company actually does day-to-day and is used to configure
 * module-specific behaviour such as the POS interface.
 */
public enum BusinessType {
    RESTAURANT,
    CAFE,
    HOTEL,
    SWEETS_RETAIL,
    GENERAL_RETAIL,
    GROCERY,
    HARDWARE,
    SANITARY,
    PHARMACY,
    CLOTHING,
    SERVICES,
    OTHER
}
