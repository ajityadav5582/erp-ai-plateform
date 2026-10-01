package com.erp.business.inventory.categories;

import com.erp.business.inventory.categories.dto.CategoryListResponse;
import com.erp.business.inventory.categories.dto.CategoryResponse;
import com.erp.business.inventory.categories.dto.CreateCategoryRequest;
import com.erp.business.inventory.categories.dto.UpdateCategoryRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for category management operations.
 *
 * @since 1.0.0
 */
public interface CategoryService {

    /**
     * Create a new category within the given company.
     *
     * <p>The company is passed explicitly rather than read from the request body so
     * that the caller cannot create data outside the company selected on the request.
     *
     * @param tenantId  the tenant ID
     * @param companyId the company ID from the validated request context
     * @param request   the create category request
     * @return the created category response
     */
    CategoryResponse createCategory(Long tenantId, Long companyId, CreateCategoryRequest request);

    /**
     * Get a category by its ID.
     *
     * @param tenantId   the tenant ID
     * @param categoryId the category ID
     * @return the category response
     */
    CategoryResponse getCategoryById(Long tenantId, Long categoryId);

    /**
     * Get a category by its slug within a company.
     *
     * @param tenantId  the tenant ID
     * @param companyId the company ID
     * @param slug      the category slug
     * @return the category response
     */
    CategoryResponse getCategoryBySlug(Long tenantId, Long companyId, String slug);

    /**
     * List all categories for a tenant and company with pagination.
     *
     * @param tenantId  the tenant ID
     * @param companyId the company ID
     * @param pageable  the pagination parameters
     * @return page of category list responses
     */
    Page<CategoryListResponse> listCategories(Long tenantId, Long companyId, Pageable pageable);

    /**
     * List all root categories (categories without a parent) for a tenant and company.
     *
     * @param tenantId  the tenant ID
     * @param companyId the company ID
     * @return list of root category responses
     */
    List<CategoryListResponse> listRootCategories(Long tenantId, Long companyId);

    /**
     * List all child categories of a given parent category.
     *
     * @param tenantId  the tenant ID
     * @param companyId the company ID
     * @param parentId  the parent category ID
     * @return list of child category responses
     */
    List<CategoryListResponse> listChildCategories(Long tenantId, Long companyId, Long parentId);

    /**
     * List all active categories for a tenant and company.
     *
     * @param tenantId  the tenant ID
     * @param companyId the company ID
     * @return list of active category responses
     */
    List<CategoryListResponse> listActiveCategories(Long tenantId, Long companyId);

    /**
     * Update an existing category.
     *
     * @param tenantId   the tenant ID
     * @param categoryId the category ID
     * @param request    the update category request
     * @return the updated category response
     */
    CategoryResponse updateCategory(Long tenantId, Long categoryId, UpdateCategoryRequest request);

    /**
     * Move a category to a new parent (or to root when parent is null).
     *
     * @param tenantId   the tenant ID
     * @param categoryId the category ID
     * @param newParentId the new parent category ID, or null to make it a root category
     * @return the moved category response
     */
    CategoryResponse moveCategory(Long tenantId, Long categoryId, Long newParentId);

    /**
     * Activate a category.
     *
     * @param tenantId   the tenant ID
     * @param categoryId the category ID
     * @return the activated category response
     */
    CategoryResponse activateCategory(Long tenantId, Long categoryId);

    /**
     * Deactivate a category.
     *
     * @param tenantId   the tenant ID
     * @param categoryId the category ID
     * @return the deactivated category response
     */
    CategoryResponse deactivateCategory(Long tenantId, Long categoryId);

    /**
     * Delete a category.
     *
     * @param tenantId   the tenant ID
     * @param categoryId the category ID
     */
    void deleteCategory(Long tenantId, Long categoryId);

    /**
     * Get the full hierarchy path from root to the specified category.
     *
     * @param tenantId   the tenant ID
     * @param companyId  the company ID
     * @param categoryId the category ID
     * @return list of category responses from root to the target category
     */
    List<CategoryResponse> getCategoryHierarchyPath(Long tenantId, Long companyId, Long categoryId);
}
