package com.erp.platform.identity.infrastructure.persistence;

import com.erp.platform.identity.domain.Role;
import com.erp.platform.identity.domain.RoleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Role aggregate.
 *
 * @since 1.0.0
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByRoleId(UUID roleId);

    Optional<Role> findByRoleCode(String roleCode);

    Page<Role> findByTenantId(Long tenantId, Pageable pageable);

    Page<Role> findByRoleType(RoleType roleType, Pageable pageable);

    Page<Role> findByTenantIdAndRoleType(Long tenantId, RoleType roleType, Pageable pageable);

    boolean existsByRoleCode(String roleCode);

    boolean existsByRoleId(UUID roleId);

    boolean existsByTenantIdAndRoleCode(Long tenantId, String roleCode);
}
