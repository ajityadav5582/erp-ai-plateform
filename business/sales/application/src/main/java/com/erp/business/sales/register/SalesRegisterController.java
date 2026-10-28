package com.erp.business.sales.register;

import com.erp.business.sales.register.dto.SalesRegisterResponse;
import com.erp.platform.business.context.BusinessContextAccessor;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Sales register endpoint: the VAT-201 figures for a period, net of credit notes.
 *
 * <p>Authorisation lives on the service via {@code @RequiresPermission}. The
 * controller only reads the authenticated tenant and company, so a caller cannot
 * widen its own scope through the payload.
 */
@RestController
@RequestMapping("/api/v1/sales/register")
@RequiredArgsConstructor
public class SalesRegisterController {

    private final SalesRegisterService salesRegisterService;
    private final BusinessContextAccessor context;

    @GetMapping
    public ResponseEntity<SalesRegisterResponse> getRegister(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(salesRegisterService.getRegister(
                context.getTenantId(), context.getCompanyId(), from, to));
    }
}
