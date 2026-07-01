package com.erp.platform.common.kernel;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

import java.io.Serializable;
import java.time.Instant;

/**
 * Soft delete entity providing logical deletion support.
 *
 * <p>Entities that require soft delete functionality should extend this class.
 * Instead of physically deleting records, the {@code deletedAt} timestamp is set.
 *
 * @param <T> the type of the entity identifier
 */
@MappedSuperclass
public abstract class SoftDeleteEntity<T extends Serializable> extends BaseEntity<T> {

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by", length = 100)
    private String deletedBy;

    /**
     * Checks if the entity has been soft-deleted.
     *
     * @return true if deleted, false otherwise
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Returns the deletion timestamp.
     *
     * @return the deletion timestamp, or null if not deleted
     */
    public Instant getDeletedAt() {
        return deletedAt;
    }

    /**
     * Sets the deletion timestamp.
     *
     * @param deletedAt the deletion timestamp
     */
    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    /**
     * Returns the user who deleted the entity.
     *
     * @return the deleter user ID, or null if not deleted
     */
    public String getDeletedBy() {
        return deletedBy;
    }

    /**
     * Sets the deleter user ID.
     *
     * @param deletedBy the deleter user ID
     */
    public void setDeletedBy(String deletedBy) {
        this.deletedBy = deletedBy;
    }

    /**
     * Marks the entity as deleted.
     *
     * @param deletedBy the user performing the deletion
     */
    public void markDeleted(String deletedBy) {
        this.deletedAt = Instant.now();
        this.deletedBy = deletedBy;
    }

    /**
     * Restores a soft-deleted entity.
     */
    public void restore() {
        this.deletedAt = null;
        this.deletedBy = null;
    }
}
