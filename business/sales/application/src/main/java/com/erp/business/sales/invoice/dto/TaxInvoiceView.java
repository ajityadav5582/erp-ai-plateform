package com.erp.business.sales.invoice.dto;

import com.erp.business.sales.invoice.domain.SalesInvoiceStatus;
import com.erp.business.sales.invoice.domain.VatType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * The printable Nepal IRD tax-invoice view: every field a valid "Tax Invoice"
 * (Kar Purji) must carry, plus a verification QR payload.
 *
 * <p>Seller fields are populated from the issuing company's IRD profile; the
 * buyer, metadata, line detail and VAT breakdown come from the invoice itself.
 * The {@code qrPayload} is a compact, verifiable encoding of the invoice's key
 * figures that an IRD scanner or the CBMS can reconcile against the ledger.
 *
 * @param documentTitle always "Tax Invoice" for a B2B supply, "Invoice" otherwise
 * @param sellerName    issuing company name
 * @param sellerPan     issuing company PAN
 * @param sellerAddress issuing company address
 * @param buyerName     customer name
 * @param buyerPan      customer PAN (present for a B2B tax invoice)
 * @param buyerAddress  customer address
 * @param invoiceNumber the IRD sequential number
 * @param invoiceDateAd issue date (Gregorian)
 * @param invoiceDateNp issue date (Bikram Sambat)
 * @param lines         the billed lines
 * @param subtotal      net of all lines (VAT-exclusive)
 * @param vatAmount     output VAT
 * @param total         amount payable
 * @param qrPayload     compact verification string for the printed QR code
 */
public record TaxInvoiceView(
        String documentTitle,
        String sellerName,
        String sellerPan,
        String sellerAddress,
        String buyerName,
        String buyerPan,
        String buyerAddress,
        String invoiceNumber,
        LocalDate invoiceDateAd,
        String invoiceDateNp,
        SalesInvoiceStatus status,
        String currency,
        List<TaxInvoiceLineView> lines,
        BigDecimal subtotal,
        BigDecimal vatAmount,
        BigDecimal total,
        String qrPayload
) {
}
