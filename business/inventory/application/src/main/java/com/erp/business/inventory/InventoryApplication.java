package com.erp.business.inventory;

import com.erp.platform.security.web.SecurityExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.erp.platform.data.audit.AuditingConfig;
import com.erp.platform.security.aspect.PermissionAspect;
import com.erp.platform.security.context.DefaultSecurityContext;

/**
 * Main application class for the Inventory Service.
 *
 * <p>This service provides inventory management for the multi-tenant ERP platform.
 * It handles category management, product management, stock tracking, and inventory operations.
 *
 * <p>{@code @ComponentScan} is required because the REST controllers and exception
 * handlers live in {@code com.erp.business.inventory.interfaces.rest}, outside the
 * application's component-scan root. Without it those beans are never registered.
 *
 * <p>{@code @EntityScan} is required because the entities live in the sibling
 * {@code :business:inventory:domain} Gradle module, which sits outside this
 * application's component-scan root. Without it the persistence unit is empty,
 * no JPA metadata is discovered, and {@code spring.jpa.hibernate.ddl-auto}
 * creates no tables.
 *
 * <p>{@code PermissionAspect} and {@code DefaultSecurityContext} are imported
 * explicitly because both are ordinary {@code @Component}s in
 * {@code libraries:common-security} under {@code com.erp.platform.security},
 * while this application's scan root is {@code com.erp.business.inventory} -
 * so component scanning never reaches them. The failure is silent:
 * {@code @RequiresPermission} annotations still compile, are silently ignored
 * at runtime, and every endpoint stays open to any authenticated caller.
 *
 * @since 1.0.0
 */
@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {
        "com.erp.business.inventory",
        "com.erp.platform.shared",
        "com.erp.business.inventory.interfaces.rest"
})

@EntityScan(basePackages = {
        "com.erp.business.inventory.domain",
        "com.erp.business.inventory.shared.domain"
})
@EnableJpaRepositories(basePackages = {
        "com.erp.business.inventory.infrastructure.persistence"
})
@Import({AuditingConfig.class, PermissionAspect.class, DefaultSecurityContext.class,
        SecurityExceptionHandler.class})
public class InventoryApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryApplication.class, args);
    }
}
