package com.erp.platform.identity.application;

import com.erp.platform.identity.application.dto.LocalLevelListResponse;
import com.erp.platform.identity.application.dto.LocalLevelResponse;

import java.util.List;

/**
 * Service interface for local level (municipality / rural municipality) management.
 *
 * @since 1.0.0
 */
public interface LocalLevelService {

    /**
     * Gets a local level by its municipality id.
     *
     * @param municipalityId the municipality id
     * @return the local level response
     */
    LocalLevelResponse getLocalLevelByMunicipalityId(String municipalityId);

    /**
     * Lists all local levels for a district.
     *
     * @param districtId the district ID
     * @return list of local level list responses for the district
     */
    List<LocalLevelListResponse> listLocalLevelsByDistrict(Long districtId);
}
