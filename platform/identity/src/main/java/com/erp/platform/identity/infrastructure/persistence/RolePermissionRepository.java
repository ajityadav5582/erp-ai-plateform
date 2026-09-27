package com.erp.platform.identity.infrastructure.persistence;

import com.erp.platform.identity.domain.RolePermission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Persistence operations for role-to-permission assignments. */
@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    @Query("SELECT rp FROM RolePermission rp WHERE rp.id = :id AND rp.role.tenantId = :tenantId")
    Optional<RolePermission> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("SELECT rp FROM RolePermission rp WHERE rp.role.id = :roleId AND rp.role.tenantId = :tenantId")
    List<RolePermission> findByRoleIdAndTenantId(@Param("roleId") Long roleId, @Param("tenantId") Long tenantId);

    @Query("SELECT rp FROM RolePermission rp WHERE rp.role.id = :roleId AND rp.role.tenantId = :tenantId")
    Page<RolePermission> findByRoleIdAndTenantId(
            @Param("roleId") Long roleId, @Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT rp FROM RolePermission rp WHERE rp.permission.id = :permissionId AND rp.role.tenantId = :tenantId")
    List<RolePermission> findByPermissionIdAndTenantId(
            @Param("permissionId") Long permissionId, @Param("tenantId") Long tenantId);

    @Query("SELECT COUNT(rp) > 0 FROM RolePermission rp WHERE rp.role.id = :roleId AND rp.role.tenantId = :tenantId AND rp.permission.id = :permissionId")
    boolean existsByRoleIdAndTenantIdAndPermissionId(
            @Param("roleId") Long roleId, @Param("tenantId") Long tenantId, @Param("permissionId") Long permissionId);

    @Query("SELECT COUNT(rp) > 0 FROM RolePermission rp WHERE rp.role.tenantId = :tenantId AND rp.role.id = :roleId AND rp.permission.id = :permissionId")
    boolean existsByTenantIdRoleIdAndPermissionId(
            @Param("tenantId") Long tenantId, @Param("roleId") Long roleId, @Param("permissionId") Long permissionId);

    @Query("SELECT rp FROM RolePermission rp WHERE rp.role.tenantId = :tenantId AND rp.role.id = :roleId AND rp.permission.id = :permissionId")
    Optional<RolePermission> findByTenantIdRoleIdAndPermissionId(
            @Param("tenantId") Long tenantId, @Param("roleId") Long roleId, @Param("permissionId") Long permissionId);

    @Modifying(clearAutomatically = true)
    @Query(value = """
            INSERT INTO role_permissions (role_id, permission_id, assigned_by, assigned_at, active)
            SELECT :roleId, :permissionId, :assignedBy, :assignedAt, TRUE
            WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = :roleId AND permission_id = :permissionId)
            """, nativeQuery = true)
    int insertIdempotent(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId,
                         @Param("assignedBy") String assignedBy, @Param("assignedAt") LocalDateTime assignedAt);

    @Modifying(clearAutomatically = true)
    @Query(value = """
            INSERT INTO role_permissions (role_id, permission_id, assigned_by, assigned_at, active)
            VALUES (:roleId, :permissionId, :assignedBy, :assignedAt, TRUE)
            ON CONFLICT (role_id, permission_id) DO NOTHING
            """, nativeQuery = true)
    int insertOnConflictDoNothing(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId,
                                 @Param("assignedBy") String assignedBy, @Param("assignedAt") LocalDateTime assignedAt);

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM role_permissions WHERE role_id = :roleId AND permission_id NOT IN (:permissionIds)",
            nativeQuery = true)
    int deleteRolePermissionsExcept(@Param("roleId") Long roleId, @Param("permissionIds") List<Long> permissionIds);

    @Query("SELECT rp.permission.id FROM RolePermission rp WHERE rp.role.id = :roleId AND rp.role.tenantId = :tenantId AND rp.active = TRUE")
    List<Long> findPermissionIdsByRoleIdAndTenantId(@Param("roleId") Long roleId, @Param("tenantId") Long tenantId);

    @Query("SELECT rp.role.id FROM RolePermission rp WHERE rp.permission.id = :permissionId AND rp.role.tenantId = :tenantId AND rp.active = TRUE")
    List<Long> findRoleIdsByPermissionIdAndTenantId(
            @Param("permissionId") Long permissionId, @Param("tenantId") Long tenantId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM RolePermission rp WHERE rp.role.id = :roleId AND rp.role.tenantId = :tenantId")
    long deleteByRoleIdAndTenantId(@Param("roleId") Long roleId, @Param("tenantId") Long tenantId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM RolePermission rp WHERE rp.permission.id = :permissionId AND rp.role.tenantId = :tenantId")
    long deleteByPermissionIdAndTenantId(@Param("permissionId") Long permissionId, @Param("tenantId") Long tenantId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM RolePermission rp WHERE rp.role.id = :roleId AND rp.permission.id = :permissionId AND rp.role.tenantId = :tenantId")
    long deleteByRoleIdAndTenantIdAndPermissionId(
            @Param("roleId") Long roleId, @Param("tenantId") Long tenantId, @Param("permissionId") Long permissionId);

    @Query("""
            SELECT ur.roleId FROM UserRole ur JOIN Role r ON ur.roleId = r.id JOIN User u ON ur.userId = u.id
            WHERE ur.userId = :userId AND u.tenantId = :tenantId
              AND (r.tenantId = :tenantId OR (r.tenantId IS NULL AND r.roleType = com.erp.platform.identity.domain.RoleType.SYSTEM))
              AND ur.active = TRUE AND (ur.expiresAt IS NULL OR ur.expiresAt > CURRENT_TIMESTAMP)
            """)
    List<Long> findRoleIdsByUserIdAndTenantId(@Param("userId") Long userId, @Param("tenantId") Long tenantId);

    @Query("""
            SELECT DISTINCT p.permissionCode FROM RolePermission rp JOIN rp.permission p
            WHERE rp.role.id IN :roleIds
              AND (rp.role.tenantId = :tenantId OR (rp.role.tenantId IS NULL AND rp.role.roleType = com.erp.platform.identity.domain.RoleType.SYSTEM))
              AND rp.active = TRUE
              AND p.status = com.erp.platform.identity.domain.PermissionStatus.ACTIVE
            """)
    List<String> findPermissionCodesByRoleIdsAndTenantId(
            @Param("roleIds") List<Long> roleIds, @Param("tenantId") Long tenantId);
}
