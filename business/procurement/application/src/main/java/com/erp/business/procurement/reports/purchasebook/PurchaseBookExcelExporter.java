package com.erp.business.procurement.reports.purchasebook;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;

public final class PurchaseBookExcelExporter {

    private PurchaseBookExcelExporter() {
    }

    public static byte[] export(List<PurchaseBookRow> rows, PurchaseBookTotals totals) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Purchase Book (Annex 7)");
            int r = 0;
            Row title = sheet.createRow(r++);
            title.createCell(0).setCellValue("Government of Nepal - Inland Revenue Department");
            Row sub = sheet.createRow(r++);
            sub.createCell(0).setCellValue("VAT Purchase Book (खरिद खाता) - Annex 7");
            r++;

            String[] headers = {"Miti (BS)", "Date (AD)", "Invoice No", "Custom Decl.", "Supplier Name",
                    "Supplier PAN", "Total Purchase", "Exempt", "Taxable", "Input VAT 13%",
                    "Import Taxable", "Import VAT", "Capital Asset"};
            Row headerRow = sheet.createRow(r++);
            for (int i = 0; i < headers.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(headers[i]);
                CellStyle st = wb.createCellStyle();
                Font f = wb.createFont();
                f.setBold(true);
                st.setFont(f);
                c.setCellStyle(st);
            }

            for (PurchaseBookRow row : rows) {
                Row rr = sheet.createRow(r++);
                rr.createCell(0).setCellValue(row.mitiBs());
                rr.createCell(1).setCellValue(row.dateAd() == null ? "" : row.dateAd().toString());
                rr.createCell(2).setCellValue(row.invoiceNumber());
                rr.createCell(3).setCellValue(row.customDeclarationNumber() == null ? "" : row.customDeclarationNumber());
                rr.createCell(4).setCellValue(row.supplierName());
                rr.createCell(5).setCellValue(row.supplierPan() == null ? "" : row.supplierPan());
                setNum(rr, 6, row.totalPurchaseAmount());
                setNum(rr, 7, row.exemptPurchaseAmount());
                setNum(rr, 8, row.taxablePurchaseAmount());
                setNum(rr, 9, row.inputVatAmount());
                setNum(rr, 10, row.importTaxableAmount());
                setNum(rr, 11, row.importVatAmount());
                rr.createCell(12).setCellValue(row.capitalAsset() ? "Capital Asset" : "Raw/Trading");
            }

            Row tot = sheet.createRow(r++);
            tot.createCell(4).setCellValue("TOTAL");
            setNum(tot, 6, totals.totalPurchaseAmount());
            setNum(tot, 7, totals.exemptPurchaseAmount());
            setNum(tot, 8, totals.taxablePurchaseAmount());
            setNum(tot, 9, totals.inputVatAmount());
            setNum(tot, 10, totals.importTaxableAmount());
            setNum(tot, 11, totals.importVatAmount());

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Excel", e);
        }
    }

    private static void setNum(Row row, int idx, BigDecimal v) {
        Cell c = row.createCell(idx);
        c.setCellValue(v == null ? 0.0 : v.doubleValue());
    }
}
