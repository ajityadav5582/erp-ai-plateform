package com.erp.business.pos.table;

import com.erp.business.pos.domain.PosTable;
import com.erp.business.pos.table.dto.PosTableResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PosTableServiceImpl implements PosTableService {

    private final PosTableRepository repository;

    public PosTableServiceImpl(PosTableRepository repository) {
        this.repository = repository;
    }

    @Override
    public PosTableResponse createTable(Long tenantId, Long companyId, String tableName, Long branchId) {
        if (repository.existsByTenantIdAndCompanyIdAndTableName(tenantId, companyId, tableName.trim())) {
            throw new IllegalArgumentException("Table already exists: " + tableName);
        }
        PosTable table = PosTable.create(tableName, branchId);
        table.assignScope(tenantId, companyId);
        return toResponse(repository.save(table));
    }

    @Override
    @Transactional(readOnly = true)
    public PosTableResponse getTableById(Long tenantId, Long companyId, Long id) {
        PosTable table = repository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Table not found: " + id));
        return toResponse(table);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PosTableResponse> listTables(Long tenantId, Long companyId, Pageable pageable) {
        return repository.findByTenantIdAndCompanyId(tenantId, companyId, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PosTableResponse> listAvailableTables(Long tenantId, Long companyId) {
        return repository.findByTenantIdAndCompanyId(tenantId, companyId).stream()
                .filter(t -> "AVAILABLE".equals(t.getStatus()))
                .map(this::toResponse)
                .toList();
    }

    @Override
    public PosTableResponse markOccupied(Long tenantId, Long companyId, Long id) {
        PosTable table = repository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Table not found: " + id));
        table.markOccupied();
        return toResponse(repository.save(table));
    }

    @Override
    public PosTableResponse markAvailable(Long tenantId, Long companyId, Long id) {
        PosTable table = repository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Table not found: " + id));
        table.markAvailable();
        return toResponse(repository.save(table));
    }

    private PosTableResponse toResponse(PosTable table) {
        return new PosTableResponse(table.getId(), table.getTableName(), table.getBranchId(), table.getStatus());
    }
}
