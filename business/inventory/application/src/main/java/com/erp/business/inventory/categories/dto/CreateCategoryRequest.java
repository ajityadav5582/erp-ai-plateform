package com.erp.business.inventory.categories.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new category.
 *
 * <p>There is deliberately no {@code companyId} field: the target company comes from
 * the {@code X-Company-Id} request header that the gateway forwards after the user
 * selected a company, so a client cannot create a category in a company it has not
 * selected.
 *
 * @since 1.0.0
 */
public record CreateCategoryRequest(
        @NotBlank(message = "Category name is required")
        @Size(min = 1, max = 255, message = "Category name must be between 1 and 255 characters")
        String name,

        @NotBlank(message = "Category slug is required")
        @Size(min = 1, max = 255, message = "Category slug must be between 1 and 255 characters")
        @Pattern(regexp = "^[a-z0-9-]+$", message = "Category slug must contain only lowercase letters, numbers, and hyphens")
        String slug,

        @Size(max = 65535, message = "Description must not exceed 65535 characters")
        String description,

        Long parentId
) {
}
