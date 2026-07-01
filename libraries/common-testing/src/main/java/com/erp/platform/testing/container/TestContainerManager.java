package com.erp.platform.testing.container;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Test container manager for integration tests.
 *
 * <p>Provides managed test containers for PostgreSQL.
 * Containers are started once and reused across tests.
 *
 * @since 1.0.0
 */
public class TestContainerManager {

    private static PostgreSQLContainer<?> postgresContainer;
    private static boolean initialized = false;

    /**
     * Initializes all test containers.
     *
     * <p>This should be called once before any integration tests run.
     */
    public static synchronized void initialize() {
        if (initialized) {
            return;
        }

        postgresContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("test_db")
            .withUsername("test")
            .withPassword("test");
        postgresContainer.start();

        initialized = true;
    }

    /**
     * Stops all test containers.
     *
     * <p>This should be called after all integration tests complete.
     */
    public static synchronized void stop() {
        if (!initialized) {
            return;
        }

        if (postgresContainer != null) {
            postgresContainer.stop();
        }

        initialized = false;
    }

    public static PostgreSQLContainer<?> getPostgresContainer() {
        if (!initialized) {
            throw new IllegalStateException("Test containers not initialized. Call initialize() first.");
        }
        return postgresContainer;
    }
}
