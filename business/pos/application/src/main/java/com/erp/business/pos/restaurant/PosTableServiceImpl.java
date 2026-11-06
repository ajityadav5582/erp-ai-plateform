package com.erp.business.pos.restaurant;

import com.erp.business.pos.restaurant.domain.PosTable;
import com.erp.business.pos.restaurant.domain.TableSession;
import com.erp.business.pos.restaurant.infrastructure.persistence.PosTableRepository;
import com.erp.business.pos.restaurant.infrastructure.persistence.TableSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PosTableServiceImpl implements PosTableService {

    private final PosTableRepository tableRepository;
    private final TableSessionRepository sessionRepository;

    public PosTableServiceImpl(PosTableRepository tableRepository, TableSessionRepository sessionRepository) {
        this.tableRepository = tableRepository;
        this.sessionRepository = sessionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PosTable> listTables(Long tenantId, Long companyId) {
        return tableRepository.findByTenantIdAndCompanyId(tenantId, companyId);
    }

    @Override
    public PosTable occupy(Long tenantId, Long companyId, Long tableId) {
        PosTable table = tableRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, tableId)
                .orElseThrow(() -> new IllegalArgumentException("Table not found: " + tableId));
        if (table.getStatus() != PosTable.Status.FREE) {
            throw new IllegalStateException("Table " + table.getTableNumber() + " is not free");
        }
        TableSession session = TableSession.builder().tableId(tableId).build();
        session.assignScope(tenantId, companyId);
        TableSession saved = sessionRepository.save(session);
        table.occupy(saved.getId());
        return tableRepository.save(table);
    }

    @Override
    public void free(Long tenantId, Long companyId, Long tableId) {
        PosTable table = tableRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, tableId)
                .orElseThrow(() -> new IllegalArgumentException("Table not found: " + tableId));
        table.free();
        tableRepository.save(table);
    }

    @Override
    public TableSession openSession(Long tenantId, Long companyId, Long tableId) {
        PosTable table = tableRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, tableId)
                .orElseThrow(() -> new IllegalArgumentException("Table not found: " + tableId));
        TableSession session = TableSession.builder().tableId(tableId).build();
        session.assignScope(tenantId, companyId);
        TableSession saved = sessionRepository.save(session);
        table.occupy(saved.getId());
        tableRepository.save(table);
        return saved;
    }

    @Override
    public PosTable markOrdered(Long tenantId, Long companyId, Long tableId) {
        PosTable table = tableRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, tableId)
                .orElseThrow(() -> new IllegalArgumentException("Table not found: " + tableId));
        table.markOrdered();
        return tableRepository.save(table);
    }

    @Override
    public PosTable markCleaning(Long tenantId, Long companyId, Long tableId) {
        PosTable table = tableRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, tableId)
                .orElseThrow(() -> new IllegalArgumentException("Table not found: " + tableId));
        table.markCleaning();
        return tableRepository.save(table);
    }
}
