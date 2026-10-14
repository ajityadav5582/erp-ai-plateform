package com.erp.business.pos.config;

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
 * Provides the Inventory {@link ItemService} and {@link StockMovementService}
 * ports as HTTP clients.
 *
 * <p>POS and Inventory are separate services, so each port is satisfied by an
 * adapter that calls Inventory over HTTP, forwarding the caller's bearer token
 * and company header. The beans are {@code @ConditionalOnMissingBean} so a future
 * in-process adapter can take precedence simply by being on the classpath.
 */
@Configuration
public class InventoryClientConfiguration {

    @Bean
    @ConditionalOnMissingBean(ItemService.class)
    public ItemService itemService(
            @Value("${inventory.service-uri:http://inventory-service:8083}") String inventoryUri) {
        return new HttpItemService(inventoryRestClient(inventoryUri));
    }

    /**
     * The outbound stock port used by checkout (issue) and void (issue reversal).
     * Both operations are idempotent by {@code (referenceType, referenceId)} on
     * the Inventory side, so a retried checkout cannot deduct stock twice.
     */
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
