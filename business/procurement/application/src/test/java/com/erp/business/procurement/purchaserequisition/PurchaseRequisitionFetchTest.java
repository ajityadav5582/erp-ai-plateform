package com.erp.business.procurement.purchaserequisition;

import com.erp.business.procurement.purchaserequisition.domain.ApprovalAction;
import com.erp.business.procurement.purchaserequisition.domain.PurchaseRequisition;
import com.erp.business.procurement.purchaserequisition.domain.PurchaseRequisitionApproval;
import com.erp.business.procurement.purchaserequisition.domain.RequisitionPriority;
import com.erp.business.procurement.purchaserequisition.infrastructure.persistence.PurchaseRequisitionApprovalRepository;
import com.erp.business.procurement.purchaserequisition.infrastructure.persistence.PurchaseRequisitionRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the requisition read path against N+1 regressions.
 *
 * <p>Two fetches are asserted to be eager:
 * <ul>
 *   <li>the approver inbox query loads each step's requisition with the step
 *       ({@code JOIN FETCH a.requisition}) - without it the service's
 *       {@code getRequisition()} per step is one query per row; and</li>
 *   <li>the detail finder loads the lines with the header
 *       ({@code LEFT JOIN FETCH r.lines}).</li>
 * </ul>
 *
 * <p>The persistence context is cleared after seeding so the assertions observe a
 * fresh load: a managed entity left in the context would read as initialized even
 * without the fetch, hiding a regression. Real PostgreSQL is required because the
 * queries are JPQL over the Hibernate-generated schema.
 *
 * <p>Runs against {@link RequisitionSliceTestApplication}, a JPA-only slice that
 * keeps the service's web-security configuration out of the context.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class PurchaseRequisitionFetchTest {

    private static final Long TENANT = 1L;
    private static final Long COMPANY = 1L;
    private static final Long APPROVER_ID = 4242L;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
    }

    @Autowired private PurchaseRequisitionRepository requisitionRepository;
    @Autowired private PurchaseRequisitionApprovalRepository approvalRepository;
    @Autowired private EntityManager entityManager;

    private Long requisitionId;

    @BeforeEach
    void seed() {
        PurchaseRequisition requisition = PurchaseRequisition.draft(
                "PR-000001", 10L, 100L, LocalDate.of(2026, 1, 15), null,
                RequisitionPriority.NORMAL, "NPR", null, null);
        requisition.assignScope(TENANT, COMPANY);
        PurchaseRequisition saved = requisitionRepository.save(requisition);

        PurchaseRequisitionApproval step = PurchaseRequisitionApproval.builder()
                .sequenceNo(1)
                .approverId(APPROVER_ID)
                .approverRole("Manager")
                .action(ApprovalAction.PENDING)
                .requisition(saved)
                .build();
        step.assignScope(TENANT, COMPANY);
        approvalRepository.save(step);

        requisitionId = saved.getId();

        // Persist, then detach everything so the test observes a fresh load rather
        // than the already-managed instances from seeding.
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void inboxQuery_loadsRequisitionWithEachStep() {
        List<PurchaseRequisitionApproval> steps = approvalRepository.findActionableStepsFor(
                TENANT, COMPANY, APPROVER_ID, List.of("role.no-role-in-token"));

        assertThat(steps).hasSize(1);
        // JOIN FETCH a.requisition => the requisition is already initialized, so the
        // service's getRequisition() per step issues no extra SELECT.
        assertThat(steps).allSatisfy(step ->
                assertThat(Hibernate.isInitialized(step.getRequisition())).isTrue());
    }

    @Test
    void detailFinder_loadsLinesWithHeader() {
        Optional<PurchaseRequisition> loaded =
                requisitionRepository.findWithLinesByTenantIdAndCompanyIdAndId(
                        TENANT, COMPANY, requisitionId);

        assertThat(loaded).isPresent();
        // LEFT JOIN FETCH r.lines => the collection is loaded with the header, not
        // left as an uninitialized lazy bag.
        assertThat(Hibernate.isInitialized(loaded.get().getLines())).isTrue();
    }
}
