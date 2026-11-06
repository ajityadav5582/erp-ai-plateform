package com.erp.business.pos.reports.salesbook;

import com.erp.business.pos.transaction.infrastructure.persistence.PosTransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class SalesBookService {

    private final PosTransactionRepository repository;

    public SalesBookService(PosTransactionRepository repository) {
        this.repository = repository;
    }

    public List<SalesBookRow> fetch(Long companyId) {
        return repository.findSalesBook(companyId).stream().map(r -> new SalesBookRow(
                String.valueOf(r[1]),
                String.valueOf(r[1]),
                (String) r[0],
                (String) r[2],
                (String) r[3],
                toBd(r[4]),
                toBd(r[5]),
                toBd(r[6]),
                toBd(r[7]),
                toBd(r[8]),
                String.valueOf(r[9])
        )).toList();
    }

    private BigDecimal toBd(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal) return (BigDecimal) o;
        return new BigDecimal(o.toString());
    }
}
