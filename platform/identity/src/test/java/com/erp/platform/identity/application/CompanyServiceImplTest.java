package com.erp.platform.identity.application;

import com.erp.platform.identity.application.dto.CompanyResponse;
import com.erp.platform.identity.application.dto.CreateCompanyRequest;
import com.erp.platform.identity.domain.*;
import com.erp.platform.identity.infrastructure.persistence.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyServiceImplTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserBranchRepository userBranchRepository;

    @Mock
    private CurrentTenantProvider currentTenantProvider;

    @Mock
    private MasterFiscalYearRepository masterFiscalYearRepository;

    @Mock
    private CompanyFiscalYearRepository companyFiscalYearRepository;

    @InjectMocks
    private CompanyServiceImpl companyService;

    @Test
    @DisplayName("createCompany automatically bootstraps and activates current fiscal year")
    void createCompanyBootstrapsFiscalYear() {
        Long tenantId = 1L;
        Long userId = 10L;

        CreateCompanyRequest request = new CreateCompanyRequest(
                "Himalayan Tech Pvt Ltd",
                "601234567",
                "Kathmandu",
                "+977-9812345678",
                "info@himalayan.com.np",
                CompanyType.PRIVATE_LIMITED,
                BusinessType.SERVICES,
                FiscalYearType.NEPALI_BS,
                true,
                "601234567",
                null,
                null,
                "New Road",
                "22",
                "Main Street",
                (short) 3,
                (short) 27,
                (short) 201
        );

        when(currentTenantProvider.getCurrentTenantId()).thenReturn(tenantId);
        when(companyRepository.existsByTenantIdAndPanNumberIgnoreCase(tenantId, "601234567")).thenReturn(false);
        when(companyRepository.existsByTenantIdAndCompanyNameIgnoreCase(tenantId, "Himalayan Tech Pvt Ltd")).thenReturn(false);

        Company savedCompany = Company.create(
                tenantId,
                "PAN-601234567",
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
        org.springframework.test.util.ReflectionTestUtils.setField(savedCompany, "id", 100L);
        when(companyRepository.save(any(Company.class))).thenReturn(savedCompany);

        Branch savedBranch = Branch.create(tenantId, 100L, "HO", "Head Office");
        org.springframework.test.util.ReflectionTestUtils.setField(savedBranch, "id", 1L);
        when(branchRepository.save(any(Branch.class))).thenReturn(savedBranch);

        MasterFiscalYear mockMasterFy = MasterFiscalYear.create(
                "FY 2081/82",
                "2081-82",
                "2081-04-01",
                "2082-03-31",
                LocalDate.now().minusMonths(2),
                LocalDate.now().plusMonths(10),
                (short) 1
        );
        when(masterFiscalYearRepository.findByIsActiveTrueOrderByDisplayOrderAsc()).thenReturn(List.of(mockMasterFy));

        CompanyFiscalYear mockSavedCfy = CompanyFiscalYear.create(
                tenantId, 1L, mockMasterFy.getId(), mockMasterFy.getName(), mockMasterFy.getCode(),
                mockMasterFy.getStartDateBs(), mockMasterFy.getEndDateBs(),
                mockMasterFy.getStartDateAd(), mockMasterFy.getEndDateAd(),
                FiscalYearType.NEPALI_BS
        );
        mockSavedCfy.activate();
        when(companyFiscalYearRepository.save(any(CompanyFiscalYear.class))).thenReturn(mockSavedCfy);

        CompanyResponse response = companyService.createCompany(null, userId, request);

        assertThat(response).isNotNull();
        assertThat(response.companyName()).isEqualTo("Himalayan Tech Pvt Ltd");

        ArgumentCaptor<CompanyFiscalYear> cfyCaptor = ArgumentCaptor.forClass(CompanyFiscalYear.class);
        verify(companyFiscalYearRepository).save(cfyCaptor.capture());

        CompanyFiscalYear captured = cfyCaptor.getValue();
        assertThat(captured.getCode()).isEqualTo("2081-82");
        assertThat(captured.isActive()).isTrue();
        assertThat(captured.getCalendarType()).isEqualTo(FiscalYearType.NEPALI_BS);
    }

    @Test
    @DisplayName("Company updateBankDetails correctly sets and trims bank details")
    void companyUpdateBankDetailsSetsAndTrimsFields() {
        Company company = Company.create(1L, "TEST-01", "Test Company");
        assertThat(company.getBankName()).isNull();

        company.updateBankDetails("  Nabil Bank Ltd  ", "  Test Pvt Ltd  ", " 1234567890 ", " New Road ", " https://qr.image/1.png ");
        assertThat(company.getBankName()).isEqualTo("Nabil Bank Ltd");
        assertThat(company.getBankAccountName()).isEqualTo("Test Pvt Ltd");
        assertThat(company.getBankAccountNumber()).isEqualTo("1234567890");
        assertThat(company.getBankBranch()).isEqualTo("New Road");
        assertThat(company.getQrCodeUrl()).isEqualTo("https://qr.image/1.png");

        // Clear via empty string
        company.updateBankDetails("", "   ", null, "", null);
        assertThat(company.getBankName()).isNull();
        assertThat(company.getBankAccountName()).isNull();
        assertThat(company.getBankAccountNumber()).isEqualTo("1234567890"); // null leaves existing
        assertThat(company.getBankBranch()).isNull();
        assertThat(company.getQrCodeUrl()).isEqualTo("https://qr.image/1.png"); // null leaves existing
    }
}
