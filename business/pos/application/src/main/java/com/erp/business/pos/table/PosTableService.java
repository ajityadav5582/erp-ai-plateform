package com.erp.business.pos.table;

import com.erp.business.pos.table.dto.PosTableResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PosTableService {
    PosTableResponse createTable(Long tenantId, Long companyId, String tableName, Long branchId);
    PosTableResponse getTableById(Long tenantId, Long companyId, Long id);
    Page<PosTableResponse> listTables(Long tenantId, Long companyId, Pageable pageable);
    List<PosTableResponse> listAvailableTables(Long tenantId, Long companyId);
    PosTableResponse markOccupied(Long tenantId, Long companyId, Long id);
    PosTableResponse markAvailable(Long tenantId, Long companyId, Long id);
}
