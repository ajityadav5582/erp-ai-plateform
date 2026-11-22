package com.erp.platform.identity.application;

import com.erp.platform.identity.application.dto.CreateCompanyRequest;
import com.erp.platform.identity.domain.BusinessType;
import com.erp.platform.identity.domain.CompanyType;
import com.erp.platform.identity.domain.FiscalYearType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CompanyValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private CreateCompanyRequest buildRequest(String panNumber) {
        return new CreateCompanyRequest(
                "Acme Nepal Pvt Ltd",
                panNumber,
                "Kathmandu",
                "+977-9812345678",
                "info@acme.com.np",
                CompanyType.PRIVATE_LIMITED,
                BusinessType.SERVICES,
                FiscalYearType.NEPALI_BS,
                false,
                null,
                null,
                null,
                "Ward 4, New Road",
                "4",
                "New Road",
                (short) 3,
                (short) 27,
                (short) 201
        );
    }

    @Test
    @DisplayName("Valid 9-digit PAN passes validation")
    void validNineDigitPanPasses() {
        CreateCompanyRequest request = buildRequest("601234567");
        Set<ConstraintViolation<CreateCompanyRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Less than 9 digits PAN fails validation")
    void shortPanFails() {
        CreateCompanyRequest request = buildRequest("12345");
        Set<ConstraintViolation<CreateCompanyRequest>> violations = validator.validate(request);
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Nepal PAN must be exactly 9 digits");
    }

    @Test
    @DisplayName("More than 9 digits PAN fails validation")
    void longPanFails() {
        CreateCompanyRequest request = buildRequest("1234567890");
        Set<ConstraintViolation<CreateCompanyRequest>> violations = validator.validate(request);
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Nepal PAN must be exactly 9 digits");
    }

    @Test
    @DisplayName("Non-numeric PAN fails validation")
    void nonNumericPanFails() {
        CreateCompanyRequest request = buildRequest("60123456A");
        Set<ConstraintViolation<CreateCompanyRequest>> violations = validator.validate(request);
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Nepal PAN must be exactly 9 digits");
    }

    @Test
    @DisplayName("CreateCompanyRequest with bank details passes validation")
    void validBankDetailsPasses() {
        CreateCompanyRequest request = new CreateCompanyRequest(
                "Acme Nepal Pvt Ltd",
                "601234567",
                "Kathmandu",
                "+977-9812345678",
                "info@acme.com.np",
                CompanyType.PRIVATE_LIMITED,
                BusinessType.SERVICES,
                FiscalYearType.NEPALI_BS,
                false,
                null,
                null,
                null,
                "Ward 4, New Road",
                "4",
                "New Road",
                (short) 3,
                (short) 27,
                (short) 201,
                "Nabil Bank Ltd",
                "Acme Nepal Pvt Ltd",
                "01234567890123",
                "New Road Branch",
                "https://storage.erpai.np/qr/acme-fonepay.png"
        );
        Set<ConstraintViolation<CreateCompanyRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
        assertThat(request.bankName()).isEqualTo("Nabil Bank Ltd");
        assertThat(request.bankAccountNumber()).isEqualTo("01234567890123");
        assertThat(request.qrCodeUrl()).isEqualTo("https://storage.erpai.np/qr/acme-fonepay.png");
    }
}

