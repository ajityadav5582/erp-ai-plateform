package com.erp.business.pos.session;

import com.erp.business.pos.domain.PosRegister;
import com.erp.business.pos.domain.PosSession;
import com.erp.business.pos.domain.PosSessionStatus;
import com.erp.business.pos.register.exception.PosRegisterNotFoundException;
import com.erp.business.pos.register.infrastructure.persistence.PosRegisterRepository;
import com.erp.business.pos.session.dto.CloseSessionRequest;
import com.erp.business.pos.session.dto.OpenSessionRequest;
import com.erp.business.pos.session.dto.PosSessionResponse;
import com.erp.business.pos.session.exception.PosSessionNotFoundException;
import com.erp.business.pos.session.infrastructure.persistence.PosSessionRepository;
import com.erp.business.pos.session.mapper.PosSessionMapper;
import com.erp.business.pos.shared.context.BusinessContextAccessor;
import com.erp.business.pos.shared.numbering.DocumentNumberService;
import com.erp.business.pos.shared.security.PosSessionPermissions;
import com.erp.platform.security.annotation.RequiresPermission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * POS cashier session use-case implementation.
 *
 * <p>Opening a session is the coordination point: it allocates a gap-free session
 * number, persists the session, and flips the register's open-session pointer, all
 * inside one transaction — so a partial failure leaves neither a session without its
 * number nor a register pointing at a session that was never saved. Closing reverses
 * the pointer and records the cash variance.
 */
@Service
@Transactional
public class PosSessionServiceImpl implements PosSessionService {

    private static final String DOCUMENT_TYPE = "POS_SESSION";
    private static final String NUMBER_PREFIX = "SES-";

    private final PosSessionRepository sessionRepository;
    private final PosRegisterRepository registerRepository;
    private final PosSessionMapper sessionMapper;
    private final DocumentNumberService documentNumberService;
    private final BusinessContextAccessor context;

    public PosSessionServiceImpl(PosSessionRepository sessionRepository,
                                 PosRegisterRepository registerRepository,
                                 PosSessionMapper sessionMapper,
                                 DocumentNumberService documentNumberService,
                                 BusinessContextAccessor context) {
        this.sessionRepository = sessionRepository;
        this.registerRepository = registerRepository;
        this.sessionMapper = sessionMapper;
        this.documentNumberService = documentNumberService;
        this.context = context;
    }

    @Override
    @RequiresPermission(PosSessionPermissions.POS_SESSION_OPEN)
    public PosSessionResponse openSession(Long tenantId, Long companyId, OpenSessionRequest request) {
        PosRegister register = registerRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, request.registerId())
                .orElseThrow(() -> new PosRegisterNotFoundException(
                        "Register not found with ID: " + request.registerId()));

        Long fiscalYearId = context.findFiscalYearId();
        String periodKey = fiscalYearId != null
                ? "FY" + fiscalYearId : "CY" + LocalDate.now().getYear();
        String sessionNumber = documentNumberService.nextNumber(
                tenantId, companyId, DOCUMENT_TYPE, periodKey, NUMBER_PREFIX);

        PosSession session = PosSession.open(
                sessionNumber, register.getId(), register.getRegisterCode(), register.getRegisterName(),
                context.getUserId(), context.getCurrentUserFullName(),
                request.openingCashAmount(), fiscalYearId, request.notes());
        session.assignScope(tenantId, companyId);

        PosSession saved = sessionRepository.save(session);
        register.recordSessionOpened(saved.getId());
        registerRepository.save(register);

        return sessionMapper.toResponse(saved);
    }

    @Override
    @RequiresPermission(PosSessionPermissions.POS_SESSION_CLOSE)
    public PosSessionResponse closeSession(Long tenantId, Long companyId, Long id, CloseSessionRequest request) {
        PosSession session = load(tenantId, companyId, id);
        session.close(request.closingCashAmount(), request.closeNotes());

        PosRegister register = registerRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, session.getRegisterId())
                .orElseThrow(() -> new PosRegisterNotFoundException(
                        "Register not found with ID: " + session.getRegisterId()));
        register.recordSessionClosed(session.getId());
        registerRepository.save(register);

        return sessionMapper.toResponse(sessionRepository.save(session));
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(PosSessionPermissions.POS_SESSION_VIEW)
    public PosSessionResponse getSessionById(Long tenantId, Long companyId, Long id) {
        return sessionMapper.toResponse(load(tenantId, companyId, id));
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(PosSessionPermissions.POS_SESSION_VIEW)
    public PosSessionResponse getSessionByNumber(Long tenantId, Long companyId, String sessionNumber) {
        PosSession session = sessionRepository
                .findByTenantIdAndCompanyIdAndSessionNumber(tenantId, companyId, sessionNumber)
                .orElseThrow(() -> new PosSessionNotFoundException(
                        "Session not found with number: " + sessionNumber));
        return sessionMapper.toResponse(session);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(PosSessionPermissions.POS_SESSION_VIEW)
    public Optional<PosSessionResponse> findOpenSessionForRegister(Long tenantId, Long companyId, Long registerId) {
        return sessionRepository
                .findByTenantIdAndCompanyIdAndRegisterIdAndStatus(tenantId, companyId, registerId, PosSessionStatus.OPEN)
                .map(sessionMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(PosSessionPermissions.POS_SESSION_VIEW)
    public List<PosSessionResponse> listOpenSessionsForCurrentCashier(Long tenantId, Long companyId) {
        return sessionRepository
                .findByTenantIdAndCompanyIdAndCashierIdAndStatus(tenantId, companyId, context.getUserId(), PosSessionStatus.OPEN)
                .stream()
                .map(sessionMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(PosSessionPermissions.POS_SESSION_VIEW)
    public Page<PosSessionResponse> listSessions(Long tenantId, Long companyId, Pageable pageable) {
        return sessionRepository.findByTenantIdAndCompanyId(tenantId, companyId, pageable)
                .map(sessionMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(PosSessionPermissions.POS_SESSION_VIEW)
    public Page<PosSessionResponse> listSessionsByStatus(Long tenantId, Long companyId,
                                                         PosSessionStatus status, Pageable pageable) {
        return sessionRepository.findByTenantIdAndCompanyIdAndStatus(tenantId, companyId, status, pageable)
                .map(sessionMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(PosSessionPermissions.POS_SESSION_VIEW)
    public Page<PosSessionResponse> listSessionsByRegister(Long tenantId, Long companyId,
                                                           Long registerId, Pageable pageable) {
        return sessionRepository.findByTenantIdAndCompanyIdAndRegisterId(tenantId, companyId, registerId, pageable)
                .map(sessionMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(PosSessionPermissions.POS_SESSION_VIEW)
    public Page<PosSessionResponse> searchSessions(Long tenantId, Long companyId, String term, Pageable pageable) {
        return sessionRepository.search(tenantId, companyId, term, pageable)
                .map(sessionMapper::toResponse);
    }

    private PosSession load(Long tenantId, Long companyId, Long id) {
        return sessionRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, id)
                .orElseThrow(() -> new PosSessionNotFoundException("Session not found with ID: " + id));
    }
}
