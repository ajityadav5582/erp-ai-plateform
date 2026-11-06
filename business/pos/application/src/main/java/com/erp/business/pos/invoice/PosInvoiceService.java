package com.erp.business.pos.invoice;

import com.erp.business.pos.invoice.dto.CreatePosInvoiceRequest;
import com.erp.business.pos.domain.PosInvoice;

public interface PosInvoiceService {
    PosInvoice createInvoice(Long tenantId, Long companyId, CreatePosInvoiceRequest request);
}
