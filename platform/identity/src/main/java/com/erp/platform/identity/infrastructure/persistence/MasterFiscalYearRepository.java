package com.erp.platform.identity.infrastructure.persistence;

import com.erp.platform.identity.domain.MasterFiscalYear;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MasterFiscalYearRepository extends JpaRepository<MasterFiscalYear, Short> {
    List<MasterFiscalYear> findByIsActiveTrueOrderByDisplayOrderAsc();
    Optional<MasterFiscalYear> findByIdAndIsActiveTrue(Short id);
}
