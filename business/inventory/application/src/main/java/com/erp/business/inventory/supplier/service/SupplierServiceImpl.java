package com.erp.business.inventory.supplier.service;

import com.erp.business.inventory.domain.Supplier;
import com.erp.business.inventory.domain.SupplierType;
import com.erp.business.inventory.supplier.dto.SupplierCreateRequest;
import com.erp.business.inventory.supplier.dto.SupplierResponse;
import com.erp.business.inventory.supplier.dto.SupplierUpdateRequest;
import com.erp.business.inventory.supplier.exception.DuplicateSupplierCodeException;
import com.erp.business.inventory.supplier.exception.DuplicateSupplierNameException;
import com.erp.business.inventory.supplier.exception.SupplierNotFoundException;
import com.erp.business.inventory.supplier.mapper.SupplierMapper;
import com.erp.business.inventory.infrastructure.persistence.SupplierRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@Transactional
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;
    private final SupplierMapper supplierMapper;

    public SupplierServiceImpl(SupplierRepository supplierRepository, SupplierMapper supplierMapper) {
        this.supplierRepository = supplierRepository;
        this.supplierMapper = supplierMapper;
    }

    @Override
    public SupplierResponse createSupplier(Long tenantId, Long companyId, SupplierCreateRequest request) {
        String code = normalizeCode(request.code());

        if (supplierRepository.existsByTenantIdAndCompanyIdAndCode(tenantId, companyId, code)) {
            throw new DuplicateSupplierCodeException(
                    "Supplier with code '" + code + "' already exists in this company"
            );
        }

        String name = request.name().trim();
        if (supplierRepository.existsByTenantIdAndCompanyIdAndName(tenantId, companyId, name)) {
            throw new DuplicateSupplierNameException(
                    "Supplier with name '" + name + "' already exists in this company"
            );
        }

        Supplier supplier = Supplier.create(
                tenantId, companyId, name, code, request.type(),
                request.email(), request.phone(), request.address(),
                request.taxId(), request.paymentTermsDays(), request.currency()
        );

        Supplier saved = supplierRepository.save(supplier);
        return supplierMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierResponse getSupplierById(Long tenantId, Long id) {
        Supplier supplier = supplierRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new SupplierNotFoundException("Supplier not found with ID: " + id));
        return supplierMapper.toResponse(supplier);
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierResponse getSupplierByCode(Long tenantId, Long companyId, String code) {
        Supplier supplier = supplierRepository.findByTenantIdAndCompanyIdAndCode(tenantId, companyId, normalizeCode(code))
                .orElseThrow(() -> new SupplierNotFoundException(
                        "Supplier not found with code: " + code
                ));
        return supplierMapper.toResponse(supplier);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupplierResponse> listSuppliers(Long tenantId, Long companyId, Pageable pageable) {
        return supplierRepository.findByTenantIdAndCompanyId(tenantId, companyId, pageable)
                .map(supplierMapper::toResponse);
    }

    @Override
    public SupplierResponse updateSupplier(Long tenantId, Long id, SupplierUpdateRequest request) {
        Supplier supplier = supplierRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new SupplierNotFoundException("Supplier not found with ID: " + id));

        Long companyId = supplier.getCompanyId();

        if (request.code() != null) {
            String code = normalizeCode(request.code());
            if (!code.equals(supplier.getCode())
                    && supplierRepository.existsByTenantIdAndCompanyIdAndCode(tenantId, companyId, code)) {
                throw new DuplicateSupplierCodeException(
                        "Supplier with code '" + code + "' already exists in this company"
                );
            }
        }

        if (request.name() != null) {
            String name = request.name().trim();
            if (!name.equalsIgnoreCase(supplier.getName())
                    && supplierRepository.existsByTenantIdAndCompanyIdAndName(tenantId, companyId, name)) {
                throw new DuplicateSupplierNameException(
                        "Supplier with name '" + name + "' already exists in this company"
                );
            }
        }

        supplier.update(
                request.name(), request.code(), request.type(),
                request.email(), request.phone(), request.address(),
                request.taxId(), request.paymentTermsDays(), request.currency()
        );

        return supplierMapper.toResponse(supplierRepository.save(supplier));
    }

    @Override
    public SupplierResponse activateSupplier(Long tenantId, Long id) {
        Supplier supplier = supplierRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new SupplierNotFoundException("Supplier not found with ID: " + id));
        if (Boolean.TRUE.equals(supplier.getIsActive())) {
            return supplierMapper.toResponse(supplier);
        }
        supplier.activate();
        return supplierMapper.toResponse(supplierRepository.save(supplier));
    }

    @Override
    public SupplierResponse deactivateSupplier(Long tenantId, Long id) {
        Supplier supplier = supplierRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new SupplierNotFoundException("Supplier not found with ID: " + id));
        if (Boolean.FALSE.equals(supplier.getIsActive())) {
            return supplierMapper.toResponse(supplier);
        }
        supplier.deactivate();
        return supplierMapper.toResponse(supplierRepository.save(supplier));
    }

    @Override
    public void deleteSupplier(Long tenantId, Long id) {
        Supplier supplier = supplierRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new SupplierNotFoundException("Supplier not found with ID: " + id));
        supplierRepository.delete(supplier);
    }

    private String normalizeCode(String code) {
        return code == null ? null : code.trim().toUpperCase();
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant != null ? LocalDateTime.ofInstant(instant, ZoneId.systemDefault()) : null;
    }
}
