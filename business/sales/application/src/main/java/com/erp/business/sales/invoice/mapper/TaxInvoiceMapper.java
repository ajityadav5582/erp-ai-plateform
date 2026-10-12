package com.erp.business.sales.invoice.mapper;

import com.erp.business.sales.invoice.domain.SalesInvoice;
import com.erp.business.sales.invoice.domain.SalesInvoiceLine;
import com.erp.business.sales.invoice.dto.TaxInvoiceLineView;
import com.erp.business.sales.invoice.dto.TaxInvoiceView;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Builds the printable IRD tax-invoice view from the {@link SalesInvoice}
 * aggregate, including the verification QR payload.
 *
 * <p>Seller identity (name, PAN, address) is supplied by the caller from the
 * issuing company's IRD profile, because the company master is not owned by this
 * service; everything else is derived from the invoice.
 */
@Component
public class TaxInvoiceMapper {

    /**
     * Builds the tax-invoice view. Seller fields are passed in from the company
     * profile; a null seller PAN is rendered as an empty string so the QR stays
     * well-formed until the profile is completed.
     */
    public TaxInvoiceView toView(SalesInvoice invoice,
                                 String sellerName, String sellerPan, String sellerAddress,
                                 String buyerAddress) {
        List<TaxInvoiceLineView> lineViews = invoice.getLines().stream()
                .map(this::toLineView)
                .toList();

        String documentTitle = Boolean.TRUE.equals(invoice.getIsTaxInvoice())
                ? "Tax Invoice" : "Invoice";

        return new TaxInvoiceView(
                documentTitle,
                nullToEmpty(sellerName),
                nullToEmpty(sellerPan),
                nullToEmpty(sellerAddress),
                invoice.getCustomerName(),
                nullToEmpty(invoice.getCustomerPanNumber()),
                nullToEmpty(buyerAddress),
                invoice.getInvoiceNumber(),
                invoice.getInvoiceDate(),
                invoice.getInvoiceDateNp(),
                invoice.getStatus(),
                invoice.getCurrency(),
                lineViews,
                invoice.getSubtotalAmount(),
                invoice.getVatAmount(),
                invoice.getTotalAmount(),
                buildQrPayload(invoice, sellerPan)
        );
    }

    private TaxInvoiceLineView toLineView(SalesInvoiceLine line) {
        return new TaxInvoiceLineView(
                line.getLineNumber(),
                line.getItemCode(),
                line.getItemName(),
                line.getUomCode(),
                line.getQuantity(),
                line.getUnitPrice(),
                line.getDiscountPercentage(),
                line.getVatType(),
                line.getVatRate(),
                line.getLineAmount(),
                line.vatAmount()
        );
    }

    /**
     * A compact, pipe-delimited encoding of the invoice's reconcilable figures for
     * the printed QR code. Field order is fixed so a scanner can parse it
     * deterministically; the seller PAN is included once the company profile
     * carries it.
     */
    private String buildQrPayload(SalesInvoice invoice, String sellerPan) {
        return String.join("|",
                nullToEmpty(sellerPan),
                nullToEmpty(invoice.getCustomerPanNumber()),
                invoice.getInvoiceNumber(),
                invoice.getInvoiceDateNp(),
                invoice.getSubtotalAmount().toPlainString(),
                invoice.getVatAmount().toPlainString(),
                invoice.getTotalAmount().toPlainString());
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
