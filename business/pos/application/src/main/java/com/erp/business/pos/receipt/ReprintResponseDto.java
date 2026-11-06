package com.erp.business.pos.receipt;

public record ReprintResponseDto(
        boolean isCopyOfOriginal,
        int copyNumber,
        int printCount
) {
}
