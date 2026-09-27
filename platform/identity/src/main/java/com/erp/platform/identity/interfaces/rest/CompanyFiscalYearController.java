package com.erp.platform.identity.interfaces.rest;

import com.erp.platform.identity.application.CurrentTenantProvider;
import com.erp.platform.identity.application.dto.CompanyFiscalYearResponse;
import com.erp.platform.identity.application.dto.CreateCompanyFiscalYearRequest;
import com.erp.platform.identity.domain.MasterFiscalYear;
import com.erp.platform.identity.domain.CompanyFiscalYear;
import com.erp.platform.identity.domain.Company;
import com.erp.platform.identity.infrastructure.persistence.CompanyFiscalYearRepository;
import com.erp.platform.identity.infrastructure.persistence.CompanyRepository;
import com.erp.platform.identity.infrastructure.persistence.MasterFiscalYearRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/companies/{companyId}/fiscal-years")
@RequiredArgsConstructor
public class CompanyFiscalYearController {
    private final CompanyFiscalYearRepository fiscalYearRepository;
    private final CompanyRepository companyRepository;
    private final MasterFiscalYearRepository masterFiscalYearRepository;
    private final CurrentTenantProvider currentTenantProvider;

    @GetMapping
    public List<CompanyFiscalYearResponse> list(@PathVariable Long companyId) {
        Long tenantId = currentTenantProvider.getCurrentTenantId();
        requireCompany(tenantId, companyId);
        return fiscalYearRepository.findByTenantIdAndCompanyIdOrderByStartDateDesc(tenantId, companyId)
                .stream().map(CompanyFiscalYearResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyFiscalYearResponse create(@PathVariable Long companyId,
                                            @Valid @RequestBody CreateCompanyFiscalYearRequest request) {
        Long tenantId = currentTenantProvider.getCurrentTenantId();
        Company company = requireCompany(tenantId, companyId);
        if (!Objects.equals(company.getFiscalYearType(), request.calendarType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fiscal year calendar must match the company calendar type");
        }
        MasterFiscalYear master = masterFiscalYearRepository.findByIdAndIsActiveTrue(request.masterFiscalYearId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select an active master fiscal year"));
        if (fiscalYearRepository.existsByTenantIdAndCompanyIdAndCodeIgnoreCase(tenantId, companyId, master.getCode())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A fiscal year with this code already exists for this company");
        }
        LocalDate startAd;
        LocalDate endAd;
        try {
            startAd = LocalDate.parse(request.startDateAd());
            endAd = LocalDate.parse(request.endDateAd());
        } catch (RuntimeException invalidDate) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter valid AD start and end dates");
        }
        if (request.startDateBs().compareTo(request.endDateBs()) >= 0 || !startAd.isBefore(endAd)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fiscal year end dates must be after their start dates");
        }
        CompanyFiscalYear saved = fiscalYearRepository.save(CompanyFiscalYear.create(tenantId, company.getId(),
                master.getId(), request.name(), master.getCode(), request.startDateBs(), request.endDateBs(),
                startAd, endAd, request.calendarType()));
        return CompanyFiscalYearResponse.from(saved);
    }

    @PutMapping("/{fiscalYearId}/activate")
    @Transactional
    public CompanyFiscalYearResponse activate(@PathVariable Long companyId, @PathVariable Long fiscalYearId) {
        Long tenantId = currentTenantProvider.getCurrentTenantId();
        requireCompany(tenantId, companyId);
        CompanyFiscalYear selected = fiscalYearRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, fiscalYearId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fiscal year not found"));
        fiscalYearRepository.findByTenantIdAndCompanyIdOrderByStartDateDesc(tenantId, companyId)
                .forEach(CompanyFiscalYear::deactivate);
        selected.activate();
        return CompanyFiscalYearResponse.from(selected);
    }

    private Company requireCompany(Long tenantId, Long companyId) {
        return companyRepository.findByTenantIdAndId(tenantId, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));
    }
}
