package com.erp.business.reports.vat.annex10;

import com.erp.platform.common.nepali.date.NepaliDate;
import com.erp.platform.common.nepali.date.NepaliDateConverter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class VatReturnServiceImpl implements VatReturnService {

    private final JdbcTemplate jdbc;

    public VatReturnServiceImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public VatReturnResponse compute(Long tenantId, Long companyId, Long fiscalYearId, int nepaliMonth) {
        BigDecimal totalSales = BigDecimal.ZERO;
        BigDecimal taxExemptSales = BigDecimal.ZERO;
        BigDecimal zeroRatedExports = BigDecimal.ZERO;
        BigDecimal taxableSales = BigDecimal.ZERO;
        BigDecimal outputVat = BigDecimal.ZERO;
        BigDecimal lessSalesReturnVat = BigDecimal.ZERO;

        List<Map<String, Object>> sales = jdbc.queryForList(
                "SELECT total_amount, taxable_amount, exempt_amount, zero_rated_amount, vat_amount, invoice_date, status " +
                        "FROM sales_invoices WHERE tenant_id = ? AND company_id = ? AND fiscal_year_id = ?",
                tenantId, companyId, fiscalYearId);
        for (Map<String, Object> r : sales) {
            String status = str(r.get("status"));
            if ("CANCELLED".equalsIgnoreCase(status)) continue;
            LocalDate d = toLocalDate(r.get("invoice_date"));
            if (d == null) continue;
            NepaliDate nd;
            try { nd = NepaliDateConverter.fromAd(d); } catch (Exception e) { continue; }
            if (nd.month() != nepaliMonth) continue;
            totalSales = totalSales.add(bd(r.get("total_amount")));
            taxExemptSales = taxExemptSales.add(bd(r.get("exempt_amount")));
            zeroRatedExports = zeroRatedExports.add(bd(r.get("zero_rated_amount")));
            taxableSales = taxableSales.add(bd(r.get("taxable_amount")));
            outputVat = outputVat.add(bd(r.get("vat_amount")));
        }

        List<Map<String, Object>> sreturns = jdbc.queryForList(
                "SELECT vat_amount, return_date FROM sales_returns WHERE tenant_id = ? AND company_id = ? AND fiscal_year_id = ?",
                tenantId, companyId, fiscalYearId);
        for (Map<String, Object> r : sreturns) {
            LocalDate d = toLocalDate(r.get("return_date"));
            if (d == null) continue;
            NepaliDate nd;
            try { nd = NepaliDateConverter.fromAd(d); } catch (Exception e) { continue; }
            if (nd.month() != nepaliMonth) continue;
            lessSalesReturnVat = lessSalesReturnVat.add(bd(r.get("vat_amount")));
        }

        BigDecimal netOutputVat = outputVat.subtract(lessSalesReturnVat);

        BigDecimal totalLocalPurchases = BigDecimal.ZERO;
        BigDecimal taxExemptPurchases = BigDecimal.ZERO;
        BigDecimal taxableLocalPurchases = BigDecimal.ZERO;
        BigDecimal importPurchasesTaxable = BigDecimal.ZERO;
        BigDecimal capitalGoodsPurchases = BigDecimal.ZERO;
        BigDecimal totalEligibleInputVat = BigDecimal.ZERO;
        BigDecimal lessPurchaseReturnVat = BigDecimal.ZERO;

        List<Map<String, Object>> purchases = jdbc.queryForList(
                "SELECT total_amount, subtotal_amount, tax_amount, invoice_date, status FROM purchase_invoices " +
                        "WHERE tenant_id = ? AND company_id = ? AND fiscal_year_id = ?",
                tenantId, companyId, fiscalYearId);
        for (Map<String, Object> r : purchases) {
            String status = str(r.get("status"));
            if ("CANCELLED".equalsIgnoreCase(status)) continue;
            LocalDate d = toLocalDate(r.get("invoice_date"));
            if (d == null) continue;
            NepaliDate nd;
            try { nd = NepaliDateConverter.fromAd(d); } catch (Exception e) { continue; }
            if (nd.month() != nepaliMonth) continue;
            BigDecimal total = bd(r.get("total_amount"));
            BigDecimal subtotal = bd(r.get("subtotal_amount"));
            BigDecimal tax = bd(r.get("tax_amount"));
            totalLocalPurchases = totalLocalPurchases.add(total);
            taxableLocalPurchases = taxableLocalPurchases.add(subtotal);
            totalEligibleInputVat = totalEligibleInputVat.add(tax);
        }

        try {
            List<Map<String, Object>> pret = jdbc.queryForList(
                    "SELECT tax_amount, return_date FROM purchase_returns WHERE tenant_id = ? AND company_id = ? AND fiscal_year_id = ?",
                    tenantId, companyId, fiscalYearId);
            for (Map<String, Object> r : pret) {
                LocalDate d = toLocalDate(r.get("return_date"));
                if (d == null) continue;
                NepaliDate nd;
                try { nd = NepaliDateConverter.fromAd(d); } catch (Exception e) { continue; }
                if (nd.month() != nepaliMonth) continue;
                lessPurchaseReturnVat = lessPurchaseReturnVat.add(bd(r.get("tax_amount")));
            }
        } catch (Exception ignored) {
        }

        BigDecimal netEligibleInputVat = totalEligibleInputVat.subtract(lessPurchaseReturnVat);

        BigDecimal previousCredit = BigDecimal.ZERO;
        BigDecimal netVatPayable = netOutputVat.subtract(netEligibleInputVat).subtract(previousCredit);
        BigDecimal carriedForward = BigDecimal.ZERO;
        BigDecimal payableToGoN = BigDecimal.ZERO;
        if (netVatPayable.compareTo(BigDecimal.ZERO) >= 0) {
            payableToGoN = netVatPayable;
        } else {
            carriedForward = netVatPayable.abs();
        }

        return new VatReturnResponse(
                companyId, fiscalYearId, nepaliMonth,
                totalSales, taxExemptSales, zeroRatedExports, taxableSales, outputVat, lessSalesReturnVat, netOutputVat,
                totalLocalPurchases, taxExemptPurchases, taxableLocalPurchases, importPurchasesTaxable, capitalGoodsPurchases,
                totalEligibleInputVat, lessPurchaseReturnVat, netEligibleInputVat,
                previousCredit, payableToGoN, carriedForward
        );
    }

    @Override
    public byte[] exportPdf(Long tenantId, Long companyId, Long fiscalYearId, int nepaliMonth) {
        VatReturnResponse r = compute(tenantId, companyId, fiscalYearId, nepaliMonth);
        StringBuilder sb = new StringBuilder();
        sb.append("%PDF-1.4\n");
        sb.append("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");
        sb.append("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");
        sb.append("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>\nendobj\n");
        StringBuilder content = new StringBuilder();
        content.append("BT /F1 12 Tf 50 750 Td\n");
        String[] lines = new String[] {
                "VAT Return - Annex 10",
                "Company: " + companyId + "  FiscalYear: " + fiscalYearId + "  Month: " + nepaliMonth,
                "Total Sales: " + r.totalSales(),
                "Taxable Sales: " + r.taxableSales(),
                "Output VAT: " + r.outputVatCollected(),
                "Net Output VAT: " + r.netOutputVatPayable(),
                "Total Purchases: " + r.totalLocalPurchases(),
                "Input VAT: " + r.totalEligibleInputVat(),
                "Net Input VAT: " + r.netEligibleInputVat(),
                "Net VAT Payable to GoN: " + r.netVatPayableToGoN(),
                "Net VAT Carried Forward: " + r.netVatCarriedForward()
        };
        for (String line : lines) {
            content.append("(").append(line.replace("(", "\\(").replace(")", "\\)")).append(") Tj T*\n");
        }
        content.append("ET\n");
        String stream = content.toString();
        sb.append("4 0 obj\n<< /Length ").append(stream.length()).append(" >>\nstream\n").append(stream).append("endstream\nendobj\n");
        sb.append("5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n");
        sb.append("xref\n0 6\n0000000000 65535 f \n");
        sb.append("trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n0\n%%EOF\n");
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    }

    private static BigDecimal bd(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal) return (BigDecimal) o;
        if (o instanceof Number) return new BigDecimal(((Number) o).toString());
        try { return new BigDecimal(o.toString()); } catch (Exception e) { return BigDecimal.ZERO; }
    }

    private static String str(Object o) { return o == null ? null : o.toString(); }

    private static LocalDate toLocalDate(Object o) {
        if (o == null) return null;
        if (o instanceof LocalDate) return (LocalDate) o;
        if (o instanceof Date) return ((Date) o).toLocalDate();
        if (o instanceof java.sql.Timestamp) return ((java.sql.Timestamp) o).toLocalDateTime().toLocalDate();
        try { return LocalDate.parse(o.toString().substring(0, 10)); } catch (Exception e) { return null; }
    }
}
