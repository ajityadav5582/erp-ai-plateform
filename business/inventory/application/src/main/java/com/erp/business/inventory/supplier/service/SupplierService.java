package com.erp.business.inventory.supplier.service;

import com.erp.business.inventory.supplier.dto.SupplierCreateRequest;
import com.erp.business.inventory.supplier.dto.SupplierResponse;
import com.erp.business.inventory.supplier.dto.SupplierUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SupplierService {

    SupplierResponse createSupplier(Long tenantId, Long companyId, SupplierCreateRequest request);

    SupplierResponse getSupplierById(Long tenantId, Long id);

    SupplierResponse getSupplierByCode(Long tenantId, Long companyId, String code);

    Page<SupplierResponse> listSuppliers(Long tenantId, Long companyId, Pageable pageable);

    SupplierResponse updateSupplier(Long tenantId, Long id, SupplierUpdateRequest request);

    SupplierResponse activateSupplier(Long tenantId, Long id);

    SupplierResponse deactivateSupplier(Long tenantId, Long id);

    void deleteSupplier(Long tenantId, Long id);
}
