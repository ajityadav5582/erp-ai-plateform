package com.erp.platform.identity.application;

import com.erp.platform.identity.application.dto.CreateProvinceRequest;
import com.erp.platform.identity.application.dto.ProvinceListResponse;
import com.erp.platform.identity.application.dto.ProvinceResponse;
import com.erp.platform.identity.application.dto.UpdateProvinceRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for province management operations.
 *
 * <p>Provinces are master data shared across tenants, so no tenant ID is
 * required for any operation.
 *
 * @since 1.0.0
 */
public interface ProvinceService {

    /**
     * Create a new province.
     *
     * @param request the create province request
     * @return the created province response
     */
    ProvinceResponse createProvince(CreateProvinceRequest request);

    /**
     * Get a province by its id.
     *
     * @param id the province id
     * @return the province response
     */
    ProvinceResponse getProvinceById(Long id);

    /**
     * Get a province by its unique province code.
     *
     * @param provinceCode the province code
     * @return the province response
     */
    ProvinceResponse getProvinceByCode(String provinceCode);

    /**
     * List all provinces with pagination.
     *
     * @param pageable the pagination parameters
     * @return page of province list responses
     */
    Page<ProvinceListResponse> listProvinces(Pageable pageable);

    /**
     * List all active provinces.
     *
     * @return list of active province responses
     */
    List<ProvinceListResponse> listActiveProvinces();

    /**
     * Update an existing province.
     *
     * @param id the province id
     * @param request the update province request
     * @return the updated province response
     */
    ProvinceResponse updateProvince(Long id, UpdateProvinceRequest request);

    /**
     * Activate a province (INACTIVE → ACTIVE).
     *
     * @param id the province id
     * @return the activated province response
     */
    ProvinceResponse activateProvince(Long id);

    /**
     * Deactivate a province (ACTIVE → INACTIVE).
     *
     * @param id the province id
     * @return the deactivated province response
     */
    ProvinceResponse deactivateProvince(Long id);

    /**
     * Delete a province.
     *
     * @param id the province id
     */
    void deleteProvince(Long id);
}
