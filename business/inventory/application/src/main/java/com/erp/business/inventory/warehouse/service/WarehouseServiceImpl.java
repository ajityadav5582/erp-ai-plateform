package com.erp.business.inventory.warehouse.service;

import com.erp.business.inventory.domain.Warehouse;
import com.erp.business.inventory.infrastructure.persistence.WarehouseRepository;
import com.erp.business.inventory.warehouse.dto.CreateWarehouseRequest;
import com.erp.business.inventory.warehouse.dto.UpdateWarehouseRequest;
import com.erp.business.inventory.warehouse.dto.WarehouseResponse;
import com.erp.business.inventory.warehouse.exception.DuplicateWarehouseCodeException;
import com.erp.business.inventory.warehouse.exception.DuplicateWarehouseNameException;
import com.erp.business.inventory.warehouse.exception.WarehouseNotFoundException;
import com.erp.business.inventory.warehouse.exception.WarehouseOperationException;
import com.erp.business.inventory.warehouse.mapper.WarehouseMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * Warehouse management, including the single-default-per-company guarantee.
 *
 * <p><strong>Provisioning is on demand, not on company creation.</strong> A company is
 * created by the identity service in a different transaction, so there is no hook here
 * to run synchronously, and an event would be lost whenever the broker is down.
 * Instead every consumer of a warehouse asks for the default through
 * {@link #getOrCreateDefault(Long, Long, Long)}, which mints one if the company has
 * none. The guarantee then holds without depending on any event arriving.
 */
@Service
@Transactional
@Slf4j
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final WarehouseMapper warehouseMapper;

    /**
     * Runs the provisioning insert on its own transaction so a lost race can be caught
     * and recovered from. See {@link #provisionDefault} for why that matters.
     */
    private final TransactionTemplate requiresNew;

    public WarehouseServiceImpl(WarehouseRepository warehouseRepository,
                                WarehouseMapper warehouseMapper,
                                PlatformTransactionManager transactionManager) {
        this.warehouseRepository = warehouseRepository;
        this.warehouseMapper = warehouseMapper;
        this.requiresNew = new TransactionTemplate(transactionManager);
        this.requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public WarehouseResponse createWarehouse(Long tenantId, Long companyId,
                                             CreateWarehouseRequest request) {
        String code = normalizeCode(request.code());

        if (warehouseRepository.existsByTenantIdAndCompanyIdAndCode(tenantId, companyId, code)) {
            throw new DuplicateWarehouseCodeException(
                    "Warehouse with code '" + code + "' already exists in this company");
        }

        String name = request.name().trim();
        if (warehouseRepository.existsByTenantIdAndCompanyIdAndName(tenantId, companyId, name)) {
            throw new DuplicateWarehouseNameException(
                    "Warehouse with name '" + name + "' already exists in this company");
        }

        Warehouse warehouse = Warehouse.create(
                tenantId, companyId, code, name, request.type(),
                request.branchId(), request.address(), request.phone());

        return warehouseMapper.toResponse(warehouseRepository.save(warehouse));
    }

    @Override
    @Transactional(readOnly = true)
    public WarehouseResponse getOrCreateDefault(Long tenantId, Long companyId, Long branchId) {
        validateScope(tenantId, companyId);

        return warehouseRepository.findDefaultWarehouse(tenantId, companyId)
                .map(warehouseMapper::toResponse)
                .orElseGet(() -> warehouseMapper.toResponse(
                        provisionDefault(tenantId, companyId, branchId)));
    }

    /**
     * Inserts the default and returns it, tolerating the case where a concurrent
     * caller got there first.
     *
     * <p><strong>How the race is handled.</strong> Two requests can both find no default
     * and both try to insert. {@code uk_warehouses_tenant_company_default} stops the
     * loser, and rather than surfacing a confusing conflict it re-reads and returns the
     * winner's row. Losing that race is the normal outcome under concurrency, not an
     * error.
     *
     * <p><strong>Why the insert needs its own transaction.</strong> It runs through an
     * explicit {@link TransactionTemplate} rather than a {@code REQUIRES_NEW} method on
     * this class: a self-invoked {@code @Transactional} method bypasses the Spring
     * proxy entirely and would silently join the caller's transaction. That matters
     * because a constraint violation marks the surrounding transaction rollback-only -
     * the re-read would then find nothing and the caller would see a spurious failure
     * instead of the default. Isolating the insert keeps the recovery possible.
     *
     * <p>The read runs outside that inner transaction, after the insert has committed,
     * so it is guaranteed to observe the winner's row.
     */
    private Warehouse provisionDefault(Long tenantId, Long companyId, Long branchId) {
        try {
            return requiresNew.execute(status -> warehouseRepository.saveAndFlush(
                    Warehouse.defaultFor(tenantId, companyId, branchId)));
        } catch (DataIntegrityViolationException ex) {
            log.debug("Lost the race to provision the default warehouse for company {}; "
                    + "re-reading the winner", companyId);
            return warehouseRepository.findDefaultWarehouse(tenantId, companyId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Failed to provision a default warehouse for company " + companyId
                                    + " and none exists", ex));
        }
    }

    @Override
    public WarehouseResponse setDefaultWarehouse(Long tenantId, Long companyId, Long id) {
        validateScope(tenantId, companyId);

        // Lock the company's whole warehouse set first. Promoting means demoting the
        // incumbent and promoting the challenger, and without the lock two concurrent
        // promotions both demote and then both promote - tripping the single-default
        // constraint and failing a request the caller made correctly. One to a handful
        // of rows, so the lock is cheap.
        List<Warehouse> locked = warehouseRepository.findAllForUpdate(tenantId, companyId);

        Warehouse target = locked.stream()
                .filter(w -> w.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new WarehouseNotFoundException(
                        "Warehouse not found with ID: " + id));

        if (Boolean.TRUE.equals(target.getIsDefault())) {
            return warehouseMapper.toResponse(target);
        }

        for (Warehouse warehouse : locked) {
            if (Boolean.TRUE.equals(warehouse.getIsDefault())) {
                warehouse.clearDefault();
            }
        }

        try {
            target.markAsDefault();
        } catch (IllegalStateException ex) {
            // An inactive warehouse cannot receive goods, so promoting it would leave
            // the company with a default nothing can be booked into.
            throw new WarehouseOperationException(ex.getMessage(), ex);
        }

        return warehouseMapper.toResponse(warehouseRepository.save(target));
    }

    @Override
    @Transactional(readOnly = true)
    public WarehouseResponse getWarehouseById(Long tenantId, Long companyId, Long id) {
        return warehouseMapper.toResponse(requireWarehouse(tenantId, companyId, id));
    }

    @Override
    @Transactional(readOnly = true)
    public WarehouseResponse getWarehouseByCode(Long tenantId, Long companyId, String code) {
        return warehouseRepository
                .findByTenantIdAndCompanyIdAndCode(tenantId, companyId, normalizeCode(code))
                .map(warehouseMapper::toResponse)
                .orElseThrow(() -> new WarehouseNotFoundException(
                        "Warehouse not found with code: " + code));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WarehouseResponse> listWarehouses(Long tenantId, Long companyId, Pageable pageable) {
        return warehouseRepository.findByTenantIdAndCompanyId(tenantId, companyId, pageable)
                .map(warehouseMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseResponse> listActiveWarehouses(Long tenantId, Long companyId) {
        return warehouseRepository
                .findByTenantIdAndCompanyIdAndIsActiveTrueOrderByCodeAsc(tenantId, companyId)
                .stream()
                .map(warehouseMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseResponse> listWarehousesByBranch(Long tenantId, Long companyId, Long branchId) {
        return warehouseRepository
                .findByTenantIdAndCompanyIdAndBranchIdOrderByCodeAsc(tenantId, companyId, branchId)
                .stream()
                .map(warehouseMapper::toResponse)
                .toList();
    }

    @Override
    public WarehouseResponse updateWarehouse(Long tenantId, Long companyId, Long id,
                                             UpdateWarehouseRequest request) {
        Warehouse warehouse = requireWarehouse(tenantId, companyId, id);

        if (request.code() != null && !request.code().isBlank()) {
            String code = normalizeCode(request.code());
            if (!code.equals(warehouse.getCode())
                    && warehouseRepository.existsByTenantIdAndCompanyIdAndCode(
                            tenantId, companyId, code)) {
                throw new DuplicateWarehouseCodeException(
                        "Warehouse with code '" + code + "' already exists in this company");
            }
        }

        if (request.name() != null && !request.name().isBlank()) {
            String name = request.name().trim();
            if (!name.equalsIgnoreCase(warehouse.getName())
                    && warehouseRepository.existsByTenantIdAndCompanyIdAndName(
                            tenantId, companyId, name)) {
                throw new DuplicateWarehouseNameException(
                        "Warehouse with name '" + name + "' already exists in this company");
            }
        }

        warehouse.updateDetails(request.code(), request.name(), request.type(),
                null, request.address(), request.phone());

        return warehouseMapper.toResponse(warehouseRepository.save(warehouse));
    }

    @Override
    public WarehouseResponse activateWarehouse(Long tenantId, Long companyId, Long id) {
        Warehouse warehouse = requireWarehouse(tenantId, companyId, id);
        if (Boolean.TRUE.equals(warehouse.getIsActive())) {
            return warehouseMapper.toResponse(warehouse);
        }
        warehouse.activate();
        return warehouseMapper.toResponse(warehouseRepository.save(warehouse));
    }

    @Override
    public WarehouseResponse deactivateWarehouse(Long tenantId, Long companyId, Long id) {
        Warehouse warehouse = requireWarehouse(tenantId, companyId, id);
        if (Boolean.FALSE.equals(warehouse.getIsActive())) {
            return warehouseMapper.toResponse(warehouse);
        }
        try {
            warehouse.deactivate();
        } catch (IllegalStateException ex) {
            throw new WarehouseOperationException(ex.getMessage(), ex);
        }
        return warehouseMapper.toResponse(warehouseRepository.save(warehouse));
    }

    // ==================== Helpers ====================

    private Warehouse requireWarehouse(Long tenantId, Long companyId, Long id) {
        return warehouseRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, id)
                .orElseThrow(() -> new WarehouseNotFoundException(
                        "Warehouse not found with ID: " + id));
    }

    private void validateScope(Long tenantId, Long companyId) {
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalArgumentException("Tenant ID must be a positive number");
        }
        if (companyId == null || companyId <= 0) {
            throw new IllegalArgumentException("Company ID must be a positive number");
        }
    }

    private String normalizeCode(String code) {
        return code == null ? null : code.trim().toUpperCase();
    }
}
