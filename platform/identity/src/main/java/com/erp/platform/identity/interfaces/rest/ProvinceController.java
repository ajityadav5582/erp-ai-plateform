package com.erp.platform.identity.interfaces.rest;

import com.erp.platform.identity.application.ProvinceService;
import com.erp.platform.identity.application.dto.ProvinceListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/identity/provinces")
@RequiredArgsConstructor
public class ProvinceController {

    private final ProvinceService provinceService;

    @GetMapping("/active")
    public List<ProvinceListResponse> listActiveProvinces() {
        return provinceService.listActiveProvinces();
    }
}
