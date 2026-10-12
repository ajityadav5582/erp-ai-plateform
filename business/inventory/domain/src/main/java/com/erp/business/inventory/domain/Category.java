package com.erp.business.inventory.domain;

import com.erp.business.inventory.shared.domain.TenantScopedEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(
        name = "categories",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_company_category_slug",
                        columnNames = {"company_id", "slug"}
                )
        },
        indexes = {
                @Index(name = "idx_categories_tenant_company", columnList = "tenant_id, company_id"),
                @Index(name = "idx_categories_parent", columnList = "parent_id")
        }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Category extends TenantScopedEntity<Long> {

    private static final long serialVersionUID = 1L;

    /**
     * Parent category for hierarchical organization.
     * Null indicates a root-level category.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "parent_id",
            foreignKey = @ForeignKey(name = "fk_category_parent", foreignKeyDefinition = "FOREIGN KEY (parent_id) REFERENCES categories(id) ON DELETE SET NULL")
    )
    @ToString.Exclude
    private Category parent;


    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    @ToString.Exclude
    @Builder.Default
    @SuppressWarnings("serial") // JPA state, never serialized; Hibernate re-reads it on load
    private java.util.List<Category> children = new java.util.ArrayList<>();


    @NotBlank(message = "Category name is required")
    @Size(max = 255, message = "Category name must not exceed 255 characters")
    @Column(name = "name", nullable = false, length = 255)
    private String name;


    @NotBlank(message = "Category slug is required")
    @Size(max = 255, message = "Category slug must not exceed 255 characters")
    @Column(name = "slug", nullable = false, length = 255)
    private String slug;


    @Size(max = 65535, message = "Description must not exceed 65535 characters")
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;


    @NotNull(message = "Active status is required")
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;


    public void activate() {
        if (Boolean.TRUE.equals(this.isActive)) {
            throw new IllegalStateException("Category is already active");
        }
        this.isActive = true;
    }


    public void deactivate() {
        if (Boolean.FALSE.equals(this.isActive)) {
            throw new IllegalStateException("Category is already inactive");
        }
        this.isActive = false;
    }


    public void updateDetails(String name, String slug, String description) {
        if (name != null) {
            validateName(name);
            this.name = name.trim();
        }
        if (slug != null) {
            validateSlug(slug);
            this.slug = slug.trim().toLowerCase();
        }
        if (description != null) {
            this.description = description.trim().isEmpty() ? null : description.trim();
        }
    }


    public void update(String name, String slug, String description) {
        if (name != null || slug != null || description != null) {
            updateDetails(name, slug, description);
        }
    }


    public void setParent(Category parent) {
        if (parent != null && parent.getId() != null && parent.getId().equals(this.getId())) {
            throw new IllegalArgumentException("A category cannot be its own parent");
        }
        if (parent != null && isDescendantOf(parent)) {
            throw new IllegalArgumentException(
                    "A category cannot be moved under one of its own descendants");
        }
        if (this.parent != null) {
            this.parent.getChildren().remove(this);
        }
        this.parent = parent;
        if (parent != null && !parent.getChildren().contains(this)) {
            parent.getChildren().add(this);
        }
    }


    private boolean isDescendantOf(Category ancestor) {
        Category current = this.parent;
        while (current != null) {
            if (current.getId() != null && current.getId().equals(ancestor.getId())) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }


    public void addChild(Category child) {
        if (child == null) {
            throw new IllegalArgumentException("Child category cannot be null");
        }
        if (child.equals(this)) {
            throw new IllegalArgumentException("A category cannot be its own child");
        }
        if (!this.children.contains(child)) {
            this.children.add(child);
            child.setParent(this);
        }
    }


    public void removeChild(Category child) {
        if (child != null && this.children.remove(child)) {
            child.setParent(null);
        }
    }


    public boolean isRoot() {
        return this.parent == null;
    }


    public boolean hasChildren() {
        return !this.children.isEmpty();
    }


    public java.util.List<Category> getPath() {
        java.util.List<Category> path = new java.util.ArrayList<>();
        Category current = this;
        while (current != null) {
            path.add(0, current);
            current = current.getParent();
        }
        return path;
    }


    public static Category create(Long tenantId, Long companyId, String name, String slug) {
        return create(tenantId, companyId, name, slug, null, null);
    }


    public static Category create(Long tenantId, Long companyId, String name, String slug,
                                  Category parent, String description) {
        validateTenantId(tenantId);
        validateCompanyId(companyId);
        validateName(name);
        validateSlug(slug);

        Category category = Category.builder()
                .name(name.trim())
                .slug(slug.trim().toLowerCase())
                .description(description != null ? description.trim() : null)
                .isActive(true)
                .build();

        // tenantId/companyId live on TenantScopedEntity and are not part of the
        // builder, so the scope is assigned explicitly. This is the single
        // place a Category becomes tenant/company bound.
        category.assignScope(tenantId, companyId);

        if (parent != null) {
            category.setParent(parent);
        }

        return category;
    }

    // ==================== Validation Methods ====================

    private static void validateTenantId(Long tenantId) {
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalArgumentException("Tenant ID must be a positive number");
        }
    }

    private static void validateCompanyId(Long companyId) {
        if (companyId == null || companyId <= 0) {
            throw new IllegalArgumentException("Company ID must be a positive number");
        }
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Category name must not be blank");
        }
        if (name.length() > 255) {
            throw new IllegalArgumentException("Category name must not exceed 255 characters");
        }
    }

    private static void validateSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new IllegalArgumentException("Category slug must not be blank");
        }
        if (slug.length() > 255) {
            throw new IllegalArgumentException("Category slug must not exceed 255 characters");
        }
        // Slug should be URL-friendly (lowercase, alphanumeric, hyphens)
        if (!slug.matches("^[a-z0-9-]+$")) {
            throw new IllegalArgumentException("Category slug must contain only lowercase letters, numbers, and hyphens");
        }
    }
}
