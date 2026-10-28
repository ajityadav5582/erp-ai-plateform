package com.erp.business.inventory.interfaces.unit;

import com.erp.platform.common.dto.PageRequest;
import com.erp.platform.common.dto.PageResponse;
import com.erp.platform.common.dto.ResponseWrapper;

import java.util.List;
import java.util.Optional;

/**
 * Application interface for Unit of Measure operations.
 *
 * <p>This interface defines the contract for unit management that other modules
 * (e.g., Procurement) can depend on without coupling to Inventory's internal
 * implementation, JPA entities, or repositories.
 *
 * <p>Implementations live in the Inventory application module. Consumers should
 * depend on this interface (from {@code business:inventory:interfaces}) and
 * receive an implementation via dependency injection.
 *
 * @since 1.0.0
 */
public interface UnitService {

    /**
     * Returns a unit by its ID, scoped to the current tenant and company.
     *
     * @param unitId the unit identifier
     * @return the unit DTO if found and accessible
     */
    Optional<UnitResponse> findById(Long unitId);

    /**
     * Returns a unit by its code, scoped to the current tenant and company.
     *
     * @param code the unit code (e.g., "PCS", "KG", "M")
     * @return the unit DTO if found and accessible
     */
    Optional<UnitResponse> findByCode(String code);

    /**
     * Resolves a unit flexibly by code, alias, or name.
     *
     * <p>This is the primary method for external modules (like Procurement) to
     * resolve a unit from a user-provided UOM string. It applies the same
     * alias mapping and fallback logic as the internal resolver.
     *
     * @param rawUomCode the input UOM string (e.g., "PIECES", "PCS", "KG")
     * @return the resolved unit DTO, or empty if no unit can be resolved
     */
    Optional<UnitResponse> resolveUnit(String rawUomCode);

    /**
     * Validates that a unit exists, is active, and belongs to the current scope.
     *
     * @param unitId the unit identifier to validate
     * @return true if valid and accessible
     */
    boolean validateUnit(Long unitId);

    /**
     * Validates that a unit code exists, is active, and belongs to the current scope.
     *
     * @param code the unit code to validate
     * @return true if valid and accessible
     */
    boolean validateUnitCode(String code);

    /**
     * Returns all active units for the current tenant and company.
     *
     * @return list of active unit DTOs
     */
    List<UnitResponse> findAllActive();

    /**
     * Returns all units for the current tenant and company with pagination.
     *
     * @param pageRequest the pagination request
     * @return paginated response of unit DTOs
     */
    PageResponse<UnitResponse> findAll(PageRequest pageRequest);

    /**
     * Data transfer object for Unit information exposed to other modules.
     *
     * <p>This DTO contains only the data needed by consumers. It does not
     * expose JPA entities, lazy-loading proxies, or internal state.
     */
    interface UnitResponse {
        Long getId();
        String getCode();
        String getName();
        String getSymbol();
        String getDimension(); // e.g., "WEIGHT", "LENGTH", "VOLUME", "COUNT", "TIME"
        Integer getBaseUnitId();
        Integer getConversionFactor();
        Boolean getIsActive();
        Boolean getIsBaseUnit();
    }

    /**
     * Request DTO for creating a unit (used by Inventory's own interfaces).
     */
    interface UnitCreateRequest {
        String getCode();
        String getName();
        String getSymbol();
        String getDimension();
        Integer getBaseUnitId();
        Integer getConversionFactor();
        Boolean getIsActive();
    }

    /**
     * Request DTO for updating a unit (used by Inventory's own interfaces).
     */
    interface UnitUpdateRequest {
        String getName();
        String getSymbol();
        String getDimension();
        Integer getBaseUnitId();
        Integer getConversionFactor();
        Boolean getIsActive();
    }
}
