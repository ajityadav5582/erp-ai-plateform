package com.erp.platform.business.inventory.http;

import com.erp.platform.business.inventory.InventoryClientException;

import com.erp.business.inventory.interfaces.warehouse.WarehouseService;
import com.erp.platform.common.dto.PageRequest;
import com.erp.platform.common.dto.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Secondary adapter that satisfies the warehouse port over HTTP by calling
 * Inventory's warehouse endpoints.
 *
 * <p>The caller's bearer token and company header are forwarded, so Inventory
 * authorises and scopes the call exactly as it would a direct call - the tenant
 * comes from the token, the company from the header. This is the microservice
 * wiring of the port; a modular-monolith deployment can replace it with an
 * in-process adapter against the same interface.
 */
public class HttpWarehouseService implements WarehouseService {

    private static final String BY_ID_PATH = "/api/v1/inventory/warehouses/{id}";
    private static final String BY_CODE_PATH = "/api/v1/inventory/warehouses/by-code";
    private static final String ACTIVE_PATH = "/api/v1/inventory/warehouses/active";
    private static final String LIST_PATH = "/api/v1/inventory/warehouses";

    private final RestClient restClient;

    public HttpWarehouseService(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Optional<WarehouseResponse> findById(Long warehouseId) {
        try {
            WarehouseResponseDto dto = restClient.get()
                    .uri(BY_ID_PATH, warehouseId)
                    .headers(this::forwardRequestContext)
                    .retrieve()
                    .body(WarehouseResponseDto.class);
            return Optional.ofNullable(dto);
        } catch (Exception ex) {
            throw new InventoryClientException(
                    "Inventory warehouse call failed: GET " + BY_ID_PATH, ex);
        }
    }

    @Override
    public Optional<WarehouseResponse> findByCode(String code) {
        try {
            WarehouseResponseDto dto = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path(BY_CODE_PATH).queryParam("code", code).build())
                    .headers(this::forwardRequestContext)
                    .retrieve()
                    .body(WarehouseResponseDto.class);
            return Optional.ofNullable(dto);
        } catch (Exception ex) {
            throw new InventoryClientException(
                    "Inventory warehouse call failed: GET " + BY_CODE_PATH, ex);
        }
    }

    @Override
    public boolean validateWarehouse(Long warehouseId) {
        return findById(warehouseId)
                .map(w -> Boolean.TRUE.equals(w.getIsActive()))
                .orElse(false);
    }

    @Override
    public boolean validateWarehouseCode(String code) {
        return findByCode(code)
                .map(w -> Boolean.TRUE.equals(w.getIsActive()))
                .orElse(false);
    }

    @Override
    public List<WarehouseResponse> findAllActive() {
        return List.of();
    }

    @Override
    public PageResponse<WarehouseResponse> findAll(PageRequest pageRequest) {
        try {
            WarehousePageDto page = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(LIST_PATH)
                            .queryParam("page", String.valueOf(pageRequest.page() != null ? pageRequest.page() : 0))
                            .queryParam("size", String.valueOf(pageRequest.size() != null ? pageRequest.size() : 20))
                            .build())
                    .headers(this::forwardRequestContext)
                    .retrieve()
                    .body(WarehousePageDto.class);
            if (page == null) {
                return PageResponse.of(
                        List.of(),
                        new PageResponse.Pagination(0, 20, 0, 0, true, true)
                );
            }
            List<WarehouseResponse> content = page.content() != null
                    ? page.content().stream().map(dto -> (WarehouseResponse) dto).toList()
                    : List.of();
            return PageResponse.of(
                    content,
                    new PageResponse.Pagination(
                            page.page(),
                            page.size(),
                            page.totalElements(),
                            page.totalPages(),
                            page.first(),
                            page.last()
                    )
            );
        } catch (Exception ex) {
            throw new InventoryClientException(
                    "Inventory warehouse call failed: GET " + LIST_PATH, ex);
        }
    }

    private void forwardRequestContext(HttpHeaders headers) {
        Map<String, String> captured = com.erp.platform.business.inventory.InventoryRequestContext.get();
        if (captured != null && !captured.isEmpty()) {
            captured.forEach(headers::set);
            return;
        }
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

    /**
     * DTO for deserializing Inventory's single warehouse response.
     */
    record WarehouseResponseDto(
            Long id,
            String code,
            String name,
            String description,
            String address,
            String warehouseType,
            Boolean isActive,
            Boolean isDefault
    ) implements WarehouseService.WarehouseResponse {

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public String getCode() {
            return code;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public String getAddress() {
            return address;
        }

        @Override
        public String getWarehouseType() {
            return warehouseType;
        }

        @Override
        public Boolean getIsActive() {
            return isActive;
        }

        @Override
        public Boolean getIsDefault() {
            return isDefault;
        }
    }

    /**
     * DTO for deserializing Inventory's paginated warehouse list response.
     */
    record WarehousePageDto(
            List<WarehouseResponseDto> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean first,
            boolean last
    ) {
    }
}
