package com.erp.business.pos;

import com.erp.platform.data.audit.AuditingConfig;
import com.erp.platform.security.aspect.PermissionAspect;
import com.erp.platform.security.context.DefaultSecurityContext;
import com.erp.platform.security.web.SecurityExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for the POS Service.
 *
 * <p>Runs the atomic point-of-sale checkout flow (registers, cashier sessions,
 * transactions, quick items) and reports sales to the IRD CBMS. It reuses the
 * Inventory module for item lookup and stock deduction.
 */
@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {"com.erp.business.pos", "com.erp.platform.shared", "com.erp.platform.business"},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = com.erp.platform.shared.numbering.DocumentNumberService.class))
@EntityScan(basePackages = {
        "com.erp.business.pos.domain",
        "com.erp.business.pos.shared.domain",
        "com.erp.business.pos.restaurant.domain",
        "com.erp.business.pos.audit",
        "com.erp.business.pos.service.sequence",
        "com.erp.platform.identity.domain",
        "com.erpai.integration.ird.domain",
        "com.erpai.integration.ird.outbox.entity",
        "com.erpai.integration.ird.adapter.out.persistence"
})
@EnableJpaRepositories(basePackages = {"com.erp.business.pos", "com.erp.platform.identity.infrastructure.persistence", "com.erpai.integration.ird.infrastructure.persistence", "com.erpai.integration.ird.adapter.out.persistence", "com.erpai.integration.ird.outbox.repository"})
@Import({AuditingConfig.class, PermissionAspect.class, DefaultSecurityContext.class,
        SecurityExceptionHandler.class})
public class PosApplication {

    public static void main(String[] args) {
        SpringApplication.run(PosApplication.class, args);
    }
}
