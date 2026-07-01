package com.erp.platform.data.config;

import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Flyway database migration configuration.
 *
 * <p>Provides base Flyway configuration for database migrations.
 * Microservices should extend this configuration with their specific
 * migration locations.
 *
 * @since 1.0.0
 */
@Configuration
public class FlywayConfig {

    /**
     * Creates a Flyway instance with baseline configuration.
     *
     * @param dataSource the data source
     * @return the Flyway instance
     */
    @Bean
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
            .dataSource(dataSource)
            .baselineOnMigrate(true)
            .validateOnMigrate(true)
            .cleanDisabled(true)
            .load();
    }
}
