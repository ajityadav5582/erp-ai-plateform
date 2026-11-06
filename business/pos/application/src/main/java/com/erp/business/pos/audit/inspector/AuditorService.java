package com.erp.business.pos.audit.inspector;

import com.erp.business.pos.audit.FiscalAuditLog;
import com.erp.business.pos.audit.FiscalAuditLogRepository;
import com.erp.business.pos.transaction.infrastructure.persistence.PosTransactionRepository;
import com.erpai.integration.ird.outbox.entity.IrdSyncOutboxEntity;
import com.erpai.integration.ird.outbox.repository.IrdSyncOutboxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AuditorService {

    private final PosTransactionRepository transactionRepository;
    private final FiscalAuditLogRepository auditLogRepository;
    private final IrdSyncOutboxRepository cbmsRepository;

    public AuditorService(PosTransactionRepository transactionRepository,
                          FiscalAuditLogRepository auditLogRepository,
                          IrdSyncOutboxRepository cbmsRepository) {
        this.transactionRepository = transactionRepository;
        this.auditLogRepository = auditLogRepository;
        this.cbmsRepository = cbmsRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> bills(Long companyId, LocalDate from, LocalDate to) {
        List<Object[]> rows = transactionRepository.findSalesBook(companyId);
        List<Map<String, Object>> bills = new ArrayList<>();
        List<Long> gaps = new ArrayList<>();
        long expected = -1;
        boolean first = true;
        for (Object[] r : rows) {
            String billNumber = (String) r[0];
            long seq = extractSeq(billNumber);
            if (first) {
                expected = seq;
                first = false;
            } else {
                while (expected < seq) {
                    gaps.add(expected);
                    expected++;
                }
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("billNumber", billNumber);
            m.put("billDate", String.valueOf(r[1]));
            m.put("buyerName", r[2]);
            m.put("buyerPan", r[3]);
            m.put("total", toBd(r[4]));
            m.put("taxable", toBd(r[7]));
            m.put("vat", toBd(r[8]));
            m.put("status", String.valueOf(r[9]));
            m.put("gap", false);
            bills.add(m);
            expected = seq + 1;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("bills", bills);
        result.put("gaps", gaps);
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> voids(Long companyId, LocalDate from, LocalDate to) {
        List<Object[]> rows = transactionRepository.findCreditNotes(companyId);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object[] r : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("creditNoteNo", r[0]);
            m.put("billDate", String.valueOf(r[1]));
            m.put("originalInvoice", r[0]);
            m.put("buyerName", r[2]);
            m.put("buyerPan", r[3]);
            m.put("returnAmount", toBd(r[4]));
            m.put("taxable", toBd(r[5]));
            m.put("vat", toBd(r[6]));
            m.put("reason", r[7]);
            m.put("cancelledAt", String.valueOf(r[8]));
            out.add(m);
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> auditTrail(Long companyId, LocalDate from, LocalDate to) {
        OffsetDateTime f = from == null ? null : from.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime t = to == null ? null : to.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        List<FiscalAuditLog> logs = auditLogRepository.findByCompanyAndRange(companyId, f, t,
                org.springframework.data.domain.PageRequest.of(0, 500));
        List<Map<String, Object>> out = new ArrayList<>();
        for (FiscalAuditLog l : logs) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", l.getId());
            m.put("createdAt", l.getCreatedAt());
            m.put("userName", l.getUserName());
            m.put("actionType", l.getActionType());
            m.put("entityName", l.getEntityName());
            m.put("entityId", l.getEntityId());
            m.put("details", l.getDetailsJson());
            m.put("ipAddress", l.getIpAddress());
            out.add(m);
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> cbmsLogs(Long companyId, LocalDate from, LocalDate to) {
        List<IrdSyncOutboxEntity> entities = cbmsRepository.findByCompanyIdAndSyncStatusIn(companyId,
                List.of(com.erpai.integration.ird.outbox.entity.IrdSyncOutboxStatus.SYNCED,
                        com.erpai.integration.ird.outbox.entity.IrdSyncOutboxStatus.FAILED,
                        com.erpai.integration.ird.outbox.entity.IrdSyncOutboxStatus.DEAD_LETTER));
        List<Map<String, Object>> out = new ArrayList<>();
        for (IrdSyncOutboxEntity e : entities) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("invoiceNumber", e.getInvoiceNumber());
            m.put("responseCode", e.getLastResponseCode());
            m.put("responseBody", e.getLastErrorMessage());
            m.put("saleTime", e.getCreatedAt());
            m.put("syncTime", e.getLastAttemptedAt());
            m.put("status", e.getSyncStatus());
            out.add(m);
        }
        return out;
    }

    @Transactional(readOnly = true)
    public byte[] dossier(Long companyId, LocalDate from, LocalDate to) throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(baos);

        // Sales Book CSV
        java.util.zip.ZipEntry e1 = new java.util.zip.ZipEntry("sales-book.csv");
        zos.putNextEntry(e1);
        StringBuilder sb = new StringBuilder();
        sb.append("Bill No,Buyer,PAN,Total,Taxable,VAT,Status\n");
        Map<String, Object> billsResult = bills(companyId, from, to);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> bills = (List<Map<String, Object>>) billsResult.get("bills");
        for (Map<String, Object> b : bills) {
            sb.append(csv(b.get("billNumber"))).append(",")
               .append(csv(b.get("buyerName"))).append(",")
               .append(csv(b.get("buyerPan"))).append(",")
               .append(b.get("total")).append(",")
               .append(b.get("taxable")).append(",")
               .append(b.get("vat")).append(",")
               .append(b.get("status")).append("\n");
        }
        zos.write(sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        zos.closeEntry();

        // Credit Notes CSV
        java.util.zip.ZipEntry e2 = new java.util.zip.ZipEntry("credit-notes.csv");
        zos.putNextEntry(e2);
        StringBuilder sb2 = new StringBuilder();
        sb2.append("Credit Note No,Buyer,PAN,Return Amount,VAT,Reason\n");
        for (Map<String, Object> v : voids(companyId, from, to)) {
            sb2.append(csv(v.get("creditNoteNo"))).append(",")
               .append(csv(v.get("buyerName"))).append(",")
               .append(csv(v.get("buyerPan"))).append(",")
               .append(v.get("returnAmount")).append(",")
               .append(v.get("vat")).append(",")
               .append(csv(v.get("reason"))).append("\n");
        }
        zos.write(sb2.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        zos.closeEntry();

        // Audit Log CSV
        java.util.zip.ZipEntry e3 = new java.util.zip.ZipEntry("audit-log.csv");
        zos.putNextEntry(e3);
        StringBuilder sb3 = new StringBuilder();
        sb3.append("Time,User,Action,Entity,Entity ID,Details,IP\n");
        for (Map<String, Object> a : auditTrail(companyId, from, to)) {
            sb3.append(csv(a.get("createdAt"))).append(",")
               .append(csv(a.get("userName"))).append(",")
               .append(csv(a.get("actionType"))).append(",")
               .append(csv(a.get("entityName"))).append(",")
               .append(csv(a.get("entityId"))).append(",")
               .append(csv(a.get("details"))).append(",")
               .append(csv(a.get("ipAddress"))).append("\n");
        }
        zos.write(sb3.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        zos.closeEntry();

        zos.close();
        return baos.toByteArray();
    }

    private long extractSeq(String billNumber) {
        if (billNumber == null) return 0;
        String digits = billNumber.replaceAll(".*?(\\d+)$", "$1");
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private BigDecimal toBd(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal) return (BigDecimal) o;
        return new BigDecimal(o.toString());
    }

    private String csv(Object o) {
        if (o == null) return "";
        String s = o.toString();
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
