package com.erp.business.inventory.interfaces.warehouse;

import com.erp.platform.common.dto.PageRequest;
import com.erp.platform.common.dto.PageResponse;

import java.util.List;
import java.util.Optional;

/**
 * Application interface for Warehouse operations.
 *
 * <p>This interface defines the contract for warehouse management that other modules
 * (e.g., Procurement) can depend on without coupling to Inventory's internal
 * implementation, JPA entities, or repositories.
 *
 * @since 1.0.0
 */
public interface WarehouseService {

    /**
     * Returns a warehouse by its ID, scoped to the current tenant and company.
     *
     * @param warehouseId the warehouse identifier
     * @return the warehouse DTO if found and accessible
     */
    Optional<WarehouseResponse> findById(Long warehouseId);

    /**
     * Returns a warehouse by its code, scoped to the current tenant and company.
     *
     * @param code the warehouse code
     * @return the warehouse DTO if found and accessible
     */
    Optional<WarehouseResponse> findByCode(String code);

    /**
     * Validates that a warehouse exists, is active, and belongs to the current scope.
     *
     * @param warehouseId the warehouse identifier to validate
     * @return true if valid and accessible
     */
    boolean validateWarehouse(Long warehouseId);

    /**
     * Validates that a warehouse code exists, is active, and belongs to the current scope.
     *
     * @param code the warehouse code to validate
     * @return true if valid and accessible
     */
    boolean validateWarehouseCode(String code);

    /**
     * Returns all active warehouses for the current tenant and company.
     *
     * @return list of active warehouse DTOs
     */
    List<WarehouseResponse> findAllActive();

    /**
     * Returns all warehouses for the current tenant and company with pagination.
     *
     * @param pageRequest the pagination request
     * @return paginated response of warehouse DTOs
     */
    PageResponse<WarehouseResponse> findAll(PageRequest pageRequest);

    /**
     * Data transfer object for Warehouse information exposed to other modules.
     */
    interface WarehouseResponse {
        Long getId();
        String getCode();
        String getName();
        String getDescription();
        String getAddress();
        String getWarehouseType(); // MAIN, BRANCH, VIRTUAL, etc.
        Boolean getIsActive();
        Boolean getIsDefault();
    }

    /**
     * Request DTO for creating a warehouse (used by Inventory's own interfaces).
     */
    interface WarehouseCreateRequest {
        String getCode();
        String getName();
        String getDescription();
        String getAddress();
        String getWarehouseType();
        Boolean getIsActive();
        Boolean getIsDefault();
    }

    /**
     * Request DTO for updating a warehouse (used by Inventory's own interfaces).
     */
    interface WarehouseUpdateRequest {
        String getName();
        String getDescription();
        String getAddress();
        String getWarehouseType();
        Boolean getIsActive();
        Boolean getIsDefault();
    }
}
