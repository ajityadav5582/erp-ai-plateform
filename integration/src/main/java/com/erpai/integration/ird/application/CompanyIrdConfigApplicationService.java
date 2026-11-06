package com.erpai.integration.ird.application;

import com.erpai.integration.ird.adapter.in.web.dto.CompanyIrdConfigResponse;
import com.erpai.integration.ird.domain.CompanyIrdConfig;
import com.erpai.integration.ird.domain.IrdEnvironment;
import com.erpai.integration.ird.domain.SellerPan;
import com.erpai.integration.ird.port.in.ConfigureCompanyIrdUseCase;
import com.erpai.integration.ird.port.in.VerifyIrdConnectionUseCase;
import com.erpai.integration.ird.port.out.CompanyIrdConfigRepositoryPort;
import com.erpai.integration.ird.port.out.IrdCryptoPort;
import com.erpai.integration.ird.port.out.IrdFiscalSyncPort;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class CompanyIrdConfigApplicationService implements ConfigureCompanyIrdUseCase, VerifyIrdConnectionUseCase {

    private final CompanyIrdConfigRepositoryPort repositoryPort;
    private final IrdCryptoPort cryptoPort;
    private final IrdFiscalSyncPort fiscalSyncPort;

    public CompanyIrdConfigApplicationService(CompanyIrdConfigRepositoryPort repositoryPort,
                                             IrdCryptoPort cryptoPort,
                                             IrdFiscalSyncPort fiscalSyncPort) {
        this.repositoryPort = repositoryPort;
        this.cryptoPort = cryptoPort;
        this.fiscalSyncPort = fiscalSyncPort;
    }

    @Override
    public CompanyIrdConfig configure(Long tenantId, Long companyId, SellerPan sellerPan, String apiUsername,
                                      String apiPassword, IrdEnvironment environment) {
        String encrypted = cryptoPort.encrypt(apiPassword);
        CompanyIrdConfig config = new CompanyIrdConfig(tenantId, companyId, sellerPan, apiUsername, encrypted, environment);
        return repositoryPort.save(config);
    }

    @Override
    public CompanyIrdConfig update(Long tenantId, Long companyId, SellerPan sellerPan, String apiUsername,
                                   String apiPassword, IrdEnvironment environment) {
        Optional<CompanyIrdConfig> existing = repositoryPort.findByTenantIdAndCompanyId(tenantId, companyId);
        if (existing.isPresent()) {
            CompanyIrdConfig config = existing.get();
            config.updateCredentials(apiUsername, cryptoPort.encrypt(apiPassword), sellerPan, environment);
            return repositoryPort.save(config);
        }
        return configure(tenantId, companyId, sellerPan, apiUsername, apiPassword, environment);
    }

    @Override
    public boolean verify(Long tenantId, Long companyId) {
        boolean ok = fiscalSyncPort.verifyConnection(tenantId, companyId);
        Optional<CompanyIrdConfig> config = repositoryPort.findByTenantIdAndCompanyId(tenantId, companyId);
        config.ifPresent(c -> {
            if (ok) c.markVerified(); else c.markFailed("Connection verification failed");
            repositoryPort.save(c);
        });
        return ok;
    }
}
