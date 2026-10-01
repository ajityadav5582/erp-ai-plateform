package com.erp.business.inventory.unit.service;

import com.erp.business.inventory.domain.Unit;
import com.erp.business.inventory.infrastructure.persistence.UnitRepository;
import com.erp.business.inventory.unit.dto.UnitCreateRequest;
import com.erp.business.inventory.unit.dto.UnitResponse;
import com.erp.business.inventory.unit.dto.UnitUpdateRequest;
import com.erp.business.inventory.unit.exception.DuplicateUnitCodeException;
import com.erp.business.inventory.unit.exception.DuplicateUnitNameException;
import com.erp.business.inventory.unit.exception.UnitNotFoundException;
import com.erp.business.inventory.unit.mapper.UnitMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Default {@link UnitService} implementation.
 *
 * <p>Reads run {@code readOnly = true} so Hibernate can skip dirty checking and
 * flushes, which matters more here than usual: the list endpoint maps a whole
 * page of entities and a needless flush on a read path is pure overhead.
 */
@Service
@Transactional
public class UnitServiceImpl implements UnitService {

    /** Guards against a pathological {@code %} term collapsing into a full scan. */
    private static final int MAX_SEARCH_TERM_LENGTH = 100;

    private final UnitRepository unitRepository;
    private final UnitMapper unitMapper;

    public UnitServiceImpl(UnitRepository unitRepository, UnitMapper unitMapper) {
        this.unitRepository = unitRepository;
        this.unitMapper = unitMapper;
    }

    @Override
    public UnitResponse createUnit(Long tenantId, Long companyId, UnitCreateRequest request) {
        String code = normalizeCode(request.code());

        if (unitRepository.existsByTenantIdAndCompanyIdAndCode(tenantId, companyId, code)) {
            throw new DuplicateUnitCodeException(
                    "Unit with code '" + code + "' already exists in this company"
            );
        }

        String name = request.name().trim();
        if (unitRepository.existsByTenantIdAndCompanyIdAndNameIgnoreCase(tenantId, companyId, name)) {
            throw new DuplicateUnitNameException(
                    "Unit with name '" + name + "' already exists in this company"
            );
        }

        Unit unit = Unit.create(
                tenantId, companyId, name, code,
                request.dimension(), request.symbol(), request.decimalScale()
        );

        return unitMapper.toResponse(unitRepository.save(unit));
    }

    @Override
    @Transactional(readOnly = true)
    public UnitResponse getUnitById(Long tenantId, Long id) {
        return unitMapper.toResponse(findOrThrow(tenantId, id));
    }

    @Override
    @Transactional(readOnly = true)
    public UnitResponse getUnitByCode(Long tenantId, Long companyId, String code) {
        Unit unit = unitRepository.findByTenantIdAndCompanyIdAndCode(tenantId, companyId, normalizeCode(code))
                .orElseThrow(() -> new UnitNotFoundException("Unit not found with code: " + code));
        return unitMapper.toResponse(unit);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UnitResponse> listUnits(Long tenantId, Long companyId, Pageable pageable) {
        return unitRepository.findByTenantIdAndCompanyId(tenantId, companyId, pageable)
                .map(unitMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UnitResponse> searchUnits(Long tenantId, Long companyId, String term, Pageable pageable) {
        String normalized = normalizeSearchTerm(term);

        // An empty term would make CONCAT('%', '', '%') match every row, which is
        // correct but wasteful: fall through to the plain scoped listing instead.
        if (normalized == null) {
            return listUnits(tenantId, companyId, pageable);
        }

        return unitRepository.search(tenantId, companyId, normalized, pageable)
                .map(unitMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UnitResponse> listActiveUnits(Long tenantId, Long companyId) {
        return unitRepository.findByTenantIdAndCompanyIdAndIsActiveTrue(tenantId, companyId)
                .stream()
                .map(unitMapper::toResponse)
                .toList();
    }

    @Override
    public UnitResponse updateUnit(Long tenantId, Long id, UnitUpdateRequest request) {
        Unit unit = findOrThrow(tenantId, id);
        Long companyId = unit.getCompanyId();

        // Only re-check uniqueness when the value actually moves. Re-running the
        // lookup for an unchanged value would make every no-op save pay for a query.
        if (request.code() != null) {
            String code = normalizeCode(request.code());
            if (!code.equals(unit.getCode())
                    && unitRepository.existsByTenantIdAndCompanyIdAndCode(tenantId, companyId, code)) {
                throw new DuplicateUnitCodeException(
                        "Unit with code '" + code + "' already exists in this company"
                );
            }
        }

        if (request.name() != null) {
            String name = request.name().trim();
            if (!name.equalsIgnoreCase(unit.getName())
                    && unitRepository.existsByTenantIdAndCompanyIdAndNameIgnoreCase(tenantId, companyId, name)) {
                throw new DuplicateUnitNameException(
                        "Unit with name '" + name + "' already exists in this company"
                );
            }
        }

        unit.update(
                request.name(), request.code(), request.dimension(),
                request.symbol(), request.decimalScale()
        );

        return unitMapper.toResponse(unitRepository.save(unit));
    }

    @Override
    public UnitResponse activateUnit(Long tenantId, Long id) {
        Unit unit = findOrThrow(tenantId, id);
        if (Boolean.TRUE.equals(unit.getIsActive())) {
            // Idempotent by design: an activate on an already-active unit is a
            // no-op rather than an error, so a double-clicked button cannot fail.
            return unitMapper.toResponse(unit);
        }
        unit.activate();
        return unitMapper.toResponse(unitRepository.save(unit));
    }

    @Override
    public UnitResponse deactivateUnit(Long tenantId, Long id) {
        Unit unit = findOrThrow(tenantId, id);
        if (Boolean.FALSE.equals(unit.getIsActive())) {
            return unitMapper.toResponse(unit);
        }
        unit.deactivate();
        return unitMapper.toResponse(unitRepository.save(unit));
    }

    @Override
    public void deleteUnit(Long tenantId, Long id) {
        unitRepository.delete(findOrThrow(tenantId, id));
    }

    private Unit findOrThrow(Long tenantId, Long id) {
        return unitRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new UnitNotFoundException("Unit not found with ID: " + id));
    }

    private String normalizeCode(String code) {
        return code == null ? null : code.trim().toUpperCase();
    }

    private String normalizeSearchTerm(String term) {
        if (term == null) {
            return null;
        }
        String trimmed = term.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() > MAX_SEARCH_TERM_LENGTH ? trimmed.substring(0, MAX_SEARCH_TERM_LENGTH) : trimmed;
    }
}
