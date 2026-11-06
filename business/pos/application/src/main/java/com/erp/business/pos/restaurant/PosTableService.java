package com.erp.business.pos.restaurant;

import com.erp.business.pos.restaurant.domain.PosTable;
import com.erp.business.pos.restaurant.domain.TableSession;

import java.util.List;

/**
 * Application service for restaurant table management.
 * Guarded by PosCapabilityGuard (restaurant family) at call sites.
 */
public interface PosTableService {
    List<PosTable> listTables(Long tenantId, Long companyId);
    PosTable occupy(Long tenantId, Long companyId, Long tableId);
    void free(Long tenantId, Long companyId, Long tableId);
    TableSession openSession(Long tenantId, Long companyId, Long tableId);
    PosTable markOrdered(Long tenantId, Long companyId, Long tableId);
    PosTable markCleaning(Long tenantId, Long companyId, Long tableId);
}
