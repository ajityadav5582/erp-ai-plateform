package com.erp.platform.tenant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for the Tenant Service.
 *
 * <p>This service provides tenant lifecycle management and tenant context propagation
 * for the multi-tenant ERP platform.
 *
 * @since 1.0.0
 */
@SpringBootApplication
public class TenantApplication {

    public static void main(String[] args) {
        SpringApplication.run(TenantApplication.class, args);
    }
}
