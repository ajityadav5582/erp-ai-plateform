package com.erp.business.inventory.infrastructure.persistence;

import com.erp.business.inventory.domain.Supplier;
import com.erp.business.inventory.domain.SupplierType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    Optional<Supplier> findByTenantIdAndId(Long tenantId, Long id);

    Optional<Supplier> findByTenantIdAndCompanyIdAndCode(Long tenantId, Long companiesId, String code);

    Optional<Supplier> findByTenantIdAndCompanyIdAndName(Long tenantId, Long companiesId, String name);

    List<Supplier> findByTenantIdAndCompanyId(Long tenantId, Long companiesId);

    Page<Supplier> findByTenantIdAndCompanyId(Long tenantId, Long companiesId, Pageable pageable);

    List<Supplier> findByTenantIdAndCompanyIdAndIsActiveTrue(Long tenantId, Long companiesId);

    List<Supplier> findByTenantIdAndCompanyIdAndType(Long tenantId, Long companiesId, SupplierType type);

    boolean existsByTenantIdAndCompanyIdAndCode(Long tenantId, Long companiesId, String code);

    boolean existsByTenantIdAndCompanyIdAndName(Long tenantId, Long companiesId, String name);

    boolean existsByTenantIdAndCompanyIdAndId(Long tenantId, Long companiesId, Long id);

    long countByTenantIdAndCompanyId(Long tenantId, Long companiesId);

    long countByTenantIdAndCompanyIdAndIsActiveTrue(Long tenantId, Long companiesId);

    @Query("SELECT s FROM Supplier s WHERE s.tenantId = :tenantId AND s.companyId = :companiesId AND LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Supplier> findByTenantIdAndCompanyIdAndNameContainingIgnoreCase(
            @Param("tenantId") Long tenantId,
            @Param("companiesId") Long companiesId,
            @Param("name") String name,
            Pageable pageable);
}
