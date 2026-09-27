package com.erp.platform.identity.application;

import com.erp.platform.identity.application.dto.CreateDistrictRequest;
import com.erp.platform.identity.application.dto.DistrictListResponse;
import com.erp.platform.identity.application.dto.DistrictResponse;
import com.erp.platform.identity.application.dto.UpdateDistrictRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for district management operations.
 *
 * <p>Districts are master data shared across tenants, so no tenant ID is
 * required for any operation.
 *
 * @since 1.0.0
 */
public interface DistrictService {

    /**
     * Create a new district.
     *
     * @param request the create district request
     * @return the created district response
     */
    DistrictResponse createDistrict(CreateDistrictRequest request);

    /**
     * Get a district by its id.
     *
     * @param id the district id
     * @return the district response
     */
    DistrictResponse getDistrictById(Long id);

    /**
     * Get a district by its unique district code.
     *
     * @param districtCode the district code
     * @return the district response
     */
    DistrictResponse getDistrictByCode(String districtCode);

    /**
     * List all districts with pagination.
     *
     * @param pageable the pagination parameters
     * @return page of district list responses
     */
    Page<DistrictListResponse> listDistricts(Pageable pageable);

    /**
     * List all active districts.
     *
     * @return list of active district responses
     */
    List<DistrictListResponse> listActiveDistricts();

    /**
     * List all active districts for a province.
     *
     * @param provinceId the province ID
     * @return list of active district responses for the province
     */
    List<DistrictListResponse> listDistrictsByProvince(Long provinceId);

    /**
     * Update an existing district.
     *
     * @param id the district id
     * @param request the update district request
     * @return the updated district response
     */
    DistrictResponse updateDistrict(Long id, UpdateDistrictRequest request);

    /**
     * Activate a district (INACTIVE → ACTIVE).
     *
     * @param id the district id
     * @return the activated district response
     */
    DistrictResponse activateDistrict(Long id);

    /**
     * Deactivate a district (ACTIVE → INACTIVE).
     *
     * @param id the district id
     * @return the deactivated district response
     */
    DistrictResponse deactivateDistrict(Long id);

    /**
     * Delete a district.
     *
     * @param id the district id
     */
    void deleteDistrict(Long id);
}
