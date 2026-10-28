package com.erp.business.sales;

import com.erp.platform.security.web.SecurityExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.erp.platform.data.audit.AuditingConfig;
import com.erp.platform.security.aspect.PermissionAspect;
import com.erp.platform.security.context.DefaultSecurityContext;

/**
 * Main application class for the Sales Service.
 *
 * <p>Handles the sell side of the ERP: customers, sales orders, deliveries,
 * IRD-compliant tax invoices, customer receipts, sales returns and the sales
 * register that feeds the VAT return. It is the mirror image of the Procurement
 * Service and follows the same aggregate, permission and numbering conventions.
 *
 * <p>{@code @EntityScan} is required because the entities live in the sibling
 * {@code :business:sales:domain} Gradle module, which sits outside this
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
 * {@code com.erp.business.sales} - so component scanning never reaches them.
 * The failure is silent: {@code @RequiresPermission} annotations still compile,
 * nothing enforces them, and every endpoint stays open to any authenticated caller.
 * Importing them here makes the permission matrix actually load-bearing.
 *
 * @since 1.0.0
 */
@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {
        "com.erp.business.sales",
        "com.erp.business.sales.interfaces.rest",
        "com.erp.platform.shared"
})

@EntityScan(basePackages = {
        "com.erp.business.sales.customer.domain",
        "com.erp.business.sales.salesorder.domain",
        "com.erp.business.sales.quotation.domain",
        "com.erp.business.sales.delivery.domain",
        "com.erp.business.sales.invoice.domain",
        "com.erp.business.sales.receipt.domain",
        "com.erp.business.sales.salesreturn.domain",
        "com.erp.business.sales.shared.domain"
})
@EnableJpaRepositories(basePackages = {
        "com.erp.business.sales.customer.infrastructure.persistence",
        "com.erp.business.sales.salesorder.infrastructure.persistence",
        "com.erp.business.sales.quotation.infrastructure.persistence",
        "com.erp.business.sales.delivery.infrastructure.persistence",
        "com.erp.business.sales.invoice.infrastructure.persistence",
        "com.erp.business.sales.receipt.infrastructure.persistence",
        "com.erp.business.sales.salesreturn.infrastructure.persistence",
        "com.erp.business.sales.shared.infrastructure.persistence"
})
@Import({AuditingConfig.class, PermissionAspect.class, DefaultSecurityContext.class,
        SecurityExceptionHandler.class})
public class SalesApplication {

    public static void main(String[] args) {
        SpringApplication.run(SalesApplication.class, args);
    }
}
