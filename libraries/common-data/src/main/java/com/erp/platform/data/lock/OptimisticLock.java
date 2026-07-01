package com.erp.platform.data.lock;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import org.springframework.data.annotation.Version;

import java.io.Serializable;

/**
 * Entity with optimistic locking support.
 *
 * <p>Entities that require optimistic locking should extend this class.
 * The {@code version} field is automatically managed by JPA/Hibernate
 * to prevent lost updates in concurrent scenarios.
 *
 * @param <T> the type of the entity identifier
 * @since 1.0.0
 */
@MappedSuperclass
@EntityListeners(org.springframework.data.jpa.domain.support.AuditingEntityListener.class)
public abstract class OptimisticLock<T extends Serializable> extends com.erp.platform.common.kernel.BaseEntity<T> {

    private static final long serialVersionUID = 1L;

    @Version
    @Column(nullable = false)
    private Integer version;

    /**
     * Returns the version number for optimistic locking.
     *
     * @return the version number
     */
    public Integer getVersion() {
        return version;
    }

    /**
     * Sets the version number.
     *
     * <p>This method should only be called by the persistence framework.
     *
     * @param version the version number
     */
    public void setVersion(Integer version) {
        this.version = version;
    }

    /**
     * Increments the version number.
     *
     * <p>This is typically called automatically by the persistence framework
     * when the entity is updated.
     */
    public void incrementVersion() {
        this.version = (this.version == null) ? 1 : this.version + 1;
    }
}
