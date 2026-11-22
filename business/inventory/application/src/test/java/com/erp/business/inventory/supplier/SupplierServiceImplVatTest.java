package com.erp.business.inventory.supplier;

import com.erp.business.inventory.domain.Supplier;
import com.erp.business.inventory.supplier.dto.SupplierCreateRequest;
import com.erp.business.inventory.supplier.dto.SupplierResponse;
import com.erp.business.inventory.supplier.dto.SupplierUpdateRequest;
import com.erp.business.inventory.supplier.service.SupplierServiceImpl;
import com.erp.business.inventory.infrastructure.persistence.SupplierRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SupplierServiceImplVatTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

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

    @Test
    void createVatRegisteredWithTaxIdAutoSyncsVatNumber() {
        SupplierRepository repo = Mockito.mock(SupplierRepository.class);
        com.erp.business.inventory.supplier.mapper.SupplierMapper mapper = Mockito.mock(com.erp.business.inventory.supplier.mapper.SupplierMapper.class);
        SupplierServiceImpl service = new SupplierServiceImpl(repo, mapper);

        SupplierCreateRequest req = new SupplierCreateRequest(
                "Acme Supplies", "SUP-101", com.erp.business.inventory.domain.SupplierType.OTHER,
                "acme@example.com", "9800000000", "Kathmandu", "123456789", 30, "NPR",
                com.erp.business.inventory.domain.Supplier.VatRegistrationStatus.VAT_REGISTERED,
                null, null, null);

        when(repo.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createSupplier(1L, 10L, req);

        ArgumentCaptor<Supplier> captor = ArgumentCaptor.forClass(Supplier.class);
        verify(repo).save(captor.capture());
        Supplier saved = captor.getValue();

        assertThat(saved.getTaxId()).isEqualTo("123456789");
        assertThat(saved.getVatRegistrationNumber()).isEqualTo("123456789");
        assertThat(saved.getVatRegistrationStatus()).isEqualTo(Supplier.VatRegistrationStatus.VAT_REGISTERED);
    }

    @Test
    void panValidationRejectsNonNineDigits() {
        SupplierCreateRequest invalidPan = new SupplierCreateRequest(
                "Acme Supplies", "SUP-101", com.erp.business.inventory.domain.SupplierType.OTHER,
                "acme@example.com", "9800000000", "Kathmandu", "12345", 30, "NPR",
                com.erp.business.inventory.domain.Supplier.VatRegistrationStatus.PAN_ONLY,
                null, null, null);

        var violations = validator.validate(invalidPan);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("taxId"));

        SupplierCreateRequest validPan = new SupplierCreateRequest(
                "Acme Supplies", "SUP-101", com.erp.business.inventory.domain.SupplierType.OTHER,
                "acme@example.com", "9800000000", "Kathmandu", "987654321", 30, "NPR",
                com.erp.business.inventory.domain.Supplier.VatRegistrationStatus.PAN_ONLY,
                null, null, null);

        assertThat(validator.validate(validPan)).isEmpty();
    }
}
