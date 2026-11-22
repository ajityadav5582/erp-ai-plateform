package com.erp.platform.identity.interfaces.rest;

import com.erp.platform.identity.application.CompanyService;
import com.erp.platform.identity.application.CurrentTenantProvider;
import com.erp.platform.identity.application.dto.CompanyResponse;
import com.erp.platform.identity.application.dto.CreateCompanyRequest;
import com.erp.platform.identity.application.dto.UpdateCompanyRequest;
import com.erp.platform.identity.domain.Company;
import com.erp.platform.identity.infrastructure.persistence.CompanyRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/identity/companies")
@RequiredArgsConstructor
@Slf4j
public class CompanyController {

    private final CompanyRepository companyRepository;
    private final CompanyService companyService;
    private final CurrentTenantProvider currentTenantProvider;

    @GetMapping("/{id}")
    public ResponseEntity<CompanyResponse> getCompany(@PathVariable Long id) {
        Long tenantId = currentTenantProvider.getCurrentTenantId();
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));
        if (!tenantId.equals(company.getTenantId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Company does not belong to this tenant");
        }
        return ResponseEntity.ok(CompanyResponse.from(company));
    }

    @GetMapping
    public List<CompanyResponse> listCompanies() {
        Long tenantId = currentTenantProvider.getCurrentTenantId();
        return companyRepository.findByTenantId(tenantId).stream()
                .map(CompanyResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyResponse createCompany(@Valid @RequestBody CreateCompanyRequest request) {
        Long tenantId = currentTenantProvider.getCurrentTenantId();
        try {
            return companyService.createCompany(tenantId, null, request);
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompanyResponse> updateCompany(
            @PathVariable Long id, @Valid @RequestBody UpdateCompanyRequest request) {
        Long tenantId = currentTenantProvider.getCurrentTenantId();
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));
        if (!tenantId.equals(company.getTenantId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Company does not belong to this tenant");
        }
        if (request.companyName() != null && !request.companyName().isBlank()
                && companyRepository.existsByTenantIdAndCompanyNameIgnoreCase(tenantId, request.companyName().trim())
                && !request.companyName().trim().equalsIgnoreCase(company.getCompanyName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A company with this name already exists");
        }
        company.updateCompanyName(request.companyName());
        company.updatePanNumber(request.panNumber());
        company.updateCity(request.city());
        company.updateDetails(request.legalName(), request.email(), request.phone(), request.website(), request.address());
        company.updateCompanyType(request.companyType());
        company.updateBusinessType(request.businessType());
        company.updateFiscalYearType(request.fiscalYearType());
        company.updateVatRegistered(request.vatRegistered());
        company.updateVatRegistration(request.vatRegistrationNumber(), request.vatEffectiveFrom(), request.vatEffectiveTo());
        company.updateWardNo(request.wardNo());
        company.updateToleStreet(request.toleStreet());
        company.updateLocation(request.provinceId(), request.districtId(), request.localLevelId(), request.wardNo(), request.toleStreet());
        company.updateBankDetails(request.bankName(), request.bankAccountName(), request.bankAccountNumber(), request.bankBranch(), request.qrCodeUrl());
        Company saved = companyRepository.save(company);
        log.info("COMPLIANCE AUDIT: Company settings updated for companyId={}, tenantId={}, panNumber={}, vatRegNumber={}, bank={}",
                saved.getId(), tenantId, saved.getPanNumber(), saved.getVatRegistrationNumber(), saved.getBankName());
        return ResponseEntity.ok(CompanyResponse.from(saved));
    }
}
