package com.erp.business.pos.register;

import com.erp.business.pos.domain.PosRegisterStatus;
import com.erp.business.pos.register.dto.CreatePosRegisterRequest;
import com.erp.business.pos.register.dto.PosRegisterResponse;
import com.erp.business.pos.register.dto.UpdatePosRegisterRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * POS register (terminal) use cases.
 *
 * <p>Every method is tenant- and company-scoped by its leading parameters; the
 * controller supplies them from the authenticated request context, never from the
 * payload. Authorisation is declared with {@code @RequiresPermission} on the
 * implementation and enforced by the shared aspect before the body runs.
 */
public interface PosRegisterService {

    PosRegisterResponse createRegister(Long tenantId, Long companyId, CreatePosRegisterRequest request);

    PosRegisterResponse getRegisterById(Long tenantId, Long companyId, Long id);

    PosRegisterResponse getRegisterByCode(Long tenantId, Long companyId, String registerCode);

    Page<PosRegisterResponse> listRegisters(Long tenantId, Long companyId, Pageable pageable);

    Page<PosRegisterResponse> listRegistersByStatus(Long tenantId, Long companyId,
                                                    PosRegisterStatus status, Pageable pageable);

    List<PosRegisterResponse> listActiveRegisters(Long tenantId, Long companyId);

    Page<PosRegisterResponse> searchRegisters(Long tenantId, Long companyId, String term, Pageable pageable);

    PosRegisterResponse updateRegister(Long tenantId, Long companyId, Long id, UpdatePosRegisterRequest request);

    PosRegisterResponse activateRegister(Long tenantId, Long companyId, Long id);

    PosRegisterResponse deactivateRegister(Long tenantId, Long companyId, Long id);
}
