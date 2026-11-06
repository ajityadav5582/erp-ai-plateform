package com.erp.business.procurement.reports.purchasebook;

import com.erp.business.procurement.purchaseinvoice.domain.PurchaseInvoice;
import com.erp.business.procurement.purchaseinvoice.infrastructure.persistence.PurchaseInvoiceLineRepository;
import com.erp.business.procurement.purchaseinvoice.infrastructure.persistence.PurchaseInvoiceRepository;
import com.erp.platform.common.nepali.date.NepaliDate;
import com.erp.platform.common.nepali.date.NepaliDateConverter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class PurchaseBookServiceImpl implements PurchaseBookService {

    private final PurchaseInvoiceRepository repository;
    private final PurchaseInvoiceLineRepository lineRepository;

    public PurchaseBookServiceImpl(PurchaseInvoiceRepository repository,
                                  PurchaseInvoiceLineRepository lineRepository) {
        this.repository = repository;
        this.lineRepository = lineRepository;
    }

    @Override
    public PurchaseBookResponse getPurchaseBook(Long tenantId, Long companyId, Long fiscalYearId,
                                               String startDateNp, String endDateNp, Long supplierId,
                                               Pageable pageable) {
        LocalDate from = toAd(startDateNp);
        LocalDate to = toAd(endDateNp);
        Page<Object[]> page = repository.findPurchaseBook(tenantId, companyId, fiscalYearId, supplierId, from, to, pageable);

        // Collect invoice IDs for grouped line aggregation (avoids N+1)
        List<Long> invoiceIds = new ArrayList<>();
        for (Object[] r : page.getContent()) {
            // r[0] is now invoice id (added to query)
            if (r[0] != null) {
                invoiceIds.add(((Number) r[0]).longValue());
            }
        }

        // Grouped aggregation: one query for all invoices on this page
        Map<Long, CategoryTotals> lineTotals = new HashMap<>();
        java.util.Set<Long> invoicesWithLines = new java.util.HashSet<>();
        if (!invoiceIds.isEmpty()) {
            for (Object[] agg : lineRepository.aggregateByInvoiceAndTaxCategory(invoiceIds)) {
                Long invId = ((Number) agg[0]).longValue();
                invoicesWithLines.add(invId);
                String category = agg[1] == null ? "" : agg[1].toString();
                BigDecimal lineAmt = toBd(agg[2]);
                BigDecimal taxableAmt = toBd(agg[3]);
                BigDecimal taxAmt = toBd(agg[4]);
                CategoryTotals ct = lineTotals.computeIfAbsent(invId, k -> new CategoryTotals());
                switch (category) {
                    case "TAXABLE" -> { ct.taxable = ct.taxable.add(taxableAmt); ct.vat = ct.vat.add(taxAmt); }
                    case "EXEMPT" -> ct.exempt = ct.exempt.add(lineAmt);
                    case "ZERO_RATED" -> ct.zeroRated = ct.zeroRated.add(lineAmt);
                    case "NON_TAXABLE" -> ct.nonTaxable = ct.nonTaxable.add(lineAmt);
                    default -> { /* unknown category, skip */ }
                }
            }
        }

        List<PurchaseBookRow> rows = new ArrayList<>();
        BigDecimal totalPurchase = BigDecimal.ZERO;
        BigDecimal exempt = BigDecimal.ZERO;
        BigDecimal taxable = BigDecimal.ZERO;
        BigDecimal vat = BigDecimal.ZERO;
        BigDecimal importTaxable = BigDecimal.ZERO;
        BigDecimal importVat = BigDecimal.ZERO;
        for (Object[] r : page.getContent()) {
            Long invId = ((Number) r[0]).longValue();
            CategoryTotals ct = lineTotals.getOrDefault(invId, new CategoryTotals());
            PurchaseBookRow row = map(r, ct, invoicesWithLines.contains(invId));
            rows.add(row);
            totalPurchase = totalPurchase.add(nz(row.totalPurchaseAmount()));
            exempt = exempt.add(nz(row.exemptPurchaseAmount()));
            taxable = taxable.add(nz(row.taxablePurchaseAmount()));
            vat = vat.add(nz(row.inputVatAmount()));
            importTaxable = importTaxable.add(nz(row.importTaxableAmount()));
            importVat = importVat.add(nz(row.importVatAmount()));
        }
        PurchaseBookTotals totals = new PurchaseBookTotals(totalPurchase, exempt, taxable, vat, importTaxable, importVat);
        return new PurchaseBookResponse(rows, totals, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Override
    public byte[] exportExcel(Long tenantId, Long companyId, Long fiscalYearId,
                             String startDateNp, String endDateNp, Long supplierId) {
        PurchaseBookResponse resp = getPurchaseBook(tenantId, companyId, fiscalYearId, startDateNp, endDateNp, supplierId, PageRequest.of(0, Integer.MAX_VALUE));
        return PurchaseBookExcelExporter.export(resp.rows(), resp.totals());
    }

    @Override
    public String exportCsv(Long tenantId, Long companyId, Long fiscalYearId,
                           String startDateNp, String endDateNp, Long supplierId) {
        PurchaseBookResponse resp = getPurchaseBook(tenantId, companyId, fiscalYearId, startDateNp, endDateNp, supplierId, PageRequest.of(0, Integer.MAX_VALUE));
        StringBuilder sb = new StringBuilder();
        sb.append("Miti (BS),Date (AD),Invoice No,Supplier,PAN,Total,Taxable,Exempt,VAT,Import Taxable,Import VAT,Capital Asset\n");
        for (PurchaseBookRow r : resp.rows()) {
            sb.append(csv(r.mitiBs())).append(',')
               .append(csv(r.dateAd() == null ? "" : r.dateAd().toString())).append(',')
               .append(csv(r.invoiceNumber())).append(',')
               .append(csv(r.supplierName())).append(',')
               .append(csv(r.supplierPan())).append(',')
               .append(r.totalPurchaseAmount()).append(',')
               .append(r.taxablePurchaseAmount()).append(',')
               .append(r.exemptPurchaseAmount()).append(',')
               .append(r.inputVatAmount()).append(',')
               .append(r.importTaxableAmount()).append(',')
               .append(r.importVatAmount()).append(',')
               .append(r.capitalAsset()).append('\n');
        }
        return sb.toString();
    }

    private PurchaseBookRow map(Object[] r, CategoryTotals ct, boolean hasLineData) {
        // r[0] = invoice id, r[1] = invoice_date, r[2] = invoice_number, r[3] = supplier_name, r[4] = tax_id (PAN)
        // r[5] = total_amount, r[6] = subtotal_amount, r[7] = tax_amount
        LocalDate ad = (r[1] instanceof java.sql.Date) ? ((java.sql.Date) r[1]).toLocalDate()
                : (r[1] instanceof LocalDate ? (LocalDate) r[1] : LocalDate.parse(r[1].toString()));
        String miti = NepaliDateConverter.fromAd(ad).toString().replace("-", ".");
        String invoice = r[2] == null ? "" : r[2].toString();
        String supplier = r[3] == null ? "" : r[3].toString();
        String pan = r[4] == null ? "" : r[4].toString();
        BigDecimal total = toBd(r[5]);
        BigDecimal subtotal = toBd(r[6]);
        BigDecimal tax = toBd(r[7]);
        BigDecimal exemptAmt = ct.exempt;
        BigDecimal taxableAmt = ct.taxable;
        BigDecimal vatAmt = ct.vat;
        // Fallback only when no aggregation rows exist for this invoice (historical/null data)
        if (!hasLineData) {
            taxableAmt = subtotal;
            vatAmt = tax;
            exemptAmt = BigDecimal.ZERO;
        }
        return new PurchaseBookRow(miti, ad, invoice, null, supplier, pan, total, exemptAmt, taxableAmt, vatAmt,
                BigDecimal.ZERO, BigDecimal.ZERO, false);
    }

    private LocalDate toAd(String bs) {
        if (bs == null || bs.isBlank()) return null;
        try {
            String normalized = bs.replace(".", "-");
            return NepaliDate.parse(normalized).toAdDate();
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal toBd(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal) return (BigDecimal) o;
        return new BigDecimal(o.toString());
    }

    private BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    private String csv(String v) {
        if (v == null) return "";
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            return "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }

    /** Helper to accumulate per-category totals per invoice. */
    private static class CategoryTotals {
        BigDecimal taxable = BigDecimal.ZERO;
        BigDecimal exempt = BigDecimal.ZERO;
        BigDecimal zeroRated = BigDecimal.ZERO;
        BigDecimal nonTaxable = BigDecimal.ZERO;
        BigDecimal vat = BigDecimal.ZERO;
    }
}
