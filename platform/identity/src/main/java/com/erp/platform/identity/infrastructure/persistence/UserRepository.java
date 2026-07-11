package com.erp.platform.identity.infrastructure.persistence;

import com.erp.platform.identity.domain.User;
import com.erp.platform.identity.domain.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for User aggregate.
 *
 * @since 1.0.0
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUserId(UUID userId);

    Optional<User> findByTenantIdAndUserId(Long tenantId, UUID userId);

    Optional<User> findByTenantIdAndUsername(Long tenantId, String username);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByTenantIdAndEmail(Long tenantId, String email);

    Page<User> findByTenantId(Long tenantId, Pageable pageable);

    Page<User> findByTenantIdAndStatus(Long tenantId, UserStatus status, Pageable pageable);

    Page<User> findByTenantIdAndBranchId(Long tenantId, Long branchId, Pageable pageable);

    Page<User> findByTenantIdAndDepartmentId(Long tenantId, Long departmentId, Pageable pageable);

    boolean existsByTenantIdAndUsername(Long tenantId, String username);

    boolean existsByTenantIdAndEmail(Long tenantId, String email);

    boolean existsByUserId(UUID userId);
}
