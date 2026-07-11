package com.erp.platform.identity.application;

import com.erp.platform.identity.application.dto.CreatePermissionRequest;
import com.erp.platform.identity.application.dto.PermissionListResponse;
import com.erp.platform.identity.application.dto.PermissionResponse;
import com.erp.platform.identity.application.dto.UpdatePermissionRequest;
import com.erp.platform.identity.application.mapper.PermissionMapper;
import com.erp.platform.identity.domain.Action;
import com.erp.platform.identity.domain.Permission;
import com.erp.platform.identity.domain.PermissionStatus;
import com.erp.platform.identity.domain.Resource;
import com.erp.platform.identity.domain.exception.DuplicatePermissionCodeException;
import com.erp.platform.identity.domain.exception.PermissionNotFoundException;
import com.erp.platform.identity.infrastructure.persistence.PermissionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service implementation for Permission aggregate operations.
 *
 * @since 1.0.0
 */
@Service
@Transactional(readOnly = true)
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;

    public PermissionServiceImpl(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    @Override
    @Transactional
    public PermissionResponse createPermission(CreatePermissionRequest request) {
        // Check for duplicate permission code
        String permissionCode = Permission.generatePermissionCode(request.resource(), request.action());
        if (permissionRepository.existsByPermissionCode(permissionCode)) {
            throw new DuplicatePermissionCodeException(permissionCode);
        }

        // Create permission using factory method
        Permission permission = Permission.create(
                UUID.randomUUID(),
                request.resource(),
                request.action(),
                request.description()
        );

        Permission savedPermission = permissionRepository.save(permission);
        return PermissionMapper.toResponse(savedPermission);
    }

    @Override
    @Transactional
    public PermissionResponse updatePermission(UUID permissionId, UpdatePermissionRequest request) {
        Permission permission = permissionRepository.findByPermissionId(permissionId)
                .orElseThrow(() -> PermissionNotFoundException.byPermissionId(permissionId));

        permission.updateDescription(request.description());

        Permission updatedPermission = permissionRepository.save(permission);
        return PermissionMapper.toResponse(updatedPermission);
    }

    @Override
    @Transactional
    public void deletePermission(UUID permissionId) {
        Permission permission = permissionRepository.findByPermissionId(permissionId)
                .orElseThrow(() -> PermissionNotFoundException.byPermissionId(permissionId));

        // Soft delete via deactivation
        permission.deactivate(Instant.now());

        permissionRepository.save(permission);
    }

    @Override
    public PermissionResponse getPermissionById(UUID permissionId) {
        Permission permission = permissionRepository.findByPermissionId(permissionId)
                .orElseThrow(() -> PermissionNotFoundException.byPermissionId(permissionId));
        return PermissionMapper.toResponse(permission);
    }

    @Override
    public PermissionResponse getPermissionByCode(String permissionCode) {
        Permission permission = permissionRepository.findByPermissionCode(permissionCode)
                .orElseThrow(() -> PermissionNotFoundException.byPermissionCode(permissionCode));
        return PermissionMapper.toResponse(permission);
    }

    @Override
    public PermissionListResponse listPermissions(Resource resource, Action action, PermissionStatus status, Pageable pageable) {
        Page<Permission> permissionPage;

        if (resource != null && action != null && status != null) {
            // Filter by resource and status, then filter by action in memory
            permissionPage = permissionRepository.findByResourceAndStatus(resource, status, pageable);
            List<Permission> filteredPermissions = permissionPage.getContent().stream()
                    .filter(permission -> permission.getAction() == action)
                    .toList();
            return PermissionMapper.toListResponse(
                    PermissionMapper.toResponseList(filteredPermissions),
                    permissionPage.getTotalElements(),
                    permissionPage.getTotalPages(),
                    permissionPage.getNumber(),
                    permissionPage.getSize()
            );
        } else if (resource != null && status != null) {
            permissionPage = permissionRepository.findByResourceAndStatus(resource, status, pageable);
        } else if (resource != null) {
            permissionPage = permissionRepository.findByResource(resource, pageable);
        } else if (action != null) {
            permissionPage = permissionRepository.findByAction(action, pageable);
        } else if (status != null) {
            permissionPage = permissionRepository.findByStatus(status, pageable);
        } else {
            permissionPage = permissionRepository.findAll(pageable);
        }

        return PermissionMapper.toListResponse(
                PermissionMapper.toResponseList(permissionPage.getContent()),
                permissionPage.getTotalElements(),
                permissionPage.getTotalPages(),
                permissionPage.getNumber(),
                permissionPage.getSize()
        );
    }

    @Override
    public PermissionListResponse listPermissionsByResource(Resource resource, Pageable pageable) {
        Page<Permission> permissionPage = permissionRepository.findByResource(resource, pageable);
        return PermissionMapper.toListResponse(
                PermissionMapper.toResponseList(permissionPage.getContent()),
                permissionPage.getTotalElements(),
                permissionPage.getTotalPages(),
                permissionPage.getNumber(),
                permissionPage.getSize()
        );
    }

    @Override
    public PermissionListResponse listPermissionsByStatus(PermissionStatus status, Pageable pageable) {
        Page<Permission> permissionPage = permissionRepository.findByStatus(status, pageable);
        return PermissionMapper.toListResponse(
                PermissionMapper.toResponseList(permissionPage.getContent()),
                permissionPage.getTotalElements(),
                permissionPage.getTotalPages(),
                permissionPage.getNumber(),
                permissionPage.getSize()
        );
    }

    @Override
    public PermissionListResponse listPermissionsByAction(Action action, Pageable pageable) {
        Page<Permission> permissionPage = permissionRepository.findByAction(action, pageable);
        return PermissionMapper.toListResponse(
                PermissionMapper.toResponseList(permissionPage.getContent()),
                permissionPage.getTotalElements(),
                permissionPage.getTotalPages(),
                permissionPage.getNumber(),
                permissionPage.getSize()
        );
    }

    @Override
    public boolean existsByPermissionCode(String permissionCode) {
        return permissionRepository.existsByPermissionCode(permissionCode);
    }

    @Override
    public boolean existsByPermissionId(UUID permissionId) {
        return permissionRepository.existsByPermissionId(permissionId);
    }
}
