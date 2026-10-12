package com.erp.business.inventory.interfaces.adapter;

import com.erp.business.inventory.interfaces.unit.UnitService;
import com.erp.business.inventory.shared.context.BusinessContextAccessor;
import com.erp.business.inventory.unit.dto.UnitResponse;
import com.erp.platform.common.dto.PageRequest;
import com.erp.platform.common.dto.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Adapter that implements the public {@link UnitService} interface from the
 * {@code business:inventory:interfaces} module by delegating to the internal
 * {@link com.erp.business.inventory.unit.service.UnitService} implementation.
 *
 * <p>This adapter bridges the gap between the public contract (which uses
 * request-scoped context for tenant/company) and the internal service (which
 * requires explicit tenant/company parameters).
 *
 * <p>The adapter reads the current tenant and company from the request context
 * via {@link BusinessContextAccessor}, ensuring that cross-module callers
 * (like Procurement) don't need to pass these explicitly.
 *
 * @since 1.0.0
 */
@Component
public class UnitServiceAdapter implements UnitService {

    private final com.erp.business.inventory.unit.service.UnitService internalUnitService;
    private final BusinessContextAccessor contextAccessor;

    public UnitServiceAdapter(com.erp.business.inventory.unit.service.UnitService internalUnitService, BusinessContextAccessor contextAccessor) {
        this.internalUnitService = internalUnitService;
        this.contextAccessor = contextAccessor;
    }

    @Override
    public Optional<UnitService.UnitResponse> findById(Long unitId) {
        Long tenantId = contextAccessor.getTenantId();
        Long companyId = contextAccessor.getCompanyId();
        try {
            com.erp.business.inventory.unit.dto.UnitResponse internal = internalUnitService.getUnitById(tenantId, unitId);
            return Optional.of(mapToInterface(internal));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<UnitService.UnitResponse> findByCode(String code) {
        Long tenantId = contextAccessor.getTenantId();
        Long companyId = contextAccessor.getCompanyId();
        try {
            com.erp.business.inventory.unit.dto.UnitResponse internal = internalUnitService.getUnitByCode(tenantId, companyId, code);
            return Optional.of(mapToInterface(internal));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<UnitService.UnitResponse> resolveUnit(String rawUomCode) {
        Long tenantId = contextAccessor.getTenantId();
        Long companyId = contextAccessor.getCompanyId();

        // Try exact code match first
        Optional<UnitService.UnitResponse> byCode = findByCode(rawUomCode);
        if (byCode.isPresent()) {
            return byCode;
        }

        // Try case-insensitive search
        if (rawUomCode != null && !rawUomCode.isBlank()) {
            String normalized = rawUomCode.trim().toUpperCase();
            Page<com.erp.business.inventory.unit.dto.UnitResponse> results = internalUnitService.searchUnits(tenantId, companyId, normalized, Pageable.unpaged());
            if (!results.isEmpty()) {
                return Optional.of(mapToInterface(results.getContent().get(0)));
            }
        }

        // Fallback: return first active unit
        List<com.erp.business.inventory.unit.dto.UnitResponse> activeUnits = internalUnitService.listActiveUnits(tenantId, companyId);
        return activeUnits.isEmpty() ? Optional.empty() : Optional.of(mapToInterface(activeUnits.get(0)));
    }

    @Override
    public boolean validateUnit(Long unitId) {
        return findById(unitId).map(u -> Boolean.TRUE.equals(u.getIsActive())).orElse(false);
    }

    @Override
    public boolean validateUnitCode(String code) {
        return findByCode(code).map(u -> Boolean.TRUE.equals(u.getIsActive())).orElse(false);
    }

    @Override
    public List<UnitService.UnitResponse> findAllActive() {
        Long tenantId = contextAccessor.getTenantId();
        Long companyId = contextAccessor.getCompanyId();
        return internalUnitService.listActiveUnits(tenantId, companyId).stream()
                .map(this::mapToInterface)
                .toList();
    }

    @Override
    public PageResponse<UnitService.UnitResponse> findAll(PageRequest pageRequest) {
        Long tenantId = contextAccessor.getTenantId();
        Long companyId = contextAccessor.getCompanyId();
        Pageable pageable = org.springframework.data.domain.PageRequest.of(
                pageRequest.page(), pageRequest.size());
        Page<com.erp.business.inventory.unit.dto.UnitResponse> page = internalUnitService.listUnits(tenantId, companyId, pageable);
        List<UnitService.UnitResponse> content = page.getContent().stream()
                .map(this::mapToInterface)
                .toList();
        PageResponse.Pagination pagination = PageResponse.Pagination.of(
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
        return PageResponse.of(content, pagination);
    }

    private UnitService.UnitResponse mapToInterface(com.erp.business.inventory.unit.dto.UnitResponse internal) {
        return new UnitService.UnitResponse() {
            @Override public Long getId() { return internal.id(); }
            @Override public String getCode() { return internal.code(); }
            @Override public String getName() { return internal.name(); }
            @Override public String getSymbol() { return internal.symbol(); }
            @Override public String getDimension() { return internal.dimension().name(); }
            @Override public Integer getBaseUnitId() { return null; }
            @Override public Integer getConversionFactor() { return internal.decimalScale(); }
            @Override public Boolean getIsActive() { return internal.isActive(); }
            @Override public Boolean getIsBaseUnit() { return null; }
        };
    }
}
