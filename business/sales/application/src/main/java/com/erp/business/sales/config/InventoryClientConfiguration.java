package com.erp.business.sales.config;

import com.erp.business.inventory.interfaces.item.ItemService;
import com.erp.business.inventory.interfaces.stock.StockMovementService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Provides the Inventory ports the Sales service depends on
 * ({@link ItemService}, {@link StockMovementService}) as HTTP clients.
 *
 * <p>Sales and Inventory are separate services, so these ports are satisfied by
 * adapters that call Inventory over HTTP, forwarding the caller's bearer token
 * and company header. Without them the sales controllers cannot be constructed
 * and every request fails with an {@code UnsatisfiedDependencyException} that
 * surfaces as a 403 at the gateway - the symptom that motivated this class.
 *
 * <p>Both beans are {@code @ConditionalOnMissingBean} so a future in-process
 * adapter (a true modular monolith) can take precedence simply by being on the
 * classpath: the port is the contract, these are swappable implementations.
 */
@Configuration
public class InventoryClientConfiguration {

    @Bean
    @ConditionalOnMissingBean(ItemService.class)
    public ItemService itemService(
            @Value("${inventory.service-uri:http://inventory-service:8083}") String inventoryUri) {
        return new HttpItemService(inventoryRestClient(inventoryUri));
    }

    @Bean
    @ConditionalOnMissingBean(StockMovementService.class)
    public StockMovementService stockMovementService(
            @Value("${inventory.service-uri:http://inventory-service:8083}") String inventoryUri) {
        return new HttpStockMovementService(inventoryRestClient(inventoryUri));
    }

    private RestClient inventoryRestClient(String inventoryUri) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        return RestClient.builder()
                .baseUrl(inventoryUri)
                .requestFactory(requestFactory)
                .build();
    }
}
