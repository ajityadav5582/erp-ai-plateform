package com.erp.platform.identity.infrastructure.persistence;

import com.erp.platform.identity.domain.User;
import com.erp.platform.identity.domain.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for User aggregate.
 *
 * @since 1.0.0
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByIdAndTenantId(Long id, Long tenantId);

    Optional<User> findByTenantIdAndUsername(Long tenantId, String username);

    Optional<User> findByTenantIdAndEmail(Long tenantId, String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);

    Page<User> findByTenantId(Long tenantId, Pageable pageable);

    Page<User> findByTenantIdAndStatus(Long tenantId, UserStatus status, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("""
            SELECT u FROM User u
            WHERE u.tenantId = :tenantId
              AND (:status IS NULL OR u.status = :status)
              AND (CAST(:search AS string) IS NULL OR CAST(:search AS string) = '' OR
                LOWER(u.username) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR
                LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR
                LOWER(u.firstName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR
                LOWER(u.lastName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            """)
    Page<User> findByTenantIdAndSearchAndStatus(
            @org.springframework.data.repository.query.Param("tenantId") Long tenantId,
            @org.springframework.data.repository.query.Param("search") String search,
            @org.springframework.data.repository.query.Param("status") UserStatus status,
            Pageable pageable);

    Page<User> findByTenantIdAndBranchId(Long tenantId, Long branchId, Pageable pageable);

    /**
     * Finds the given users, restricted to a single tenant.
     *
     * <p>Resolves the users behind a page of branch assignments in one query
     * instead of one {@code findById} per row.
     *
     * @param ids the user identifiers to load
     * @param tenantId the tenant ID
     * @return the users owned by the tenant, never another tenant's rows
     */
    java.util.List<User> findAllByIdInAndTenantId(java.util.Collection<Long> ids, Long tenantId);

    Page<User> findByTenantIdAndDepartmentId(Long tenantId, Long departmentId, Pageable pageable);

    boolean existsByTenantIdAndUsername(Long tenantId, String username);

    boolean existsByTenantIdAndEmail(Long tenantId, String email);

}
