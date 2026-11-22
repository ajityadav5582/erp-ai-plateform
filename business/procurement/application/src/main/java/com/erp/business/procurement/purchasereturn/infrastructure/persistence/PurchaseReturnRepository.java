package com.erp.business.procurement.purchasereturn.infrastructure.persistence;

import com.erp.business.procurement.purchasereturn.domain.PurchaseReturn;
import com.erp.business.procurement.purchasereturn.domain.PurchaseReturnStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PurchaseReturnRepository extends JpaRepository<PurchaseReturn, Long> {

    Optional<PurchaseReturn> findByTenantIdAndCompanyIdAndId(
            Long tenantId, Long companyId, Long id);

    @Query("SELECT r FROM PurchaseReturn r LEFT JOIN FETCH r.lines "
         + "WHERE r.tenantId = :tenantId AND r.companyId = :companyId AND r.id = :id")
    Optional<PurchaseReturn> findWithLinesByTenantIdAndCompanyIdAndId(
            @Param("tenantId") Long tenantId,
            @Param("companyId") Long companyId,
            @Param("id") Long id);

    default Optional<PurchaseReturn> findWithLinesById(Long tenantId, Long companyId, Long id) {
        return findWithLinesByTenantIdAndCompanyIdAndId(tenantId, companyId, id);
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM PurchaseReturn r WHERE r.tenantId = :tenantId "
         + "AND r.companyId = :companyId AND r.id = :id")
    Optional<PurchaseReturn> findByIdForUpdate(
            @Param("tenantId") Long tenantId,
            @Param("companyId") Long companyId,
            @Param("id") Long id);

    Optional<PurchaseReturn> findByTenantIdAndCompanyIdAndDebitNoteNumber(
            Long tenantId, Long companyId, String debitNoteNumber);

    Page<PurchaseReturn> findByTenantIdAndCompanyId(
            Long tenantId, Long companyId, Pageable pageable);

    Page<PurchaseReturn> findByTenantIdAndCompanyIdAndStatus(
            Long tenantId, Long companyId, PurchaseReturnStatus status, Pageable pageable);

    Page<PurchaseReturn> findByTenantIdAndCompanyIdAndPurchaseInvoiceId(
            Long tenantId, Long companyId, Long purchaseInvoiceId, Pageable pageable);

    Page<PurchaseReturn> findByTenantIdAndCompanyIdAndSupplierId(
            Long tenantId, Long companyId, Long supplierId, Pageable pageable);

    java.util.List<PurchaseReturn> findByTenantIdAndCompanyIdAndSupplierId(
            Long tenantId, Long companyId, Long supplierId);

    @Query("SELECT r FROM PurchaseReturn r WHERE r.tenantId = :tenantId AND r.companyId = :companyId "
         + "AND (LOWER(r.debitNoteNumber) LIKE LOWER(CONCAT('%', :term, '%')) "
         + "OR LOWER(r.invoiceNumber) LIKE LOWER(CONCAT('%', :term, '%')) "
         + "OR LOWER(r.supplierName) LIKE LOWER(CONCAT('%', :term, '%')) "
         + "OR LOWER(r.supplierCode) LIKE LOWER(CONCAT('%', :term, '%')))")
    Page<PurchaseReturn> searchByTerm(
            @Param("tenantId") Long tenantId,
            @Param("companyId") Long companyId,
            @Param("term") String term,
            Pageable pageable);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(r.debitNoteNumber, :prefixLength + 1) AS java.lang.Long)), 0) "
         + "FROM PurchaseReturn r "
         + "WHERE r.tenantId = :tenantId AND r.companyId = :companyId "
         + "AND r.debitNoteNumber LIKE CONCAT(:prefix, '%')")
    Long findHighestIssuedNumber(@Param("tenantId") Long tenantId,
                                 @Param("companyId") Long companyId,
                                 @Param("prefix") String prefix,
                                 @Param("prefixLength") int prefixLength);
}

