package com.erp.platform.identity.infrastructure.persistence;

import com.erp.platform.identity.domain.District;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DistrictRepository extends JpaRepository<District, Short> {
    List<District> findByProvinceIdOrderByName(Short provinceId);
}
