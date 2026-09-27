package com.erp.platform.identity.infrastructure.persistence;

import com.erp.platform.identity.domain.Permission;
import com.erp.platform.identity.domain.PermissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Permission aggregate.
 *
 * @since 1.0.0
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findById(Long id);

    Optional<Permission> findByPermissionCode(String permissionCode);

    Page<Permission> findByResourceCode(String resourceCode, Pageable pageable);

    Page<Permission> findByStatus(PermissionStatus status, Pageable pageable);

    Page<Permission> findByResourceCodeAndStatus(String resourceCode, PermissionStatus status, Pageable pageable);

    Page<Permission> findByActionCode(String actionCode, Pageable pageable);

    boolean existsByPermissionCode(String permissionCode);

    boolean existsByResourceCodeAndActionCode(String resourceCode, String actionCode);

    /**
     * Finds a permission by its resource and action.
     *
     * @param resource the resource
     * @param action the action
     * @return the permission if found, empty otherwise
     */
    Optional<Permission> findByResourceCodeAndActionCode(String resourceCode, String actionCode);
}
