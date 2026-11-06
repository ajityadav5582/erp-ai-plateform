package com.erp.business.inventory.interfaces.adapter;

import com.erp.business.inventory.interfaces.supplier.SupplierService;
import com.erp.business.inventory.supplier.dto.SupplierResponse;
import com.erp.business.inventory.supplier.service.SupplierServiceImpl;
import com.erp.platform.business.context.BusinessContextAccessor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Adapter that implements the public {@link SupplierService} interface from the
 * {@code business:inventory:interfaces} module by delegating to the internal
 * {@link SupplierServiceImpl}.
 */
@Component
public class SupplierServiceAdapter implements SupplierService {

    private final SupplierServiceImpl internalSupplierService;
    private final BusinessContextAccessor contextAccessor;

    public SupplierServiceAdapter(SupplierServiceImpl internalSupplierService,
                                 BusinessContextAccessor contextAccessor) {
        this.internalSupplierService = internalSupplierService;
        this.contextAccessor = contextAccessor;
    }

    @Override
    public Optional<SupplierResponse> findById(Long supplierId) {
        Long tenantId = contextAccessor.getTenantId();
        try {
            com.erp.business.inventory.supplier.dto.SupplierResponse internal =
                    internalSupplierService.getSupplierById(tenantId, supplierId);
            return Optional.of(mapToInterface(internal));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<SupplierResponse> findByCode(String code) {
        Long tenantId = contextAccessor.getTenantId();
        Long companyId = contextAccessor.getCompanyId();
        try {
            com.erp.business.inventory.supplier.dto.SupplierResponse internal =
                    internalSupplierService.getSupplierByCode(tenantId, companyId, code);
            return Optional.of(mapToInterface(internal));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean validateSupplier(Long supplierId) {
        return findById(supplierId).isPresent();
    }

    @Override
    public boolean validateSupplierCode(String code) {
        return findByCode(code).isPresent();
    }

    @Override
    public java.util.List<SupplierResponse> findAllActive() {
        Long tenantId = contextAccessor.getTenantId();
        Long companyId = contextAccessor.getCompanyId();
        return internalSupplierService.listSuppliers(tenantId, companyId,
                org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE))
                .getContent().stream()
                .map(this::mapToInterface)
                .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                .toList();
    }

    @Override
    public com.erp.platform.common.dto.PageResponse<SupplierResponse> findAll(
            com.erp.platform.common.dto.PageRequest pageRequest) {
        Long tenantId = contextAccessor.getTenantId();
        Long companyId = contextAccessor.getCompanyId();
        org.springframework.data.domain.Page<com.erp.business.inventory.supplier.dto.SupplierResponse> page =
                internalSupplierService.listSuppliers(tenantId, companyId,
                        org.springframework.data.domain.PageRequest.of(
                                pageRequest.page(), pageRequest.size()));
        java.util.List<SupplierResponse> content = page.getContent().stream()
                .map(this::mapToInterface).toList();
        boolean first = page.isFirst();
        boolean last = page.isLast();
        com.erp.platform.common.dto.PageResponse.Pagination pagination =
                new com.erp.platform.common.dto.PageResponse.Pagination(
                        page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), first, last);
        return com.erp.platform.common.dto.PageResponse.of(content, pagination);
    }

    private SupplierResponse mapToInterface(com.erp.business.inventory.supplier.dto.SupplierResponse internal) {
        return new SupplierResponse() {
            public Long getId() { return internal.id(); }
            public String getCode() { return internal.code(); }
            public String getName() { return internal.name(); }
            public String getSupplierType() { return internal.type() != null ? internal.type().name() : null; }
            public String getContactPerson() { return null; }
            public String getEmail() { return internal.email(); }
            public String getPhone() { return internal.phone(); }
            public String getAddress() { return internal.address(); }
            public String getTaxNumber() { return internal.taxId(); }
            public Boolean getIsActive() { return internal.isActive(); }
            public String getVatRegistrationStatus() {
                return internal.vatRegistrationStatus() != null ? internal.vatRegistrationStatus().name() : null;
            }
            public String getVatRegistrationNumber() { return internal.vatRegistrationNumber(); }
            public java.time.LocalDate getVatEffectiveFrom() { return internal.vatEffectiveFrom(); }
            public java.time.LocalDate getVatEffectiveTo() { return internal.vatEffectiveTo(); }
        };
    }
}
