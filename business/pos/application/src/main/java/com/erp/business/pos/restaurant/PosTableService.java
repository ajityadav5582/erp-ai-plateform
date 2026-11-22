package com.erp.business.pos.restaurant;

import com.erp.business.pos.restaurant.domain.PosTable;
import com.erp.business.pos.restaurant.domain.TableSession;
import com.erp.business.pos.restaurant.dto.CreatePosTableRequest;
import com.erp.business.pos.restaurant.dto.TableBillSummaryResponse;
import com.erp.business.pos.restaurant.dto.UpdatePosTableRequest;

import java.util.List;

/**
 * Application service for restaurant table management and KOT bill lifecycle.
 */
public interface PosTableService {
    List<PosTable> listTables(Long tenantId, Long companyId);
    PosTable occupy(Long tenantId, Long companyId, Long tableId);
    void free(Long tenantId, Long companyId, Long tableId);
    TableSession openSession(Long tenantId, Long companyId, Long tableId);
    PosTable markOrdered(Long tenantId, Long companyId, Long tableId);
    PosTable markCleaning(Long tenantId, Long companyId, Long tableId);

    PosTable createTable(Long tenantId, Long companyId, CreatePosTableRequest request);
    PosTable updateTable(Long tenantId, Long companyId, Long tableId, UpdatePosTableRequest request);
    void deleteTable(Long tenantId, Long companyId, Long tableId);
    List<PosTable> bootstrapDefaultTables(Long tenantId, Long companyId);

    TableBillSummaryResponse getTableCurrentBill(Long tenantId, Long companyId, Long tableId);
    void transferTable(Long tenantId, Long companyId, Long fromTableId, Long toTableId);
    void mergeTables(Long tenantId, Long companyId, Long fromTableId, Long intoTableId);
    void closeTableSessionOnCheckout(Long tenantId, Long companyId, Long tableId, Long posTransactionId);
    void closeTakeawayOrderOnCheckout(Long tenantId, Long companyId, String tokenNumber, Long posTransactionId);
}
