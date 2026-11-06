package com.erp.business.pos.receipt.qr;

public class QrPayloadGenerator {
    public static String build(String sellerPan, String invoiceNo, String invoiceDateBs,
                              String totalAmount, String vatAmount, String cbmsStatus) {
        return sellerPan + "|" + invoiceNo + "|" + invoiceDateBs + "|" + totalAmount + "|" + vatAmount + "|" + cbmsStatus;
    }

    public static String verifyUrl(String sellerPan, String invoiceNo, String fiscalYearCode) {
        return "https://taxpayer.ird.gov.np/verify?pan=" + sellerPan + "&bill=" + invoiceNo + "&fy=" + fiscalYearCode;
    }
}
