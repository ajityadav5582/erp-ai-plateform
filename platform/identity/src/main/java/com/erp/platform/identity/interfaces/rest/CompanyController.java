package com.erp.platform.identity.interfaces.rest;

import com.erp.platform.identity.application.CurrentTenantProvider;
import com.erp.platform.identity.application.dto.CompanyResponse;
import com.erp.platform.identity.application.dto.CreateCompanyRequest;
import com.erp.platform.identity.application.dto.UpdateCompanyRequest;
import com.erp.platform.identity.domain.Company;
import com.erp.platform.identity.infrastructure.persistence.CompanyRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/identity/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyRepository companyRepository;
    private final CurrentTenantProvider currentTenantProvider;

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
        if (companyRepository.existsByTenantIdAndPanNumberIgnoreCase(tenantId, request.panNumber().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A company with this PAN number already exists");
        }
        if (companyRepository.existsByTenantIdAndCompanyNameIgnoreCase(tenantId, request.companyName().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A company with this name already exists");
        }
        String companyCode = "PAN-" + request.panNumber().trim().toUpperCase().replaceAll("[^A-Z0-9]", "");
        if (companyCode.length() > 50) {
            companyCode = companyCode.substring(0, 50);
        }
        Company company = Company.create(
                tenantId,
                companyCode,
                request.companyName(),
                request.panNumber(),
                request.city(),
                request.phone(),
                request.email(),
                 request.companyType(),
                 request.businessType(),
                 request.fiscalYearType(),
                 request.vatRegistered()
        );
        return CompanyResponse.from(companyRepository.save(company));
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
        return ResponseEntity.ok(CompanyResponse.from(companyRepository.save(company)));
    }
}
