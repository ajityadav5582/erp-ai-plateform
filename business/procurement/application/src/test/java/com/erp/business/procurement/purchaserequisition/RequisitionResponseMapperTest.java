package com.erp.business.procurement.purchaserequisition;

import com.erp.business.procurement.purchaserequisition.domain.PurchaseRequisition;
import com.erp.business.procurement.purchaserequisition.domain.PurchaseRequisitionLine;
import com.erp.business.procurement.purchaserequisition.domain.PurchaseRequisitionStatus;
import com.erp.business.procurement.purchaserequisition.domain.RequisitionPriority;
import com.erp.business.procurement.purchaserequisition.dto.RequisitionDetailResponse;
import com.erp.business.procurement.purchaserequisition.dto.RequisitionListResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RequisitionResponseMapper}.
 *
 * <p>The mapper is pure - it only reads aggregate getters and derived-value
 * methods - so it is exercised directly with no Spring context or database. This
 * is the payoff of extracting it from the service: presentation shaping is now
 * pinned down on its own.
 */
class RequisitionResponseMapperTest {

    private final RequisitionResponseMapper mapper = new RequisitionResponseMapper();

    private PurchaseRequisition draftWithOneLine() {
        PurchaseRequisition requisition = PurchaseRequisition.draft(
                "PR-000123", 7L, 55L, LocalDate.of(2026, 2, 1), null,
                RequisitionPriority.NORMAL, "NPR", 3L, 9L);
        requisition.assignScope(1L, 1L);
        requisition.addLine(PurchaseRequisitionLine.create(
                1, 100L, "ITM-1", "Widget", "PCS", 2L,
                new BigDecimal("5"), new BigDecimal("10.00"), new BigDecimal("13.00"), "note"));
        requisition.recalculateTotals();
        return requisition;
    }

    @Test
    void toDetail_mapsHeaderLinesAndEmptyCollections() {
        RequisitionDetailResponse dto = mapper.toDetail(draftWithOneLine());

        assertThat(dto.prNumber()).isEqualTo("PR-000123");
        assertThat(dto.tenantId()).isEqualTo(1L);
        assertThat(dto.companyId()).isEqualTo(1L);
        assertThat(dto.branchId()).isEqualTo(7L);
        assertThat(dto.status()).isEqualTo(PurchaseRequisitionStatus.DRAFT);
        assertThat(dto.priority()).isEqualTo(RequisitionPriority.NORMAL);
        assertThat(dto.currency()).isEqualTo("NPR");

        assertThat(dto.lines()).hasSize(1);
        assertThat(dto.lines().get(0).itemName()).isEqualTo("Widget");
        assertThat(dto.lines().get(0).itemCode()).isEqualTo("ITM-1");
        assertThat(dto.lines().get(0).uomCode()).isEqualTo("PCS");
        assertThat(dto.lines().get(0).requestedQuantity()).isEqualByComparingTo("5");

        // A fresh draft has no approvals and no status history yet.
        assertThat(dto.approvals()).isEmpty();
        assertThat(dto.statusHistory()).isEmpty();
    }

    @Test
    void toList_mapsSummaryAndDerivesOutstandingValue() {
        RequisitionListResponse dto = mapper.toList(draftWithOneLine());

        assertThat(dto.prNumber()).isEqualTo("PR-000123");
        assertThat(dto.status()).isEqualTo(PurchaseRequisitionStatus.DRAFT);
        // The estimate is derived from the requested line; outstanding value is the
        // approved total (zero for a draft) less what is ordered (also zero).
        assertThat(dto.estimatedTotalAmount()).isNotNull().isGreaterThan(BigDecimal.ZERO);
        assertThat(dto.outstandingValue()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
