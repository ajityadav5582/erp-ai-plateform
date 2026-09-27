package com.erp.platform.identity.application;

import com.erp.platform.identity.application.dto.LocalLevelListResponse;
import com.erp.platform.identity.application.dto.LocalLevelResponse;
import com.erp.platform.identity.application.mapper.LocalLevelMapper;
import com.erp.platform.identity.domain.LocalLevel;
import com.erp.platform.identity.infrastructure.persistence.LocalLevelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of LocalLevelService.
 *
 * @since 1.0.0
 */
@Service
@Transactional
public class LocalLevelServiceImpl implements LocalLevelService {

    private final LocalLevelRepository localLevelRepository;
    private final LocalLevelMapper localLevelMapper;

    public LocalLevelServiceImpl(LocalLevelRepository localLevelRepository, LocalLevelMapper localLevelMapper) {
        this.localLevelRepository = localLevelRepository;
        this.localLevelMapper = localLevelMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public LocalLevelResponse getLocalLevelByMunicipalityId(String municipalityId) {
        LocalLevel localLevel = localLevelRepository.findByMunicipalityId(municipalityId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Local level not found with municipality id: " + municipalityId
            ));
        return localLevelMapper.toResponse(localLevel);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocalLevelListResponse> listLocalLevelsByDistrict(Long districtId) {
        return localLevelRepository.findByDistrictIdOrderByName(districtId)
            .stream()
            .map(localLevelMapper::toListResponse)
            .toList();
    }
}
