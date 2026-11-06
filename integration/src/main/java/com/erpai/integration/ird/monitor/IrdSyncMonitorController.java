package com.erpai.integration.ird.monitor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/integration/ird")
public class IrdSyncMonitorController {

    private final IrdSyncMonitorService service;

    public IrdSyncMonitorController(IrdSyncMonitorService service) {
        this.service = service;
    }

    @GetMapping("/companies/{companyId}/sync-status")
    public SyncStatusDto syncStatus(@PathVariable Long companyId) {
        return service.status(companyId);
    }

    @GetMapping("/companies/{companyId}/failures")
    public List<com.erpai.integration.ird.outbox.entity.IrdSyncOutboxEntity> failures(@PathVariable Long companyId) {
        return service.failures(companyId);
    }

    @PostMapping("/outbox/{outboxId}/retry-now")
    public void retryNow(@PathVariable UUID outboxId) {
        service.retryNow(outboxId);
    }

    @PostMapping("/companies/{companyId}/retry-all-failed")
    public void retryAllFailed(@PathVariable Long companyId) {
        service.retryAllFailed(companyId);
    }
}
