package com.erp.business.pos.invoice.infrastructure.persistence;

import com.erp.business.pos.domain.PosInvoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PosInvoiceRepository extends JpaRepository<PosInvoice, Long> {
    Optional<PosInvoice> findByIdempotencyKey(UUID idempotencyKey);
}
