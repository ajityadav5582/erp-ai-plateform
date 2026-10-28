package com.erp.platform.security.context;

/**
 * Convenience accessors for the request-scoped company and fiscal-year selection.
 *
 * <p>Business services (inventory, sales, finance, ...) use this instead of accepting
 * a company or fiscal year as a request parameter. The values originate from the
 * {@code X-Company-Id} / {@code X-Fiscal-Year-Id} headers forwarded by the API Gateway
 * and are populated by the shared JWT authentication filter, so a client cannot
 * influence them through request bodies or query strings.
 *
 * <p>Accessors return {@code null} when the user has not selected a company or fiscal
 * year yet. Callers that require the context should use
 * {@link #requireCompanyId()} / {@link #requireFiscalYearId()}.
 *
 * @since 1.0.0
 */
public final class BusinessContext {

    private BusinessContext() {
    }

    /**
     * @return the active company id, or {@code null} if not selected.
     */
    public static Long getCompanyId() {
        return parse(com.erp.platform.common.context.RequestContext.getCompanyId());
    }

    /**
     * @return the active company id.
     * @throws IllegalStateException if no company has been selected.
     */
    public static Long requireCompanyId() {
        Long companyId = getCompanyId();
        if (companyId == null) {
            throw new IllegalStateException(
                    "No company selected. Send the X-Company-Id header (obtained after company selection).");
        }
        return companyId;
    }

    /**
     * @return the active fiscal year id, or {@code null} if not selected.
     */
    public static Long getFiscalYearId() {
        return parse(com.erp.platform.common.context.RequestContext.getFiscalYearId());
    }

    /**
     * @return the active fiscal year id.
     * @throws IllegalStateException if no fiscal year has been selected.
     */
    public static Long requireFiscalYearId() {
        Long fiscalYearId = getFiscalYearId();
        if (fiscalYearId == null) {
            throw new IllegalStateException(
                    "No fiscal year selected. Send the X-Fiscal-Year-Id header (obtained after fiscal year selection).");
        }
        return fiscalYearId;
    }

    /**
     * @return true if a company has been selected for this request.
     */
    public static boolean hasCompany() {
        return getCompanyId() != null;
    }

    /**
     * @return true if a fiscal year has been selected for this request.
     */
    public static boolean hasFiscalYear() {
        return getFiscalYearId() != null;
    }

    private static Long parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
