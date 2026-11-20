package com.erp.platform.identity.application;

import com.erp.platform.identity.application.dto.CreateCompanyRequest;
import com.erp.platform.identity.application.dto.CompanyResponse;

/**
 * Service for company lifecycle management, including bootstrap operations
 * that automatically provision branch and warehouse resources when a company
 * is first created.
 *
 * @since 1.0.0
 */
public interface CompanyService {

    /**
     * Creates a new company and auto-provisions its initial branch and warehouse.
     *
     * <p>After persisting the company, this method:
     * <ul>
     *   <li>Creates a "Head Office" branch (code = "HO") with the company's
     *       localLevelId, wardNo, address, phone, and email transferred from the
     *       company record</li>
     *   <li>Creates a "Main Store" warehouse (code = "WH-MAIN") linked to the
     *       head office branch</li>
     *   <li>Sets the branch's default warehouse to the warehouse's ID</li>
     *   <li>Saves a UserBranch assignment linking the creator user to the head
     *       office branch with {@code isPrimary = true}</li>
     *   <li>Sets the user's branch ID to the head office branch ID if currently
     *       null</li>
     * </ul>
     *
     * @param tenantId      the tenant the company belongs to
     * @param creatorUserId the user performing the creation (becomes the primary
     *                      branch user)
     * @param request       the company creation request
     * @return the created company response
     * @throws IllegalStateException if the creator user cannot be found or the
     *   bootstrap operations fail
     */
    CompanyResponse createCompany(Long tenantId, Long creatorUserId, CreateCompanyRequest request);
}