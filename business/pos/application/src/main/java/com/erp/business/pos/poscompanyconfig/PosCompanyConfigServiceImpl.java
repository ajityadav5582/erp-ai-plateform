package com.erp.business.pos.poscompanyconfig;

import com.erp.business.pos.domain.PosCompanyConfig;
import com.erp.business.pos.poscompanyconfig.dto.PosCompanyConfigRequest;
import com.erp.business.pos.poscompanyconfig.dto.PosCompanyConfigResponse;
import com.erp.business.pos.poscompanyconfig.infrastructure.persistence.PosCompanyConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PosCompanyConfigServiceImpl implements PosCompanyConfigService {

    private final PosCompanyConfigRepository repository;

    public PosCompanyConfigServiceImpl(PosCompanyConfigRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public PosCompanyConfigResponse get(Long tenantId, Long companyId) {
        return repository.findByTenantIdAndCompanyId(tenantId, companyId)
                .map(this::toResponse)
                .orElse(null);
    }

    @Override
    public PosCompanyConfigResponse createOrUpdate(Long tenantId, Long companyId, PosCompanyConfigRequest request) {
        PosCompanyConfig config = repository.findByTenantIdAndCompanyId(tenantId, companyId)
                .orElseGet(() -> {
                    PosCompanyConfig c = PosCompanyConfig.builder().build();
                    c.assignScope(tenantId, companyId);
                    return c;
                });

        if (request.enabledPaymentMethods() != null) {
            config.updateEnabledPaymentMethods(request.enabledPaymentMethods());
        }
        if (request.cashRounding() != null) {
            config.updateCashRounding(request.cashRounding());
        }
        if (request.serviceChargePercent() != null) {
            config.updateServiceChargePercent(request.serviceChargePercent());
        }
        if (request.tipHandling() != null) {
            config.updateTipHandling(request.tipHandling());
        }
        if (request.defaultRegisterId() != null) {
            config.updateDefaultRegisterId(request.defaultRegisterId());
        }
        if (request.defaultWarehouseId() != null) {
            config.updateDefaultWarehouseId(request.defaultWarehouseId());
        }
        if (request.priceListId() != null) {
            config.updatePriceListId(request.priceListId());
        }
        if (request.restaurantSettings() != null) {
            config.updateRestaurantSettings(request.restaurantSettings());
        }

        return toResponse(repository.save(config));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isPaymentMethodEnabled(Long tenantId, Long companyId, String paymentMethod) {
        if (paymentMethod == null || paymentMethod.isBlank()) return true;
        PosCompanyConfig config = repository.findByTenantIdAndCompanyId(tenantId, companyId).orElse(null);
        if (config == null || config.getEnabledPaymentMethods() == null || config.getEnabledPaymentMethods().isBlank()) {
            return true; // null/blank => all methods enabled (backward compatible)
        }
        for (String m : config.getEnabledPaymentMethods().split(",")) {
            if (m.trim().equalsIgnoreCase(paymentMethod.trim())) {
                return true;
            }
        }
        return false;
    }

    private PosCompanyConfigResponse toResponse(PosCompanyConfig c) {
        return new PosCompanyConfigResponse(
                c.getId(),
                c.getTenantId(),
                c.getCompanyId(),
                c.getEnabledPaymentMethods(),
                c.isCashRounding(),
                c.getServiceChargePercent(),
                c.getTipHandling(),
                c.getDefaultRegisterId(),
                c.getDefaultWarehouseId(),
                c.getPriceListId(),
                c.getRestaurantSettings()
        );
    }
}
