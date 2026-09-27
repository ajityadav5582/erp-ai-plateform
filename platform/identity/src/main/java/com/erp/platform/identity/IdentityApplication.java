package com.erp.platform.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Main application class for the Identity Service.
 *
 * <p>This service provides user and role management for the multi-tenant ERP platform.
 * It handles user authentication, role-based access control (RBAC), and identity lifecycle.
 *
 * @since 1.0.0
 */
@SpringBootApplication
@EnableJpaRepositories(basePackages = {
        "com.erp.platform.identity.infrastructure.persistence",
        "com.erp.platform.tenant.infrastructure.persistence"
})
@EntityScan(basePackages = {
        "com.erp.platform.identity.domain",
        "com.erp.platform.tenant.domain"
})
@Import(com.erp.platform.data.audit.AuditingConfig.class)
@EnableConfigurationProperties({
        com.erp.platform.identity.application.security.JwtProperties.class,
        com.erp.platform.identity.application.security.PasswordResetProperties.class
})
public class IdentityApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdentityApplication.class, args);
    }
}
