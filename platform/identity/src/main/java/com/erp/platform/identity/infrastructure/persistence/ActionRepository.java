package com.erp.platform.identity.infrastructure.persistence;

import com.erp.platform.identity.domain.ActionEntity;
import com.erp.platform.identity.domain.ActionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Action entity.
 *
 * @since 1.0.0
 */
@Repository
public interface ActionRepository extends JpaRepository<ActionEntity, Long> {

    Optional<ActionEntity> findByActionCode(String actionCode);

    Page<ActionEntity> findByStatus(ActionStatus status, Pageable pageable);

    boolean existsByActionCode(String actionCode);
}
