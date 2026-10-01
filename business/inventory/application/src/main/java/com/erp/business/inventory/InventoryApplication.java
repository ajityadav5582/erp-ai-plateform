package com.erp.business.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.context.annotation.Import;

import com.erp.platform.data.audit.AuditingConfig;

/**
 * Main application class for the Inventory Service.
 *
 * <p>This service provides inventory management for the multi-tenant ERP platform.
 * It handles category management, product management, stock tracking, and inventory operations.
 *
 * @since 1.0.0
 */
@SpringBootApplication
@EnableJpaRepositories(basePackages = {
        "com.erp.business.inventory.infrastructure.persistence"
})
@EntityScan(basePackages = {
        "com.erp.business.inventory.domain"
})
@Import(AuditingConfig.class)
public class InventoryApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryApplication.class, args);
    }
}
