package com.erp.business.pos.register;

import com.erp.business.pos.domain.PosRegister;
import com.erp.business.pos.domain.PosRegisterStatus;
import com.erp.business.pos.register.dto.CreatePosRegisterRequest;
import com.erp.business.pos.register.dto.PosRegisterResponse;
import com.erp.business.pos.register.dto.UpdatePosRegisterRequest;
import com.erp.business.pos.register.exception.DuplicatePosRegisterCodeException;
import com.erp.business.pos.register.exception.PosRegisterNotFoundException;
import com.erp.business.pos.register.infrastructure.persistence.PosRegisterRepository;
import com.erp.business.pos.register.mapper.PosRegisterMapper;
import com.erp.business.pos.shared.security.PosRegisterPermissions;
import com.erp.platform.security.annotation.RequiresPermission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * POS register master use-case implementation.
 *
 * <p>Authorisation is declared per method with {@code @RequiresPermission} and
 * enforced by the shared aspect before the body runs. Code uniqueness is checked
 * here before the save and again by the database unique constraint, so a race
 * between two concurrent creates surfaces as a conflict rather than a duplicate row.
 */
@Service
@Transactional
public class PosRegisterServiceImpl implements PosRegisterService {

    private final PosRegisterRepository registerRepository;
    private final PosRegisterMapper registerMapper;

    public PosRegisterServiceImpl(PosRegisterRepository registerRepository, PosRegisterMapper registerMapper) {
        this.registerRepository = registerRepository;
        this.registerMapper = registerMapper;
    }

    @Override
    @RequiresPermission(PosRegisterPermissions.POS_REGISTER_CREATE)
    public PosRegisterResponse createRegister(Long tenantId, Long companyId, CreatePosRegisterRequest request) {
        String code = request.registerCode().trim().toUpperCase();
        if (registerRepository.existsByTenantIdAndCompanyIdAndRegisterCode(tenantId, companyId, code)) {
            throw new DuplicatePosRegisterCodeException(
                    "Register with code '" + code + "' already exists in this company");
        }

        PosRegister register = PosRegister.create(
                code, request.registerName(),
                request.warehouseId(), request.warehouseCode(), request.warehouseName(),
                request.branchId(), request.branchName(), request.notes());
        register.assignScope(tenantId, companyId);

        return registerMapper.toResponse(registerRepository.save(register));
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(PosRegisterPermissions.POS_REGISTER_VIEW)
    public PosRegisterResponse getRegisterById(Long tenantId, Long companyId, Long id) {
        return registerMapper.toResponse(load(tenantId, companyId, id));
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(PosRegisterPermissions.POS_REGISTER_VIEW)
    public PosRegisterResponse getRegisterByCode(Long tenantId, Long companyId, String registerCode) {
        PosRegister register = registerRepository
                .findByTenantIdAndCompanyIdAndRegisterCode(tenantId, companyId, registerCode.trim().toUpperCase())
                .orElseThrow(() -> new PosRegisterNotFoundException(
                        "Register not found with code: " + registerCode));
        return registerMapper.toResponse(register);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(PosRegisterPermissions.POS_REGISTER_VIEW)
    public Page<PosRegisterResponse> listRegisters(Long tenantId, Long companyId, Pageable pageable) {
        return registerRepository.findByTenantIdAndCompanyId(tenantId, companyId, pageable)
                .map(registerMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(PosRegisterPermissions.POS_REGISTER_VIEW)
    public Page<PosRegisterResponse> listRegistersByStatus(Long tenantId, Long companyId,
                                                           PosRegisterStatus status, Pageable pageable) {
        return registerRepository.findByTenantIdAndCompanyIdAndStatus(tenantId, companyId, status, pageable)
                .map(registerMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(PosRegisterPermissions.POS_REGISTER_VIEW)
    public List<PosRegisterResponse> listActiveRegisters(Long tenantId, Long companyId) {
        return registerRepository.findByTenantIdAndCompanyIdAndStatus(tenantId, companyId, PosRegisterStatus.ACTIVE)
                .stream()
                .map(registerMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(PosRegisterPermissions.POS_REGISTER_VIEW)
    public Page<PosRegisterResponse> searchRegisters(Long tenantId, Long companyId, String term, Pageable pageable) {
        return registerRepository.search(tenantId, companyId, term, pageable)
                .map(registerMapper::toResponse);
    }

    @Override
    @RequiresPermission(PosRegisterPermissions.POS_REGISTER_UPDATE)
    public PosRegisterResponse updateRegister(Long tenantId, Long companyId, Long id, UpdatePosRegisterRequest request) {
        PosRegister register = load(tenantId, companyId, id);
        register.updateDetails(request.registerName(), request.branchId(), request.branchName(), request.notes());
        return registerMapper.toResponse(registerRepository.save(register));
    }

    @Override
    @RequiresPermission(PosRegisterPermissions.POS_REGISTER_MANAGE)
    public PosRegisterResponse activateRegister(Long tenantId, Long companyId, Long id) {
        PosRegister register = load(tenantId, companyId, id);
        register.activate();
        return registerMapper.toResponse(registerRepository.save(register));
    }

    @Override
    @RequiresPermission(PosRegisterPermissions.POS_REGISTER_MANAGE)
    public PosRegisterResponse deactivateRegister(Long tenantId, Long companyId, Long id) {
        PosRegister register = load(tenantId, companyId, id);
        register.deactivate();
        return registerMapper.toResponse(registerRepository.save(register));
    }

    private PosRegister load(Long tenantId, Long companyId, Long id) {
        return registerRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, id)
                .orElseThrow(() -> new PosRegisterNotFoundException("Register not found with ID: " + id));
    }
}
