package com.erpai.integration.ird.monitor;

import com.erpai.integration.ird.outbox.entity.IrdSyncOutboxEntity;
import com.erpai.integration.ird.outbox.entity.IrdSyncOutboxStatus;
import com.erpai.integration.ird.outbox.repository.IrdSyncOutboxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class IrdSyncMonitorService {

    private final IrdSyncOutboxRepository repository;

    public IrdSyncMonitorService(IrdSyncOutboxRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public SyncStatusDto status(Long companyId) {
        long synced = repository.countByCompanyIdAndSyncStatus(companyId, IrdSyncOutboxStatus.SYNCED);
        long simulated = repository.countByCompanyIdAndSyncStatus(companyId, IrdSyncOutboxStatus.SIMULATED);
        long pending = repository.countByCompanyIdAndSyncStatus(companyId, IrdSyncOutboxStatus.PENDING);
        long failed = repository.countByCompanyIdAndSyncStatus(companyId, IrdSyncOutboxStatus.FAILED);
        long dead = repository.countByCompanyIdAndSyncStatus(companyId, IrdSyncOutboxStatus.DEAD_LETTER);
        Instant last = null;
        return new SyncStatusDto(synced, simulated, pending, failed, dead, last);
    }

    @Transactional(readOnly = true)
    public List<IrdSyncOutboxEntity> failures(Long companyId) {
        return repository.findByCompanyIdAndSyncStatusIn(companyId,
                List.of(IrdSyncOutboxStatus.FAILED, IrdSyncOutboxStatus.DEAD_LETTER));
    }

    @Transactional
    public void retryNow(UUID outboxId) {
        repository.findById(outboxId).ifPresent(e -> {
            e.setRetryCount(0);
            e.setNextRetryAt(Instant.now());
            e.setSyncStatus(IrdSyncOutboxStatus.PENDING);
            repository.save(e);
        });
    }

    @Transactional
    public void retryAllFailed(Long companyId) {
        List<IrdSyncOutboxEntity> failed = failures(companyId);
        for (IrdSyncOutboxEntity e : failed) {
            e.setRetryCount(0);
            e.setNextRetryAt(Instant.now());
            e.setSyncStatus(IrdSyncOutboxStatus.PENDING);
            repository.save(e);
        }
    }
}
