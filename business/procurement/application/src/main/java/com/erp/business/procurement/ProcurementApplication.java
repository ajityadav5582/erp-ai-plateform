package com.erp.business.procurement;

import com.erp.platform.security.web.SecurityExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.erp.platform.data.audit.AuditingConfig;
import com.erp.platform.security.aspect.PermissionAspect;
import com.erp.platform.security.context.DefaultSecurityContext;

/**
 * Main application class for the Procurement Service.
 *
 * <p>Handles purchase requisitions and requisition-to-purchase-order
 * traceability links for the multi-tenant ERP platform.
 *
 * <p>{@code @EntityScan} is required because the entities live in the sibling
 * {@code :business:procurement:domain} Gradle module, which sits outside this
 * application's component-scan root. Without it the persistence unit is empty,
 * no JPA metadata is discovered, and {@code spring.jpa.hibernate.ddl-auto}
 * creates no tables. The same applies to {@code @EnableJpaRepositories}, and both
 * lists are explicit rather than package-wildcarded, so a new feature package has
 * to be added here by hand. Forgetting is not a startup error in either case - the
 * entity simply has no table and the repository bean does not exist, which
 * surfaces much later as a missing-bean failure or a query against a table that
 * was never created.
 *
 * <p>{@code PermissionAspect} and {@code DefaultSecurityContext} are imported
 * explicitly for the same underlying reason. Both are ordinary {@code @Component}s
 * in {@code libraries:common-security}, and that library sits under
 * {@code com.erp.platform.security} while this application's scan root is
 * {@code com.erp.business.procurement} - so component scanning never reaches them.
 * The failure is silent: {@code @RequiresPermission} annotations still compile,
 * nothing enforces them, and every endpoint stays open to any authenticated caller.
 * Importing them here makes the permission matrix actually load-bearing.
 *
 * @since 1.0.0
 */
@SpringBootApplication
@ComponentScan(basePackages = {
        "com.erp.business.procurement",
        "com.erp.business.procurement.interfaces.rest"
})

@EntityScan(basePackages = {
        "com.erp.business.procurement.purchaserequisition.domain",
        "com.erp.business.procurement.purchaseorder.domain",
        "com.erp.business.procurement.goodsreceipt.domain",
        "com.erp.business.procurement.purchaseinvoice.domain",
        "com.erp.business.procurement.supplierpayment.domain",
        "com.erp.business.procurement.shared.domain",
        "com.erp.business.procurement.workflow.domain"
})
@EnableJpaRepositories(basePackages = {
        "com.erp.business.procurement.purchaserequisition.infrastructure.persistence",
        "com.erp.business.procurement.purchaseorder.infrastructure.persistence",
        "com.erp.business.procurement.goodsreceipt.infrastructure.persistence",
        "com.erp.business.procurement.purchaseinvoice.infrastructure.persistence",
        "com.erp.business.procurement.supplierpayment.infrastructure.persistence",
        "com.erp.business.procurement.shared.infrastructure.persistence",
        "com.erp.business.procurement.workflow.infrastructure.persistence"
})
@Import({AuditingConfig.class, PermissionAspect.class, DefaultSecurityContext.class,
        SecurityExceptionHandler.class})
public class ProcurementApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProcurementApplication.class, args);
    }
}
