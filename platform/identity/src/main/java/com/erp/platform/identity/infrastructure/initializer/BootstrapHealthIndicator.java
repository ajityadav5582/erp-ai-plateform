package com.erp.platform.identity.infrastructure.initializer;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Component("bootstrap")
public class BootstrapHealthIndicator implements HealthIndicator {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Health health() {
        try {
            Long count = (Long) entityManager.createNativeQuery(
                            "SELECT COUNT(*) FROM db_bootstrap_state WHERE step_name = 'platform-master-data' AND status = 'COMPLETED'")
                    .getSingleResult();
            if (count != null && count > 0) {
                return Health.up().withDetail("platform-master-data", "COMPLETED").build();
            }
            return Health.down().withDetail("platform-master-data", "PENDING").build();
        } catch (Exception e) {
            return Health.unknown().withDetail("error", e.getMessage()).build();
        }
    }
}
