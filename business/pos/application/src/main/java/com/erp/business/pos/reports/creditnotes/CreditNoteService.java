package com.erp.business.pos.reports.creditnotes;

import com.erp.business.pos.transaction.infrastructure.persistence.PosTransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CreditNoteService {

    private final PosTransactionRepository repository;

    public CreditNoteService(PosTransactionRepository repository) {
        this.repository = repository;
    }

    public List<CreditNoteRow> fetch(Long companyId) {
        return repository.findCreditNotes(companyId).stream().map(r -> new CreditNoteRow(
                String.valueOf(r[1]),
                (String) r[0],
                (String) r[0], // placeholder original invoice
                (String) r[2],
                (String) r[3],
                toBd(r[4]),
                toBd(r[5]),
                toBd(r[6]),
                (String) r[7],
                String.valueOf(r[8])
        )).toList();
    }

    private BigDecimal toBd(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal) return (BigDecimal) o;
        return new BigDecimal(o.toString());
    }
}
