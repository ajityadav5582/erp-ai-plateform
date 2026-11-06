package com.erp.business.pos.restaurant.infrastructure.persistence;

import com.erp.business.pos.restaurant.domain.KitchenOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KitchenOrderRepository extends JpaRepository<KitchenOrder, Long> {
    List<KitchenOrder> findByTenantIdAndCompanyIdAndTableId(Long tenantId, Long companyId, Long tableId);
    List<KitchenOrder> findByTenantIdAndCompanyId(Long tenantId, Long companyId);
}
