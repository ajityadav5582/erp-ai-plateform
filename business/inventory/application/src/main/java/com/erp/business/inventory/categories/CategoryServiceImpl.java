package com.erp.business.inventory.categories;

import com.erp.business.inventory.categories.dto.CategoryListResponse;
import com.erp.business.inventory.categories.dto.CategoryResponse;
import com.erp.business.inventory.categories.dto.CreateCategoryRequest;
import com.erp.business.inventory.categories.dto.UpdateCategoryRequest;
import com.erp.business.inventory.categories.exception.CategoryNotFoundException;
import com.erp.business.inventory.categories.exception.DuplicateCategoryNameException;
import com.erp.business.inventory.categories.exception.DuplicateCategorySlugException;
import com.erp.business.inventory.categories.exception.InvalidCategoryHierarchyException;
import com.erp.business.inventory.categories.mapper.CategoryMapper;
import com.erp.business.inventory.domain.Category;
import com.erp.business.inventory.infrastructure.persistence.CategoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of CategoryService.
 *
 * @since 1.0.0
 */
@Service
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public CategoryServiceImpl(CategoryRepository categoryRepository, CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
    }

    @Override
    public CategoryResponse createCategory(Long tenantId, Long companyId, CreateCategoryRequest request) {
        // The domain lower-cases the slug on write, so uniqueness must be checked
        // against the same normalised value, otherwise 'Electronics' would pass
        // the check and then collide with the stored 'electronics' row.
        String slug = normalizeSlug(request.slug());
        String name = request.name().trim();

        if (categoryRepository.existsByTenantIdAndCompanyIdAndSlug(tenantId, companyId, slug)) {
            throw new DuplicateCategorySlugException(
                    "Category with slug '" + slug + "' already exists in this company"
            );
        }

        if (categoryRepository.existsByTenantIdAndCompanyIdAndName(tenantId, companyId, name)) {
            throw new DuplicateCategoryNameException(
                    "Category with name '" + name + "' already exists in this company"
            );
        }

        // Resolve the real, managed parent entity rather than letting the mapper
        // fabricate one with nothing but an id: a parent loaded here also lets us
        // verify the company before anything is written.
        Category parent = null;
        if (request.parentId() != null) {
            parent = resolveParent(tenantId, companyId, request.parentId(), null);
        }

        Category category = categoryMapper.toEntity(request, tenantId, companyId);
        if (parent != null) {
            category.setParent(parent);
        }

        Category savedCategory = categoryRepository.save(category);
        return categoryMapper.toResponse(savedCategory);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long tenantId, Long categoryId) {
        Category category = categoryRepository.findByTenantIdAndId(tenantId, categoryId)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found with ID: " + categoryId));
        return categoryMapper.toResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryBySlug(Long tenantId, Long companyId, String slug) {
        Category category = categoryRepository.findByTenantIdAndCompanyIdAndSlug(tenantId, companyId, slug)
                .orElseThrow(() -> new CategoryNotFoundException(
                        "Category not found with slug: " + slug
                ));
        return categoryMapper.toResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CategoryListResponse> listCategories(Long tenantId, Long companyId, Pageable pageable) {
        return categoryRepository.findByTenantIdAndCompanyId(tenantId, companyId, pageable)
                .map(categoryMapper::toListResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryListResponse> listRootCategories(Long tenantId, Long companyId) {
        return categoryRepository.findByTenantIdAndCompanyIdAndParentIsNull(tenantId, companyId).stream()
                .map(categoryMapper::toListResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryListResponse> listChildCategories(Long tenantId, Long companyId, Long parentId) {
        return categoryRepository.findByTenantIdAndCompanyIdAndParentId(tenantId, companyId, parentId).stream()
                .map(categoryMapper::toListResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryListResponse> listActiveCategories(Long tenantId, Long companyId) {
        return categoryRepository.findByTenantIdAndCompanyIdAndIsActiveTrue(tenantId, companyId).stream()
                .map(categoryMapper::toListResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryResponse updateCategory(Long tenantId, Long categoryId, UpdateCategoryRequest request) {
        Category category = categoryRepository.findByTenantIdAndId(tenantId, categoryId)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found with ID: " + categoryId));

        Long companyId = category.getCompanyId();

        // Validate slug uniqueness if changed, comparing the normalised form the
        // domain will actually persist.
        if (request.slug() != null) {
            String slug = normalizeSlug(request.slug());
            if (!slug.equals(category.getSlug())
                    && categoryRepository.existsByTenantIdAndCompanyIdAndSlug(tenantId, companyId, slug)) {
                throw new DuplicateCategorySlugException(
                        "Category with slug '" + slug + "' already exists in this company"
                );
            }
        }

        // Validate name uniqueness if changed
        if (request.name() != null) {
            String name = request.name().trim();
            if (!name.equalsIgnoreCase(category.getName())
                    && categoryRepository.existsByTenantIdAndCompanyIdAndName(tenantId, companyId, name)) {
                throw new DuplicateCategoryNameException(
                        "Category with name '" + name + "' already exists in this company"
                );
            }
        }

        // Resolve and validate the new parent before mutating anything, so a
        // rejected request leaves the entity untouched.
        //
        // parentId has three distinct meanings, see UpdateCategoryRequest:
        //   absent -> leave the hierarchy alone
        //   a value -> re-parent under that category
        //   null   -> promote to a root category
        boolean parentProvided = request.isParentIdPresent();
        Category newParent = null;
        if (parentProvided && request.parentId() != null) {
            newParent = resolveParent(tenantId, companyId, request.parentId(), categoryId);
        }

        // Mutate the managed entity in place. Building a copy via toBuilder()
        // dropped id/tenantId/companyId, so Spring Data called persist() and the
        // @PrePersist scope guard turned every update into a 500.
        categoryMapper.applyUpdate(category, request);
        if (parentProvided) {
            // Set unconditionally, including null, so clearing the parent works.
            category.setParent(newParent);
        }

        Category savedCategory = categoryRepository.save(category);
        return categoryMapper.toResponse(savedCategory);
    }

    @Override
    public CategoryResponse moveCategory(Long tenantId, Long categoryId, Long newParentId) {
        Category category = categoryRepository.findByTenantIdAndId(tenantId, categoryId)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found with ID: " + categoryId));

        Category newParent = null;
        if (newParentId != null) {
            newParent = resolveParent(tenantId, category.getCompanyId(), newParentId, categoryId);
        }

        category.setParent(newParent);
        Category savedCategory = categoryRepository.save(category);
        return categoryMapper.toResponse(savedCategory);
    }

    @Override
    public CategoryResponse activateCategory(Long tenantId, Long categoryId) {
        Category category = categoryRepository.findByTenantIdAndId(tenantId, categoryId)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found with ID: " + categoryId));
        if (Boolean.TRUE.equals(category.getIsActive())) {
            // Idempotent read-back: activating an already active category is not
            // an error, it is a no-op, so the caller still gets a 200.
            return categoryMapper.toResponse(category);
        }
        category.activate();
        Category savedCategory = categoryRepository.save(category);
        return categoryMapper.toResponse(savedCategory);
    }

    @Override
    public CategoryResponse deactivateCategory(Long tenantId, Long categoryId) {
        Category category = categoryRepository.findByTenantIdAndId(tenantId, categoryId)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found with ID: " + categoryId));
        if (Boolean.FALSE.equals(category.getIsActive())) {
            return categoryMapper.toResponse(category);
        }
        category.deactivate();
        Category savedCategory = categoryRepository.save(category);
        return categoryMapper.toResponse(savedCategory);
    }

    @Override
    public void deleteCategory(Long tenantId, Long categoryId) {
        Category category = categoryRepository.findByTenantIdAndId(tenantId, categoryId)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found with ID: " + categoryId));
        categoryRepository.delete(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategoryHierarchyPath(Long tenantId, Long companyId, Long categoryId) {
        List<Category> path = categoryRepository.findHierarchyPathById(tenantId, companyId, categoryId);
        if (path.isEmpty()) {
            throw new CategoryNotFoundException("Category not found with ID: " + categoryId);
        }
        return path.stream()
                .map(categoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Loads the parent category and verifies it can legally parent
     * {@code childId}.
     *
     * <p>All three failure modes here used to surface as {@code IllegalArgumentException}
     * or a bare 500. They are caller mistakes, so they are reported as a 400 with a
     * message that names the rule that was broken.
     *
     * @param tenantId the owning tenant
     * @param companyId the owning company; the parent must belong to the same one
     * @param parentId the requested parent id
     * @param childId  the category being re-parented, or null on create
     * @return the managed parent entity
     * @throws CategoryNotFoundException         if the parent does not exist in this tenant
     * @throws InvalidCategoryHierarchyException if the parent belongs to another
     *                                            company, is the category itself, or is one
     *                                            of its own descendants
     */
    private Category resolveParent(Long tenantId, Long companyId, Long parentId, Long childId) {
        Category parent = categoryRepository.findByTenantIdAndId(tenantId, parentId)
                .orElseThrow(() -> new CategoryNotFoundException(
                        "Parent category not found with ID: " + parentId));

        if (!companyId.equals(parent.getCompanyId())) {
            throw new InvalidCategoryHierarchyException(
                    "Parent category " + parentId + " belongs to company "
                            + parent.getCompanyId() + " and cannot be used in company " + companyId);
        }

        if (childId != null) {
            if (childId.equals(parentId)) {
                throw new InvalidCategoryHierarchyException(
                        "Category " + childId + " cannot be its own parent");
            }
            if (wouldCreateCycle(parent, childId)) {
                throw new InvalidCategoryHierarchyException(
                        "Category " + childId + " cannot be moved under " + parentId
                                + " because that would create a circular reference in the category hierarchy");
            }
        }

        return parent;
    }

    /**
     * Checks whether {@code candidateId} is one of {@code category}'s ancestors.
     *
     * @param category    the category that would become the new child
     * @param candidateId the proposed new parent's id
     * @return true if the move would create a cycle, false otherwise
     */
    private boolean wouldCreateCycle(Category category, Long candidateId) {
        Category current = category.getParent();
        while (current != null) {
            if (current.getId() != null && current.getId().equals(candidateId)) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    /**
     * Normalises a slug exactly the way {@link Category#create} does, so
     * uniqueness checks run against the value that will be persisted.
     *
     * @param slug the raw slug from the request
     * @return the trimmed, lower-cased slug
     */
    private String normalizeSlug(String slug) {
        return slug == null ? null : slug.trim().toLowerCase();
    }
}
