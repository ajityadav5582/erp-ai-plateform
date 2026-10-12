package com.erp.business.inventory.interfaces.adapter;

import com.erp.business.inventory.interfaces.warehouse.WarehouseService;
import com.erp.business.inventory.warehouse.dto.WarehouseResponse;
import com.erp.business.inventory.shared.context.BusinessContextAccessor;
import com.erp.platform.common.dto.PageRequest;
import com.erp.platform.common.dto.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Adapter that implements the public {@link WarehouseService} interface from the
 * {@code business:inventory:interfaces} module by delegating to the internal
 * {@link com.erp.business.inventory.warehouse.service.WarehouseService} implementation.
 *
 * @since 1.0.0
 */
@Component
public class WarehouseServiceAdapter implements WarehouseService {

    private final com.erp.business.inventory.warehouse.service.WarehouseService internalWarehouseService;
    private final BusinessContextAccessor contextAccessor;

    public WarehouseServiceAdapter(com.erp.business.inventory.warehouse.service.WarehouseService internalWarehouseService, BusinessContextAccessor contextAccessor) {
        this.internalWarehouseService = internalWarehouseService;
        this.contextAccessor = contextAccessor;
    }

    @Override
    public Optional<WarehouseService.WarehouseResponse> findById(Long warehouseId) {
        Long tenantId = contextAccessor.getTenantId();
        Long companyId = contextAccessor.getCompanyId();
        try {
            com.erp.business.inventory.warehouse.dto.WarehouseResponse internal = internalWarehouseService.getWarehouseById(tenantId, companyId, warehouseId);
            return Optional.of(mapToInterface(internal));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<WarehouseService.WarehouseResponse> findByCode(String code) {
        Long tenantId = contextAccessor.getTenantId();
        Long companyId = contextAccessor.getCompanyId();
        try {
            com.erp.business.inventory.warehouse.dto.WarehouseResponse internal = internalWarehouseService.getWarehouseByCode(tenantId, companyId, code);
            return Optional.of(mapToInterface(internal));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean validateWarehouse(Long warehouseId) {
        return findById(warehouseId).map(u -> Boolean.TRUE.equals(u.getIsActive())).orElse(false);
    }

    @Override
    public boolean validateWarehouseCode(String code) {
        return findByCode(code).map(u -> Boolean.TRUE.equals(u.getIsActive())).orElse(false);
    }

    @Override
    public List<WarehouseService.WarehouseResponse> findAllActive() {
        Long tenantId = contextAccessor.getTenantId();
        Long companyId = contextAccessor.getCompanyId();
        return internalWarehouseService.listActiveWarehouses(tenantId, companyId).stream()
                .map(this::mapToInterface)
                .toList();
    }

    @Override
    public PageResponse<WarehouseService.WarehouseResponse> findAll(PageRequest pageRequest) {
        Long tenantId = contextAccessor.getTenantId();
        Long companyId = contextAccessor.getCompanyId();
        Pageable pageable = org.springframework.data.domain.PageRequest.of(
                pageRequest.page(), pageRequest.size());
        Page<com.erp.business.inventory.warehouse.dto.WarehouseResponse> page = internalWarehouseService.listWarehouses(tenantId, companyId, pageable);
        List<WarehouseService.WarehouseResponse> content = page.getContent().stream()
                .map(this::mapToInterface)
                .toList();
        PageResponse.Pagination pagination = PageResponse.Pagination.of(
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
        return PageResponse.of(content, pagination);
    }

    private WarehouseService.WarehouseResponse mapToInterface(com.erp.business.inventory.warehouse.dto.WarehouseResponse internal) {
        return new WarehouseService.WarehouseResponse() {
            @Override public Long getId() { return internal.id(); }
            @Override public String getCode() { return internal.code(); }
            @Override public String getName() { return internal.name(); }
            @Override public String getDescription() { return internal.address(); }
            @Override public String getAddress() { return internal.address(); }
            @Override public String getWarehouseType() { return internal.type().name(); }
            @Override public Boolean getIsActive() { return internal.isActive(); }
            @Override public Boolean getIsDefault() { return internal.isDefault(); }
        };
    }
}
