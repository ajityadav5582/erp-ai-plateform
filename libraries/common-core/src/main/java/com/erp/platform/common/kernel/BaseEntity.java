package com.erp.platform.common.kernel;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Base entity providing common identity and audit fields.
 *
 * <p>All persistent entities in the system should extend this class
 * to ensure consistent identity management and audit trail.
 *
 * @param <T> the type of the entity identifier
 */
public abstract class BaseEntity<T extends Serializable> implements Serializable {

    private T id;

    private Instant createdAt;

    private Instant updatedAt;

    private String createdBy;

    private String updatedBy;

    /**
     * Returns the entity identifier.
     *
     * @return the entity ID
     */
    public T getId() {
        return id;
    }

    /**
     * Sets the entity identifier.
     *
     * <p>This method should only be called by persistence frameworks.
     *
     * @param id the entity ID
     */
    public void setId(T id) {
        this.id = id;
    }

    /**
     * Returns the timestamp when the entity was created.
     *
     * @return the creation timestamp
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the creation timestamp.
     *
     * @param createdAt the creation timestamp
     */
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Returns the timestamp when the entity was last updated.
     *
     * @return the last update timestamp
     */
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Sets the last update timestamp.
     *
     * @param updatedAt the last update timestamp
     */
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Returns the user who created the entity.
     *
     * @return the creator user ID
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * Sets the creator user ID.
     *
     * @param createdBy the creator user ID
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * Returns the user who last updated the entity.
     *
     * @return the last updater user ID
     */
    public String getUpdatedBy() {
        return updatedBy;
    }

    /**
     * Sets the last updater user ID.
     *
     * @param updatedBy the last updater user ID
     */
    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    /**
     * Checks if the entity has an ID assigned.
     *
     * @return true if the entity has an ID, false otherwise
     */
    public boolean hasId() {
        return id != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BaseEntity<?> that = (BaseEntity<?>) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "BaseEntity{" +
                "id=" + id +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", createdBy='" + createdBy + '\'' +
                ", updatedBy='" + updatedBy + '\'' +
                '}';
    }
}
