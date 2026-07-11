package com.erp.platform.identity.infrastructure.persistence;

import com.erp.platform.identity.domain.Action;
import com.erp.platform.identity.domain.Permission;
import com.erp.platform.identity.domain.PermissionStatus;
import com.erp.platform.identity.domain.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Permission aggregate.
 *
 * @since 1.0.0
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findByPermissionId(UUID permissionId);

    Optional<Permission> findByPermissionCode(String permissionCode);

    Page<Permission> findByResource(Resource resource, Pageable pageable);

    Page<Permission> findByStatus(PermissionStatus status, Pageable pageable);

    Page<Permission> findByResourceAndStatus(Resource resource, PermissionStatus status, Pageable pageable);

    Page<Permission> findByAction(Action action, Pageable pageable);

    boolean existsByPermissionCode(String permissionCode);

    boolean existsByPermissionId(UUID permissionId);

    boolean existsByResourceAndAction(Resource resource, Action action);
}
