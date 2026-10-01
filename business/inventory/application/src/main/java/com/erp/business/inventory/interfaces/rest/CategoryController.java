package com.erp.business.inventory.interfaces.rest;

import com.erp.business.inventory.categories.CategoryService;
import com.erp.business.inventory.categories.dto.CategoryListResponse;
import com.erp.business.inventory.categories.dto.CategoryResponse;
import com.erp.business.inventory.categories.dto.CreateCategoryRequest;
import com.erp.business.inventory.categories.dto.UpdateCategoryRequest;
import com.erp.business.inventory.shared.context.BusinessContextAccessor;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

/**
 * REST controller for category management.
 *
 * <p>Exposes a standard REST API for category CRUD operations, hierarchical
 * (parent/child) navigation, and lifecycle state transitions, following the
 * platform API standards:
 * <ul>
 *   <li>Versioned base path {@code /api/v1/inventory/categories}</li>
 *   <li>Proper HTTP methods (GET, POST, PUT, PATCH, DELETE)</li>
 *   <li>Appropriate status codes (201 + Location on create, 204 on delete)</li>
 *   <li>Pagination and filtering on collection endpoints</li>
 * </ul>
 *
 * <h2>Scoping</h2>
 * Neither the tenant nor the company is accepted from the request. Both are read
 * through the injected {@link BusinessContextAccessor} from the validated request
 * context that
 * {@link com.erp.platform.security.filter.PlatformJwtAuthenticationFilter} populates:
 * the tenant from the signed JWT claims, the company from the {@code X-Company-Id}
 * header the gateway forwards after the user selected a company. This removes the
 * possibility of a client reading or writing another company's categories by
 * simply changing a query parameter.
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/inventory/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final BusinessContextAccessor context;

    /**
     * Creates a new category in the currently selected company.
     *
     * @param request    the category creation request
     * @param uriBuilder URI builder for location header
     * @return 201 Created with the created category and a {@code Location} header
     */
    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(
            @Valid @RequestBody CreateCategoryRequest request,
            UriComponentsBuilder uriBuilder) {
        Long tenantId = context.getTenantId();
        Long companyId = context.getCompanyId();
        CategoryResponse response = categoryService.createCategory(tenantId, companyId, request);
        return ResponseEntity
                .created(uriBuilder.path("/api/v1/inventory/categories/{id}").buildAndExpand(response.id()).toUri())
                .body(response);
    }

    /**
     * Lists all categories for the current tenant and company with pagination.
     *
     * @param pageable pagination and sorting parameters
     * @return 200 OK with a page of categories
     */
    @GetMapping
    public ResponseEntity<Page<CategoryListResponse>> listCategories(Pageable pageable) {
        Long tenantId = context.getTenantId();
        Long companyId = context.getCompanyId();
        Page<CategoryListResponse> page = categoryService.listCategories(tenantId, companyId, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * Gets a category by its ID.
     *
     * @param categoryId the category ID
     * @return 200 OK with the category
     */
    @GetMapping("/{categoryId}")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable Long categoryId) {
        Long tenantId = context.getTenantId();
        CategoryResponse response = categoryService.getCategoryById(tenantId, categoryId);
        return ResponseEntity.ok(response);
    }

    /**
     * Gets a category by its slug within the selected company.
     *
     * @param slug the category slug
     * @return 200 OK with the category
     */
    @GetMapping("/by-slug")
    public ResponseEntity<CategoryResponse> getCategoryBySlug(@RequestParam String slug) {
        Long tenantId = context.getTenantId();
        Long companyId = context.getCompanyId();
        CategoryResponse response = categoryService.getCategoryBySlug(tenantId, companyId, slug);
        return ResponseEntity.ok(response);
    }

    /**
     * Lists all root categories (categories without a parent) for the selected company.
     *
     * @return 200 OK with the list of root categories
     */
    @GetMapping("/roots")
    public ResponseEntity<List<CategoryListResponse>> listRootCategories() {
        Long tenantId = context.getTenantId();
        Long companyId = context.getCompanyId();
        List<CategoryListResponse> response = categoryService.listRootCategories(tenantId, companyId);
        return ResponseEntity.ok(response);
    }

    /**
     * Lists all child categories of a given parent category.
     *
     * @param parentId the parent category ID
     * @return 200 OK with the list of child categories
     */
    @GetMapping("/children")
    public ResponseEntity<List<CategoryListResponse>> listChildCategories(@RequestParam Long parentId) {
        Long tenantId = context.getTenantId();
        Long companyId = context.getCompanyId();
        List<CategoryListResponse> response = categoryService.listChildCategories(tenantId, companyId, parentId);
        return ResponseEntity.ok(response);
    }

    /**
     * Lists all active categories for the selected company.
     *
     * @return 200 OK with the list of active categories
     */
    @GetMapping("/active")
    public ResponseEntity<List<CategoryListResponse>> listActiveCategories() {
        Long tenantId = context.getTenantId();
        Long companyId = context.getCompanyId();
        List<CategoryListResponse> response = categoryService.listActiveCategories(tenantId, companyId);
        return ResponseEntity.ok(response);
    }

    /**
     * Fully updates a category.
     *
     * @param categoryId the category ID
     * @param request    the update request
     * @return 200 OK with the updated category
     */
    @PutMapping("/{categoryId}")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable Long categoryId,
            @Valid @RequestBody UpdateCategoryRequest request) {
        Long tenantId = context.getTenantId();
        CategoryResponse response = categoryService.updateCategory(tenantId, categoryId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Partially updates a category.
     *
     * <p>Only the fields present in the body are applied; a null field leaves the
     * current value untouched.
     *
     * @param categoryId the category ID
     * @param request    the partial update request
     * @return 200 OK with the updated category
     */
    @PatchMapping("/{categoryId}")
    public ResponseEntity<CategoryResponse> patchCategory(
            @PathVariable Long categoryId,
            @Valid @RequestBody UpdateCategoryRequest request) {
        Long tenantId = context.getTenantId();
        CategoryResponse response = categoryService.updateCategory(tenantId, categoryId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Moves a category to a new parent (or to root when parent is null).
     *
     * @param categoryId  the category ID
     * @param newParentId the new parent category ID, or null to make it a root category
     * @return 200 OK with the moved category
     */
    @PostMapping("/{categoryId}/move")
    public ResponseEntity<CategoryResponse> moveCategory(
            @PathVariable Long categoryId,
            @RequestParam(required = false) Long newParentId) {
        Long tenantId = context.getTenantId();
        CategoryResponse response = categoryService.moveCategory(tenantId, categoryId, newParentId);
        return ResponseEntity.ok(response);
    }

    /**
     * Activates a category.
     *
     * @param categoryId the category ID
     * @return 200 OK with the activated category
     */
    @PostMapping("/{categoryId}/activate")
    public ResponseEntity<CategoryResponse> activateCategory(@PathVariable Long categoryId) {
        Long tenantId = context.getTenantId();
        CategoryResponse response = categoryService.activateCategory(tenantId, categoryId);
        return ResponseEntity.ok(response);
    }

    /**
     * Deactivates a category.
     *
     * @param categoryId the category ID
     * @return 200 OK with the deactivated category
     */
    @PostMapping("/{categoryId}/deactivate")
    public ResponseEntity<CategoryResponse> deactivateCategory(@PathVariable Long categoryId) {
        Long tenantId = context.getTenantId();
        CategoryResponse response = categoryService.deactivateCategory(tenantId, categoryId);
        return ResponseEntity.ok(response);
    }

    /**
     * Gets the full hierarchy path from root to the specified category.
     *
     * @param categoryId the category ID
     * @return 200 OK with the list of categories from root to target
     */
    @GetMapping("/{categoryId}/path")
    public ResponseEntity<List<CategoryResponse>> getCategoryHierarchyPath(@PathVariable Long categoryId) {
        Long tenantId = context.getTenantId();
        Long companyId = context.getCompanyId();
        List<CategoryResponse> response = categoryService.getCategoryHierarchyPath(tenantId, companyId, categoryId);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a category.
     *
     * @param categoryId the category ID
     * @return 204 No Content
     */
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long categoryId) {
        Long tenantId = context.getTenantId();
        categoryService.deleteCategory(tenantId, categoryId);
        return ResponseEntity.noContent().build();
    }
}
