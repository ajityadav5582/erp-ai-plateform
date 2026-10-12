package com.erp.business.sales.config;

import com.erp.business.inventory.interfaces.item.ItemService;
import com.erp.platform.common.dto.PageRequest;
import com.erp.platform.common.dto.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Optional;

/**
 * Secondary adapter that satisfies the item port over HTTP by calling
 * Inventory's item endpoint.
 *
 * <p>Sales freezes an item's display fields (sku, name, unit) onto each order
 * line, so the lookup has to return the real item, not a placeholder. This
 * adapter calls {@code GET /api/v1/inventory/items/{id}} and maps Inventory's
 * response shape ({@code nameEn}, {@code uomCode}, ...) onto the port's
 * {@link ItemService.ItemResponse} contract ({@code getName()},
 * {@code getUnitCode()}, ...). The caller's bearer token and company header are
 * forwarded so Inventory authorises and scopes the read exactly as a direct call.
 */
class HttpItemService implements ItemService {

    private static final String ITEMS_PATH = "/api/v1/inventory/items";

    private final RestClient restClient;

    HttpItemService(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Optional<ItemResponse> findById(Long itemId) {
        if (itemId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(get(ITEMS_PATH + "/" + itemId)).map(this::toResponse);
    }

    @Override
    public Optional<ItemResponse> findBySku(String sku) {
        if (sku == null || sku.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(get(ITEMS_PATH + "/by-sku/" + sku)).map(this::toResponse);
    }

    @Override
    public Optional<ItemResponse> findByBarcode(String barcode) {
        if (barcode == null || barcode.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(get(ITEMS_PATH + "/by-barcode/" + barcode)).map(this::toResponse);
    }

    @Override
    public boolean validateItem(Long itemId) {
        return findById(itemId).map(i -> !Boolean.FALSE.equals(i.getIsActive())).orElse(false);
    }

    @Override
    public boolean validateItemSku(String sku) {
        return findBySku(sku).map(i -> !Boolean.FALSE.equals(i.getIsActive())).orElse(false);
    }

    @Override
    public List<ItemResponse> findAllActive() {
        // Not used by the sales order flow; item selection happens in the UI,
        // which reads Inventory directly. Kept empty rather than guessing a
        // second response shape over the wire.
        return List.of();
    }

    @Override
    public PageResponse<ItemResponse> findAll(PageRequest pageRequest) {
        return PageResponse.of(List.of(), PageResponse.Pagination.of(0, 20, 0, 0));
    }

    private InventoryItemDto get(String path) {
        try {
            return restClient.get()
                    .uri(path)
                    .headers(this::forwardRequestContext)
                    .retrieve()
                    .body(InventoryItemDto.class);
        } catch (HttpClientErrorException.NotFound notFound) {
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

    private ItemResponse toResponse(InventoryItemDto dto) {
        return new ItemResponse() {
            @Override public Long getId() { return dto.id(); }
            @Override public String getSku() { return dto.sku(); }
            @Override public String getBarcode() { return dto.barcode(); }
            @Override public String getName() { return dto.nameEn(); }
            @Override public String getDescription() { return dto.descriptionEn(); }
            @Override public Long getCategoryId() { return dto.categoryId(); }
            @Override public String getCategoryName() { return dto.categoryName(); }
            @Override public Long getUnitId() { return dto.uomId(); }
            @Override public String getUnitCode() { return dto.uomCode(); }
            @Override public String getUnitName() { return dto.uomName(); }
            @Override public String getItemType() { return dto.itemType(); }
            @Override public String getTaxabilityType() { return dto.taxabilityType(); }
            @Override public Boolean getIsActive() { return dto.isActive(); }
            @Override public Boolean getIsStockable() { return dto.isInventoryTracked(); }
        };
    }

    /**
     * Wire shape of Inventory's item response - only the fields the port needs.
     * Enums arrive as their string names, which is exactly what the port exposes.
     */
    private record InventoryItemDto(
            Long id,
            String sku,
            String barcode,
            String nameEn,
            String descriptionEn,
            Long categoryId,
            String categoryName,
            Long uomId,
            String uomCode,
            String uomName,
            String itemType,
            String taxabilityType,
            Boolean isActive,
            Boolean isInventoryTracked
    ) {
    }
}
