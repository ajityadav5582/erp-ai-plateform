package com.erp.platform.identity.application;

import com.erp.platform.identity.application.dto.LocalLevelTypeResponse;

import java.util.List;

/**
 * Service interface for local level type master data.
 *
 * <p>Provides read-only access to local level type master data seeded by Flyway.
 *
 * @since 2.0.0
 */
public interface LocalLevelTypeService {

    /**
     * Gets a local level type by its ID.
     *
     * @param id the local level type ID
     * @return the local level type response
     */
    LocalLevelTypeResponse getLocalLevelTypeById(Short id);

    /**
     * Gets a local level type by its name.
     *
     * @param name the local level type name
     * @return the local level type response
     */
    LocalLevelTypeResponse getLocalLevelTypeByName(String name);

    /**
     * Lists all local level types.
     *
     * @return list of all local level type responses
     */
    List<LocalLevelTypeResponse> listAllLocalLevelTypes();
}
