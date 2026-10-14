package com.erp.business.pos.session;

import com.erp.business.pos.domain.PosSessionStatus;
import com.erp.business.pos.session.dto.CloseSessionRequest;
import com.erp.business.pos.session.dto.OpenSessionRequest;
import com.erp.business.pos.session.dto.PosSessionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * POS cashier session (shift) use cases.
 *
 * <p>Every method is tenant- and company-scoped by its leading parameters; the
 * controller supplies them from the authenticated request context, never from the
 * payload. Authorisation is declared with {@code @RequiresPermission} on the
 * implementation and enforced by the shared aspect before the body runs.
 */
public interface PosSessionService {

    PosSessionResponse openSession(Long tenantId, Long companyId, OpenSessionRequest request);

    PosSessionResponse closeSession(Long tenantId, Long companyId, Long id, CloseSessionRequest request);

    PosSessionResponse getSessionById(Long tenantId, Long companyId, Long id);

    PosSessionResponse getSessionByNumber(Long tenantId, Long companyId, String sessionNumber);

    Optional<PosSessionResponse> findOpenSessionForRegister(Long tenantId, Long companyId, Long registerId);

    List<PosSessionResponse> listOpenSessionsForCurrentCashier(Long tenantId, Long companyId);

    Page<PosSessionResponse> listSessions(Long tenantId, Long companyId, Pageable pageable);

    Page<PosSessionResponse> listSessionsByStatus(Long tenantId, Long companyId,
                                                  PosSessionStatus status, Pageable pageable);

    Page<PosSessionResponse> listSessionsByRegister(Long tenantId, Long companyId,
                                                    Long registerId, Pageable pageable);

    Page<PosSessionResponse> searchSessions(Long tenantId, Long companyId, String term, Pageable pageable);
}
