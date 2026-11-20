package com.erp.platform.identity.infrastructure.persistence;

import com.erp.platform.identity.domain.LocalLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LocalLevelRepository extends JpaRepository<LocalLevel, Short> {
    List<LocalLevel> findByDistrictIdOrderByName(Short districtId);
}
