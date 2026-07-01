package com.erp.platform.testing.containers;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Test containers configuration for the ERP AI Platform.
 *
 * <p>This class provides pre-configured test containers
 * for integration testing.
 *
 * <p>Usage:
 * <pre>
 * {@code
 * @Container
 * static PostgreSQLContainer<?> postgres = TestContainersConfig.postgres();
 * }
 * </pre>
 *
 * @since 1.0.0
 */
public final class TestContainersConfig {

    private static final String POSTGRES_IMAGE = "postgres:16-alpine";

    private TestContainersConfig() {
        // Utility class
    }

    /**
     * Creates a PostgreSQL test container.
     *
     * @return the PostgreSQL container
     */
    public static PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>(DockerImageName.parse(POSTGRES_IMAGE))
            .withDatabaseName("erp_test")
            .withUsername("test")
            .withPassword("test");
    }

    /**
     * Creates a PostgreSQL test container with custom configuration.
     *
     * @param databaseName the database name
     * @param username the username
     * @param password the password
     * @return the PostgreSQL container
     */
    public static PostgreSQLContainer<?> postgres(String databaseName, String username, String password) {
        return new PostgreSQLContainer<>(DockerImageName.parse(POSTGRES_IMAGE))
            .withDatabaseName(databaseName)
            .withUsername(username)
            .withPassword(password);
    }
}
