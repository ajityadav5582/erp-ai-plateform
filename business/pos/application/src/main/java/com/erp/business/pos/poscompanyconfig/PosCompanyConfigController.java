package com.erp.business.pos.poscompanyconfig;

import com.erp.business.pos.poscompanyconfig.dto.PosCompanyConfigRequest;
import com.erp.business.pos.poscompanyconfig.dto.PosCompanyConfigResponse;
import com.erp.platform.business.context.BusinessContextAccessor;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pos/company-config")
@RequiredArgsConstructor
public class PosCompanyConfigController {

    private final PosCompanyConfigService service;
    private final BusinessContextAccessor context;

    @GetMapping
    public ResponseEntity<PosCompanyConfigResponse> get(
            @org.springframework.web.bind.annotation.RequestParam(required = false) Long tenantId,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Long companyId) {
        Long resolvedTenant = (tenantId != null) ? tenantId : context.getTenantId();
        Long resolvedCompany = (companyId != null) ? companyId : context.findCompanyId();
        if (resolvedCompany == null) {
            return ResponseEntity.badRequest().build();
        }
        PosCompanyConfigResponse response = service.get(resolvedTenant, resolvedCompany);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    @PutMapping
    public ResponseEntity<PosCompanyConfigResponse> upsert(
            @org.springframework.web.bind.annotation.RequestParam(required = false) Long tenantId,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Long companyId,
            @Valid @RequestBody PosCompanyConfigRequest request) {
        Long resolvedTenant = (tenantId != null) ? tenantId : context.getTenantId();
        Long resolvedCompany = (companyId != null) ? companyId : context.findCompanyId();
        if (resolvedCompany == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(service.createOrUpdate(resolvedTenant, resolvedCompany, request));
    }
}
