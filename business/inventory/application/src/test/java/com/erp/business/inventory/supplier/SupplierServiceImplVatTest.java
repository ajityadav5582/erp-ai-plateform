package com.erp.business.inventory.supplier;

import com.erp.business.inventory.domain.Supplier;
import com.erp.business.inventory.supplier.dto.SupplierCreateRequest;
import com.erp.business.inventory.supplier.dto.SupplierResponse;
import com.erp.business.inventory.supplier.dto.SupplierUpdateRequest;
import com.erp.business.inventory.supplier.service.SupplierServiceImpl;
import com.erp.business.inventory.infrastructure.persistence.SupplierRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class SupplierServiceImplVatTest {

    @Test
    void createVatRegisteredWithoutNumberThrows() {
        SupplierRepository repo = Mockito.mock(SupplierRepository.class);
        com.erp.business.inventory.supplier.mapper.SupplierMapper mapper = Mockito.mock(com.erp.business.inventory.supplier.mapper.SupplierMapper.class);
        SupplierServiceImpl service = new SupplierServiceImpl(repo, mapper);

        SupplierCreateRequest req = new SupplierCreateRequest(
                "Supplier", "SUP", com.erp.business.inventory.domain.SupplierType.OTHER,
                null, null, null, null, null, null,
                com.erp.business.inventory.domain.Supplier.VatRegistrationStatus.VAT_REGISTERED,
                null, null, null);

        assertThrows(IllegalArgumentException.class, () -> service.createSupplier(1L, 1L, req));
    }
}
