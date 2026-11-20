package com.erp.platform.business.inventory.http;

import com.erp.business.inventory.interfaces.supplier.SupplierService;
import com.erp.platform.common.dto.PageRequest;
import com.erp.platform.common.dto.PageResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class HttpSupplierService implements SupplierService {

    private static final String SUPPLIERS_PATH = "/api/v1/inventory/suppliers";

    private final RestClient restClient;

    public HttpSupplierService(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Optional<SupplierResponse> findById(Long supplierId) {
        if (supplierId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(get(SUPPLIERS_PATH + "/" + supplierId)).map(this::toResponse);
    }

    @Override
    public Optional<SupplierResponse> findByCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(get(SUPPLIERS_PATH + "/by-code/" + code)).map(this::toResponse);
    }

    @Override
    public boolean validateSupplier(Long supplierId) {
        return findById(supplierId).map(s -> !Boolean.FALSE.equals(s.getIsActive())).orElse(false);
    }

    @Override
    public boolean validateSupplierCode(String code) {
        return findByCode(code).map(s -> !Boolean.FALSE.equals(s.getIsActive())).orElse(false);
    }

    @Override
    public List<SupplierResponse> findAllActive() {
        return List.of();
    }

    @Override
    public PageResponse<SupplierResponse> findAll(PageRequest pageRequest) {
        return PageResponse.of(List.of(), PageResponse.Pagination.of(0, 20, 0, 0));
    }

    private InventorySupplierDto get(String path) {
        try {
            return restClient.get()
                    .uri(path)
                    .headers(this::forwardRequestContext)
                    .retrieve()
                    .body(InventorySupplierDto.class);
        } catch (HttpClientErrorException.NotFound notFound) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private void forwardRequestContext(HttpHeaders headers) {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            return;
        }
        HttpServletRequest request = attrs.getRequest();
        copyHeader(request, headers, "Authorization");
        copyHeader(request, headers, "X-Company-Id");
        copyHeader(request, headers, "X-Fiscal-Year-Id");
    }

    private void copyHeader(HttpServletRequest request, HttpHeaders headers, String name) {
        String value = request.getHeader(name);
        if (value != null && !value.isBlank()) {
            headers.set(name, value);
        }
    }

    private SupplierResponse toResponse(InventorySupplierDto dto) {
        return new SupplierResponse() {
            @Override public Long getId() { return dto.id(); }
            @Override public String getCode() { return dto.code(); }
            @Override public String getName() { return dto.name(); }
            @Override public String getSupplierType() { return dto.supplierType(); }
            @Override public String getContactPerson() { return dto.contactPerson(); }
            @Override public String getEmail() { return dto.email(); }
            @Override public String getPhone() { return dto.phone(); }
            @Override public String getAddress() { return dto.address(); }
            @Override public String getTaxNumber() { return dto.taxNumber(); }
            @Override public Boolean getIsActive() { return dto.isActive(); }
            @Override public String getVatRegistrationStatus() { return dto.vatRegistrationStatus(); }
            @Override public String getVatRegistrationNumber() { return dto.vatRegistrationNumber(); }
            @Override public LocalDate getVatEffectiveFrom() { return dto.vatEffectiveFrom(); }
            @Override public LocalDate getVatEffectiveTo() { return dto.vatEffectiveTo(); }
        };
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record InventorySupplierDto(
            Long id,
            String code,
            String name,
            String supplierType,
            String contactPerson,
            String email,
            String phone,
            String address,
            String taxNumber,
            Boolean isActive,
            String vatRegistrationStatus,
            String vatRegistrationNumber,
            LocalDate vatEffectiveFrom,
            LocalDate vatEffectiveTo
    ) {}
}

