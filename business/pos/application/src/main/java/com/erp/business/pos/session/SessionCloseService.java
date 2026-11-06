package com.erp.business.pos.session;

import com.erp.business.pos.domain.PosSession;
import com.erp.business.pos.session.infrastructure.persistence.PosSessionRepository;
import com.erp.business.pos.transaction.infrastructure.persistence.PosTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class SessionCloseService {

    private final PosSessionRepository sessionRepository;
    private final PosTransactionRepository transactionRepository;

    public SessionCloseService(PosSessionRepository sessionRepository, PosTransactionRepository transactionRepository) {
        this.sessionRepository = sessionRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public SessionCloseResponse close(Long sessionId, SessionCloseRequest req) {
        PosSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        BigDecimal closingCash = BigDecimal.valueOf(1000L * req.note1000() + 500L * req.note500() + 100L * req.note100()
                + 50L * req.note50() + 20L * req.note20() + 10L * req.note10() + 5L * req.note5() + req.coins());

        Object[] agg = transactionRepository.aggregateBySession(sessionId);
        BigDecimal grossSales = toBd(agg[0]);
        BigDecimal taxableSales = toBd(agg[1]);
        BigDecimal vatTotal = toBd(agg[2]);
        BigDecimal cashSales = toBd(agg[3]);
        String startBill = (String) agg[6];
        String endBill = (String) agg[7];
        int count = ((Number) agg[8]).intValue();

        BigDecimal expectedCash = session.getOpeningCashAmount().add(cashSales);
        BigDecimal variance = closingCash.subtract(expectedCash);

        session.setClosingCashCounted(closingCash);
        session.setExpectedCash(expectedCash);
        session.setCashVariance(variance);
        session.setGrossSales(grossSales);
        session.setTaxableSales(taxableSales);
        session.setVatTotal(vatTotal);
        session.setStartBillNumber(startBill);
        session.setEndBillNumber(endBill);
        session.setTotalBillCount(count);
        session.setClosedAt(Instant.now());
        session.setStatus(com.erp.business.pos.domain.PosSessionStatus.CLOSED);
        session.setZReportNumber("Z-" + sessionId);
        sessionRepository.save(session);

        return new SessionCloseResponse(closingCash, expectedCash, variance, grossSales, taxableSales, vatTotal,
                startBill, endBill, count, session.getZReportNumber());
    }

    private BigDecimal toBd(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal) return (BigDecimal) o;
        return new BigDecimal(o.toString());
    }
}
