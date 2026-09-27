package com.erp.platform.identity.infrastructure.persistence;

import com.erp.platform.identity.domain.UserRole;
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

/** Tenant-aware queries for user-role associations; tenant ownership comes from the user and role rows. */
@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    @Query("SELECT ur FROM UserRole ur JOIN User u ON u.id = ur.userId WHERE ur.id = :id AND u.tenantId = :tenantId")
    Optional<UserRole> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("""
            SELECT ur FROM UserRole ur JOIN User u ON u.id = ur.userId
            WHERE ur.userId = :userId AND u.tenantId = :tenantId AND ur.active = TRUE
              AND (ur.expiresAt IS NULL OR ur.expiresAt > :now)
            """)
    List<UserRole> findActiveByUserIdAndTenantId(
            @Param("userId") Long userId, @Param("tenantId") Long tenantId, @Param("now") LocalDateTime now);

    @Query("""
            SELECT ur FROM UserRole ur JOIN User u ON u.id = ur.userId
            WHERE ur.userId = :userId AND u.tenantId = :tenantId AND ur.isPrimaryRole = TRUE AND ur.active = TRUE
              AND (ur.expiresAt IS NULL OR ur.expiresAt > :now)
            """)
    Optional<UserRole> findPrimaryByUserIdAndTenantId(
            @Param("userId") Long userId, @Param("tenantId") Long tenantId, @Param("now") LocalDateTime now);

    @Query("""
            SELECT ur FROM UserRole ur JOIN Role r ON r.id = ur.roleId JOIN User u ON u.id = ur.userId
            WHERE ur.roleId = :roleId AND (r.tenantId = :tenantId OR (r.tenantId IS NULL AND r.roleType = com.erp.platform.identity.domain.RoleType.SYSTEM))
              AND u.tenantId = :tenantId AND ur.active = TRUE
              AND (ur.expiresAt IS NULL OR ur.expiresAt > :now)
            """)
    List<UserRole> findActiveByRoleIdAndTenantId(
            @Param("roleId") Long roleId, @Param("tenantId") Long tenantId, @Param("now") LocalDateTime now);

    @Query("""
            SELECT COUNT(ur) > 0 FROM UserRole ur JOIN User u ON u.id = ur.userId JOIN Role r ON r.id = ur.roleId
            WHERE ur.userId = :userId AND u.tenantId = :tenantId AND ur.roleId = :roleId
              AND (r.tenantId = :tenantId OR (r.tenantId IS NULL AND r.roleType = com.erp.platform.identity.domain.RoleType.SYSTEM))
              AND ur.active = TRUE AND (ur.expiresAt IS NULL OR ur.expiresAt > :now)
            """)
    boolean existsActiveByUserIdAndTenantIdAndRoleId(
            @Param("userId") Long userId, @Param("tenantId") Long tenantId,
            @Param("roleId") Long roleId, @Param("now") LocalDateTime now);

    @Query("SELECT ur FROM UserRole ur JOIN User u ON u.id = ur.userId WHERE ur.userId = :userId AND u.tenantId = :tenantId")
    List<UserRole> findByUserIdAndTenantId(@Param("userId") Long userId, @Param("tenantId") Long tenantId);

    @Query("SELECT ur FROM UserRole ur JOIN Role r ON r.id = ur.roleId JOIN User u ON u.id = ur.userId WHERE ur.roleId = :roleId AND (r.tenantId = :tenantId OR (r.tenantId IS NULL AND r.roleType = com.erp.platform.identity.domain.RoleType.SYSTEM)) AND u.tenantId = :tenantId")
    List<UserRole> findByRoleIdAndTenantId(@Param("roleId") Long roleId, @Param("tenantId") Long tenantId);

    @Query("SELECT ur FROM UserRole ur JOIN User u ON u.id = ur.userId WHERE u.tenantId = :tenantId")
    Page<UserRole> findByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query("""
            SELECT ur FROM UserRole ur JOIN User u ON u.id = ur.userId
            WHERE u.tenantId = :tenantId AND ur.active = TRUE
              AND (ur.expiresAt IS NULL OR ur.expiresAt > :now)
            """)
    Page<UserRole> findActiveByTenantId(
            @Param("tenantId") Long tenantId, @Param("now") LocalDateTime now, Pageable pageable);

    @Query("""
            SELECT ur FROM UserRole ur JOIN User u ON u.id = ur.userId
            WHERE u.tenantId = :tenantId AND ur.expiresAt IS NOT NULL AND ur.expiresAt <= :now AND ur.active = TRUE
            """)
    List<UserRole> findExpiredByTenantId(@Param("tenantId") Long tenantId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM UserRole ur WHERE ur.userId = :userId AND EXISTS (SELECT u.id FROM User u WHERE u.id = ur.userId AND u.tenantId = :tenantId)")
    long deleteByUserIdAndTenantId(@Param("userId") Long userId, @Param("tenantId") Long tenantId);

    @Modifying
    @Query("DELETE FROM UserRole ur WHERE ur.roleId = :roleId AND EXISTS (SELECT r.id FROM Role r WHERE r.id = ur.roleId AND (r.tenantId = :tenantId OR (r.tenantId IS NULL AND r.roleType = com.erp.platform.identity.domain.RoleType.SYSTEM))) AND EXISTS (SELECT u.id FROM User u WHERE u.id = ur.userId AND u.tenantId = :tenantId)")
    long deleteByRoleIdAndTenantId(@Param("roleId") Long roleId, @Param("tenantId") Long tenantId);

    @Query("""
            SELECT ur FROM UserRole ur JOIN User u ON u.id = ur.userId JOIN Role r ON r.id = ur.roleId
            WHERE ur.userId = :userId AND u.tenantId = :tenantId AND ur.roleId = :roleId
              AND (r.tenantId = :tenantId OR (r.tenantId IS NULL AND r.roleType = com.erp.platform.identity.domain.RoleType.SYSTEM))
            """)
    Optional<UserRole> findByUserIdAndTenantIdAndRoleId(
            @Param("userId") Long userId, @Param("tenantId") Long tenantId, @Param("roleId") Long roleId);

    @Query("SELECT ur FROM UserRole ur JOIN User u ON u.id = ur.userId WHERE ur.userId = :userId AND u.tenantId = :tenantId")
    Page<UserRole> findByUserIdAndTenantId(
            @Param("userId") Long userId, @Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT ur FROM UserRole ur JOIN Role r ON r.id = ur.roleId JOIN User u ON u.id = ur.userId WHERE ur.roleId = :roleId AND (r.tenantId = :tenantId OR (r.tenantId IS NULL AND r.roleType = com.erp.platform.identity.domain.RoleType.SYSTEM)) AND u.tenantId = :tenantId")
    Page<UserRole> findByRoleIdAndTenantId(
            @Param("roleId") Long roleId, @Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT COUNT(ur) > 0 FROM UserRole ur JOIN User u ON u.id = ur.userId WHERE ur.id = :id AND u.tenantId = :tenantId")
    boolean existsByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);
}
