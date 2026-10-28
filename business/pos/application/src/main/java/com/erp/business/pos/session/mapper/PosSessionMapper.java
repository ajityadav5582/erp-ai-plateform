package com.erp.business.pos.session.mapper;

import com.erp.business.pos.domain.PosSession;
import com.erp.business.pos.session.dto.PosSessionResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Maps the {@link PosSession} aggregate to its read model.
 */
@Component
public class PosSessionMapper {

    public PosSessionResponse toResponse(PosSession session) {
        return new PosSessionResponse(
                session.getId(),
                session.getTenantId(),
                session.getCompanyId(),
                session.getSessionNumber(),
                session.getRegisterId(),
                session.getRegisterCode(),
                session.getRegisterName(),
                session.getFiscalYearId(),
                session.getCashierId(),
                session.getCashierName(),
                toLocalDateTime(session.getOpenedAt()),
                toLocalDateTime(session.getClosedAt()),
                session.getOpeningCashAmount(),
                session.getClosingCashAmount(),
                session.getExpectedCash(),
                session.getCashVariance(),
                session.getTotalTransactions(),
                session.getTotalVoided(),
                session.getTotalCashSales(),
                session.getTotalCardSales(),
                session.getTotalWalletSales(),
                session.getTotalOtherSales(),
                session.getTotalVatCollected(),
                session.getGrossSales(),
                session.getStatus(),
                session.getNotes(),
                session.getCloseNotes(),
                session.getVersion()
        );
    }

    private LocalDateTime toLocalDateTime(java.time.Instant instant) {
        return instant != null ? LocalDateTime.ofInstant(instant, ZoneId.systemDefault()) : null;
    }
}
