package com.erp.platform.identity.infrastructure.initializer;

/**
 * Contract for an idempotent master-data seeding step.
 *
 * <p>Implementations are discovered by Spring and executed in {@code order()} order by
 * {@link DatabaseBootstrapRunner}, which records completion in {@code db_bootstrap_state}.
 */
public interface MasterDataSeeder {

    /** Stable identifier of the seeding step (e.g. {@code platform-master-data}). */
    String stepName();

    /** Version of the seeding logic; bump to force a re-run. */
    String stepVersion();

    /** Execution order (lower runs first). */
    int order();

    /** Whether this seeder should run in the current environment. */
    default boolean isEnabled() {
        return true;
    }

    /** Performs the (idempotent) seeding work. */
    void seed();
}
