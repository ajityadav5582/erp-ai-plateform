package com.erp.platform.business.inventory.http;

import com.erp.platform.business.inventory.InventoryClientException;

import com.erp.business.inventory.interfaces.unit.UnitService;
import com.erp.platform.common.dto.PageRequest;
import com.erp.platform.common.dto.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Optional;

public class HttpUnitService implements UnitService {

    private static final String UNITS_PATH = "/api/v1/inventory/units";

    private final RestClient restClient;

    public HttpUnitService(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Optional<UnitResponse> findById(Long unitId) {
        if (unitId == null) {
            return Optional.empty();
        }
        try {
            UnitDto dto = restClient.get()
                    .uri(UNITS_PATH + "/{id}", unitId)
                    .headers(this::forwardRequestContext)
                    .retrieve()
                    .body(UnitDto.class);
            return Optional.ofNullable(dto).map(this::toResponse);
        } catch (HttpClientErrorException.NotFound notFound) {
            return Optional.empty();
        } catch (Exception ex) {
            throw new InventoryClientException("Inventory unit call failed: GET " + UNITS_PATH + "/" + unitId, ex);
        }
    }

    @Override
    public Optional<UnitResponse> findByCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        try {
            UnitDto dto = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path(UNITS_PATH + "/by-code").queryParam("code", code).build())
                    .headers(this::forwardRequestContext)
                    .retrieve()
                    .body(UnitDto.class);
            return Optional.ofNullable(dto).map(this::toResponse);
        } catch (HttpClientErrorException.NotFound notFound) {
            return Optional.empty();
        } catch (Exception ex) {
            throw new InventoryClientException("Inventory unit call failed: GET " + UNITS_PATH + "/by-code", ex);
        }
    }

    @Override
    public Optional<UnitResponse> resolveUnit(String rawUomCode) {
        if (rawUomCode == null || rawUomCode.isBlank()) {
            List<UnitResponse> active = findAllActive();
            return active.isEmpty() ? Optional.empty() : Optional.of(active.get(0));
        }
        Optional<UnitResponse> byCode = findByCode(rawUomCode);
        if (byCode.isPresent()) {
            return byCode;
        }
        String normalized = rawUomCode.trim().toUpperCase();
        for (UnitResponse u : findAllActive()) {
            if (u.getCode() != null && u.getCode().equalsIgnoreCase(normalized)) {
                return Optional.of(u);
            }
            if (u.getName() != null && u.getName().equalsIgnoreCase(rawUomCode.trim())) {
                return Optional.of(u);
            }
        }
        List<UnitResponse> active = findAllActive();
        return active.isEmpty() ? Optional.empty() : Optional.of(active.get(0));
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
    public List<UnitResponse> findAllActive() {
        try {
            List<UnitDto> dtos = restClient.get()
                    .uri(UNITS_PATH + "/active")
                    .headers(this::forwardRequestContext)
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<>() {});
            if (dtos == null) {
                return List.of();
            }
            return dtos.stream().map(this::toResponse).toList();
        } catch (Exception ex) {
            throw new InventoryClientException("Inventory unit call failed: GET " + UNITS_PATH + "/active", ex);
        }
    }

    @Override
    public PageResponse<UnitResponse> findAll(PageRequest pageRequest) {
        return PageResponse.of(List.of(), PageResponse.Pagination.of(0, 20, 0, 0));
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

    private UnitResponse toResponse(UnitDto dto) {
        return new UnitResponse() {
            @Override public Long getId() { return dto.id(); }
            @Override public String getCode() { return dto.code(); }
            @Override public String getName() { return dto.name(); }
            @Override public String getSymbol() { return dto.symbol(); }
            @Override public String getDimension() { return dto.dimension() == null ? null : dto.dimension().toString(); }
            @Override public Integer getBaseUnitId() { return null; }
            @Override public Integer getConversionFactor() { return dto.decimalScale(); }
            @Override public Boolean getIsActive() { return dto.isActive(); }
            @Override public Boolean getIsBaseUnit() { return null; }
        };
    }

    private record UnitDto(
            Long id,
            Long tenantId,
            Long companyId,
            String name,
            String code,
            Object dimension,
            String symbol,
            Integer decimalScale,
            @JsonProperty("isActive") Boolean isActive
    ) {
    }
}
