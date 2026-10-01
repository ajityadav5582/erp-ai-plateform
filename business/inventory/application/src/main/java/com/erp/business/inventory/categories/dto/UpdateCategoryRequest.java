package com.erp.business.inventory.categories.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating an existing category.
 *
 * <p>This is a class rather than a record because {@code parentId} needs
 * <em>presence</em> semantics, and a record cannot express them. The three states
 * a client may want are genuinely different:
 *
 * <ul>
 *   <li>{@code parentId} absent from the JSON - leave the parent untouched</li>
 *   <li>{@code parentId: 7} - re-parent under category 7</li>
 *   <li>{@code parentId: null} - promote to a root category</li>
 * </ul>
 *
 * <p>A record deserialises all three cases to {@code null}, so a client could
 * never move a category back to the root level through this endpoint. Jackson
 * only invokes a setter for properties that are actually present in the payload,
 * so {@link #isParentIdPresent()} is the reliable signal.
 *
 * @since 1.0.0
 */
public class UpdateCategoryRequest {

    @Size(min = 1, max = 255, message = "Category name must be between 1 and 255 characters")
    private String name;

    @Size(min = 1, max = 255, message = "Category slug must be between 1 and 255 characters")
    @Pattern(regexp = "^[a-z0-9-]+$",
            message = "Category slug must contain only lowercase letters, numbers, and hyphens")
    private String slug;

    @Size(max = 65535, message = "Description must not exceed 65535 characters")
    private String description;

    private Long parentId;

    /** Tracks whether {@code parentId} appeared in the request body at all. */
    private boolean parentIdPresent;

    public UpdateCategoryRequest() {
        // Required by Jackson.
    }

    public String name() {
        return name;
    }

    @JsonSetter("name")
    public void setName(String name) {
        this.name = name;
    }

    public String slug() {
        return slug;
    }

    @JsonSetter("slug")
    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String description() {
        return description;
    }

    @JsonSetter("description")
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the requested parent id, or {@code null} when the client explicitly
     * asked for a root category or when the field was omitted
     */
    public Long parentId() {
        return parentId;
    }

    @JsonSetter("parentId")
    public void setParentId(Long parentId) {
        this.parentId = parentId;
        this.parentIdPresent = true;
    }

    /**
     * @return true when the request body contained a {@code parentId} key,
     * regardless of its value
     */
    @JsonIgnore
    public boolean isParentIdPresent() {
        return parentIdPresent;
    }
}
