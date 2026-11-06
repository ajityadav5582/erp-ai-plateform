package com.erpai.integration.ird.adapter.in.web;

import com.erpai.integration.ird.adapter.in.web.dto.CompanyIrdConfigResponse;
import com.erpai.integration.ird.adapter.in.web.dto.ConfigureCompanyIrdRequest;
import com.erpai.integration.ird.application.CompanyIrdConfigApplicationService;
import com.erpai.integration.ird.domain.CompanyIrdConfig;
import com.erpai.integration.ird.domain.SellerPan;
import com.erpai.integration.ird.port.out.CompanyIrdConfigRepositoryPort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ird/company-config")
public class CompanyIrdConfigController {

    private final CompanyIrdConfigApplicationService service;
    private final CompanyIrdConfigRepositoryPort repositoryPort;

    public CompanyIrdConfigController(CompanyIrdConfigApplicationService service,
                                     CompanyIrdConfigRepositoryPort repositoryPort) {
        this.service = service;
        this.repositoryPort = repositoryPort;
    }

    @GetMapping
    public ResponseEntity<CompanyIrdConfigResponse> get(
            @RequestParam Long tenantId,
            @RequestParam Long companyId) {
        return repositoryPort.findByTenantIdAndCompanyId(tenantId, companyId)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<CompanyIrdConfigResponse> configure(
            @RequestParam Long tenantId,
            @RequestParam Long companyId,
            @RequestBody ConfigureCompanyIrdRequest request) {
        CompanyIrdConfig config = service.configure(
                tenantId, companyId,
                SellerPan.of(request.sellerPan()),
                request.apiUsername(),
                request.apiPassword(),
                request.environment());
        return ResponseEntity.ok(toResponse(config));
    }

    @PutMapping
    public ResponseEntity<CompanyIrdConfigResponse> update(
            @RequestParam Long tenantId,
            @RequestParam Long companyId,
            @RequestBody ConfigureCompanyIrdRequest request) {
        CompanyIrdConfig config = service.update(
                tenantId, companyId,
                SellerPan.of(request.sellerPan()),
                request.apiUsername(),
                request.apiPassword(),
                request.environment());
        return ResponseEntity.ok(toResponse(config));
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify(
            @RequestParam Long tenantId,
            @RequestParam Long companyId) {
        boolean ok = service.verify(tenantId, companyId);
        return ResponseEntity.ok(Map.of("verified", ok));
    }

    @PostMapping("/activate-cbms")
    public ResponseEntity<CompanyIrdConfigResponse> activateCbms(
            @RequestParam Long tenantId,
            @RequestParam Long companyId) {
        CompanyIrdConfig config = repositoryPort.findByTenantIdAndCompanyId(tenantId, companyId)
                .orElseThrow(() -> new IllegalStateException("Config not found"));
        config.activateCbms();
        return ResponseEntity.ok(toResponse(repositoryPort.save(config)));
    }

    @PostMapping("/deactivate-cbms")
    public ResponseEntity<CompanyIrdConfigResponse> deactivateCbms(
            @RequestParam Long tenantId,
            @RequestParam Long companyId) {
        CompanyIrdConfig config = repositoryPort.findByTenantIdAndCompanyId(tenantId, companyId)
                .orElseThrow(() -> new IllegalStateException("Config not found"));
        config.deactivateCbms();
        return ResponseEntity.ok(toResponse(repositoryPort.save(config)));
    }

    private CompanyIrdConfigResponse toResponse(CompanyIrdConfig c) {
        return new CompanyIrdConfigResponse(
                c.getId(), c.getTenantId(), c.getCompanyId(),
                c.getSellerPan().value(), c.getApiUsername(), c.getEnvironment(),
                c.isCbmsActive(), c.getVerificationStatus(), c.getLastVerifiedAt(),
                c.getLastErrorMessage(), c.getCreatedAt(), c.getUpdatedAt());
    }
}
