package com.erp.platform.identity.interfaces.rest;

import com.erp.platform.identity.application.dto.LocalLevelListResponse;
import com.erp.platform.identity.domain.LocalLevel;
import com.erp.platform.identity.infrastructure.persistence.LocalLevelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/identity/local-levels")
@RequiredArgsConstructor
public class LocalLevelController {

    private final LocalLevelRepository localLevelRepository;

    @GetMapping("/by-district")
    public List<LocalLevelListResponse> listByDistrict(@RequestParam Short districtId) {
        return localLevelRepository.findByDistrictIdOrderByName(districtId).stream()
                .map(l -> new LocalLevelListResponse(
                        String.valueOf(l.getId()),
                        l.getName(),
                        l.getNepaliName(),
                        l.getDistrictId() != null ? l.getDistrictId().longValue() : null,
                        String.valueOf(l.getLocalLevelTypeId())
                ))
                .toList();
    }
}
