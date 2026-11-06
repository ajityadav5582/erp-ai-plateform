package com.erp.business.inventory.taxrate;

import com.erp.business.inventory.domain.TaxRate;
import com.erp.business.inventory.domain.TaxabilityType;
import com.erp.platform.business.context.BusinessContextAccessor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory/tax-rates")
public class TaxRateController {

    private final TaxRateService service;
    private final BusinessContextAccessor context;

    public TaxRateController(TaxRateService service, BusinessContextAccessor context) {
        this.service = service;
        this.context = context;
    }

    @GetMapping
    public ResponseEntity<List<TaxRate>> list() {
        return ResponseEntity.ok(service.list(context.getTenantId(), context.getCompanyId()));
    }

    @PostMapping
    public ResponseEntity<TaxRate> create(@RequestBody TaxRateCreateRequest request) {
        TaxRate tr = service.create(
                context.getTenantId(), context.getCompanyId(),
                request.code(), request.name(), request.rate(), request.applicability(),
                request.effectiveFrom(), request.effectiveTo());
        return ResponseEntity.ok(tr);
    }

    public record TaxRateCreateRequest(String code, String name, BigDecimal rate,
                                      TaxabilityType applicability, LocalDate effectiveFrom, LocalDate effectiveTo) {
    }
}
