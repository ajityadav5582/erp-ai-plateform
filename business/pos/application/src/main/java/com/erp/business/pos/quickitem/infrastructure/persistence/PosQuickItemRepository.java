package com.erp.business.pos.quickitem.infrastructure.persistence;

import com.erp.business.pos.domain.PosQuickItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Persistence port for the {@link PosQuickItem} aggregate. Every finder is
 * tenant- and company-scoped so a query can never leak across a company boundary.
 */
public interface PosQuickItemRepository extends JpaRepository<PosQuickItem, Long> {

    Optional<PosQuickItem> findByTenantIdAndCompanyIdAndId(Long tenantId, Long companyId, Long id);

    Page<PosQuickItem> findByTenantIdAndCompanyId(Long tenantId, Long companyId, Pageable pageable);

    Page<PosQuickItem> findByTenantIdAndCompanyIdAndRegisterId(Long tenantId, Long companyId,
                                                               Long registerId, Pageable pageable);

    Page<PosQuickItem> findByTenantIdAndCompanyIdAndRegisterIdIsNull(Long tenantId, Long companyId,
                                                                     Pageable pageable);

    Page<PosQuickItem> findByTenantIdAndCompanyIdAndIsActive(Long tenantId, Long companyId,
                                                             boolean isActive, Pageable pageable);

    /**
     * The terminal grid load: every company-shared tile plus the tiles pinned to
     * one register, active first, then by grid position. One query renders the
     * whole grid without a second round-trip.
     */
    @Query("SELECT q FROM PosQuickItem q WHERE q.tenantId = :tenantId AND q.companyId = :companyId "
            + "AND (q.registerId IS NULL OR q.registerId = :registerId) "
            + "ORDER BY q.isActive DESC, q.sortOrder ASC, q.gridRow ASC, q.gridCol ASC")
    List<PosQuickItem> findGridForRegister(@Param("tenantId") Long tenantId,
                                           @Param("companyId") Long companyId,
                                           @Param("registerId") Long registerId);

    @Query("SELECT q FROM PosQuickItem q WHERE q.tenantId = :tenantId AND q.companyId = :companyId "
            + "AND (LOWER(q.itemName) LIKE LOWER(CONCAT('%', :term, '%')) "
            + "OR LOWER(q.itemCode) LIKE LOWER(CONCAT('%', :term, '%')))")
    Page<PosQuickItem> search(@Param("tenantId") Long tenantId,
                              @Param("companyId") Long companyId,
                              @Param("term") String term,
                              Pageable pageable);
}
