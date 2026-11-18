package com.erp.platform.identity.infrastructure.initializer;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * Runs all {@link MasterDataSeeder} beans in order, tracking completion in
 * {@code db_bootstrap_state} so seeding is idempotent and observable.
 */
@Component
@Order(0)
public class DatabaseBootstrapRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseBootstrapRunner.class);

    private final List<MasterDataSeeder> seeders;

    @PersistenceContext
    private EntityManager entityManager;

    public DatabaseBootstrapRunner(List<MasterDataSeeder> seeders) {
        this.seeders = seeders;
    }

    @Override
    @Transactional
    public void run(String... args) {
        List<MasterDataSeeder> ordered = seeders.stream()
                .sorted(Comparator.comparingInt(MasterDataSeeder::order))
                .toList();

        for (MasterDataSeeder seeder : ordered) {
            if (!seeder.isEnabled()) {
                log.info("Bootstrap step disabled, skipping: {}", seeder.stepName());
                continue;
            }
            if (isCompleted(seeder)) {
                log.debug("Bootstrap step already completed: {} v{}", seeder.stepName(), seeder.stepVersion());
                continue;
            }
            log.info("Running bootstrap step: {} v{}", seeder.stepName(), seeder.stepVersion());
            try {
                seeder.seed();
                markCompleted(seeder);
                log.info("Completed bootstrap step: {} v{}", seeder.stepName(), seeder.stepVersion());
            } catch (Exception e) {
                log.error("Bootstrap step failed: {} v{}", seeder.stepName(), seeder.stepVersion(), e);
                throw e;
            }
        }
        log.info("Database bootstrap completed ({} step(s)).", ordered.size());
    }

    private boolean isCompleted(MasterDataSeeder seeder) {
        try {
            Long count = (Long) entityManager.createNativeQuery(
                            "SELECT COUNT(*) FROM db_bootstrap_state WHERE step_name = :step AND step_version = :ver AND status = 'COMPLETED'")
                    .setParameter("step", seeder.stepName())
                    .setParameter("ver", seeder.stepVersion())
                    .getSingleResult();
            return count != null && count > 0;
        } catch (Exception e) {
            // Table may not exist (e.g. local dev without Flyway) -> treat as not completed.
            return false;
        }
    }

    private void markCompleted(MasterDataSeeder seeder) {
        try {
            entityManager.createNativeQuery(
                            "INSERT INTO db_bootstrap_state (step_name, step_version, status) VALUES (:step, :ver, 'COMPLETED') " +
                                    "ON CONFLICT (step_name, step_version) DO UPDATE SET status = 'COMPLETED', completed_at = NOW()")
                    .setParameter("step", seeder.stepName())
                    .setParameter("ver", seeder.stepVersion())
                    .executeUpdate();
        } catch (Exception e) {
            log.warn("Could not record bootstrap state for {} v{}: {}", seeder.stepName(), seeder.stepVersion(), e.getMessage());
        }
    }
}
