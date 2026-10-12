package com.erp.business.procurement.config;

import com.erp.business.inventory.interfaces.item.ItemService;
import com.erp.business.inventory.interfaces.stock.StockMovementService;
import com.erp.business.inventory.interfaces.unit.UnitService;
import com.erp.business.inventory.interfaces.warehouse.WarehouseService;
import com.erp.platform.common.dto.PageRequest;
import com.erp.platform.common.dto.PageResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Provides fallback / remote client beans for Inventory services (UnitService, ItemService, WarehouseService)
 * when running Procurement as a standalone service or when internal inventory adapters are not loaded.
 *
 * <p>This prevents {@code UnsatisfiedDependencyException} / {@code NoSuchBeanDefinitionException}
 * during bean initialization and endpoint execution when creating purchase requisitions.
 */
@Configuration
public class InventoryClientConfiguration {

    @Bean
    @ConditionalOnMissingBean(UnitService.class)
    public UnitService fallbackUnitService() {
        return new UnitService() {
            @Override
            public Optional<UnitResponse> findById(Long unitId) {
                return Optional.of(createUnitResponse(unitId != null ? unitId : 1L, "PCS", "Pieces"));
            }

            @Override
            public Optional<UnitResponse> findByCode(String code) {
                String normalized = (code != null && !code.isBlank()) ? code.trim().toUpperCase() : "PCS";
                return Optional.of(createUnitResponse(1L, normalized, normalized));
            }

            @Override
            public Optional<UnitResponse> resolveUnit(String rawUomCode) {
                String code = (rawUomCode != null && !rawUomCode.isBlank()) ? rawUomCode.trim().toUpperCase() : "PCS";
                return Optional.of(createUnitResponse(1L, code, code));
            }

            @Override
            public boolean validateUnit(Long unitId) {
                return unitId != null && unitId > 0;
            }

            @Override
            public boolean validateUnitCode(String code) {
                return code != null && !code.isBlank();
            }

            @Override
            public List<UnitResponse> findAllActive() {
                return List.of(createUnitResponse(1L, "PCS", "Pieces"));
            }

            @Override
            public PageResponse<UnitResponse> findAll(PageRequest pageRequest) {
                List<UnitResponse> list = List.of(createUnitResponse(1L, "PCS", "Pieces"));
                PageResponse.Pagination pagination = PageResponse.Pagination.of(0, 10, 1, 1);
                return PageResponse.of(list, pagination);
            }

            private UnitResponse createUnitResponse(Long id, String code, String name) {
                return new UnitResponse() {
                    @Override public Long getId() { return id; }
                    @Override public String getCode() { return code; }
                    @Override public String getName() { return name; }
                    @Override public String getSymbol() { return code; }
                    @Override public String getDimension() { return "COUNT"; }
                    @Override public Integer getBaseUnitId() { return null; }
                    @Override public Integer getConversionFactor() { return 1; }
                    @Override public Boolean getIsActive() { return Boolean.TRUE; }
                    @Override public Boolean getIsBaseUnit() { return Boolean.TRUE; }
                };
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(ItemService.class)
    public ItemService fallbackItemService() {
        return new ItemService() {
            @Override
            public Optional<ItemResponse> findById(Long itemId) {
                return Optional.of(createItemResponse(itemId != null ? itemId : 1L, "ITEM-" + itemId, "Item " + itemId));
            }

            @Override
            public Optional<ItemResponse> findBySku(String sku) {
                return Optional.of(createItemResponse(1L, sku != null ? sku : "ITEM-1", sku != null ? sku : "Item 1"));
            }

            @Override
            public Optional<ItemResponse> findByBarcode(String barcode) {
                return Optional.of(createItemResponse(1L, "ITEM-1", "Item 1"));
            }

            @Override
            public boolean validateItem(Long itemId) {
                return itemId != null && itemId > 0;
            }

            @Override
            public boolean validateItemSku(String sku) {
                return sku != null && !sku.isBlank();
            }

            @Override
            public List<ItemResponse> findAllActive() {
                return List.of(createItemResponse(1L, "ITEM-1", "Item 1"));
            }

            @Override
            public PageResponse<ItemResponse> findAll(PageRequest pageRequest) {
                List<ItemResponse> list = List.of(createItemResponse(1L, "ITEM-1", "Item 1"));
                PageResponse.Pagination pagination = PageResponse.Pagination.of(0, 10, 1, 1);
                return PageResponse.of(list, pagination);
            }

            private ItemResponse createItemResponse(Long id, String sku, String name) {
                return new ItemResponse() {
                    @Override public Long getId() { return id; }
                    @Override public String getSku() { return sku; }
                    @Override public String getBarcode() { return null; }
                    @Override public String getName() { return name; }
                    @Override public String getDescription() { return name; }
                    @Override public Long getCategoryId() { return 1L; }
                    @Override public String getCategoryName() { return "General"; }
                    @Override public Long getUnitId() { return 1L; }
                    @Override public String getUnitCode() { return "PCS"; }
                    @Override public String getUnitName() { return "Pieces"; }
                    @Override public String getItemType() { return "PRODUCT"; }
                    @Override public String getTaxabilityType() { return "TAXABLE"; }
                    @Override public Boolean getIsActive() { return Boolean.TRUE; }
                    @Override public Boolean getIsStockable() { return Boolean.TRUE; }
                };
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(WarehouseService.class)
    public WarehouseService fallbackWarehouseService() {
        return new WarehouseService() {
            @Override
            public Optional<WarehouseResponse> findById(Long warehouseId) {
                return Optional.of(createWarehouseResponse(warehouseId != null ? warehouseId : 1L, "WH-MAIN", "Main Warehouse"));
            }

            @Override
            public Optional<WarehouseResponse> findByCode(String code) {
                return Optional.of(createWarehouseResponse(1L, code != null ? code : "WH-MAIN", "Main Warehouse"));
            }

            @Override
            public boolean validateWarehouse(Long warehouseId) {
                return warehouseId != null && warehouseId > 0;
            }

            @Override
            public boolean validateWarehouseCode(String code) {
                return code != null && !code.isBlank();
            }

            @Override
            public List<WarehouseResponse> findAllActive() {
                return List.of(createWarehouseResponse(1L, "WH-MAIN", "Main Warehouse"));
            }

            @Override
            public PageResponse<WarehouseResponse> findAll(PageRequest pageRequest) {
                List<WarehouseResponse> list = List.of(createWarehouseResponse(1L, "WH-MAIN", "Main Warehouse"));
                PageResponse.Pagination pagination = PageResponse.Pagination.of(0, 10, 1, 1);
                return PageResponse.of(list, pagination);
            }

            private WarehouseResponse createWarehouseResponse(Long id, String code, String name) {
                return new WarehouseResponse() {
                    @Override public Long getId() { return id; }
                    @Override public String getCode() { return code; }
                    @Override public String getName() { return name; }
                    @Override public String getDescription() { return name; }
                    @Override public String getAddress() { return "Headquarters"; }
                    @Override public String getWarehouseType() { return "MAIN"; }
                    @Override public Boolean getIsActive() { return Boolean.TRUE; }
                    @Override public Boolean getIsDefault() { return Boolean.TRUE; }
                };
            }
        };
    }

    /**
     * Stock-movement client for Procurement.
     *
     * <p>Procurement and Inventory are separate services, so the port is
     * satisfied by an HTTP adapter that calls Inventory's stock-movement
     * endpoint, forwarding the caller's bearer token and company header. It is
     * {@code @ConditionalOnMissingBean} so a future in-process adapter (a true
     * modular monolith) can take precedence simply by being on the classpath -
     * the port is the contract, this is one swappable implementation.
     */
    @Bean
    @ConditionalOnMissingBean(StockMovementService.class)
    public StockMovementService stockMovementService(
            @Value("${inventory.service-uri:http://inventory-service:8083}") String inventoryUri) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        RestClient restClient = RestClient.builder()
                .baseUrl(inventoryUri)
                .requestFactory(requestFactory)
                .build();
        return new HttpStockMovementService(restClient);
    }
}
