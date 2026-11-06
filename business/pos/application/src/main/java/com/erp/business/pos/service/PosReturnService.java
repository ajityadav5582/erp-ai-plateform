package com.erp.business.pos.service;

public interface PosReturnService {
    void processSalesReturn(Long originalTransactionId, ReturnRequestDto dto);
}
