package com.erp.platform.data.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Base Flyway configuration for the platform.
 *
 * <p>Provides common Flyway settings and migration strategy.
 * Each microservice should extend this configuration with its own
 * migration locations.
 *
 * @since 1.0.0
 */
@Configuration
public class FlywayBaseConfig {

    /**
     * Creates a Flyway migration strategy that validates migrations
     * but does not clean the database.
     *
     * @return the migration strategy
     */
    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            // Validate migrations before applying
            flyway.validate();
            flyway.migrate();
        };
    }

    // Note: Flyway configuration is now done via application properties
    // (spring.flyway.*) or FlywayConfigurationCustomizer beans.
    // The setter-based configuration below was removed due to API changes
    // in Flyway 10+.
}
