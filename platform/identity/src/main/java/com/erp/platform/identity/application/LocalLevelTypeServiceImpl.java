package com.erp.platform.identity.application;

import com.erp.platform.identity.application.dto.LocalLevelTypeResponse;
import com.erp.platform.identity.application.mapper.LocalLevelTypeMapper;
import com.erp.platform.identity.domain.LocalLevelType;
import com.erp.platform.identity.domain.exception.LocalLevelTypeNotFoundException;
import com.erp.platform.identity.infrastructure.persistence.LocalLevelTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of LocalLevelTypeService for master data.
 *
 * <p>Provides read-only access to local level type master data seeded by Flyway.
 *
 * @since 2.0.0
 */
@Service
@Transactional(readOnly = true)
public class LocalLevelTypeServiceImpl implements LocalLevelTypeService {

    private final LocalLevelTypeRepository localLevelTypeRepository;
    private final LocalLevelTypeMapper localLevelTypeMapper;

    public LocalLevelTypeServiceImpl(LocalLevelTypeRepository localLevelTypeRepository, LocalLevelTypeMapper localLevelTypeMapper) {
        this.localLevelTypeRepository = localLevelTypeRepository;
        this.localLevelTypeMapper = localLevelTypeMapper;
    }

    @Override
    public LocalLevelTypeResponse getLocalLevelTypeById(Short id) {
        LocalLevelType localLevelType = localLevelTypeRepository.findById(id)
            .orElseThrow(() -> new LocalLevelTypeNotFoundException("Local level type not found with ID: " + id));
        return localLevelTypeMapper.toResponse(localLevelType);
    }

    @Override
    public LocalLevelTypeResponse getLocalLevelTypeByName(String name) {
        LocalLevelType localLevelType = localLevelTypeRepository.findByName(name)
            .orElseThrow(() -> new LocalLevelTypeNotFoundException("Local level type not found with name: " + name));
        return localLevelTypeMapper.toResponse(localLevelType);
    }

    @Override
    public List<LocalLevelTypeResponse> listAllLocalLevelTypes() {
        return localLevelTypeRepository.findAllByOrderByNameAsc()
            .stream()
            .map(localLevelTypeMapper::toResponse)
            .toList();
    }
}
