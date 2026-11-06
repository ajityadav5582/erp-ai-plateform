package com.erp.business.pos.reports.salesbook;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pos/reports/sales-book")
public class SalesBookController {

    private final SalesBookService service;

    public SalesBookController(SalesBookService service) {
        this.service = service;
    }

    @GetMapping
    public List<SalesBookRow> get(@RequestParam Long companyId) {
        return service.fetch(companyId);
    }
}
