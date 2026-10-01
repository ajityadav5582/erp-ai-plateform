package com.erp.business.inventory.unit.service;

import com.erp.business.inventory.unit.dto.UnitCreateRequest;
import com.erp.business.inventory.unit.dto.UnitResponse;
import com.erp.business.inventory.unit.dto.UnitUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Application service for units of measure.
 *
 * <p>Every method takes the tenant and, where relevant, the company explicitly
 * rather than reading them from a thread-local. That makes the scope a visible,
 * testable parameter of every call instead of hidden state, and it means a
 * service method can never accidentally run unscoped.
 */
public interface UnitService {

    UnitResponse createUnit(Long tenantId, Long companyId, UnitCreateRequest request);

    UnitResponse getUnitById(Long tenantId, Long id);

    UnitResponse getUnitByCode(Long tenantId, Long companyId, String code);

    Page<UnitResponse> listUnits(Long tenantId, Long companyId, Pageable pageable);

    /**
     * Lists units matching a free-text term on either name or code.
     *
     * @param term the search term; blank is treated as "no filter"
     */
    Page<UnitResponse> searchUnits(Long tenantId, Long companyId, String term, Pageable pageable);

    /** Returns only the active units, unpaginated. Intended for picker dropdowns. */
    List<UnitResponse> listActiveUnits(Long tenantId, Long companyId);

    UnitResponse updateUnit(Long tenantId, Long id, UnitUpdateRequest request);

    UnitResponse activateUnit(Long tenantId, Long id);

    UnitResponse deactivateUnit(Long tenantId, Long id);

    void deleteUnit(Long tenantId, Long id);
}
