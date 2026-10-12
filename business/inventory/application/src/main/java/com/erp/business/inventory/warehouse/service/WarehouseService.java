package com.erp.business.inventory.warehouse.service;

import com.erp.business.inventory.warehouse.dto.CreateWarehouseRequest;
import com.erp.business.inventory.warehouse.dto.UpdateWarehouseRequest;
import com.erp.business.inventory.warehouse.dto.WarehouseResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Manages warehouses and guarantees every company has a default one.
 *
 * <p>Every operation is scoped to both the tenant and the active company. A tenant may
 * own several companies, each with its own warehouse network, so a warehouse belonging
 * to another company of the same tenant must never be visible or writable.
 */
public interface WarehouseService {

    /**
     * Adds another warehouse to the company.
     *
     * <p>Never creates a default: adding a site is not the same decision as choosing
     * where goods with no stated destination land. Use
     * {@link #setDefaultWarehouse(Long, Long, Long)} for that.
     *
     * @throws com.erp.business.inventory.warehouse.exception.DuplicateWarehouseCodeException
     *         if the code is taken in this company
     * @throws com.erp.business.inventory.warehouse.exception.DuplicateWarehouseNameException
     *         if the name is taken in this company
     */
    WarehouseResponse createWarehouse(Long tenantId, Long companyId,
                                      CreateWarehouseRequest request);

    /**
     * Returns the company's default warehouse, creating one if it has none.
     *
     * <p>This is the entry point the goods receipt flow should use: it never fails
     * because a new company has not finished configuring its warehouses. Calling it
     * twice for the same company returns the same row.
     *
     * <p><strong>Why it resolves on demand rather than firing on company creation.</strong>
     * {@code Company} is mastered by the identity service, so a company is created in a
     * different service and a different database transaction. Creating the warehouse
     * synchronously from there would be a distributed transaction that can
     * half-complete; publishing an event instead makes it eventually consistent, and an
     * event lost during a broker outage leaves the company with no default at all.
     * Resolving on demand is correct on the first call regardless of whether any event
     * ever arrived.
     *
     * @param branchId the branch to hang the new warehouse under, or null
     */
    WarehouseResponse getOrCreateDefault(Long tenantId, Long companyId, Long branchId);

    /**
     * Promotes a warehouse to be the company default, demoting the previous one.
     *
     * @throws com.erp.business.inventory.warehouse.exception.WarehouseNotFoundException
     *         if the warehouse is not in this company
     * @throws IllegalStateException if the warehouse is inactive and so cannot receive
     *         goods
     */
    WarehouseResponse setDefaultWarehouse(Long tenantId, Long companyId, Long id);

    WarehouseResponse getWarehouseById(Long tenantId, Long companyId, Long id);

    WarehouseResponse getWarehouseByCode(Long tenantId, Long companyId, String code);

    Page<WarehouseResponse> listWarehouses(Long tenantId, Long companyId, Pageable pageable);

    /** Active warehouses only, ordered by code. This is what pickers should offer. */
    List<WarehouseResponse> listActiveWarehouses(Long tenantId, Long companyId);

    List<WarehouseResponse> listWarehousesByBranch(Long tenantId, Long companyId, Long branchId);

    WarehouseResponse updateWarehouse(Long tenantId, Long companyId, Long id,
                                      UpdateWarehouseRequest request);

    WarehouseResponse activateWarehouse(Long tenantId, Long companyId, Long id);

    /**
     * Deactivates a warehouse.
     *
     * @throws IllegalStateException if it is the company default, which has to be
     *         replaced before it can be switched off
     */
    WarehouseResponse deactivateWarehouse(Long tenantId, Long companyId, Long id);
}
