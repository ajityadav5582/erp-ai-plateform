package com.erp.platform.identity.interfaces.rest;

import com.erp.platform.identity.application.CurrentTenantProvider;
import com.erp.platform.identity.application.dto.CompanyResponse;
import com.erp.platform.identity.application.dto.CreateCompanyRequest;
import com.erp.platform.identity.domain.Company;
import com.erp.platform.identity.infrastructure.persistence.CompanyRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
                request.fiscalYearType(),
                request.vatRegistered()
        );
        return CompanyResponse.from(companyRepository.save(company));
    }
}
