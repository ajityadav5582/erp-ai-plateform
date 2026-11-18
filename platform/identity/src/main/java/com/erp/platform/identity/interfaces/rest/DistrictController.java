package com.erp.platform.identity.interfaces.rest;

import com.erp.platform.identity.application.dto.DistrictListResponse;
import com.erp.platform.identity.domain.District;
import com.erp.platform.identity.infrastructure.persistence.DistrictRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/identity/districts")
@RequiredArgsConstructor
public class DistrictController {

    private final DistrictRepository districtRepository;

    @GetMapping("/by-province")
    public List<DistrictListResponse> listByProvince(@RequestParam Long provinceId) {
        return districtRepository.findByProvinceIdOrderByName(provinceId).stream()
                .map(d -> new DistrictListResponse(
                        d.getId(),
                        0L,
                        d.getProvinceId(),
                        d.getName(),
                        d.getName(),
                        d.getNepaliName(),
                        null,
                        "ACTIVE"
                ))
                .toList();
    }
}
