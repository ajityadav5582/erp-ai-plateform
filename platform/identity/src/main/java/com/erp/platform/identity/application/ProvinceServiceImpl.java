package com.erp.platform.identity.application;

import com.erp.platform.identity.application.dto.CreateProvinceRequest;
import com.erp.platform.identity.application.dto.DistrictListResponse;
import com.erp.platform.identity.application.dto.DistrictResponse;
import com.erp.platform.identity.application.dto.ProvinceListResponse;
import com.erp.platform.identity.application.dto.ProvinceResponse;
import com.erp.platform.identity.application.dto.UpdateProvinceRequest;
import com.erp.platform.identity.application.mapper.ProvinceMapper;
import com.erp.platform.identity.domain.District;
import com.erp.platform.identity.domain.DistrictStatus;
import com.erp.platform.identity.domain.Province;
import com.erp.platform.identity.domain.ProvinceStatus;
import com.erp.platform.identity.domain.exception.ProvinceNotFoundException;
import com.erp.platform.identity.infrastructure.persistence.DistrictRepository;
import com.erp.platform.identity.infrastructure.persistence.ProvinceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of ProvinceService.
 *
 * @since 1.0.0
 */
@Service
@Transactional
public class ProvinceServiceImpl implements ProvinceService {

    private final ProvinceRepository provinceRepository;
    private final DistrictRepository districtRepository;
    private final ProvinceMapper provinceMapper;

    public ProvinceServiceImpl(ProvinceRepository provinceRepository, DistrictRepository districtRepository, ProvinceMapper provinceMapper) {
        this.provinceRepository = provinceRepository;
        this.districtRepository = districtRepository;
        this.provinceMapper = provinceMapper;
    }

    @Override
    public ProvinceResponse createProvince(CreateProvinceRequest request) {
        if (provinceRepository.existsByProvinceCode(request.provinceCode())) {
            throw new IllegalArgumentException(
                "Province with code '" + request.provinceCode() + "' already exists"
            );
        }

        Province province = provinceMapper.toEntity(request);
        Province savedProvince = provinceRepository.save(province);
        return provinceMapper.toResponse(savedProvince);
    }

    @Override
    @Transactional(readOnly = true)
    public ProvinceResponse getProvinceById(Long id) {
        Province province = provinceRepository.findById(id)
            .orElseThrow(() -> new ProvinceNotFoundException("Province not found with ID: " + id));
        return provinceMapper.toResponse(province);
    }

    @Override
    @Transactional(readOnly = true)
    public ProvinceResponse getProvinceByCode(String provinceCode) {
        Province province = provinceRepository.findByProvinceCode(provinceCode)
            .orElseThrow(() -> new ProvinceNotFoundException(
                "Province not found with code: " + provinceCode
            ));
        return provinceMapper.toResponse(province);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProvinceListResponse> listProvinces(Pageable pageable) {
        return provinceRepository.findAll(pageable).map(provinceMapper::toListResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProvinceListResponse> listActiveProvinces() {
        return provinceRepository.findByStatusOrderByProvinceName(ProvinceStatus.ACTIVE)
            .stream()
            .map(provinceMapper::toListResponse)
            .toList();
    }

    @Override
    public ProvinceResponse updateProvince(Long id, UpdateProvinceRequest request) {
        Province province = provinceRepository.findById(id)
            .orElseThrow(() -> new ProvinceNotFoundException("Province not found with ID: " + id));

        if (request.provinceCode() != null
                && !request.provinceCode().equalsIgnoreCase(province.getProvinceCode())
                && provinceRepository.existsByProvinceCode(request.provinceCode())) {
            throw new IllegalArgumentException(
                "Province with code '" + request.provinceCode() + "' already exists"
            );
        }

        Province updated = province.toBuilder()
            .provinceCode(request.provinceCode() != null ? request.provinceCode() : province.getProvinceCode())
            .provinceName(request.provinceName() != null ? request.provinceName() : province.getProvinceName())
            .nepaliName(request.nepaliName() != null ? request.nepaliName() : province.getNepaliName())
            .countryCode(request.countryCode() != null ? request.countryCode() : province.getCountryCode())
            .build();

        Province savedProvince = provinceRepository.save(updated);
        return provinceMapper.toResponse(savedProvince);
    }

    @Override
    public ProvinceResponse activateProvince(Long id) {
        Province province = provinceRepository.findById(id)
            .orElseThrow(() -> new ProvinceNotFoundException("Province not found with ID: " + id));
        province.activate();
        Province savedProvince = provinceRepository.save(province);
        return provinceMapper.toResponse(savedProvince);
    }

    @Override
    public ProvinceResponse deactivateProvince(Long id) {
        Province province = provinceRepository.findById(id)
            .orElseThrow(() -> new ProvinceNotFoundException("Province not found with ID: " + id));
        province.deactivate();
        Province savedProvince = provinceRepository.save(province);
        return provinceMapper.toResponse(savedProvince);
    }

    @Override
    public void deleteProvince(Long id) {
        Province province = provinceRepository.findById(id)
            .orElseThrow(() -> new ProvinceNotFoundException("Province not found with ID: " + id));
        provinceRepository.delete(province);
    }
}
