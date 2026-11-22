package com.erp.business.pos.restaurant.infrastructure.persistence;

import com.erp.business.pos.restaurant.domain.KitchenOrderLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KitchenOrderLineRepository extends JpaRepository<KitchenOrderLine, Long> {
    List<KitchenOrderLine> findByTenantIdAndCompanyIdAndKitchenOrderId(Long tenantId, Long companyId, Long kitchenOrderId);
    List<KitchenOrderLine> findByTenantIdAndCompanyIdAndKitchenOrderIdAndStation(Long tenantId, Long companyId, Long kitchenOrderId, String station);
    List<KitchenOrderLine> findByTenantIdAndCompanyIdAndStation(Long tenantId, Long companyId, String station);
}
