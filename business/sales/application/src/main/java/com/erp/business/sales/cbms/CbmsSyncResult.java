package com.erp.business.sales.cbms;

public record CbmsSyncResult(
        boolean success,
        int statusCode,
        String message,
        String responseBody
) {
    public static CbmsSyncResult ok(String responseBody) {
        return new CbmsSyncResult(true, 200, "OK", responseBody);
    }

    public static CbmsSyncResult fail(int statusCode, String message) {
        return new CbmsSyncResult(false, statusCode, message, message);
    }
}
