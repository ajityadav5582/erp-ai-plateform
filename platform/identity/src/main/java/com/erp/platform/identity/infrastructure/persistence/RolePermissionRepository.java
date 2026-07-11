package com.erp.platform.identity.infrastructure.persistence;

import com.erp.platform.identity.domain.RolePermission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for RolePermission association entity.
 *
 * <p>Provides methods for managing the many-to-many relationship between
 * Role and Permission with assignment metadata.
 *
 * @since 1.0.0
 */
@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    Optional<RolePermission> findByRolePermissionId(UUID rolePermissionId);

    /**
     * Finds all permission assignments for a role.
     *
     * @param roleId the role ID
     * @return list of role permission assignments
     */
    List<RolePermission> findByRoleId(Long roleId);

    /**
     * Finds all permission assignments for a role with pagination.
     *
     * @param roleId the role ID
     * @param pageable the pagination parameters
     * @return page of role permission assignments
     */
    Page<RolePermission> findByRoleId(Long roleId, Pageable pageable);

    /**
     * Finds all role assignments for a permission.
     *
     * @param permissionId the permission ID
     * @return list of role permission assignments
     */
    List<RolePermission> findByPermissionId(Long permissionId);

    /**
     * Checks if a role has a specific permission assigned.
     *
     * @param roleId the role ID
     * @param permissionId the permission ID
     * @return true if the role has the permission, false otherwise
     */
    boolean existsByRoleIdAndPermissionId(Long roleId, Long permissionId);

    /**
     * Finds all permissions assigned to a role.
     *
     * @param roleId the role ID
     * @return list of permission IDs assigned to the role
     */
    @Query("SELECT rp.permissionId FROM RolePermission rp WHERE rp.roleId = :roleId")
    List<Long> findPermissionIdsByRoleId(@Param("roleId") Long roleId);

    /**
     * Finds all roles that have a specific permission assigned.
     *
     * @param permissionId the permission ID
     * @return list of role IDs that have the permission
     */
    @Query("SELECT rp.roleId FROM RolePermission rp WHERE rp.permissionId = :permissionId")
    List<Long> findRoleIdsByPermissionId(@Param("permissionId") Long permissionId);

    /**
     * Deletes all permission assignments for a role.
     *
     * @param roleId the role ID
     * @return number of deleted assignments
     */
    long deleteByRoleId(Long roleId);

    /**
     * Deletes all role assignments for a permission.
     *
     * @param permissionId the permission ID
     * @return number of deleted assignments
     */
    long deleteByPermissionId(Long permissionId);

    /**
     * Deletes a specific role-permission assignment.
     *
     * @param roleId the role ID
     * @param permissionId the permission ID
     * @return number of deleted assignments (0 or 1)
     */
    long deleteByRoleIdAndPermissionId(Long roleId, Long permissionId);

    boolean existsByRolePermissionId(UUID rolePermissionId);
}
