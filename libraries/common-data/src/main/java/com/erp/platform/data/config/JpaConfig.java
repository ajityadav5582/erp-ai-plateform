package com.erp.platform.data.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Base JPA configuration for the platform.
 *
 * <p>Enables JPA auditing and configures base repository settings.
 * Each microservice should extend this configuration with its own
 * repository base package.
 *
 * @since 1.0.0
 */
@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(
    basePackages = "com.erp.platform",
    repositoryBaseClass = com.erp.platform.data.repository.BaseRepository.class
)
public class JpaConfig {
    // Configuration is done via annotations
}
