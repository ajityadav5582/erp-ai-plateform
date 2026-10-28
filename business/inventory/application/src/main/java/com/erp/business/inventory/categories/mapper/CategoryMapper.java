package com.erp.business.inventory.categories.mapper;

import com.erp.business.inventory.categories.dto.CategoryListResponse;
import com.erp.business.inventory.categories.dto.CategoryResponse;
import com.erp.business.inventory.categories.dto.CreateCategoryRequest;
import com.erp.business.inventory.categories.dto.UpdateCategoryRequest;
import com.erp.business.inventory.domain.Category;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Mapper for converting between Category entity and DTOs.
 *
 * @since 1.0.0
 */
@Component
public class CategoryMapper {

    /**
     * Convert CreateCategoryRequest to a new Category entity.
     *
     * <p>The parent is intentionally <em>not</em> wired up here. Previously the
     * mapper fabricated one with {@code Category.builder().build()} plus
     * {@code setId(request.parentId())}: a detached stub with a null
     * {@code tenantId}/{@code companyId} and an empty {@code children} list. It
     * was never validated and would break any later flush that touched it. The
     * service now loads the real, managed parent and calls
     * {@link Category#setParent(Category)} itself.
     *
     * @param request   the create request
     * @param tenantId  the tenant ID
     * @param companyId the company ID, taken from the validated request context
     * @return the Category entity
     */
    public Category toEntity(CreateCategoryRequest request, Long tenantId, Long companyId) {
        return Category.create(
                tenantId,
                companyId,
                request.name(),
                request.slug(),
                null,
                request.description()
        );
    }

    /**
     * Convert Category entity to CategoryResponse.
     *
     * @param category the category entity
     * @return the CategoryResponse
     */
    public CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getTenantId(),
                category.getCompanyId(),
                category.getParent() != null ? category.getParent().getId() : null,
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getIsActive(),
                toLocalDateTime(category.getCreatedAt()),
                toLocalDateTime(category.getUpdatedAt()),
                category.getCreatedBy(),
                category.getUpdatedBy(),
                category.getVersion()
        );
    }

    /**
     * Convert Category entity to CategoryListResponse.
     *
     * @param category the category entity
     * @return the CategoryListResponse
     */
    public CategoryListResponse toListResponse(Category category) {
        Category parent = category.getParent();
        return new CategoryListResponse(
                category.getId(),
                category.getTenantId(),
                parent != null ? parent.getId() : null,
                parent != null ? parent.getName() : null,
                category.getName(),
                category.getSlug(),
                category.getIsActive(),
                toLocalDateTime(category.getCreatedAt())
        );
    }

    /**
     * Apply non-null fields from UpdateCategoryRequest directly onto the managed
     * Category entity.
     *
     * <p>The entity is mutated in place rather than copied. This matters: Lombok's
     * {@code @Builder} on {@link Category} only knows about {@code Category}'s own
     * fields, so {@code toBuilder().build()} produced an entity with a null
     * {@code id}, {@code tenantId} and {@code companyId}. Spring Data then treated
     * it as new, called {@code persist()}, the {@code @PrePersist} scope guard
     * rejected it, and every update failed with {@code 500 INTERNAL_ERROR}.
     *
     * @param category the existing managed category entity; mutated in place
     * @param request  the update request; a null field leaves the current value
     * @return the same instance, now carrying the requested changes
     */
    public Category applyUpdate(Category category, UpdateCategoryRequest request) {
        category.update(request.name(), request.slug(), request.description());
        return category;
    }

    /**
     * Convert Instant to LocalDateTime.
     *
     * @param instant the instant to convert
     * @return the local date time
     */
    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant != null ? LocalDateTime.ofInstant(instant, ZoneId.systemDefault()) : null;
    }
}
