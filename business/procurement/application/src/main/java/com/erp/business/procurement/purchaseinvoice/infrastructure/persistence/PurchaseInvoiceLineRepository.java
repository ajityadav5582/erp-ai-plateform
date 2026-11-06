package com.erp.business.procurement.purchaseinvoice.infrastructure.persistence;

import com.erp.business.procurement.purchaseinvoice.domain.PurchaseInvoiceLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PurchaseInvoiceLineRepository extends JpaRepository<PurchaseInvoiceLine, Long> {

    /**
     * Returns per-invoice, per-tax-category aggregated amounts for a set of invoices.
     * Columns: invoice_id, tax_category, sum(line_amount), sum(taxable_amount), sum(tax_amount)
     */
    @Query(value = "SELECT pil.purchase_invoice_id, pil.tax_category, "
            + "COALESCE(SUM(pil.line_amount), 0), "
            + "COALESCE(SUM(pil.taxable_amount), 0), "
            + "COALESCE(SUM(pil.tax_amount), 0) "
            + "FROM purchase_invoice_lines pil "
            + "WHERE pil.purchase_invoice_id IN :invoiceIds "
            + "GROUP BY pil.purchase_invoice_id, pil.tax_category",
            nativeQuery = true)
    List<Object[]> aggregateByInvoiceAndTaxCategory(@Param("invoiceIds") List<Long> invoiceIds);
}
