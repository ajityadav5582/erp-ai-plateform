package com.erp.platform.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for the Identity Service.
 *
 * <p>This service provides user and role management for the multi-tenant ERP platform.
 * It handles user authentication, role-based access control (RBAC), and identity lifecycle.
 *
 * @since 1.0.0
 */
@SpringBootApplication
public class IdentityApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdentityApplication.class, args);
    }
}
