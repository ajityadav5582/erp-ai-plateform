package com.erp.business.inventory.interfaces.category;

import com.erp.platform.common.dto.PageRequest;
import com.erp.platform.common.dto.PageResponse;

import java.util.List;
import java.util.Optional;

/**
 * Application interface for Category operations.
 *
 * <p>This interface defines the contract for category management that other modules
 * can depend on without coupling to Inventory's internal implementation.
 *
 * @since 1.0.0
 */
public interface CategoryService {

    Optional<CategoryResponse> findById(Long categoryId);

    Optional<CategoryResponse> findByCode(String code);

    Optional<CategoryResponse> findBySlug(String slug);

    boolean validateCategory(Long categoryId);

    boolean validateCategoryCode(String code);

    List<CategoryResponse> findAllActive();

    PageResponse<CategoryResponse> findAll(PageRequest pageRequest);

    interface CategoryResponse {
        Long getId();
        String getCode();
        String getName();
        String getSlug();
        String getDescription();
        Long getParentId();
        String getParentName();
        Boolean getIsActive();
    }

    interface CategoryCreateRequest {
        String getCode();
        String getName();
        String getSlug();
        String getDescription();
        Long getParentId();
        Boolean getIsActive();
    }

    interface CategoryUpdateRequest {
        String getName();
        String getSlug();
        String getDescription();
        Long getParentId();
        Boolean getIsActive();
    }
}
