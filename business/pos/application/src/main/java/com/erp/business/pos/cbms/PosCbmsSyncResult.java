package com.erp.business.pos.cbms;

/**
 * Outcome of one CBMS push attempt. Mirrors the Sales module's result type so
 * the two modules' sync workers behave identically.
 */
public record PosCbmsSyncResult(
        boolean success,
        int statusCode,
        String message,
        String responseBody
) {
    public static PosCbmsSyncResult ok(String responseBody) {
        return new PosCbmsSyncResult(true, 200, "OK", responseBody);
    }

    public static PosCbmsSyncResult fail(int statusCode, String message) {
        return new PosCbmsSyncResult(false, statusCode, message, message);
    }
}
