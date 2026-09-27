package com.erp.platform.identity.infrastructure.persistence;

import com.erp.platform.identity.domain.Role;
import com.erp.platform.identity.domain.RoleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Role aggregate.
 *
 * @since 1.0.0
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Page<Role> findByTenantId(Long tenantId, Pageable pageable);

    Page<Role> findByRoleType(RoleType roleType, Pageable pageable);

    Page<Role> findByTenantIdAndRoleType(Long tenantId, RoleType roleType, Pageable pageable);

    boolean existsByTenantIdAndRoleCode(Long tenantId, String roleCode);

    /**
     * Finds a role by tenant ID and role code.
     *
     * @param tenantId the tenant ID
     * @param roleCode the role code
     * @return the role if found, empty otherwise
     */
    Optional<Role> findByTenantIdAndRoleCode(Long tenantId, String roleCode);

    Optional<Role> findByTenantIdAndRoleCodeAndRoleType(Long tenantId, String roleCode, RoleType roleType);

    /**
     * Finds a role by database ID and tenant ID.
     *
     * @param id the database ID
     * @param tenantId the tenant ID
     * @return the role if found, empty otherwise
     */
    Optional<Role> findByIdAndTenantId(Long id, Long tenantId);

    @Query("SELECT r FROM Role r WHERE r.id = :id AND (r.tenantId = :tenantId OR (r.tenantId IS NULL AND r.roleType = com.erp.platform.identity.domain.RoleType.SYSTEM))")
    Optional<Role> findByIdAndTenantIdOrGlobalSystem(@Param("id") Long id, @Param("tenantId") Long tenantId);

}
