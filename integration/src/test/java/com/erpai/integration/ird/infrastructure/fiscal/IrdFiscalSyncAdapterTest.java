package com.erpai.integration.ird.infrastructure.fiscal;

import com.erpai.integration.ird.config.IrdProperties;
import com.erpai.integration.ird.domain.CompanyIrdConfig;
import com.erpai.integration.ird.domain.IrdEnvironment;
import com.erpai.integration.ird.domain.SellerPan;
import com.erpai.integration.ird.port.FiscalBillCommand;
import com.erpai.integration.ird.port.FiscalCreditNoteCommand;
import com.erpai.integration.ird.port.IrdSyncResult;
import com.erpai.integration.ird.port.out.IrdCryptoPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class IrdFiscalSyncAdapterTest {

    @Mock
    private IrdCryptoPort cryptoPort;

    @Mock
    private RestClient restClient;

    private IrdProperties properties;
    private IrdFiscalSyncAdapter adapter;

    @BeforeEach
    void setUp() {
        properties = new IrdProperties();
        properties.setEnabled(true);
        properties.setUsername("static_user");
        properties.setPassword("static_pass");
        adapter = new IrdFiscalSyncAdapter(properties, restClient, cryptoPort);
    }

    @Test
    void syncSalesBill_whenDisabledOrEmptyUser_simulatesSuccess() {
        properties.setEnabled(false);
        FiscalBillCommand command = new FiscalBillCommand(
                1L, 10L, "INV-001", "2081/82", "300123456", "300654321", "Test Buyer",
                BigDecimal.valueOf(1130), BigDecimal.valueOf(1000), BigDecimal.valueOf(130),
                BigDecimal.ZERO, true, LocalDate.now()
        );

        IrdSyncResult result = adapter.syncSalesBill(command);

        assertTrue(result.isSuccess());
        assertTrue(result.isSimulated());
        assertEquals(200, result.getStatusCode());
    }

    @Test
    void syncSalesBill_withCompanyConfig_simulatesWhenCbmsInactive() {
        CompanyIrdConfig config = new CompanyIrdConfig(
                1L, 10L, SellerPan.of("123456789"), "company_user", "enc_pass_123", IrdEnvironment.SANDBOX
        );
        assertFalse(config.isCbmsActive());

        FiscalBillCommand command = new FiscalBillCommand(
                1L, 10L, "INV-002", "2081/82", "123456789", "300654321", "Test Buyer",
                BigDecimal.valueOf(1130), BigDecimal.valueOf(1000), BigDecimal.valueOf(130),
                BigDecimal.ZERO, true, LocalDate.now()
        );

        IrdSyncResult result = adapter.syncSalesBill(command, config);
        assertTrue(result.isSuccess());
        assertTrue(result.isSimulated());
    }

    @Test
    void syncCreditNote_whenStaticFallback_simulatesWhenDisabled() {
        properties.setEnabled(false);
        FiscalCreditNoteCommand command = new FiscalCreditNoteCommand(
                1L, 10L, "CN-001", "2081-08-05", "INV-001", "2081/82",
                "300654321", "Test Customer", BigDecimal.valueOf(113),
                BigDecimal.valueOf(100), BigDecimal.valueOf(13), BigDecimal.ZERO,
                BigDecimal.ZERO, "Defective goods return", true
        );

        IrdSyncResult result = adapter.syncCreditNote(command);
        assertTrue(result.isSuccess());
        assertTrue(result.isSimulated());
    }
}
