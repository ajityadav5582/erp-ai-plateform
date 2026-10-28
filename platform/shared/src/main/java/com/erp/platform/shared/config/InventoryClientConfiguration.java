package com.erp.platform.shared.config;

import com.erp.business.inventory.interfaces.item.ItemService;
import com.erp.business.inventory.interfaces.stock.StockMovementService;
import com.erp.business.inventory.interfaces.unit.UnitService;
import com.erp.business.inventory.interfaces.warehouse.WarehouseService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class InventoryClientConfiguration {

    @Bean
    @ConditionalOnMissingBean(ItemService.class)
    public ItemService itemService(
            @Value("${inventory.service-uri:http://inventory-service:8083}") String inventoryUri) {
        return new com.erp.platform.business.inventory.http.HttpItemService(inventoryRestClient(inventoryUri));
    }

    @Bean
    @ConditionalOnMissingBean(UnitService.class)
    public UnitService unitService(
            @Value("${inventory.service-uri:http://inventory-service:8083}") String inventoryUri) {
        return new com.erp.platform.business.inventory.http.HttpUnitService(inventoryRestClient(inventoryUri));
    }

    @Bean
    @ConditionalOnMissingBean(StockMovementService.class)
    public StockMovementService stockMovementService(
            @Value("${inventory.service-uri:http://inventory-service:8083}") String inventoryUri) {
        return new com.erp.platform.business.inventory.http.HttpStockMovementService(inventoryRestClient(inventoryUri));
    }

    @Bean
    @ConditionalOnMissingBean(WarehouseService.class)
    public WarehouseService warehouseService(
            @Value("${inventory.service-uri:http://inventory-service:8083}") String inventoryUri) {
        return new com.erp.platform.business.inventory.http.HttpWarehouseService(inventoryRestClient(inventoryUri));
    }

    private RestClient inventoryRestClient(String inventoryUri) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        return RestClient.builder()
                .baseUrl(inventoryUri)
                .requestFactory(requestFactory)
                .build();
    }
}
