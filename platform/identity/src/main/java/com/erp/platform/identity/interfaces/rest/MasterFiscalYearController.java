package com.erp.platform.identity.interfaces.rest;

import com.erp.platform.identity.application.dto.MasterFiscalYearResponse;
import com.erp.platform.identity.infrastructure.persistence.MasterFiscalYearRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/identity/master-fiscal-years")
@RequiredArgsConstructor
public class MasterFiscalYearController {
    private final MasterFiscalYearRepository repository;

    @GetMapping
    public List<MasterFiscalYearResponse> listActive() {
        return repository.findByIsActiveTrueOrderByDisplayOrderAsc().stream()
                .map(MasterFiscalYearResponse::from).toList();
    }
}
