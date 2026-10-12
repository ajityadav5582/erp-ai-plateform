package com.erp.business.inventory.interfaces.supplier;

import com.erp.platform.common.dto.PageRequest;
import com.erp.platform.common.dto.PageResponse;

import java.util.List;
import java.util.Optional;

/**
 * Application interface for Supplier operations.
 *
 * @since 1.0.0
 */
public interface SupplierService {

    Optional<SupplierResponse> findById(Long supplierId);

    Optional<SupplierResponse> findByCode(String code);

    boolean validateSupplier(Long supplierId);

    boolean validateSupplierCode(String code);

    List<SupplierResponse> findAllActive();

    PageResponse<SupplierResponse> findAll(PageRequest pageRequest);

    interface SupplierResponse {
        Long getId();
        String getCode();
        String getName();
        String getSupplierType(); // MANUFACTURER, DISTRIBUTOR, WHOLESALER, etc.
        String getContactPerson();
        String getEmail();
        String getPhone();
        String getAddress();
        String getTaxNumber();
        Boolean getIsActive();
    }

    interface SupplierCreateRequest {
        String getCode();
        String getName();
        String getSupplierType();
        String getContactPerson();
        String getEmail();
        String getPhone();
        String getAddress();
        String getTaxNumber();
        Boolean getIsActive();
    }

    interface SupplierUpdateRequest {
        String getName();
        String getSupplierType();
        String getContactPerson();
        String getEmail();
        String getPhone();
        String getAddress();
        String getTaxNumber();
        Boolean getIsActive();
    }
}
