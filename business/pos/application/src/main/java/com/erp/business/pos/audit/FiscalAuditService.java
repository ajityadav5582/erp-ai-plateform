package com.erp.business.pos.audit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FiscalAuditService {

    private final FiscalAuditLogRepository repository;

    public FiscalAuditService(FiscalAuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(Long tenantId, Long companyId, String actionType, String entityName,
                          String entityId, Long userId, String userName, String detailsJson, String ipAddress) {
        FiscalAuditLog log = new FiscalAuditLog();
        log.setTenantId(tenantId);
        log.setCompanyId(companyId);
        log.setActionType(actionType);
        log.setEntityName(entityName);
        log.setEntityId(entityId);
        log.setUserId(userId);
        log.setUserName(userName);
        log.setDetailsJson(detailsJson);
        log.setIpAddress(ipAddress);
        repository.save(log);
    }
}
