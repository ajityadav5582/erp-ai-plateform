package com.erp.platform.identity.application;

import com.erp.platform.identity.application.dto.CreateDistrictRequest;
import com.erp.platform.identity.application.dto.DistrictListResponse;
import com.erp.platform.identity.application.dto.DistrictResponse;
import com.erp.platform.identity.application.dto.UpdateDistrictRequest;
import com.erp.platform.identity.application.mapper.DistrictMapper;
import com.erp.platform.identity.domain.District;
import com.erp.platform.identity.domain.DistrictStatus;
import com.erp.platform.identity.domain.exception.DistrictNotFoundException;
import com.erp.platform.identity.infrastructure.persistence.DistrictRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of DistrictService.
 *
 * @since 1.0.0
 */
@Service
@Transactional
public class DistrictServiceImpl implements DistrictService {

    private final DistrictRepository districtRepository;
    private final DistrictMapper districtMapper;

    public DistrictServiceImpl(DistrictRepository districtRepository, DistrictMapper districtMapper) {
        this.districtRepository = districtRepository;
        this.districtMapper = districtMapper;
    }

    @Override
    public DistrictResponse createDistrict(CreateDistrictRequest request) {
        if (districtRepository.existsByDistrictCode(request.districtCode())) {
            throw new IllegalArgumentException(
                "District with code '" + request.districtCode() + "' already exists"
            );
        }

        District district = districtMapper.toEntity(request);
        District savedDistrict = districtRepository.save(district);
        return districtMapper.toResponse(savedDistrict);
    }

    @Override
    @Transactional(readOnly = true)
    public DistrictResponse getDistrictById(Long id) {
        District district = districtRepository.findById(id)
            .orElseThrow(() -> new DistrictNotFoundException("District not found with ID: " + id));
        return districtMapper.toResponse(district);
    }

    @Override
    @Transactional(readOnly = true)
    public DistrictResponse getDistrictByCode(String districtCode) {
        District district = districtRepository.findByDistrictCode(districtCode)
            .orElseThrow(() -> new DistrictNotFoundException(
                "District not found with code: " + districtCode
            ));
        return districtMapper.toResponse(district);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DistrictListResponse> listDistricts(Pageable pageable) {
        return districtRepository.findAll(pageable).map(districtMapper::toListResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DistrictListResponse> listActiveDistricts() {
        return districtRepository.findByStatusOrderByDistrictName(DistrictStatus.ACTIVE)
            .stream()
            .map(districtMapper::toListResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DistrictListResponse> listDistrictsByProvince(Long provinceId) {
        return districtRepository.findByProvinceIdAndStatusOrderByDistrictName(provinceId, DistrictStatus.ACTIVE)
            .stream()
            .map(districtMapper::toListResponse)
            .toList();
    }

    @Override
    public DistrictResponse updateDistrict(Long id, UpdateDistrictRequest request) {
        District district = districtRepository.findById(id)
            .orElseThrow(() -> new DistrictNotFoundException("District not found with ID: " + id));

        if (request.districtCode() != null
                && !request.districtCode().equalsIgnoreCase(district.getDistrictCode())
                && districtRepository.existsByDistrictCode(request.districtCode())) {
            throw new IllegalArgumentException(
                "District with code '" + request.districtCode() + "' already exists"
            );
        }

        District updated = district.toBuilder()
            .districtCode(request.districtCode() != null ? request.districtCode() : district.getDistrictCode())
            .districtName(request.districtName() != null ? request.districtName() : district.getDistrictName())
            .nepaliName(request.nepaliName() != null ? request.nepaliName() : district.getNepaliName())
            .countryCode(request.countryCode() != null ? request.countryCode() : district.getCountryCode())
            .build();

        District savedDistrict = districtRepository.save(updated);
        return districtMapper.toResponse(savedDistrict);
    }

    @Override
    public DistrictResponse activateDistrict(Long id) {
        District district = districtRepository.findById(id)
            .orElseThrow(() -> new DistrictNotFoundException("District not found with ID: " + id));
        district.activate();
        District savedDistrict = districtRepository.save(district);
        return districtMapper.toResponse(savedDistrict);
    }

    @Override
    public DistrictResponse deactivateDistrict(Long id) {
        District district = districtRepository.findById(id)
            .orElseThrow(() -> new DistrictNotFoundException("District not found with ID: " + id));
        district.deactivate();
        District savedDistrict = districtRepository.save(district);
        return districtMapper.toResponse(savedDistrict);
    }

    @Override
    public void deleteDistrict(Long id) {
        District district = districtRepository.findById(id)
            .orElseThrow(() -> new DistrictNotFoundException("District not found with ID: " + id));
        districtRepository.delete(district);
    }
}
